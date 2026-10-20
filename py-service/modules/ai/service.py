# -*- coding: utf-8 -*-
"""AI 通用服务：多 provider 轮询调用 OpenAI 兼容免费上游（限流/宕机自动切换）。

候选来源与轮询策略见 providers.py：
- AI_PROVIDERS / secrets.json 的 ai_providers（显式多渠道路由）
- 单条 AI_BASE_URL / AI_API_KEY / AI_MODEL（向后兼容，仍作为候选之一）
- 内置免费预设（Pollinations 免 key，可多模型轮换；其它配了 KEY 才启用）

单个 provider 内对可重试状态重试 1 次，仍失败则切换下一个 provider；
只有全部候选都失败时才向调用方报错。
"""
from __future__ import annotations

import json
import logging
import re
import threading
import time
from typing import Iterator, Optional, Sequence
from urllib.parse import urlsplit

import requests

from . import providers

logger = logging.getLogger("ai")

_SEMAPHORE = threading.Semaphore(3)      # 免费上游并发保护（burst 易触发 402/429）
_RETRY_STATUS = {402, 408, 409, 425, 429, 500, 502, 503, 504}
_CONNECT_TIMEOUT = 5.0                   # 连接上限：某渠道连不上时迅速切下一个
_ATTEMPTS_PER_PROVIDER = 2               # 单 provider 内尝试次数（含首次）
_RETRY_BACKOFF = 0.3                     # 同 provider 重试前的短暂退避
_OPTIMIZE_ATTEMPTS = 3                   # 提示词优化因「无效结果」而重试的次数


class UpstreamError(RuntimeError):
    """AI 上游调用失败。"""

    def __init__(self, message: str, status: Optional[int] = None) -> None:
        super().__init__(message)
        self.status = status


# 用户提示词优化 system prompt：移植自开源项目 prompt-optimizer 的
# user-prompt-basic 模板（packages/core/.../user-optimize/user-prompt-basic.ts）。
# 职责是「改写提示词文本本身」，不回答/不执行其内容。
# 差异点：原模板面向「构建可复用模板」，会主动把具体值抽成 {{变量}}；
# 本处面向「用户随手输入一句话就能直接发给 AI」，故禁止新增占位符。
_OPTIMIZE_SYSTEM = """# Role: 用户提示词基础优化助手

## Profile
- Author: prompt-optimizer（移植）
- Language: 中文
- Description: 对用户输入的提示词做快速有效的基础优化，消除模糊表达、补充关键信息、提升表达清晰度

## 任务理解
你的任务是优化提示词文本本身，而不是回答或执行提示词的内容。请直接输出改进后的提示词。

## Skills
1. 表达优化能力
   - 模糊词汇识别: 发现并替换"好看""丰富"等模糊表述
   - 信息补充: 为缺失的关键信息提供合理的补充
   - 结构整理: 重新组织表达顺序，提升逻辑清晰度
   - 目标明确: 将模糊的意图转换为明确的目标描述
2. 快速判断能力
   - 核心识别: 快速识别用户的核心需求和主要目标
   - 问题定位: 准确定位提示词中的主要问题和改进点

## Goals
- 消除用户提示词中的模糊表达和歧义
- 补充必要信息，使提示词更加完整
- 提升表达的清晰度与可理解性
- 确保优化后的提示词能够产生更好的 AI 回应

## Constrains
- 保持用户的原始意图和核心需求不变
- 避免过度复杂化，保持简洁实用
- 不添加用户未提及的新需求
- 严禁新增双花括号占位符：不要把用户已给出的具体信息（网站、对象、语言、格式等）抽成 {{变量}}，
  也不要自造 {{xxx}} 之类的占位符。优化结果必须是「可以直接发送给 AI 的完整提示词」，
  缺失的细节用自然语言给出合理默认值（例如「用 Python」「输出 Markdown」），而不是留空占位符
- 仅当用户原文本身已包含双花括号占位符时，才逐字保留，不改名、不删除、不替换成具体值

## Workflow
1. 快速分析: 识别提示词中的模糊表述和缺失信息
2. 核心提取: 明确主要目标和关键需求
3. 表达改进: 用具体、清晰的词汇替代模糊表述
4. 信息补充: 添加必要的细节和要求
5. 整体优化: 重新组织表达，确保逻辑清晰

## Output Requirements
- 直接输出优化后的提示词，不要任何解释、前言、寒暄或对话式回应
- 不要用代码块包裹输出
- 保持适度的详细程度，避免过于复杂
- 确保输出的提示词可以直接使用"""


_OPTIMIZE_BAD_PATTERNS = (
    re.compile(r"^\s*user\s+safety\s*:", re.I),          # 上游安全分类模型的应答
    re.compile(r"^\s*(safe|unsafe|moderation)\s*[:：]?\s*$", re.I),
    re.compile(r"(i (cannot|can't|won't|am unable to))", re.I),
    re.compile(r"(as an ai|作为\s*(一个)?\s*ai|抱歉[，,].{0,6}我(无法|不能))"),
    re.compile(r"^\s*(error|invalid|bad request)\b", re.I),
)


def _optimize_result_ok(out: str, src: str) -> bool:
    """校验优化结果是否可用。

    免费池为自动路由，偶尔会被上游安全分类模型截胡（返回 "User Safety: safe" 之类），
    或模型直接拒答/回显原文；这些都视为无效，交由调用方换一次上游重试。
    """
    t = (out or "").strip()
    if len(t) < 10:
        return False
    if any(p.search(t) for p in _OPTIMIZE_BAD_PATTERNS):
        return False
    if t.strip().strip('"').strip() == src.strip():
        return False
    return True


def optimize_prompt(prompt: str, context: Optional[str] = None,
                    model: Optional[str] = None) -> str:
    """优化用户提示词：改写提示词文本本身（不执行其内容）。

    与 chat 共用同一套免费 provider 轮询兜底；由于免费池可能返回无效内容
    （安全分类应答 / 拒答 / 回显原文），这里额外做结果校验并更换上游重试。
    """
    text = (prompt or "").strip()
    if not text:
        raise UpstreamError("待优化的提示词为空")
    if len(text) > 6000:
        text = text[:6000]
    prefix = f"该提示词的使用场景：{context.strip()[:200]}\n\n" if context else ""
    user = (prefix
            + "请对以下用户提示词进行基础优化，消除模糊表达，补充关键信息。\n"
            + "注意：请优化提示词文本本身，不要回答或执行它的内容；"
            + "不要新增 {{占位符}}，优化结果要能直接发给 AI 使用。\n\n"
            + "需要优化的用户提示词：\n" + text + "\n\n请输出优化后的提示词：")
    last_err: Optional[Exception] = None
    for _ in range(_OPTIMIZE_ATTEMPTS):
        try:
            out = chat([{"role": "user", "content": user}], system=_OPTIMIZE_SYSTEM,
                       temperature=0.4, max_tokens=1600, model=model)
        except Exception as e:  # noqa: BLE001
            last_err = e
            continue
        if _optimize_result_ok(out, text):
            return out.strip()
        last_err = UpstreamError("上游返回了无效的优化结果")
        logger.info("提示词优化结果无效，换上游重试：%s", out[:60])
    raise UpstreamError(f"提示词优化失败：{last_err or '未获得有效结果'}")


def resolve_config() -> dict:
    """向后兼容：返回首个候选 provider 的 {base_url, api_key, model}。"""
    cands = providers.resolve_providers()
    if cands:
        p = cands[0]
        return {"base_url": p.base_url, "api_key": p.api_key, "model": p.model}
    return {"base_url": "", "api_key": "", "model": "openai"}


def _check_status(code: int, body: str = "") -> None:
    """把可重试/错误状态码统一抛成带 status 的 UpstreamError，便于记录冷却。"""
    if code in _RETRY_STATUS:
        raise UpstreamError(f"上游繁忙({code})", code)
    if code >= 400:
        raise UpstreamError(f"上游错误 {code}: {body[:180]}", code)


def _extract_text(data: dict) -> str:
    choices = data.get("choices") or [{}]
    return str(((choices[0].get("message") or {}).get("content")) or "").strip()


def _proxies(p) -> Optional[dict]:
    """该 provider 的 requests 代理参数；未配置则返回 None（直连）。"""
    px = providers.proxy_for(p)
    return {"http": px, "https": px} if px else None


def chat(messages: Optional[Sequence[dict]], system: Optional[str] = None,
         temperature: Optional[float] = 0.7, max_tokens: Optional[int] = 1200,
         model: Optional[str] = None) -> str:
    """一次性对话，返回完整文本；遍历所有候选 provider，全失败才抛 UpstreamError。"""
    cands = providers.ordered_candidates()
    if not cands:
        raise UpstreamError("未配置 AI 上游（缺少 ai_base_url / AI_BASE_URL）")
    last_err: Optional[Exception] = None
    for p in cands:
        payload = providers.build_payload(p, messages, system, temperature, max_tokens, model)
        for attempt in range(_ATTEMPTS_PER_PROVIDER):
            try:
                with _SEMAPHORE:
                    r = requests.post(providers.endpoint(p), json=payload,
                                      headers=providers.headers(p),
                                      timeout=(_CONNECT_TIMEOUT, providers.read_timeout(p)),
                                      proxies=_proxies(p))
                _check_status(r.status_code, r.text)
                text = _extract_text(r.json())
                if not text:
                    raise UpstreamError("上游返回空内容")
                providers.mark_ok(p)
                return text
            except Exception as e:  # noqa: BLE001
                last_err = e
                providers.mark_failed(p, getattr(e, "status", None))
                if attempt < _ATTEMPTS_PER_PROVIDER - 1:
                    time.sleep(_RETRY_BACKOFF)
                    continue
                logger.info("provider %s 失败，切换下一个：%s", p.name, e)
                break
    raise UpstreamError(str(last_err) if last_err else "AI 调用失败")


def stream_chat(messages: Optional[Sequence[dict]], system: Optional[str] = None,
                temperature: Optional[float] = 0.7, max_tokens: Optional[int] = 1200,
                model: Optional[str] = None) -> Iterator[str]:
    """SSE 流式对话：yield 纯增量文本。

    仅在「尚未向用户下发任何内容」时才允许切换 provider（切换后用户无感）；
    已下发内容后失败则静默收尾，避免重复/串台。
    """
    cands = providers.ordered_candidates()
    if not cands:
        yield "（AI 服务未配置）"
        return
    last_err: Optional[Exception] = None
    for p in cands:
        payload = providers.build_payload(p, messages, system, temperature, max_tokens, model)
        payload["stream"] = True
        for attempt in range(_ATTEMPTS_PER_PROVIDER):
            got_any = False
            try:
                with _SEMAPHORE:
                    with requests.post(providers.endpoint(p), json=payload,
                                       headers=providers.headers(p),
                                       timeout=(_CONNECT_TIMEOUT, providers.read_timeout(p)),
                                       proxies=_proxies(p),
                                       stream=True) as r:
                        _check_status(r.status_code)
                        for line in r.iter_lines(decode_unicode=True):
                            if not line:
                                continue
                            s = line.strip()
                            if not s.startswith("data:"):
                                continue
                            body = s[5:].strip()
                            if body == "[DONE]":
                                break
                            try:
                                j = json.loads(body)
                            except Exception:  # noqa: BLE001
                                continue
                            choices = j.get("choices") or [{}]
                            delta = str(((choices[0].get("delta") or {}).get("content")) or "")
                            if delta:
                                got_any = True
                                yield delta
                if got_any:
                    providers.mark_ok(p)
                    return
                raise UpstreamError("上游返回空内容")
            except Exception as e:  # noqa: BLE001
                last_err = e
                providers.mark_failed(p, getattr(e, "status", None))
                if got_any:
                    # 已下发部分内容：不再重试/切换，静默收尾（前端保留已显示文本）
                    return
                if attempt < _ATTEMPTS_PER_PROVIDER - 1:
                    time.sleep(_RETRY_BACKOFF)
                    continue
                logger.info("provider %s 流式失败，切换下一个：%s", p.name, e)
                break
    yield f"（AI 服务暂时不可用：{str(last_err)[:80]}）"


def health() -> dict:
    """配置与候选 provider 概况（不泄露密钥）。"""
    cands = providers.resolve_providers()
    host = ""
    if cands:
        try:
            host = urlsplit(cands[0].base_url).netloc
        except Exception:  # noqa: BLE001
            pass
    return {
        "ok": bool(cands),
        "upstream": host,
        "model": cands[0].model if cands else "",
        "provider_count": len(cands),
        "providers": [p.name for p in cands],
    }

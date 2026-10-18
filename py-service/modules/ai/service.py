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


class UpstreamError(RuntimeError):
    """AI 上游调用失败。"""

    def __init__(self, message: str, status: Optional[int] = None) -> None:
        super().__init__(message)
        self.status = status


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

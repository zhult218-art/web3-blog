# -*- coding: utf-8 -*-
"""AI 通用服务：解析免费上游配置并调用 OpenAI 兼容接口（含重试与简易限流）。

配置优先级（与 .env 注释约定一致）：
1. cockpit secrets.json（py-service/data/cockpit/user_data/secrets.json，设置页热配置）
2. 环境变量 AI_BASE_URL / AI_API_KEY / AI_MODEL（仓库根 .env，py 侧主配置）
3. 环境变量 LLM_BASE_URL / LLM_API_KEY / LLM_MODEL（量化模块兼容）
"""
from __future__ import annotations

import json
import logging
import os
import threading
import time
from pathlib import Path
from typing import Iterator, Optional, Sequence
from urllib.parse import urlsplit

import requests

logger = logging.getLogger("ai")

_SECRETS_PATH = Path(__file__).resolve().parents[2] / "data" / "cockpit" / "user_data" / "secrets.json"
_SEMAPHORE = threading.Semaphore(3)      # 免费上游并发保护（burst 易触发 402/429）
_RETRY_STATUS = {402, 408, 409, 425, 429, 500, 502, 503, 504}
_TIMEOUT = (15, 180)                     # (连接, 读取) 秒：免费模型偶发慢，给足窗口
_MAX_ATTEMPTS = 3


class UpstreamError(RuntimeError):
    """AI 上游调用失败。"""


def _read_secrets() -> dict:
    try:
        if _SECRETS_PATH.is_file():
            data = json.loads(_SECRETS_PATH.read_text(encoding="utf-8"))
            return data if isinstance(data, dict) else {}
    except Exception as e:  # noqa: BLE001
        logger.debug("secrets.json 读取失败: %s", e)
    return {}


def resolve_config() -> dict:
    """合并三级配置源，返回 {base_url, api_key, model}。"""
    s = _read_secrets()
    base = str(s.get("ai_base_url") or os.environ.get("AI_BASE_URL") or os.environ.get("LLM_BASE_URL") or "").strip()
    key = str(s.get("ai_api_key") or os.environ.get("AI_API_KEY") or os.environ.get("LLM_API_KEY") or "").strip()
    model = str(s.get("ai_model") or os.environ.get("AI_MODEL") or os.environ.get("LLM_MODEL") or "openai").strip()
    return {"base_url": base.rstrip("/"), "api_key": key, "model": model or "openai"}


def _endpoint(cfg: dict) -> str:
    base = cfg["base_url"]
    if base.endswith("/chat/completions"):
        return base
    return base + "/chat/completions"


def _headers(cfg: dict) -> dict:
    h = {"Content-Type": "application/json", "Accept": "*/*"}
    if cfg.get("api_key"):
        h["Authorization"] = "Bearer " + cfg["api_key"]
    return h


def _build_payload(cfg: dict, messages: Optional[Sequence[dict]], system: Optional[str],
                   temperature: Optional[float], max_tokens: Optional[int],
                   model: Optional[str]) -> dict:
    msgs = []
    if system:
        msgs.append({"role": "system", "content": str(system)})
    for m in messages or []:
        role = str((m or {}).get("role") or "user")
        content = str((m or {}).get("content") or "")
        if role in ("system", "user", "assistant") and content:
            msgs.append({"role": role, "content": content})
    payload = {
        "model": (model or cfg["model"]),
        "messages": msgs,
        "temperature": 0.7 if temperature is None else float(temperature),
    }
    if max_tokens:
        payload["max_tokens"] = int(max_tokens)
    return payload


def chat(messages: Optional[Sequence[dict]], system: Optional[str] = None,
         temperature: Optional[float] = 0.7, max_tokens: Optional[int] = 1200,
         model: Optional[str] = None) -> str:
    """一次性对话，返回完整文本；失败抛 UpstreamError。"""
    cfg = resolve_config()
    if not cfg["base_url"]:
        raise UpstreamError("未配置 AI 上游（缺少 ai_base_url / AI_BASE_URL）")
    payload = _build_payload(cfg, messages, system, temperature, max_tokens, model)
    last_err: Optional[Exception] = None
    for attempt in range(_MAX_ATTEMPTS):
        try:
            with _SEMAPHORE:
                r = requests.post(_endpoint(cfg), json=payload, headers=_headers(cfg), timeout=_TIMEOUT)
            if r.status_code in _RETRY_STATUS:
                raise UpstreamError(f"上游繁忙({r.status_code})")
            if r.status_code >= 400:
                raise UpstreamError(f"上游错误 {r.status_code}: {r.text[:180]}")
            data = r.json()
            choices = data.get("choices") or [{}]
            text = str(((choices[0].get("message") or {}).get("content")) or "").strip()
            if not text:
                raise UpstreamError("上游返回空内容")
            return text
        except Exception as e:  # noqa: BLE001
            last_err = e
            if attempt < _MAX_ATTEMPTS - 1:
                time.sleep(1.2 * (attempt + 1))
    raise UpstreamError(str(last_err) if last_err else "AI 调用失败")


def stream_chat(messages: Optional[Sequence[dict]], system: Optional[str] = None,
                temperature: Optional[float] = 0.7, max_tokens: Optional[int] = 1200,
                model: Optional[str] = None) -> Iterator[str]:
    """SSE 流式对话：yield 纯增量文本；彻底失败时 yield 一条错误提示并结束。"""
    cfg = resolve_config()
    if not cfg["base_url"]:
        yield "（AI 服务未配置）"
        return
    payload = _build_payload(cfg, messages, system, temperature, max_tokens, model)
    payload["stream"] = True
    for attempt in range(_MAX_ATTEMPTS):
        got_any = False
        try:
            with _SEMAPHORE:
                with requests.post(_endpoint(cfg), json=payload, headers=_headers(cfg),
                                   timeout=_TIMEOUT, stream=True) as r:
                    if r.status_code in _RETRY_STATUS:
                        raise UpstreamError(f"上游繁忙({r.status_code})")
                    if r.status_code >= 400:
                        raise UpstreamError(f"上游错误 {r.status_code}")
                    for line in r.iter_lines(decode_unicode=True):
                        if not line:
                            continue
                        s = line.strip()
                        if not s.startswith("data:"):
                            continue
                        body = s[5:].strip()
                        if body == "[DONE]":
                            return
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
                return
            raise UpstreamError("上游返回空内容")
        except Exception as e:  # noqa: BLE001
            if got_any:
                # 已下发部分内容：不再重试，静默收尾（前端保留已显示文本）
                return
            if attempt < _MAX_ATTEMPTS - 1:
                time.sleep(1.2 * (attempt + 1))
                continue
            yield f"（AI 服务暂时不可用：{str(e)[:80]}）"
            return


def health() -> dict:
    cfg = resolve_config()
    host = ""
    try:
        host = urlsplit(cfg["base_url"]).netloc
    except Exception:  # noqa: BLE001
        pass
    return {"ok": bool(cfg["base_url"]), "upstream": host, "model": cfg["model"]}

# -*- coding: utf-8 -*-
"""LLM 适配层：为星图助手提供流式文本生成。

优先使用 py-service 内已有的 OpenAI 兼容客户端（cockpit 模块的 ai_provider，
读取项目「AI 设置」中配置的 Key）；不可用时回退到网关的 Agent SSE 接口
（POST /api/agent/chat，应用级令牌，前端 @/api/agent 已在用）。
"""
from __future__ import annotations

import json
import os
import sys
from collections.abc import AsyncIterator
from pathlib import Path

import httpx

_GATEWAY_AGENT_URL = os.environ.get("VOICE_AGENT_URL", "http://127.0.0.1:8080/agent/chat")
_AGENT_TIMEOUT = float(os.environ.get("VOICE_LLM_TIMEOUT", "120"))

SYSTEM_PROMPT = (
    "你是「星图助手」，本站（极光世界）的语音智能助理，叫「星图」或「星途」都行。"
    "说话要口语化、简短自然，适合朗读：每句尽量不超过 30 字，不要用 Markdown、列表符号、"
    "代码块或表情符号；一次最多 3 句话。"
    "\n站点主要模块：首页/、社区 /community、博客 /blog、商城 /shop、多媒体 /media、"
    "金融量化 /quant、在线工具 /tools、软件库 /software、资源 /resources、Agent 工作台 /agent、"
    "个人中心 /profile。"
    "\n当用户想「打开/进入/去/看看某个模块」时：先用一句话确认并说明到了那里可以做什么，"
    "然后在回复的最后单独一行输出引导块，格式严格为："
    '[[GUIDE]]{"route":"路径","tip":"到达后给用户的一句操作提示"}'
    "。引导块不要朗读、不要放进正文句子里。若用户只是闲聊或提问，就不要输出引导块。"
)


def _cockpit_provider():
    """加载 cockpit 的 ai_provider（需先注入其 sys.path 与配置 env）。返回模块或 None。"""
    cockpit_dir = Path(__file__).resolve().parent.parent / "cockpit"
    if not cockpit_dir.exists():
        return None
    if str(cockpit_dir) not in sys.path:
        sys.path.insert(0, str(cockpit_dir))
    # 与 cockpit/__init__.py 一致的默认注入，保证 app.config 能正常加载
    data_dir = cockpit_dir.parent.parent / "data" / "cockpit"
    static_dir = cockpit_dir / "static"
    os.environ.setdefault("DATA_DIR", str(data_dir))
    os.environ.setdefault("STATIC_DIR", str(static_dir))
    os.environ.setdefault("TIERS_YAML", str(cockpit_dir / "tiers.yaml"))
    try:
        from app.services import ai_provider  # type: ignore
        return ai_provider
    except Exception:  # noqa: BLE001
        return None


def current_backend() -> str:
    """返回当前可用的 LLM 后端标识：'cockpit' | 'agent'。"""
    p = _cockpit_provider()
    if p is not None:
        try:
            if p.ai_configured():
                return "cockpit"
        except Exception:  # noqa: BLE001
            pass
    return "agent"


async def _stream_cockpit(messages: list[dict]) -> AsyncIterator[str]:
    p = _cockpit_provider()
    if p is None:
        return
    async for chunk in p.stream_ai_text(messages, temperature=0.6, max_tokens=800, timeout=_AGENT_TIMEOUT):
        if chunk:
            yield chunk


async def _stream_agent(messages: list[dict]) -> AsyncIterator[str]:
    payload = {"messages": messages}
    async with httpx.AsyncClient(timeout=httpx.Timeout(_AGENT_TIMEOUT, connect=10.0)) as client:
        async with client.stream("POST", _GATEWAY_AGENT_URL, json=payload) as resp:
            if resp.status_code >= 400:
                body = ""
                try:
                    body = (await resp.aread()).decode("utf-8", "replace")
                except Exception:  # noqa: BLE001
                    pass
                raise RuntimeError(f"Agent 接口异常({resp.status_code}): {body[:200]}")
            async for line in resp.aiter_lines():
                if not line:
                    continue
                line = line.strip()
                if not line.startswith("data:"):
                    continue
                data = line[5:].strip()
                if data == "[DONE]":
                    break
                try:
                    obj = json.loads(data)
                except Exception:  # noqa: BLE001
                    continue
                delta = (((obj.get("choices") or [{}])[0].get("delta") or {}).get("content"))
                if delta:
                    yield delta


async def stream_reply(messages: list[dict]) -> AsyncIterator[str]:
    """流式生成回复文本增量；后端失败时抛出异常由路由层转为 error 事件。"""
    backend = current_backend()
    if backend == "cockpit":
        got = False
        async for chunk in _stream_cockpit([{"role": "system", "content": SYSTEM_PROMPT}, *messages]):
            got = True
            yield chunk
        if got:
            return
    async for chunk in _stream_agent([{"role": "system", "content": SYSTEM_PROMPT}, *messages]):
        yield chunk


def available() -> bool:
    """LLM 是否可用（任一后端可尝试即视为可用）。"""
    return True

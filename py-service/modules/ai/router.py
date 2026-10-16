# -*- coding: utf-8 -*-
"""ai 模块路由：全站免费 AI 通用接口（一次性 JSON + SSE 流式）。

- POST /api/ai/chat        一次性返回完整文本
- POST /api/ai/chat/stream SSE 流式（data: {"delta": "..."} 增量）
- GET  /api/ai/health      配置与上游信息（不泄露密钥）
"""
from __future__ import annotations

import json
import logging
from typing import List, Optional

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from . import service

logger = logging.getLogger("ai")

router = APIRouter(prefix="/api/ai", tags=["ai"])


class ChatMessage(BaseModel):
    role: str = "user"
    content: str = Field(default="")


class ChatRequest(BaseModel):
    messages: List[ChatMessage] = Field(default_factory=list)
    system: Optional[str] = None
    temperature: Optional[float] = 0.7
    max_tokens: Optional[int] = 1200
    model: Optional[str] = None


def _to_dicts(req: ChatRequest) -> List[dict]:
    return [{"role": m.role, "content": m.content} for m in req.messages]


@router.get("/health")
def ai_health() -> dict:
    return {"code": 0, "data": service.health()}


@router.post("/chat")
def ai_chat(req: ChatRequest) -> dict:
    try:
        text = service.chat(_to_dicts(req), system=req.system, temperature=req.temperature,
                            max_tokens=req.max_tokens, model=req.model)
        return {"code": 0, "msg": "ok", "data": {"text": text}}
    except Exception as e:  # noqa: BLE001
        logger.warning("ai/chat 失败: %s", e)
        return {"code": 500, "msg": str(e)[:200], "data": {"text": ""}}


@router.post("/chat/stream")
def ai_chat_stream(req: ChatRequest) -> StreamingResponse:
    def gen():
        try:
            for delta in service.stream_chat(_to_dicts(req), system=req.system, temperature=req.temperature,
                                             max_tokens=req.max_tokens, model=req.model):
                yield "data: " + json.dumps({"delta": delta}, ensure_ascii=False) + "\n\n"
        except Exception as e:  # noqa: BLE001
            logger.warning("ai/chat/stream 失败: %s", e)
            yield "data: " + json.dumps({"delta": f"（AI 服务异常：{str(e)[:80]}）"}, ensure_ascii=False) + "\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(gen(), media_type="text/event-stream",
                             headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})

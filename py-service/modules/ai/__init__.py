# -*- coding: utf-8 -*-
"""AI 通用模块：全站免费 AI 通道（OpenAI 兼容上游，默认 pollinations 免费端点）。

路径：POST /api/ai/chat（一次性）、POST /api/ai/chat/stream（SSE 流式）；
前端经 Vite /pyai 代理访问（/pyai/chat → /api/ai/chat）。

配置优先级：cockpit secrets.json（设置页热配置）→ 环境变量 AI_*（.env）→ 环境变量 LLM_*（量化兼容）。
供：博客 AI 摘要/问答、社区助手、后台 AI 解读、AI 语义搜索、音乐/相册 AI 等全站功能共用。
"""
from __future__ import annotations

import logging

from . import service
from .router import router

logger = logging.getLogger("ai")

MODULE_META = {
    "title": "AI 通用通道",
    "description": "全站免费 AI 能力（摘要/问答/解读/搜索/助手）",
    "icon": "🤖",
    "version": "1.0.0",
}


def health() -> dict:
    return service.health()


__all__ = ["router", "MODULE_META", "health", "service"]

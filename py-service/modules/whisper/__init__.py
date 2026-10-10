# -*- coding: utf-8 -*-
"""语音识别模块（faster-whisper）。路径：POST /transcribe。"""
from __future__ import annotations

from .router import router
from . import service

MODULE_META = {
    "title": "语音识别",
    "description": "faster-whisper 本地语音转文字",
    "icon": "🎙️",
    "version": "1.0.0",
}


def health() -> dict:
    return {
        "ok": service.available(),
        "model": service.MODEL_SIZE,
        "device": service.DEVICE,
    }


__all__ = ["router", "MODULE_META", "health", "service"]

# -*- coding: utf-8 -*-
"""语音助手模块：全双工语音编排（流式 ASR + VAD 打断 + LLM 流式 + 句子下发 + 页面引导）。

路径：WS /api/voice/ws（前端经 Vite /voiceapi 代理）。

能力：
  - 服务端流式 ASR（sherpa-onnx streaming paraformer）：边说边出字幕，端点给最终文本
  - 服务端能量 VAD：开口即打断、静音 700ms 判定说完了（零额外依赖）
  - LLM 流式生成并按句切分下发，前端 Edge TTS 逐句播放（低首音延迟）
  - 静态/LLM 双通道页面引导
  - 服务端 ASR 不可用时自动退回前端 Web Speech（CT_TEXT）路径
"""
from __future__ import annotations

import logging
import threading

from . import asr, llm, vad
from .router import router

logger = logging.getLogger("voice")

MODULE_META = {
    "title": "语音助手",
    "description": "全双工语音对话编排（流式 ASR + VAD 打断 + 流式 LLM + 引导）",
    "icon": "🎤",
    "version": "2.0.0",
}


def _warmup() -> None:
    try:
        asr.warmup()
    except Exception as e:  # noqa: BLE001
        logger.warning("语音 ASR 预热失败: %s", e)


def startup() -> None:
    """uvicorn 启动时在独立线程预热 ASR 模型，避免首次识别卡顿。"""
    if not asr.available():
        logger.info("语音 ASR 未就绪（模型缺失），将使用前端 Web Speech 兜底")
        return
    threading.Thread(target=_warmup, name="voice-asr-warmup", daemon=True).start()


def health() -> dict:
    return {
        "ok": True,
        "vad": vad.health(),
        "llm": {"backend": llm.current_backend()},
        "asr": asr.health(),
    }


__all__ = ["router", "MODULE_META", "health", "startup", "asr", "llm", "vad"]

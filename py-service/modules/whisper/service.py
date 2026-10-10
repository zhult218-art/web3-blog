# -*- coding: utf-8 -*-
"""whisper 模块：语音识别服务（faster-whisper 懒加载，未安装时模块仍可被发现）。"""
from __future__ import annotations

import os
import tempfile

os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")
os.environ.setdefault("HF_HUB_DISABLE_XET", "1")

MODEL_SIZE = os.environ.get("WHISPER_MODEL", "small")
DEVICE = os.environ.get("WHISPER_DEVICE", "cpu")
COMPUTE = os.environ.get("WHISPER_COMPUTE", "int8")

_model = None


def available() -> bool:
    try:
        import faster_whisper  # noqa: F401
        return True
    except Exception:  # noqa: BLE001
        return False


def get_model():
    global _model
    if _model is None:
        from faster_whisper import WhisperModel

        _model = WhisperModel(MODEL_SIZE, device=DEVICE, compute_type=COMPUTE)
    return _model


def transcribe(data: bytes, language: str = "zh") -> str:
    if not data:
        return ""
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        tmp.write(data)
        tmp_path = tmp.name
    try:
        model = get_model()
        segments, _info = model.transcribe(
            tmp_path,
            language=language,
            beam_size=1,
            vad_filter=True,
            vad_parameters={"min_silence_duration_ms": 350},
        )
        return "".join(s.text for s in segments).strip()
    finally:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass

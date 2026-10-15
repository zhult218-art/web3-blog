# -*- coding: utf-8 -*-
"""服务端流式 ASR：sherpa-onnx 流式 Paraformer（中英双语，CPU 实时）。

选型说明：Web Speech 依赖 Google 语音服务（国内不可用），faster-whisper 为整段
转写不具备流式能力；sherpa-onnx 的 streaming paraformer 无需 torch、纯 CPU 即可
实时出字，最贴合"边说话边转文字"的需求。

模型目录（可用 VOICE_ASR_DIR 覆盖）：
  py-service/data/asr/sherpa-onnx-streaming-paraformer-bilingual-zh-en/
    tokens.txt  encoder.int8.onnx  decoder.int8.onnx

未安装 sherpa-onnx 或模型缺失时 available() 返回 False，语音链路退回
「浏览器 Web Speech + 前端端点检测」，功能不中断。
"""
from __future__ import annotations

import logging
import os
from pathlib import Path

logger = logging.getLogger("voice.asr")

_DEFAULT_DIR = (
    Path(__file__).resolve().parent.parent.parent
    / "data" / "asr" / "sherpa-onnx-streaming-paraformer-bilingual-zh-en"
)
MODEL_DIR = Path(os.environ.get("VOICE_ASR_DIR") or _DEFAULT_DIR)
NUM_THREADS = int(os.environ.get("VOICE_ASR_THREADS", "2"))
# 服务端端点检测：仅在 VAD 关闭时作为兜底（VAD 默认开启且更灵敏）
ENABLE_ENDPOINT = os.environ.get("VOICE_ASR_ENDPOINT", "0") == "1"
SAMPLE_RATE = 16000

_recognizer = None
_load_failed = False


def _model_files() -> tuple[Path, Path, Path]:
    return MODEL_DIR / "tokens.txt", MODEL_DIR / "encoder.int8.onnx", MODEL_DIR / "decoder.int8.onnx"


def available() -> bool:
    """sherpa-onnx 可导入且模型文件齐全。"""
    if _load_failed:
        return False
    try:
        import sherpa_onnx  # noqa: F401
    except Exception:  # noqa: BLE001
        return False
    return all(p.exists() for p in _model_files())


def get_recognizer():
    """懒加载识别器；加载失败置 _load_failed 并返回 None。"""
    global _recognizer, _load_failed
    if _recognizer is not None:
        return _recognizer
    if _load_failed or not available():
        return None
    try:
        import sherpa_onnx

        tokens, encoder, decoder = _model_files()
        _recognizer = sherpa_onnx.OnlineRecognizer.from_paraformer(
            tokens=str(tokens),
            encoder=str(encoder),
            decoder=str(decoder),
            num_threads=NUM_THREADS,
            sample_rate=SAMPLE_RATE,
            feature_dim=80,
            enable_endpoint_detection=ENABLE_ENDPOINT,
            decoding_method="greedy_search",
            provider="cpu",
        )
        logger.info("sherpa-onnx 流式 ASR 已加载: %s", MODEL_DIR.name)
    except Exception as e:  # noqa: BLE001
        logger.warning("sherpa-onnx 流式 ASR 加载失败: %s", e)
        _load_failed = True
        _recognizer = None
    return _recognizer


def warmup() -> bool:
    """启动期预热，避免首句识别卡顿。"""
    return get_recognizer() is not None


class Stream:
    """单条流式识别会话（每轮用户说话一条）。"""

    __slots__ = ("_rec", "_s", "_text")

    def __init__(self) -> None:
        self._rec = get_recognizer()
        self._s = self._rec.create_stream() if self._rec is not None else None
        self._text = ""

    @property
    def ok(self) -> bool:
        return self._s is not None

    def accept(self, pcm) -> None:
        """喂入 float32 16kHz 单声道 PCM。"""
        if self._s is not None:
            self._s.accept_waveform(SAMPLE_RATE, pcm)

    def decode(self) -> str:
        """解码当前已就绪的部分，返回累计文本（无变化时返回原值）。"""
        if self._s is None:
            return self._text
        while self._rec.is_ready(self._s):
            self._rec.decode_stream(self._s)
        text = (self._rec.get_result(self._s) or "").strip()
        if text:
            self._text = text
        return self._text

    def finish(self) -> str:
        """标记输入结束并跑完剩余帧，返回完整文本（不调用会丢掉尾字）。"""
        if self._s is None:
            return self._text
        try:
            self._s.input_finished()
        except Exception:  # noqa: BLE001
            pass
        while self._rec.is_ready(self._s):
            self._rec.decode_stream(self._s)
        text = (self._rec.get_result(self._s) or "").strip()
        if text:
            self._text = text
        return self._text

    def text(self) -> str:
        return self._text

    def is_endpoint(self) -> bool:
        if self._s is None:
            return False
        return bool(self._rec.is_endpoint(self._s))

    def reset(self) -> None:
        if self._s is not None and self.is_endpoint():
            self._rec.reset(self._s)


def health() -> dict:
    return {
        "engine": "sherpa-onnx-streaming-paraformer",
        "available": available(),
        "dir": str(MODEL_DIR),
    }

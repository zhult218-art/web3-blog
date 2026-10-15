# -*- coding: utf-8 -*-
"""语音端点检测（VAD）。

优先使用 sherpa-onnx 内置的 Silero VAD（神经网络，抗噪；且在端点处给出**完整
语音段**，用于最终识别，避免纯能量 VAD 丢首尾字）。模型缺失时退回纯 numpy 的
能量 VAD，功能不中断。

push() 返回事件列表，元素为 (kind, samples)：
  ("start", pre_samples)  —— 检测到开口；pre_samples 含开口前的预滚缓冲，
                              供流式识别补上被 VAD 延迟吞掉的起始音
  ("end",   seg_samples)  —— 检测到说完了；seg_samples 为该句完整音频
"""
from __future__ import annotations

import os
from pathlib import Path

import numpy as np

FRAME = 512                                   # 32ms @ 16kHz
FRAME_MS = 32
SILENCE_MS = int(os.environ.get("VOICE_SILENCE_MS", "600"))
START_FRAMES = int(os.environ.get("VOICE_START_FRAMES", "3"))    # 连续 ~96ms 判定开口
ENERGY_MULT = float(os.environ.get("VOICE_ENERGY_MULT", "3.0"))
MIN_RMS = float(os.environ.get("VOICE_MIN_RMS", "0.008"))

_PRE_ROLL_MS = 800                            # 预滚缓冲：补上 VAD 判定延迟吞掉的起始音
_PRE_ROLL_SAMPLES = 16000 * _PRE_ROLL_MS // 1000

_SILERO_MODEL = Path(
    os.environ.get("VOICE_VAD_MODEL")
    or (Path(__file__).resolve().parent.parent.parent / "data" / "asr" / "silero_vad.onnx")
)

_vad_silero = None
try:  # noqa: SIM105
    if _SILERO_MODEL.exists():
        import sherpa_onnx  # type: ignore

        _vad_silero = sherpa_onnx
except Exception:  # noqa: BLE001
    _vad_silero = None

BACKEND = "silero" if _vad_silero is not None else "energy"


class SileroVAD:
    """基于 sherpa-onnx Silero VAD 的流式端点检测。"""

    def __init__(self) -> None:
        cfg = _vad_silero.VadModelConfig()
        cfg.silero_vad.model = str(_SILERO_MODEL)
        cfg.silero_vad.threshold = float(os.environ.get("VOICE_VAD_THRESHOLD", "0.5"))
        cfg.silero_vad.min_silence_duration = SILENCE_MS / 1000.0
        cfg.silero_vad.min_speech_duration = 0.15
        cfg.silero_vad.max_speech_duration = 30.0
        cfg.sample_rate = 16000
        self._vad = _vad_silero.VoiceActivityDetector(cfg, buffer_size_in_seconds=30)
        self.speaking = False
        self._pre: list[np.ndarray] = []
        self._pre_len = 0
        self._cur: list[np.ndarray] = []      # 本轮语音累积（该版本不从 VAD 取回样本）

    def reset(self) -> None:
        self._vad.reset()
        self.speaking = False
        self._pre = []
        self._pre_len = 0
        self._cur = []

    def _push_pre(self, pcm: np.ndarray) -> None:
        self._pre.append(pcm)
        self._pre_len += pcm.size
        while self._pre_len > _PRE_ROLL_SAMPLES and self._pre:
            self._pre_len -= self._pre.pop(0).size

    def _take_pre(self) -> np.ndarray:
        if not self._pre:
            return np.zeros(0, dtype=np.float32)
        out = np.concatenate(self._pre)
        self._pre = []
        self._pre_len = 0
        return out

    def push(self, pcm: np.ndarray) -> list[tuple[str, np.ndarray]]:
        events: list[tuple[str, np.ndarray]] = []
        if pcm is None or len(pcm) == 0:
            return events
        pcm = np.asarray(pcm, dtype=np.float32)

        if not self.speaking:
            self._push_pre(pcm)

        self._vad.accept_waveform(pcm)

        just_started = False
        if not self.speaking and self._vad.is_speech_detected():
            self.speaking = True
            just_started = True
            pre = self._take_pre()            # 含当前 pcm，补上 VAD 判定延迟吞掉的起始音
            self._cur = [pre]
            events.append(("start", pre))

        ended = False
        while not self._vad.empty():
            self._vad.pop()                   # 该版本段样本为空，音频由 self._cur 自建
            ended = True
        if ended and self.speaking:
            self.speaking = False
            seg = np.concatenate(self._cur) if self._cur else np.zeros(0, dtype=np.float32)
            self._cur = []
            self._pre = []
            self._pre_len = 0
            events.append(("end", seg))

        if self.speaking and not just_started:
            self._cur.append(pcm)

        return events


class EnergyVAD:
    """纯能量 VAD（Silero 不可用时的兜底）。push() 语义与 SileroVAD 一致。"""

    def __init__(self) -> None:
        self.buf = np.zeros(0, dtype=np.float32)
        self.noise = 0.0
        self.speaking = False
        self.speech_run = 0
        self.silence_frames = 0
        self.silence_target = max(1, round(SILENCE_MS / FRAME_MS))
        self._cur: list[np.ndarray] = []

    def reset(self) -> None:
        self.buf = np.zeros(0, dtype=np.float32)
        self.speaking = False
        self.speech_run = 0
        self.silence_frames = 0
        self._cur = []

    def push(self, pcm: np.ndarray) -> list[tuple[str, np.ndarray]]:
        events: list[tuple[str, np.ndarray]] = []
        if pcm is None or len(pcm) == 0:
            return events
        self.buf = np.concatenate([self.buf, np.asarray(pcm, dtype=np.float32)])

        while len(self.buf) >= FRAME:
            frame = self.buf[:FRAME]
            self.buf = self.buf[FRAME:]
            rms = float(np.sqrt(np.mean(np.square(frame))))
            if not self.speaking:
                self.noise = rms if self.noise <= 0 else 0.95 * self.noise + 0.05 * rms
            threshold = max(self.noise * ENERGY_MULT, MIN_RMS)

            if rms > threshold:
                self.speech_run += 1
                self.silence_frames = 0
                if not self.speaking and self.speech_run >= START_FRAMES:
                    self.speaking = True
                    self._cur = [frame]
                    events.append(("start", frame))
                    continue
            else:
                self.speech_run = 0
                if self.speaking:
                    self.silence_frames += 1
                    if self.silence_frames >= self.silence_target:
                        self.speaking = False
                        self.silence_frames = 0
                        seg = np.concatenate(self._cur) if self._cur else np.zeros(0, dtype=np.float32)
                        self._cur = []
                        events.append(("end", seg))
                        continue
            if self.speaking:
                self._cur.append(frame)
        return events


def create_vad():
    """按可用性返回 Silero VAD 或能量 VAD。"""
    return SileroVAD() if _vad_silero is not None else EnergyVAD()


def health() -> dict:
    return {
        "backend": BACKEND,
        "silence_ms": SILENCE_MS,
        "model": str(_SILERO_MODEL) if _vad_silero is not None else None,
    }

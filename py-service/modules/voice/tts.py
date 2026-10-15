# -*- coding: utf-8 -*-
"""voice 模块 TTS 子路由：服务端 Edge 神经网络语音合成。

为什么放服务端：
    微软 Edge readaloud 端点的 DRM 已从 v1（日期串 + SHA256）升级为 v2
    （Windows 纪元 ticks + 自定义 CS64 摘要 + `2-` 版本前缀）。前端 JS 自行
    实现极易随服务端改动静默 403，进而回退到系统本地音色（听感非常机械）。
    改用官方 edge-tts 客户端（随上游同步 DRM 实现），可长期稳定出声。

接口（前端经 Vite /voiceapi 代理 → /api/voice）：
    GET  /api/voice/voices  可用音色清单（供前端渲染下拉）
    POST /api/voice/tts     合成 mp3，直接返回 audio/mpeg 字节流
"""
from __future__ import annotations

import asyncio
import logging

from fastapi import APIRouter, Body, HTTPException
from fastapi.responses import Response

logger = logging.getLogger("voice")

router = APIRouter()

try:  # 官方客户端缺失时接口降级为 503，不影响其它功能
    import edge_tts

    EDGE_TTS_OK = True
except Exception:  # noqa: BLE001
    edge_tts = None
    EDGE_TTS_OK = False

# 已实测可用的中文音色；rate/pitch 为逐音色微调，用于削弱机械感
# （免费端点会忽略 mstts:express-as 情感标签，故靠语速/音高塑造语气差异）
TTS_VOICES: dict[str, dict] = {
    "zh-CN-XiaoxiaoNeural": {
        "name": "晓晓 · 温柔女声", "gender": "女", "locale": "普通话",
        "desc": "温暖亲切，适合日常对话", "rate": "+6%", "pitch": "+0Hz",
    },
    "zh-CN-XiaoyiNeural": {
        "name": "晓伊 · 元气女声", "gender": "女", "locale": "普通话",
        "desc": "活泼上扬，适合俏皮回复", "rate": "+10%", "pitch": "+3Hz",
    },
    "zh-CN-YunxiNeural": {
        "name": "云希 · 阳光男声", "gender": "男", "locale": "普通话",
        "desc": "清爽自然，通用男声", "rate": "+6%", "pitch": "+0Hz",
    },
    "zh-CN-YunjianNeural": {
        "name": "云健 · 沉稳男声", "gender": "男", "locale": "普通话",
        "desc": "低沉厚实，适合严肃叙述", "rate": "+2%", "pitch": "-3Hz",
    },
    "zh-CN-YunxiaNeural": {
        "name": "云夏 · 磁性男声", "gender": "男", "locale": "普通话",
        "desc": "磁性偏暖，适合科普讲解", "rate": "+6%", "pitch": "-2Hz",
    },
    "zh-CN-YunyangNeural": {
        "name": "云扬 · 播音男声", "gender": "男", "locale": "普通话",
        "desc": "新闻播音腔，字正腔圆最自然", "rate": "+4%", "pitch": "+0Hz",
    },
    "zh-CN-liaoning-XiaobeiNeural": {
        "name": "小北 · 东北话女声", "gender": "女", "locale": "东北方言",
        "desc": "幽默接地气", "rate": "+6%", "pitch": "+0Hz",
    },
    "zh-CN-shaanxi-XiaoniNeural": {
        "name": "小妮 · 陕西话女声", "gender": "女", "locale": "陕西方言",
        "desc": "明亮爽利", "rate": "+6%", "pitch": "+0Hz",
    },
    "zh-HK-HiuMaanNeural": {
        "name": "曉曼 · 粤语女声", "gender": "女", "locale": "粤语",
        "desc": "标准粤语", "rate": "+4%", "pitch": "+0Hz",
    },
    "zh-TW-HsiaoChenNeural": {
        "name": "曉臻 · 台湾腔女声", "gender": "女", "locale": "台湾国语",
        "desc": "柔和台湾腔", "rate": "+4%", "pitch": "+0Hz",
    },
}

DEFAULT_VOICE = "zh-CN-XiaoxiaoNeural"
MAX_CHARS = 600                      # 单次合成上限，防止被当作无限 TTS 代理滥用
MAX_CONCURRENCY = 4                  # 同时合成路数，避免打满上游与 CPU
_sem = asyncio.Semaphore(MAX_CONCURRENCY)


def _percent(value: object, default: str, lo: int, hi: int) -> str:
    """把 '+10%' / '10' / 10 归一化为带符号百分数字符串，并夹紧到 [lo, hi]。"""
    try:
        raw = str(value).strip().rstrip("%")
        num = int(float(raw)) if raw else int(default.rstrip("%").lstrip("+"))
    except (TypeError, ValueError):
        num = int(default.rstrip("%").lstrip("+"))
    num = max(lo, min(hi, num))
    return f"+{num}%" if num >= 0 else f"{num}%"


def _hz(value: object, default: str, lo: int, hi: int) -> str:
    """把 '+3Hz' / '3' / 3 归一化为带符号赫兹字符串（SSML prosody 的 pitch 单位）。"""
    try:
        raw = str(value).strip().rstrip("Hzhz")
        num = int(float(raw)) if raw else int(default.rstrip("Hzhz").lstrip("+"))
    except (TypeError, ValueError):
        num = int(default.rstrip("Hzhz").lstrip("+"))
    num = max(lo, min(hi, num))
    return f"+{num}Hz" if num >= 0 else f"{num}Hz"


@router.get("/voices")
async def list_voices() -> dict:
    """可用音色清单（前端下拉展示用，不依赖网络）。"""
    return {
        "ok": EDGE_TTS_OK,
        "default": DEFAULT_VOICE,
        "voices": [
            {"uri": uri, **{k: v for k, v in meta.items() if k not in ("rate", "pitch")}}
            for uri, meta in TTS_VOICES.items()
        ],
    }


@router.post("/tts")
async def synthesize(payload: dict = Body(...)) -> Response:
    """合成语音，直接回 mp3 字节流（前端 new Audio(URL.createObjectURL(blob)) 播放）。"""
    if not EDGE_TTS_OK:
        raise HTTPException(status_code=503, detail="edge-tts 未安装，语音合成不可用")

    text = str(payload.get("text") or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="text 不能为空")
    if len(text) > MAX_CHARS:
        text = text[:MAX_CHARS]

    voice = str(payload.get("voice") or DEFAULT_VOICE)
    meta = TTS_VOICES.get(voice)
    if meta is None:
        raise HTTPException(status_code=400, detail=f"不支持的音色: {voice}")

    rate = _percent(payload.get("rate"), meta["rate"], -50, 100)
    pitch = _hz(payload.get("pitch"), meta["pitch"], -50, 50)

    buf = bytearray()
    try:
        async with _sem:
            comm = edge_tts.Communicate(text, voice, rate=rate, pitch=pitch)
            async for chunk in comm.stream():
                if chunk.get("type") == "audio" and chunk.get("data"):
                    buf.extend(chunk["data"])
    except Exception as exc:  # noqa: BLE001
        logger.warning("TTS 合成失败 voice=%s len=%d: %s", voice, len(text), exc)
        raise HTTPException(status_code=502, detail=f"语音合成失败: {exc}") from exc

    if not buf:
        raise HTTPException(status_code=502, detail="语音合成结果为空")

    return Response(
        content=bytes(buf),
        media_type="audio/mpeg",
        headers={"Cache-Control": "no-store", "X-Voice": voice},
    )

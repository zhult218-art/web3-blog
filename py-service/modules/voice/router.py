# -*- coding: utf-8 -*-
"""voice 模块路由：全双工语音编排 WebSocket。

前端 AudioWorklet 采集 16kHz Int16 PCM 经二进制帧上行：
  - 服务端 VAD 判定「开口」（下发 interrupt 打断 TTS）与「静音说完了」（下发 endpoint）；
  - 服务端流式 ASR（sherpa-onnx streaming paraformer）边说边出字幕（asr_partial），
    端点处给最终文本（asr），由前端统一决策（本地命令 / 导航 / 交给 LLM）。

LLM 流式生成按句切分下发 sentence，由前端 Edge TTS 逐句播放。
服务端 ASR 不可用时退回前端 Web Speech（CT_TEXT）路径，功能不中断。
"""
from __future__ import annotations

import asyncio
import json
import logging

import numpy as np
from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from . import asr, guide, llm, tts
from .protocol import (
    CT_CONTROL,
    CT_PTT,
    CT_START,
    CT_TEXT,
    EV_ASR,
    EV_ASR_PARTIAL,
    EV_DONE,
    EV_ENDPOINT,
    EV_ERROR,
    EV_GUIDE,
    EV_INTERRUPT,
    EV_READY,
    EV_SENTENCE,
    EV_TEXT,
)
from .vad import create_vad

logger = logging.getLogger("voice")

router = APIRouter(prefix="/api/voice", tags=["voice"])

# TTS 子路由：/api/voice/voices、/api/voice/tts（服务端 Edge 合成，见 tts.py）
router.include_router(tts.router)

SENT_DELIM = set(",，。！？；.!?;\n")
GUIDE_MARK = "[[GUIDE]]"   # 引导块标记：只用于路由跳转，绝不能朗读（含其中的 JSON）
MAX_HISTORY_TURNS = 8
ASR_MIN_INTERVAL = 0.12     # 两次部分解码最小间隔（秒），避免高频解码占满 CPU
ASR_MIN_SAMPLES = 3200      # 攒够 200ms 才跑一次部分解码


def _hold_len(buf: str) -> int:
    """返回需要扣留的尾部字符数：仅当结尾是 GUIDE_MARK 的真前缀时才扣留。

    固定扣留 len(GUIDE_MARK)-1 个字符会把每句首字都往后拖，导致首句出声变慢；
    这里只在该结尾确实可能是"半个标记"（如 "[[GU"）时扣留对应长度，否则返回 0
    立即全量下发。完整标记由上层 find 处理，故最长只扣留到 len(GUIDE_MARK)-1。
    """
    max_k = min(len(GUIDE_MARK) - 1, len(buf))
    for k in range(max_k, 0, -1):
        if GUIDE_MARK.startswith(buf[-k:]):
            return k
    return 0


@router.websocket("/ws")
async def voice_ws(websocket: WebSocket) -> None:
    await websocket.accept()
    send_lock = asyncio.Lock()

    async def send(obj: dict) -> None:
        async with send_lock:
            await websocket.send_json(obj)

    # 首帧必须立即下发，否则 Vite/网关代理会缓冲响应头，前端一直 CONNECTING
    await send({"type": EV_READY, "backend": llm.current_backend(), "asr": asr.available()})

    vad = create_vad()
    history: list[dict] = []
    active_task: asyncio.Task | None = None

    use_asr = asr.available()
    a_stream: asr.Stream | None = None      # 当前轮的流式识别会话
    a_partial = ""                          # 已下发的部分文本
    a_pending = 0                           # 上次解码后新增样本数
    a_last_at = 0.0                         # 上次解码时间戳
    ptt = False                             # 按住说话模式（绕过 VAD 自动端点）
    ptt_pcm: list[np.ndarray] = []          # 按住说话期间累积的 PCM（松手后整体识别）

    def new_asr_stream() -> None:
        nonlocal a_stream, a_partial, a_pending, a_last_at
        a_stream = asr.Stream() if use_asr else None
        a_partial = ""
        a_pending = 0
        a_last_at = 0.0

    async def cancel_active() -> None:
        nonlocal active_task
        if active_task and not active_task.done():
            active_task.cancel()
            try:
                await active_task
            except (asyncio.CancelledError, Exception):  # noqa: BLE001
                pass
        active_task = None

    async def accept_pcm(pcm: np.ndarray) -> None:
        """把 PCM 喂给流式识别器，并在合适的时机下发实时字幕。"""
        nonlocal a_pending, a_last_at
        if a_stream is None:
            return
        a_stream.accept(pcm)
        a_pending += pcm.size
        now = asyncio.get_event_loop().time()
        if a_pending < ASR_MIN_SAMPLES or (now - a_last_at) < ASR_MIN_INTERVAL:
            return
        a_pending = 0
        a_last_at = now
        text = await asyncio.to_thread(a_stream.decode)
        await emit_partial(text)

    async def emit_partial(text: str) -> None:
        nonlocal a_partial
        if text and text != a_partial:
            a_partial = text
            await send({"type": EV_ASR_PARTIAL, "text": text})

    async def finalize_asr(seg: np.ndarray | None = None) -> str:
        """端点/松手：收尾流式解码并下发最终文本。

        增量流自开口起（含预滚）已连续喂入同一段音频，finish() 会 flush 尾帧，
        因此结果与整段识别等价且几乎零延迟；若流结果为空再用完整段兜底重识别。
        """
        if a_stream is None:
            return ""
        text = (await asyncio.to_thread(a_stream.finish) or "").strip()
        if not text and seg is not None and len(seg) > 0:
            fallback = asr.Stream()
            if fallback.ok:
                fallback.accept(seg)
                text = (await asyncio.to_thread(fallback.finish) or "").strip()
        if text:
            await send({"type": EV_ASR, "text": text})
        return text

    async def handle_turn(text: str) -> None:
        text = (text or "").strip()
        if not text:
            await send({"type": EV_DONE})
            return
        # 1) 静态导航意图：确定性引导，直接给提示，不调用 LLM
        g = guide.resolve_guide(text)
        if g:
            await send({"type": EV_GUIDE, **g})
            tip = g.get("tip") or "好的"
            await send({"type": EV_SENTENCE, "text": tip})
            history.append({"role": "user", "content": text})
            history.append({"role": "assistant", "content": tip})
            await send({"type": EV_DONE})
            return

        history.append({"role": "user", "content": text})
        cancelled = False
        buf = ""
        full = ""
        pending = ""        # 扣留的尾巴：防止 [[GUIDE]] 的半个标记被提前下发/朗读
        guide_tail = None   # 一旦出现引导块，其后内容只暂存、绝不朗读

        async def flush_sentences(final: bool = False) -> None:
            """按句切分并下发 sentence（TTS 队列）。"""
            nonlocal buf
            while True:
                pos = -1
                for i, ch in enumerate(buf):
                    if ch in SENT_DELIM:
                        pos = i
                        break
                if pos == -1:
                    break
                sentence = buf[: pos + 1].strip()
                buf = buf[pos + 1:]
                if sentence:
                    await send({"type": EV_SENTENCE, "text": sentence})
            if final and buf.strip():
                sentence = buf.strip()
                buf = ""
                await send({"type": EV_SENTENCE, "text": sentence})

        async def emit(chunk: str) -> None:
            """下发正文增量并切句。引导块绝不经过这里。"""
            nonlocal buf
            if not chunk:
                return
            await send({"type": EV_TEXT, "text": chunk})
            buf += chunk
            await flush_sentences()

        try:
            async for delta in llm.stream_reply(history[-MAX_HISTORY_TURNS:]):
                full += delta
                if guide_tail is not None:
                    # 引导块之后的内容：只累积，不进正文也不朗读
                    guide_tail += delta
                    continue
                pending += delta
                idx = pending.find(GUIDE_MARK)
                if idx != -1:
                    # 检测到引导块：正文到此为止
                    head = pending[:idx]
                    guide_tail = pending[idx:]
                    pending = ""
                    await emit(head)
                    continue
                # 尚未出现标记：只扣留可能构成半个标记的尾部，其余立即下发（保证首句出声够快）
                keep = _hold_len(pending)
                if len(pending) > keep:
                    head = pending[: len(pending) - keep]
                    pending = pending[len(pending) - keep:]
                    await emit(head)
            if pending:
                await emit(pending)
                pending = ""
            await flush_sentences(final=True)
            clean, g2 = guide.extract_llm_guide(full)
            if g2:
                await send({"type": EV_GUIDE, **g2})
            history.append({"role": "assistant", "content": clean or full})
        except asyncio.CancelledError:
            cancelled = True
            raise
        except Exception as e:  # noqa: BLE001
            logger.warning("voice LLM 失败: %s", e)
            await send({"type": EV_ERROR, "message": str(e)[:300]})
        finally:
            if not cancelled:
                try:
                    await send({"type": EV_DONE})
                except Exception:  # noqa: BLE001
                    pass

    new_asr_stream()

    try:
        while True:
            message = await websocket.receive()

            if "bytes" in message and message["bytes"] is not None:
                pcm16 = np.frombuffer(message["bytes"], dtype=np.int16)
                if pcm16.size == 0:
                    continue
                pcm = pcm16.astype(np.float32) / 32768.0

                # 按住说话：不看 VAD，持续喂识别器直到松手
                if ptt:
                    ptt_pcm.append(pcm)
                    await accept_pcm(pcm)
                    continue

                for kind, samples in vad.push(pcm):
                    if kind == "start":
                        # 用户开口 → 打断（取消在途生成 + 通知前端立即静音）
                        await cancel_active()
                        new_asr_stream()
                        if use_asr:
                            # 预滚缓冲先喂入，补上 VAD 判定延迟吞掉的起始音
                            await accept_pcm(samples)
                        await send({"type": EV_INTERRUPT})
                    elif kind == "end":
                        if use_asr:
                            await finalize_asr(samples)
                        new_asr_stream()
                        await send({"type": EV_ENDPOINT})

                # 已在说话的后续分片继续喂增量流（start 那次已随预滚喂过）
                if use_asr and a_stream is not None and vad.speaking:
                    await accept_pcm(pcm)

            elif "text" in message and message["text"]:
                try:
                    data = json.loads(message["text"])
                except Exception:  # noqa: BLE001
                    continue
                m_type = data.get("type")
                if m_type == CT_START:
                    await cancel_active()
                    vad.reset()
                    ptt = False
                    ptt_pcm = []
                    new_asr_stream()
                elif m_type == CT_PTT:
                    action = data.get("action")
                    if action == "start":
                        await cancel_active()
                        vad.reset()
                        ptt = True
                        ptt_pcm = []
                        new_asr_stream()
                    elif action == "end":
                        ptt = False
                        seg = np.concatenate(ptt_pcm) if ptt_pcm else None
                        ptt_pcm = []
                        if use_asr:
                            await finalize_asr(seg)
                        vad.reset()
                        new_asr_stream()
                        await send({"type": EV_ENDPOINT})
                elif m_type == CT_TEXT:
                    user_text = str(data.get("text") or "")
                    if user_text.strip():
                        await cancel_active()
                        vad.reset()
                        ptt = False
                        new_asr_stream()
                        active_task = asyncio.create_task(handle_turn(user_text))
                elif m_type == CT_CONTROL and data.get("action") == "stop":
                    await cancel_active()

    except WebSocketDisconnect:
        pass
    except RuntimeError:
        pass
    except Exception as e:  # noqa: BLE001
        logger.warning("voice WS 异常: %s", e)
    finally:
        await cancel_active()
        try:
            await websocket.close()
        except Exception:  # noqa: BLE001
            pass

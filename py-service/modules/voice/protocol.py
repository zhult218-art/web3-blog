# -*- coding: utf-8 -*-
"""voice 模块协议常量：WS 上下行事件类型（前端 useVoiceStream 对齐）。"""
from __future__ import annotations

# 服务端 → 前端
EV_READY = "ready"          # 连接建立（必须首帧下发，避免代理缓冲）
EV_ASR = "asr"              # 本轮最终识别文本（服务端流式 ASR 端点处下发）
EV_ASR_PARTIAL = "asr_partial"  # 边说边出的实时字幕（增量、可覆盖）
EV_TEXT = "text"            # LLM 增量文本
EV_SENTENCE = "sentence"    # 按句切分后的播报文本（前端 Edge TTS 逐句播放）
EV_GUIDE = "guide"          # 页面引导：{route, tip, selector?}
EV_INTERRUPT = "interrupt"  # VAD 检测到用户开口 → 前端立即静音（打断）
EV_ENDPOINT = "endpoint"    # VAD 静音到达阈值 → 该轮说话结束
EV_DONE = "done"            # 本轮回复结束
EV_ERROR = "error"          # 异常：{message}

# 前端 → 服务端
CT_START = "start"          # 重置当前轮
CT_TEXT = "text"            # {type:'text', text} 用户文本（前端本地命令 / Web Speech 兜底）
CT_PTT = "ptt"              # {type:'ptt', action:'start'|'end'} 按住说话（绕过 VAD 自动端点）
CT_CONTROL = "control"      # {type:'control', action:'stop'} 取消当前生成

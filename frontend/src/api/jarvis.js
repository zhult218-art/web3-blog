// ============================================================
// 语音助手 Jarvis 接口（jarvis-service，走网关 /api）
// ============================================================
import request from './request'

// 获取可用的语音指令列表
export function getVoiceCommands() {
  return request.get('/jarvis/commands')
}

// 语音识别：将语音转出的文本交由 Jarvis 解析为操作指令
export function recognizeVoice(text, sessionId) {
  return request.post('/jarvis/recognize', { text, sessionId })
}

// 创建新的语音会话
export function createVoiceSession() {
  return request.post('/jarvis/session')
}

// 查询语音会话状态
export function getVoiceSession(sessionId) {
  return request.get('/jarvis/session/' + sessionId)
}
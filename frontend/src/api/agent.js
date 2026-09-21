// ============================================================
// Agent 聊天接口（ai-proxy-service 的 /agent/chat，走网关 /api）
// 前端无需携带 sk- 令牌；后端以应用级系统令牌代表 Agent 调用并计费。
// 返回 OpenAI SSE 流：data: {choices:[{delta:{content:"..."}}]}
// ============================================================

const AGENT_PATH = '/api/agent/chat'
const FLOW_PATH = '/api/agent/workflow'

/**
 * 发起 Agent 流式对话。
 * @param {Array} messages [{role:'user'|'assistant', content}]
 * @returns {ReadableStream<Uint8Array>} fetch 的 response body 流
 */
export function agentChatStream(messages) {
  return fetch(AGENT_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ messages }),
    credentials: 'include',
  })
}

/**
 * 解析 SSE 流并逐段回调增量文本。
 * @param {Response} res fetch 响应
 * @param {(text:string)=>void} onDelta 增量文本回调
 * @param {(done:boolean)=>void} [onDone] 流结束/出错通知
 * @returns {Promise<void>}
 */
export async function consumeAgentStream(res, onDelta, onDone) {
  if (!res.ok || !res.body) {
    let text = ''
    try { text = await res.text() } catch {}
    onDone?.(true)
    throw new Error(text || `Agent 请求失败 (${res.status})`)
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  try {
    for (;;) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, idx).trim()
        buffer = buffer.slice(idx + 1)
        if (!line.startsWith('data:')) continue
        const payload = line.slice(5).trim()
        if (payload === '[DONE]') { onDone?.(true); return }
        try {
          const json = JSON.parse(payload)
          const delta = json?.choices?.[0]?.delta?.content
          if (delta) onDelta(delta)
        } catch { /* 忽略非 JSON 行 */ }
      }
    }
  } finally {
    reader.releaseLock?.()
  }
  onDone?.(true)
}

// ═══════════════ 技能工作流（Workflow） ═══════════════

/**
 * 拉取技能列表（chart / table / word / image …）。
 * @returns {Promise<Array>} [{slug,name,description,icon,color,sample,outputFormat,stepCount}]
 */
export async function agentWorkflows() {
  const res = await fetch('/api/agent/workflows', { credentials: 'include' })
  if (!res.ok) throw new Error(`技能列表请求失败 (${res.status})`)
  return res.json()
}

/**
 * 关键词匹配技能。
 * @param {string} message
 * @returns {Promise<{matched:boolean, workflow?:object}>}
 */
export async function agentMatchWorkflow(message) {
  const res = await fetch(`${FLOW_PATH}/match`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
    credentials: 'include',
  })
  if (!res.ok) throw new Error(`技能匹配失败 (${res.status})`)
  return res.json()
}

/**
 * 执行技能（SSE）。
 * @param {string} slug
 * @param {object} answers {步骤id: 回答}
 * @returns {Response}
 */
export function agentExecuteFlow(slug, answers) {
  return fetch(`${FLOW_PATH}/execute`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ slug, answers }),
    credentials: 'include',
  })
}

/**
 * 解析技能执行 SSE 流。事件信封：
 *   data: {"type":"state"|"text"|"artifact"|"meta"|"error", ...}
 *   data: [DONE]
 * @param {Response} res
 * @param {{onText?, onArtifact?, onDone?, onError?}} handlers
 */
export async function consumeAgentFlowEvents(res, { onText, onArtifact, onDone, onError }) {
  if (!res.ok || !res.body) {
    let text = ''
    try { text = await res.text() } catch {}
    onError?.(text || `技能执行失败 (${res.status})`)
    onDone?.()
    return
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  try {
    for (;;) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, idx).trim()
        buffer = buffer.slice(idx + 1)
        if (!line.startsWith('data:')) continue
        const payload = line.slice(5).trim()
        if (payload === '[DONE]') { onDone?.(); return }
        try {
          const evt = JSON.parse(payload)
          if (evt.type === 'text') onText?.(evt.content || '')
          else if (evt.type === 'artifact') onArtifact?.(evt)
          else if (evt.type === 'error') onError?.(evt.content || '生成失败')
        } catch { /* 忽略非 JSON 行 */ }
      }
    }
  } finally {
    reader.releaseLock?.()
  }
  onDone?.()
}

// 全站免费 AI 通用接口（py-service ai 模块，经 Vite /pyai 代理 → 9006 /api/ai）
// 上游：pollinations 免费 OpenAI 兼容端点（配置见 py-service ai/service.py 三级合并）。
// 供博客摘要/问答、社区助手、后台解读、AI 搜索、音乐/相册 AI 等功能共用。
const BASE = '/pyai'

/**
 * 一次性对话（非流式）
 * @param {Array<{role:string,content:string}>} messages
 * @param {object} [opts] { system, temperature, max_tokens, model }
 * @returns {Promise<string>} AI 完整文本
 */
export async function aiChat(messages, opts = {}) {
  const res = await fetch(BASE + '/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ messages, ...opts }),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok || data?.code !== 0) {
    throw new Error(data?.msg || `AI 请求失败(${res.status})`)
  }
  return data?.data?.text || ''
}

/**
 * 流式对话（SSE）
 * @param {Array<{role:string,content:string}>} messages
 * @param {(delta:string)=>void} onDelta 增量回调
 * @param {object} [opts] { system, temperature, max_tokens, model, signal }
 * @returns {Promise<string>} 累积全文
 */
export async function aiChatStream(messages, onDelta, opts = {}) {
  const { signal, ...rest } = opts
  const res = await fetch(BASE + '/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ messages, ...rest }),
    signal,
  })
  if (!res.ok || !res.body) throw new Error(`AI 流式请求失败(${res.status})`)
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buf = ''
  let full = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    const lines = buf.split('\n')
    buf = lines.pop() || ''
    for (const line of lines) {
      const s = line.trim()
      if (!s.startsWith('data:')) continue
      const payload = s.slice(5).trim()
      if (!payload || payload === '[DONE]') continue
      try {
        const delta = JSON.parse(payload)?.delta || ''
        if (delta) {
          full += delta
          onDelta && onDelta(delta)
        }
      } catch {
        /* 忽略无法解析的行 */
      }
    }
  }
  return full
}

/** 健康检查（含上游信息，便于诊断） */
export async function aiHealth() {
  const res = await fetch(BASE + '/health')
  return res.json().catch(() => ({}))
}

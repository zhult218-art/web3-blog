// Edge TTS（微软 Edge 在线神经网络语音，WebSocket 直连，纯前端无后端依赖）
// 失败时返回 false，由调用方回退到浏览器 speechSynthesis
//
// 2026-09-23 修订（参考 edge-tts-universal / Azure SSML 官方文档）：
//   1. SSML 增加 xmlns:mstts 命名空间，启用 mstts:express-as 情感样式与 mstts:silence 句首静默；
//   2. prosody 参数从 pitch='+1Hz'/rate=0/volume='+10%' 改为 pitch='+2%'/rate='+10%'/volume='+0%'，
//      中文旁白甜区为 rate +10%~+15%，+1Hz 人耳不可察，volume +10% 易爆音；
//   3. 长文本按句号/问号/感叹号拆分，每句单独合成后 mp3 拼接，节奏更自然；
//   4. 句间插入 <break time="300ms"/>，并在每句句首加 mstts:silence type="Leading" 200ms；
//   5. 默认音色 zh-CN-XiaoxiaoNeural 走 friendly 情感，styledegree 0.8 避免夸张。
const TRUSTED_CLIENT_TOKEN = '6A5AA1D4EAFF4E9FB37E23D68491D6F4'
const WSS_BASE = 'wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1'

// 音色元数据：expressStyle 表示该音色支持的 mstts:express-as 主推情感
export const EDGE_VOICES = [
  { uri: 'zh-CN-XiaoxiaoNeural', name: '晓晓 · 温柔女声', gender: '女', style: 'friendly' },
  { uri: 'zh-CN-XiaoyiNeural',  name: '晓伊 · 元气女声', gender: '女', style: 'cheerful' },
  { uri: 'zh-CN-YunxiNeural',   name: '云希 · 阳光男声', gender: '男', style: 'chat' },
  { uri: 'zh-CN-YunjianNeural', name: '云健 · 沉稳男声', gender: '男', style: 'narration-relaxed' },
  { uri: 'zh-CN-YunxiaNeural',  name: '云夏 · 磁性男声', gender: '男', style: 'chat' },
]

let currentAudio = null
let currentAbortController = null

export function isEdgeTtsSupported() {
  return typeof WebSocket !== 'undefined' && typeof crypto?.subtle?.digest === 'function' && typeof Audio !== 'undefined'
}

async function makeSecMsGec() {
  const now = new Date()
  const mod = now.getUTCMinutes() % 5
  if (mod) now.setUTCMinutes(now.getUTCMinutes() - mod)
  now.setUTCSeconds(0)
  now.setUTCMilliseconds(0)
  const dateStr = now.toUTCString()
  const data = new TextEncoder().encode(TRUSTED_CLIENT_TOKEN + dateStr)
  const buf = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('')
}

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16)
  })
}

function isoNow() {
  return new Date().toISOString()
}

export function stopEdgeSpeech() {
  if (currentAbortController) { try { currentAbortController.abort() } catch { /* ignore */ } currentAbortController = null }
  if (currentAudio) {
    try { currentAudio.pause(); currentAudio.src = '' } catch { /* ignore */ }
    currentAudio = null
  }
}

function escapeXml(text) {
  return text.replace(/[&<>]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]))
}

// 按句末标点（。！？!?…）拆分，保留标点。空句跳过。
function splitSentences(text) {
  const parts = String(text || '').split(/(?<=[。！？!?…])\s*/)
  return parts.map(s => s.trim()).filter(Boolean)
}

// 为单个句子构造 SSML。注入 mstts:silence 句首 200ms + 句末 break 300ms。
function buildSentenceSsml(sentence, voiceURI, style) {
  const safe = escapeXml(sentence)
  const styleBlock = style
    ? `<mstts:express-as style="${style}" styledegree="0.8">${safe}</mstts:express-as>`
    : safe
  return `<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xmlns:mstts='https://www.w3.org/2001/mstts' xml:lang='zh-CN'><voice name='${voiceURI}'><mstts:silence type='Leading' value='200ms'/><prosody rate='+10%' pitch='+2%' volume='+0%'>${styleBlock}<break time='300ms'/></prosody></voice></speak>`
}

function findHeaderEnd(bytes) {
  for (let i = 0; i + 3 < bytes.length; i++) {
    if (bytes[i] === 13 && bytes[i + 1] === 10 && bytes[i + 2] === 13 && bytes[i + 3] === 10) return i + 4
  }
  return bytes.length
}

// 单次 SSML 合成；返回 mp3 Blob 或 null
function synthesizeOnce(ws, ssml, signal) {
  return new Promise((resolve, reject) => {
    const audioChunks = []
    let finished = false
    const timer = setTimeout(() => {
      if (finished) return
      finished = true
      reject(new Error('tts timeout'))
    }, 15000)

    const prevOnMessage = ws.onmessage
    ws.onmessage = (e) => {
      const data = e.data
      if (typeof data === 'string') return
      const bytes = new Uint8Array(data)
      const headEnd = findHeaderEnd(bytes)
      const head = new TextDecoder().decode(bytes.subarray(0, headEnd))
      if (head.includes('Path:turn.end')) {
        clearTimeout(timer)
        finished = true
        ws.onmessage = prevOnMessage
        if (!audioChunks.length) { resolve(null); return }
        resolve(new Blob(audioChunks, { type: 'audio/mpeg' }))
        return
      }
      if (head.includes('Path:audio')) {
        const payload = bytes.subarray(headEnd)
        if (payload.length) audioChunks.push(payload.slice())
      }
    }

    const prevOnError = ws.onerror
    ws.onerror = (err) => {
      if (finished) return
      clearTimeout(timer)
      finished = true
      ws.onmessage = prevOnMessage
      ws.onerror = prevOnError
      reject(new Error('tts ws error'))
    }

    try {
      ws.send(`X-RequestId:${uuid()}\r\nContent-Type:application/ssml+xml\r\nX-Timestamp:${isoNow()}\r\nPath:ssml\r\n\r\n${ssml}`)
    } catch (e) {
      clearTimeout(timer)
      finished = true
      ws.onmessage = prevOnMessage
      ws.onerror = prevOnError
      reject(new Error('ws send failed'))
    }
  })
}

/**
 * Edge TTS 合成并播放。
 * @param {string} text 待播报文本
 * @param {string} voiceURI 音色 URI，默认 zh-CN-XiaoxiaoNeural
 * @param {number} _ratePercent 保留参数（已废弃，由 SSML 内 +10% 控制）
 * @returns {Promise<boolean>} 是否成功播放
 */
export async function speakEdge(text, voiceURI = 'zh-CN-XiaoxiaoNeural', _ratePercent = 0) {
  if (!text) return false
  const voice = EDGE_VOICES.find(v => v.uri === voiceURI) || EDGE_VOICES[0]
  const style = voice?.style || 'friendly'
  const sentences = splitSentences(text)
  if (!sentences.length) return false

  const token = await makeSecMsGec()
  const connId = uuid()
  const wsUrl = `${WSS_BASE}?TrustedClientToken=${TRUSTED_CLIENT_TOKEN}&Sec-MS-GEC=${token}&Sec-MS-GEC-Version=1-130.0.2849.68&ConnectionId=${connId}`
  const ws = new WebSocket(wsUrl)
  const ac = new AbortController()
  currentAbortController = ac

  const finishWs = () => { try { ws.close() } catch { /* ignore */ } }

  // 等待 WebSocket 打开 + 发送 speech.config
  let wsReady = false
  try {
    await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('ws open timeout')), 8000)
      ws.onopen = () => {
        clearTimeout(timer)
        const config = {
          context: {
            synthesis: {
              audio: {
                metadataoptions: { sentenceBoundaryEnabled: 'false', wordBoundaryEnabled: 'true' },
                outputFormat: 'audio-24khz-48kbitrate-mono-mp3',
              },
            },
          },
        }
        ws.send(`X-Timestamp:${isoNow()}\r\nContent-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n${JSON.stringify(config)}`)
        wsReady = true
        resolve()
      }
      ws.onerror = () => { clearTimeout(timer); reject(new Error('ws open error')) }
    })
  } catch (e) {
    finishWs()
    return false
  }

  if (!wsReady) return false

  try {
    const collectedBlobs = []
    // 按句依次合成；任一句失败立即终止整段（外层会回退本地 TTS）
    for (const s of sentences) {
      if (ac.signal.aborted) throw new Error('aborted')
      const ssml = buildSentenceSsml(s, voiceURI, style)
      const blob = await synthesizeOnce(ws, ssml, ac.signal)
      if (blob) collectedBlobs.push(blob)
    }
    finishWs()
    if (!collectedBlobs.length) return false

    // 拼接所有句子的 mp3 blob 一次性播放
    const finalBlob = new Blob(collectedBlobs, { type: 'audio/mpeg' })
    const url = URL.createObjectURL(finalBlob)
    const audio = new Audio(url)
    currentAudio = audio
    return await new Promise(resolve => {
      audio.onended = () => { URL.revokeObjectURL(url); resolve(true) }
      audio.onerror = () => { URL.revokeObjectURL(url); resolve(false) }
      audio.play().then(() => { /* started */ }).catch(() => { URL.revokeObjectURL(url); resolve(false) })
    })
  } catch (e) {
    finishWs()
    return false
  }
}

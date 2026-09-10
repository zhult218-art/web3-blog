// Edge TTS（微软 Edge 在线神经网络语音，WebSocket 直连，纯前端无后端依赖）
// 失败时返回 false，由调用方回退到浏览器 speechSynthesis
const TRUSTED_CLIENT_TOKEN = '6A5AA1D4EAFF4E9FB37E23D68491D6F4'
const WSS_BASE = 'wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1'

export const EDGE_VOICES = [
  { uri: 'zh-CN-XiaoxiaoNeural', name: '晓晓 · 温柔女声', gender: '女' },
  { uri: 'zh-CN-XiaoyiNeural', name: '晓伊 · 元气女声', gender: '女' },
  { uri: 'zh-CN-YunxiNeural', name: '云希 · 阳光男声', gender: '男' },
  { uri: 'zh-CN-YunjianNeural', name: '云健 · 沉稳男声', gender: '男' },
  { uri: 'zh-CN-YunxiaNeural', name: '云夏 · 磁性男声', gender: '男' },
]

let currentAudio = null

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
  if (currentAudio) {
    try { currentAudio.pause(); currentAudio.src = '' } catch { /* ignore */ }
    currentAudio = null
  }
}

export async function speakEdge(text, voiceURI = 'zh-CN-XiaoxiaoNeural', ratePercent = 0) {
  if (!text) return false
  const token = await makeSecMsGec()
  const connId = uuid()
  const wsUrl = `${WSS_BASE}?TrustedClientToken=${TRUSTED_CLIENT_TOKEN}&Sec-MS-GEC=${token}&Sec-MS-GEC-Version=1-130.0.2849.68&ConnectionId=${connId}`

  const ws = new WebSocket(wsUrl)
  const audioChunks = []
  let done = false

  const finish = () => { try { ws.close() } catch { /* ignore */ } }

  const p = new Promise((resolve, reject) => {
    const timer = setTimeout(() => { if (!done) { done = true; reject(new Error('tts timeout')); finish() } }, 15000)

    ws.onopen = () => {
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

      const rate = ratePercent ? ` rate='${ratePercent > 0 ? '+' : ''}${ratePercent}%'` : ''
      const ssml = `<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='zh-CN'><voice name='${voiceURI}'><prosody pitch='+1Hz'${rate} volume='+10%'>${text.replace(/[&<>]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]))}</prosody></voice></speak>`
      ws.send(`X-RequestId:${uuid()}\r\nContent-Type:application/ssml+xml\r\nX-Timestamp:${isoNow()}\r\nPath:ssml\r\n\r\n${ssml}`)
    }

    ws.binaryType = 'arraybuffer'
    ws.onmessage = (e) => {
      const data = e.data
      if (typeof data === 'string') return
      const bytes = new Uint8Array(data)
      const headEnd = findHeaderEnd(bytes)
      const head = new TextDecoder().decode(bytes.subarray(0, headEnd))
      if (head.includes('Path:turn.end')) {
        clearTimeout(timer)
        finish()
        ws.onmessage = null
        if (!audioChunks.length) { resolve(false); return }
        const blob = new Blob(audioChunks, { type: 'audio/mpeg' })
        const url = URL.createObjectURL(blob)
        if (!done) {
          done = true
          const audio = new Audio(url)
          currentAudio = audio
          const onEnd = () => { URL.revokeObjectURL(url); resolve(true); ws.onmessage = null; finish() }
          const onErr = () => { URL.revokeObjectURL(url); resolve(false); finish() }
          audio.onended = onEnd
          audio.onerror = onErr
          audio.play().then(() => { /* nothing */ }).catch(() => onErr())
        }
        return
      }
      if (head.includes('Path:audio')) {
        const payload = bytes.subarray(headEnd)
        if (payload.length) audioChunks.push(payload.slice())
      }
    }

    ws.onerror = () => {
      clearTimeout(timer)
      if (!done) { done = true; reject(new Error('tts ws error')); finish() }
    }
  })

  try {
    return await p
  } catch {
    finish()
    return false
  }
}

function findHeaderEnd(bytes) {
  for (let i = 0; i + 3 < bytes.length; i++) {
    if (bytes[i] === 13 && bytes[i + 1] === 10 && bytes[i + 2] === 13 && bytes[i + 3] === 10) return i + 4
  }
  return bytes.length
}
// Edge TTS 播报：合成下沉到 py-service（官方 edge-tts 客户端），前端只负责播放。
//
// 2026-10-15 重构（重要）：
//   原实现由前端 WebSocket 直连微软 speech.platform.bing.com 自行合成，
//   但微软已把 DRM 从 v1（日期串 + SHA256）换成 v2（Windows 纪元 ticks +
//   自定义 CS64 摘要 + `2-` 版本前缀），前端旧算法握手必定 403。
//   403 被静默吞掉后回退到浏览器 speechSynthesis（Windows 本地音色，听感
//   极度机械），这就是「星途助手说话很僵硬」的真正原因。
//   现改为 POST /voiceapi/tts → py-service 用官方客户端合成并回 mp3，
//   DRM 跟随上游 edge-tts 包升级，不会再静默失效。
//
//   同时把音色从 5 个扩到 10 个（含播音腔/东北话/陕西话/粤语/台湾腔）。
//   失败返回 false；2026-10-16 起调用方不再回退本地旧音色（避免双音/机械音），
//   仅状态栏可见提示。

const TTS_URL = '/voiceapi/tts'

// 与 py-service/modules/voice/tts.py 的 TTS_VOICES 白名单保持一致。
// defaultRate/defaultPitch 供「试听」与后端默认值对齐，仅用于提示，不参与计算。
export const EDGE_VOICES = [
  { uri: 'zh-CN-XiaoxiaoNeural', name: '晓晓 · 温柔女声', gender: '女', locale: '普通话', desc: '温暖亲切，适合日常对话' },
  { uri: 'zh-CN-XiaoyiNeural', name: '晓伊 · 元气女声', gender: '女', locale: '普通话', desc: '活泼上扬，适合俏皮回复' },
  { uri: 'zh-CN-YunxiNeural', name: '云希 · 阳光男声', gender: '男', locale: '普通话', desc: '清爽自然，通用男声' },
  { uri: 'zh-CN-YunjianNeural', name: '云健 · 沉稳男声', gender: '男', locale: '普通话', desc: '低沉厚实，适合严肃叙述' },
  { uri: 'zh-CN-YunxiaNeural', name: '云夏 · 磁性男声', gender: '男', locale: '普通话', desc: '磁性偏暖，适合科普讲解' },
  { uri: 'zh-CN-YunyangNeural', name: '云扬 · 播音男声', gender: '男', locale: '普通话', desc: '新闻播音腔，字正腔圆最自然' },
  { uri: 'zh-CN-liaoning-XiaobeiNeural', name: '小北 · 东北话女声', gender: '女', locale: '东北方言', desc: '幽默接地气' },
  { uri: 'zh-CN-shaanxi-XiaoniNeural', name: '小妮 · 陕西话女声', gender: '女', locale: '陕西方言', desc: '明亮爽利' },
  { uri: 'zh-HK-HiuMaanNeural', name: '曉曼 · 粤语女声', gender: '女', locale: '粤语', desc: '标准粤语' },
  { uri: 'zh-TW-HsiaoChenNeural', name: '曉臻 · 台湾腔女声', gender: '女', locale: '台湾国语', desc: '柔和台湾腔' },
]

export const DEFAULT_VOICE = 'zh-CN-XiaoxiaoNeural'

let currentAudio = null
let currentAbortController = null
// 流式播报的取消入口：speakEdgeStream 注册，stopEdgeSpeech 统一触发
let streamStopFn = null

export function isEdgeTtsSupported() {
  return typeof fetch === 'function' && typeof Audio !== 'undefined' && typeof URL?.createObjectURL === 'function'
}

export function stopEdgeSpeech() {
  if (streamStopFn) { try { streamStopFn() } catch { /* ignore */ } streamStopFn = null }
  if (currentAbortController) { try { currentAbortController.abort() } catch { /* ignore */ } currentAbortController = null }
  if (currentAudio) {
    try { currentAudio.pause(); currentAudio.src = '' } catch { /* ignore */ }
    currentAudio = null
  }
}

// 按句末标点（。！？!?…）拆分，保留标点。空句跳过。
function splitSentences(text) {
  const parts = String(text || '').split(/(?<=[。！？!?…])\s*/)
  return parts.map(s => s.trim()).filter(Boolean)
}

// 长文本按句聚合到 ~MAX_SEG 字一段：整段合成比逐句合成语气更连贯，
// 同时避免单次请求过大导致首音延迟过高。
const MAX_SEG = 120
function chunkText(text) {
  const sentences = splitSentences(text)
  if (!sentences.length) return []
  const segs = []
  let buf = ''
  for (const s of sentences) {
    if (buf && (buf.length + s.length) > MAX_SEG) { segs.push(buf); buf = '' }
    buf += s
  }
  if (buf) segs.push(buf)
  return segs
}

// 请求后端合成一段文本，返回 mp3 Blob；失败抛错由调用方兜底
async function synthesize(text, voiceURI, signal) {
  const res = await fetch(TTS_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, voice: voiceURI }),
    signal,
  })
  if (!res.ok) {
    let detail = ''
    try { detail = (await res.json())?.detail || '' } catch { /* ignore */ }
    throw new Error(`tts ${res.status} ${detail}`)
  }
  const blob = await res.blob()
  if (!blob.size) throw new Error('tts empty audio')
  return blob
}

/**
 * Edge TTS 流式播报：分段合成、合成完一段立刻播放，不等整段合成完。
 * 首音延迟 ≈ 单段合成耗时；播放期间可被 stopEdgeSpeech() 立即打断。
 *
 * @param {string} text 待播报文本
 * @param {string} voiceURI 音色 URI
 * @param {{onSentenceStart?:Function,onSentenceEnd?:Function,onDone?:(ok:boolean)=>void,onError?:Function}} [handlers]
 * @returns {Promise<boolean>} 是否成功播放完整
 */
export async function speakEdgeStream(text, voiceURI = DEFAULT_VOICE, handlers = {}) {
  if (!text) { try { handlers.onDone?.(false) } catch { /* ignore */ } return false }
  const segs = chunkText(text)
  if (!segs.length) { try { handlers.onDone?.(false) } catch { /* ignore */ } return false }

  const ac = new AbortController()
  currentAbortController = ac

  let stopped = false
  let doneResolved = false
  let synthDone = false
  let anyEnqueued = 0
  const queue = []
  let playing = false
  let resolveDone = null
  const doneP = new Promise(r => { resolveDone = r })
  const settle = ok => { if (!doneResolved) { doneResolved = true; resolveDone(ok) } }

  // 播放队列：按序播放，段间自然衔接
  const playNext = () => {
    if (stopped) { playing = false; settle(false); return }
    const blob = queue.shift()
    if (!blob) { playing = false; if (synthDone && !stopped) settle(true); return }
    playing = true
    const url = URL.createObjectURL(blob)
    const audio = new Audio(url)
    currentAudio = audio
    const clean = () => { try { URL.revokeObjectURL(url) } catch { /* ignore */ } if (currentAudio === audio) currentAudio = null }
    audio.onended = () => { clean(); try { handlers.onSentenceEnd?.() } catch { /* ignore */ } playNext() }
    audio.onerror = () => { clean(); playNext() }
    try { handlers.onSentenceStart?.() } catch { /* ignore */ }
    audio.play().catch(() => { clean(); playNext() })
  }
  const enqueue = blob => { if (stopped) return; anyEnqueued += 1; queue.push(blob); if (!playing) playNext() }

  streamStopFn = () => {
    stopped = true
    queue.length = 0
    try { ac.abort() } catch { /* ignore */ }
    if (currentAudio) { try { currentAudio.pause(); currentAudio.src = '' } catch { /* ignore */ } currentAudio = null }
    settle(false)
  }

  try {
    // 先并发预取第 1、2 段降低首音延迟，其余按序边合成边播
    const pending = new Map()
    const prefetch = i => {
      if (i >= segs.length || pending.has(i)) return
      pending.set(i, synthesize(segs[i], voiceURI, ac.signal).catch(() => null))
    }
    prefetch(0)
    prefetch(1)

    for (let i = 0; i < segs.length; i++) {
      if (stopped || ac.signal.aborted) break
      const blob = await pending.get(i)
      pending.delete(i)
      prefetch(i + 2)
      if (stopped) break
      if (blob) enqueue(blob)
    }
    synthDone = true

    if (stopped) { settle(false); return doneP }
    if (!anyEnqueued) { settle(false); return doneP }
    if (!playing) { settle(true); return doneP }   // 已全部播完
    return doneP                                   // 最后一段仍在播放，播完自动 settle
  } catch (e) {
    try { handlers.onError?.(e) } catch { /* ignore */ }
    settle(false)
    return doneP
  } finally {
    streamStopFn = null
  }
}

/**
 * 一次性播报（非流式语义，但内部同样分段并边合边播）。
 * 保留该导出以兼容既有调用方。
 * @returns {Promise<boolean>} 是否成功播放
 */
export async function speakEdge(text, voiceURI = DEFAULT_VOICE) {
  if (!text) return false
  return speakEdgeStream(text, voiceURI)
}

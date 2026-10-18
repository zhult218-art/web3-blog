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
// 流式播报的取消入口：createEdgeSpeaker / speakEdgeStream 注册，stopEdgeSpeech 统一触发
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

// 流式播报切分参数
const PREFETCH = 3      // 合成预取深度（段）：合成始终跑在播放之前，片段之间无「等合成」空档
const SOFT_MIN = 14     // 逗号/分号累积到该长度后才作为切分点，避免切得过碎
const HARD_MAX = 60     // 单段硬上限（字）：长时间无标点时强制切分，避免一段过大首音变慢

// 从流式文本缓冲中切出一个「可合成」片段；返回 [chunk, rest]，暂无可切时返回 null。
// 句末标点立即切；逗号/分号在累积够长后才切；超长无标点则硬切。
function takeChunk(buf, finish) {
  if (!buf) return null
  if (finish) return [buf, '']
  for (let i = 0; i < buf.length; i++) {
    if ('。！？!?…\n'.includes(buf[i])) return [buf.slice(0, i + 1), buf.slice(i + 1)]
    if ('，,；;'.includes(buf[i]) && i + 1 >= SOFT_MIN) return [buf.slice(0, i + 1), buf.slice(i + 1)]
  }
  if (buf.length >= HARD_MAX) return [buf.slice(0, HARD_MAX), buf.slice(HARD_MAX)]
  return null
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
 * 流式播报会话：持续 push(text) 投喂增量文本，边切分边合成边播放。
 * 关键点：合成始终预取 PREFETCH 段排在播放之前，片段之间没有「等合成」的空档，
 * 因此不会出现「文字已出、音频迟迟不响」或一句一停的断续感；播放期间可被
 * stopEdgeSpeech() 立即打断。同一时刻只应存在一个会话（注册在 streamStopFn 上）。
 *
 * @param {string} voiceURI 音色 URI
 * @param {{onSentenceStart?:Function,onSentenceEnd?:Function,onError?:Function}} [handlers]
 * @returns {{push:(t:string)=>void, finish:()=>Promise<boolean>, stop:()=>void, playing:boolean}}
 */
export function createEdgeSpeaker(voiceURI = DEFAULT_VOICE, handlers = {}) {
  const ac = new AbortController()
  currentAbortController = ac
  const chunks = []       // 待合成文本（按序）
  const tasks = []        // 每段合成 Promise<Blob|null>（按序，保证播放顺序不错乱）
  let textBuf = ''
  let started = 0         // 已发起合成的段数
  let consumed = 0        // 已取走播放的段数
  let finished = false
  let stopped = false
  let playing = false
  let anyEnqueued = 0
  let doneResolved = false
  let resolveDone = null
  const doneP = new Promise(r => { resolveDone = r })
  const settle = ok => { if (!doneResolved) { doneResolved = true; resolveDone(ok) } }

  const playBlob = (blob, onEnd) => {
    const url = URL.createObjectURL(blob)
    const audio = new Audio(url)
    currentAudio = audio
    const clean = () => { try { URL.revokeObjectURL(url) } catch { /* ignore */ } if (currentAudio === audio) currentAudio = null }
    audio.onended = () => { clean(); try { handlers.onSentenceEnd?.() } catch { /* ignore */ }; onEnd() }
    audio.onerror = () => { clean(); onEnd() }
    try { handlers.onSentenceStart?.() } catch { /* ignore */ }
    audio.play().catch(() => { clean(); onEnd() })
  }

  // 推进流水线：补足在途合成 → 按序取出已合成段播放 → 判定完成
  const pump = () => {
    while (!stopped && started < chunks.length && (started - consumed) < PREFETCH) {
      const idx = started
      tasks[idx] = synthesize(chunks[idx], voiceURI, ac.signal)
        .catch(e => { try { handlers.onError?.(e) } catch { /* ignore */ } return null })
      started += 1
    }
    if (!stopped && !playing && consumed < started) {
      const idx = consumed
      const p = tasks[idx]
      if (p) {
        playing = true
        p.then(blob => {
          consumed = idx + 1
          if (stopped) { playing = false; return }
          if (blob) { anyEnqueued += 1; playBlob(blob, () => { playing = false; pump() }) }
          else { playing = false; pump() }        // 该段合成失败：跳过，继续后面的
        })
      }
    }
    if (finished && !stopped && consumed >= chunks.length && !playing) settle(anyEnqueued > 0)
  }

  // 把缓冲文本切成可合成片段；finish 时把剩余文本一次性切出
  const flushBuf = finish => {
    let item = takeChunk(textBuf, finish)
    while (item) {
      textBuf = item[1]
      if (item[0]) chunks.push(item[0])
      if (finish) break
      item = takeChunk(textBuf, false)
    }
  }

  const stopAll = () => {
    if (stopped) return
    stopped = true
    textBuf = ''; chunks.length = 0; tasks.length = 0
    if (currentAbortController === ac) currentAbortController = null
    try { ac.abort() } catch { /* ignore */ }
    if (currentAudio) { try { currentAudio.pause(); currentAudio.src = '' } catch { /* ignore */ } currentAudio = null }
    settle(false)
  }
  streamStopFn = stopAll
  doneP.then(() => { if (streamStopFn === stopAll) streamStopFn = null })

  return {
    push(t) {
      if (stopped || finished) return
      textBuf += String(t || '')
      flushBuf(false)
      pump()
    },
    finish() { finished = true; flushBuf(true); pump(); return doneP },
    stop() { stopAll() },
    get playing() { return playing },
  }
}

/**
 * Edge TTS 流式播报（一次性）：内部走 createEdgeSpeaker 的流水线，
 * 分段合成、合成完一段立刻播放，不等整段合成完。
 *
 * @param {string} text 待播报文本
 * @param {string} voiceURI 音色 URI
 * @param {{onSentenceStart?:Function,onSentenceEnd?:Function,onDone?:(ok:boolean)=>void,onError?:Function}} [handlers]
 * @returns {Promise<boolean>} 是否成功播放完整
 */
export async function speakEdgeStream(text, voiceURI = DEFAULT_VOICE, handlers = {}) {
  if (!text) { try { handlers.onDone?.(false) } catch { /* ignore */ } return false }
  const sp = createEdgeSpeaker(voiceURI, handlers)
  sp.push(text)
  return sp.finish()
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

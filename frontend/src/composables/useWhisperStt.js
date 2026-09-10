// whisper 高精度语音识别（本地 faster-whisper 服务 /whisper → 127.0.0.1:9011）
// 交互模式：按住说话（push-to-talk）→ 松开 → 上传 → 转写文本

let rec = null
let chunks = []
let activeStream = null

function mediaRecorderAvailable() {
  return !!(navigator.mediaDevices?.getUserMedia && window.MediaRecorder)
}

async function getRecorder() {
  if (!mediaRecorderAvailable()) throw new Error('unsupported')
  const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
  activeStream = stream
  const mime = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'].find(t => MediaRecorder.isTypeSupported(t)) || ''
  const mr = new MediaRecorder(stream, mime ? { mimeType: mime } : undefined)
  chunks = []
  mr.ondataavailable = e => { if (e.data && e.data.size) chunks.push(e.data) }
  return mr
}

function cleanup() {
  if (activeStream) {
    activeStream.getTracks().forEach(t => t.stop())
    activeStream = null
  }
}

// 开始录音；返回停止函数，停止后 resolve 音频 blob
export function startWhisperRecord() {
  return getRecorder().then(mr => {
    rec = mr
    mr.start(250)
    let stopped = false
    const stopP = new Promise((resolve, reject) => {
      const onStop = () => {
        try {
          cleanup()
          const type = (mr.mimeType || 'audio/webm').split(';')[0]
          resolve(new Blob(chunks, { type }))
        } catch (e) { reject(e) }
      }
      mr.onstop = onStop
    })
    return {
      stop: () => {
        if (stopped) return stopP
        stopped = true
        try { rec && rec.state !== 'inactive' && rec.stop() } catch (e) { cleanup(); return Promise.reject(e) }
        return stopP
      },
      cancel: () => {
        stopped = true
        try { rec && rec.state !== 'inactive' && rec.stop() } catch (e) { /* 忽略 */ }
        cleanup()
      },
    }
  })
}

// 上传转写；lang 默认中文
export async function whisperTranscribe(blob, lang = 'zh') {
  const fd = new FormData()
  fd.append('file', blob, 'rec.webm')
  fd.append('language', lang)
  const res = await fetch('/whisper/transcribe', { method: 'POST', body: fd })
  if (!res.ok) throw new Error(`whisper ${res.status}`)
  const data = await res.json()
  return (data.text || '').trim()
}
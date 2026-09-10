// 本地语音识别引擎（vosk-browser → WASM 离线识别，中文模型）
// 麦克风 → AudioContext → ScriptProcessor（兼容性最佳）喂给 recognizer
// 注意：acceptWaveform 必须传 AudioBuffer，传裸 Float32Array 会静默失败
let modelPromise = null
let model = null
let recognizer = null
let audioCtx = null
let source = null
let processor = null
let mediaStream = null
let running = false

const MODEL_URL = '/vosk/cn.tar.gz'

export async function loadVoskModel() {
  if (model) return model
  if (modelPromise) return modelPromise
  modelPromise = (async () => {
    try {
      const Vosk = await import('vosk-browser')
      console.log('[vosk] vosk-browser module loaded')
      const loadPromise = Vosk.createModel(MODEL_URL)
      const timeoutPromise = new Promise((_, reject) =>
        setTimeout(() => reject(new Error('vosk model load timeout (30s)')), 30000)
      )
      model = await Promise.race([loadPromise, timeoutPromise])
      console.log('[vosk] model loaded')
      return model
    } catch (err) {
      console.warn('[vosk] model load failed:', err)
      modelPromise = null
      throw err
    }
  })()
  return modelPromise
}

export const callbacks = {
  onFinal: () => {},
  onPartial: () => {},
}

async function startMic() {
  mediaStream = await navigator.mediaDevices.getUserMedia({
    audio: { echoCancellation: true, noiseSuppression: true, channelCount: 1 },
  })
  audioCtx = new AudioContext()
  await audioCtx.resume()
  source = audioCtx.createMediaStreamSource(mediaStream)
  processor = audioCtx.createScriptProcessor(4096, 1, 1)
  processor.onaudioprocess = (e) => {
    if (!running || !recognizer) return
    try {
      const buf = e.inputBuffer.getChannelData(0)
      recognizer.acceptWaveform(buf)
    } catch (err) {
      console.warn('[vosk] acceptWaveform error:', err)
    }
  }
  source.connect(processor)
  processor.connect(audioCtx.destination)
}

export async function startVoskRecognition() {
  if (running) return true
  try {
    const m = await loadVoskModel()
    await startMic()
    recognizer = new m.KaldiRecognizer(audioCtx.sampleRate || 16000)
    recognizer.on('result', (msg) => {
      if (msg?.result?.text) callbacks.onFinal(msg.result.text)
    })
    recognizer.on('partialresult', (msg) => {
      if (msg?.result?.partial) callbacks.onPartial(msg.result.partial)
    })
    running = true
    console.log('[vosk] recognition started, sampleRate:', audioCtx.sampleRate)
    return true
  } catch (err) {
    console.warn('[vosk] start failed:', err)
    stopVoskRecognition()
    return false
  }
}

export function stopVoskRecognition() {
  running = false
  try { processor?.disconnect() } catch {}
  try { source?.disconnect() } catch {}
  try { audioCtx?.close() } catch {}
  try { mediaStream?.getTracks().forEach(t => t.stop()) } catch {}
  try { recognizer?.remove() } catch {}
  recognizer = null
  audioCtx = null
  processor = null
  source = null
  mediaStream = null
}

export function isVoskRunning() {
  return running
}

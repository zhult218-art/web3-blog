// 语音全双工流：麦克风 PCM（AudioWorklet 16kHz Int16）→ WebSocket → py-service
// 服务端 /api/voice/ws（经 Vite /voiceapi 代理）。服务端回传 VAD 打断 / 端点 /
// LLM 句子等事件，驱动前端 barge-in 与流式播报。
//
// 资源必须显式释放：MediaStream tracks / AudioWorkletNode / AudioContext / WebSocket
// 全部在 stop()/dispose() 中清理，避免组件卸载后残留（项目既有约定）。

const WS_PATH = '/voiceapi/ws'

let _ws = null
let _audioCtx = null
let _worklet = null
let _micStream = null
let _started = false
let _disposed = false
let _retryTimer = 0
let _backoff = 1000
let _pending = []            // WS 未就绪时挂起的 JSON 消息
let _moduleLoaded = false
let _onEvent = null
let _onStatus = null

function wsUrl() {
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${location.host}${WS_PATH}`
}

function setStatus(s) {
  try { _onStatus?.(s) } catch { /* ignore */ }
}

function connect() {
  if (_disposed) return
  if (_ws && (_ws.readyState === WebSocket.OPEN || _ws.readyState === WebSocket.CONNECTING)) return
  setStatus('connecting')
  let ws
  try { ws = new WebSocket(wsUrl()) } catch { scheduleReconnect(); return }
  _ws = ws

  ws.onopen = () => {
    _backoff = 1000
    setStatus('ready')
    // 冲掉挂起的文本消息
    const pend = _pending; _pending = []
    for (const m of pend) { try { ws.send(m) } catch { /* ignore */ } }
  }
  ws.onmessage = (e) => {
    if (typeof e.data !== 'string') return
    let msg = null
    try { msg = JSON.parse(e.data) } catch { return }
    try { _onEvent?.(msg) } catch { /* ignore */ }
  }
  ws.onclose = () => { setStatus('offline'); if (_started && !_disposed) scheduleReconnect() }
  ws.onerror = () => { setStatus('offline') }
}

function scheduleReconnect() {
  if (_disposed || !_started) return
  clearTimeout(_retryTimer)
  _retryTimer = setTimeout(() => { connect() }, _backoff)
  _backoff = Math.min(_backoff * 2, 8000)
}

async function startWorklet() {
  _micStream = await navigator.mediaDevices.getUserMedia({
    audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: false, channelCount: 1 },
  })
  _audioCtx = new (window.AudioContext || window.webkitAudioContext)()
  if (_audioCtx.state === 'suspended') { try { await _audioCtx.resume() } catch { /* ignore */ } }
  if (!_moduleLoaded) {
    await _audioCtx.audioWorklet.addModule('/pcm-processor.js')
    _moduleLoaded = true
  }
  _worklet = new AudioWorkletNode(_audioCtx, 'pcm-processor')
  _worklet.port.onmessage = (e) => {
    if (_ws && _ws.readyState === WebSocket.OPEN) { try { _ws.send(e.data) } catch { /* ignore */ } }
  }
  const source = _audioCtx.createMediaStreamSource(_micStream)
  source.connect(_worklet)
  // 接到静音增益再连 destination，保持 worklet 在音频图中处于活动状态
  const silent = _audioCtx.createGain()
  silent.gain.value = 0
  _worklet.connect(silent)
  silent.connect(_audioCtx.destination)
}

/**
 * 启动语音流。
 * @param {{onEvent?:(msg:object)=>void, onStatus?:(s:'ready'|'connecting'|'offline')=>void}} handlers
 * @returns {Promise<boolean>} 是否成功启动（麦克风/Worklet 就绪）
 */
export async function startVoiceStream({ onEvent, onStatus } = {}) {
  if (_started) return true
  _onEvent = onEvent || null
  _onStatus = onStatus || null
  _disposed = false
  try {
    await startWorklet()
  } catch (e) {
    // 启动中途失败（AudioContext/addModule 等异常）时，已分配的麦克风与音频资源必须立即释放
    stopVoiceStream()
    return false
  }
  _started = true
  connect()
  return true
}

/** 上行文本（Web Speech 最终识别结果）或控制指令 */
export function sendVoice(obj) {
  const payload = JSON.stringify(obj)
  if (_ws && _ws.readyState === WebSocket.OPEN) {
    try { _ws.send(payload) } catch { _pending.push(payload) }
  } else {
    _pending.push(payload)
    if (_started && !_disposed) connect()
  }
}

/** 停止语音流并释放全部资源（麦克风/Worklet/AudioContext/WS） */
export function stopVoiceStream() {
  _started = false
  clearTimeout(_retryTimer); _retryTimer = 0
  _backoff = 1000
  _pending = []
  if (_ws) {
    try { _ws.onclose = null; _ws.close() } catch { /* ignore */ }
    _ws = null
  }
  if (_worklet) {
    try { _worklet.port.onmessage = null; _worklet.disconnect() } catch { /* ignore */ }
    _worklet = null
  }
  if (_micStream) {
    try { _micStream.getTracks().forEach(t => t.stop()) } catch { /* ignore */ }
    _micStream = null
  }
  if (_audioCtx) {
    try { _audioCtx.close() } catch { /* ignore */ }
    _audioCtx = null
  }
  setStatus('offline')
}

/** 永久销毁（组件卸载时调用，禁止重连） */
export function disposeVoiceStream() {
  _disposed = true
  stopVoiceStream()
  _onEvent = null
  _onStatus = null
}

export function isVoiceStreamRunning() {
  return _started
}

/**
 * AudioWorklet：采集麦克风 PCM 并降采样到 16kHz Int16 单声道。
 * 直接发送原始 PCM 给后端（跳过 WebM 编码/ffmpeg 解码），供服务端 VAD 做
 * 全双工打断与静音端点检测。
 *
 * 每次攒够 800 个 16kHz 样本（=50ms）就以 Int16Array 的 buffer 零拷贝发送。
 * 移植自 AsLive-main/static/pcm-processor.js。
 */
class PCMProcessor extends AudioWorkletProcessor {
  constructor() {
    super()
    this.buffer = []
    this.srcPos = 0                 // 降采样累加器（Bresenham 式）
    this.chunkSize = 800            // 800 个 16kHz 样本 = 50ms
    this.ratio = sampleRate / 16000 // 例如 48000/16000 = 3
  }

  process(inputs) {
    const input = inputs[0]
    if (!input || input.length === 0 || !input[0]) return true

    const src = input[0] // 单声道

    for (let i = 0; i < src.length; i++) {
      // 三点移动平均做低通，避免高频折叠成噪声
      const smoothed = (src[i] + (src[i - 1] || 0) + (src[i - 2] || 0)) / 3.0
      this.srcPos += 16000
      if (this.srcPos >= sampleRate) {
        this.srcPos -= sampleRate
        this.buffer.push(smoothed)
      }
    }

    while (this.buffer.length >= this.chunkSize) {
      const chunk = this.buffer.splice(0, this.chunkSize)
      const int16 = new Int16Array(chunk.length)
      for (let j = 0; j < chunk.length; j++) {
        const s = Math.max(-1, Math.min(1, chunk[j]))
        int16[j] = s < 0 ? s * 32768 : s * 32767
      }
      this.port.postMessage(int16.buffer, [int16.buffer])
    }

    return true
  }
}

registerProcessor('pcm-processor', PCMProcessor)

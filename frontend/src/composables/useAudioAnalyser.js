// ============================================================
// useAudioAnalyser —— 全局音频频谱分析器（单例）
// 全局 Audio 元素只允许被 createMediaElementSource 一次，
// 因此详情页大柱状图与底部全局小柱图必须共享同一 AudioContext / AnalyserNode。
// 跨域音频（无 CORS）频谱会静音输出，组件需检测全零后退化为模拟流动。
// ============================================================
import { usePlayerStore } from '@/stores/modules/player'

let audioCtx = null
let analyser = null
let sourceNode = null
let freqData = null
let attached = false

function ensureGraph() {
  if (analyser) return true
  try {
    const AC = window.AudioContext || window.webkitAudioContext
    if (!AC) return false
    const player = usePlayerStore()
    const el = player.getAudioElement()
    audioCtx = new AC()
    analyser = audioCtx.createAnalyser()
    analyser.fftSize = 256 // 128 个频点，柱状图足够细腻
    analyser.smoothingTimeConstant = 0.78
    sourceNode = audioCtx.createMediaElementSource(el)
    sourceNode.connect(analyser)
    analyser.connect(audioCtx.destination) // 回连扬声器，否则无声
    freqData = new Uint8Array(analyser.frequencyBinCount)
    return true
  } catch {
    // 已被其它上下文接管或环境不支持：保持模拟模式
    audioCtx = null
    analyser = null
    sourceNode = null
    return false
  }
}

export function useAudioAnalyser() {
  // 监听首次播放（通常处于用户手势链路内，AudioContext 可直接发声）
  function attach() {
    if (attached) return
    attached = true
    const player = usePlayerStore()
    const el = player.getAudioElement()
    el.addEventListener('play', () => {
      ensureGraph()
      audioCtx?.resume?.()
    })
    if (!el.paused) {
      ensureGraph()
      audioCtx?.resume?.()
    }
  }

  // 读取频谱（0~255）；分析器未就绪时返回 null
  function getFrequency() {
    if (!analyser) return null
    analyser.getByteFrequencyData(freqData)
    return freqData
  }

  return { attach, getFrequency, get connected() { return !!analyser } }
}

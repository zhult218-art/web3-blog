<template>
  <!-- 纯黑底 + 全宽波浪画布，无任何多余 UI -->
  <div class="audio-wave" :style="{ height: height + 'px' }">
    <canvas ref="cv"></canvas>
  </div>
</template>

<script setup>
// ====================================================
// AudioWave —— Web Audio API 平滑彩色波浪可视化
// · 波形：一条平滑连续的波浪（时间域采样 + 中点二次样条），非柱状频谱
// · 颜色：左→右 蓝 → 青 → 绿 → 粉紫 渐变描边
// · 起伏：音量能量驱动幅度，层级插值让起伏柔和
// · 质感：双层描边（宽淡光晕 + 细主波浪）实现柔和发光边缘
// · 兼容：跨域音频接入 Web Audio 会静音 → 自动退化为模拟波浪
// 用法：<AudioWave :height="140" />（挂载即用，自动读取全局播放器）
// ====================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { usePlayerStore } from '@/stores/modules/player'

const props = defineProps({
  height: { type: Number, default: 140 }, // 画布高度 px
})

const cv = ref(null)
const player = usePlayerStore()

// ── Web Audio 图（模块级单例：组件卸载后音频链路保持可用）──
let audioCtx = null
let analyser = null
let freq = null          // 频域数据（取能量）
let wave = null          // 时域数据（取波形）
let hooked = false       // 是否已挂 play 事件
let connected = false    // 是否已接入 AnalyserNode
let tainted = false      // 跨域受限（频谱全 0）→ 模拟模式

let ctx = null
let raf = 0
let W = 0
let H = 0
let level = 0.06         // 平滑后的音量能量（0~1）
let t = 0                // 相位时间：暂停时也有轻微"呼吸"

// 左→右 渐变色标：蓝 → 青 → 绿 → 粉紫
const STOPS = ['#3b82f6', '#22d3ee', '#34d399', '#e879f9']

onMounted(() => {
  ctx = cv.value.getContext('2d')
  resize()
  window.addEventListener('resize', resize)
  hookElement()
  draw()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
  // 不关闭 audioCtx：全局 Audio 的发声链路必须一直存活
})

function resize() {
  const el = cv.value
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  W = el.offsetWidth
  H = el.offsetHeight
  el.width = W * dpr
  el.height = H * dpr
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

// 首次播放（用户手势链路内）把全局 Audio 接入分析器并回连扬声器
function hookElement() {
  if (hooked) return
  const el = player.getAudioElement()
  if (!el) return
  hooked = true
  el.addEventListener('play', () => {
    ensureGraph()
    audioCtx?.resume?.()
  })
}

function ensureGraph() {
  if (connected) return
  try {
    const AC = window.AudioContext || window.webkitAudioContext
    audioCtx = new AC()
    analyser = audioCtx.createAnalyser()
    analyser.fftSize = 2048
    analyser.smoothingTimeConstant = 0.82
    const src = audioCtx.createMediaElementSource(player.getAudioElement())
    src.connect(analyser)
    analyser.connect(audioCtx.destination) // 必须回连扬声器，否则无声
    freq = new Uint8Array(analyser.frequencyBinCount)
    wave = new Uint8Array(analyser.fftSize)
    connected = true
  } catch {
    connected = false // 不支持时保持模拟波浪
  }
}

// ── 渲染主循环 ──
function draw() {
  raf = requestAnimationFrame(draw)
  if (!ctx) return
  t += 0.016
  ctx.clearRect(0, 0, W, H)

  // 1) 计算目标幅度：真实能量 → 模拟呼吸；level 插值让起伏丝滑
  let target = 0.05
  if (connected && analyser) {
    analyser.getByteFrequencyData(freq)
    analyser.getByteTimeDomainData(wave)
    let sum = 0
    for (let i = 0; i < 80; i++) sum += freq[i] // 低频段最能代表"响度"
    const energy = sum / 80 / 255
    if (energy > 0.002) {
      tainted = false
      target = 0.06 + energy
    } else if (player.isPlaying) {
      tainted = true // 跨域音频：频谱全 0，退化为模拟波浪
    }
  }
  if (tainted || (!connected && player.isPlaying)) {
    target = 0.4 + 0.22 * Math.sin(t * 2.1) + 0.1 * Math.sin(t * 5.3)
  }
  level += (target - level) * 0.1

  // 2) 采样波形点：时域 24 样本均值 → 屏幕 y 坐标
  const N = 72
  const pts = []
  for (let i = 0; i <= N; i++) {
    const p = i / N
    let v
    if (connected && !tainted) {
      const start = Math.floor(p * (wave.length - 24))
      let s = 0
      for (let k = 0; k < 24; k++) s += wave[start + k] - 128
      v = s / 24 / 128
    } else {
      // 暂停/模拟：两条错相正弦叠加出自然波动
      v = Math.sin(p * 9 + t * 1.6) * 0.55 + Math.sin(p * 23 - t * 2.4) * 0.28
    }
    pts.push(H / 2 + v * Math.min(level, 1) * H * 0.42)
  }

  // 3) 中点二次样条：把离散点连成一条平滑连续的波浪
  const path = new Path2D()
  path.moveTo(0, pts[0])
  for (let i = 1; i < N; i++) {
    const xm = ((i + 0.5) / N) * W
    path.quadraticCurveTo((i / N) * W, pts[i], xm, (pts[i] + pts[i + 1]) / 2)
  }
  path.lineTo(W, pts[N])

  // 4) 渐变描边：宽淡光晕层（柔边发光）+ 细主波浪层
  const g = ctx.createLinearGradient(0, 0, W, 0)
  STOPS.forEach((c, i) => g.addColorStop(i / (STOPS.length - 1), c))
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.strokeStyle = g
  ctx.shadowColor = 'rgba(168, 85, 247, 0.55)'
  ctx.shadowBlur = 22
  ctx.globalAlpha = 0.26
  ctx.lineWidth = 9
  ctx.stroke(path)
  ctx.globalAlpha = 1
  ctx.lineWidth = 2.6
  ctx.stroke(path)
  ctx.shadowBlur = 0
}
</script>

<style scoped>
.audio-wave {
  width: 100%;
  background: #000; /* 深色纯黑背景 */
  border-radius: 14px;
  overflow: hidden;
}
.audio-wave canvas {
  display: block;
  width: 100%;
  height: 100%;
}
</style>

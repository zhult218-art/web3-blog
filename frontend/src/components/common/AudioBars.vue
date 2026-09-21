<template>
  <!-- 柱状流动频谱：圆角柱 + 极光渐变 + 发光，实时反映音量 -->
  <div class="audio-bars" :style="{ height: height + 'px' }">
    <canvas ref="cv"></canvas>
  </div>
</template>

<script setup>
// ====================================================
// AudioBars —— 柱状图 / 柱状线条流动可视化（Web Audio API）
// · 柱体：圆角胶囊柱，按频率分布映射，逐柱插值，流动丝滑
// · 颜色：青 → 紫 → 粉极光渐变，带柔和发光
// · 兼容：跨域音频频谱静音、暂停、无分析器时自动切换模拟流动
// 用法：<AudioBars :height="100" :bars="56" />
// ====================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { usePlayerStore } from '@/stores/modules/player'
import { useAudioAnalyser } from '@/composables/useAudioAnalyser'

const props = defineProps({
  height: { type: Number, default: 100 }, // 画布高度 px
  bars: { type: Number, default: 56 },    // 柱数量
})

const cv = ref(null)
const player = usePlayerStore()
const analyser = useAudioAnalyser()

let ctx = null
let raf = 0
let W = 0
let H = 0
let t = 0
let levels = []   // 每根柱的平滑高度（0~1）
let tainted = false

onMounted(() => {
  ctx = cv.value.getContext('2d')
  levels = new Array(props.bars).fill(0.05)
  resize()
  window.addEventListener('resize', resize)
  analyser.attach()
  draw()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
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

// 模拟模式：多组错相正弦叠加，让柱体自然起伏流动
function fakeLevel(i, n) {
  const p = i / n
  const flow = t * 1.8
  let v = 0.28
    + 0.22 * Math.sin(p * 6.28 * 2 + flow)
    + 0.16 * Math.sin(p * 6.28 * 3.1 - flow * 1.4 + 1.7)
    + 0.1 * Math.sin(p * 6.28 * 5 + flow * 0.6)
  return Math.max(0.04, Math.min(0.95, v))
}

function draw() {
  raf = requestAnimationFrame(draw)
  if (!ctx) return
  t += 0.016
  ctx.clearRect(0, 0, W, H)

  const n = props.bars
  const data = analyser.getFrequency()
  let hasReal = false
  if (data) {
    for (let k = 0; k < 24; k++) {
      if (data[k] > 4) { hasReal = true; break }
    }
  }
  if (player.isPlaying && !hasReal) tainted = true
  else if (hasReal) tainted = false

  const useSim = !data || tainted || !player.isPlaying

  for (let i = 0; i < n; i++) {
    let target
    if (useSim) {
      // 暂停时压低整体幅度，保持轻微呼吸
      target = fakeLevel(i, n) * (player.isPlaying ? 1 : 0.32)
    } else {
      // 对数式频率映射：低频柱密、高频柱疏，听感更均衡
      const ratio = i / n
      const bin = Math.floor(Math.pow(ratio, 1.7) * data.length * 0.72)
      const v = data[bin] / 255
      target = Math.max(0.03, Math.min(1, Math.pow(v, 1.15) * 1.05))
    }
    // 逐柱插值：升起快、落下稍慢 → 流动感
    const ease = target > levels[i] ? 0.32 : 0.16
    levels[i] += (target - levels[i]) * ease
  }

  // 柱体布局
  const gap = Math.max(2, W / n * 0.28)
  const bw = (W - gap * (n - 1)) / n
  const baseY = H
  const grad = ctx.createLinearGradient(0, 0, W, 0)
  grad.addColorStop(0, '#22d3ee')
  grad.addColorStop(0.5, '#a855f7')
  grad.addColorStop(1, '#f0abfc')

  ctx.shadowColor = 'rgba(168, 85, 247, 0.55)'
  ctx.shadowBlur = 12

  for (let i = 0; i < n; i++) {
    const h = Math.max(bw * 0.5, levels[i] * (H - 4))
    const x = i * (bw + gap)
    const y = baseY - h
    const r = Math.min(bw / 2, 4)
    // 圆角胶囊柱
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.moveTo(x, y + r)
    ctx.arcTo(x, y, x + r, y, r)
    ctx.arcTo(x + bw, y, x + bw, y + r, r)
    ctx.lineTo(x + bw, baseY)
    ctx.lineTo(x, baseY)
    ctx.closePath()
    ctx.fill()
    // 柱顶高光
    ctx.shadowBlur = 0
    ctx.globalAlpha = 0.5
    ctx.fillStyle = '#ffffff'
    ctx.beginPath()
    ctx.ellipse(x + bw / 2, y + 1.5, bw / 2, 1.6, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.globalAlpha = 1
    ctx.shadowBlur = 12
  }
  ctx.shadowBlur = 0
}
</script>

<style scoped>
.audio-bars {
  width: 100%;
  background: transparent;
  overflow: hidden;
}
.audio-bars canvas {
  display: block;
  width: 100%;
  height: 100%;
}
</style>

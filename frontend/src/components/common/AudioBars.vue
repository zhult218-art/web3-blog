<template>
  <!-- 柱状流动频谱：胶囊柱 + 逐柱竖直光晕渐变 + 峰值星点 + 底部辉光基线 -->
  <div class="audio-bars" :style="{ height: height + 'px' }">
    <canvas ref="cv"></canvas>
  </div>
</template>

<script setup>
// ====================================================
// AudioBars —— 柱状流动可视化（Web Audio API）
// · 柱体：圆角胶囊柱，逐柱"上亮下淡"竖直渐变，顶端高光
// · 配色：给了 accent（专辑主色）就跟随封面主色，否则青→紫→粉极光渐变
// · 动态：峰值点缓慢回落形成呼吸感 + 底部一道流动辉光基线
// · 兼容：跨域音频频谱静音、暂停、无分析器时自动切换模拟流动
// 用法：<AudioBars :height="100" :bars="56" :accent="{ r, g, b }" />
// ====================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { usePlayerStore } from '@/stores/modules/player'
import { useAudioAnalyser } from '@/composables/useAudioAnalyser'

const props = defineProps({
  height: { type: Number, default: 100 }, // 画布高度 px
  bars: { type: Number, default: 56 },    // 柱数量
  accent: { type: Object, default: null }, // 专辑主色 { r, g, b }，可空
})

const cv = ref(null)
const player = usePlayerStore()
const analyser = useAudioAnalyser()

// 极光三色：青 → 紫 → 粉
const AURORA = [
  [34, 211, 238],
  [168, 85, 247],
  [240, 171, 252],
]

let ctx = null
let raf = 0
let W = 0
let H = 0
let t = 0
let levels = []   // 每根柱的平滑高度（0~1）
let peaks = []    // 每根柱的峰值（px），缓慢回落
let tainted = false

onMounted(() => {
  ctx = cv.value.getContext('2d')
  levels = new Array(props.bars).fill(0.05)
  peaks = new Array(props.bars).fill(0)
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
  const v = 0.28
    + 0.22 * Math.sin(p * 6.28 * 2 + flow)
    + 0.16 * Math.sin(p * 6.28 * 3.1 - flow * 1.4 + 1.7)
    + 0.1 * Math.sin(p * 6.28 * 5 + flow * 0.6)
  return Math.max(0.04, Math.min(0.95, v))
}

const lerp = (a, b, p) => a + (b - a) * p
const clamp255 = v => Math.max(0, Math.min(255, Math.round(v)))

// 按横向比例取极光渐变色
function auroraColor(ratio) {
  const [c1, c2, p] = ratio < 0.5
    ? [AURORA[0], AURORA[1], ratio / 0.5]
    : [AURORA[1], AURORA[2], (ratio - 0.5) / 0.5]
  return [lerp(c1[0], c2[0], p), lerp(c1[1], c2[1], p), lerp(c1[2], c2[2], p)]
}

function lighten(c, amt) {
  return [clamp255(c[0] + amt), clamp255(c[1] + amt), clamp255(c[2] + amt)]
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
      target = fakeLevel(i, n) * (player.isPlaying ? 1.1 : 0.32)
    } else {
      // 对数式频率映射：低频柱密、高频柱疏，听感更均衡
      const ratio = i / n
      const bin = Math.floor(Math.pow(ratio, 1.7) * data.length * 0.72)
      const v = data[bin] / 255
      // 提升低值 + 放大倍数，让柱体随分贝起伏更明显
      target = Math.max(0.03, Math.min(1, Math.pow(v, 0.78) * 1.6))
    }
    // 逐柱插值：升起更快、落下稍慢 → 节奏感更强
    const ease = target > levels[i] ? 0.5 : 0.18
    levels[i] += (target - levels[i]) * ease
  }

  // 柱体布局
  const gap = Math.max(2, W / n * 0.28)
  const bw = (W - gap * (n - 1)) / n
  const baseY = H
  const accentRgb = props.accent
    ? [props.accent.r ?? 168, props.accent.g ?? 85, props.accent.b ?? 247]
    : null

  // ── 底部辉光基线（先画，位于柱体之后）──
  const baseGrad = ctx.createLinearGradient(0, 0, W, 0)
  if (accentRgb) {
    baseGrad.addColorStop(0, `rgba(${accentRgb},0)`)
    baseGrad.addColorStop(0.5, `rgba(${accentRgb},0.85)`)
    baseGrad.addColorStop(1, `rgba(${accentRgb},0)`)
  } else {
    baseGrad.addColorStop(0, 'rgba(34,211,238,0)')
    baseGrad.addColorStop(0.5, 'rgba(168,85,247,0.9)')
    baseGrad.addColorStop(1, 'rgba(240,171,252,0)')
  }
  ctx.save()
  ctx.shadowColor = accentRgb ? `rgba(${accentRgb},0.9)` : 'rgba(168,85,247,0.9)'
  ctx.shadowBlur = 12
  ctx.fillStyle = baseGrad
  ctx.fillRect(0, baseY - 1.4, W, 1.4)
  ctx.restore()

  // ── 柱体 ──
  for (let i = 0; i < n; i++) {
    const ratio = n > 1 ? i / (n - 1) : 0
    const base = accentRgb || auroraColor(ratio)
    const top = lighten(base, accentRgb ? 80 : 60)
    const mid = lighten(base, accentRgb ? 34 : 20)

    // 降低柱体最低高度，让静音/低分贝时柱体更矮，高低反差更大
    const h = Math.max(bw * 0.2, levels[i] * (H - 4))
    const x = i * (bw + gap)
    const y = baseY - h
    const r = Math.min(bw / 2, 4)

    // 逐柱竖直渐变：顶端发亮 → 底部半透明，营造"流动光柱"
    const grad = ctx.createLinearGradient(0, y, 0, baseY)
    grad.addColorStop(0, `rgba(${top},1)`)
    grad.addColorStop(0.45, `rgba(${mid},0.92)`)
    grad.addColorStop(1, `rgba(${base},0.42)`)

    ctx.save()
    ctx.shadowColor = `rgba(${base},0.65)`
    ctx.shadowBlur = 14
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.moveTo(x, y + r)
    ctx.arcTo(x, y, x + r, y, r)
    ctx.arcTo(x + bw, y, x + bw, y + r, r)
    ctx.lineTo(x + bw, baseY)
    ctx.lineTo(x, baseY)
    ctx.closePath()
    ctx.fill()
    ctx.restore()

    // 柱顶高光点
    ctx.save()
    ctx.globalAlpha = 0.65
    ctx.shadowColor = `rgba(${top},0.9)`
    ctx.shadowBlur = 8
    ctx.fillStyle = '#ffffff'
    ctx.beginPath()
    ctx.ellipse(x + bw / 2, y + 1.4, bw / 2, 1.5, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.restore()

    // 峰值星点：跟随柱体上升，之后缓慢回落
    if (h >= peaks[i]) peaks[i] = h
    else peaks[i] = Math.max(h, peaks[i] - 0.7)
    if (peaks[i] > h + 2) {
      ctx.save()
      ctx.shadowColor = `rgba(${top},0.95)`
      ctx.shadowBlur = 10
      ctx.fillStyle = `rgba(${lighten(base, 110)},1)`
      ctx.beginPath()
      ctx.arc(x + bw / 2, baseY - peaks[i], Math.max(1.1, bw * 0.16), 0, Math.PI * 2)
      ctx.fill()
      ctx.restore()
    }
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

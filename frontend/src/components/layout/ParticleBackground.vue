<template>
  <canvas ref="canvas" class="pointer-events-none fixed inset-0 h-full w-full" />
</template>

<script setup>
// ============================================================
// 全局 Canvas 粒子连线背景（ParticleBackground）
// 缓慢上升的圆点粒子，距离 <100px 的粒子间画连线形成网络效果
// 移动端自动减粒子并关闭连线（见 useDeviceCapability）
// ============================================================
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { useDeviceCapability } from '@/composables/useDeviceCapability'

// 设备能力：移动端 / 低配 / 减少动效 → 静态帧渲染；移动端降粒子数并省掉连线计算
const { isMobile, staticBackground } = useDeviceCapability()

const canvas = ref(null)
let ctx = null
let particles = []
let raf = 0
let running = false
let mouse = { x: -1000, y: -1000 }

// 单个粒子：位置 / 速度 / 大小 / 颜色色相 / 透明度
class Particle {
  constructor(w, h) {
    this.reset(w, h)
    this.y = Math.random() * h
  }
  // 重置粒子（移出屏幕后复用）
  reset(w, h) {
    this.x = Math.random() * w
    this.y = -10
    this.size = Math.random() * 2 + 0.5
    this.speedY = Math.random() * 0.4 + 0.15
    this.speedX = (Math.random() - 0.5) * 0.3
    this.opacity = Math.random() * 0.5 + 0.2
    this.hue = Math.random() > 0.6 ? 270 : 190
  }
  // 更新位置，越界时重置
  update(w, h) {
    this.y += this.speedY
    this.x += this.speedX
    if (this.y > h + 10) this.reset(w, h)
    if (this.x < -10) this.x = w + 10
    if (this.x > w + 10) this.x = -10
  }
  // 绘制圆点
  draw(ctx) {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fillStyle = `hsla(${this.hue}, 70%, 65%, ${this.opacity})`
    ctx.fill()
  }
}

// 适配窗口尺寸并初始化粒子数组
function resize() {
  const c = canvas.value
  if (!c) return
  c.width = window.innerWidth
  c.height = window.innerHeight
  particles = Array.from({ length: isMobile ? 40 : 100 }, () => new Particle(c.width, c.height))
}

// 绘制一帧：粒子更新 + 邻近连线（移动端跳过连线，省掉 O(n²) 计算与逐线描边）
function renderFrame() {
  const c = canvas.value
  if (!c || !ctx) return
  ctx.clearRect(0, 0, c.width, c.height)

  for (const p of particles) {
    p.update(c.width, c.height)
    p.draw(ctx)
  }

  // Connect nearby particles for network effect（仅桌面）
  if (!isMobile) {
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 100) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(168,85,247,${0.06 * (1 - dist / 100)})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }
  }
}

// ── 循环控制：静态降级 / 切后台暂停 ──
function loop() {
  if (!running) return
  renderFrame()
  raf = requestAnimationFrame(loop)
}

function startLoop() {
  if (running || staticBackground) return
  running = true
  loop()
}

function stopLoop() {
  running = false
  cancelAnimationFrame(raf)
}

// 切到后台 / 息屏 → 停止渲染；恢复可见 → 重启，避免后台空转耗电发热
function onVisibility() {
  if (document.hidden) stopLoop()
  else startLoop()
}

// 窗口尺寸变化：重设画布（静态模式下画布被清空，需补渲染一帧）
function onResize() {
  resize()
  if (staticBackground) renderFrame()
}

function onMouseMove(e) {
  mouse.x = e.clientX
  mouse.y = e.clientY
}

onMounted(() => {
  resize()
  ctx = canvas.value.getContext('2d')
  // 静态降级：只渲染一帧；否则启动动画循环
  if (staticBackground) renderFrame()
  else startLoop()
  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMouseMove)
  document.addEventListener('visibilitychange', onVisibility)
})

onBeforeUnmount(() => {
  stopLoop()
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

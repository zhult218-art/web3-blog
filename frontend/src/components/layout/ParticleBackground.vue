<template>
  <canvas ref="canvas" class="pointer-events-none fixed inset-0 h-full w-full" />
</template>

<script setup>
// ============================================================
// 全局 Canvas 粒子连线背景（ParticleBackground）
// 100 个缓慢上升的圆点粒子，距离 <100px 的粒子间画连线形成网络效果
// ============================================================
import { onMounted, onBeforeUnmount, ref } from 'vue'

const canvas = ref(null)
let ctx = null
let particles = []
let raf = 0
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
  particles = Array.from({ length: 100 }, () => new Particle(c.width, c.height))
}

// 绘制一帧：粒子更新 + 邻近连线 + 循环动画
function draw() {
  const c = canvas.value
  if (!c || !ctx) return
  ctx.clearRect(0, 0, c.width, c.height)

  for (const p of particles) {
    p.update(c.width, c.height)
    p.draw(ctx)
  }

  // Connect nearby particles for network effect
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

  raf = requestAnimationFrame(draw)
}

function onMouseMove(e) {
  mouse.x = e.clientX
  mouse.y = e.clientY
}

onMounted(() => {
  resize()
  ctx = canvas.value.getContext('2d')
  draw()
  window.addEventListener('resize', resize)
  window.addEventListener('mousemove', onMouseMove)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
  window.removeEventListener('mousemove', onMouseMove)
})
</script>

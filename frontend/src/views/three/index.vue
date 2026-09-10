<template>
  <div class="three-page">
    <!-- HERO + 3D 场景 -->
    <section class="hero" ref="hero">
      <div class="hero-back"><PageBack label="返回首页" to="/" /></div>
      <div class="canvas-wrap"><canvas ref="sceneCanvas"></canvas></div>
      <div class="hero-inner">
        <div class="badge fade">● WEBGL · THREE.JS · 手势驱动</div>
        <h1 class="title fade">3D 粒子空间</h1>
        <p class="sub fade">
          WebGL 实时粒子星云，可随滚动散聚、随鼠标流动；
          下行接入手势识别，用指尖隔空操控展厅。
        </p>
        <div class="tags fade">
          <span v-for="t in heroTags" :key="t">#{{ t }}</span>
        </div>
        <div class="hint fade">● 滚动改变粒子形态</div>
      </div>
    </section>

    <!-- 滚动联动说明 -->
    <section class="modes">
      <div class="sec-head">
        <span class="sec-num">[ 01 ]</span>
        <h2>粒子形态</h2>
        <p>PARTICLE MODES</p>
      </div>
      <div class="mode-grid">
        <div v-for="(m, i) in modes" :key="m.name" class="mode-card">
          <div class="mode-icon">{{ m.icon }}</div>
          <h3>{{ m.name }}</h3>
          <p>{{ m.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 手势展厅 -->
    <section class="gesture">
      <div class="sec-head">
        <span class="sec-num">[ 02 ]</span>
        <h2>手势展厅</h2>
        <p>GESTURE GALLERY</p>
      </div>
      <div class="gesture-flow">
        <div v-for="(g, i) in gestureSteps" :key="g.name" class="gesture-step">
          <div class="g-index">0{{ i + 1 }}</div>
          <h3>{{ g.name }}</h3>
          <p>{{ g.desc }}</p>
        </div>
      </div>
      <div class="gesture-note">
        <span class="dot"></span>
        摄像头画面仅在本地处理，关键点实时解算，不涉及隐私上传。
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-sec">
      <h2>想看更多交互玩法？</h2>
      <p>体验站内 3D 粒子云卡片，或从首页 Showcase 直接进入</p>
      <router-link to="/media" class="cta">前往 3D 展厅 →</router-link>
    </section>
  </div>
</template>

<script setup>
// ====================================================
// Three.js 粒子展厅：WebGL 星云粒子系统 + 鼠标交互
// GSAP 滚动驱动多种形态切换
// ====================================================
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as THREE from 'three'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import PageBack from '@/components/PageBack.vue'

gsap.registerPlugin(ScrollTrigger)

// 首页顶部特性标签
const heroTags = ['Three.js', 'WebGL', 'MediaPipe', 'Canvas']

const hero = ref(null)
const sceneCanvas = ref(null)

// 粒子形态模式说明（星云/鼠标流动/拖尾光点）
const modes = [
  { icon: '🌀', name: '星云形态', desc: '滚动向下，粒子从球体散开成螺旋星云，如星尘坠落' },
  { icon: '🖱️', name: '鼠标流动', desc: '粒子跟随指针产生磁场般的引力流动，迟缓而顺滑' },
  { icon: '☄️', name: '拖尾光点', desc: '色彩渐变着色，粒子旋转拖出星轨般的残影' },
]

// 手势交互步骤说明（MediaPipe 手势识别流程）
const gestureSteps = [
  { name: '小手检测', desc: 'MediaPipe Hands 实时定位 21 个关键点' },
  { name: '指尖追踪', desc: '以指尖坐标驱动展厅镜头 / 粒子交互' },
  { name: '手势指令', desc: '握拳暂停、张开收集、滑动切换展品' },
  { name: '视觉反馈', desc: '展厅画布同步渲染响应与状态光效' },
]

let renderer = null
let scene = null
let camera = null
let particles = null
let rafId = null
let mouseX = 0
let mouseY = 0
let modeT = 0
let ctx = null

const COUNT = 1800

// 初始化 Three.js 场景、相机与星云粒子系统
function initThree() {
  const canvas = sceneCanvas.value
  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(60, canvas.clientWidth / canvas.clientHeight, 0.1, 100)
  camera.position.z = 9

  const positions = new Float32Array(COUNT * 3)
  const colors = new Float32Array(COUNT * 3)
  const cA = new THREE.Color('#667eea')
  const cB = new THREE.Color('#00d4ff')
  const cC = new THREE.Color('#764ba2')
  for (let i = 0; i < COUNT; i++) {
    const r = 2.2 + Math.random() * 2.6
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    positions[i * 3] = r * Math.sin(phi) * Math.cos(theta)
    positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta)
    positions[i * 3 + 2] = r * Math.cos(phi)
    const c = i % 3 === 0 ? cA : i % 3 === 1 ? cB : cC
    colors[i * 3] = c.r
    colors[i * 3 + 1] = c.g
    colors[i * 3 + 2] = c.b
  }
  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geo.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  const mat = new THREE.PointsMaterial({
    size: 0.045, vertexColors: true, transparent: true, opacity: 0.9,
  })
  particles = new THREE.Points(geo, mat)
  scene.add(particles)

  animate()
}

// 逐帧动画：粒子波动形态 + 跟随鼠标偏移
function animate() {
  rafId = requestAnimationFrame(animate)
  const t = performance.now() * 0.00012
  particles.rotation.y += 0.0009
  particles.rotation.x = Math.sin(t * 0.5) * 0.12

  // 鼠标流动
  const px = mouseX * 0.22
  const py = mouseY * 0.22
  const pos = particles.geometry.attributes.position.array
  for (let i = 0; i < COUNT; i++) {
    const tx = pos[i * 3] + Math.sin(t * 1.4 + i) * 0.012
    const ty = pos[i * 3 + 1] * (1 + Math.sin(t * 1.1 + i * 0.5) * 0.06)
    pos[i * 3] = THREE.MathUtils.lerp(pos[i * 3], px + Math.sin(i * 0.3 + t * 2) * 0.35 + tx * 0.06, 0.02)
    pos[i * 3 + 1] = THREE.MathUtils.lerp(pos[i * 3 + 1], py + Math.cos(i * 0.4 + t * 2) * 0.35 + ty * 0.04, 0.02)
    pos[i * 3 + 2] = pos[i * 3 + 2] * (1 + modeT)
  }
  particles.geometry.attributes.position.needsUpdate = true
  renderer.render(scene, camera)
}

// 记录鼠标位置，用于驱动粒子形态
function onMouse(e) {
  mouseX = (e.clientX / window.innerWidth) * 2 - 1
  mouseY = -((e.clientY / window.innerHeight) * 2 - 1)
}

// 滚动时根据页面偏移切换粒子形态
function onScroll() {
  modeT = Math.min(0.55, Math.max(0, window.scrollY / window.innerHeight * 0.5))
}

// 窗口缩放时同步画布与相机尺寸
function resize() {
  const canvas = sceneCanvas.value
  const w = canvas.clientWidth
  const h = canvas.clientHeight
  renderer.setSize(w, h, false)
  camera.aspect = w / h
  camera.updateProjectionMatrix()
}

onMounted(() => {
  initThree()
  window.addEventListener('mousemove', onMouse)
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', resize)

  ctx = gsap.context(() => {
    gsap.from('.fade', { y: 40, opacity: 0, duration: 1, stagger: 0.15, ease: 'power3.out' })
    gsap.from('.mode-card', {
      y: 60, opacity: 0, duration: 0.9, stagger: 0.12, ease: 'power3.out',
      scrollTrigger: { trigger: '.mode-grid', start: 'top 80%', once: true },
    })
    gsap.from('.gesture-step', {
      y: 50, opacity: 0, duration: 0.8, stagger: 0.1, ease: 'power3.out',
      scrollTrigger: { trigger: '.gesture-flow', start: 'top 80%', once: true },
    })
  })
})

onBeforeUnmount(() => {
  if (rafId) cancelAnimationFrame(rafId)
  window.removeEventListener('mousemove', onMouse)
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', resize)
  if (renderer) { renderer.dispose(); renderer = null }
  if (ctx) ctx.revert()
})
</script>

<style scoped>
.three-page { position: relative; min-height: 100vh; background: linear-gradient(180deg, #06060e, #0a0a1e); color: #e0e0f0; overflow: hidden; }

.hero { position: relative; min-height: 92vh; display: flex; align-items: center; justify-content: center; }
.hero-back { position: absolute; top: 24px; left: 24px; z-index: 20; }
@media (max-width: 640px) { .hero-back { top: 16px; left: 16px; } }
.canvas-wrap { position: absolute; inset: 0; z-index: 0; }
.canvas-wrap canvas { width: 100%; height: 100%; display: block; }
.hero-inner { position: relative; z-index: 1; text-align: center; padding: 2rem; pointer-events: none; }
.badge { display: inline-flex; padding: 0.45rem 1.1rem; border: 1px solid rgba(102, 126, 234, 0.4); border-radius: 4px; color: #9db2ff; letter-spacing: 0.2em; font-size: 0.72rem; margin-bottom: 1.4rem; }
.title { font-size: clamp(2.4rem, 6vw, 4.6rem); font-weight: 900; letter-spacing: 0.06em; margin-bottom: 1.2rem; background: linear-gradient(135deg, #fff, #9db2ff 50%, #00d4ff); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; }
.sub { font-size: 1.02rem; line-height: 1.9; color: rgba(255,255,255,0.82); max-width: 620px; margin: 0 auto 1.6rem; }
.tags { display: flex; gap: 0.6rem; justify-content: center; flex-wrap: wrap; margin-bottom: 2rem; pointer-events: auto; }
.tags span { font-size: 0.72rem; font-family: monospace; color: #7fd9ff; padding: 0.3rem 0.8rem; border: 1px solid rgba(0, 212, 255, 0.25); border-radius: 999px; background: rgba(0, 212, 255, 0.06); }
.hint { font-size: 0.78rem; color: rgba(255,255,255,0.6); letter-spacing: 0.2em; animation: hintPulse 2.4s ease-in-out infinite; }
@keyframes hintPulse { 0%, 100% { opacity: 0.6; } 50% { opacity: 1; } }

.modes { position: relative; padding: 5rem 2rem; }
.sec-head { text-align: center; margin-bottom: 3.5rem; }
.sec-num { color: #667eea; font-family: monospace; font-size: 0.75rem; letter-spacing: 0.25em; }
.sec-head h2 { font-size: clamp(1.6rem, 3.4vw, 2.4rem); font-weight: 800; margin: 0.5rem 0 0.3rem; }
.sec-head p { font-size: 0.7rem; letter-spacing: 0.3em; color: rgba(255,255,255,0.6); }
.mode-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.4rem; max-width: 1050px; margin: 0 auto; }
.mode-card { padding: 2rem 1.6rem; border-radius: 18px; background: rgba(12, 12, 28, 0.65); border: 1px solid rgba(255,255,255,0.07); text-align: center; transition: all 0.35s; }
.mode-card:hover { border-color: rgba(102, 126, 234, 0.5); box-shadow: 0 0 34px rgba(102, 126, 234, 0.15); transform: translateY(-4px); }
.mode-icon { font-size: 2.2rem; margin-bottom: 0.9rem; }
.mode-card h3 { font-size: 1.1rem; font-weight: 700; margin-bottom: 0.6rem; }
.mode-card p { font-size: 0.84rem; line-height: 1.8; color: rgba(255,255,255,0.75); }

.gesture { position: relative; padding: 5rem 2rem; }
.gesture-flow { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1.2rem; max-width: 1080px; margin: 0 auto 2rem; }
.gesture-step { position: relative; padding: 1.6rem 1.4rem; border-radius: 16px; background: rgba(12, 12, 28, 0.65); border: 1px solid rgba(255,255,255,0.07); transition: all 0.3s; }
.gesture-step:hover { border-color: rgba(0, 212, 255, 0.45); box-shadow: 0 0 26px rgba(0, 212, 255, 0.12); }
.g-index { font-size: 0.72rem; font-family: monospace; color: #00d4ff; letter-spacing: 0.2em; margin-bottom: 0.6rem; }
.gesture-step h3 { font-size: 1rem; font-weight: 700; margin-bottom: 0.5rem; }
.gesture-step p { font-size: 0.8rem; line-height: 1.7; color: rgba(255,255,255,0.72); }
.gesture-note { display: flex; align-items: center; gap: 0.6rem; justify-content: center; font-size: 0.78rem; color: rgba(255,255,255,0.62); }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #00d4ff; box-shadow: 0 0 8px #00d4ff; animation: dotBlink 2s ease-in-out infinite; }
@keyframes dotBlink { 0%, 100% { opacity: 0.4; } 50% { opacity: 1; } }

.cta-sec { position: relative; text-align: center; padding: 6rem 2rem 7rem; }
.cta-sec h2 { font-size: clamp(1.5rem, 3vw, 2.2rem); font-weight: 800; margin-bottom: 0.8rem; }
.cta-sec p { color: rgba(255,255,255,0.72); font-size: 0.9rem; margin-bottom: 2rem; }
.cta { display: inline-flex; padding: 0.85rem 1.9rem; border-radius: 12px; background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; font-size: 0.88rem; font-weight: 700; letter-spacing: 0.08em; box-shadow: 0 8px 30px rgba(102, 126, 234, 0.35); transition: all 0.3s; text-decoration: none; }
.cta:hover { box-shadow: 0 12px 44px rgba(102, 126, 234, 0.55); transform: translateY(-2px); }

@media (max-width: 1000px) {
  .mode-grid { grid-template-columns: 1fr; max-width: 560px; }
  .gesture-flow { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .gesture-flow { grid-template-columns: 1fr; }
}
</style>
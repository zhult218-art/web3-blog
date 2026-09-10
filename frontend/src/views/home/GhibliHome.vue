<template>
  <div class="terminal-home">
    <!-- ============ HERO: 星际终端 (保持原有结构与 3D 场景不变) ============ -->
    <section class="hero-terminal" ref="heroSection">
      <!-- HUD Overlay -->
      <div class="hud-overlay">
        <div class="hud-top">
          <div class="hud-logo">
            <div class="logo-ring"></div>
            <span class="logo-text">VERSE<span class="logo-accent">NOTE</span></span>
          </div>
          <div class="hud-status">
            <span class="status-indicator"></span>
            <span class="status-text">ONLINE</span>
          </div>
        </div>

        <div class="hero-content">
          <div class="hero-badge">
            <span class="badge-icon">◈</span>
            <span class="badge-label">SYSTEM INITIALIZED</span>
          </div>

          <!-- 3D TERMINAL HEAD (centerpiece) -->
          <div class="hero-terminal-container">
            <canvas ref="heroCanvas" class="hero-canvas"></canvas>
            <div class="terminal-glow"></div>
          </div>

          <p class="hero-subtitle">WEB3 PORTAL v2.0</p>

          <!-- Voice hint -->
          <div class="voice-hint">
            <span class="hint-icon">🎤</span>
            <span>星途星途唤醒智能助手</span>
          </div>

          <div class="scroll-hint">
            <div class="scroll-line"></div>
            <span class="scroll-text">SCROLL TO EXPLORE</span>
          </div>
        </div>

        <div class="hud-bottom">
          <div class="hud-coords">
            <span>SYS.LAT 39.9042°N</span>
            <span class="divider">│</span>
            <span>LNG 116.4074°E</span>
          </div>
          <div class="hud-time">{{ currentTime }}</div>
        </div>
      </div>
    </section>

    <!-- ============ [01] 核心架构与技术矩阵 ============ -->
    <div class="section-seam" aria-hidden="true"></div>
    <TechMatrix />

    <!-- ============ [02] 核心作品与 Demo 橱窗 ============ -->
    <div class="section-seam" aria-hidden="true"></div>
    <ShowcaseGrid />

    <!-- ============ [03] 技术干货与深度文章 ============ -->
    <div class="section-seam" aria-hidden="true"></div>
    <ArticleCards />

    <!-- ============ [04] 动效实验室（滚动特效合集） ============ -->
    <MotionLab />

    <!-- ============ [05] 全站导览地图（同类功能归组，快速定位） ============ -->
    <div class="section-seam" aria-hidden="true"></div>
    <SiteMap />

    <!-- ============ FOOTER 极简双栏 ============ -->
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-left">
          <span class="footer-logo">VERSE<span class="logo-accent">NOTE</span></span>
          <p class="footer-slogan">以代码构建无限可能，让每一个想法都拥有沉浸式表达。</p>
        </div>
        <div class="footer-right">
          <a href="https://github.com/zhult218-art" target="_blank" rel="noopener" class="social-btn" data-glow data-magnetic aria-label="GitHub">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 .5C5.65.5.5 5.65.5 12c0 5.08 3.29 9.39 7.86 10.91.58.11.79-.25.79-.56v-2c-3.2.7-3.87-1.54-3.87-1.54-.52-1.33-1.28-1.68-1.28-1.68-1.04-.71.08-.7.08-.7 1.15.08 1.76 1.19 1.76 1.19 1.03 1.76 2.7 1.25 3.36.96.1-.75.4-1.25.72-1.54-2.55-.29-5.23-1.28-5.23-5.68 0-1.26.45-2.28 1.19-3.09-.12-.29-.52-1.46.11-3.05 0 0 .97-.31 3.17 1.18.92-.26 1.9-.38 2.88-.39.98 0 1.96.13 2.88.39 2.2-1.49 3.17-1.18 3.17-1.18.63 1.59.23 2.76.11 3.05.74.81 1.19 1.83 1.19 3.09 0 4.41-2.69 5.38-5.25 5.67.41.35.78 1.05.78 2.12v3.14c0 .31.21.67.8.56A10.5 10.5 0 0 0 23.5 12C23.5 5.65 18.35.5 12 .5Z"/></svg>
          </a>
          <a href="mailto:zhult218@gmail.com" class="social-btn" data-glow data-magnetic aria-label="Gmail">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="3"/><path d="m2 7 10 7 10-7"/></svg>
          </a>
          <a href="mailto:m17357515408@163.com" class="social-btn mail-163" data-glow data-magnetic aria-label="163 邮箱">
            <span class="mail-badge">163</span>
          </a>
        </div>
      </div>
      <div class="footer-bottom">
        <span>© {{ currentYear }} VERSE NOTE · 无限可能</span>
        <span class="fb-divider">│</span>
        <span class="footer-mails">zhult218@gmail.com · m17357515408@163.com</span>
      </div>
    </footer>

    <!-- 全局背景微光粒子场（滚动后随首屏 3D 一起浮现，形成视觉延续） -->
    <div class="bg-field" ref="bgField">
      <div class="bg-field-grid"></div>
      <span v-for="i in 56" :key="i" class="bg-point" :style="pointStyle(i)"></span>
    </div>

    <!-- 天空之城云海：柔和光云缓慢漂过，与全局壁纸背景呼应 -->
    <div class="sky-clouds" aria-hidden="true">
      <div class="cloud cloud-a"></div>
      <div class="cloud cloud-b"></div>
      <div class="cloud cloud-c"></div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 首页（吉卜力/星尘主题）：终端 HUD 风格首屏，
// Three.js 头部模型粒子场景 + 时间刷新 + 滚动视差
// ====================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import TechMatrix from '@/components/home/TechMatrix.vue'
import ShowcaseGrid from '@/components/home/ShowcaseGrid.vue'
import ArticleCards from '@/components/home/ArticleCards.vue'
import MotionLab from '@/components/home/MotionLab.vue'
import SiteMap from '@/components/home/SiteMap.vue'

const heroSection = ref(null)
const heroCanvas = ref(null)
const bgField = ref(null)
const currentTime = ref('')

let scene, camera, renderer, clock
let headGroup, particles, rings = []
let raf = 0
let timeInterval = 0
let canvasBox = null

const currentYear = new Date().getFullYear()

// 刷新页面显示的当前时间
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('en-US', { hour12: false })
}

// ============================================================
// Three.js 场景：粒子人头 + 轨道环（原有，保持不变）
// ============================================================
// 初始化 Three.js 场景：头部模型、粒子云、视线与光环
function initThreeScene() {
  const canvas = heroCanvas.value
  if (!canvas) return

  const box = canvas.parentElement.getBoundingClientRect()
  const w = box.width || 320
  const h = box.height || 320
  canvasBox = { w, h }

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(50, w / h, 0.1, 100)
  camera.position.set(0, 0, 4.5)

  renderer = new THREE.WebGLRenderer({ canvas, antialias: false, alpha: true })
  renderer.setPixelRatio(Math.min(devicePixelRatio, 1.5))
  renderer.setSize(w, h)
  renderer.setClearColor(0x000000, 0)

  headGroup = new THREE.Group()

  const points = []
  const segments = 20
  for (let i = 0; i <= segments; i++) {
    const t = i / segments
    const angle = t * Math.PI
    let r = Math.sin(angle) * 1.1
    if (t < 0.3) r *= 1.0 + (0.3 - t) * 0.5
    else if (t < 0.6) r *= 1.05
    else if (t < 0.8) r *= 0.9
    else r *= 0.6
    points.push(new THREE.Vector2(r, 1.3 - t * 2.4))
  }

  const headGeo = new THREE.LatheGeometry(points, 48)
  const headMat = new THREE.MeshBasicMaterial({
    color: 0x667eea,
    wireframe: true,
    transparent: true,
    opacity: 0.15
  })
  const headMesh = new THREE.Mesh(headGeo, headMat)
  headGroup.add(headMesh)

  const innerMat = new THREE.MeshBasicMaterial({
    color: 0x1a1a3a,
    transparent: true,
    opacity: 0.4
  })
  const innerHead = new THREE.Mesh(headGeo.clone(), innerMat)
  innerHead.scale.setScalar(0.98)
  headGroup.add(innerHead)

  const particleCount = 2500
  const headPositions = new Float32Array(particleCount * 3)
  const headColors = new Float32Array(particleCount * 3)

  for (let i = 0; i < particleCount; i++) {
    const t = Math.random()
    const angle = Math.random() * Math.PI * 2
    const y = 1.3 - t * 2.4
    let r = Math.sin(t * Math.PI) * 1.1
    if (t < 0.3) r *= 1.0 + (0.3 - t) * 0.5
    else if (t < 0.6) r *= 1.05
    else if (t < 0.8) r *= 0.9
    else r *= 0.6

    const noise = (Math.random() - 0.5) * 0.08
    r += noise

    headPositions[i * 3] = Math.cos(angle) * r
    headPositions[i * 3 + 1] = y + (Math.random() - 0.5) * 0.05
    headPositions[i * 3 + 2] = Math.sin(angle) * r

    const color = new THREE.Color()
    color.setHSL(0.65 - t * 0.15, 0.9, 0.5 + Math.random() * 0.2)
    headColors[i * 3] = color.r
    headColors[i * 3 + 1] = color.g
    headColors[i * 3 + 2] = color.b
  }

  const particleGeo = new THREE.BufferGeometry()
  particleGeo.setAttribute('position', new THREE.BufferAttribute(headPositions, 3))
  particleGeo.setAttribute('color', new THREE.BufferAttribute(headColors, 3))

  const particleMat = new THREE.PointsMaterial({
    size: 0.012,
    vertexColors: true,
    transparent: true,
    opacity: 0.9,
    sizeAttenuation: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false
  })

  particles = new THREE.Points(particleGeo, particleMat)
  headGroup.add(particles)

  const eyeGeo = new THREE.SphereGeometry(0.06, 8, 8)
  const eyeMat = new THREE.MeshBasicMaterial({ color: 0x00ffff })
  const leftEye = new THREE.Mesh(eyeGeo, eyeMat)
  leftEye.position.set(-0.22, 0.15, 0.95)
  headGroup.add(leftEye)
  const rightEye = new THREE.Mesh(eyeGeo, eyeMat)
  rightEye.position.set(0.22, 0.15, 0.95)
  headGroup.add(rightEye)

  const eyeRingGeo = new THREE.RingGeometry(0.08, 0.12, 16)
  const eyeRingMat = new THREE.MeshBasicMaterial({ color: 0x00ffff, transparent: true, opacity: 0.3, side: THREE.DoubleSide })
  const leftRing = new THREE.Mesh(eyeRingGeo, eyeRingMat)
  leftRing.position.set(-0.22, 0.15, 0.96)
  headGroup.add(leftRing)
  const rightRing = new THREE.Mesh(eyeRingGeo, eyeRingMat)
  rightRing.position.set(0.22, 0.15, 0.96)
  headGroup.add(rightRing)

  const lineMat = new THREE.LineBasicMaterial({ color: 0x667eea, transparent: true, opacity: 0.1 })
  for (let i = 0; i < 15; i++) {
    const lineGeo = new THREE.BufferGeometry()
    const start = new THREE.Vector3(
      (Math.random() - 0.5) * 1.5,
      (Math.random() - 0.5) * 2,
      (Math.random() - 0.5) * 0.5 + 0.8
    )
    const end = new THREE.Vector3(
      start.x + (Math.random() - 0.5) * 0.8,
      start.y + (Math.random() - 0.5) * 0.8,
      start.z + (Math.random() - 0.5) * 0.3
    )
    lineGeo.setFromPoints([start, end])
    const line = new THREE.Line(lineGeo, lineMat)
    headGroup.add(line)
  }

  scene.add(headGroup)

  const ringColors = [0x667eea, 0x764ba2, 0x00ffff]
  for (let i = 0; i < 3; i++) {
    const ringGeo = new THREE.TorusGeometry(1.6 + i * 0.25, 0.003, 8, 120)
    const ringMat = new THREE.MeshBasicMaterial({
      color: ringColors[i],
      transparent: true,
      opacity: 0.25 - i * 0.06
    })
    const ring = new THREE.Mesh(ringGeo, ringMat)
    ring.rotation.x = Math.PI * 0.5 + i * 0.3
    ring.rotation.y = i * 0.5
    rings.push(ring)
    scene.add(ring)
  }

  clock = new THREE.Clock()
  animate()
}

// ============================================================
// 滚动联动：首屏 3D 粒子随下滑平滑拉远 / 散开 / 渐隐，
// 同时全局暗色微光粒子网格浮现，完成页面视觉衔接
// ============================================================
// 计算英雄区滚动进度（0~1）
function getScrollProgress() {
  const el = heroSection.value
  if (!el) return 0
  const rect = el.getBoundingClientRect()
  const h = el.offsetHeight || 1
  const p = -rect.top / h
  return Math.min(1, Math.max(0, p))
}

// 渲染循环：随滚动进度与时间驱动模型旋转、粒子漂移与光环变化
function animate() {
  raf = requestAnimationFrame(animate)
  const t = performance.now() * 0.001
  const dt = clock.getDelta()

  // 滚动联动强度（0 → 1，从首屏顶部滚动到离开首屏）
  const sc = getScrollProgress()
  const ease = 1 - Math.pow(1 - sc, 2)

  if (headGroup) {
    headGroup.rotation.y = t * 0.2
    headGroup.rotation.x = Math.sin(t * 0.3) * 0.1

    // 平滑拉远 + 缩小，粒子摊开为暗色背景
    headGroup.position.z = ease * 9
    headGroup.scale.setScalar(1 - ease * 0.42)

    if (particles) {
      particles.material.size = 0.012 + ease * 0.022
      particles.material.opacity = (0.6 + Math.sin(t * 0.5) * 0.2) * (1 - ease * 0.85)
    }
  }

  rings.forEach((ring, i) => {
    ring.rotation.z = t * (0.1 + i * 0.05)
    ring.rotation.x = Math.PI * 0.5 + Math.sin(t * 0.2 + i) * 0.2
    // 轨道散开并隐去
    ring.position.z = ease * (5 + i * 4)
    ring.material.opacity = (0.25 - i * 0.06) * (1 - ease * 0.72)
  })

  camera.position.z = 4.5 + ease * 8

  // 雪花背景默认可见（0.5），随首屏下滑渐强至 0.95
  if (bgField.value) {
    bgField.value.style.opacity = 0.5 + ease * 0.45
  }

  renderer.render(scene, camera)
}

// 窗口缩放时同步更新渲染器与相机尺寸
function onResize() {
  if (!camera || !renderer) return
  const box = heroCanvas.value?.parentElement?.getBoundingClientRect()
  if (!box) return
  const w = box.width || 320
  const h = box.height || 320
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

// 生成背景雪花的位置/尺寸/下落参数（用素材 xuehua.png / xuehua1.png 替代原方块星）
function pointStyle(i) {
  const seed = (i * 89 + 13) % 100
  const size = 14 + (seed % 16) // 14~29px
  const img = i % 2 === 0 ? '/assets/xuehua.png' : '/assets/xuehua1.png'
  return {
    left: (seed % 94) + 3 + '%',
    top: ((seed * 7) % 92) + 2 + '%',
    width: size + 'px',
    height: size + 'px',
    backgroundImage: `url('${img}')`,
    animationDelay: '-' + (seed % 90) / 10 + 's',
    animationDuration: (9 + (seed % 11)) + 's',
  }
}

// 挂载时启动时间刷新、初始化 Three.js 场景并监听窗口缩放
onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)

  initThreeScene()
  window.addEventListener('resize', onResize)
})

// 卸载时清理定时器、动画帧、渲染器与场景资源
onBeforeUnmount(() => {
  if (timeInterval) clearInterval(timeInterval)
  if (raf) cancelAnimationFrame(raf)
  window.removeEventListener('resize', onResize)
  if (renderer) {
    renderer.dispose()
    renderer.forceContextLoss()
  }
  if (scene) scene.clear()
})
</script>

<style scoped>
/* ============================================================
   ROOT
   ============================================================ */
.terminal-home {
  position: relative;
  min-height: 100vh;
  overflow-x: hidden;
  /* 透明：露出全局壁纸背景层（#bg-layer），壁纸模式下呈现天空之城主题 */
  background: transparent;
  color: #e0e0f0;
}

/* ============================================================
   HERO TERMINAL (保持与原版一致)
   ============================================================ */
.hero-terminal {
  position: relative;
  z-index: 2;
  display: flex;
  min-height: calc(100vh - 64px);
  overflow: hidden;
}

.hero-canvas {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.hud-overlay {
  position: relative;
  z-index: 10;
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 1.5rem 2rem;
  pointer-events: none;
}

.hud-overlay > * {
  pointer-events: auto;
}

/* HUD Top */
.hud-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hud-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-ring {
  width: 36px;
  height: 36px;
  border: 2px solid rgba(102, 126, 234, 0.5);
  border-radius: 50%;
  position: relative;
  animation: logoSpin 8s linear infinite;
}

.logo-ring::before {
  content: '';
  position: absolute;
  inset: 4px;
  border: 1px solid rgba(118, 75, 162, 0.5);
  border-radius: 50%;
  animation: logoSpin 5s linear infinite reverse;
}

@keyframes logoSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.logo-text {
  font-size: 1.1rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: rgba(255,255,255,0.9);
}

.logo-accent {
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hud-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.8rem;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  border-radius: 4px;
}

.status-indicator {
  width: 6px;
  height: 6px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.8);
  animation: statusPulse 2s ease-in-out infinite;
}

@keyframes statusPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: 0.7rem;
  letter-spacing: 0.2em;
  color: #10b981;
}

/* Hero Content */
.hero-terminal-container {
  position: relative;
  width: 320px;
  height: 320px;
  margin: 0 auto 0.75rem;
  animation: fadeInUp 1s ease-out;
}

.terminal-glow {
  position: absolute;
  inset: -40px;
  background: radial-gradient(circle at 50% 50%, rgba(102, 126, 234, 0.15), transparent 60%);
  pointer-events: none;
  animation: glowPulse 4s ease-in-out infinite;
  z-index: 0;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.05); }
}

/* Voice hint */
.voice-hint {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 1.25rem;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 50px;
  margin: 0.4rem 0 0;
  white-space: nowrap;
  animation: fadeInUp 1.6s ease-out;
}

.hint-icon {
  font-size: 1rem;
  color: #10b981;
  animation: micPulse 2s ease-in-out infinite;
}

@keyframes micPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.voice-hint span:last-child {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 0.03em;
}

.hero-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 1rem;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 4px;
  margin-bottom: 1rem;
  animation: fadeInDown 1s ease-out;
}

.badge-icon {
  color: #667eea;
  font-size: 0.9rem;
}

.badge-label {
  font-size: 0.7rem;
  letter-spacing: 0.2em;
  color: rgba(255,255,255,0.88);
}

.hero-subtitle {
  font-size: 0.9rem;
  letter-spacing: 0.3em;
  color: rgba(255,255,255,0.75);
  margin: 0 0 0.75rem;
  animation: fadeInUp 1.4s ease-out;
}

.scroll-hint {
  position: absolute;
  bottom: 6rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  animation: fadeIn 2s ease-out 1s both;
}

.scroll-line {
  width: 1px;
  height: 40px;
  background: linear-gradient(180deg, rgba(255,255,255,0.5), transparent);
  animation: scrollPulse 2s ease-in-out infinite;
}

@keyframes scrollPulse {
  0%, 100% { transform: scaleY(1); opacity: 1; }
  50% { transform: scaleY(0.6); opacity: 0.3; }
}

.scroll-text {
  font-size: 0.65rem;
  letter-spacing: 0.3em;
  color: rgba(255,255,255,0.6);
}

/* HUD Bottom */
.hud-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-top: 1px solid rgba(255,255,255,0.05);
}

.hud-coords {
  display: flex;
  gap: 1rem;
  font-size: 0.7rem;
  letter-spacing: 0.1em;
  color: rgba(255,255,255,0.6);
  font-family: 'Courier New', monospace;
}

.hud-coords .divider {
  color: rgba(255,255,255,0.25);
}

.hud-time {
  font-size: 0.8rem;
  letter-spacing: 0.15em;
  color: rgba(255,255,255,0.72);
  font-family: 'Courier New', monospace;
}

/* ============================================================
   SECTION SEAM: 区块衔接光带（渐变呼吸流光，柔化区块切换）
   ============================================================ */
.section-seam {
  position: relative;
  height: 110px;
  z-index: 1;
  pointer-events: none;
}

.section-seam::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(720px, 74vw);
  height: 1.5px;
  transform: translate(-50%, -50%);
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(102, 126, 234, 0.05) 12%,
    rgba(102, 126, 234, 0.5) 38%,
    rgba(0, 212, 255, 0.65) 50%,
    rgba(139, 124, 246, 0.5) 62%,
    rgba(118, 75, 162, 0.05) 88%,
    transparent 100%);
  filter: drop-shadow(0 0 10px rgba(102, 126, 234, 0.45));
  animation: seamBreath 5s ease-in-out infinite;
}

/* 流光扫过 */
.section-seam::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(720px, 74vw);
  height: 1.5px;
  transform: translate(-50%, -50%);
  background: linear-gradient(90deg,
    transparent 0%, transparent 42%,
    rgba(255, 255, 255, 0.85) 50%, 
    transparent 58%, transparent 100%);
  background-size: 220% 100%;
  mix-blend-mode: screen;
  animation: seamSweep 6s linear infinite;
}

@keyframes seamBreath {
  0%, 100% { opacity: 0.55; }
  50% { opacity: 1; }
}

@keyframes seamSweep {
  from { background-position: 130% 0; }
  to { background-position: -130% 0; }
}

/* ============================================================
   SKY CLOUDS: 天空之城云海（柔和光云缓慢漂过屏幕）
   ============================================================ */
.sky-clouds {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.cloud {
  position: absolute;
  border-radius: 50%;
  filter: blur(34px);
  background: radial-gradient(ellipse at center, rgba(185, 205, 255, 0.13), transparent 68%);
  animation: cloudFloat linear infinite;
}

.cloud-a {
  width: 55vw;
  height: 17vh;
  top: 5%;
  left: -25%;
  animation-duration: 85s;
}

.cloud-b {
  width: 44vw;
  height: 13vh;
  top: 16%;
  right: -18%;
  animation-duration: 115s;
  animation-delay: -32s;
}

.cloud-c {
  width: 38vw;
  height: 11vh;
  bottom: 10%;
  left: 28%;
  animation-duration: 70s;
  animation-delay: -52s;
}

@keyframes cloudFloat {
  0%   { transform: translateX(0); opacity: 0; }
  12%  { opacity: 1; }
  85%  { opacity: 1; }
  100% { transform: translateX(68vw); opacity: 0; }
}

/* ============================================================
   GLOBAL BACKGROUND: 暗色微光粒子网格
   ============================================================ */
.bg-field { position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s linear;
}

.bg-field-grid {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(ellipse 60% 50% at 50% 0%, rgba(118, 75, 162, 0.06), transparent 70%);
  background-size: 100% 100%;
}

.bg-point {
  position: absolute;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  opacity: 0;
  filter: drop-shadow(0 0 6px rgba(180, 210, 255, 0.45));
  animation: snowFall 12s linear infinite;
  will-change: transform, opacity;
}

@keyframes snowFall {
  0%   { opacity: 0; transform: translateY(-12vh) translateX(0); }
  12%  { opacity: 0.85; }
  50%  { opacity: 0.9; transform: translateY(46vh) translateX(10px); }
  88%  { opacity: 0.8; }
  100% { opacity: 0; transform: translateY(100vh) translateX(-8px); }
}

/* ============================================================
   FOOTER 极简双栏
   ============================================================ */
.footer {
  position: relative;
  z-index: 1;
  padding: 4.5rem 2rem 2.5rem;
  border-top: 1px solid rgba(255,255,255,0.05);
  /* 顶部渐隐融入上一区块 */
  background: linear-gradient(180deg, transparent 0%, rgba(6,6,14,0.6) 22%, #06060e 100%);
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 2rem;
  flex-wrap: wrap;
}

.footer-logo {
  font-size: 1.3rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: rgba(255,255,255,0.85);
}

.footer-slogan {
  font-size: 0.8rem;
  color: rgba(255,255,255,0.35);
  margin-top: 0.6rem;
  letter-spacing: 0.05em;
  max-width: 32ch;
}

.footer-right {
  display: flex;
  gap: 0.9rem;
}

.mail-badge {
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  background: linear-gradient(135deg, #ff6fa5, #b89cff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.social-btn:hover .mail-badge {
  background: linear-gradient(135deg, #ffd166, #ff8fb7);
  -webkit-background-clip: text;
  background-clip: text;
}

.footer-mails {
  font-family: 'Courier New', monospace;
}

.social-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: rgba(255,255,255,0.55);
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.09);
  transition: all 0.3s;
  transform: translate(var(--mx, 0), var(--my, 0));
}

.social-btn:hover {
  color: #fff;
  background: rgba(102, 126, 234, 0.15);
  border-color: rgba(102, 126, 234, 0.5);
  box-shadow: 0 0 20px rgba(102, 126, 234, 0.35);
}

.footer-bottom {
  max-width: 1200px;
  margin: 3rem auto 0;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(255,255,255,0.05);
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.72rem;
  color: rgba(255,255,255,0.6);
  letter-spacing: 0.05em;
  flex-wrap: wrap;
}

.fb-divider {
  color: rgba(255,255,255,0.25);
}

/* ============================================================
   ANIMATIONS
   ============================================================ */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-height: 700px) and (min-width: 769px) {
  .hero-terminal {
    height: auto;
    min-height: 0;
  }

  .hero-terminal-container {
    width: min(230px, 40vw);
    height: min(230px, 40vw);
  }

  .hero-badge {
    margin-bottom: 0.8rem;
  }

  .hero-subtitle {
    margin-bottom: 1.2rem;
  }

  .voice-hint {
    margin: 0 0 0.8rem;
  }

  .scroll-hint {
    display: none;
  }

  .hud-bottom {
    display: none;
  }

  .hero-content {
    padding: 1.5rem 0;
  }
}

@media (max-width: 768px) {
  .hero-terminal {
    min-height: 0;
    height: auto;
  }

  .hero-terminal-container {
    width: min(280px, 76vw);
    height: min(280px, 76vw);
  }

  .hud-bottom {
    flex-direction: column;
    gap: 0.5rem;
  }

  .hud-coords {
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.5rem;
  }

  .hero-content {
    padding: 2.5rem 0;
  }

  .voice-hint {
    font-size: 0.7rem;
    padding: 0.35rem 0.75rem;
    gap: 0.4rem;
  }

  .scroll-hint {
    display: none;
  }
  .hud-top {
    flex-wrap: wrap;
    gap: 1rem;
  }

  .hud-overlay {
    padding: 1rem;
  }

  .footer-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .footer-bottom {
    justify-content: center;
  }
}
</style>
<template>
  <div class="terminal-home">
    <!-- ============ HERO: 星际终端 (保持原有结构与 3D 场景不变) ============ -->
    <section class="hero-terminal" ref="heroSection">
      <!-- HUD Overlay -->
      <div class="hud-overlay">
        <div class="hud-top">
          <div class="hud-logo">
            <div class="logo-ring"></div>
            <span class="logo-text">{{ brandName }}</span>
          </div>
          <div class="hud-status">
            <span class="status-indicator"></span>
            <span class="status-text">ONLINE</span>
            <button class="hud-refresh-btn" @click="onHardRefresh" title="清除缓存并重新加载">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12a9 9 0 1 1-9-9c2.5 0 4.8 1 6.5 2.6L21 8" />
                <path d="M21 3v5h-5" />
              </svg>
            </button>
          </div>
        </div>

        <div class="hero-content" data-voice-target>
          <div class="hero-badge">
            <span class="badge-icon">◈</span>
            <span class="badge-label">SYSTEM INITIALIZED</span>
          </div>

          <!-- 3D TERMINAL HEAD (centerpiece) -->
          <div class="hero-terminal-container">
            <canvas ref="heroCanvas" class="hero-canvas"></canvas>
          </div>

          <p class="hero-subtitle">Aurora-星途</p>

          <!-- 问候语：IP 归属地 + 昵称，逐字打出 -->
          <WelcomeGreeting />

          <!-- 每日一句：打字机效果，哲理/诗句 + 作者 -->
          <DailyQuote />

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

      <!-- 首屏与下方内容的极光分界：三层视差波浪（借鉴 blog.cosine.ren）
           波浪以渐变填充（浪尖一线青色微光 → 底部收进区块底色 #08090f），
           三层各自露出自己的浪尖，堆出极光的层次；动画只做水平漂移 -->
      <div class="hero-wave" aria-hidden="true">
        <svg class="wave" xmlns="http://www.w3.org/2000/svg" viewBox="0 24 150 28" preserveAspectRatio="none">
          <defs>
            <path id="gh-gentle-wave"
              d="m -160,44.4 c 30,0 58,-18 87.7,-18 30.3,0 58.3,18 87.3,18 30,0 58,-18 88,-18 30,0 58,18 88,18 l 0,34.5 -351,0 z" />
            <!-- userSpaceOnUse：渐变直接对齐可视波段（viewBox 的 y 24→52 即 0%→100%），
                 否则每条 use 各按自身包围盒映射，三条浪尖的浓淡会各不相同 -->
            <linearGradient id="ghWaveGrad" gradientUnits="userSpaceOnUse" x1="0" y1="24" x2="0" y2="52">
              <stop offset="0" stop-color="#00d4ff" stop-opacity="0.32" />
              <stop offset="0.28" stop-color="#8b7cf6" stop-opacity="0.16" />
              <stop offset="0.6" stop-color="#0b1220" stop-opacity="0.94" />
              <stop offset="1" stop-color="#08090f" stop-opacity="1" />
            </linearGradient>
          </defs>
          <g class="parallax">
            <use href="#gh-gentle-wave" x="50" y="0" />
            <use href="#gh-gentle-wave" x="50" y="3" />
            <use href="#gh-gentle-wave" x="50" y="6" />
          </g>
        </svg>
      </div>
    </section>

    <!-- ============ [01] 关于我（首屏之后的主内容，深空网格与星野首屏同色系，由极光波浪分界） ============ -->
    <AboutMe />

    <!-- ============ FOOTER 极简双栏 ============ -->
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-left">
          <span class="footer-logo">{{ brandName }}</span>
          <p class="footer-slogan">{{ footerNote }}</p>
        </div>
        <div class="footer-right">
          <a v-if="githubUrl" :href="githubUrl" target="_blank" rel="noopener" class="social-btn" data-glow data-magnetic aria-label="GitHub">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 .5C5.65.5.5 5.65.5 12c0 5.08 3.29 9.39 7.86 10.91.58.11.79-.25.79-.56v-2c-3.2.7-3.87-1.54-3.87-1.54-.52-1.33-1.28-1.68-1.28-1.68-1.04-.71.08-.7.08-.7 1.15.08 1.76 1.19 1.76 1.19 1.03 1.76 2.7 1.25 3.36.96.1-.75.4-1.25.72-1.54-2.55-.29-5.23-1.28-5.23-5.68 0-1.26.45-2.28 1.19-3.09-.12-.29-.52-1.46.11-3.05 0 0 .97-.31 3.17 1.18.92-.26 1.9-.38 2.88-.39.98 0 1.96.13 2.88.39 2.2-1.49 3.17-1.18 3.17-1.18.63 1.59.23 2.76.11 3.05.74.81 1.19 1.83 1.19 3.09 0 4.41-2.69 5.38-5.25 5.67.41.35.78 1.05.78 2.12v3.14c0 .31.21.67.8.56A10.5 10.5 0 0 0 23.5 12C23.5 5.65 18.35.5 12 .5Z"/></svg>
          </a>
          <button v-if="contactEmail" type="button" class="social-btn" data-glow data-magnetic aria-label="邮箱" @click="emailOpen = true">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="3"/><path d="m2 7 10 7 10-7"/></svg>
          </button>
        </div>
      </div>
      <div class="footer-bottom">
        <span>{{ copyrightText }}</span>
        <template v-if="contactEmail">
          <span class="fb-divider">│</span>
          <button type="button" class="footer-mails footer-mail-btn" @click="emailOpen = true">{{ contactEmail }}</button>
        </template>
      </div>
    </footer>

    <!-- 回到顶部按钮 -->
    <transition name="fade-slide">
      <button v-show="showBackTop" @click="scrollToTop" class="back-to-top" aria-label="回到顶部">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 19V5M5 12l7-7 7 7"/>
        </svg>
      </button>
    </transition>

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

    <!-- 联系邮箱弹窗：点击页脚邮箱图标 / 地址弹出（替代 mailto 直接跳转） -->
    <Teleport to="body">
      <transition name="mail-pop">
        <div v-if="emailOpen" class="mail-mask" @click.self="emailOpen = false">
          <div class="mail-card" role="dialog" aria-modal="true" aria-label="联系邮箱">
            <button class="mail-close" type="button" aria-label="关闭" @click="emailOpen = false">×</button>
            <div class="mail-head">
              <div class="mail-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"><rect x="2" y="4" width="20" height="16" rx="3"/><path d="m2 7 10 7 10-7"/></svg>
              </div>
              <div class="mail-head-text">
                <h3 class="mail-title">联系我</h3>
                <p class="mail-sub">欢迎邮件交流 · 合作 / 反馈 / 交个朋友</p>
              </div>
            </div>
            <div class="mail-addr">
              <span class="mail-addr-label">邮箱地址</span>
              <span class="mail-addr-text">{{ contactEmail }}</span>
            </div>
            <div class="mail-actions">
              <button class="mail-btn is-primary" type="button" @click="copyEmail">
                {{ copyState === 'ok' ? '已复制 ✓' : copyState === 'fail' ? '请手动复制' : '复制邮箱' }}
              </button>
              <a class="mail-btn is-ghost" :href="'mailto:' + contactEmail">打开邮件客户端</a>
            </div>
            <p class="mail-tip">若浏览器未安装邮件客户端，请直接复制地址发送。</p>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
// ====================================================
// 首页（吉卜力/星尘主题）：终端 HUD 风格首屏，
// Three.js 头部模型粒子场景 + 时间刷新 + 滚动视差
// ====================================================
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import * as THREE from 'three'
import AboutMe from '@/components/home/AboutMe.vue'
import DailyQuote from '@/components/home/DailyQuote.vue'
import WelcomeGreeting from '@/components/home/WelcomeGreeting.vue'
import { useSiteSettingsStore } from '@/stores/modules/siteSettings'
import { hardRefresh } from '@/utils/hardRefresh'

// 页脚品牌 / 社交 / 版权均来自系统设置（blog_setting）
const site = useSiteSettingsStore()
const brandName = computed(() => site.brandName)
const footerNote = computed(() =>
  site.footer.footerNote || site.site.slogan || '以代码构建无限可能，让每一个想法都拥有沉浸式表达。')
const githubUrl = computed(() => site.social.github || '')
const contactEmail = computed(() => site.footer.contactEmail || (
  /^mailto:/i.test(site.social.email || '') ? site.social.email.slice(7) : (site.social.email || '')
))
const copyrightText = computed(() =>
  site.footer.copyright || `© ${new Date().getFullYear()} ${site.brandName} · 无限可能`)

// ── 联系邮箱弹窗：点击页脚邮箱图标 / 邮箱地址弹出，替代 mailto 直接跳转 ──
const emailOpen = ref(false)
// copyState: '' 待复制 → 'ok' 已复制 → 'fail' 需手动复制
const copyState = ref('')
const copied = computed(() => copyState.value === 'ok')
let copyTimer = null

// 复制邮箱：Clipboard API 失败时回退 execCommand；全部失败则提示手动复制
async function copyEmail() {
  const text = contactEmail.value || ''
  if (!text) return
  let ok = false
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
      ok = true
    }
  } catch {}
  if (!ok) {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.setAttribute('readonly', '')
      ta.style.position = 'fixed'
      ta.style.top = '-1000px'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      ta.setSelectionRange(0, text.length)
      ok = document.execCommand('copy')
      document.body.removeChild(ta)
    } catch {}
  }
  copyState.value = ok ? 'ok' : 'fail'
  clearTimeout(copyTimer)
  copyTimer = setTimeout(() => { copyState.value = '' }, 2000)
}

function onMailKeydown(e) {
  if (e.key === 'Escape') emailOpen.value = false
}

const heroSection = ref(null)
const heroCanvas = ref(null)
const bgField = ref(null)
const currentTime = ref('')
const showBackTop = ref(false)

// 清除缓存并重新加载（localStorage + sessionStorage + cookies + Cache Storage + IndexedDB + SW）
// 具体实现见 utils/hardRefresh.js，首页刷新按钮与右键菜单共用
const onHardRefresh = () => { hardRefresh() }

let scene, camera, renderer, clock
let headGroup, particles
let starEyes = []
let raf = 0
let timeInterval = 0
let canvasBox = null

// ============================================================
// 星球之眼（轻量版 FULU 电子眼）
// 借鉴 FULU_Visual_Tuner-main 电子眼工作台的视觉与行为：杏仁眼形、眼球鼓起（球冠贴面）、
// 注视限位、空闲微动与随机扫视、随机眨眼；此处全部用 Three.js 基础图元实现，
// 无贴图 / 无 GLSL / 无外部依赖，开销极小。
//   · 眼形：球体统一压扁成透镜（scale 0.86×0.62），上下缘深色弧线勾勒杏仁轮廓
//   · 虹膜：分层球冠贴合眼球表面（角膜环→极光紫→冰青→瞳孔→双高光），偏转时永不穿模
//   · 注视：虹膜组绕眼球中心偏转，弹簧阻尼跟随鼠标，并限制在 MAX_TILT 内
//   · 眨眼：随机间隔整眼纵向压缩，快速闭合后回弹
// ============================================================
const EYE_R = 0.098            // 眼球半径（放大，星球上更醒目）
const EYE_FLAT_Y = 0.86        // 纵向压扁（杏仁感）
const EYE_FLAT_Z = 0.62        // 前后压扁（透镜感）
const IRIS_R = 0.040           // 虹膜半径（放大，反差更清晰）
const MAX_TILT = 0.42          // 注视最大偏角（弧度，模拟眼内限位）
const EYE_POSITIONS = [
  { x: -0.235, y: 0.165, z: 0.93 },
  { x: 0.235, y: 0.165, z: 0.93 }
]

// 构建双眼并挂到头部模型上，返回运行时状态数组
function buildStarEyes(parent) {
  const ballMat = new THREE.MeshBasicMaterial({ color: 0xf7f9ff })
  const rimMat = new THREE.MeshBasicMaterial({ color: 0x111634 })
  const limbalMat = new THREE.MeshBasicMaterial({ color: 0x141838 })   // 角膜环（瞳缘深色圈）
  const irisMat = new THREE.MeshBasicMaterial({ color: 0x7c5cff })     // 虹膜外圈：极光紫
  const irisInnerMat = new THREE.MeshBasicMaterial({ color: 0x2ee6f6 }) // 虹膜内圈：冰青
  const pupilMat = new THREE.MeshBasicMaterial({ color: 0x05070f })
  const sparkMat = new THREE.MeshBasicMaterial({ color: 0xffffff })
  const capAngle = Math.asin(IRIS_R / EYE_R)   // 虹膜在眼球上的半张角
  const arcGeo = new THREE.TorusGeometry(EYE_R, 0.0055, 5, 24, Math.PI * 0.92)
  const result = []

  for (const p of EYE_POSITIONS) {
    const group = new THREE.Group()
    group.position.set(p.x, p.y, p.z)
    group.scale.set(1, EYE_FLAT_Y, EYE_FLAT_Z)

    // 眼球：完整球体，统一缩放后自然成为透镜形（球冠虹膜始终贴合其表面）
    group.add(new THREE.Mesh(new THREE.SphereGeometry(EYE_R, 24, 16), ballMat))

    // 上下缘曲线：深色弧线勾勒杏仁轮廓
    const upper = new THREE.Mesh(arcGeo, rimMat)
    upper.position.z = 0.008
    upper.rotation.z = Math.PI * 0.04
    group.add(upper)
    const lower = new THREE.Mesh(arcGeo, rimMat)
    lower.position.z = 0.008
    lower.rotation.z = Math.PI * 0.96
    group.add(lower)

    // 注视枢轴：绕眼球中心偏转 → 虹膜/瞳孔/高光整体滑动且不穿模
    // 分层球冠（半径由内到外递增，确保逐层盖在更外层）：
    //   角膜环 → 虹膜紫 → 虹膜青 → 瞳孔 → 双高光
    const gaze = new THREE.Group()
    gaze.rotation.x = Math.PI / 2
    group.add(gaze)
    gaze.add(new THREE.Mesh(
      new THREE.SphereGeometry(EYE_R * 1.006, 22, 8, 0, Math.PI * 2, 0, capAngle * 1.16), limbalMat))
    gaze.add(new THREE.Mesh(
      new THREE.SphereGeometry(EYE_R * 1.012, 22, 9, 0, Math.PI * 2, 0, capAngle * 1.0), irisMat))
    gaze.add(new THREE.Mesh(
      new THREE.SphereGeometry(EYE_R * 1.018, 22, 9, 0, Math.PI * 2, 0, capAngle * 0.66), irisInnerMat))
    gaze.add(new THREE.Mesh(
      new THREE.SphereGeometry(EYE_R * 1.025, 18, 7, 0, Math.PI * 2, 0, capAngle * 0.40), pupilMat))
    // 主高光（左上大片）
    const spark = new THREE.Mesh(
      new THREE.SphereGeometry(EYE_R * 1.032, 14, 6, 0, Math.PI * 2, 0, capAngle * 0.30), sparkMat)
    spark.rotation.set(-capAngle * 0.55, 0, -capAngle * 0.75)
    gaze.add(spark)
    // 副高光（右下小点，让眼更有神）
    const spark2 = new THREE.Mesh(
      new THREE.SphereGeometry(EYE_R * 1.030, 10, 5, 0, Math.PI * 2, 0, capAngle * 0.14), sparkMat)
    spark2.rotation.set(capAngle * 0.62, 0, capAngle * 0.85)
    gaze.add(spark2)

    // 外圈光晕：极光紫，随呼吸轻微脉动
    const ring = new THREE.Mesh(
      new THREE.RingGeometry(EYE_R * 1.18, EYE_R * 1.56, 28),
      new THREE.MeshBasicMaterial({ color: 0xa855f7, transparent: true, opacity: 0.26, side: THREE.DoubleSide }))
    ring.position.z = 0.02
    group.add(ring)

    parent.add(group)
    result.push({
      group, gaze, ring,
      tiltX: 0, tiltY: 0, vx: 0, vy: 0,
      sacX: 0, sacY: 0, nextSaccade: 1 + Math.random() * 3,
      nextBlink: 1.5 + Math.random() * 4, blinkPhase: -1
    })
  }
  return result
}

// 鼠标位置归一化（-1~1，屏幕 Y 向上为正），供注视方向使用
let pointerNX = 0
let pointerNY = 0
function onPointerMove(e) {
  const rect = heroCanvas.value?.getBoundingClientRect?.()
  if (!rect || !rect.width) return
  pointerNX = Math.max(-1, Math.min(1, (e.clientX - rect.left - rect.width / 2) / (rect.width * 0.9)))
  pointerNY = Math.max(-1, Math.min(1, (rect.height / 2 - (e.clientY - rect.top)) / (rect.height * 0.9)))
}

// 每帧驱动：注视弹簧阻尼 + 随机扫视 + 随机眨眼 + 光晕脉动
function updateStarEyes(dt, t) {
  if (!starEyes.length) return
  const d = Math.min(dt, 0.05)
  const K = 55        // 弹簧刚度
  const C = 9         // 阻尼系数
  for (const e of starEyes) {
    // 目标注视角：鼠标方向 + 空闲微动
    let tx = pointerNX * MAX_TILT
    let ty = pointerNY * MAX_TILT + Math.sin(t * 0.7 + e.group.position.x) * 0.03

    // 随机扫视：偶尔快速瞥向别处，随后指数衰减回位
    e.nextSaccade -= d
    if (e.nextSaccade <= 0) {
      e.nextSaccade = 1.6 + Math.random() * 3.2
      e.sacX = (Math.random() - 0.5) * MAX_TILT * 1.3
      e.sacY = (Math.random() - 0.5) * MAX_TILT * 0.7
    }
    const decay = Math.pow(0.02, d)
    e.sacX *= decay
    e.sacY *= decay
    tx += e.sacX
    ty += e.sacY
    tx = Math.max(-MAX_TILT, Math.min(MAX_TILT, tx))
    ty = Math.max(-MAX_TILT, Math.min(MAX_TILT, ty))

    // 弹簧阻尼跟随
    e.vx += (tx - e.tiltX) * K * d - e.vx * C * d
    e.vy += (ty - e.tiltY) * K * d - e.vy * C * d
    e.tiltX += e.vx * d
    e.tiltY += e.vy * d
    e.gaze.rotation.z = -e.tiltX
    e.gaze.rotation.x = Math.PI / 2 - e.tiltY

    // 眨眼：约 0.3s 完成闭合与回弹
    e.nextBlink -= d
    if (e.nextBlink <= 0) {
      e.nextBlink = 2.5 + Math.random() * 4.5
      e.blinkPhase = 0
    }
    let squash = 1
    if (e.blinkPhase >= 0) {
      e.blinkPhase += d
      const P = 0.3
      if (e.blinkPhase < P) {
        squash = 1 - Math.pow(Math.sin((e.blinkPhase / P) * Math.PI), 0.7) * 0.92
      } else {
        e.blinkPhase = -1
      }
    }
    e.group.scale.y = EYE_FLAT_Y * squash
    e.ring.material.opacity = 0.16 + Math.sin(t * 1.6) * 0.1
  }
}

// ============================================================
// Three.js 场景：粒子人头 + 轨道环（原有，保持不变）
// ============================================================

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

  // preserveDrawingBuffer: true —— 修复路由跳转瞬间星球星区闪现"白色盒子"：
  // 默认 false 时 Chromium 在祖先 opacity 过渡把画布提升为合成层的第 1-2 帧
  // 会因纹理未就绪而渲染成白块；保持绘图缓冲可让合成器始终拿到有效像素
  renderer = new THREE.WebGLRenderer({ canvas, antialias: false, alpha: true, preserveDrawingBuffer: true })
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

  // ── 星球之眼：轻量版「FULU 电子眼」（替代原两个青色圆球 + 圆环）──
  starEyes = buildStarEyes(headGroup)

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

  // 2026-09-23：移除星球外围的 3 个 torus 大光环，
  // hero-canvas 除转动星球外保持 100% 透明

  // 简易时钟（避免 THREE.Clock 弃用警告）
  clock = (() => { const s = performance.now(); let l = s; return { getDelta() { const n = performance.now(); const d = (n - l) / 1000; l = n; return d; }, getElapsedTime() { return (performance.now() - s) / 1000; } } })()
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

  camera.position.z = 4.5 + ease * 8

  // 星球之眼：注视跟随 / 扫视 / 眨眼 / 光晕脉动
  updateStarEyes(dt, t)

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

// 滚动监听：控制"回到顶部"按钮的显隐
function handleScroll() {
  showBackTop.value = window.scrollY > 400
}

// 平滑滚动回顶部
function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 挂载时启动时间刷新、初始化 Three.js 场景并监听窗口缩放
onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)

  initThreeScene()
  window.addEventListener('resize', onResize)
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('keydown', onMailKeydown)
  window.addEventListener('pointermove', onPointerMove, { passive: true })
})

// 卸载时清理定时器、动画帧、渲染器与场景资源
onBeforeUnmount(() => {
  if (timeInterval) clearInterval(timeInterval)
  if (raf) cancelAnimationFrame(raf)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('keydown', onMailKeydown)
  window.removeEventListener('pointermove', onPointerMove)
  clearTimeout(copyTimer)
  if (renderer) {
    renderer.dispose()
    renderer.forceContextLoss()
  }
  if (scene) scene.clear()
  starEyes = []
})

// 路由离开瞬间（早于过渡类名）：先停渲染并同步移出渲染树，
// 从根本上杜绝 WebGL 画布在过渡合成阶段的白框闪现
onBeforeRouteLeave(() => {
  if (raf) cancelAnimationFrame(raf)
  raf = 0
  const c = heroCanvas.value
  if (c) c.style.display = 'none'
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
  /* 波浪高度：HUD 底栏据此上抬，避免时间/坐标被极光波浪压住 */
  --wave-h: clamp(64px, 8vw, 132px);
}

/* ============================================================
   首屏 → 内容区的极光分界（借鉴 blog.cosine.ren 的三层视差波浪）
   路径用一个 4 周期正弦（gentle-wave），preserveAspectRatio="none" 横向拉满；
   三层 use 分别以 14s / 9s / 6s 做水平漂移，最下层完全不透明压住首屏底边。
   填充用 ghWaveGrad：浪尖一线青色微光、中段转紫、底部收进区块底色 #08090f，
   于是「星野首屏」与「网格内容区」之间是一条会流动的极光岸线
   ============================================================ */
.hero-wave {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: var(--wave-h);
  z-index: 5;                 /* 在 canvas(1) 之上、HUD(10) 之下：不遮挡交互 */
  pointer-events: none;
}
.hero-wave .wave {
  display: block;
  width: 100%;
  height: 100%;
}
.hero-wave .parallax > use { animation: waveDrift 12s linear infinite; }
/* 后层压淡、前层最实：三层浪尖依次显影，堆出纵深 */
.hero-wave .parallax > use:nth-child(1) {
  fill: url(#ghWaveGrad); opacity: 0.5; animation-duration: 14s; animation-delay: -2s;
}
.hero-wave .parallax > use:nth-child(2) {
  fill: url(#ghWaveGrad); opacity: 0.78; animation-duration: 9s; animation-delay: -3s;
}
.hero-wave .parallax > use:nth-child(3) {
  fill: url(#ghWaveGrad); opacity: 1; animation-duration: 6s; animation-delay: -4s;
}
@keyframes waveDrift {
  0%   { transform: translateX(-90px); }
  100% { transform: translateX(85px); }
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
  /* 底边留出波浪的高度，让时间/坐标栏落在纸色岸线之上 */
  padding: 1.5rem 2rem calc(1rem + var(--wave-h) * 0.72);
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

.hud-refresh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px;
  margin-left: 0.3rem;
  color: rgba(16, 185, 129, 0.7);
  background: transparent;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s;
}
.hud-refresh-btn:hover {
  color: #10b981;
  background: rgba(16, 185, 129, 0.15);
  transform: rotate(180deg);
}
.hud-refresh-btn:active {
  transform: rotate(360deg);
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
  /* 2026-09-23：hero-canvas 除转动星球外 100% 透明，不再加深色径向基底 */
  background: transparent;
  border-radius: 50%;
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
  /* 抬高到波浪之上，避免「SCROLL TO EXPLORE」被纸色岸线吞掉 */
  bottom: calc(var(--wave-h) + 2.2rem);
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

/* 邮箱地址文字按钮：保持原有观感，仅增加可点击反馈 */
.footer-mail-btn {
  background: none;
  border: 0;
  padding: 0;
  font: inherit;
  color: inherit;
  letter-spacing: inherit;
  cursor: pointer;
  transition: color 0.25s ease;
}
.footer-mail-btn:hover { color: #b89cff; text-decoration: underline; text-underline-offset: 3px; }

.social-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  padding: 0;
  border-radius: 12px;
  color: rgba(255,255,255,0.55);
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.09);
  transition: all 0.3s;
  transform: translate(var(--mx, 0), var(--my, 0));
  cursor: pointer;
}

/* ── 联系邮箱弹窗（与站点星光/极光主题一致） ── */
.mail-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(3, 3, 12, 0.66);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.mail-card {
  position: relative;
  width: 100%;
  max-width: 390px;
  padding: 1.6rem 1.5rem 1.3rem;
  border-radius: 1.15rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(160deg, rgba(20, 18, 48, 0.96), rgba(8, 8, 22, 0.98));
  box-shadow: 0 30px 64px -20px rgba(0, 0, 0, 0.8), inset 0 1px 0 rgba(255, 255, 255, 0.07);
  overflow: hidden;
}
.mail-card::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(130px 130px at 10% 0%, rgba(184, 156, 255, 0.28), transparent 70%),
              radial-gradient(150px 150px at 100% 100%, rgba(255, 111, 165, 0.18), transparent 70%),
              radial-gradient(140px 140px at 80% 10%, rgba(103, 232, 249, 0.14), transparent 70%);
}
.mail-close {
  position: absolute;
  top: 0.55rem;
  right: 0.7rem;
  z-index: 2;
  width: 1.9rem;
  height: 1.9rem;
  border-radius: 9999px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.7);
  font-size: 1.1rem;
  line-height: 1;
  cursor: pointer;
  transition: all 0.25s ease;
}
.mail-close:hover { color: #fff; border-color: rgba(255, 255, 255, 0.3); transform: rotate(90deg); }
.mail-head { position: relative; display: flex; align-items: center; gap: 0.8rem; margin-bottom: 1.15rem; }
.mail-icon {
  width: 2.75rem;
  height: 2.75rem;
  flex-shrink: 0;
  border-radius: 0.85rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #66b0ff, #b89cff 50%, #ff6fa5);
  box-shadow: 0 0 22px rgba(184, 156, 255, 0.5);
}
.mail-title { margin: 0; font-size: 1.08rem; font-weight: 700; color: #fff; letter-spacing: 0.02em; }
.mail-sub { margin: 0.2rem 0 0; font-size: 0.72rem; color: rgba(255, 255, 255, 0.42); letter-spacing: 0.03em; }
.mail-addr {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  padding: 0.8rem 0.95rem;
  border-radius: 0.75rem;
  border: 1px dashed rgba(184, 156, 255, 0.35);
  background: rgba(184, 156, 255, 0.06);
}
.mail-addr-label { font-size: 0.62rem; letter-spacing: 0.16em; text-transform: uppercase; color: rgba(184, 156, 255, 0.8); }
.mail-addr-text { font-size: 0.92rem; font-weight: 600; color: #eaeaff; word-break: break-all; font-family: 'Courier New', monospace; }
.mail-actions { position: relative; display: flex; gap: 0.6rem; margin-top: 1.15rem; }
.mail-btn {
  flex: 1;
  text-align: center;
  padding: 0.6rem 0.9rem;
  border-radius: 0.75rem;
  font-size: 0.82rem;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.25s ease;
}
.mail-btn.is-primary {
  color: #0d0a1f;
  background: linear-gradient(135deg, #b89cff, #66b0ff);
  box-shadow: 0 10px 24px -10px rgba(184, 156, 255, 0.7);
}
.mail-btn.is-primary:hover { transform: translateY(-1px); filter: brightness(1.08); }
.mail-btn.is-ghost {
  color: rgba(255, 255, 255, 0.8);
  border-color: rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.03);
}
.mail-btn.is-ghost:hover { color: #fff; border-color: rgba(255, 255, 255, 0.32); background: rgba(255, 255, 255, 0.07); }
.mail-tip { position: relative; margin: 0.9rem 0 0; font-size: 0.66rem; color: rgba(255, 255, 255, 0.32); text-align: center; }

.mail-pop-enter-active { transition: opacity 0.22s ease; }
.mail-pop-leave-active { transition: opacity 0.18s ease; }
.mail-pop-enter-from, .mail-pop-leave-to { opacity: 0; }
.mail-pop-enter-active .mail-card { animation: mailCardIn 0.32s cubic-bezier(0.22, 1, 0.36, 1); }
.mail-pop-leave-active .mail-card { transition: transform 0.18s ease; transform: scale(0.96); }
@keyframes mailCardIn {
  from { opacity: 0; transform: translateY(16px) scale(0.94); }
  to { opacity: 1; transform: translateY(0) scale(1); }
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
    /* 小屏同样要为波浪留出底边，否则坐标为纸色岸线所盖 */
    padding: 1rem 1rem calc(0.5rem + var(--wave-h) * 0.72);
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

/* ============================================================
   BACK TO TOP: 回到顶部按钮
   ============================================================ */
.back-to-top {
  position: fixed;
  right: 24px;
  bottom: 80px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(34,211,238,0.2), rgba(34,211,238,0.05));
  border: 1px solid rgba(34,211,238,0.3);
  color: #67e8f9;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 100;
  backdrop-filter: blur(8px);
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(34,211,238,0.15);
}
.back-to-top:hover {
  background: linear-gradient(135deg, rgba(34,211,238,0.35), rgba(34,211,238,0.1));
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(34,211,238,0.3);
}
.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from, .fade-slide-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
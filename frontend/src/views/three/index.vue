<template>
  <div class="three-page">
    <!-- HERO + 3D 粒子空间 -->
    <section class="hero" ref="hero">
      <div class="hero-back"><PageBack label="返回首页" to="/" /></div>
      <div class="canvas-wrap"><canvas ref="sceneCanvas"></canvas></div>
      <div class="hero-inner">
        <div class="badge fade">● WEBGL · GPU SHADER · 30000 PARTICLES</div>
        <h1 class="title fade">3D 粒子空间</h1>
        <p class="sub fade">
          全部粒子运算在 GPU 顶点着色器完成：滚动在星云 / 星系 / 环结 / 波场四种形态间平滑 morph，
          鼠标是一道力场，拨开光尘；下方按钮或页面滚动均可切换形态。
        </p>
        <div class="tags fade">
          <span v-for="t in heroTags" :key="t">#{{ t }}</span>
        </div>
        <div class="hint fade">● 滚动改变粒子形态 · 移动鼠标扰动星尘</div>
      </div>

      <!-- 控制面板：形态 / 主题 / 性能读数 -->
      <div class="control-panel fade">
        <div class="cp-row">
          <span class="cp-label">形态</span>
          <div class="cp-shapes">
            <button v-for="(s, i) in shapeList" :key="s.name" :class="['cp-shape', { on: activeShape === i }]"
                    :title="s.name" @click="gotoShape(i)">
              <span class="cp-dot"></span>{{ s.name }}
            </button>
          </div>
        </div>
        <div class="cp-row">
          <span class="cp-label">色系</span>
          <div class="cp-themes">
            <button v-for="t in themes" :key="t.name" :class="['cp-theme', { on: theme === t.name }]"
                    :style="{ background: `linear-gradient(135deg, ${t.colors[0]}, ${t.colors[1]} 55%, ${t.colors[2]})` }"
                    :title="t.name" @click="setTheme(t.name)"></button>
          </div>
        </div>
        <div class="cp-meta"><span>{{ particleCount.toLocaleString() }} particles</span><span>{{ fps }} FPS</span></div>
      </div>
    </section>

    <!-- 形态说明 -->
    <section class="modes">
      <div class="sec-head">
        <span class="sec-num">[ 01 ]</span>
        <h2>粒子形态</h2>
        <p>PARTICLE MODES</p>
      </div>
      <div class="mode-grid">
        <div v-for="(m, i) in shapeList" :key="m.name" class="mode-card" @click="gotoShape(i)">
          <div class="mode-icon">{{ m.icon }}</div>
          <h3>{{ m.name }}</h3>
          <p>{{ m.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 技术架构说明 -->
    <section class="gesture">
      <div class="sec-head">
        <span class="sec-num">[ 02 ]</span>
        <h2>渲染管线</h2>
        <p>RENDER PIPELINE</p>
      </div>
      <div class="gesture-flow">
        <div v-for="(g, i) in pipelineSteps" :key="g.name" class="gesture-step">
          <div class="g-index">0{{ i + 1 }}</div>
          <h3>{{ g.name }}</h3>
          <p>{{ g.desc }}</p>
        </div>
      </div>
      <div class="gesture-note">
        <span class="dot"></span>
        30,000 粒子全程 GPU 计算，CPU 每帧仅更新 Uniforms；移动端自动降载至 9,000。
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
// 3D 粒子空间（GPU Shader 版）
// - 4 种形态位置预烘焙进 attributes，顶点着色器按 uMorph 混合
// - 鼠标力场 / 呼吸噪声 / 主题配色全部在 shader 内完成
// - 滚动进度 ↔ 形态索引双向联动，按钮点击平滑滚动同步
// ====================================================
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as THREE from 'three'
import PageBack from '@/components/PageBack.vue'

const hero = ref(null)
const sceneCanvas = ref(null)

const heroTags = ['Three.js', 'GLSL', 'GPU Particles', 'Morph']

const shapeList = [
  { icon: '🌌', name: '星云', desc: '球壳星云：粒子按高斯噪声散布在半径 3.2 的球壳上，自带呼吸起伏' },
  { icon: '🌀', name: '星系', desc: '螺旋星系：三条旋臂随半径自然缠绕，盘面厚度向外递减' },
  { icon: '🪢', name: '环结', desc: '三叶环面结：p2/q3 纽结曲线沿管径随机分布，如星轨编织' },
  { icon: '🌊', name: '波场', desc: '干涉波场：网格面上双正弦波叠加，粒子如光尘悬浮于海面' },
]

const pipelineSteps = [
  { name: '预烘焙形态', desc: '四种形态各 3 万个目标坐标，一次性写入 BufferGeometry attributes' },
  { name: '顶点着色器', desc: 'uMorph 加权混合形态 + 呼吸噪声 + 鼠标力场，全部 GPU 并行' },
  { name: '加性混合', desc: 'AdditiveBlending + 幂次衰减软圆点，密集处自然辉光' },
  { name: '联动与降载', desc: '滚动驱动 uMorph，页面隐藏暂停渲染，移动端粒子数自动减半' },
]

const themes = [
  { name: 'aurora', colors: ['#67e8f9', '#818cf8', '#f0abfc'] },
  { name: 'cyber', colors: ['#00d4ff', '#667eea', '#764ba2'] },
  { name: 'ember', colors: ['#fbbf24', '#f43f5e', '#a855f7'] },
  { name: 'ice', colors: ['#e0f2fe', '#38bdf8', '#6366f1'] },
]

let renderer = null
let scene = null
let camera = null
let particles = null
let material = null
let rafId = null
let clock = null
let fpsTimer = null

const particleCount = ref(30000)
const fps = ref(60)
const activeShape = ref(0)
const theme = ref('aurora')

const isMobile = typeof window !== 'undefined' && (window.innerWidth < 768 || window.matchMedia('(pointer: coarse)').matches)

// ---------------- 形态生成器 ----------------
// 返回 [sphere, galaxy, knot, wave] 四组 Float32Array
function buildShapes(count) {
  const sphere = new Float32Array(count * 3)
  const galaxy = new Float32Array(count * 3)
  const knot = new Float32Array(count * 3)
  const wave = new Float32Array(count * 3)
  const R = 3.2

  for (let i = 0; i < count; i++) {
    // 1. 球壳星云：球面均匀采样 + 轻微厚度
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    const r = R + (Math.random() - 0.5) * 0.9
    sphere[i * 3] = r * Math.sin(phi) * Math.cos(theta)
    sphere[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta) * 0.86
    sphere[i * 3 + 2] = r * Math.cos(phi)

    // 2. 螺旋星系：3 条旋臂 + 中心核球
    const arm = i % 3
    const rad = Math.pow(Math.random(), 0.65) * 4.4 + 0.12
    const spin = rad * 1.15
    const angle = arm * (Math.PI * 2 / 3) + spin + (Math.random() - 0.5) * 0.35
    const thickness = 0.42 * (1 - rad / 5.2)
    galaxy[i * 3] = Math.cos(angle) * rad + gauss() * 0.08
    galaxy[i * 3 + 1] = gauss() * thickness
    galaxy[i * 3 + 2] = Math.sin(angle) * rad + gauss() * 0.08

    // 3. 三叶环面结 (p=2, q=3)：沿曲线 + 管径圆截面
    const t = Math.random() * Math.PI * 2
    const p = 2, q = 3, tube = 0.52
    const cx = Math.sin(p * t) * 2.5
    const cy = Math.sin(q * t) * 0.9
    const cz = Math.cos(p * t) * 2.5
    const a = Math.random() * Math.PI * 2
    const rr = tube * Math.sqrt(Math.random())
    const nx = Math.sin(q * t + Math.PI / 2) // 简化法向扰动
    knot[i * 3] = cx + Math.cos(a) * rr + nx * rr * 0.3
    knot[i * 3 + 1] = cy + Math.sin(a) * rr
    knot[i * 3 + 2] = cz + (Math.random() - 0.5) * rr * 0.6

    // 4. 干涉波场：网格 + 双正弦叠加
    const gx = (Math.random() - 0.5) * 9
    const gz = (Math.random() - 0.5) * 9
    wave[i * 3] = gx
    wave[i * 3 + 1] = Math.sin(gx * 1.35) * Math.cos(gz * 1.35) * 0.7 + Math.sin(gx * 0.5 + gz * 0.8) * 0.25
    wave[i * 3 + 2] = gz
  }
  return [sphere, galaxy, knot, wave]
}

// 近似高斯分布（Box-Muller 简化版）
function gauss() { return (Math.random() + Math.random() + Math.random() - 1.5) * 0.816 }

// ---------------- 场景初始化 ----------------
function initThree() {
  const canvas = sceneCanvas.value
  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true, preserveDrawingBuffer: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(58, canvas.clientWidth / canvas.clientHeight, 0.1, 100)
  camera.position.z = 9
  clock = new THREE.Clock()

  const count = isMobile ? 9000 : 30000
  particleCount.value = count
  const [sphere, galaxy, knot, wave] = buildShapes(count)

  const aRand = new Float32Array(count)
  for (let i = 0; i < count; i++) aRand[i] = Math.random()

  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(sphere, 3)) // position 仅为占位（Frustum 剔除用）
  geo.setAttribute('aShape1', new THREE.BufferAttribute(galaxy, 3))
  geo.setAttribute('aShape2', new THREE.BufferAttribute(knot, 3))
  geo.setAttribute('aShape3', new THREE.BufferAttribute(wave, 3))
  geo.setAttribute('aRand', new THREE.BufferAttribute(aRand, 1))
  // 大位移粒子禁用视锥剔除，避免 morph 中整体消失
  geo.boundingSphere = new THREE.Sphere(new THREE.Vector3(), 30)

  material = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    uniforms: {
      uTime: { value: 0 },
      uMorph: { value: 0 },
      uSize: { value: isMobile ? 26 : 17 },
      uPixelRatio: { value: Math.min(window.devicePixelRatio, 2) },
      uMouse: { value: new THREE.Vector3(99, 99, 99) },
      uColorA: { value: new THREE.Color(themes[0].colors[0]) },
      uColorB: { value: new THREE.Color(themes[0].colors[1]) },
      uColorC: { value: new THREE.Color(themes[0].colors[2]) },
    },
    vertexShader: /* glsl */ `
      attribute vec3 aShape1;
      attribute vec3 aShape2;
      attribute vec3 aShape3;
      attribute float aRand;
      uniform float uTime;
      uniform float uMorph;
      uniform float uSize;
      uniform float uPixelRatio;
      uniform vec3 uMouse;
      varying float vMix;
      varying float vGlow;

      void main() {
        // ── 形态混合：uMorph ∈ [0,3] 相邻两形态间平滑过渡 ──
        vec3 target = position;
        target = mix(target, aShape1, clamp(uMorph, 0.0, 1.0));
        target = mix(target, aShape2, clamp(uMorph - 1.0, 0.0, 1.0));
        target = mix(target, aShape3, clamp(uMorph - 2.0, 0.0, 1.0));

        // ── 呼吸噪声：整体缓慢起伏，粒子有生命感 ──
        float breath = sin(uTime * 0.7 + aRand * 31.4 + target.y * 1.4) * 0.055;
        target += normalize(target + 0.001) * breath;

        // ── 鼠标力场：靠近指针的粒子被推开，柔顺回弹 ──
        vec3 dir = target - uMouse;
        float d = length(dir);
        float force = smoothstep(2.4, 0.0, d);
        target += (dir / max(d, 0.001)) * force * force * 1.15;

        // ── 颜色混合因子：按半径 + 高度 + 随机抖动 ──
        vMix = clamp(length(target) * 0.16 + target.y * 0.09 + aRand * 0.28, 0.0, 1.0);
        vGlow = aRand;

        vec4 mv = modelViewMatrix * vec4(target, 1.0);
        gl_PointSize = uSize * (0.55 + aRand * 0.75) * uPixelRatio / max(-mv.z, 0.1);
        gl_Position = projectionMatrix * mv;
      }
    `,
    fragmentShader: /* glsl */ `
      uniform vec3 uColorA;
      uniform vec3 uColorB;
      uniform vec3 uColorC;
      varying float vMix;
      varying float vGlow;

      void main() {
        // 软圆点：中心亮、幂次衰减到边缘，加性混合自然成辉光
        vec2 uv = gl_PointCoord - 0.5;
        float d = length(uv) * 2.0;
        if (d > 1.0) discard;
        float alpha = pow(1.0 - d, 2.6);
        vec3 color = mix(uColorA, uColorB, smoothstep(0.0, 0.55, vMix));
        color = mix(color, uColorC, smoothstep(0.5, 1.0, vMix));
        color += vGlow * 0.18;
        gl_FragColor = vec4(color, alpha * 0.85);
      }
    `,
  })

  particles = new THREE.Points(geo, material)
  scene.add(particles)

  animate()
  fpsTimer = setInterval(() => { fps.value = frames * 2; frames = 0 }, 500)
}

// ---------------- 交互与联动 ----------------
let mouseX = 0, mouseY = 0, scrollMorph = 0
let frames = 0
let raycaster = null, planeZ = null

function onMouse(e) {
  mouseX = (e.clientX / window.innerWidth) * 2 - 1
  mouseY = -((e.clientY / window.innerHeight) * 2 - 1)
}

// 鼠标射线与 z=0 平面求交 → 粒子对象空间，作为力场中心
function updateMouseWorld() {
  if (!raycaster) return
  raycaster.setFromCamera(new THREE.Vector2(mouseX, mouseY), camera)
  const hit = new THREE.Vector3()
  raycaster.ray.intersectPlane(planeZ, hit)
  // 粒子自身在旋转，需把世界坐标逆旋转回对象空间
  hit.applyQuaternion(particles.quaternion.clone().invert())
  material.uniforms.uMouse.value.lerp(hit, 0.18)
}

// 滚动映射：整个形态区间压缩在首屏（hero）内完成，
// 用户在画布可见范围内就能看到全部 4 种形态的渐变
function heroH() { return hero.value ? hero.value.offsetHeight * 0.92 : window.innerHeight }

function onScroll() {
  scrollMorph = Math.min(1, window.scrollY / Math.max(1, heroH()))
}

// 形态按钮 → 平滑滚动到 hero 内对应进度（滚动驱动 morph，按钮与滚动永远一致）
function gotoShape(i) {
  const top = (i / (shapeList.length - 1)) * heroH()
  window.scrollTo({ top, behavior: 'smooth' })
}

function setTheme(name) {
  theme.value = name
  const t = themes.find(x => x.name === name)
  if (!material) return
  material.uniforms.uColorA.value.set(t.colors[0])
  material.uniforms.uColorB.value.set(t.colors[1])
  material.uniforms.uColorC.value.set(t.colors[2])
}

// ---------------- 渲染循环 ----------------
function animate() {
  rafId = requestAnimationFrame(animate)
  frames++
  const t = clock.getElapsedTime()
  material.uniforms.uTime.value = t

  // 滚动 → 目标形态，逐帧缓动
  const targetMorph = scrollMorph * (shapeList.length - 1)
  const m = material.uniforms.uMorph.value
  material.uniforms.uMorph.value = m + (targetMorph - m) * 0.055
  activeShape.value = Math.round(targetMorph)

  // 相机视差 + 粒子慢速自转
  camera.position.x += (mouseX * 0.7 - camera.position.x) * 0.04
  camera.position.y += (mouseY * 0.5 - camera.position.y) * 0.04
  camera.lookAt(0, 0, 0)
  particles.rotation.y = t * 0.06
  particles.rotation.x = Math.sin(t * 0.1) * 0.1

  updateMouseWorld()
  renderer.render(scene, camera)
}

function resize() {
  const canvas = sceneCanvas.value
  if (!canvas || !renderer) return
  const w = canvas.clientWidth
  const h = canvas.clientHeight
  renderer.setSize(w, h, false)
  camera.aspect = w / h
  camera.updateProjectionMatrix()
}

// 页面不可见时暂停渲染，节电省 GPU
function onVisibility() {
  if (document.hidden) {
    if (rafId) { cancelAnimationFrame(rafId); rafId = null }
  } else if (!rafId && material) {
    animate()
  }
}

onMounted(() => {
  initThree()
  raycaster = new THREE.Raycaster()
  planeZ = new THREE.Plane(new THREE.Vector3(0, 0, 1), 0)

  window.addEventListener('mousemove', onMouse, { passive: true })
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', resize)
  document.addEventListener('visibilitychange', onVisibility)
  onScroll()
  resize()
})

onBeforeUnmount(() => {
  if (rafId) cancelAnimationFrame(rafId)
  if (fpsTimer) clearInterval(fpsTimer)
  window.removeEventListener('mousemove', onMouse)
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', resize)
  document.removeEventListener('visibilitychange', onVisibility)
  if (particles) { scene.remove(particles); particles.geometry.dispose() }
  if (material) material.dispose()
  if (renderer) { renderer.dispose(); renderer = null }
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
.sub { font-size: 1.02rem; line-height: 1.9; color: rgba(255,255,255,0.82); max-width: 640px; margin: 0 auto 1.6rem; }
.tags { display: flex; gap: 0.6rem; justify-content: center; flex-wrap: wrap; margin-bottom: 2rem; pointer-events: auto; }
.tags span { font-size: 0.72rem; font-family: monospace; color: #7fd9ff; padding: 0.3rem 0.8rem; border: 1px solid rgba(0, 212, 255, 0.25); border-radius: 999px; background: rgba(0, 212, 255, 0.06); }
.hint { font-size: 0.78rem; color: rgba(255,255,255,0.6); letter-spacing: 0.2em; animation: hintPulse 2.4s ease-in-out infinite; }
@keyframes hintPulse { 0%, 100% { opacity: 0.6; } 50% { opacity: 1; } }

/* 控制面板 */
.control-panel {
  position: absolute; bottom: 22px; left: 50%; transform: translateX(-50%); z-index: 5;
  display: flex; flex-direction: column; gap: 0.55rem; align-items: center;
  padding: 0.8rem 1.2rem; border-radius: 16px;
  background: rgba(8, 8, 22, 0.72); backdrop-filter: blur(14px);
  border: 1px solid rgba(103, 232, 249, 0.18); box-shadow: 0 8px 40px rgba(0, 0, 0, 0.45);
}
.cp-row { display: flex; align-items: center; gap: 0.7rem; }
.cp-label { font-size: 0.66rem; letter-spacing: 0.25em; color: rgba(255,255,255,0.5); flex-shrink: 0; }
.cp-shapes { display: flex; gap: 0.4rem; flex-wrap: wrap; justify-content: center; }
.cp-shape { display: inline-flex; align-items: center; gap: 0.35rem; padding: 0.32rem 0.75rem; border-radius: 999px; font-size: 0.72rem; color: rgba(255,255,255,0.65); background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); cursor: pointer; transition: all 0.25s; }
.cp-shape .cp-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.3); transition: all 0.25s; }
.cp-shape:hover { color: #fff; border-color: rgba(103,232,249,0.4); }
.cp-shape.on { color: #fff; background: rgba(0,212,255,0.14); border-color: rgba(0,212,255,0.5); }
.cp-shape.on .cp-dot { background: #67e8f9; box-shadow: 0 0 8px #67e8f9; }
.cp-themes { display: flex; gap: 0.5rem; }
.cp-theme { width: 22px; height: 22px; border-radius: 50%; border: 2px solid rgba(255,255,255,0.18); cursor: pointer; transition: all 0.25s; }
.cp-theme.on { border-color: #fff; transform: scale(1.12); box-shadow: 0 0 12px rgba(255,255,255,0.25); }
.cp-meta { display: flex; gap: 1.2rem; font-size: 0.62rem; font-family: 'Courier New', monospace; letter-spacing: 0.12em; color: rgba(103,232,249,0.65); }

.modes { position: relative; padding: 5rem 2rem; }
.sec-head { text-align: center; margin-bottom: 3.5rem; }
.sec-num { color: #667eea; font-family: monospace; font-size: 0.75rem; letter-spacing: 0.25em; }
.sec-head h2 { font-size: clamp(1.6rem, 3.4vw, 2.4rem); font-weight: 800; margin: 0.5rem 0 0.3rem; }
.sec-head p { font-size: 0.7rem; letter-spacing: 0.3em; color: rgba(255,255,255,0.6); }
.mode-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1.4rem; max-width: 1150px; margin: 0 auto; }
.mode-card { padding: 2rem 1.6rem; border-radius: 18px; background: rgba(12, 12, 28, 0.65); border: 1px solid rgba(255,255,255,0.07); text-align: center; transition: all 0.35s; cursor: pointer; }
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
  .mode-grid { grid-template-columns: repeat(2, 1fr); max-width: 640px; }
  .gesture-flow { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .gesture-flow { grid-template-columns: 1fr; }
  .control-panel { width: calc(100% - 24px); bottom: 12px; }
  .cp-row { flex-direction: column; gap: 0.4rem; }
}
</style>

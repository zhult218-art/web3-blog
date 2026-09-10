<template>
  <div class="fluid-warp" ref="rootEl">
    <div class="fw-frame" ref="frameEl">
      <canvas ref="canvasEl" class="fw-canvas"></canvas>
      <div class="fw-hud">
        <span class="fw-tag">EFFECT_01</span>
        <span class="fw-name">交互式流体扭曲 · INTERACTIVE FLUID</span>
        <span class="fw-fps">{{ fps }} FPS</span>
      </div>
      <div class="fw-cursor" :style="cursorStyle"></div>
    </div>
    <p class="fw-tip">移动 / 拖拽指针 —— 图像沿轨迹产生液态扭曲与色散</p>
  </div>
</template>

<script setup>
// ============================================================
// EFFECT_01 交互式流体扭曲
// Three.js 正交相机 + 全屏平面 + 自定义片元着色器：
// 指针轨迹以衰减影响场写入 uniform，片元内对 UV 做径向位移，
// 并按位移强度做 RGB 色散；静止时保留环境波纹。
// 纹理为程序化 CanvasTexture，无外部资源依赖。
// ============================================================
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'

const rootEl = ref(null)
const frameEl = ref(null)
const canvasEl = ref(null)
const fps = ref(0)
const cursorStyle = reactive({ left: '-100px', top: '-100px' })

let renderer, scene, camera, material, rafId = 0
let running = true
let resizeObs = null, io = null
let lastT = performance.now(), fpsAcc = 0, fpsFrames = 0

// 指针轨迹槽位（循环覆写最旧的点）
const N = 24
const trail = Array.from({ length: N }, () => ({ x: .5, y: .5, s: 0 }))
let head = 0, prevUv = null

// 程序化科技感纹理：深空底 + 光斑 + 网格 + 大字
// 程序化绘制一片 6 轴对称雪花（主枝 + 侧枝），保证是雪花而非方块
function drawSnowflake(g, cx, cy, R) {
  g.save()
  g.translate(cx, cy)
  g.strokeStyle = 'rgba(206, 226, 255, 0.9)'
  g.shadowColor = 'rgba(150, 200, 255, 0.7)'
  g.shadowBlur = 6
  g.lineWidth = Math.max(1, R * 0.07)
  g.lineCap = 'round'
  const branches = 6
  for (let b = 0; b < branches; b++) {
    g.rotate((Math.PI * 2) / branches)
    g.beginPath(); g.moveTo(0, 0); g.lineTo(0, -R); g.stroke()
    for (const f of [0.38, 0.62, 0.84]) {
      const y = -R * f
      const len = R * (0.3 - f * 0.18)
      for (const s of [-1, 1]) {
        g.beginPath()
        g.moveTo(0, y)
        g.lineTo(s * len, y - len * 0.7)
        g.stroke()
      }
    }
  }
  g.restore()
}

function makeTexture() {
  const c = document.createElement('canvas')
  c.width = 1024; c.height = 512
  const g = c.getContext('2d')
  const bg = g.createLinearGradient(0, 0, 1024, 512)
  bg.addColorStop(0, '#0a0a1e'); bg.addColorStop(.55, '#12123a'); bg.addColorStop(1, '#1c1040')
  g.fillStyle = bg; g.fillRect(0, 0, 1024, 512)
  const blobs = [
    [220, 160, 260, 'rgba(102,126,234,.45)'],
    [760, 340, 300, 'rgba(118,75,162,.4)'],
    [540, 120, 200, 'rgba(0,212,255,.28)'],
    [880, 90, 170, 'rgba(16,185,129,.22)'],
  ]
  for (const [x, y, r, col] of blobs) {
    const rg = g.createRadialGradient(x, y, 0, x, y, r)
    rg.addColorStop(0, col); rg.addColorStop(1, 'transparent')
    g.fillStyle = rg; g.fillRect(0, 0, 1024, 512)
  }

  // 每个 64px 网格小方块画一片程序化雪花（不依赖外部图片，确保是雪花而非方块）
  const cell = 64
  const cols = Math.ceil(1024 / cell)
  const rows = Math.ceil(512 / cell)
  for (let r = 0; r < rows; r++) {
    for (let cc = 0; cc < cols; cc++) {
      drawSnowflake(g, cc * cell + cell / 2, r * cell + cell / 2, cell * 0.36)
    }
  }

  const tex = new THREE.CanvasTexture(c)
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

const VERT = `
  varying vec2 vUv;
  void main(){ vUv = uv; gl_Position = vec4(position.xy, 0., 1.); }
`
const FRAG = `
  precision highp float;
  varying vec2 vUv;
  uniform sampler2D uTex;
  uniform float uTime;
  uniform float uAspect;
  uniform vec3 uTrail[${N}];
  void main(){
    vec2 uv = vUv;
    vec2 disp = vec2(0.);
    for(int i=0;i<${N};i++){
      vec3 p = uTrail[i];
      if(p.z < .003) continue;
      vec2 d = uv - p.xy; d.x *= uAspect;
      float inf = exp(-dot(d,d)*46.) * p.z;
      disp += normalize(d + vec2(.0001)) * inf * .055;
    }
    disp += .0035 * vec2(sin(uv.y*9.+uTime*.9), cos(uv.x*11.-uTime*.7));
    vec2 uvw = clamp(uv + disp, vec2(.001), vec2(.999));
    vec3 col;
    col.r = texture2D(uTex, uvw + disp*.85).r;
    col.g = texture2D(uTex, uvw).g;
    col.b = texture2D(uTex, uvw - disp*.85).b;
    float glow = smoothstep(.02,.12,length(disp)) * .18;
    col += vec3(.35,.75,1.)*glow;
    gl_FragColor = vec4(col,1.);
  }
`

// 指针事件：记录轨迹 + 同步自定义光标
function onPointer(e) {
  const rect = frameEl.value.getBoundingClientRect()
  const uvx = (e.clientX - rect.left) / rect.width
  const uvy = 1 - (e.clientY - rect.top) / rect.height
  if (uvx < -.1 || uvx > 1.1 || uvy < -.1 || uvy > 1.1) return
  if (prevUv) {
    const speed = Math.hypot(uvx - prevUv[0], uvy - prevUv[1])
    const p = trail[head]
    p.x = uvx; p.y = uvy
    p.s = Math.max(p.s, Math.min(1, .25 + speed * 14))
    head = (head + 1) % N
  }
  prevUv = [uvx, uvy]
  cursorStyle.left = (e.clientX - rect.left) + 'px'
  cursorStyle.top = (e.clientY - rect.top) + 'px'
}
function onLeave() { prevUv = null; cursorStyle.left = '-100px'; cursorStyle.top = '-100px' }

// 渲染循环：轨迹衰减 -> 上传 uniform -> 绘制；顺带统计 FPS
function tick() {
  rafId = requestAnimationFrame(tick)
  if (!running || !renderer) return
  const now = performance.now()
  const dt = now - lastT
  lastT = now
  for (const p of trail) p.s *= .94
  const arr = material.uniforms.uTrail.value
  for (let i = 0; i < N; i++) arr[i].set(trail[i].x, trail[i].y, trail[i].s)
  material.uniforms.uTime.value = now * .001
  renderer.render(scene, camera)
  fpsAcc += dt; fpsFrames++
  if (fpsAcc >= 500) { fps.value = Math.round(fpsFrames * 1000 / fpsAcc); fpsAcc = 0; fpsFrames = 0 }
}

function resize() {
  if (!renderer || !frameEl.value) return
  const w = frameEl.value.clientWidth || 600
  const h = frameEl.value.clientHeight || 360
  renderer.setSize(w, h, false)
  material.uniforms.uAspect.value = w / h
}

onMounted(() => {
  scene = new THREE.Scene()
  camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1)
  material = new THREE.ShaderMaterial({
    vertexShader: VERT,
    fragmentShader: FRAG,
    uniforms: {
      uTex: { value: makeTexture() },
      uTime: { value: 0 },
      uAspect: { value: 16 / 9 },
      uTrail: { value: Array.from({ length: N }, () => new THREE.Vector3()) },
    },
  })
  const quad = new THREE.Mesh(new THREE.PlaneGeometry(2, 2), material)
  quad.frustumCulled = false
  scene.add(quad)

  renderer = new THREE.WebGLRenderer({ canvas: canvasEl.value, antialias: false })
  renderer.setPixelRatio(Math.min(devicePixelRatio, 1.6))

  resize()
  resizeObs = new ResizeObserver(resize)
  resizeObs.observe(frameEl.value)

  // 离屏自动暂停渲染
  io = new IntersectionObserver(([en]) => { running = en.isIntersecting }, { threshold: .05 })
  io.observe(frameEl.value)

  const el = frameEl.value
  el.addEventListener('pointermove', onPointer, { passive: true })
  el.addEventListener('pointerdown', onPointer, { passive: true })
  el.addEventListener('pointerleave', onLeave, { passive: true })
  tick()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  resizeObs?.disconnect()
  io?.disconnect()
  material?.uniforms.uTex.value?.dispose()
  material?.dispose()
  renderer?.dispose()
})
</script>

<style scoped>
.fluid-warp { width: 100%; }

.fw-frame {
  position: relative;
  height: clamp(320px, 52vh, 520px);
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,.08);
  background: #0a0a1e;
  touch-action: none;
  cursor: none;
}

.fw-canvas { position: absolute; inset: 0; width: 100%; height: 100%; display: block; }

.fw-hud {
  position: absolute;
  top: 0; left: 0; right: 0;
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: .7rem 1rem;
  font-family: 'Courier New', monospace;
  font-size: .68rem;
  letter-spacing: .12em;
  color: rgba(255,255,255,.55);
  background: linear-gradient(180deg, rgba(5,5,15,.65), transparent);
  pointer-events: none;
}

.fw-tag { color: #00d4ff; }
.fw-name { flex: 1; }
.fw-fps { color: rgba(16,185,129,.9); }

.fw-cursor {
  position: absolute;
  width: 26px; height: 26px;
  margin: -13px 0 0 -13px;
  border-radius: 50%;
  border: 1px solid rgba(0,212,255,.8);
  box-shadow: 0 0 18px rgba(0,212,255,.45), inset 0 0 10px rgba(0,212,255,.25);
  pointer-events: none;
  transition: opacity .3s;
}

.fw-tip {
  margin-top: .7rem;
  font-size: .72rem;
  letter-spacing: .08em;
  color: rgba(255,255,255,.35);
  text-align: center;
}
</style>

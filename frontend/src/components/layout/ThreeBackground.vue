<template>
  <div class="pointer-events-none fixed inset-0 z-0">
    <canvas ref="canvas" class="bg-gl-canvas h-full w-full" />
    <div class="absolute inset-0 bg-gradient-to-br from-[#a855f7]/15 via-transparent to-[#06b6d4]/10" />
  </div>
</template>

<script setup>
// ============================================================
// 全局 Three.js 背景（ThreeBackground）
// 固定全屏：粒子云 + 三层科技光环（着色器立体发光环：紫/青/粉）
// 相机随鼠标位置轻微平移，营造沉浸式空间感
// ============================================================
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as THREE from 'three'
import { useDeviceCapability } from '@/composables/useDeviceCapability'

// 设备能力：移动端 / 低配 / 减少动效 → 静态帧渲染，不跑动画循环
const { isMobile, staticBackground } = useDeviceCapability()

const canvas = ref(null)
let scene, camera, renderer, particles, raf, clock
let ringMeshes = []
let running = false
let mouseX = 0, mouseY = 0, targetX = 0, targetY = 0

// 生成雪花精灵贴图：离屏 canvas 绘制六臂雪花，作为粒子贴图
// （Three.js 的 Points 不带贴图时默认渲染为实心小方块，必须用 map 覆盖）
function makeSnowflakeTexture() {
  const s = 64
  const cv = document.createElement('canvas')
  cv.width = s
  cv.height = s
  const g = cv.getContext('2d')
  g.translate(s / 2, s / 2)
  g.strokeStyle = '#ffffff'
  g.lineWidth = 3
  g.lineCap = 'round'
  for (let i = 0; i < 6; i++) {
    g.rotate(Math.PI / 3)
    g.beginPath()
    g.moveTo(0, 4)
    g.lineTo(0, -s * 0.42)
    g.moveTo(0, -s * 0.2)
    g.lineTo(-s * 0.09, -s * 0.3)
    g.moveTo(0, -s * 0.2)
    g.lineTo(s * 0.09, -s * 0.3)
    g.stroke()
  }
  g.beginPath()
  g.arc(0, 0, 3, 0, Math.PI * 2)
  g.fillStyle = '#ffffff'
  g.fill()
  const tex = new THREE.CanvasTexture(cv)
  tex.needsUpdate = true
  return tex
}

// ============================================================
// 科技光环着色器：把普通 MeshBasicMaterial 的"平环"升级为立体发光环——
// 管壁伪光照（圆管体积感）+ 沿环双色渐变扫色 + 科技分段虚线 +
// 锐利刻度点 + 沿环行进扫描高亮。
// ============================================================
const RING_VERT = /* glsl */`
varying vec2 vUv;
varying vec3 vN;
void main() {
  vUv = uv;
  vN = normalize(normalMatrix * normal);
  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}
`

const RING_FRAG = /* glsl */`
uniform vec3 uColorA;
uniform vec3 uColorB;
uniform vec3 uScan;
uniform float uTime;
uniform float uOpacity;
uniform float uSegments;
uniform float uScanSpeed;
varying vec2 vUv;
varying vec3 vN;

void main() {
  float ang = vUv.x;                 // 0..1 绕主环一周

  // 沿圆周缓慢流动的双色渐变 → 科技扫色
  vec3 base = mix(uColorA, uColorB, 0.5 + 0.5 * sin(ang * 6.28318 + uTime * 0.35));

  // 管壁伪光照：朝向相机的管面更亮、背向更暗，营造圆管立体感
  vec3 n = normalize(vN);
  float lambert = 0.28 + 0.72 * pow(abs(n.z), 0.65);
  float tubeSheen = 0.60 + 0.40 * sin(vUv.y * 3.14159);  // 管截面周向微高光
  base *= lambert * tubeSheen * 1.5;

  // 科技分段虚线：沿圆周切成 uSegments 段，段内亮、段间暗
  float seg = fract(ang * uSegments);
  float dash = smoothstep(0.06, 0.22, seg) * (1.0 - smoothstep(0.78, 0.94, seg));
  dash = mix(0.18, 1.0, dash);

  // 锐利刻度点：每段一个亮点，形成仪表环刻度
  float ticks = pow(max(0.0, sin(ang * 6.28318 * uSegments)), 22.0);

  // 沿环行进扫描高亮（主束 + 对侧次束）
  float s1 = fract(ang - uTime * uScanSpeed);
  float beam = smoothstep(0.0, 0.05, s1) * (1.0 - smoothstep(0.05, 0.20, s1));
  float s2 = fract(ang - uTime * uScanSpeed + 0.5);
  float beam2 = smoothstep(0.0, 0.03, s2) * (1.0 - smoothstep(0.03, 0.10, s2));

  vec3 col = base * dash;
  col += uScan * ticks * 0.55;
  col += uScan * (beam * 1.0 + beam2 * 0.35);

  float alpha = uOpacity * dash * (0.70 + 0.30 * lambert)
              + uOpacity * ticks * 0.50
              + uOpacity * (beam * 0.90 + beam2 * 0.30);

  gl_FragColor = vec4(col, clamp(alpha, 0.0, 1.0));
}
`

// 初始化场景：相机、渲染器、粒子云与三层圆环
function init() {
  const c = canvas.value
  if (!c) return

  // 简易时钟（避免 THREE.Clock 弃用警告）
  clock = (() => { const s = performance.now(); let l = s; return { getDelta() { const n = performance.now(); const d = (n - l) / 1000; l = n; return d; }, getElapsedTime() { return (performance.now() - s) / 1000; } } })()
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(50, innerWidth / innerHeight, 0.1, 1000)
  // 桌面端保留 preserveDrawingBuffer 以修复布局级路由切换（如首页→博客）的全屏白闪；
  // 移动端关抗锯齿、DPR 上限降至 1、不保留绘制缓冲——全屏逐像素渲染下这三点最耗电发热
  renderer = new THREE.WebGLRenderer({
    canvas: c,
    alpha: true,
    antialias: !isMobile,
    preserveDrawingBuffer: !isMobile
  })
  renderer.setPixelRatio(Math.min(devicePixelRatio, isMobile ? 1 : 2))
  renderer.setSize(innerWidth, innerHeight)
  renderer.setClearColor(0x000000, 0)

  // Particle cloud
  const geo = new THREE.BufferGeometry()
  const count = isMobile ? 280 : 760
  const positions = new Float32Array(count * 3)
  const colors = new Float32Array(count * 3)
  for (let i = 0; i < count; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 22
    positions[i * 3 + 1] = (Math.random() - 0.5) * 14
    positions[i * 3 + 2] = (Math.random() - 0.5) * 10
    // Purple to cyan gradient
    const t = Math.random()
    colors[i * 3] = 0.44 + t * 0.22    // R: purple .66 -> cyan .02
    colors[i * 3 + 1] = 0.22 + t * 0.49 // G
    colors[i * 3 + 2] = 0.66 - t * 0.22 // B
  }
  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geo.setAttribute('color', new THREE.BufferAttribute(colors, 3))
  // 雪花精灵贴图 + 加色混合：每颗粒子渲染为发光小雪花（替代默认方形粒子）
  // 提亮：更大颗粒 + 不透明度拉满
  const mat = new THREE.PointsMaterial({
    size: 0.19,
    map: makeSnowflakeTexture(),
    vertexColors: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    transparent: true,
    opacity: 1,
    sizeAttenuation: true
  })
  particles = new THREE.Points(geo, mat)
  scene.add(particles)

  // ===== 三层科技光环 =====
  // 立体感 + 科技感来源：
  //   ① 外发光层：略粗的低透明度加色圆管，模拟 bloom 光晕；
  //   ② 主体层：细管 + 自定义 ShaderMaterial —— 沿圆周双色渐变扫色、
  //      管壁伪光照（朝相机的管面更亮 → 圆管立体）、科技分段虚线、
  //      刻度亮点、沿环行进扫描高亮；
  //   ③ 每层独立倾斜角 + 自转/进动，形成交错的空间轨道感。
  const RING_DEFS = [
    { radius: 3.8, tube: 0.018, colorA: 0x7c3aed, colorB: 0x22d3ee, scan: 0xffffff, opacity: 0.95, segments: 48, scanSpeed: 0.10, rx: Math.PI * 0.45, ry: Math.PI * 0.2,  spin: 1 },
    { radius: 4.5, tube: 0.015, colorA: 0x06b6d4, colorB: 0x3b82f6, scan: 0x9bffea, opacity: 0.80, segments: 64, scanSpeed: 0.07, rx: Math.PI * 0.25, ry: -Math.PI * 0.3, spin: -1 },
    { radius: 5.2, tube: 0.013, colorA: 0xec4899, colorB: 0xa855f7, scan: 0xffd6f2, opacity: 0.68, segments: 80, scanSpeed: 0.05, rx: Math.PI * 0.55, ry: Math.PI * 0.4,  spin: 1 }
  ]

  ringMeshes = RING_DEFS.map((def) => {
    const group = new THREE.Group()

    // ① 外发光层（bloom 光晕）
    group.add(new THREE.Mesh(
      new THREE.TorusGeometry(def.radius, def.tube * 5, 12, 200),
      new THREE.MeshBasicMaterial({
        color: def.colorA, blending: THREE.AdditiveBlending,
        depthWrite: false, transparent: true, opacity: def.opacity * 0.15
      })
    ))

    // ② 主体层（着色器细管）
    const coreMat = new THREE.ShaderMaterial({
      uniforms: {
        uColorA: { value: new THREE.Color(def.colorA) },
        uColorB: { value: new THREE.Color(def.colorB) },
        uScan: { value: new THREE.Color(def.scan) },
        uTime: { value: 0 },
        uOpacity: { value: def.opacity },
        uSegments: { value: def.segments },
        uScanSpeed: { value: def.scanSpeed }
      },
      vertexShader: RING_VERT,
      fragmentShader: RING_FRAG,
      transparent: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    })
    group.add(new THREE.Mesh(new THREE.TorusGeometry(def.radius, def.tube, 10, 240), coreMat))

    // ③ 独立倾斜角 → 三层交错的空间轨道
    group.rotation.x = def.rx
    group.rotation.y = def.ry
    scene.add(group)
    return { group, coreMat, spin: def.spin }
  })

  camera.position.z = 8
}

// 动画循环：相机缓动跟随鼠标 + 粒子/圆环自转
function animate() {
  if (!running) return
  raf = requestAnimationFrame(animate)
  const dt = clock.getDelta()
  const t = performance.now() * 0.001

  targetX += (mouseX - targetX) * 0.04
  targetY += (mouseY - targetY) * 0.04

  camera.position.x += (targetX * 2.5 - camera.position.x) * 0.04
  camera.position.y += (-targetY * 2.5 - camera.position.y) * 0.04
  camera.lookAt(0, 0, 0)

  if (particles) {
    particles.rotation.y += dt * 0.08
    particles.rotation.x += dt * 0.04
    particles.rotation.z += dt * 0.02
  }
  ringMeshes.forEach((r, i) => {
    r.coreMat.uniforms.uTime.value = t
    // 绕环自转（交替方向）+ 轻微进动 → 轨道立体运动
    r.group.rotation.z += dt * (0.16 - i * 0.03) * r.spin
    r.group.rotation.x += dt * 0.05 * (i === 1 ? -1 : 1)
    if (i === 2) r.group.rotation.y += dt * 0.04
  })

  renderer.render(scene, camera)
}

// ── 循环控制：静态降级 / 切后台暂停 ──
// 静态帧：只渲染一帧（移动端/低配/减少动效），保留视觉但不再持续占用 GPU
function renderOnce() {
  if (!renderer || !scene || !camera) return
  const t = performance.now() * 0.001
  ringMeshes.forEach((r) => { r.coreMat.uniforms.uTime.value = t })
  camera.lookAt(0, 0, 0)
  renderer.render(scene, camera)
}

function startLoop() {
  if (running || staticBackground) return
  running = true
  animate()
}

function stopLoop() {
  running = false
  cancelAnimationFrame(raf)
}

// 切到后台 / 息屏 → 立即停止渲染；恢复可见 → 重启。避免后台空转耗电发热
function onVisibility() {
  if (document.hidden) stopLoop()
  else startLoop()
}

// 窗口尺寸变化时同步相机与渲染器
const onResize = () => {
  if (!camera || !renderer) return
  camera.aspect = innerWidth / innerHeight
  camera.updateProjectionMatrix()
  renderer.setSize(innerWidth, innerHeight)
  // 静态模式下 setSize 会清空画布，需补渲染一帧
  if (staticBackground) renderOnce()
}

// 记录鼠标位置（归一化到 -1 ~ 1）
const onMouse = (e) => {
  mouseX = (e.clientX / innerWidth) * 2 - 1
  mouseY = (e.clientY / innerHeight) * 2 - 1
}

onMounted(() => {
  init()
  // 静态降级：只渲染一帧；否则启动动画循环
  if (staticBackground) renderOnce()
  else startLoop()
  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMouse)
  document.addEventListener('visibilitychange', onVisibility)
  // WebGL 上下文丢失时 Chrome 会把 canvas 渲染成白屏，需拦截并等恢复后重绘
  canvas.value?.addEventListener('webglcontextlost', (e) => {
    e.preventDefault()
  }, false)
  canvas.value?.addEventListener('webglcontextrestored', () => {
    if (renderer) renderer.render(scene, camera)
  }, false)
})

onBeforeUnmount(() => {
  stopLoop()
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onMouse)
  document.removeEventListener('visibilitychange', onVisibility)
  // 显式销毁三层光环的几何体与着色器材质，避免 WebGL 资源泄漏
  ringMeshes.forEach((r) => {
    r.group.traverse((o) => {
      if (o.geometry) o.geometry.dispose()
      if (o.material) o.material.dispose()
    })
  })
  ringMeshes = []
  if (renderer) renderer.dispose()
  if (scene) scene.clear()
})
</script>

<template>
  <div class="pointer-events-none fixed inset-0 z-0">
    <canvas ref="canvas" class="h-full w-full" />
    <div class="absolute inset-0 bg-gradient-to-br from-[#a855f7]/15 via-transparent to-[#06b6d4]/10" />
  </div>
</template>

<script setup>
// ============================================================
// 全局 Three.js 背景（ThreeBackground）
// 固定全屏：1800 颗粒子云 + 三层旋转圆环（紫/青/粉）
// 相机随鼠标位置轻微平移，营造沉浸式空间感
// ============================================================
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as THREE from 'three'

const canvas = ref(null)
let scene, camera, renderer, particles, ring, ring2, ring3, raf, clock
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

// 初始化场景：相机、渲染器、粒子云与三层圆环
function init() {
  const c = canvas.value
  if (!c) return

  clock = new THREE.Clock()
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(50, innerWidth / innerHeight, 0.1, 1000)
  renderer = new THREE.WebGLRenderer({ canvas: c, alpha: true, antialias: true })
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2))
  renderer.setSize(innerWidth, innerHeight)
  renderer.setClearColor(0x000000, 0)

  // Particle cloud
  const geo = new THREE.BufferGeometry()
  const count = 760
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

  // Triple rings（提亮：三层圆环不透明度上调）
  const ringGeo1 = new THREE.TorusGeometry(3.8, 0.015, 16, 140)
  const ringMat1 = new THREE.MeshBasicMaterial({ color: 0xa855f7, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true, opacity: 0.72 })
  ring = new THREE.Mesh(ringGeo1, ringMat1)
  ring.rotation.x = Math.PI * 0.45
  ring.rotation.y = Math.PI * 0.2
  scene.add(ring)

  const ringGeo2 = new THREE.TorusGeometry(4.5, 0.012, 16, 160)
  const ringMat2 = new THREE.MeshBasicMaterial({ color: 0x06b6d4, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true, opacity: 0.58 })
  ring2 = new THREE.Mesh(ringGeo2, ringMat2)
  ring2.rotation.x = Math.PI * 0.25
  ring2.rotation.y = -Math.PI * 0.3
  scene.add(ring2)

  const ringGeo3 = new THREE.TorusGeometry(5.2, 0.01, 16, 180)
  const ringMat3 = new THREE.MeshBasicMaterial({ color: 0xec4899, blending: THREE.AdditiveBlending, depthWrite: false, transparent: true, opacity: 0.46 })
  ring3 = new THREE.Mesh(ringGeo3, ringMat3)
  ring3.rotation.x = Math.PI * 0.55
  ring3.rotation.y = Math.PI * 0.4
  scene.add(ring3)

  camera.position.z = 8
}

// 动画循环：相机缓动跟随鼠标 + 粒子/圆环自转
function animate() {
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
  if (ring) {
    ring.rotation.z += dt * 0.15
    ring.rotation.x += dt * 0.06
  }
  if (ring2) {
    ring2.rotation.z -= dt * 0.12
    ring2.rotation.y += dt * 0.08
  }
  if (ring3) {
    ring3.rotation.z += dt * 0.1
    ring3.rotation.x -= dt * 0.07
  }

  renderer.render(scene, camera)
}

// 窗口尺寸变化时同步相机与渲染器
const onResize = () => {
  if (!camera || !renderer) return
  camera.aspect = innerWidth / innerHeight
  camera.updateProjectionMatrix()
  renderer.setSize(innerWidth, innerHeight)
}

// 记录鼠标位置（归一化到 -1 ~ 1）
const onMouse = (e) => {
  mouseX = (e.clientX / innerWidth) * 2 - 1
  mouseY = (e.clientY / innerHeight) * 2 - 1
}

onMounted(() => {
  init()
  animate()
  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMouse)
  // WebGL 上下文丢失时 Chrome 会把 canvas 渲染成白屏，需拦截并等恢复后重绘
  canvas.value?.addEventListener('webglcontextlost', (e) => {
    e.preventDefault()
  }, false)
  canvas.value?.addEventListener('webglcontextrestored', () => {
    if (renderer) renderer.render(scene, camera)
  }, false)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onMouse)
  if (renderer) renderer.dispose()
  if (scene) scene.clear()
})
</script>

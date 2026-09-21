<template>
  <!-- 3D 服务拓扑数字孪生画布（容器自带 webgl canvas 与悬浮提示层） -->
  <div ref="wrap" class="twin-canvas" :style="{ height: height + 'px' }">
    <div v-if="hint" ref="tip" class="twin-tip" :class="{ 'is-on': !!hovered }">
      <p class="tip-name">{{ hovered?.label }}</p>
      <p class="tip-sub">{{ hovered?.sub }}</p>
      <p class="tip-status" :class="'st-' + (hovered?.status || 'UNKNOWN').toLowerCase()">
        {{ STATUS_TEXT[hovered?.status] || hovered?.status }}
      </p>
    </div>
    <div class="twin-legend">
      <span><i class="dot run"></i>运行中</span>
      <span><i class="dot stop"></i>已停止</span>
      <span><i class="dot unknown"></i>未知</span>
    </div>
    <p class="twin-hint">拖拽旋转 · 滚轮缩放 · 点击节点查看详情</p>
  </div>
</template>

<script setup>
// ============================================================
// ServiceTwin —— 微服务拓扑 3D 数字孪生
// · 依据真实注册表把 13 个微服务 + 基础设施渲染成四层三维拓扑
// · 节点颜色 / 连线流动速度实时反映健康状态（由父组件传入 status）
// · 交互：OrbitControls 拖拽旋转缩放、射线拾取 hover 高亮、点击选中
// · 资源管理：几何体 / 材质 / 贴图 / 渲染器在卸载时全部释放
// ============================================================
import { ref, shallowRef, watch, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

const props = defineProps({
  // [{ id, label, sub, layer, status }]，layer: 1 客户端 / 2 网关 / 3 业务服务 / 4 基础设施
  nodes: { type: Array, default: () => [] },
  // [{ from, to }] 节点 id 之间的调用关系
  links: { type: Array, default: () => [] },
  height: { type: Number, default: 560 },
  // 是否自动缓慢旋转（hover 时自动暂停）
  autoRotate: { type: Boolean, default: true },
  selectedId: { type: String, default: '' },
  hint: { type: Boolean, default: true },
})

const emit = defineEmits(['select'])

const STATUS_TEXT = { RUNNING: '运行中', STOPPED: '已停止', UNKNOWN: '状态未知' }
const STATUS_COLOR = { RUNNING: 0x34d399, STOPPED: 0xf87171, UNKNOWN: 0x94a3b8 }
const LAYER_Y = { 1: 10.5, 2: 4, 3: -4, 4: -11 }
const LAYER_RADIUS = { 1: 3.6, 2: 0, 3: 12.5, 4: 9 }

const wrap = ref(null)
const tip = ref(null)
const hovered = ref(null)

// three 对象统一放 shallowRef/普通变量，避免被 Vue 代理导致性能与相等判断问题
let renderer, scene, camera, controls, raycaster, pointer
let raf = 0
let ro = null
let disposed = false

const nodeMap = new Map()   // id -> { data, group, mesh, halo }
const linkObjs = []         // { curve, particles: Mesh[], speed }
const disposables = []      // 需要 dispose 的几何体 / 材质 / 贴图

// ── 标签贴图：用 canvas 绘制文字，免引入字体文件 ──
function makeLabel(text, sub) {
  const cv = document.createElement('canvas')
  cv.width = 512
  cv.height = 160
  const c = cv.getContext('2d')
  c.clearRect(0, 0, cv.width, cv.height)
  c.textAlign = 'center'
  c.fillStyle = '#ffffff'
  c.font = 'bold 54px "Segoe UI", "Microsoft YaHei", sans-serif'
  c.fillText(text, 256, 66)
  if (sub) {
    c.fillStyle = 'rgba(190,205,225,0.85)'
    c.font = '34px "Segoe UI", "Microsoft YaHei", sans-serif'
    c.fillText(sub, 256, 116)
  }
  const tex = new THREE.CanvasTexture(cv)
  tex.colorSpace = THREE.SRGBColorSpace
  const mat = new THREE.SpriteMaterial({ map: tex, transparent: true, depthWrite: false })
  const sprite = new THREE.Sprite(mat)
  sprite.scale.set(5.2, 1.63, 1)
  disposables.push(tex, mat)
  return sprite
}

const colorOf = status => STATUS_COLOR[status] || STATUS_COLOR.UNKNOWN

// ── 计算每个节点在三维空间中的坐标 ──
function layout(nodes) {
  const byLayer = {}
  nodes.forEach(n => {
    const l = n.layer || 3
    ;(byLayer[l] = byLayer[l] || []).push(n)
  })
  const pos = new Map()
  Object.keys(byLayer).forEach(k => {
    const layer = Number(k)
    const list = byLayer[k]
    const y = LAYER_Y[layer] ?? 0
    const r = LAYER_RADIUS[layer] ?? 10
    list.forEach((n, i) => {
      if (r === 0 || list.length === 1) {
        pos.set(n.id, new THREE.Vector3(0, y, 0))
      } else {
        const a = (i / list.length) * Math.PI * 2 - Math.PI / 2
        pos.set(n.id, new THREE.Vector3(Math.cos(a) * r, y, Math.sin(a) * r))
      }
    })
  })
  return pos
}

// ── 构建节点 ──
function buildNodes(nodes) {
  const pos = layout(nodes)
  nodes.forEach(n => {
    const p = pos.get(n.id) || new THREE.Vector3()
    const color = colorOf(n.status)

    const group = new THREE.Group()
    group.position.copy(p)

    const geo = new THREE.IcosahedronGeometry(1.05, 1)
    const mat = new THREE.MeshStandardMaterial({
      color,
      emissive: color,
      emissiveIntensity: 0.55,
      roughness: 0.32,
      metalness: 0.35,
    })
    const mesh = new THREE.Mesh(geo, mat)
    mesh.userData.id = n.id
    group.add(mesh)

    const haloGeo = new THREE.IcosahedronGeometry(1.75, 1)
    const haloMat = new THREE.MeshBasicMaterial({ color, wireframe: true, transparent: true, opacity: 0.2 })
    const halo = new THREE.Mesh(haloGeo, haloMat)
    group.add(halo)

    const label = makeLabel(n.label, n.sub)
    label.position.set(0, 2.3, 0)
    group.add(label)

    disposables.push(geo, mat, haloGeo, haloMat)
    scene.add(group)
    nodeMap.set(n.id, { data: n, group, mesh, halo, baseColor: color })
  })

  // 层基准平面（淡色圆环，增强层次感）
  Object.keys(LAYER_RADIUS).forEach(k => {
    const layer = Number(k)
    const r = LAYER_RADIUS[layer] || 4
    if (r <= 0) return
    const ringGeo = new THREE.RingGeometry(r - 0.12, r - 0.02, 96)
    const ringMat = new THREE.MeshBasicMaterial({
      color: 0x8ea3c7, transparent: true, opacity: 0.08, side: THREE.DoubleSide,
    })
    const ring = new THREE.Mesh(ringGeo, ringMat)
    ring.rotation.x = -Math.PI / 2
    ring.position.y = LAYER_Y[layer] ?? 0
    scene.add(ring)
    disposables.push(ringGeo, ringMat)
  })
}

// ── 构建连线（二次贝塞尔曲线 + 流动粒子） ──
function buildLinks(links) {
  links.forEach(l => {
    const a = nodeMap.get(l.from)
    const b = nodeMap.get(l.to)
    if (!a || !b) return
    const start = a.group.position.clone()
    const end = b.group.position.clone()
    // 控制点：向两侧外扩，形成柔和的弧线
    const mid = start.clone().add(end).multiplyScalar(0.5)
    mid.x += (start.x + end.x) * 0.12 || 2.5
    mid.z += (start.z + end.z) * 0.12 || 2.5
    const curve = new THREE.QuadraticBezierCurve3(start, mid, end)
    const pts = curve.getPoints(40)
    const lineGeo = new THREE.BufferGeometry().setFromPoints(pts)
    // 跟随目标服务状态：停掉的链路变暗变红
    const up = b.data.status === 'RUNNING'
    const lineMat = new THREE.LineBasicMaterial({
      color: up ? 0x6ee7d7 : 0x8792a8,
      transparent: true,
      opacity: b.data.status === 'STOPPED' ? 0.08 : 0.22,
    })
    const line = new THREE.Line(lineGeo, lineMat)
    scene.add(line)
    disposables.push(lineGeo, lineMat)

    // 流动粒子：每个链路一颗，沿曲线循环前进
    const pGeo = new THREE.SphereGeometry(0.13, 8, 8)
    const pMat = new THREE.MeshBasicMaterial({ color: up ? 0x67e8f9 : 0x94a3b8 })
    const particle = new THREE.Mesh(pGeo, pMat)
    scene.add(particle)
    disposables.push(pGeo, pMat)

    linkObjs.push({
      curve,
      particle,
      lineMat,
      line,
      t: Math.random(),
      speed: b.data.status === 'RUNNING' ? 0.0032 : 0.0009,
      targetId: l.to,
    })
  })
}

// ── 背景星尘 ──
function buildStars() {
  const count = 900
  const arr = new Float32Array(count * 3)
  for (let i = 0; i < count; i++) {
    const r = 40 + Math.random() * 60
    const th = Math.random() * Math.PI * 2
    const ph = Math.acos(Math.random() * 1.6 - 0.8)
    arr[i * 3] = r * Math.sin(ph) * Math.cos(th)
    arr[i * 3 + 1] = r * Math.cos(ph)
    arr[i * 3 + 2] = r * Math.sin(ph) * Math.sin(th)
  }
  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(arr, 3))
  const mat = new THREE.PointsMaterial({ color: 0x9fb6dd, size: 0.32, transparent: true, opacity: 0.5 })
  const stars = new THREE.Points(geo, mat)
  scene.add(stars)
  disposables.push(geo, mat)
}

// ── 重建整个场景内容（nodes / links 变化时调用） ──
function rebuild() {
  if (!scene) return
  clearContent()
  buildStars()
  // 基础设施层不参与健康探测，统一按 UNKNOWN 之外的中性色呈现
  buildNodes(props.nodes)
  buildLinks(props.links)
  applySelected()
}

function clearContent() {
  const removable = [...nodeMap.values()].map(v => v.group)
  nodeMap.clear()
  linkObjs.length = 0
  // 收集所有直接挂在 scene 上的对象（节点组 / 线 / 粒子 / 星尘 / 圆环）
  scene.children.slice().forEach(ch => {
    if (ch.isLight) return
    removable.push(ch)
  })
  removable.forEach(o => scene.remove(o))
  disposables.forEach(d => { try { d.dispose() } catch { /* 已释放 */ } })
  disposables.length = 0
}

// ── 高亮选中节点 ──
function applySelected() {
  nodeMap.forEach((v, id) => {
    const on = id === props.selectedId
    v.mesh.scale.setScalar(on ? 1.45 : 1)
    v.halo.material.opacity = on ? 0.55 : 0.2
    v.mesh.material.emissiveIntensity = on ? 1.1 : 0.55
  })
}

// ── 状态更新：不重建场景，只改颜色与速度 ──
function applyStatus() {
  props.nodes.forEach(n => {
    const v = nodeMap.get(n.id)
    if (!v) return
    const color = colorOf(n.status)
    v.mesh.material.color.setHex(color)
    v.mesh.material.emissive.setHex(color)
    v.halo.material.color.setHex(color)
    v.data.status = n.status
  })
  linkObjs.forEach(l => {
    const t = nodeMap.get(l.targetId)
    const up = t?.data.status === 'RUNNING'
    const stopped = t?.data.status === 'STOPPED'
    l.speed = up ? 0.0032 : 0.0009
    l.lineMat.color.setHex(up ? 0x6ee7d7 : 0x8792a8)
    l.lineMat.opacity = stopped ? 0.08 : 0.22
    l.particle.material.color.setHex(up ? 0x67e8f9 : 0x94a3b8)
  })
}

// ── 拾取：屏幕坐标 → 射线 → 最近节点 ──
function pick(ev) {
  const rect = renderer.domElement.getBoundingClientRect()
  pointer.x = ((ev.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((ev.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(pointer, camera)
  const meshes = [...nodeMap.values()].map(v => v.mesh)
  const hits = raycaster.intersectObjects(meshes, false)
  return hits.length ? hits[0].object.userData.id : null
}

function onPointerMove(ev) {
  const id = pick(ev)
  if (id === hovered.value?.id) return
  hovered.value = id ? nodeMap.get(id).data : null
  renderer.domElement.style.cursor = id ? 'pointer' : 'grab'
  if (controls) controls.autoRotate = props.autoRotate && !id
  // 悬浮提示跟随鼠标
  if (tip.value && id) {
    const rect = renderer.domElement.getBoundingClientRect()
    tip.value.style.left = (ev.clientX - rect.left) + 'px'
    tip.value.style.top = (ev.clientY - rect.top) + 'px'
  }
}

function onClick(ev) {
  const id = pick(ev)
  emit('select', id ? nodeMap.get(id).data : null)
}

function resize() {
  if (!renderer || !wrap.value) return
  const w = wrap.value.clientWidth
  const h = wrap.value.clientHeight
  if (!w || !h) return
  renderer.setSize(w, h, false)
  camera.aspect = w / h
  camera.updateProjectionMatrix()
}

function animate() {
  if (disposed) return
  raf = requestAnimationFrame(animate)
  const t = performance.now() / 1000

  // 节点呼吸：运行中的服务轻微起伏，停掉的保持静止
  nodeMap.forEach(v => {
    if (v.data.status !== 'RUNNING') return
    const s = 1 + Math.sin(t * 2.1 + v.group.position.x) * 0.045
    v.halo.scale.setScalar(s * 1.02)
  })
  // 链路粒子流动
  linkObjs.forEach(l => {
    l.t += l.speed
    if (l.t > 1) l.t -= 1
    l.particle.position.copy(l.curve.getPoint(l.t))
  })

  controls?.update()
  renderer.render(scene, camera)
}

onMounted(() => {
  if (!wrap.value) return
  const w = wrap.value.clientWidth || 800
  const h = wrap.value.clientHeight || props.height

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2))
  renderer.setSize(w, h, false)
  renderer.domElement.style.display = 'block'
  renderer.domElement.style.width = '100%'
  renderer.domElement.style.height = '100%'
  wrap.value.appendChild(renderer.domElement)

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(48, w / h, 0.1, 400)
  camera.position.set(0, 7, 36)

  scene.add(new THREE.AmbientLight(0xffffff, 0.62))
  const dir = new THREE.DirectionalLight(0xdff3ff, 1.0)
  dir.position.set(8, 16, 10)
  scene.add(dir)
  const point = new THREE.PointLight(0x8b5cf6, 1.1, 90)
  point.position.set(-9, 2, -6)
  scene.add(point)
  const point2 = new THREE.PointLight(0x22d3ee, 0.9, 90)
  point2.position.set(10, -4, 6)
  scene.add(point2)

  raycaster = new THREE.Raycaster()
  pointer = new THREE.Vector2()

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.07
  controls.minDistance = 14
  controls.maxDistance = 70
  controls.autoRotate = props.autoRotate
  controls.autoRotateSpeed = 0.5
  controls.target.set(0, 0, 0)

  rebuild()
  renderer.domElement.style.cursor = 'grab'
  renderer.domElement.addEventListener('pointermove', onPointerMove)
  renderer.domElement.addEventListener('click', onClick)

  ro = new ResizeObserver(resize)
  ro.observe(wrap.value)
  window.addEventListener('resize', resize)

  animate()
})

onBeforeUnmount(() => {
  disposed = true
  cancelAnimationFrame(raf)
  renderer?.domElement?.removeEventListener('pointermove', onPointerMove)
  renderer?.domElement?.removeEventListener('click', onClick)
  window.removeEventListener('resize', resize)
  ro?.disconnect()
  controls?.dispose()
  if (scene) clearContent()
  disposables.forEach(d => { try { d.dispose() } catch { /* 已释放 */ } })
  disposables.length = 0
  renderer?.dispose()
  if (renderer?.domElement?.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement)
  renderer = scene = camera = controls = null
})

// 节点集合结构变化（增删）→ 重建；仅状态变化 → 原地更新颜色
watch(() => props.nodes.map(n => n.id).join(','), rebuild)
watch(() => props.links.map(l => l.from + '>' + l.to).join(','), rebuild)
watch(() => props.nodes.map(n => n.id + ':' + n.status).join(','), applyStatus)
watch(() => props.selectedId, applySelected)
watch(() => props.autoRotate, v => { if (controls) controls.autoRotate = v && !hovered.value })
</script>

<style scoped>
.twin-canvas {
  position: relative;
  width: 100%;
  border-radius: 18px;
  overflow: hidden;
  background: radial-gradient(ellipse at 50% 0%, #16213c 0%, #0a0d1a 55%, #05060c 100%);
  border: 1px solid rgba(255, 255, 255, 0.07);
}
.twin-legend {
  position: absolute;
  left: 14px;
  bottom: 12px;
  display: flex;
  gap: 14px;
  font-size: 11px;
  color: #9fb0c8;
  pointer-events: none;
}
.twin-legend span { display: inline-flex; align-items: center; gap: 5px; }
.dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot.run { background: #34d399; box-shadow: 0 0 6px #34d399; }
.dot.stop { background: #f87171; box-shadow: 0 0 6px #f87171; }
.dot.unknown { background: #94a3b8; }
.twin-hint {
  position: absolute;
  right: 14px;
  bottom: 12px;
  font-size: 11px;
  color: #64748b;
  pointer-events: none;
  letter-spacing: 0.04em;
}
.twin-tip {
  position: absolute;
  transform: translate(14px, -50%);
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(9, 13, 26, 0.92);
  border: 1px solid rgba(120, 200, 255, 0.22);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.16s;
  pointer-events: none;
  white-space: nowrap;
  z-index: 2;
}
.twin-tip.is-on { opacity: 1; }
.tip-name { font-size: 13px; font-weight: 600; color: #fff; }
.tip-sub { font-size: 11px; color: #8fa1bd; margin-top: 2px; }
.tip-status { font-size: 11px; margin-top: 4px; }
.tip-status.st-running { color: #34d399; }
.tip-status.st-stopped { color: #f87171; }
.tip-status.st-unknown { color: #94a3b8; }
</style>
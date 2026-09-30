<template>
  <Teleport to="body">
    <div class="pe-root">
      <!-- 顶栏 -->
      <div class="pe-top">
        <button class="pe-btn ghost" title="退出编辑 (Esc)" @click="close">✕</button>
        <span class="pe-divider" />
        <button class="pe-btn ghost" title="顺时针旋转 90°" @click="rotate90">⟳</button>
        <button class="pe-btn ghost" :class="{ on: flipH }" title="水平翻转" @click="doFlip('h')">⇋</button>
        <button class="pe-btn ghost" :class="{ on: flipV }" title="垂直翻转" @click="doFlip('v')">⇵</button>
        <span class="pe-divider" />
        <button class="pe-btn ghost" title="撤销 (Ctrl+Z)" :disabled="!past.length" @click="undo">↶</button>
        <button class="pe-btn ghost" title="重做 (Ctrl+Y)" :disabled="!future.length" @click="redo">↷</button>
        <button class="pe-btn ghost" :class="{ on: holdOriginal }" title="按住对比原图" @click="holdOriginal = !holdOriginal">
          <span v-if="holdOriginal">原图</span><span v-else>对比</span>
        </button>
        <button class="pe-btn ghost" title="全部重置" @click="resetAll">重置</button>

        <div class="ml-auto flex items-center gap-2">
          <button class="pe-btn ghost" :disabled="working" title="复制图片到剪贴板" @click="copyImage">
            {{ copying ? '复制中…' : '📋 复制' }}
          </button>
          <button class="pe-btn primary" :disabled="working" @click="exportOpen = !exportOpen">
            {{ exporting ? '导出中…' : '⬇ 导出图片' }}
          </button>
        </div>
      </div>

      <div class="pe-body">
        <!-- 左侧工具栏 -->
        <div class="pe-rail">
          <button v-for="t in TOOLS" :key="t.key" class="pe-rail-btn" :class="{ on: tool === t.key }"
            :title="t.label" @click="switchTool(t.key)">
            <span class="text-lg leading-none">{{ t.icon }}</span>
            <span class="text-[10px] mt-1">{{ t.label }}</span>
          </button>
        </div>

        <!-- 画布舞台 -->
        <div ref="stageEl" class="pe-stage" :class="'cur-' + tool">
          <canvas ref="canvasEl"
            @pointerdown="onPointerDown" @pointermove="onPointerMove"
            @pointerup="onPointerUp" @pointercancel="onPointerUp"
            @dblclick.prevent="onDblClick" />

          <!-- 裁剪框覆盖层 -->
          <template v-if="tool === 'crop' && cropDraft && drawRect">
            <div class="pe-crop-layer" :style="layerStyle" @pointerdown="onCropDown($event, 'move')">
              <div class="pe-crop-box" :style="cropBoxStyle" @pointerdown.stop="onCropDown($event, 'move')">
                <span v-for="h in CROP_HANDLES" :key="h"
                  :class="['pe-crop-handle', h]" @pointerdown.stop="onCropDown($event, h)" />
                <div class="pe-crop-grid" />
              </div>
            </div>
          </template>

          <!-- 文字编辑浮层 -->
          <input v-if="editingText" ref="textInputEl" v-model="editingText.content"
            class="pe-text-input" :style="textInputStyle"
            @keydown.enter.prevent="commitTextEdit"
            @keydown.esc.prevent="cancelTextEdit"
            @blur="commitTextEdit"
            @pointerdown.stop />

          <div v-if="loadError" class="pe-loaderr">
            <p>🖼 图片加载失败</p>
            <button class="pe-btn ghost mt-3" @click="loadImage">重新加载</button>
          </div>

          <!-- 缩放指示 -->
          <div class="pe-zoom-tag">{{ Math.round(viewZoom * 100) }}% · {{ outNW }}×{{ outNH }}</div>

          <!-- 导出弹层 -->
          <transition name="pe-pop">
            <div v-if="exportOpen" class="pe-export-pop" @pointerdown.stop>
              <p class="pe-pop-title">导出图片</p>
              <label class="pe-field-label">文件名</label>
              <input v-model="exportName" class="pe-textfield" maxlength="60" />
              <label class="pe-field-label mt-3">格式</label>
              <div class="flex gap-1.5">
                <button v-for="f in EXPORT_FORMATS" :key="f.k" class="pe-chip"
                  :class="{ on: exportFmt === f.k }" @click="exportFmt = f.k">{{ f.t }}</button>
              </div>
              <template v-if="exportFmt !== 'png'">
                <label class="pe-field-label mt-3">质量 <span class="text-cyan-300">{{ exportQuality }}%</span></label>
                <input type="range" min="50" max="100" step="1" v-model.number="exportQuality" class="pe-range" />
              </template>
              <p class="pe-field-label mt-3 text-gray-500">
                输出尺寸 {{ outNW }}×{{ outNH }}px · 编辑仅在本地完成，不影响原图
              </p>
              <p v-if="canvasTainted" class="pe-warn-text">
                ⚠ 该图片来自外部图床且未开放 CORS，浏览器安全策略可能阻止导出，可先在查看器中「下载原图」后重新打开编辑
              </p>
              <div class="flex gap-2 mt-4">
                <button class="pe-btn ghost flex-1" @click="exportOpen = false">取消</button>
                <button class="pe-btn primary flex-1" :disabled="working" @click="downloadImage">下载</button>
              </div>
            </div>
          </transition>
        </div>

        <!-- 右侧调节面板 -->
        <div class="pe-panel">
          <!-- 工具选项 -->
          <section v-if="tool === 'view'" class="pe-sec">
            <p class="pe-sec-title">✋ 查看</p>
            <p class="pe-hint">滚轮缩放 · 双击放大 · 拖拽平移</p>
            <div class="flex gap-1.5 mt-3">
              <button class="pe-chip flex-1" @click="viewZoom = 1; panX = 0; panY = 0">适应</button>
              <button class="pe-chip flex-1" @click="zoomTo(2)">100%…2x</button>
            </div>
          </section>

          <section v-else-if="tool === 'crop'" class="pe-sec">
            <p class="pe-sec-title">✂️ 裁剪</p>
            <label class="pe-field-label">比例</label>
            <div class="flex flex-wrap gap-1.5">
              <button v-for="r in RATIOS" :key="r.k" class="pe-chip"
                :class="{ on: cropRatio === r.k }" @click="setCropRatio(r.k)">{{ r.t }}</button>
            </div>
            <div class="flex gap-2 mt-4">
              <button class="pe-chip flex-1" @click="cancelCrop">取消</button>
              <button class="pe-chip on flex-1" @click="applyCrop">应用裁剪</button>
            </div>
            <p class="pe-hint mt-3">拖拽边框手柄调整区域，框内移动位置</p>
          </section>

          <section v-else-if="tool === 'brush'" class="pe-sec">
            <p class="pe-sec-title">🖌 画笔</p>
            <ColorSwatches v-model="brush.color" />
            <label class="pe-field-label mt-3">粗细 {{ brush.size.toFixed(1) }}%</label>
            <input type="range" min="0.5" max="12" step="0.1" v-model.number="brush.size" class="pe-range" />
            <p class="pe-hint mt-3">在图片上按住拖动即可绘制</p>
          </section>

          <section v-else-if="tool === 'mosaic'" class="pe-sec">
            <p class="pe-sec-title">🧱 马赛克</p>
            <label class="pe-field-label">块大小 {{ mosaicSize.toFixed(1) }}%</label>
            <input type="range" min="1" max="15" step="0.1" v-model.number="mosaicSize" class="pe-range" />
            <p class="pe-hint mt-3">涂抹区域自动打码，可用擦除工具撤销</p>
          </section>

          <section v-else-if="tool === 'eraser'" class="pe-sec">
            <p class="pe-sec-title">◻️ 擦除</p>
            <label class="pe-field-label">橡皮大小 {{ eraserSize.toFixed(1) }}%</label>
            <input type="range" min="1" max="15" step="0.1" v-model.number="eraserSize" class="pe-range" />
            <p class="pe-hint mt-3">擦除画笔、马赛克与文字标注</p>
          </section>

          <section v-else-if="tool === 'text'" class="pe-sec">
            <p class="pe-sec-title">🔤 文字标注</p>
            <ColorSwatches v-model="textStyle.color" />
            <label class="pe-field-label mt-3">字号 {{ textStyle.size.toFixed(1) }}%</label>
            <input type="range" min="2" max="18" step="0.1" v-model.number="textStyle.size" class="pe-range" />
            <label class="pe-field-label mt-3">字体</label>
            <div class="flex gap-1.5">
              <button v-for="f in FONTS" :key="f.k" class="pe-chip"
                :class="{ on: textStyle.font === f.k }" @click="textStyle.font = f.k"
                :style="{ fontFamily: f.css }">{{ f.t }}</button>
            </div>
            <button v-if="selectedTextId !== null" class="pe-chip danger w-full mt-4" @click="deleteSelectedText">
              删除选中文字
            </button>
            <p class="pe-hint mt-3">点击图片添加文字，拖动已有文字可移动</p>
          </section>

          <span class="pe-sec-sep" />

          <!-- 滤镜预设 -->
          <section class="pe-sec">
            <p class="pe-sec-title">✨ 滤镜预设</p>
            <div class="pe-preset-grid">
              <button v-for="(p, i) in PRESETS" :key="p.name" class="pe-preset"
                :class="{ on: presetIdx === i }" @click="applyPreset(i)">{{ p.name }}</button>
            </div>
          </section>

          <span class="pe-sec-sep" />

          <!-- 精细调整 -->
          <section class="pe-sec pb-6">
            <p class="pe-sec-title">🎛 精细调整</p>
            <template v-for="g in ADJ_GROUPS" :key="g.name">
              <p class="pe-group-name">{{ g.name }}</p>
              <div v-for="s in g.keys" :key="s.key" class="pe-slider-row">
                <span>{{ s.label }}</span>
                <input type="range" :min="s.min" :max="s.max" :step="s.step"
                  v-model.number="adj[s.key]" class="pe-range"
                  @pointerdown="sliderCommitArmed = true" @change="onSliderCommitted" />
                <b :class="{ zero: adj[s.key] === s.def }">{{ adj[s.key] }}{{ s.unit }}</b>
              </div>
            </template>
            <button class="pe-chip w-full mt-2" @click="resetAdj">恢复默认调节</button>
          </section>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
// ============================================================
// PhotoEditor —— 全屏照片编辑器（纯本地 canvas 处理，不修改服务端原图）
// 能力：几何（旋转/翻转/比例裁剪）、画笔/马赛克/橡皮/文字标注、
// 12 滤镜预设、12 项精细调节（高光/阴影/色温/锐化/暗角/颗粒等）、
// 撤销重做、对比原图、JPEG/PNG/WebP 导出与复制剪贴板。
// 预览与导出共用 renderScene()，保证所见即所得。
// ============================================================
import { ref, reactive, computed, watch, nextTick, onMounted, onBeforeUnmount, h } from 'vue'
import { useToastStore } from '@/stores/modules/toast'

const props = defineProps({
  src: { type: String, required: true },
  title: { type: String, default: 'photo' }
})
const emit = defineEmits(['close'])

const toast = useToastStore()

// 内联色块组件（避免再多建一个文件）
const ColorSwatches = {
  props: { modelValue: String },
  emits: ['update:modelValue'],
  setup(p, { emit }) {
    return () => h('div', { class: 'flex flex-wrap gap-1.5 mt-2' },
      COLORS.map(c => h('button', {
        class: 'pe-swatch' + (p.modelValue === c ? ' on' : ''),
        style: { background: c },
        title: c,
        onClick: () => emit('update:modelValue', c)
      }))
    )
  }
}

// ---------------- 常量 ----------------
const TOOLS = [
  { key: 'view', icon: '✋', label: '查看' },
  { key: 'crop', icon: '✂️', label: '裁剪' },
  { key: 'brush', icon: '🖌', label: '画笔' },
  { key: 'mosaic', icon: '🧱', label: '马赛克' },
  { key: 'eraser', icon: '◻️', label: '擦除' },
  { key: 'text', icon: '🔤', label: '文字' }
]
const CROP_HANDLES = ['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w']
const RATIOS = [
  { k: 'free', t: '自由', r: 0 },
  { k: '1:1', t: '1:1', r: 1 },
  { k: '4:3', t: '4:3', r: 4 / 3 },
  { k: '3:4', t: '3:4', r: 3 / 4 },
  { k: '16:9', t: '16:9', r: 16 / 9 },
  { k: '9:16', t: '9:16', r: 9 / 16 }
]
const COLORS = ['#ffffff', '#000000', '#ff3b5c', '#ff9500', '#ffd60a', '#34c759',
  '#00c7ce', '#0a84ff', '#5e5ce6', '#bf5af2', '#ff6482', '#8e8e93']
const FONTS = [
  { k: 'sans', t: '黑体', css: '"PingFang SC","Microsoft YaHei",sans-serif' },
  { k: 'serif', t: '宋体', css: 'Georgia,"Songti SC","SimSun",serif' },
  { k: 'mono', t: '等宽', css: '"JetBrains Mono",Consolas,monospace' }
]
const EXPORT_FORMATS = [
  { k: 'jpeg', t: 'JPEG' },
  { k: 'png', t: 'PNG' },
  { k: 'webp', t: 'WebP' }
]

const ADJ_GROUPS = [
  { name: '光影', keys: [
    { key: 'brightness', label: '亮度', min: 40, max: 180, def: 100, unit: '%' },
    { key: 'contrast', label: '对比度', min: 40, max: 180, def: 100, unit: '%' },
    { key: 'highlight', label: '高光', min: -100, max: 100, def: 0, unit: '' },
    { key: 'shadow', label: '阴影', min: -100, max: 100, def: 0, unit: '' }
  ]},
  { name: '色彩', keys: [
    { key: 'saturate', label: '饱和度', min: 0, max: 220, def: 100, unit: '%' },
    { key: 'warmth', label: '色温', min: -100, max: 100, def: 0, unit: '' },
    { key: 'hue', label: '色相', min: -180, max: 180, def: 0, unit: '°' },
    { key: 'sepia', label: '褪色', min: 0, max: 100, def: 0, unit: '%' }
  ]},
  { name: '效果', keys: [
    { key: 'sharpen', label: '锐化', min: 0, max: 100, def: 0, unit: '' },
    { key: 'vignette', label: '暗角', min: 0, max: 100, def: 0, unit: '' },
    { key: 'grain', label: '颗粒', min: 0, max: 100, def: 0, unit: '' },
    { key: 'blur', label: '模糊', min: 0, max: 12, step: 0.1, def: 0, unit: 'px' }
  ]}
]
const DEFAULT_ADJ = {
  brightness: 100, contrast: 100, saturate: 100,
  highlight: 0, shadow: 0, warmth: 0,
  hue: 0, sepia: 0, sharpen: 0, vignette: 0, grain: 0, blur: 0
}
const PRESETS = [
  { name: '原图', adj: {} },
  { name: '黑白', adj: { saturate: 0, contrast: 110 } },
  { name: '复古', adj: { sepia: 38, contrast: 96, saturate: 88, warmth: 12, vignette: 18, grain: 8 } },
  { name: '冷调', adj: { warmth: -28, saturate: 104, brightness: 103 } },
  { name: '暖调', adj: { warmth: 26, saturate: 112, brightness: 102 } },
  { name: '鲜艳', adj: { saturate: 145, contrast: 110 } },
  { name: '清透', adj: { brightness: 108, contrast: 94, saturate: 96 } },
  { name: '日系', adj: { brightness: 107, contrast: 92, saturate: 86, warmth: 8, highlight: 12 } },
  { name: '胶片', adj: { sepia: 16, contrast: 90, warmth: 6, grain: 20, vignette: 16 } },
  { name: '青橙', adj: { warmth: 22, saturate: 128, contrast: 108, hue: -8 } },
  { name: '赛博', adj: { saturate: 140, contrast: 114, hue: 26, vignette: 32, highlight: -8 } },
  { name: '暗调', adj: { brightness: 88, contrast: 112, vignette: 34, shadow: 8 } }
]

// ---------------- 编辑状态 ----------------
const tool = ref('view')
const adj = reactive({ ...DEFAULT_ADJ })
const presetIdx = ref(0)
const rotation = ref(0)       // 0/90/180/270
const flipH = ref(false)
const flipV = ref(false)
const crop = reactive({ x: 0, y: 0, w: 1, h: 1 })

const paths = ref([])        // {tool:'brush'|'mosaic'|'eraser', color, size, pts:[{x,y}]}
const texts = ref([])        // {id,x,y,content,color,size,font}
let textSeq = 1

const brush = reactive({ color: '#ff3b5c', size: 3 })
const mosaicSize = ref(4)
const eraserSize = ref(5)
const textStyle = reactive({ color: '#ffffff', size: 6, font: 'sans' })
const selectedTextId = ref(null)
const editingText = ref(null)

// 视图
const viewZoom = ref(1)
const panX = ref(0)
const panY = ref(0)
const holdOriginal = ref(false)

// 历史
const past = ref([])
const future = ref([])
const sliderCommitArmed = ref(false)
const HISTORY_LIMIT = 40

// ---------------- 图片加载 ----------------
const stageEl = ref(null)
const canvasEl = ref(null)
const textInputEl = ref(null)
let img = null
const imgW = ref(0)
const imgH = ref(0)
const loadError = ref(false)
const working = ref(false)

// canvasTainted：外部图床不带 CORS 头时只能回退普通加载，
// 画布会被标记为 tainted（可预览编辑，但 toBlob 导出会被浏览器拦截）
const canvasTainted = ref(false)
function loadImage() {
  loadError.value = false
  const im = new Image()
  // 统一普通加载，不设 crossOrigin：
  // - 同源图（/photo/file/...）天然不会污染画布；
  // - 跨域图床大多不带 CORS 头，先发 crossOrigin 请求只会以 net::ERR_FAILED
  //   刷控制台红字再回退，纯噪音。这里按来源直接判定 tainted。
  let crossOrigin = false
  try { crossOrigin = !!props.src && new URL(props.src, location.href).origin !== location.origin } catch { /* 非法 URL 按同源处理 */ }
  im.onload = () => {
    img = im
    imgW.value = im.naturalWidth
    imgH.value = im.naturalHeight
    canvasTainted.value = crossOrigin
    nextTick(fitStage)
  }
  im.onerror = () => { loadError.value = true }
  im.src = props.src
}

// ---------------- 几何派生 ----------------
// 当前视图（几何变换后）的自然像素尺寸
const viewNW = computed(() => imgW.value * crop.w)
const viewNH = computed(() => imgH.value * crop.h)
const outNW = computed(() => rotation.value % 180 === 90 ? Math.round(viewNH.value) : Math.round(viewNW.value))
const outNH = computed(() => rotation.value % 180 === 90 ? Math.round(viewNW.value) : Math.round(viewNH.value))

// 画布上图片显示矩形（CSS 像素）
const drawRect = ref(null)
function computeDrawRect() {
  const stage = stageEl.value
  if (!stage || !imgW.value) return null
  const cw = stage.clientWidth, ch = stage.clientHeight
  const ar = outNW.value / outNH.value
  let dw = cw, dh = dw / ar
  if (dh > ch) { dh = ch; dw = dh * ar }
  dw *= viewZoom.value; dh *= viewZoom.value
  return { x: (cw - dw) / 2 + panX.value, y: (ch - dh) / 2 + panY.value, w: dw, h: dh }
}
const layerStyle = computed(() => drawRect.value ? {
  left: drawRect.value.x + 'px', top: drawRect.value.y + 'px',
  width: drawRect.value.w + 'px', height: drawRect.value.h + 'px'
} : {})
const textInputStyle = computed(() => {
  if (!editingText.value || !drawRect.value) return {}
  const t = editingText.value
  const r = drawRect.value
  const font = FONTS.find(f => f.k === t.font) || FONTS[0]
  return {
    left: r.x + t.x * r.w + 'px',
    top: r.y + t.y * r.h + 'px',
    fontSize: Math.max(10, t.size / 100 * r.w) + 'px',
    color: t.color,
    fontFamily: font.css
  }
})

// ---------------- 渲染管线 ----------------
const dpr = Math.min(window.devicePixelRatio || 1, 2)
let rafId = 0
// 复用离屏画布，避免每帧 GC
const off = {}
function getOff(key, w, h) {
  let c = off[key]
  if (!c) { c = document.createElement('canvas'); off[key] = c }
  if (c.width !== Math.max(1, Math.round(w)) || c.height !== Math.max(1, Math.round(h))) {
    c.width = Math.max(1, Math.round(w)); c.height = Math.max(1, Math.round(h))
  }
  return c
}

// 固定种子的颗粒点（归一化坐标）
const GRAIN_POINTS = Array.from({ length: 900 }, () => ({
  x: Math.random(), y: Math.random(), v: Math.random(), a: 0.05 + Math.random() * 0.12
}))

function scheduleRender() {
  if (rafId) return
  rafId = requestAnimationFrame(() => { rafId = 0; render() })
}

function cssFilter() {
  const a = adj
  return `brightness(${a.brightness}%) contrast(${a.contrast}%) saturate(${a.saturate}%) ` +
    `sepia(${a.sepia}%) hue-rotate(${a.hue}deg) blur(${a.blur}px)`
}

/**
 * 核心渲染：底图（滤镜/旋转/翻转/裁剪）→ 像素处理 → 标注层（画笔/马赛克/文字/橡皮）→ 暗角/颗粒
 * @param ctx 目标上下文
 * @param rect 图像矩形 {x,y,w,h}（canvas 像素坐标）
 * @param canvasW/canvasH 画布总尺寸
 */
function renderScene(ctx, rect, canvasW, canvasH) {
  ctx.clearRect(0, 0, canvasW, canvasH)
  if (!img || rect.w < 2 || rect.h < 2) return

  const original = holdOriginal.value
  const W = rect.w, H = rect.h

  // 1) 底图：crop 源矩形 + 旋转 + 翻转 + CSS 滤镜
  ctx.save()
  ctx.beginPath(); ctx.rect(rect.x, rect.y, W, H); ctx.clip()
  if (!original) ctx.filter = cssFilter()
  ctx.translate(rect.x + W / 2, rect.y + H / 2)
  const swap = rotation.value % 180 === 90
  if (rotation.value) ctx.rotate(rotation.value * Math.PI / 180)
  if (rotation.value === 90) ctx.translate(-H / 2, -W / 2)
  else if (rotation.value === 270) ctx.translate(-H / 2, -W / 2)
  else ctx.translate(-W / 2, -H / 2)
  if (flipH.value) { ctx.translate(swap ? H : W, 0); ctx.scale(-1, 1) }
  if (flipV.value) { ctx.translate(0, swap ? W : H); ctx.scale(1, -1) }
  const dw = swap ? H : W
  const dh = swap ? W : H
  ctx.drawImage(img,
    imgW.value * crop.x, imgH.value * crop.y,
    imgW.value * crop.w, imgH.value * crop.h,
    0, 0, dw, dh)
  ctx.restore()

  if (original) return

  // 2) 像素级处理：高光/阴影/色温/锐化
  if (adj.highlight || adj.shadow || adj.warmth || adj.sharpen) {
    try { pixelProcess(ctx, rect) } catch { /* 超大图偶发失败时降级跳过 */ }
  }

  // 3) 标注层
  const hasBrush = paths.value.some(p => p.tool === 'brush')
  const hasMosaic = paths.value.some(p => p.tool === 'mosaic')
  const hasErase = paths.value.some(p => p.tool === 'eraser')
  if (hasBrush || hasMosaic || hasErase || texts.value.length) {
    const A = getOff('ann', W, H)
    const actx = A.getContext('2d')
    actx.setTransform(1, 0, 0, 1, 0, 0)
    actxtClear(actx, W, H)
    actx.lineCap = 'round'; actx.lineJoin = 'round'

    // 3a 画笔
    for (const p of paths.value) {
      if (p.tool !== 'brush' || p.pts.length < 1) continue
      actx.strokeStyle = p.color
      actx.lineWidth = Math.max(0.5, p.size / 100 * W)
      actx.globalCompositeOperation = 'source-over'
      strokePath(actx, p, W, H)
    }

    // 3b 马赛克（底图快照 → 缩小放大 → 涂抹蒙版）
    if (hasMosaic) {
      const S = getOff('snap', W, H)
      S.getContext('2d').drawImage(ctx.canvas, rect.x, rect.y, W, H, 0, 0, W, H)
      const mPaths = paths.value.filter(p => p.tool === 'mosaic')
      for (const p of mPaths) {
        const bs = Math.max(2, p.size / 100 * W)
        const mw = Math.max(1, Math.round(W / bs)), mh = Math.max(1, Math.round(H / bs))
        const mSmall = getOff('ms', mw, mh)
        const ms = mSmall.getContext('2d')
        ms.imageSmoothingEnabled = true
        ms.clearRect(0, 0, mw, mh)
        ms.drawImage(S, 0, 0, W, H, 0, 0, mw, mh)
        const mFull = getOff('mf', W, H)
        const mf = mFull.getContext('2d')
        mf.imageSmoothingEnabled = false
        mf.clearRect(0, 0, W, H)
        mf.drawImage(mSmall, 0, 0, mw, mh, 0, 0, W, H)
        // 涂抹蒙版
        const P = getOff('mask', W, H)
        const pctx = P.getContext('2d')
        actxtClear(pctx, W, H)
        pctx.strokeStyle = '#fff'; pctx.lineCap = 'round'; pctx.lineJoin = 'round'
        pctx.lineWidth = bs
        strokePath(pctx, p, W, H)
        mf.globalCompositeOperation = 'destination-in'
        mf.drawImage(P, 0, 0)
        mf.globalCompositeOperation = 'source-over'
        actx.drawImage(mFull, 0, 0)
      }
    }

    // 3c 文字
    for (const t of texts.value) {
      const font = FONTS.find(f => f.k === t.font) || FONTS[0]
      const fs = Math.max(4, t.size / 100 * W)
      actx.font = `600 ${fs}px ${font.css}`
      actx.fillStyle = t.color
      actx.textAlign = 'center'; actx.textBaseline = 'middle'
      actx.shadowColor = 'rgba(0,0,0,0.5)'; actx.shadowBlur = fs * 0.12
      actx.fillText(t.content || '文字', t.x * W, t.y * H)
      actx.shadowBlur = 0
    }

    // 3d 橡皮（最后 destination-out，可擦除一切标注）
    if (hasErase) {
      actx.globalCompositeOperation = 'destination-out'
      for (const p of paths.value) {
        if (p.tool !== 'eraser') continue
        actx.lineWidth = Math.max(1, p.size / 100 * W)
        strokePath(actx, p, W, H)
      }
      actx.globalCompositeOperation = 'source-over'
    }
    ctx.drawImage(A, rect.x, rect.y)
  }

  // 4) 暗角
  if (adj.vignette > 0) {
    const g = ctx.createRadialGradient(
      rect.x + W / 2, rect.y + H / 2, Math.min(W, H) * 0.32,
      rect.x + W / 2, rect.y + H / 2, Math.max(W, H) * 0.72)
    g.addColorStop(0, 'rgba(0,0,0,0)')
    g.addColorStop(1, `rgba(0,0,0,${0.62 * adj.vignette / 100})`)
    ctx.fillStyle = g
    ctx.fillRect(rect.x, rect.y, W, H)
  }

  // 5) 颗粒
  if (adj.grain > 0) {
    const n = Math.round(GRAIN_POINTS.length * adj.grain / 100)
    ctx.save()
    ctx.beginPath(); ctx.rect(rect.x, rect.y, W, H); ctx.clip()
    for (let i = 0; i < n; i++) {
      const p = GRAIN_POINTS[i]
      const gv = p.v > 0.5 ? 255 : 0
      ctx.fillStyle = `rgba(${gv},${gv},${gv},${p.a})`
      ctx.fillRect(rect.x + p.x * W, rect.y + p.y * H, 1, 1)
    }
    ctx.restore()
  }
}

function actxtClear(c, w, h) {
  c.globalCompositeOperation = 'source-over'
  c.clearRect(0, 0, w, h)
}

function strokePath(c, p, W, H) {
  if (!p.pts.length) return
  c.beginPath()
  c.moveTo(p.pts[0].x * W, p.pts[0].y * H)
  for (let i = 1; i < p.pts.length; i++) c.lineTo(p.pts[i].x * W, p.pts[i].y * H)
  if (p.pts.length === 1) {
    // 单点：点一个圆
    c.arc(p.pts[0].x * W, p.pts[0].y * H, c.lineWidth / 2, 0, Math.PI * 2)
    c.fillStyle = c.strokeStyle
    c.fill()
  } else {
    c.stroke()
  }
}

// 像素处理：高光/阴影/色温一次遍历，锐化卷积
function pixelProcess(ctx, rect) {
  const W = Math.round(rect.w), H = Math.round(rect.h)
  let imgd = ctx.getImageData(rect.x, rect.y, W, H)
  const d = imgd.data
  const hi = adj.highlight / 100, sh = adj.shadow / 100, wm = adj.warmth / 100
  if (hi || sh || wm) {
    for (let i = 0; i < d.length; i += 4) {
      const r = d[i], g = d[i + 1], b = d[i + 2]
      const y = (0.299 * r + 0.587 * g + 0.114 * b) / 255
      if (hi) {
        const k = hi * 62 * Math.max(y - 0.5, 0) * 2
        d[i] = r + k; d[i + 1] = g + k; d[i + 2] = b + k
      }
      if (sh) {
        const k = sh * 58 * Math.max(0.5 - y, 0) * 2
        d[i] = r + k; d[i + 1] = g + k; d[i + 2] = b + k
      }
      if (wm) {
        d[i] = r + wm * 24
        d[i + 1] = g + wm * 7
        d[i + 2] = b - wm * 26
      }
    }
  }
  if (adj.sharpen > 0) {
    const amt = (adj.sharpen / 100) * 0.9
    const src = new Uint8ClampedArray(d)
    for (let yp = 1; yp < H - 1; yp++) {
      for (let xp = 1; xp < W - 1; xp++) {
        const i = (yp * W + xp) * 4
        for (let c = 0; c < 3; c++) {
          const v = src[i + c] * (1 + 4 * amt)
            - amt * (src[i - 4 + c] + src[i + 4 + c] + src[i - W * 4 + c] + src[i + W * 4 + c])
          d[i + c] = v
        }
      }
    }
  }
  ctx.putImageData(imgd, rect.x, rect.y)
}

function render() {
  const cv = canvasEl.value
  const stage = stageEl.value
  if (!cv || !stage) return
  const dprNow = dpr
  const w = Math.round(stage.clientWidth * dprNow)
  const h = Math.round(stage.clientHeight * dprNow)
  if (cv.width !== w || cv.height !== h) { cv.width = w; cv.height = h }
  const ctx = cv.getContext('2d')
  ctx.clearRect(0, 0, w, h)
  if (!img) return
  const r = computeDrawRect()
  if (!r) return
  const rr = { x: r.x * dprNow, y: r.y * dprNow, w: r.w * dprNow, h: r.h * dprNow }
  drawRect.value = r
  renderScene(ctx, rr, w, h)
}

function fitStage() {
  viewZoom.value = 1; panX.value = 0; panY.value = 0
  resizeCanvas()
}

function resizeCanvas() {
  const cv = canvasEl.value, stage = stageEl.value
  if (!cv || !stage) return
  cv.style.width = stage.clientWidth + 'px'
  cv.style.height = stage.clientHeight + 'px'
  // 立即同步像素尺寸，避免首帧停留在 canvas 默认 300x150
  const w = Math.round(stage.clientWidth * dpr), h = Math.round(stage.clientHeight * dpr)
  if (cv.width !== w || cv.height !== h) { cv.width = w; cv.height = h }
  scheduleRender()
}

let ro = null
onMounted(() => {
  document.body.style.overflow = 'hidden'
  loadImage()
  ro = new ResizeObserver(resizeCanvas)
  if (stageEl.value) {
    ro.observe(stageEl.value)
    // 非 passive 才能 preventDefault，且只在舞台区域缩放，不影响右侧面板滚动
    stageEl.value.addEventListener('wheel', onWheel, { passive: false })
  }
  window.addEventListener('keydown', onKeyDown)
})
onBeforeUnmount(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', onKeyDown)
  stageEl.value?.removeEventListener('wheel', onWheel)
  ro?.disconnect()
  if (rafId) cancelAnimationFrame(rafId)
})

// ---------------- 历史 ----------------
function snapshot() {
  return {
    adj: { ...adj },
    rotation: rotation.value, flipH: flipH.value, flipV: flipV.value,
    crop: { ...crop },
    paths: JSON.parse(JSON.stringify(paths.value)),
    texts: JSON.parse(JSON.stringify(texts.value))
  }
}
function pushHistory() {
  past.value.push(snapshot())
  if (past.value.length > HISTORY_LIMIT) past.value.shift()
  future.value = []
}
function restore(s) {
  Object.assign(adj, s.adj)
  rotation.value = s.rotation; flipH.value = s.flipH; flipV.value = s.flipV
  Object.assign(crop, s.crop)
  paths.value = s.paths
  texts.value = s.texts
  selectedTextId.value = null
  scheduleRender()
}
function undo() {
  const s = past.value.pop()
  if (!s) return
  future.value.push(snapshot())
  restore(s)
}
function redo() {
  const s = future.value.pop()
  if (!s) return
  past.value.push(snapshot())
  restore(s)
}
function onSliderCommitted() {
  if (sliderCommitArmed.value) {
    sliderCommitArmed.value = false
    pushHistory()
  }
}
// 滑条拖动过程中实时预览（预设批量赋值期间不清空高亮）
let applyingPreset = false
watch(adj, () => {
  if (!applyingPreset) presetIdx.value = -1
  scheduleRender()
}, { deep: true })
watch([rotation, flipH, flipV, holdOriginal, viewZoom, panX, panY, paths, texts, () => editingText.value?.content], () => scheduleRender(), { deep: true })

// ---------------- 工具切换 ----------------
function switchTool(k) {
  // 离开裁剪工具且未应用：丢弃草稿
  if (tool.value === 'crop' && cropDraft.value) cropDraft.value = null
  tool.value = k
  selectedTextId.value = null
  if (k === 'crop') {
    // 草稿基于当前视图归一化，默认中心 84% 选区
    cropDraft.value = { x: 0.08, y: 0.08, w: 0.84, h: 0.84 }
    keepDraftInside()
  }
}

// ---------------- 几何操作 ----------------
function mapPoints(fn) {
  paths.value.forEach(p => p.pts.forEach(pt => Object.assign(pt, fn(pt))))
  // editingText 与 texts 中对象是同一引用，无需重复映射
  texts.value.forEach(t => Object.assign(t, fn(t)))
}
function rotate90() {
  pushHistory()
  // 当前视图坐标顺时针 90°：(x,y) → (1-y, x)
  mapPoints(p => ({ x: 1 - p.y, y: p.x }))
  rotation.value = (rotation.value + 90) % 360
  scheduleRender()
}
function doFlip(axis) {
  pushHistory()
  if (axis === 'h') {
    mapPoints(p => ({ x: 1 - p.x, y: p.y }))
    flipH.value = !flipH.value
  } else {
    mapPoints(p => ({ x: p.x, y: 1 - p.y }))
    flipV.value = !flipV.value
  }
  scheduleRender()
}
function resetAll() {
  pushHistory()
  Object.assign(adj, DEFAULT_ADJ)
  presetIdx.value = 0
  rotation.value = 0; flipH.value = false; flipV.value = false
  Object.assign(crop, { x: 0, y: 0, w: 1, h: 1 })
  paths.value = []; texts.value = []
  cropDraft.value = null
  viewZoom.value = 1; panX.value = 0; panY.value = 0
  scheduleRender()
  toast.success('已重置全部编辑')
}
function resetAdj() {
  pushHistory()
  Object.assign(adj, DEFAULT_ADJ)
  presetIdx.value = 0
}
function applyPreset(i) {
  pushHistory()
  applyingPreset = true
  presetIdx.value = i
  Object.assign(adj, DEFAULT_ADJ, PRESETS[i].adj)
  nextTick(() => { applyingPreset = false })
}

// ---------------- 画布指针交互 ----------------
let gesture = null  // {kind, ...}

function eventToNorm(e) {
  const r = drawRect.value
  if (!r) return null
  return {
    x: (e.clientX - r.x) / r.w,
    y: (e.clientY - r.y) / r.h
  }
}

function onPointerDown(e) {
  if (!img || loadError.value) return
  canvasEl.value.setPointerCapture?.(e.pointerId)
  const p = eventToNorm(e)
  if (!p) return

  if (tool.value === 'view') {
    gesture = { kind: 'pan', sx: e.clientX, sy: e.clientY, px: panX.value, py: panY.value }
    return
  }
  if (tool.value === 'brush' || tool.value === 'mosaic' || tool.value === 'eraser') {
    const np = { x: clamp(p.x), y: clamp(p.y) }
    const size = tool.value === 'brush' ? brush.size
      : tool.value === 'mosaic' ? mosaicSize.value : eraserSize.value
    const path = {
      tool: tool.value,
      color: tool.value === 'brush' ? brush.color : '#fff',
      size, pts: [np]
    }
    paths.value.push(path)
    gesture = { kind: 'draw', last: np, minDist: size / 100 * 0.35 }
    scheduleRender()
    return
  }
  if (tool.value === 'text') {
    // 命中已有文字 → 拖动；否则新建
    const hit = hitText(p)
    if (hit) {
      selectedTextId.value = hit.id
      gesture = { kind: 'text-move', id: hit.id, dx: p.x - hit.x, dy: p.y - hit.y }
    } else {
      pushHistory()
      const t = {
        id: textSeq++, x: clamp(p.x), y: clamp(p.y),
        content: '', color: textStyle.color, size: textStyle.size, font: textStyle.font
      }
      texts.value.push(t)
      selectedTextId.value = t.id
      editingText.value = t
      nextTick(() => textInputEl.value?.focus())
    }
  }
}

function onPointerMove(e) {
  if (!gesture) {
    // 非拖拽时更新光标文字 hover（轻量，无需处理）
    return
  }
  if (gesture.kind === 'pan') {
    panX.value = gesture.px + (e.clientX - gesture.sx)
    panY.value = gesture.py + (e.clientY - gesture.sy)
    return
  }
  if (gesture.kind === 'draw') {
    const p = eventToNorm(e)
    if (!p) return
    const np = { x: clamp(p.x), y: clamp(p.y) }
    const d = Math.hypot(np.x - gesture.last.x, np.y - gesture.last.y)
    if (d >= gesture.minDist) {
      paths.value[paths.value.length - 1].pts.push(np)
      gesture.last = np
      scheduleRender()
    }
    return
  }
  if (gesture.kind === 'text-move') {
    const p = eventToNorm(e)
    if (!p) return
    const t = texts.value.find(x => x.id === gesture.id)
    if (t) { t.x = clamp(p.x - gesture.dx); t.y = clamp(p.y - gesture.dy); scheduleRender() }
  }
}

function onPointerUp() {
  if (gesture?.kind === 'draw') {
    const path = paths.value[paths.value.length - 1]
    if (path && path.pts.length) pushHistory()
    else paths.value.pop()
  }
  if (gesture?.kind === 'text-move') pushHistory()
  gesture = null
}

function onWheel(e) {
  if (!img) return
  const f = e.deltaY < 0 ? 1.12 : 1 / 1.12
  viewZoom.value = Math.min(Math.max(viewZoom.value * f, 0.2), 6)
}
function onDblClick() {
  if (tool.value !== 'view') return
  zoomToggle()
}
function zoomToggle() {
  viewZoom.value = viewZoom.value > 1.05 ? 1 : 2.5
  if (viewZoom.value === 1) { panX.value = 0; panY.value = 0 }
}
function zoomTo(v) {
  viewZoom.value = v
  if (v === 1) { panX.value = 0; panY.value = 0 }
}

function hitText(p) {
  for (let i = texts.value.length - 1; i >= 0; i--) {
    const t = texts.value[i]
    const fs = t.size / 100
    if (Math.abs(p.x - t.x) < fs * 2.2 && Math.abs(p.y - t.y) < fs * 0.9) return t
  }
  return null
}
function clamp(v, a = 0, b = 1) { return Math.min(b, Math.max(a, v)) }

// ---------------- 文字编辑 ----------------
function commitTextEdit() {
  const t = editingText.value
  if (!t) return
  const content = (t.content || '').trim()
  const idx = texts.value.findIndex(x => x.id === t.id)
  if (!content) {
    // 空内容：删除新建的文字（不入历史，因为新建时已入栈）
    if (idx >= 0) texts.value.splice(idx, 1)
    if (past.value.length) past.value.pop() // 撤销掉"新建空文字"这一步
  } else if (idx >= 0) {
    texts.value[idx].content = content
  }
  editingText.value = null
  scheduleRender()
}
function cancelTextEdit() {
  const t = editingText.value
  if (t) {
    const idx = texts.value.findIndex(x => x.id === t.id)
    if (idx >= 0) texts.value.splice(idx, 1)
    if (past.value.length) past.value.pop()
  }
  editingText.value = null
}
function deleteSelectedText() {
  if (selectedTextId.value === null) return
  pushHistory()
  texts.value = texts.value.filter(t => t.id !== selectedTextId.value)
  selectedTextId.value = null
}

// ---------------- 裁剪 ----------------
const cropDraft = ref(null)
const cropRatio = ref('free')
const cropBoxStyle = computed(() => {
  const d = cropDraft.value
  if (!d) return {}
  return {
    left: d.x * 100 + '%', top: d.y * 100 + '%',
    width: d.w * 100 + '%', height: d.h * 100 + '%'
  }
})

function setCropRatio(k) {
  cropRatio.value = k
  const d = cropDraft.value
  if (!d || k === 'free') return
  const r = RATIOS.find(x => x.k === k).r
  // 以中心为锚按比例调整
  const cx = d.x + d.w / 2, cy = d.y + d.h / 2
  let nw = d.w, nh = nw / r
  if (nh > 1) { nh = 1; nw = nh * r }
  d.x = clamp(cx - nw / 2); d.w = nw
  d.y = clamp(cy - nh / 2); d.h = nh
  keepDraftInside()
}

function keepDraftInside() {
  const d = cropDraft.value
  if (!d) return
  d.w = Math.min(d.w, 1); d.h = Math.min(d.h, 1)
  d.x = clamp(d.x, 0, 1 - d.w)
  d.y = clamp(d.y, 0, 1 - d.h)
}

function onCropDown(e, mode) {
  if (!cropDraft.value) return
  const r = drawRect.value
  const start = {
    x: (e.clientX - r.x) / r.w,
    y: (e.clientY - r.y) / r.h
  }
  const snapD = { ...cropDraft.value }
  const ratio = RATIOS.find(x => x.k === cropRatio.value).r
  gesture = null // 切断画布手势
  const move = (ev) => {
    const nx = clamp((ev.clientX - r.x) / r.w)
    const ny = clamp((ev.clientY - r.y) / r.h)
    const d = cropDraft.value
    const dx = nx - start.x, dy = ny - start.y
    if (mode === 'move') {
      d.x = clamp(snapD.x + dx, 0, 1 - d.w)
      d.y = clamp(snapD.y + dy, 0, 1 - d.h)
      return
    }
    let { x, y, w, h } = snapD
    if (mode.includes('e')) w = clamp(nx - x, 0.05, 1 - x)
    if (mode.includes('s')) h = clamp(ny - y, 0.05, 1 - y)
    if (mode.includes('w')) {
      const nx2 = clamp(nx, 0, x + w - 0.05)
      w = x + w - nx2; x = nx2
    }
    if (mode.includes('n')) {
      const ny2 = clamp(ny, 0, y + h - 0.05)
      h = y + h - ny2; y = ny2
    }
    // 锁定比例
    if (ratio > 0) {
      const cx = x + w / 2, cy = y + h / 2
      if (mode === 'e' || mode === 'w') {
        h = w / ratio
        y = clamp(cy - h / 2, 0, 1 - h)
        if (y === 0 || y === 1 - h) { h = Math.min(h, 1); w = h * ratio }
      } else if (mode === 'n' || mode === 's') {
        w = h * ratio
        x = clamp(cx - w / 2, 0, 1 - w)
        if (x === 0 || x === 1 - w) { w = Math.min(w, 1); h = w / ratio }
      } else {
        // 角手柄：取较大变化
        if (Math.abs(dx) > Math.abs(dy) * ratio) { h = w / ratio; y = clamp(cy - h / 2, 0, 1 - h) }
        else { w = h * ratio; x = clamp(cx - w / 2, 0, 1 - w) }
      }
    }
    d.x = x; d.y = y; d.w = w; d.h = h
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

function applyCrop() {
  const d = cropDraft.value
  if (!d) return
  pushHistory()
  // 标注坐标映射到新视图
  mapPoints(p => ({ x: (p.x - d.x) / d.w, y: (p.y - d.y) / d.h }))
  paths.value.forEach(p => { p.size = p.size / d.w })
  texts.value = texts.value.filter(t => t.x >= -0.1 && t.x <= 1.1 && t.y >= -0.1 && t.y <= 1.1)
  // 绝对 crop（相对原图）
  crop.x = crop.x + d.x * crop.w
  crop.y = crop.y + d.y * crop.h
  crop.w = d.w * crop.w
  crop.h = d.h * crop.h
  cropDraft.value = null
  cropRatio.value = 'free'
  tool.value = 'view'
  viewZoom.value = 1; panX.value = 0; panY.value = 0
  scheduleRender()
  toast.success('裁剪已应用')
}
function cancelCrop() {
  cropDraft.value = null
  cropRatio.value = 'free'
  switchTool('view')
}

// ---------------- 键盘 ----------------
function onKeyDown(e) {
  const tag = e.target.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA') return
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z' && !e.shiftKey) { e.preventDefault(); undo() }
  else if ((e.ctrlKey || e.metaKey) && (e.key.toLowerCase() === 'y' || (e.shiftKey && e.key.toLowerCase() === 'z'))) {
    e.preventDefault(); redo()
  } else if (e.key === 'Escape') close()
  else if (e.key === 'Enter' && tool.value === 'text' && !editingText.value) {
    // 快捷添加文字（中心）
  }
}
function close() { emit('close') }

// ---------------- 导出 ----------------
const exportOpen = ref(false)
const exportFmt = ref(/\.png$/i.test(props.src) ? 'png' : 'jpeg')
const exportQuality = ref(92)
const exportName = ref(safeName(props.title) + '-edited')
const exporting = ref(false)
const copying = ref(false)

function safeName(t) {
  return String(t || 'photo').replace(/[\\/:*?"<>|]/g, '_').slice(0, 40) || 'photo'
}

async function renderExportCanvas() {
  if (!img) throw new Error('image not loaded')
  const W = Math.max(1, outNW.value), H = Math.max(1, outNH.value)
  const cv = document.createElement('canvas')
  cv.width = W; cv.height = H
  const ctx = cv.getContext('2d')
  renderScene(ctx, { x: 0, y: 0, w: W, h: H }, W, H)
  return cv
}

function toBlob(cv, type, q) {
  return new Promise((resolve, reject) => {
    cv.toBlob(b => b ? resolve(b) : reject(new Error('toBlob failed')), type, q)
  })
}

async function downloadImage() {
  exportOpen.value = false
  exporting.value = true
  try {
    const cv = await renderExportCanvas()
    const mime = exportFmt.value === 'png' ? 'image/png'
      : exportFmt.value === 'webp' ? 'image/webp' : 'image/jpeg'
    const blob = await toBlob(cv, mime, exportQuality.value / 100)
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `${safeName(exportName.value)}.${exportFmt.value === 'jpeg' ? 'jpg' : exportFmt.value}`
    a.click()
    setTimeout(() => URL.revokeObjectURL(a.href), 4000)
    toast.success('图片已导出到下载目录')
  } catch (err) {
    const msg = String(err?.name || '') === 'SecurityError'
      ? '该外部图片受浏览器跨域安全策略限制无法导出，请先下载原图后编辑'
      : '导出失败：' + (err.message || '未知错误')
    toast.error(msg)
  } finally {
    exporting.value = false
    working.value = false
  }
}

async function copyImage() {
  copying.value = true
  try {
    const cv = await renderExportCanvas()
    const blob = await toBlob(cv, 'image/png')
    if (!navigator.clipboard?.write || typeof ClipboardItem === 'undefined') {
      toast.warning('当前浏览器不支持复制图片，请使用下载')
      return
    }
    await navigator.clipboard.write([new ClipboardItem({ 'image/png': blob })])
    toast.success('图片已复制到剪贴板')
  } catch (err) {
    toast.error('复制失败：' + (err.message || '请改用下载'))
  } finally {
    copying.value = false
  }
}
</script>

<style scoped>
.pe-root {
  position: fixed; inset: 0; z-index: 3300;
  display: flex; flex-direction: column;
  background: rgba(6, 6, 16, 0.96);
  backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px);
  animation: pe-fade 0.2s ease both;
}
@keyframes pe-fade { from { opacity: 0 } to { opacity: 1 } }

/* 顶栏 */
.pe-top {
  display: flex; align-items: center; gap: 0.4rem;
  padding: 0.55rem 0.9rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  flex-shrink: 0; flex-wrap: wrap;
}
.pe-btn {
  height: 32px; padding: 0 0.7rem; border-radius: 9px;
  font-size: 0.75rem; white-space: nowrap;
  display: inline-flex; align-items: center; justify-content: center; gap: 0.3rem;
  transition: all 0.18s;
}
.pe-btn.ghost {
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.09);
}
.pe-btn.ghost:hover:not(:disabled) { color: #fff; background: rgba(255, 255, 255, 0.12); }
.pe-btn.ghost.on { color: #67e8f9; border-color: rgba(103, 232, 249, 0.45); background: rgba(103, 232, 249, 0.12); }
.pe-btn.ghost:disabled { opacity: 0.35; cursor: not-allowed; }
.pe-btn.primary {
  font-weight: 600; color: #06121a;
  background: linear-gradient(90deg, #22d3ee, #a78bfa);
}
.pe-btn.primary:hover:not(:disabled) { filter: brightness(1.1); }
.pe-btn.primary:disabled { opacity: 0.6; cursor: wait; }
.pe-divider { width: 1px; height: 18px; background: rgba(255, 255, 255, 0.12); margin: 0 0.35rem; }

.pe-body { flex: 1; min-height: 0; display: flex; }

/* 左侧工具列 */
.pe-rail {
  width: 64px; flex-shrink: 0;
  display: flex; flex-direction: column; gap: 0.35rem;
  padding: 0.7rem 0.5rem;
  border-right: 1px solid rgba(255, 255, 255, 0.07);
}
.pe-rail-btn {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 0.2rem; padding: 0.55rem 0.2rem; border-radius: 11px;
  color: rgba(255, 255, 255, 0.55);
  transition: all 0.18s;
}
.pe-rail-btn:hover { color: #fff; background: rgba(255, 255, 255, 0.06); }
.pe-rail-btn.on {
  color: #67e8f9; background: rgba(103, 232, 249, 0.1);
  box-shadow: inset 0 0 0 1px rgba(103, 232, 249, 0.35);
}

/* 舞台 */
.pe-stage { position: relative; flex: 1; min-width: 0; overflow: hidden; background:
  repeating-conic-gradient(rgba(255,255,255,0.018) 0% 25%, transparent 0% 50%) 0 0 / 28px 28px,
  #07070f; }
.pe-stage canvas { position: absolute; inset: 0; display: block; touch-action: none; }
.cur-view canvas { cursor: grab; }
.cur-view canvas:active { cursor: grabbing; }
.cur-brush canvas, .cur-mosaic canvas, .cur-eraser canvas { cursor: crosshair; }
.cur-text canvas { cursor: copy; }
.cur-crop canvas { cursor: default; }

.pe-zoom-tag {
  position: absolute; left: 0.8rem; bottom: 0.7rem; pointer-events: none;
  font-size: 0.66rem; color: rgba(255, 255, 255, 0.55);
  padding: 0.25rem 0.6rem; border-radius: 999px;
  background: rgba(0, 0, 0, 0.5); border: 1px solid rgba(255, 255, 255, 0.08);
}
.pe-loaderr {
  position: absolute; inset: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  color: rgba(255, 255, 255, 0.6); font-size: 0.9rem;
}

/* 裁剪 */
.pe-crop-layer {
  position: absolute; z-index: 5;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.55);
  cursor: move;
}
.pe-crop-box {
  position: absolute; inset: 0;
  border: 1.5px solid #22d3ee;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.35);
}
.pe-crop-grid {
  position: absolute; inset: 0; pointer-events: none;
  background:
    linear-gradient(rgba(255,255,255,0.25) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.25) 1px, transparent 1px);
  background-size: 33.33% 33.33%;
}
.pe-crop-handle {
  position: absolute; width: 12px; height: 12px;
  background: #22d3ee; border: 2px solid #06121a; border-radius: 3px;
  z-index: 2;
}
.pe-crop-handle.nw { left: -7px; top: -7px; cursor: nwse-resize; }
.pe-crop-handle.n { left: calc(50% - 6px); top: -7px; cursor: ns-resize; }
.pe-crop-handle.ne { right: -7px; top: -7px; cursor: nesw-resize; }
.pe-crop-handle.e { right: -7px; top: calc(50% - 6px); cursor: ew-resize; }
.pe-crop-handle.se { right: -7px; bottom: -7px; cursor: nwse-resize; }
.pe-crop-handle.s { left: calc(50% - 6px); bottom: -7px; cursor: ns-resize; }
.pe-crop-handle.sw { left: -7px; bottom: -7px; cursor: nesw-resize; }
.pe-crop-handle.w { left: -7px; top: calc(50% - 6px); cursor: ew-resize; }

/* 文字输入浮层 */
.pe-text-input {
  position: absolute; z-index: 8;
  transform: translate(-50%, -50%);
  background: rgba(34, 211, 238, 0.08);
  border: 1px dashed rgba(103, 232, 249, 0.7);
  border-radius: 4px; padding: 2px 6px;
  outline: none; min-width: 80px; text-align: center;
  font-weight: 600; line-height: 1.2;
}

/* 右侧面板 */
.pe-panel {
  width: 268px; flex-shrink: 0;
  border-left: 1px solid rgba(255, 255, 255, 0.07);
  overflow-y: auto; padding: 0.9rem 0.9rem 1.2rem;
}
.pe-sec { margin-bottom: 0.4rem; }
.pe-sec-title { font-size: 0.78rem; font-weight: 600; color: #fff; margin-bottom: 0.6rem; }
.pe-sec-sep { display: block; height: 1px; background: rgba(255, 255, 255, 0.07); margin: 0.8rem 0; }
.pe-group-name {
  font-size: 0.66rem; color: rgba(255, 255, 255, 0.38);
  margin: 0.7rem 0 0.35rem; letter-spacing: 0.1em;
}
.pe-hint { font-size: 0.66rem; color: rgba(255, 255, 255, 0.38); line-height: 1.6; }
.pe-field-label { display: block; font-size: 0.68rem; color: rgba(255, 255, 255, 0.55); margin-bottom: 0.35rem; }

.pe-slider-row {
  display: grid; grid-template-columns: 42px 1fr 40px; align-items: center; gap: 0.5rem;
  margin-bottom: 0.45rem; font-size: 0.7rem; color: rgba(255, 255, 255, 0.7);
}
.pe-slider-row b { font-size: 0.66rem; color: #67e8f9; text-align: right; font-weight: 500; font-variant-numeric: tabular-nums; }
.pe-slider-row b.zero { color: rgba(255, 255, 255, 0.3); }
.pe-range { width: 100%; height: 4px; accent-color: #22d3ee; cursor: pointer; }

.pe-chip {
  padding: 0.3rem 0.6rem; border-radius: 8px;
  font-size: 0.68rem; color: rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.09);
  transition: all 0.18s;
}
.pe-chip:hover { color: #fff; }
.pe-chip.on { color: #67e8f9; border-color: rgba(103, 232, 249, 0.45); background: rgba(103, 232, 249, 0.12); }
.pe-chip.danger { color: #fda4af; border-color: rgba(244, 63, 94, 0.4); background: rgba(244, 63, 94, 0.1); }
.w-full { width: 100%; }

.pe-preset-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.4rem; }
.pe-preset {
  padding: 0.5rem 0.2rem; border-radius: 9px; text-align: center;
  font-size: 0.68rem; color: rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.09);
  transition: all 0.18s;
}
.pe-preset:hover { color: #fff; transform: translateY(-1px); }
.pe-preset.on { color: #06121a; font-weight: 600; background: linear-gradient(135deg, #67e8f9, #a78bfa); border-color: transparent; }

.pe-swatch {
  width: 22px; height: 22px; border-radius: 50%;
  border: 2px solid transparent;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.25);
  transition: transform 0.15s;
}
.pe-swatch:hover { transform: scale(1.15); }
.pe-swatch.on { border-color: #fff; transform: scale(1.12); }

.pe-textfield {
  width: 100%; height: 32px; padding: 0 0.6rem; border-radius: 8px;
  font-size: 0.75rem; color: #fff;
  background: rgba(255, 255, 255, 0.06); border: 1px solid rgba(255, 255, 255, 0.12);
  outline: none;
}
.pe-textfield:focus { border-color: rgba(103, 232, 249, 0.55); }

/* 导出弹层 */
.pe-export-pop {
  position: absolute; right: 1rem; top: 3.4rem; z-index: 20;
  width: 264px; padding: 1rem; border-radius: 14px;
  background: rgba(12, 12, 30, 0.96);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(16px);
}
.pe-pop-title { font-size: 0.82rem; font-weight: 600; color: #fff; margin-bottom: 0.7rem; }
.pe-warn-text {
  font-size: 0.64rem; line-height: 1.55; color: #fbbf24;
  margin-top: 0.5rem; padding: 0.45rem 0.55rem; border-radius: 8px;
  background: rgba(251, 191, 36, 0.08); border: 1px solid rgba(251, 191, 36, 0.25);
}
.pe-pop-enter-active, .pe-pop-leave-active { transition: all 0.18s ease; }
.pe-pop-enter-from, .pe-pop-leave-to { opacity: 0; transform: translateY(-8px); }

@media (max-width: 820px) {
  .pe-body { flex-direction: column; }
  .pe-rail {
    width: auto; flex-direction: row; border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.07);
    padding: 0.4rem 0.6rem; overflow-x: auto;
  }
  .pe-rail-btn { flex-direction: row; padding: 0.4rem 0.7rem; }
  .pe-panel {
    width: 100%; max-height: 42vh; border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.07);
  }
  .pe-export-pop { right: 0.6rem; width: calc(100vw - 1.2rem); }
}
</style>

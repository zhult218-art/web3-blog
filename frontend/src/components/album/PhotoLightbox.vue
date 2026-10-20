<template>
  <Teleport to="body">
    <div class="lb-root" @click.self="close">
      <!-- 爱心粒子层 -->
      <div ref="heartLayer" class="pointer-events-none absolute inset-0 overflow-hidden"></div>
      <!-- 顶栏：标题 / 工具按钮 -->
      <div class="lb-top">
        <div class="min-w-0 flex-1">
          <p class="lb-title">{{ current?.title || '图片预览' }}</p>
          <p class="lb-sub">{{ idx + 1 }} / {{ photos.length }} · {{ zoomPercent }}%</p>
        </div>
        <div class="flex items-center gap-1.5 flex-shrink-0">
          <button class="lb-tool" :class="{ active: editorOpen }" title="图片编辑（裁剪/标注/滤镜/导出）" @click="editorOpen = true">✎</button>
          <button class="lb-tool" title="旋转 90°" @click="rot += 90">⟳</button>
          <button class="lb-tool" title="重置视图" @click="resetView">⌂</button>
          <a class="lb-tool" title="下载原图" :href="current?.url || '#'" :download="safeName(current?.title)" @click.stop>⬇</a>
          <button class="lb-tool" title="关闭 (Esc)" @click="close">✕</button>
        </div>
      </div>

      <!-- 主舞台：图片 + 左右切换 -->
      <div ref="stageEl" class="lb-stage" @click.self="close">
        <div v-if="loading && !broken" class="lb-loading"><span class="lb-spinner"></span></div>
        <div v-if="broken" class="lb-broken bg-gradient-to-br" :class="gradientCls">
          <span class="text-4xl opacity-40">🖼</span>
        </div>
        <img v-else ref="imgEl" :src="current?.url" :alt="current?.title"
          class="lb-img" :class="dragging ? '' : 'lb-img-anim'" :style="imgStyle"
          draggable="false"
          @load="onImgLoad" @error="onImgError"
          @dblclick.prevent="toggleZoom" />

        <button v-if="idx > 0" class="lb-arrow left-3" title="上一张 (←)" @click.stop="go(idx - 1)">←</button>
        <button v-if="idx < photos.length - 1" class="lb-arrow right-3" title="下一张 (→)" @click.stop="go(idx + 1)">→</button>

        <!-- 缩放 HUD -->
        <div class="lb-zoom-hud">
          <button title="缩小 (-)" @click.stop="zoomBy(1 / 1.3)">−</button>
          <span class="cursor-pointer select-none" title="点击复位 1:1" @click.stop="resetView">{{ zoomPercent }}%</span>
          <button title="放大 (+)" @click.stop="zoomBy(1.3)">＋</button>
        </div>
      </div>

      <!-- 全屏图片编辑器（裁剪/标注/滤镜/导出） -->
      <PhotoEditor v-if="editorOpen && current" :src="current.url" :title="current.title" @close="editorOpen = false" />

      <!-- 信息栏：点赞 / 上传者 / 描述 -->
      <div class="lb-info" @click.stop>
        <button class="lb-like" :class="{ liked: isLiked }" :disabled="isLiked" :title="isLiked ? '已点赞' : '点赞'"
          @click="onLike">
          <svg viewBox="0 0 24 24" class="w-5 h-5" :fill="isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          <span>{{ current?.likes || 0 }}</span>
        </button>
        <div class="min-w-0 flex-1">
          <p v-if="current?.description" class="text-xs text-gray-300 leading-relaxed line-clamp-2">{{ current.description }}</p>
          <p class="text-[10px] text-gray-500 mt-0.5">上传者：{{ current?.uploader || '匿名' }} · {{ formatDate(current?.createdAt) }}</p>
        </div>
      </div>

      <!-- 缩略图条 -->
      <div v-if="photos.length > 1" class="lb-thumbs" @click.stop>
        <div class="flex gap-2 overflow-x-auto lb-thumbs-scroll px-4 py-2.5 mx-auto">
          <img v-for="(p, i) in photos" :key="p.id" :ref="el => setThumbRef(el, i)" :src="p.url" :alt="p.title"
            class="lb-thumb" :class="{ active: i === idx }" loading="lazy" @click="go(i)" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
// ============================================================
// PhotoLightbox —— 相册大图查看器
// 缩放（滚轮/双击/按钮）、平移拖拽、旋转/镜像、触屏滑动切换、
// 键盘操作、缩略图条跳转、相邻预加载；✎ 调出全屏 PhotoEditor
// 完成裁剪/标注/滤镜与本地导出。所有编辑仅在浏览器本地处理。
// ============================================================
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useToastStore } from '@/stores/modules/toast'
import { formatDayCN } from '@/utils/date'
import PhotoEditor from './PhotoEditor.vue'
import { formatDayCN as formatDate } from '@/utils/date.js'

const props = defineProps({
  photos: { type: Array, default: () => [] },
  startIndex: { type: Number, default: 0 },
  likedIds: { type: Array, default: () => [] },
  isAdmin: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'like', 'edit'])

const toast = useToastStore()

// ---------- 导航 ----------
const idx = ref(Math.min(Math.max(props.startIndex, 0), Math.max(props.photos.length - 1, 0)))
const current = computed(() => props.photos[idx.value] || null)
const broken = ref(false)
const loading = ref(true)
const preloaded = new Set()

function go(i) {
  const n = Math.min(Math.max(i, 0), props.photos.length - 1)
  if (n === idx.value) return
  idx.value = n
  broken.value = false
  loading.value = true
  resetView()
  preloadAround(n)
  nextTick(scrollThumb)
}
function preloadAround(i) {
  ;[i - 1, i + 1].forEach(j => {
    const p = props.photos[j]
    if (p && !preloaded.has(p.url)) {
      const im = new Image()
      im.src = p.url
      preloaded.add(p.url)
    }
  })
}
function onImgLoad() { loading.value = false; preloadAround(idx.value) }
function onImgError() { broken.value = true; loading.value = false }

// ---------- 视图变换：缩放 / 平移 / 旋转 / 镜像 ----------
const scale = ref(1)
const tx = ref(0)
const ty = ref(0)
const rot = ref(0)
const flipH = ref(false)
const flipV = ref(false)
const dragging = ref(false)
const zoomPercent = computed(() => Math.round(scale.value * 100))

function zoomBy(factor) {
  const next = Math.min(Math.max(scale.value * factor, 0.5), 8)
  scale.value = Math.round(next * 100) / 100
  if (scale.value === 1) { tx.value = 0; ty.value = 0 }
}
function toggleZoom() {
  if (scale.value > 1) { scale.value = 1; tx.value = 0; ty.value = 0 }
  else { scale.value = 2.5; zoomBy(1) }
}
function onWheel(e) {
  e.preventDefault()
  zoomBy(e.deltaY < 0 ? 1.15 : 1 / 1.15)
}

// 拖拽平移（仅放大后）
const stageEl = ref(null)
const imgEl = ref(null)
let dragStart = null
function onPointerDown(e) {
  if (scale.value <= 1) return
  dragging.value = true
  dragStart = { x: e.clientX, y: e.clientY, tx: tx.value, ty: ty.value }
  stageEl.value?.setPointerCapture?.(e.pointerId)
}
function onPointerMove(e) {
  if (!dragging.value || !dragStart) return
  tx.value = dragStart.tx + (e.clientX - dragStart.x)
  ty.value = dragStart.ty + (e.clientY - dragStart.y)
}
function onPointerUp() { dragging.value = false; dragStart = null }

// 触屏滑动切换
let touchStart = null
function onTouchStart(e) {
  if (scale.value > 1) return
  const t = e.changedTouches[0]
  touchStart = { x: t.clientX, y: t.clientY }
}
function onTouchEnd(e) {
  if (!touchStart || scale.value > 1) return
  const t = e.changedTouches[0]
  const dx = t.clientX - touchStart.x
  const dy = t.clientY - touchStart.y
  if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 2) go(idx.value + (dx < 0 ? 1 : -1))
  touchStart = null
}

// ---------- 全屏图片编辑器 ----------
const editorOpen = ref(false)

const imgStyle = computed(() => ({
  transform: `translate(${tx.value}px, ${ty.value}px) rotate(${rot.value}deg) scale(${flipH.value ? -scale.value : scale.value}, ${flipV.value ? -scale.value : scale.value})`,
  transition: dragging.value ? 'none' : undefined
}))

function resetView() {
  scale.value = 1; tx.value = 0; ty.value = 0; rot.value = 0
  flipH.value = false; flipV.value = false
}

function safeName(t) {
  return String(t || 'photo').replace(/[\\/:*?"<>|]/g, '_').slice(0, 40) || 'photo'
}

// ---------- 点赞 ----------
const isLiked = computed(() => props.likedIds.includes(current.value?.id))
const likeBtnEl = ref(null)
function onLike(e) {
  if (isLiked.value || !current.value) return
  spawnHearts(e?.clientX, e?.clientY)
  emit('like', current.value, e)
}
const heartLayer = ref(null)
function spawnHearts(x, y, n = 12) {
  const layer = heartLayer.value
  if (!layer) return
  for (let i = 0; i < n; i++) {
    const s = document.createElement('span')
    s.className = 'lb-heart'
    s.textContent = '♥'
    s.style.cssText = `left:${x}px;top:${y}px;font-size:${10 + Math.random() * 14}px;` +
      `--dx:${(Math.random() - 0.5) * 150}px;--dy:${-(40 + Math.random() * 120)}px;` +
      `--rot:${(Math.random() - 0.5) * 140}deg;--c:${['#f472b6', '#fb7185', '#f9a8d4', '#c4b5fd'][i % 4]};`
    layer.appendChild(s)
    s.addEventListener('animationend', () => s.remove(), { once: true })
  }
}

// ---------- 缩略图条 ----------
const thumbEls = []
function setThumbRef(el, i) { if (el) thumbEls[i] = el }
function scrollThumb() {
  thumbEls[idx.value]?.scrollIntoView({ inline: 'center', block: 'nearest', behavior: 'smooth' })
}

// ---------- 其他 ----------
function close() { emit('close') }
const gradientCls = 'from-purple-500/40 to-pink-500/40'

function onKeydown(e) {
  // 全屏编辑器打开时，所有按键交给 PhotoEditor 自己处理
  if (editorOpen.value) return
  if (e.key === 'Escape') close()
  else if (e.key === 'ArrowLeft') go(idx.value - 1)
  else if (e.key === 'ArrowRight') go(idx.value + 1)
  else if (e.key === '+' || e.key === '=') zoomBy(1.3)
  else if (e.key === '-') zoomBy(1 / 1.3)
  else if (e.key === '0') resetView()
}

onMounted(() => {
  document.body.style.overflow = 'hidden'
  window.addEventListener('keydown', onKeydown)
  stageEl.value?.addEventListener('wheel', onWheel, { passive: false })
  preloadAround(idx.value)
  nextTick(scrollThumb)
})
onBeforeUnmount(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', onKeydown)
  stageEl.value?.removeEventListener('wheel', onWheel)
})
</script>

<style scoped>
.lb-root {
  position: fixed; inset: 0; z-index: 3000;
  display: flex; flex-direction: column;
  background: rgba(4, 4, 12, 0.92);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  animation: lb-fade 0.25s ease both;
}
@keyframes lb-fade { from { opacity: 0 } to { opacity: 1 } }

/* 顶栏 */
.lb-top {
  display: flex; align-items: center; justify-content: space-between; gap: 1rem;
  padding: 0.8rem 1.2rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}
.lb-title { font-size: 0.9rem; font-weight: 600; color: #fff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lb-sub { font-size: 0.65rem; color: rgba(255, 255, 255, 0.35); margin-top: 2px; }
.lb-tool {
  width: 32px; height: 32px; border-radius: 9px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 0.85rem; color: rgba(255, 255, 255, 0.65); text-decoration: none;
  background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.2s;
}
.lb-tool:hover { color: #fff; background: rgba(255, 255, 255, 0.12); }
.lb-tool.active { color: #67e8f9; border-color: rgba(103, 232, 249, 0.4); background: rgba(103, 232, 249, 0.12); }

/* 主舞台 */
.lb-stage {
  position: relative; flex: 1; min-height: 0;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; touch-action: pan-y;
  cursor: grab;
}
.lb-img { max-width: 92%; max-height: calc(100% - 24px); object-fit: contain; border-radius: 8px; user-select: none; }
.lb-img-anim { transition: transform 0.25s ease, filter 0.25s ease; }
.lb-loading { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; pointer-events: none; }
.lb-spinner {
  width: 34px; height: 34px; border-radius: 50%;
  border: 2.5px solid rgba(255, 255, 255, 0.12); border-top-color: #22d3ee;
  animation: lb-spin 0.8s linear infinite;
}
@keyframes lb-spin { to { transform: rotate(360deg) } }
.lb-broken {
  width: min(60%, 420px); aspect-ratio: 4/3; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

/* 左右切换箭头 */
.lb-arrow {
  position: absolute; top: 50%; transform: translateY(-50%);
  width: 40px; height: 40px; border-radius: 50%;
  color: #fff; font-size: 1rem;
  background: rgba(0, 0, 0, 0.5); border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(6px);
  transition: all 0.2s;
}
.lb-arrow:hover { background: rgba(34, 211, 238, 0.25); border-color: rgba(34, 211, 238, 0.5); }
.lb-arrow.left-3 { left: 0.75rem; }
.lb-arrow.right-3 { right: 0.75rem; }

/* 缩放 HUD */
.lb-zoom-hud {
  position: absolute; left: 1rem; bottom: 0.9rem;
  display: flex; align-items: center; gap: 0.4rem;
  padding: 0.25rem 0.6rem; border-radius: 999px;
  font-size: 0.68rem; color: rgba(255, 255, 255, 0.7);
  background: rgba(0, 0, 0, 0.45); border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(6px);
}
.lb-zoom-hud button {
  width: 22px; height: 22px; border-radius: 6px; color: rgba(255, 255, 255, 0.8);
  display: inline-flex; align-items: center; justify-content: center;
  transition: background 0.2s;
}
.lb-zoom-hud button:hover { background: rgba(255, 255, 255, 0.12); }

/* 信息栏 */
.lb-info {
  display: flex; align-items: center; gap: 0.9rem;
  padding: 0.7rem 1.2rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}
.lb-like {
  display: inline-flex; align-items: center; gap: 0.35rem;
  padding: 0.35rem 0.75rem; border-radius: 999px; flex-shrink: 0;
  font-size: 0.78rem; color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.09);
  transition: all 0.2s;
}
.lb-like:hover:not(:disabled) { color: #f472b6; border-color: rgba(244, 114, 182, 0.4); transform: scale(1.05); }
.lb-like.liked { color: #f472b6; border-color: rgba(244, 114, 182, 0.45); background: rgba(244, 114, 182, 0.1); }
.lb-like.liked svg { animation: lb-pop 0.35s ease; }
@keyframes lb-pop { 40% { transform: scale(1.45) } 100% { transform: scale(1) } }

/* 缩略图条 */
.lb-thumbs { flex-shrink: 0; border-top: 1px solid rgba(255, 255, 255, 0.06); }
.lb-thumbs-scroll { scrollbar-width: thin; scrollbar-color: rgba(255, 255, 255, 0.15) transparent; max-width: 100%; }
.lb-thumb {
  width: 56px; height: 42px; object-fit: cover; border-radius: 8px; flex-shrink: 0;
  cursor: pointer; opacity: 0.45;
  border: 1.5px solid transparent;
  transition: all 0.2s;
}
.lb-thumb:hover { opacity: 0.85; }
.lb-thumb.active { opacity: 1; border-color: #22d3ee; box-shadow: 0 0 12px rgba(34, 211, 238, 0.4); }

@media (max-width: 640px) {
  .lb-arrow { width: 32px; height: 32px; }
}
</style>

<style>
/* 爱心粒子（JS 动态创建，不能用 scoped） */
.lb-heart {
  position: fixed; z-index: 3100; pointer-events: none;
  color: var(--c, #f472b6);
  text-shadow: 0 0 8px var(--c, #f472b6);
  animation: lb-heart-fly 0.9s ease-out forwards;
}
@keyframes lb-heart-fly {
  0% { transform: translate(-50%, -50%) scale(0.6) rotate(0deg); opacity: 1 }
  100% { transform: translate(calc(-50% + var(--dx)), calc(-50% + var(--dy))) rotate(var(--rot)) scale(0.2); opacity: 0 }
}
</style>

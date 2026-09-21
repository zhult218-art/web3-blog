<template>
  <!-- ============================================================
       BookReader —— 3D 书页卷曲阅读器
       生命周期动画（全程仅 transform + opacity）：
         zoom（从点击封面位置拉近放大到屏幕中心）
         → open（展开成打开的双页书）
         → reading（左右翻页 / 键盘 / 页码跳转）
         → closing（合书）后由父组件销毁
       ============================================================ -->
  <div class="br-overlay" :class="{ ready: stage !== 'zoom' }" @click.self="onOverlayClick">
    <!-- 关闭按钮（合书） -->
    <button class="br-close" @click="close" aria-label="关闭阅读">✕</button>

    <!-- 加载 / 错误态 -->
    <div v-if="loading || errorMsg" class="br-state">
      <div v-if="loading" class="br-spinner"></div>
      <template v-else>
        <span class="br-state-icon">📕</span>
        <p class="br-state-text">{{ errorMsg }}</p>
        <button class="br-retry" @click="loadAndPaginate">重新加载</button>
      </template>
    </div>

    <!-- 书本舞台 -->
    <div
      v-else
      class="br-stage"
      :class="stageClass"
      :style="stageStyle"
      ref="stageRef"
    >
      <div class="br-book" :class="{ opened: stage === 'reading' || stage === 'open' }">
        <!-- 封面（合书状态可见，翻开后隐藏） -->
        <div class="br-cover" v-show="stage !== 'reading'">
          <img v-if="book.cover" :src="book.cover" :alt="book.title" draggable="false" @contextmenu.prevent />
          <div v-else class="br-cover-fallback">{{ book.title }}</div>
          <div class="br-cover-title">{{ book.title }}</div>
        </div>

        <!-- 打开的双页书 -->
        <div class="br-open-book" v-show="stage === 'reading'">
          <!-- 左底页（偶数页 2i） -->
          <div class="br-page br-page-left">
            <div class="br-page-inner" v-html="pages[flipped * 2] || ''"></div>
            <span class="br-page-num" v-if="flipped > 0">{{ flipped * 2 + 1 }}</span>
          </div>

          <!-- 右底页（最后一张叶之后的页面） -->
          <div class="br-page br-page-right">
            <div class="br-page-inner" v-html="rightBasePage"></div>
            <span class="br-page-num">{{ (flipped + 1) * 2 }}</span>
          </div>

          <!-- 翻页叶片堆：每张叶 front=右页(奇)，back=翻过之后的左页(偶) -->
          <div
            v-for="(leaf, i) in leaves"
            :key="i"
            class="br-leaf"
            :class="{ flipped: i < flipped, current: i === flipped }"
            :style="{ zIndex: i === flipped ? 50 : i < flipped ? 1 : leaves.length - i }"
            @click="onLeafClick"
          >
            <div class="br-leaf-face br-leaf-front">
              <div class="br-page-inner" v-html="leaf.front"></div>
              <span class="br-page-num">{{ i * 2 + 2 }}</span>
            </div>
            <div class="br-leaf-face br-leaf-back">
              <div class="br-page-inner" v-html="leaf.back"></div>
              <span class="br-page-num">{{ i * 2 + 3 }}</span>
            </div>
          </div>

          <!-- 书脊中缝阴影 -->
          <div class="br-spine-shadow"></div>
        </div>
      </div>
    </div>

    <!-- 底部控制条（阅读态显示） -->
    <div class="br-controls" v-show="stage === 'reading' && !loading && !errorMsg">
      <button class="br-arrow" :disabled="flipped <= 0 || flipping" @click="prevPage" aria-label="上一页">‹</button>
      <div class="br-jump">
        <span>{{ flipped + 1 }} / {{ totalSpreads }}</span>
        <input
          type="number"
          min="1"
          :max="totalSpreads"
          v-model.number="jumpInput"
          @keyup.enter="jumpTo"
          class="br-jump-input"
        />
        <button class="br-jump-btn" @click="jumpTo">跳转</button>
      </div>
      <button class="br-arrow" :disabled="flipped >= totalSpreads - 1 || flipping" @click="nextPage" aria-label="下一页">›</button>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 逻辑：marked 解析 → 逐块渲染 → 按页面高度贪心分页
// 3D 叶片：flipped 表示已翻过的叶片数（当前摊开的是第 flipped 个跨页）
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { marked } from 'marked'
import { getClassicBookDetail } from '@/api/classicBooks'

const props = defineProps({
  book: { type: Object, required: true },
  // 拉近动画起点（封面点击位置的屏幕坐标与尺寸）
  origin: { type: Object, default: () => null },
})
const emit = defineEmits(['closed'])

const stage = ref('zoom')           // zoom → open → reading → closing
const stageRef = ref(null)
const loading = ref(true)
const errorMsg = ref('')
const pages = ref([''])             // 分页后的 HTML，每页一块
const flipped = ref(0)              // 已翻过的叶片数
const flipping = ref(false)
const jumpInput = ref(1)

// 叶片：front = 第 (2i+1) 页，back = 第 (2i+2) 页
// （数组下标 0 是封面/扉页底页，不放进叶片）
const leaves = computed(() => {
  const arr = []
  for (let i = 0; i < pages.value.length - 1; i += 2) {
    arr.push({
      front: pages.value[i + 1] || '',
      back: pages.value[i + 2] || '',
    })
  }
  return arr
})
const totalSpreads = computed(() => Math.max(1, leaves.value.length + 1))
// 右侧底页内容：所有叶片都翻完后露出的最后一页
const rightBasePage = computed(() => {
  const idx = (flipped.value + 1) * 2
  return pages.value[idx] || ''
})
const stageClass = computed(() => `stage-${stage.value}`)

// 拉近动画起点样式：用 fixed 定位把书本先放到点击封面位置，再平移到中心
const stageStyle = computed(() => {
  if (stage.value !== 'zoom' || !props.origin) return {}
  return {
    '--origin-x': `${props.origin.x}px`,
    '--origin-y': `${props.origin.y}px`,
    '--origin-w': `${props.origin.w}px`,
    '--origin-h': `${props.origin.h}px`,
  }
})

// ---------- 数据加载 + 分页 ----------
async function loadAndPaginate() {
  loading.value = true
  errorMsg.value = ''
  try {
    const detail = await getClassicBookDetail(props.book.id)
    const html = marked.parse(detail.content || '# ' + (detail.title || props.book.title))
    // 首页放标题，让封面打开后有扉页
    const titleHtml = `<h1 class="br-doc-title">${detail.title || props.book.title}</h1>` +
      (detail.summary ? `<p class="br-doc-summary">${detail.summary}</p>` : '')
    await nextTick()
    pages.value = paginateHtml(titleHtml + html)
  } catch (e) {
    errorMsg.value = '书籍内容加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

/**
 * 将整段 HTML 按 .br-page-inner 的实际可用高度贪心切成多页。
 * 做法：把顶层块逐个放入离屏测量容器，累计超高就另起一页。
 */
function paginateHtml(fullHtml) {
  // 1. 拆成顶层块（用临时容器解析）
  const tmp = document.createElement('div')
  tmp.innerHTML = fullHtml
  const blocks = Array.from(tmp.children)

  // 2. 测量容器（不可见但占据真实布局尺寸）
  const measure = document.createElement('div')
  measure.className = 'br-page-inner br-measure'
  document.body.appendChild(measure)
  const maxH = measure.clientHeight

  const result = []
  let cur = document.createElement('div')
  let curH = 0
  measure.appendChild(cur)

  for (const block of blocks) {
    cur.appendChild(block)
    const h = cur.scrollHeight
    // 当前页已有内容且放不下去 → 另起一页
    if (cur.children.length > 1 && h > maxH) {
      cur.removeChild(block)
      result.push(cur.innerHTML)
      cur = document.createElement('div')
      cur.appendChild(block)
      measure.innerHTML = ''
      measure.appendChild(cur)
    }
    curH = cur.scrollHeight
    // 单个块就超过一页（长代码块/长段落）：保留在该页，允许溢出滚动
  }
  if (cur.innerHTML.trim()) result.push(cur.innerHTML)
  document.body.removeChild(measure)

  return result.length ? result : ['<p>（暂无内容）</p>']
}

// ---------- 翻页 ----------
function nextPage() {
  if (flipped.value >= totalSpreads.value - 1 || flipping.value) return
  flipping.value = true
  flipped.value++
  jumpInput.value = flipped.value + 1
  setTimeout(() => { flipping.value = false }, 620)
}
function prevPage() {
  if (flipped.value <= 0 || flipping.value) return
  flipping.value = true
  flipped.value--
  jumpInput.value = flipped.value + 1
  setTimeout(() => { flipping.value = false }, 620)
}
function jumpTo() {
  const n = Number(jumpInput.value)
  if (!n || n < 1 || n > totalSpreads.value) return
  flipped.value = Math.min(Math.max(n - 1, 0), totalSpreads.value - 1)
}
// 点击右半页下一页、左半页上一页
function onLeafClick(ev) {
  const rect = ev.currentTarget.getBoundingClientRect()
  if (ev.clientX - rect.left > rect.width / 2) nextPage()
  else prevPage()
}

// ---------- 键盘 ----------
function onKeydown(e) {
  if (stage.value !== 'reading') return
  if (e.key === 'ArrowRight') nextPage()
  else if (e.key === 'ArrowLeft') prevPage()
  else if (e.key === 'Escape') close()
}

// ---------- 关闭（合书动画后销毁） ----------
function close() {
  if (stage.value === 'closing') return
  stage.value = 'closing'
  setTimeout(() => emit('closed'), 420)
}
function onOverlayClick() {
  close()
}

// ---------- 生命周期：zoom → open → reading ----------
onMounted(async () => {
  document.body.style.overflow = 'hidden'
  window.addEventListener('keydown', onKeydown)
  // 等一帧确保 zoom 起点样式生效，再开始拉近
  await nextTick()
  requestAnimationFrame(() => {
    stage.value = 'open'
  })
  // 同时加载内容
  await loadAndPaginate()
  // 拉近 + 展开动画完成后进入阅读态
  setTimeout(() => {
    if (stage.value !== 'closing') stage.value = 'reading'
  }, 900)
})

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ============ 遮罩 ============ */
.br-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  background: rgba(58, 52, 46, 0.55);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.4s ease-out;
  perspective: 2200px;
}
.br-overlay.ready { opacity: 1; }

.br-close {
  position: absolute;
  top: 26px;
  right: 32px;
  z-index: 5;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  transition: transform 0.25s, background 0.25s;
}
.br-close:hover { transform: rotate(90deg); background: rgba(255, 255, 255, 0.3); }

/* 状态 */
.br-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  color: rgba(255, 255, 255, 0.85);
}
.br-spinner {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.25);
  border-top-color: #fff;
  animation: br-spin 0.9s linear infinite;
}
@keyframes br-spin { to { transform: rotate(360deg); } }
.br-state-icon { font-size: 44px; }
.br-state-text { margin: 0; font-size: 14px; }
.br-retry {
  padding: 8px 22px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: transparent;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

/* ============ 书本舞台（拉近动画） ============ */
.br-stage {
  position: relative;
  transform-style: preserve-3d;
}
/* zoom 阶段：从点击封面位置放大到中心 */
.stage-zoom {
  position: fixed;
  left: var(--origin-x, 50%);
  top: var(--origin-y, 50%);
  width: var(--origin-w, 132px);
  height: var(--origin-h, 178px);
  transform: translate(-50%, -50%) scale(1);
  z-index: 3;
}
/* open / reading：书本居中、展开尺寸 */
.stage-open,
.stage-reading,
.stage-closing {
  position: relative;
  width: min(900px, 92vw);
  height: min(600px, 72vh);
  animation: br-zoom-in 0.75s cubic-bezier(0.22, 0.7, 0.3, 1) both;
}
@keyframes br-zoom-in {
  from {
    position: fixed;
    left: var(--origin-x, 50%);
    top: var(--origin-y, 50%);
    width: var(--origin-w, 132px);
    height: var(--origin-h, 178px);
    transform: translate(-50%, -50%) scale(1);
  }
  to {
    position: fixed;
    left: 50%;
    top: 48%;
    width: min(900px, 92vw);
    height: min(600px, 72vh);
    transform: translate(-50%, -50%) scale(1);
  }
}
/* 合书动画：缩小 + 透明度 */
.stage-closing {
  animation: br-zoom-out 0.4s ease-in both !important;
}
@keyframes br-zoom-out {
  from { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  to { transform: translate(-50%, -50%) scale(0.82); opacity: 0; }
}

.br-book {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
}

/* ============ 封面 ============ */
.br-cover {
  position: absolute;
  inset: 0;
  border-radius: 6px 16px 16px 6px;
  overflow: hidden;
  box-shadow: 0 30px 70px -20px rgba(0, 0, 0, 0.6);
  background: #8f8375;
  transition: opacity 0.35s ease-out, transform 0.35s ease-out;
}
.stage-open .br-cover,
.stage-reading .br-cover {
  opacity: 0;
  transform: rotateY(-70deg);
  transform-origin: left center;
}
.br-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  user-select: none;
  -webkit-user-drag: none;
}
.br-cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #a89a89, #6f6457);
  color: rgba(255, 255, 255, 0.9);
  font-size: 22px;
  letter-spacing: 0.15em;
  padding: 20px;
  text-align: center;
}
.br-cover-title {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 28px;
  text-align: center;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.5);
}

/* ============ 打开的书 ============ */
.br-open-book {
  position: absolute;
  inset: 0;
  display: flex;
  transform-style: preserve-3d;
  animation: br-open-book 0.55s ease-out 0.28s both;  /* 展开动画 */
}
@keyframes br-open-book {
  from { transform: rotateY(38deg) scale(0.9); opacity: 0; }
  to { transform: rotateY(0) scale(1); opacity: 1; }
}

.br-page {
  position: relative;
  flex: 1;
  height: 100%;
  background: linear-gradient(180deg, #fdfbf6, #f6f1e7);
  box-shadow: inset 0 0 40px rgba(140, 120, 95, 0.08);
  overflow: hidden;
}
.br-page-left {
  border-radius: 12px 0 0 12px;
  box-shadow: inset -18px 0 30px -22px rgba(90, 75, 55, 0.35);
}
.br-page-right {
  border-radius: 0 12px 12px 0;
  box-shadow: inset 18px 0 30px -22px rgba(90, 75, 55, 0.35);
}

.br-page-inner {
  position: absolute;
  inset: 0;
  padding: 42px 40px 56px;
  overflow: hidden;
  color: #4d463d;
  font-size: 14.5px;
  line-height: 1.95;
  word-break: break-word;
}
.br-page-inner :deep(h1) { font-size: 24px; color: #4a4238; margin: 0 0 18px; letter-spacing: 0.04em; }
.br-page-inner :deep(h2) { font-size: 19px; color: #5a5043; margin: 24px 0 12px; }
.br-page-inner :deep(h3) { font-size: 16px; color: #65594b; margin: 18px 0 10px; }
.br-page-inner :deep(p) { margin: 0 0 12px; }
.br-page-inner :deep(ul),
.br-page-inner :deep(ol) { margin: 0 0 12px; padding-left: 22px; }
.br-page-inner :deep(li) { margin-bottom: 6px; }
.br-page-inner :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
  user-select: none;
  -webkit-user-drag: none;
}
.br-page-inner :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 16px;
  border-left: 3px solid #b8a998;
  background: rgba(184, 169, 152, 0.12);
  color: #6f6457;
  border-radius: 0 8px 8px 0;
}
.br-page-inner :deep(code) {
  background: rgba(140, 120, 95, 0.14);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}
.br-page-inner :deep(pre) {
  background: #4a4238;
  color: #f2ede4;
  padding: 14px 16px;
  border-radius: 10px;
  overflow-x: auto;
  font-size: 12.5px;
  line-height: 1.7;
  margin: 10px 0 14px;
}
.br-page-inner :deep(pre code) { background: none; padding: 0; color: inherit; }
.br-doc-title { text-align: center; margin-top: 30% !important; }
.br-doc-summary { text-align: center; color: #8f8375; font-size: 13px; }

.br-page-num {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 11px;
  color: #b0a494;
  letter-spacing: 0.1em;
  pointer-events: none;
}

/* 离屏测量用（与真实页面同尺寸） */
.br-measure {
  position: absolute;
  left: -9999px;
  top: 0;
  width: calc((min(900px, 92vw)) / 2);
  height: min(600px, 72vh);
  visibility: hidden;
}

/* ============ 3D 翻页叶片 ============ */
.br-leaf {
  position: absolute;
  top: 0;
  left: 50%;
  width: 50%;
  height: 100%;
  transform-origin: left center;
  transform-style: preserve-3d;
  transition: transform 0.62s cubic-bezier(0.4, 0.1, 0.25, 1);  /* 柔和，无夸张弹簧 */
  z-index: 1;
  cursor: pointer;
}
.br-leaf.current { z-index: 50; }
/* 已翻过的叶片翻转到左侧（z-index 由内联样式控制，其余未翻叶片越靠前越在上层） */
.br-leaf.flipped {
  transform: rotateY(-180deg);
}

.br-leaf-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  background: linear-gradient(180deg, #fdfbf6, #f6f1e7);
  overflow: hidden;
}
.br-leaf-front {
  border-radius: 0 12px 12px 0;
  box-shadow: inset 18px 0 30px -22px rgba(90, 75, 55, 0.35);
}
.br-leaf-back {
  transform: rotateY(180deg);
  border-radius: 12px 0 0 12px;
  box-shadow: inset -18px 0 30px -22px rgba(90, 75, 55, 0.35);
}

/* 书脊中缝 */
.br-spine-shadow {
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 26px;
  transform: translateX(-50%);
  background: linear-gradient(90deg,
    rgba(90, 75, 55, 0) 0%,
    rgba(90, 75, 55, 0.18) 45%,
    rgba(60, 50, 38, 0.3) 50%,
    rgba(90, 75, 55, 0.18) 55%,
    rgba(90, 75, 55, 0) 100%);
  pointer-events: none;
  z-index: 20;
}

/* ============ 控制条 ============ */
.br-controls {
  position: absolute;
  bottom: 26px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(40, 35, 30, 0.55);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  z-index: 30;
}
.br-arrow {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
}
.br-arrow:hover:not(:disabled) { background: rgba(255, 255, 255, 0.3); transform: scale(1.08); }
.br-arrow:disabled { opacity: 0.3; cursor: not-allowed; }
.br-jump {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  white-space: nowrap;
}
.br-jump-input {
  width: 52px;
  padding: 4px 8px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 12px;
  text-align: center;
  outline: none;
}
.br-jump-btn {
  padding: 4px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
  cursor: pointer;
}
.br-jump-btn:hover { background: rgba(255, 255, 255, 0.15); }

/* ============ 移动端：单页阅读 ============ */
@media (max-width: 767px) {
  .stage-open,
  .stage-reading,
  .stage-closing {
    width: 92vw;
    height: 70vh;
  }
  @keyframes br-zoom-in {
    from {
      position: fixed;
      left: var(--origin-x, 50%);
      top: var(--origin-y, 50%);
      width: var(--origin-w, 100px);
      height: var(--origin-h, 140px);
      transform: translate(-50%, -50%) scale(1);
    }
    to {
      position: fixed;
      left: 50%;
      top: 46%;
      width: 92vw;
      height: 70vh;
      transform: translate(-50%, -50%) scale(1);
    }
  }
  .br-page-inner { padding: 28px 24px 48px; font-size: 14px; }
  .br-measure { width: 92vw; height: 70vh; }
  .br-controls { bottom: 16px; gap: 12px; }
}
</style>

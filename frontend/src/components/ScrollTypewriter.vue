<template>
  <div ref="wrap" class="scroll-tw" :class="{ done: progress >= 1 }">
    <!-- 底层占位：完整文本低透明度，保证滚动高度稳定不跳动 -->
    <div class="tw-ghost" aria-hidden="true">{{ text }}</div>
    <!-- 上层：按字符数裁剪显示，随滚动逐字写出/回收 -->
    <div class="tw-live" aria-hidden="false">
      <span>{{ visible }}</span><span v-if="showCaret" class="tw-caret"></span>
    </div>
    <div v-if="showProgress" class="tw-meter">
      <span class="bar"><i :style="{ width: (progress * 100).toFixed(1) + '%' }"></i></span>
      <span class="num">{{ charCount }} / {{ totalChars }}</span>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 滚动打字机 · 进度映射字符
// 把滚动进度映射到字符数：文本随下滑逐字写出，倒滚则回收。
// - 底层 ghost 占位保证布局高度恒定（打字过程无回流跳动）
// - rect.top 实时计算进度，天然可逆（向上滚即回收字符）
// - rAF 节流 + 字符数变化才写 DOM，长文本也流畅
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  text: { type: String, required: true },      // 支持含 \n 的多段文本
  showProgress: { type: Boolean, default: true }, // 是否显示进度条与字数
})

const wrap = ref(null)
const progress = ref(0)

const totalChars = computed(() => props.text.length)
const charCount = computed(() => Math.round(progress.value * totalChars.value))
const visible = computed(() => props.text.slice(0, charCount.value))
const showCaret = computed(() => progress.value > 0 && progress.value < 1)

let rafPending = false

// 进度 = 元素在「视口 85% → 45%」区间内的相对位移，叠加自身高度
function update() {
  rafPending = false
  const el = wrap.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const vh = window.innerHeight
  const start = vh * 0.85
  const distance = start - vh * 0.4 + rect.height
  const passed = start - rect.top
  progress.value = Math.min(1, Math.max(0, passed / distance))
}

function onScroll() {
  if (!rafPending) {
    rafPending = true
    requestAnimationFrame(update)
  }
}

onMounted(() => {
  update()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onScroll)
  if (rafPending) cancelAnimationFrame(rafPending)
})
</script>

<style scoped>
.scroll-tw { position: relative; }

/* 底层占位与上层渲染必须完全同构：同字体/行高/白空格规则 */
.tw-ghost, .tw-live {
  font-size: 0.95rem;
  line-height: 2.1;
  letter-spacing: 0.02em;
  white-space: pre-wrap;
  word-break: break-word;
  color: rgba(255,255,255,0.88);
}

.tw-ghost { visibility: hidden; user-select: none; }
.tw-live { position: absolute; inset: 0; }

/* 光标：竖条闪烁，写完隐藏 */
.tw-caret {
  display: inline-block;
  width: 2px;
  height: 1.05em;
  margin-left: 2px;
  vertical-align: -0.15em;
  background: #67e8f9;
  box-shadow: 0 0 8px rgba(103, 232, 249, 0.9);
  animation: twBlink 0.9s steps(1) infinite;
}
@keyframes twBlink { 0%, 55% { opacity: 1; } 56%, 100% { opacity: 0; } }

/* 进度读数 */
.tw-meter { display: flex; align-items: center; gap: 0.8rem; margin-top: 1.1rem; }
.tw-meter .bar { flex: 1; height: 3px; border-radius: 2px; background: rgba(255,255,255,0.08); overflow: hidden; }
.tw-meter .bar i { display: block; height: 100%; border-radius: 2px; background: linear-gradient(90deg, #667eea, #00d4ff, #67e8f9); box-shadow: 0 0 10px rgba(0,212,255,0.6); transition: width 0.1s linear; }
.tw-meter .num { font-size: 0.68rem; font-family: 'Courier New', monospace; color: rgba(103,232,249,0.75); letter-spacing: 0.1em; flex-shrink: 0; }
</style>

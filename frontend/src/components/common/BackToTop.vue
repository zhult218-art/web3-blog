<template>
  <Transition name="fade">
    <button v-if="visible" @click="scrollToTop" aria-label="回到顶部"
      class="fixed bottom-6 right-6 z-50 w-9 h-9 md:w-10 md:h-10 rounded-full glass-panel flex items-center justify-center
             hover:bg-white/10 transition-colors cursor-pointer group">
      <svg class="w-4 h-4 md:w-5 md:h-5 text-white/70 group-hover:text-white transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M5 15l7-7 7 7" />
      </svg>
    </button>
  </Transition>
</template>

<script setup>
// ============================================================
// 回到顶部组件（BackToTop）
// 滚动超过 300px 时显示浮动按钮，点击平滑回到页面顶部
// 使用 requestAnimationFrame 节流滚动检测，性能友好
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'

const visible = ref(false)
let rafId = null
let ticking = false

function onScroll() {
  if (!ticking) {
    ticking = true
    rafId = requestAnimationFrame(() => {
      visible.value = window.scrollY > 300
      ticking = false
    })
  }
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  // 初始化时检查一次当前位置
  visible.value = window.scrollY > 300
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  if (rafId) cancelAnimationFrame(rafId)
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

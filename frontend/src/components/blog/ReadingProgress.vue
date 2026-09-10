<template>
  <div class="fixed top-0 left-0 right-0 h-0.5 z-[70] bg-transparent pointer-events-none">
    <div class="h-full transition-[width] duration-150 ease-out" :style="{ width: progress + '%', background: 'linear-gradient(90deg, var(--color-primary), var(--color-accent))' }"></div>
  </div>
</template>

<script setup>
// ============================================================
// 阅读进度条：页面滚动时顶部细条填充
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'

const progress = ref(0)
const route = useRoute()
let ticking = false

function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    const doc = document.documentElement
    const max = doc.scrollHeight - doc.clientHeight
    progress.value = max > 0 ? Math.min(100, (doc.scrollTop / max) * 100) : 0
    ticking = false
  })
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

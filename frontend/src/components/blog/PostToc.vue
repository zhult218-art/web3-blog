<template>
  <nav class="panel p-4 max-h-[calc(100vh-120px)] overflow-y-auto">
    <h4 class="widget-title">文章目录</h4>
    <ul class="space-y-0.5">
      <li v-for="item in toc" :key="item.id">
        <a :href="`#${item.id}`"
          :class="['toc-item', { 'toc-active': activeId === item.id, 'toc-sub': item.level > 1 }]"
          @click.prevent="scrollTo(item.id)">
          {{ item.text }}
        </a>
      </li>
    </ul>
  </nav>
</template>

<script setup>
// ============================================================
// 文章目录（TOC）：点击平滑滚动 + 滚动时高亮当前章节
// ============================================================
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  toc: { type: Array, default: () => [] }
})

const activeId = ref('')
const observer = ref(null)

function scrollTo(id) {
  const el = document.getElementById(id)
  if (!el) return
  const y = el.getBoundingClientRect().top + window.scrollY - 88
  window.scrollTo({ top: y, behavior: 'smooth' })
  activeId.value = id
}

function setupObserver() {
  observer.value?.disconnect()
  if (!props.toc.length) return
  const targets = props.toc.map(t => document.getElementById(t.id)).filter(Boolean)
  if (!targets.length) return
  observer.value = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) activeId.value = entry.target.id
      }
    },
    { rootMargin: '-90px 0px -70% 0px', threshold: 0 }
  )
  targets.forEach(el => observer.value.observe(el))
}

watch(() => props.toc, () => {
  setTimeout(setupObserver, 120)
}, { deep: true })

onMounted(() => setTimeout(setupObserver, 120))
onBeforeUnmount(() => observer.value?.disconnect())
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }
.widget-title { @apply text-sm font-bold text-white mb-2.5; }
.toc-item {
  @apply block px-2.5 py-1 rounded-md text-xs text-gray-500 hover:text-white hover:bg-[#121230] transition-colors truncate;
  border-left: 2px solid transparent;
}
.toc-sub { @apply pl-5 text-[11px] text-gray-600; }
.toc-active {
  color: var(--color-primary);
  border-left-color: var(--color-primary);
  background: rgba(168, 85, 247, 0.08);
}
</style>

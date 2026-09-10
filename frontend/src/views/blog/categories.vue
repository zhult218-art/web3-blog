<template>
  <div class="space-y-6">
    <PageBack label="返回博客" to="/blog" />
    <h1 class="text-2xl font-black text-white">分类</h1>
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <button v-for="c in categories" :key="c.name" @click="goCategory(c.name)"
        class="panel p-5 text-left group">
        <div class="flex items-center justify-between">
          <span class="text-white font-bold group-hover:text-[color:var(--color-primary)] transition-colors">{{ c.name }}</span>
          <span class="text-xs text-gray-500">{{ c.count }} 篇</span>
        </div>
        <div class="mt-2 h-1 rounded-full bg-[#141432] overflow-hidden">
          <div class="h-full rounded-full bg-gradient-to-r from-purple-500 to-cyan-400 transition-all duration-500"
            :style="{ width: `${Math.min(100, (c.count / maxCount) * 100)}%` }"></div>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBlogCategories } from '@/api/blog'
import PageBack from '@/components/PageBack.vue'

const router = useRouter()
const categories = ref([])
const maxCount = computed(() => Math.max(1, ...categories.value.map(c => c.count || 0)))

function goCategory(name) { router.push({ path: '/blog', query: { category: name } }) }

onMounted(async () => {
  try { categories.value = (await getBlogCategories())?.data ?? [] } catch {}
})
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm p-5 hover:border-white/[0.18] hover:bg-[#121230] transition-all duration-300; }
</style>

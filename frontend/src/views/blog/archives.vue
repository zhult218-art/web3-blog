<template>
  <div class="space-y-6">
    <PageBack label="返回博客" to="/blog" />
    <h1 class="text-2xl font-black text-white">归档</h1>
    <div class="panel p-6">
      <div v-for="a in archives" :key="a.month" class="archive-row group">
        <button @click="goArchive(a.month)" class="flex items-center gap-3 text-left">
          <span class="w-2 h-2 rounded-full bg-gradient-to-br from-purple-500 to-cyan-400 group-hover:scale-125 transition-transform"></span>
          <span class="text-gray-300 font-medium group-hover:text-[color:var(--color-primary)] transition-colors">{{ a.month }}</span>
          <span class="text-xs text-gray-600">{{ a.count }} 篇</span>
        </button>
      </div>
      <p v-if="!archives.length" class="py-10 text-center text-xs text-gray-600">暂无归档</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBlogArchives } from '@/api/blog'
import PageBack from '@/components/PageBack.vue'

const router = useRouter()
const archives = ref([])

function goArchive(month) { router.push({ path: '/blog', query: { month } }) }

onMounted(async () => {
  try { archives.value = (await getBlogArchives())?.data ?? [] } catch {}
})
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }
.archive-row { @apply py-2.5 border-b border-white/[0.04] last:border-0; }
</style>

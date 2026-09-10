<template>
  <div class="space-y-6">
    <PageBack label="返回博客" to="/blog" />
    <h1 class="text-2xl font-black text-white">标签</h1>
    <div class="panel p-6 flex flex-wrap gap-2">
      <button v-for="t in tags" :key="t.name" @click="goTag(t.name)"
        class="tag-chip transition-all"
        :style="{ fontSize: `${Math.min(16, 11 + Math.min(t.count, 10))}px` }">
        {{ t.name }} <sup class="ml-0.5 opacity-60">{{ t.count }}</sup>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBlogTags } from '@/api/blog'
import PageBack from '@/components/PageBack.vue'

const router = useRouter()
const tags = ref([])

function goTag(name) { router.push({ path: '/blog', query: { tag: name } }) }

onMounted(async () => {
  try { tags.value = (await getBlogTags())?.data ?? [] } catch {}
})
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }
.tag-chip {
  @apply px-2.5 py-1 rounded-lg text-gray-400 hover:text-white hover:bg-purple-500/20 transition-colors;
}
</style>

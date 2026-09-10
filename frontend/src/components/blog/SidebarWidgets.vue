<template>
  <aside class="space-y-5">
    <!-- 作者卡 -->
    <div class="panel p-5 text-center">
      <div class="mx-auto w-16 h-16 rounded-full bg-gradient-to-br from-purple-500 to-cyan-500 flex items-center justify-center text-2xl text-white font-black shadow-lg shadow-purple-500/25 mb-3">
        {{ (author.nickname || 'W')[0] }}
      </div>
      <h3 class="text-white font-bold">{{ author.nickname || 'Web3 Portal' }}</h3>
      <p class="text-xs text-gray-500 mt-1">{{ author.slogan || '探秘元宇宙，记录成长' }}</p>
      <div class="flex justify-center gap-3 mt-3 text-gray-400">
        <a v-if="author.github" :href="author.github" target="_blank" rel="noopener" class="hover:text-white transition-colors" title="Github">
          <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2a10 10 0 00-3.16 19.49c.5.09.68-.22.68-.48v-1.7c-2.78.6-3.37-1.34-3.37-1.34-.45-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.9 1.52 2.34 1.08 2.91.83.09-.65.35-1.09.63-1.34-2.22-.25-4.56-1.11-4.56-4.94 0-1.09.39-1.98 1.03-2.68-.1-.25-.45-1.27.1-2.64 0 0 .84-.27 2.75 1.02a9.58 9.58 0 015 0c1.91-1.3 2.75-1.02 2.75-1.02.55 1.37.2 2.39.1 2.64.64.7 1.03 1.6 1.03 2.68 0 3.84-2.34 4.68-4.57 4.93.36.31.68.92.68 1.85V21c0 .27.18.58.69.48A10 10 0 0012 2z"/></svg>
        </a>
        <a v-if="author.email" :href="`mailto:${author.email}`" class="hover:text-white transition-colors" title="Mail">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 8l9 6 9-6M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>
        </a>
      </div>
    </div>

    <!-- 网站资讯 -->
    <div class="panel p-5">
      <h4 class="widget-title">网站资讯</h4>
      <ul class="space-y-2 text-sm">
        <li class="flex justify-between"><span class="text-gray-500">文章总数</span><span class="text-white">{{ stats.totalArticles ?? '--' }}</span></li>
        <li class="flex justify-between"><span class="text-gray-500">全站字数</span><span class="text-white">{{ formatWords(stats.totalWords) }}</span></li>
        <li class="flex justify-between"><span class="text-gray-500">最后更新</span><span class="text-white">{{ formatDate(stats.lastUpdated) }}</span></li>
      </ul>
    </div>

    <!-- 兴趣点（标签云） -->
    <div class="panel p-5">
      <h4 class="widget-title">兴趣点</h4>
      <p class="text-[10px] text-gray-600 mb-2">寻找你感兴趣的领域</p>
      <div class="flex flex-wrap gap-1.5">
        <span v-for="t in tagCloud" :key="t.name"
          class="cursor-pointer px-2 py-0.5 rounded-md text-[11px] transition-colors hover:bg-purple-500/20 hover:text-purple-300"
          :style="{ fontSize: `${Math.min(13, 11 + Math.min(t.count, 10))}px`, color: t.count > 5 ? 'var(--color-primary)' : '' }"
          @click="goTag(t.name)">
          {{ t.name }}<sup class="ml-0.5 text-[9px] opacity-60">{{ t.count }}</sup>
        </span>
      </div>
    </div>

    <!-- 归档 -->
    <div class="panel p-5">
      <h4 class="widget-title">
        归档
        <router-link to="/blog/archives" class="text-[10px] text-gray-500 hover:text-[color:var(--color-primary)]">查看全部 →</router-link>
      </h4>
      <ul class="space-y-1.5 text-sm">
        <li v-for="a in archives.slice(0, 8)" :key="a.month" class="flex justify-between text-gray-400">
          <span class="hover:text-[color:var(--color-primary)] cursor-pointer transition-colors" @click="goArchive(a.month)">{{ a.month }}</span>
          <span class="text-gray-600">{{ a.count }} 篇</span>
        </li>
      </ul>
    </div>

    <!-- 最近更新 -->
    <div class="panel p-5">
      <h4 class="widget-title">最近更新</h4>
      <ul class="space-y-2">
        <li v-for="r in recent" :key="r.id">
          <router-link :to="`/blog/post/${r.id}`" class="block text-sm text-gray-400 hover:text-[color:var(--color-primary)] transition-colors line-clamp-1">{{ r.title }}</router-link>
          <span class="text-[10px] text-gray-600">{{ formatDate(r.updatedAt) }}</span>
        </li>
      </ul>
    </div>

    <!-- 统计数字 -->
    <div class="panel p-4 grid grid-cols-3 divide-x divide-white/[0.06] text-center">
      <div>
        <p class="text-lg font-bold text-white">{{ stats.totalArticles ?? '--' }}</p>
        <p class="text-[10px] text-gray-500">文章</p>
      </div>
      <div>
        <p class="text-lg font-bold text-white">{{ tagCount }}</p>
        <p class="text-[10px] text-gray-500">标签</p>
      </div>
      <div>
        <p class="text-lg font-bold text-white">{{ categoryCount }}</p>
        <p class="text-[10px] text-gray-500">分类</p>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBlogStats, getBlogArchives, getBlogTags, getBlogCategories, getBlogRecent, getBlogSettings } from '@/api/blog'

const router = useRouter()
const stats = ref({})
const archives = ref([])
const tags = ref([])
const categories = ref([])
const recent = ref([])
const author = ref({})

const tagCloud = computed(() => tags.value.slice(0, 40))
const tagCount = computed(() => tags.value.length)
const categoryCount = computed(() => categories.value.length)

function formatDate(d) { return d ? String(d).slice(0, 10) : '--' }
function formatWords(n) {
  const v = Number(n || 0)
  if (v >= 10000) return (v / 10000).toFixed(1) + 'w'
  if (v >= 1000) return (v / 1000).toFixed(1) + 'k'
  return String(v)
}
function goTag(tag) { router.push({ path: '/blog', query: { tag } }) }
function goArchive(month) { router.push({ path: '/blog', query: { month } }) }

onMounted(async () => {
  try {
    const [s, a, t, c, r, cfg] = await Promise.all([
      getBlogStats(), getBlogArchives(), getBlogTags(), getBlogCategories(), getBlogRecent(), getBlogSettings()
    ])
    stats.value = s?.data ?? s ?? {}
    archives.value = a?.data ?? a ?? []
    tags.value = t?.data ?? t ?? []
    categories.value = c?.data ?? c ?? []
    recent.value = r?.data ?? r ?? []
    try { author.value = JSON.parse(cfg?.author || '{}') } catch { author.value = {} }
  } catch { /* 静默降级，显示 '--' */ }
})
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }
.widget-title { @apply text-sm font-bold text-white mb-2.5 flex items-center justify-between; }
</style>

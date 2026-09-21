<template>
  <div class="space-y-6">
    <!-- 页面 Hero Header：标题 + 副标题 + 站点资讯统计 -->
    <header class="blog-hero panel p-6 md:p-8">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div class="min-w-0">
          <div class="flex items-center gap-2 text-[11px] text-gray-500 mb-2">
            <span class="hero-dot"></span>
            <span class="tracking-[0.2em] uppercase">Aurora · Verse</span>
          </div>
          <h1 class="text-2xl md:text-3xl font-black text-white leading-snug">
            <span class="hero-title-gradient">极光数字空间</span>
          </h1>
          <p class="mt-2 text-sm text-gray-400 max-w-xl leading-relaxed">
            记录元宇宙、嵌入式、软件工程与 Web3 的实践笔记 — 在代码与电波间，搭建一座可被检索的小宇宙。
          </p>
        </div>
        <dl class="grid grid-cols-3 gap-3 text-center shrink-0">
          <div class="hero-stat">
            <dt>文章</dt>
            <dd>{{ stats.totalArticles ?? '--' }}</dd>
          </div>
          <div class="hero-stat">
            <dt>分类</dt>
            <dd>{{ categories.length || '--' }}</dd>
          </div>
          <div class="hero-stat">
            <dt>字数</dt>
            <dd>{{ formatWords(stats.totalWords) }}</dd>
          </div>
        </dl>
      </div>
    </header>

    <!-- 分类筛选条 -->
    <div class="flex flex-wrap gap-2">
      <router-link to="/blog"
        :class="['chip', !category && !tag && !month ? 'chip-active' : '']">全部</router-link>
      <button v-for="c in categories" :key="c.name" @click="toggleCategory(c.name)"
        :class="['chip', category === c.name ? 'chip-active' : '']">
        {{ c.name }} <sup class="ml-0.5 text-[9px] opacity-60">{{ c.count }}</sup>
      </button>
    </div>

    <!-- 当前筛选提示 -->
    <div v-if="category || tag || month" class="flex items-center gap-2 text-xs text-gray-400">
      <span class="px-2 py-1 rounded-md bg-purple-500/10 border border-purple-500/20 text-purple-300">
        {{ tag ? `标签：#${tag}` : month ? `归档：${month}` : `分类：${category}` }}
      </span>
      <span class="text-gray-600">·</span>
      <span class="text-gray-500">共 {{ filteredArticles.length }} 篇</span>
      <button class="text-gray-500 hover:text-white transition-colors" @click="clearFilter">清除筛选 ✕</button>
    </div>

    <!-- 文章列表 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
      <ArticleCard v-for="a in filteredArticles" :key="a.id" :article="a" />
      <template v-if="loading">
        <div v-for="i in 4" :key="`sk-${i}`" class="skeleton-card panel p-4 space-y-3">
          <div class="aspect-video w-full rounded-lg bg-white/[0.04] animate-pulse"></div>
          <div class="h-4 w-3/4 rounded bg-white/8 animate-pulse"></div>
          <div class="h-3 w-full rounded bg-white/6 animate-pulse"></div>
          <div class="h-3 w-2/3 rounded bg-white/6 animate-pulse"></div>
        </div>
      </template>
    </div>
    <div v-if="!loading && !filteredArticles.length" class="py-16 text-center text-gray-500 text-sm">
      <div class="text-4xl opacity-30 mb-2">✦</div>
      此分类下暂无文章
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1 && !month" class="flex items-center justify-center gap-2 pt-2">
      <button class="page-btn" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <button v-for="p in pageRange" :key="p" @click="changePage(p)"
        :class="['page-btn', p === page ? 'page-btn-active' : '']">{{ p }}</button>
      <button class="page-btn" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBlogList, getBlogCategories, getBlogStats } from '@/api/blog'
import ArticleCard from '@/components/blog/ArticleCard.vue'

const route = useRoute()
const router = useRouter()

const articles = ref([])
const categories = ref([])
const stats = ref({})
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

const category = computed(() => route.query.category || '')
const tag = computed(() => route.query.tag || '')
const month = computed(() => route.query.month || '')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
const pageRange = computed(() => {
  const range = []
  const start = Math.max(1, page.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) range.push(i)
  return range
})

function formatWords(n) {
  const v = Number(n || 0)
  if (v >= 10000) return (v / 10000).toFixed(1) + 'w'
  if (v >= 1000) return (v / 1000).toFixed(1) + 'k'
  return String(v || '--')
}

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: month.value ? 200 : size.value, keyword: route.query.kw || '' }
    if (category.value) params.category = category.value
    if (tag.value) params.tag = tag.value
    const res = await getBlogList(params)
    articles.value = res?.data?.records ?? res?.records ?? []
    total.value = res?.data?.total ?? res?.total ?? 0
  } catch {
    articles.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 归档月份筛选（后端不支持 month 参数，前端按创建时间过滤）
const filteredArticles = computed(() => {
  if (!month.value) return articles.value
  return articles.value.filter(a => String(a.createdAt || '').startsWith(month.value))
})

function toggleCategory(name) {
  router.push({ path: '/blog', query: { ...route.query, category: category.value === name ? '' : name } })
}
function clearFilter() {
  router.push('/blog')
}
function changePage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  fetchList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  fetchList()
  try {
    const [c, s] = await Promise.allSettled([getBlogCategories(), getBlogStats()])
    if (c.status === 'fulfilled') categories.value = c.value?.data ?? []
    if (s.status === 'fulfilled') stats.value = s.value?.data ?? s.value ?? {}
  } catch {}
})

watch(() => [route.query.category, route.query.tag, route.query.month, route.query.kw], () => {
  page.value = 1
  fetchList()
})
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }

.blog-hero {
  position: relative;
  overflow: hidden;
}
.blog-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 0% 0%, rgba(168, 85, 247, 0.15), transparent),
    radial-gradient(ellipse 60% 50% at 100% 100%, rgba(6, 182, 212, 0.12), transparent);
  pointer-events: none;
}
.hero-dot {
  @apply inline-block w-1.5 h-1.5 rounded-full;
  background: linear-gradient(135deg, #a855f7, #06b6d4);
  box-shadow: 0 0 8px rgba(168, 85, 247, 0.6);
}
.hero-title-gradient {
  background: linear-gradient(135deg, #fff 0%, #e9d5ff 50%, #67e8f9 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.hero-stat {
  @apply rounded-xl border border-white/[0.08] bg-white/[0.03] px-3 py-2 min-w-[64px];
}
.hero-stat dt {
  @apply text-[10px] text-gray-500 tracking-wider uppercase mb-0.5;
}
.hero-stat dd {
  @apply text-base font-bold text-white tabular-nums;
}
.skeleton-card { @apply rounded-2xl border border-white/[0.06]; }

.chip {
  @apply px-3 py-1.5 rounded-lg text-xs border border-white/[0.10] text-gray-400 hover:text-white hover:border-white/[0.18] transition-colors;
}
.chip-active {
  @apply text-white border-transparent;
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.25), rgba(6, 182, 212, 0.2));
  color: #fff;
}
.page-btn {
  @apply min-w-8 h-8 px-2.5 rounded-lg text-xs text-gray-400 border border-white/[0.10] hover:text-white hover:border-white/[0.18] transition-colors disabled:opacity-40 disabled:cursor-not-allowed;
}
.page-btn-active {
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.25), rgba(6, 182, 212, 0.2));
  color: #fff;
  border-color: transparent;
}
</style>

<template>
  <div class="space-y-6">
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
      <button class="text-gray-500 hover:text-white transition-colors" @click="clearFilter">清除筛选 ✕</button>
    </div>

    <!-- 文章列表 -->
    <div class="space-y-5">
      <ArticleCard v-for="a in filteredArticles" :key="a.id" :article="a" />
      <div v-if="loading" class="py-16 flex flex-col items-center gap-3 text-gray-500">
        <div class="h-8 w-8 rounded-full border-2 border-white/15 border-t-cyan-400 animate-spin"></div>
        <span class="text-xs">加载中...</span>
      </div>
      <div v-else-if="!filteredArticles.length" class="py-16 text-center text-gray-500 text-sm">暂无文章</div>
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
import { getBlogList, getBlogCategories } from '@/api/blog'
import ArticleCard from '@/components/blog/ArticleCard.vue'

const route = useRoute()
const router = useRouter()

const articles = ref([])
const categories = ref([])
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
  try { categories.value = (await getBlogCategories())?.data ?? [] } catch {}
})

watch(() => [route.query.category, route.query.tag, route.query.month, route.query.kw], () => {
  page.value = 1
  fetchList()
})
</script>

<style scoped>
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

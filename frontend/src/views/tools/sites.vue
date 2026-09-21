<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <PageBack label="返回工具箱" to="/tools" class="mb-5" />
      <div class="flex items-end justify-between gap-4 mb-6">
        <div>
          <h1 class="text-3xl font-bold text-gradient-cyber">分享网站</h1>
          <p class="text-sm text-gray-500 matrix-text mt-1">优质外链收藏 · AI Tools · Design · Resources</p>
        </div>
        <div class="hidden sm:flex items-center gap-2 shrink-0">
          <span class="px-2.5 py-1 rounded-full bg-white/5 border border-white/10 text-[11px] text-gray-300">
            <span class="text-cyan-300 font-semibold">{{ matchedSites.length }}</span> 站点
          </span>
          <span class="px-2.5 py-1 rounded-full bg-white/5 border border-white/10 text-[11px] text-gray-300">
            <span class="text-purple-300 font-semibold">{{ categories.length }}</span> 分类
          </span>
        </div>
      </div>

      <!-- 搜索框 -->
      <div class="relative mb-6">
        <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input v-model="keyword" type="text" placeholder="搜索名称或描述…"
          class="w-full pl-10 pr-10 py-2.5 text-sm rounded-xl bg-[#0e0e2c] border border-white/10 text-gray-200 placeholder-gray-600
                 focus:outline-none focus:border-cyan-400/40 focus:ring-2 focus:ring-cyan-400/10 transition" />
        <button v-if="keyword" @click="keyword = ''"
          class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-white text-lg leading-none">×</button>
      </div>

      <!-- Category Tabs（emoji + 数量徽章） -->
      <div class="flex flex-wrap gap-2 mb-8">
        <button v-for="cat in tabs" :key="cat.key" @click="tab = cat.key"
          :class="['inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[11px] transition',
                   tab === cat.key
                     ? 'bg-purple-500/25 text-purple-100 border border-purple-400/40 shadow-[0_0_12px_rgba(168,85,247,0.25)]'
                     : 'border border-white/10 text-gray-400 hover:text-white hover:border-white/25']">
          <span class="text-sm leading-none">{{ cat.icon }}</span>
          <span>{{ cat.label }}</span>
          <span :class="['ml-0.5 px-1.5 py-px rounded-full text-[10px] leading-none font-semibold',
                         tab === cat.key ? 'bg-purple-400/30 text-purple-100' : 'bg-white/8 text-gray-400']">
            {{ cat.count }}
          </span>
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="py-16"><Loading /></div>

      <!-- 空数据 / 无匹配 -->
      <div v-else-if="!filtered.length" class="py-16 text-center">
        <div class="text-5xl mb-3 opacity-60">🔍</div>
        <p class="text-sm text-gray-500">
          {{ keyword ? `没有找到与「${keyword}」相关的内容` : '暂无数据，等管理员来添加哦~' }}
        </p>
      </div>

      <!-- 站点卡片 -->
      <div v-else>
        <!-- 全部 Tab：按分类分组 + 分类计数 -->
        <template v-if="tab === '全部'">
          <div v-for="g in groupedByCategory" :key="g.category" class="mb-7">
            <div class="flex items-center gap-2 mb-3">
              <span class="text-base">{{ g.icon }}</span>
              <h2 class="text-sm font-semibold text-gray-200">{{ g.label }}</h2>
              <span class="text-[10px] px-1.5 py-px rounded-full bg-white/8 text-gray-400 font-semibold">{{ g.list.length }}</span>
              <div class="flex-1 h-px bg-gradient-to-r from-white/10 to-transparent ml-1"></div>
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
              <a v-for="s in g.list" :key="s.id" :href="s.url" target="_blank" rel="noopener"
                class="site-card glass-panel-sm p-4 text-center cursor-pointer group relative transition-all duration-300 hover:-translate-y-1">
                <span class="ext-arr absolute top-2 right-2.5 text-[11px] text-cyan-300/0 group-hover:text-cyan-300/80 transition-colors">↗</span>
                <span class="text-2xl block transition-transform duration-300 group-hover:scale-110">{{ s.icon || '🔗' }}</span>
                <p class="mt-2 text-xs text-white group-hover:text-cyan-300 transition font-medium truncate" v-html="highlight(s.name)"></p>
                <p class="text-[10px] text-gray-500 mt-1 line-clamp-2 min-h-[26px]" v-html="highlight(s.description)"></p>
                <p class="text-[9px] text-gray-600 mt-1.5 truncate">{{ domainOf(s.url) }}</p>
              </a>
            </div>
          </div>
        </template>

        <!-- AI 导航 Tab：本地数据分组网格（来源 carrot-main） -->
        <template v-else-if="tab === '__ainav__'">
          <div v-for="g in aiGroupsFiltered" :key="g.id" class="mb-7">
            <div class="flex items-center gap-2 mb-3">
              <span class="text-base">{{ g.icon }}</span>
              <h2 class="text-sm font-semibold text-gray-200">{{ g.name }}</h2>
              <span class="text-[10px] px-1.5 rounded-full bg-white/8 text-gray-400">{{ g.sites.length }}</span>
              <div class="flex-1 h-px bg-gradient-to-r from-white/10 to-transparent ml-1"></div>
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
              <a v-for="s in g.sites" :key="s.url + s.name" :href="s.url" target="_blank" rel="noopener"
                class="site-card glass-panel-sm p-4 text-center cursor-pointer group relative transition-all duration-300 hover:-translate-y-1">
                <span class="ext-arr absolute top-2 right-2.5 text-[11px] text-cyan-300/0 group-hover:text-cyan-300/80 transition-colors">↗</span>
                <span class="text-2xl block transition-transform duration-300 group-hover:scale-110">🔮</span>
                <p class="mt-2 text-xs text-white group-hover:text-cyan-300 transition font-medium truncate" v-html="highlight(s.name)"></p>
                <p class="text-[10px] text-gray-500 mt-1 line-clamp-2 min-h-[26px]" v-html="highlight(s.desc)"></p>
                <p class="text-[9px] text-gray-600 mt-1.5 truncate">{{ domainOf(s.url) }}</p>
              </a>
            </div>
          </div>
        </template>

        <!-- 单分类 Tab：扁平网格 -->
        <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
          <a v-for="s in filtered" :key="s.id" :href="s.url" target="_blank" rel="noopener"
            class="site-card glass-panel-sm p-4 text-center cursor-pointer group relative transition-all duration-300 hover:-translate-y-1">
            <span class="ext-arr absolute top-2 right-2.5 text-[11px] text-cyan-300/0 group-hover:text-cyan-300/80 transition-colors">↗</span>
            <span class="text-2xl block transition-transform duration-300 group-hover:scale-110">{{ s.icon || '🔗' }}</span>
            <p class="mt-2 text-xs text-white group-hover:text-cyan-300 transition font-medium truncate" v-html="highlight(s.name)"></p>
            <p class="text-[10px] text-gray-500 mt-1 line-clamp-2 min-h-[26px]" v-html="highlight(s.description)"></p>
            <p class="text-[9px] text-gray-600 mt-1.5 truncate">{{ domainOf(s.url) }}</p>
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 分享网站页：分类筛选 + 搜索高亮 + 图标卡片网格，
// 点击卡片外链新窗口打开（数据来自 tool-service）
// ====================================================
import { ref, computed, onMounted } from 'vue'
import { getSiteList, getSiteCategories } from '@/api/sites'
import { aiSiteCategories } from '@/data/aiSites'
import Loading from '@/components/common/Loading.vue'
import PageBack from '@/components/PageBack.vue'

const sites = ref([])
const categories = ref([])
const tab = ref('全部')
const keyword = ref('')
const loading = ref(true)

// 拆出 emoji 与文本（emoji 始终在分类名首部）
function splitEmoji(cat) {
  if (!cat) return { icon: '🗂️', label: '其他' }
  const arr = [...cat]
  // emoji 通常占 1-2 个 code point；取首字符即可
  const first = arr[0] || ''
  // 简单判定：非 ASCII 字符且非汉字/字母数字
  if (/\p{Extended_Pictographic}/u.test(first)) {
    return { icon: first, label: arr.slice(first.includes('\u200d') ? 2 : 1).join('').trim() }
  }
  return { icon: '🗂️', label: cat }
}

const searchTerm = computed(() => keyword.value.trim().toLowerCase())

// 命中搜索的站点全集
const matchedSites = computed(() => {
  if (!searchTerm.value) return sites.value
  return sites.value.filter(s =>
    (s.name || '').toLowerCase().includes(searchTerm.value) ||
    (s.description || '').toLowerCase().includes(searchTerm.value)
  )
})

// 按分类归组的站点映射（仅命中搜索的部分）
const byCategory = computed(() => {
  const map = {}
  for (const s of matchedSites.value) {
    const c = s.category || '其他'
    ;(map[c] = map[c] || []).push(s)
  }
  return map
})

// AI 导航本地数据（carrot-main 提炼），按关键词过滤
const aiGroupsFiltered = computed(() => {
  const kw = searchTerm.value
  return aiSiteCategories
    .map(c => ({ ...c, sites: kw ? c.sites.filter(s => (s.name + s.desc).toLowerCase().includes(kw)) : c.sites }))
    .filter(c => c.sites.length)
})
const aiTotal = computed(() => aiGroupsFiltered.value.reduce((n, c) => n + c.sites.length, 0))

// Tab 配置：全部 + AI导航（本地） + 各分类（含数量、emoji、文本）
const tabs = computed(() => {
  const list = [{
    key: '全部',
    icon: '🌐',
    label: '全部',
    count: matchedSites.value.length
  }, {
    key: '__ainav__',
    icon: '🤖',
    label: 'AI导航',
    count: aiTotal.value
  }]
  for (const c of categories.value) {
    const { icon, label } = splitEmoji(c)
    list.push({ key: c, icon, label: label || c, count: (byCategory.value[c] || []).length })
  }
  return list
})

// 当前 Tab 下的站点
const filtered = computed(() => {
  if (tab.value === '全部') return matchedSites.value
  if (tab.value === '__ainav__') return aiGroupsFiltered.value.flatMap(g => g.sites)
  return byCategory.value[tab.value] || []
})

// 全部 Tab 下按 sort 升序分组
const groupedByCategory = computed(() => {
  return Object.keys(byCategory.value)
    .map(cat => {
      const { icon, label } = splitEmoji(cat)
      const list = [...(byCategory.value[cat] || [])].sort((a, b) => (a.sort || 0) - (b.sort || 0))
      return { category: cat, icon, label, list }
    })
    .sort((a, b) => {
      // 按分类首个站点的 sort 排
      const sa = a.list[0]?.sort || 0
      const sb = b.list[0]?.sort || 0
      return sa - sb
    })
})

// 搜索高亮：返回带 <mark> 的 HTML（已做 HTML 转义）
function escapeHtml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
function highlight(text) {
  const safe = escapeHtml(text || '')
  const q = keyword.value.trim()
  if (!q) return safe
  // 转义正则元字符
  const esc = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return safe.replace(new RegExp(esc, 'gi'),
    m => `<mark class="bg-cyan-400/30 text-cyan-100 rounded px-0.5">${m}</mark>`)
}

// 提取域名用于卡片底部展示
function domainOf(url) {
  try {
    return String(url || '').replace(/^https?:\/\//, '').replace(/^www\./, '').split('/')[0]
  } catch { return '' }
}

onMounted(async () => {
  try {
    const res = await getSiteList({ onlyVisible: true })
    sites.value = res.data || []
  } catch {}
  try {
    const res = await getSiteCategories()
    categories.value = res.data || []
  } catch {}
  loading.value = false
})
</script>

<style scoped>
/* 卡片 hover：缩放 + 发光 */
.site-card {
  transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
}
.site-card:hover {
  transform: translateY(-4px) scale(1.03);
  border-color: rgba(34, 211, 238, 0.4);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5), 0 0 20px rgba(34, 211, 238, 0.25);
}
</style>

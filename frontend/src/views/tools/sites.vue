<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <PageBack label="返回工具箱" to="/tools" class="mb-5" />
      <h1 class="text-3xl font-bold mb-2 text-gradient-cyber">分享网站</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">优质外链收藏 · AI Tools · Design · Resources</p>

      <!-- Category Tabs -->
      <div class="flex flex-wrap gap-2 mb-8">
        <button v-for="cat in tabs" :key="cat.key" @click="tab = cat.key"
          :class="['px-2.5 py-1 rounded-full text-[11px] transition', tab === cat.key ? 'bg-purple-500/30 text-purple-200 border border-purple-400/30' : 'border border-white/10 text-gray-500 hover:text-white']">
          {{ cat.label }} <span class="opacity-60">{{ cat.count }}</span>
        </button>
      </div>

      <!-- Site Cards -->
      <div v-if="filtered.length" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
        <a v-for="s in filtered" :key="s.id" :href="s.url" target="_blank" rel="noopener"
          class="glass-panel-sm p-4 text-center cursor-pointer hover:border-cyan-400/30 group transition-all duration-300 hover:-translate-y-1">
          <span class="text-2xl">{{ s.icon || '🔗' }}</span>
          <p class="mt-2 text-xs text-white group-hover:text-cyan-300 transition font-medium truncate">{{ s.name }}</p>
          <p class="text-[10px] text-gray-500 mt-1 line-clamp-2 min-h-[26px]">{{ s.description }}</p>
        </a>
      </div>
      <div v-else class="py-16"><Loading /></div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 分享网站页：分类筛选 + 图标卡片网格，
// 点击卡片外链新窗口打开（数据来自 tool-service）
// ====================================================
import { ref, computed, onMounted } from 'vue'
import { getSiteList, getSiteCategories } from '@/api/sites'
import Loading from '@/components/common/Loading.vue'
import PageBack from '@/components/PageBack.vue'

const sites = ref([])
const categories = ref([])
const tab = ref('全部')

// 按分类归组的站点映射
const byCategory = computed(() => {
  const map = {}
  for (const s of sites.value) {
    const c = s.category || '其他'
    ;(map[c] = map[c] || []).push(s)
  }
  return map
})

// Tab 配置：全部 + 各分类（含数量）
const tabs = computed(() => {
  const list = [{ key: '全部', label: '全部', count: sites.value.length }]
  for (const c of categories.value) {
    list.push({ key: c, label: c, count: (byCategory.value[c] || []).length })
  }
  return list
})

// 当前 Tab 下的站点
const filtered = computed(() => {
  if (tab.value === '全部') return sites.value
  return byCategory.value[tab.value] || []
})

onMounted(async () => {
  try {
    const res = await getSiteList({ onlyVisible: true })
    sites.value = res.data || []
  } catch {}
  try {
    const res = await getSiteCategories()
    categories.value = res.data || []
  } catch {}
})
</script>
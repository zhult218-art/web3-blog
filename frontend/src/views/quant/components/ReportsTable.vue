<template>
  <div v-if="items.length" class="space-y-2 max-h-72 overflow-y-auto">
    <div v-for="(r, i) in items" :key="(r.title || '') + i" class="p-3 rounded-lg bg-[#a855f7]/[0.04] border border-[#a855f7]/20">
      <a v-if="r.pdfUrl" :href="r.pdfUrl" target="_blank" rel="noopener"
         class="text-xs text-[#ece8ff]/90 hover:text-cyan-300 transition">{{ r.title }}</a>
      <div v-else class="text-xs text-[#ece8ff]/80">{{ r.title }}</div>
      <div class="flex items-center gap-3 mt-1.5 flex-wrap">
        <span class="text-[10px] text-[#7d8fc4]">{{ r.institution }}</span>
        <span class="text-[10px] text-[#7d8fc4]">{{ r.date }}</span>
        <span v-if="r.rating" :class="['text-[10px] px-1.5 py-0.5 rounded-full',
          r.rating.includes('买入') ? 'bg-red-500/10 text-red-400' :
          r.rating.includes('增持') ? 'bg-orange-500/10 text-orange-400' :
          r.rating.includes('减持') || r.rating.includes('卖出') ? 'bg-green-500/10 text-green-400' :
          'bg-[#a855f7]/15 text-[#c084fc]']">{{ r.rating }}</span>
        <span v-if="r.pdfUrl" class="text-[10px] text-cyan-400/60">PDF</span>
      </div>
    </div>
  </div>
  <div v-else class="text-xs text-[#7d8fc4] py-4">暂无研报数据</div>
</template>

// 券商研报组件：个股最新研报标题/机构/评级/发布日期，标题可点击打开 PDF（东财研报源）
<script setup>
import { ref, onMounted } from 'vue'
import { getReports } from '@/api/quant'

// 接收股票代码属性
const props = defineProps({ code: String })
const items = ref([])

// 东财原始字段 → 组件字段
function mapRow(r) {
  return {
    title: r.title || '',
    institution: r.orgSName || r.orgName || '',
    date: String(r.publishDate || '').slice(0, 10),
    rating: r.emRatingName || r.sRatingName || '',
    pdfUrl: r.pdfUrl || null,
  }
}

// 挂载时拉取研报数据，失败则置空
onMounted(async () => {
  try {
    const res = await getReports(props.code)
    const list = Array.isArray(res.data) ? res.data : []
    items.value = list.slice(0, 20).map(mapRow)
  } catch (e) { items.value = [] }
})
</script>

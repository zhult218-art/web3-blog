<template>
  <div v-if="items.length" class="space-y-2 max-h-60 overflow-y-auto">
    <div v-for="r in items" :key="r.title + r.date" class="p-3 rounded-lg bg-[#a855f7]/[0.04] border border-[#a855f7]/20">
      <div class="text-xs text-[#ece8ff]/80">{{ r.title }}</div>
      <div class="flex items-center gap-3 mt-1.5">
        <span class="text-[10px] text-[#7d8fc4]">{{ r.institution }}</span>
        <span class="text-[10px] text-[#7d8fc4]">{{ r.date }}</span>
        <span v-if="r.rating" :class="['text-[10px] px-1.5 py-0.5 rounded-full',
          r.rating.includes('买入') ? 'bg-red-500/10 text-red-400' :
          r.rating.includes('增持') ? 'bg-orange-500/10 text-orange-400' :
          'bg-[#a855f7]/15 text-[#c084fc]']">{{ r.rating }}</span>
      </div>
    </div>
  </div>
  <div v-else class="text-xs text-[#7d8fc4] py-4">暂无研报数据</div>
</template>

// 券商研报表格组件：展示个股最新研报标题/机构/评级
<script setup>
import { ref, onMounted } from 'vue'
import { getReports } from '@/api/quant'

// 接收股票代码属性
const props = defineProps({ code: String })
// 研报列表
const items = ref([])

// 挂载时拉取研报数据，失败则置空
onMounted(async () => {
  try {
    const res = await getReports(props.code)
    items.value = res.data || []
  } catch (e) { items.value = [] }
})
</script>

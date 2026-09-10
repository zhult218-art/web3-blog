<template>
  <div v-if="fundData.length" class="space-y-2 max-h-60 overflow-y-auto">
    <div v-for="f in fundData" :key="f.date" class="flex items-center justify-between py-2 px-3 rounded-lg bg-[#151538]">
      <span class="text-[10px] text-[#93c5fd]/70 matrix-text w-20">{{ f.date }}</span>
      <div class="flex-1 text-center">
        <div class="text-[10px]" :class="(f.main_net || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
          {{ (f.main_net || 0) >= 0 ? '+' : '' }}{{ f.main_net }}
        </div>
        <div class="text-[9px] text-[#7d8fc4]">主力净流入</div>
      </div>
      <div class="text-[10px] text-[#93c5fd]/70 w-16 text-right">{{ f.retail_net || '--' }}</div>
    </div>
  </div>
  <div v-else class="text-xs text-[#7d8fc4] py-4">暂无资金流向数据</div>
</template>

// 资金流向组件：展示个股每日主力/散户净流入明细
<script setup>
import { ref, onMounted } from 'vue'
import { getFundFlow } from '@/api/quant'

// 接收股票代码属性
const props = defineProps({ code: String })
// 资金流向列表
const fundData = ref([])

// 挂载时拉取资金流向数据，失败则置空
onMounted(async () => {
  try {
    const res = await getFundFlow(props.code)
    fundData.value = res.data || []
  } catch (e) { fundData.value = [] }
})
</script>

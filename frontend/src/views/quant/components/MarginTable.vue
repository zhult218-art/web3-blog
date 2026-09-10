<template>
  <div v-if="items.length" class="overflow-x-auto max-h-60">
    <table class="web3-table text-xs">
      <thead>
        <tr><th>日期</th><th>融资余额(万)</th><th>融券余额(万)</th><th>融资买入(万)</th><th>融券卖出(万)</th><th>净融资(万)</th></tr>
      </thead>
      <tbody>
        <tr v-for="d in items" :key="d.date">
          <td class="matrix-text">{{ d.date }}</td>
          <td class="text-[#ece8ff]/70">{{ d.finance_balance }}</td>
          <td class="text-[#ece8ff]/70">{{ d.short_balance }}</td>
          <td class="text-red-400">{{ d.finance_buy }}</td>
          <td class="text-green-400">{{ d.short_sell }}</td>
          <td :class="(d.net_finance || 0) >= 0 ? 'text-red-400' : 'text-green-400'">{{ (d.net_finance || 0) >= 0 ? '+' : '' }}{{ d.net_finance }}</td>
        </tr>
      </tbody>
    </table>
  </div>
  <div v-else class="text-xs text-[#7d8fc4] py-4">暂无融资融券数据</div>
</template>

// 融资融券表格组件：展示个股每日融资/融券余额与买卖明细
<script setup>
import { ref, onMounted } from 'vue'
import { getMarginData } from '@/api/quant'

// 接收股票代码属性
const props = defineProps({ code: String })
// 融资融券记录列表
const items = ref([])

// 挂载时拉取两融数据，失败则置空
onMounted(async () => {
  try {
    const res = await getMarginData(props.code)
    items.value = res.data || []
  } catch (e) { items.value = [] }
})
</script>

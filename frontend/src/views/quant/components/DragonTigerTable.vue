<template>
  <div v-if="items.length" class="overflow-x-auto max-h-60">
    <table class="web3-table text-xs">
      <thead>
        <tr><th>日期</th><th>上榜原因</th><th>买入(万)</th><th>卖出(万)</th><th>净额(万)</th></tr>
      </thead>
      <tbody>
        <tr v-for="d in items" :key="d.date">
          <td class="matrix-text">{{ d.date }}</td>
          <td class="text-[#ece8ff]/70">{{ d.reason }}</td>
          <td class="text-red-400">{{ d.buy }}</td>
          <td class="text-green-400">{{ d.sell }}</td>
          <td :class="(d.net || 0) >= 0 ? 'text-red-400' : 'text-green-400'">{{ (d.net || 0) >= 0 ? '+' : '' }}{{ d.net }}</td>
        </tr>
      </tbody>
    </table>
  </div>
  <div v-else class="text-xs text-[#7d8fc4] py-4">暂无龙虎榜数据</div>
</template>

// 龙虎榜数据表格组件：展示个股最近上榜记录（日期/原因/买卖净额）
<script setup>
import { ref, onMounted } from 'vue'
import { getDragonTiger } from '@/api/quant'

// 接收股票代码属性
const props = defineProps({ code: String })
// 上榜记录列表
const items = ref([])

// 挂载时拉取最近 30 天的龙虎榜数据，失败则置空
onMounted(async () => {
  try {
    const res = await getDragonTiger(props.code, 30)
    items.value = res.data || []
  } catch (e) { items.value = [] }
})
</script>

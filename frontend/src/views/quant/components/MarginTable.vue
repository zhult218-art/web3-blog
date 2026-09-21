<template>
  <div v-if="items.length" class="space-y-3">
    <!-- 最新两融概览 -->
    <div class="grid grid-cols-3 gap-2">
      <div class="p-2.5 rounded-lg bg-[#151538] text-center">
        <div class="text-[9px] text-[#7d8fc4]">融资余额({{ latest.date }})</div>
        <div class="text-sm font-bold text-red-400 num">{{ fmtYi(latest.rzye) }}亿</div>
      </div>
      <div class="p-2.5 rounded-lg bg-[#151538] text-center">
        <div class="text-[9px] text-[#7d8fc4]">融券余额</div>
        <div class="text-sm font-bold text-green-400 num">{{ fmtWan(latest.rqye) }}万</div>
      </div>
      <div class="p-2.5 rounded-lg bg-[#151538] text-center">
        <div class="text-[9px] text-[#7d8fc4]">两融余额</div>
        <div class="text-sm font-bold text-cyan-300 num">{{ fmtYi(latest.rzrqye) }}亿</div>
      </div>
    </div>
    <div class="overflow-x-auto max-h-72">
      <table class="web3-table text-xs">
        <thead>
          <tr><th>日期</th><th>融资余额(亿)</th><th>融资买入(万)</th><th>融资偿还(万)</th><th>融资净买入(万)</th><th>融券余额(万)</th></tr>
        </thead>
        <tbody>
          <tr v-for="(d, i) in items" :key="d.date">
            <td class="matrix-text">{{ d.date }}</td>
            <td class="text-[#ece8ff]/70">{{ fmtYi(d.rzye) }}</td>
            <td class="text-[#ece8ff]/70">{{ fmtWan(d.rzmre) }}</td>
            <td class="text-[#ece8ff]/70">{{ fmtWan(d.rzche) }}</td>
            <td :class="netBuy(d) >= 0 ? 'text-red-400' : 'text-green-400'">
              {{ netBuy(d) >= 0 ? '+' : '' }}{{ fmtWan(netBuy(d)) }}
            </td>
            <td class="text-[#ece8ff]/70">{{ fmtWan(d.rqye) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="text-[9px] text-[#7d8fc4]">数据来源：东方财富（日级融资融券明细，最近 30 个交易日）</div>
  </div>
  <div v-else class="text-xs text-[#7d8fc4] py-4">暂无融资融券数据</div>
</template>

// 融资融券表格组件：展示个股每日融资/融券余额与买卖明细（东财 RPTA_WEB_RZRQ_GGMX，单位：元）
<script setup>
import { ref, computed, onMounted } from 'vue'
import { getPyStockMargin } from '@/api/quant'

// 接收股票代码属性（sh600519 / sz000001 等带前缀格式）
const props = defineProps({ code: String })
// 融资融券记录列表
const items = ref([])

// 最新一条记录（用于概览卡片）
const latest = computed(() => items.value[0] || {})

// 元 → 亿（保留 2 位）
const fmtYi = v => (v === null || v === undefined || isNaN(Number(v))) ? '--' : (Number(v) / 1e8).toFixed(2)
// 元 → 万（保留 1 位）
const fmtWan = v => (v === null || v === undefined || isNaN(Number(v))) ? '--' : (Number(v) / 1e4).toFixed(1)
// 融资净买入 = 融资买入额 - 融资偿还额
const netBuy = d => (Number(d.rzmre) || 0) - (Number(d.rzche) || 0)

// 挂载时拉取两融数据（pyquant：Supabase 优先 + 实时兜底），失败则置空
onMounted(async () => {
  try {
    const res = await getPyStockMargin(props.code)
    items.value = res.data || []
  } catch (e) { items.value = [] }
})
</script>

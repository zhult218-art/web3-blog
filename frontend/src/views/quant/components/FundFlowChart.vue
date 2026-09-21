<template>
  <div v-if="rows.length" class="space-y-3">
    <!-- 近 5 日 / 近 20 日主力净流入汇总 -->
    <div class="grid grid-cols-2 gap-2">
      <div class="p-2.5 rounded-lg bg-[#151538] text-center">
        <div class="text-[9px] text-[#7d8fc4]">近5日主力净流入</div>
        <div class="text-sm font-bold num" :class="sum(5) >= 0 ? 'text-red-400' : 'text-green-400'">
          {{ sum(5) >= 0 ? '+' : '' }}{{ fmtYi(sum(5)) }}亿
        </div>
      </div>
      <div class="p-2.5 rounded-lg bg-[#151538] text-center">
        <div class="text-[9px] text-[#7d8fc4]">近20日主力净流入</div>
        <div class="text-sm font-bold num" :class="sum(20) >= 0 ? 'text-red-400' : 'text-green-400'">
          {{ sum(20) >= 0 ? '+' : '' }}{{ fmtYi(sum(20)) }}亿
        </div>
      </div>
    </div>
    <!-- 主力净流入柱状图（最近 30 个交易日） -->
    <div ref="chartRef" class="w-full h-56"></div>
    <div class="text-[9px] text-[#7d8fc4]">数据来源：东方财富 push2his（日级资金流，红=净流入 / 绿=净流出，悬停查看超大单/大单/中单/小单明细）</div>
  </div>
  <div v-else class="text-xs text-[#7d8fc4] py-4">暂无资金流向数据</div>
</template>

// 资金流向组件：个股每日主力/超大/大/中/小单净流入（echarts 柱状图 + 汇总卡片）
<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getPyStockFundFlow } from '@/api/quant'

// 接收股票代码属性（sh600519 / sz000001 等带前缀格式）
const props = defineProps({ code: String })
// 资金流向列表（120 个交易日，图上取最近 30 日）
const rows = ref([])
const chartRef = ref(null)
let chart = null

// 元 → 亿
const fmtYi = v => (Number(v) / 1e8).toFixed(2)
// 近 n 日主力净流入合计（元）
const sum = n => rows.value.slice(0, n).reduce((acc, r) => acc + (Number(r.main_net) || 0), 0)

function renderChart() {
  if (!chartRef.value || !rows.value.length) return
  if (!chart) chart = echarts.init(chartRef.value)
  const data = rows.value.slice(0, 30).reverse() // 时间正序
  const dates = data.map(d => d.date)
  const mains = data.map(d => +(Number(d.main_net) || 0).toFixed(0))
  chart.setOption({
    backgroundColor: 'transparent',
    grid: { left: 8, right: 8, top: 26, bottom: 4, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10,16,40,0.92)', borderColor: '#22d3ee55', textStyle: { color: '#e2e8f0', fontSize: 11 },
      formatter: (params) => {
        const d = data[params[0].dataIndex]
        const line = (label, v) => {
          const yi = (v / 1e8).toFixed(2)
          return `${label}：<span style="color:${v >= 0 ? '#f87171' : '#4ade80'}">${v >= 0 ? '+' : ''}${yi}亿</span>`
        }
        return `<b>${d.date}</b><br/>` +
          line('主力净流入', d.main_net) + '<br/>' +
          line('超大单', d.super_net) + '<br/>' +
          line('大单', d.large_net) + '<br/>' +
          line('中单', d.mid_net) + '<br/>' +
          line('小单', d.small_net)
      },
    },
    xAxis: { type: 'category', data: dates, axisLabel: { color: '#7d8fc4', fontSize: 9, interval: 5 }, axisLine: { lineStyle: { color: '#22d3ee33' } } },
    yAxis: {
      type: 'value', axisLabel: { color: '#7d8fc4', fontSize: 9, formatter: v => (v / 1e8).toFixed(1) + '亿' },
      splitLine: { lineStyle: { color: '#22d3ee11' } },
    },
    series: [{
      type: 'bar', data: mains.map(v => ({ value: v, itemStyle: { color: v >= 0 ? '#ef4444' : '#22c55e' } })),
      barMaxWidth: 8,
    }],
  })
}

onMounted(async () => {
  try {
    // pyquant 网关：Supabase 历史优先 + 实时兜底写穿
    const res = await getPyStockFundFlow(props.code)
    rows.value = res.data || []
  } catch (e) { rows.value = [] }
  await nextTick()
  renderChart()
})

onBeforeUnmount(() => {
  if (chart) { chart.dispose(); chart = null }
})
</script>

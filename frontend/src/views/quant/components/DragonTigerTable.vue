<template>
  <div v-if="records.length || hasSeats" class="space-y-3">
    <!-- 机构席位净买概览 -->
    <div class="grid grid-cols-3 gap-2" v-if="hasSeats">
      <div class="p-2.5 rounded-lg bg-[#151538] text-center">
        <div class="text-[9px] text-[#7d8fc4]">机构买入(万)</div>
        <div class="text-sm font-bold text-red-400 num">{{ inst.buy_amt ?? 0 }}</div>
      </div>
      <div class="p-2.5 rounded-lg bg-[#151538] text-center">
        <div class="text-[9px] text-[#7d8fc4]">机构卖出(万)</div>
        <div class="text-sm font-bold text-green-400 num">{{ inst.sell_amt ?? 0 }}</div>
      </div>
      <div class="p-2.5 rounded-lg bg-[#151538] text-center">
        <div class="text-[9px] text-[#7d8fc4]">机构净买(万)</div>
        <div class="text-sm font-bold num" :class="(inst.net_amt || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
          {{ (inst.net_amt || 0) >= 0 ? '+' : '' }}{{ inst.net_amt ?? 0 }}
        </div>
      </div>
    </div>

    <!-- 上榜记录 -->
    <div v-if="records.length" class="overflow-x-auto max-h-40">
      <table class="web3-table text-xs">
        <thead>
          <tr><th>日期</th><th>上榜原因</th><th>龙虎榜净买(万)</th><th>市场占比(%)</th></tr>
        </thead>
        <tbody>
          <tr v-for="(d, i) in records" :key="d.date + d.reason + i">
            <td class="matrix-text">{{ d.date }}</td>
            <td class="text-[#ece8ff]/70">{{ d.reason }}</td>
            <td :class="(d.net_buy || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
              {{ (d.net_buy || 0) >= 0 ? '+' : '' }}{{ d.net_buy }}
            </td>
            <td class="text-[#ece8ff]/70">{{ d.turnover ?? '--' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 最近一次上榜：买卖席位 TOP5 -->
    <div v-if="hasSeats" class="grid grid-cols-2 gap-2">
      <div>
        <div class="text-[10px] text-cyan-300 font-bold mb-1">买入席位 TOP5（{{ seatsDate }}）</div>
        <div v-for="(s, i) in seats.buy" :key="'b' + i" class="flex justify-between text-[10px] py-1 px-2 rounded bg-[#151538] mb-1">
          <span class="text-[#ece8ff]/70 truncate mr-2">{{ s.name }}</span>
          <span class="text-red-400 shrink-0 num">{{ s.buy_amt }}</span>
        </div>
      </div>
      <div>
        <div class="text-[10px] text-cyan-300 font-bold mb-1">卖出席位 TOP5（{{ seatsDate }}）</div>
        <div v-for="(s, i) in seats.sell" :key="'s' + i" class="flex justify-between text-[10px] py-1 px-2 rounded bg-[#151538] mb-1">
          <span class="text-[#ece8ff]/70 truncate mr-2">{{ s.name }}</span>
          <span class="text-green-400 shrink-0 num">{{ s.sell_amt }}</span>
        </div>
      </div>
    </div>
    <div class="text-[9px] text-[#7d8fc4]">数据来源：东方财富龙虎榜（近 30 日上榜记录与最新一次席位明细，单位：万元）</div>
  </div>
  <div v-else class="text-xs text-[#7d8fc4] py-4">近 30 日暂无龙虎榜上榜记录</div>
</template>

// 龙虎榜组件：上榜记录 + 最新买卖席位 TOP5 + 机构净买（东财数据中心）
<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDragonTiger } from '@/api/quant'

// 接收股票代码属性
const props = defineProps({ code: String })
const records = ref([])
const seats = ref({ buy: [], sell: [] })
const inst = ref({ buy_amt: 0, sell_amt: 0, net_amt: 0 })

// 是否有席位明细
const hasSeats = computed(() => !!(seats.value.buy?.length || seats.value.sell?.length))
// 席位明细对应的上榜日期
const seatsDate = computed(() => records.value[0]?.date || '')

// 挂载时拉取近 30 天龙虎榜数据
onMounted(async () => {
  try {
    const res = await getDragonTiger(props.code, 30)
    const payload = res.data || {}
    records.value = Array.isArray(payload) ? payload : (payload.records || [])
    if (payload.seats) seats.value = payload.seats
    if (payload.institution) inst.value = payload.institution
  } catch (e) { records.value = [] }
})
</script>

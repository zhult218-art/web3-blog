<template>
  <div>
    <!-- 组合策略：多策略选股合并 -->
    <div class="scroll-card p-6 mb-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-sm font-bold text-amber-100 flex items-center gap-2"><span>🎯</span> 组合策略</h3>
        <div class="flex items-center gap-3">
          <span class="text-[10px] text-amber-600/50">{{ strategyPortfolio.length }} 个策略 · 选中 {{ selectedStrategyIds.length }} · 合并 {{ combinedStocks.length }} 只</span>
          <button v-if="strategyPortfolio.length" class="text-[10px] px-2 py-1 rounded-lg border border-red-500/20 text-red-400/70 hover:text-red-300 transition" @click="emit('clear-strategies')">清空</button>
        </div>
      </div>

      <div v-if="strategyPortfolio.length" class="space-y-2 mb-4">
        <label v-for="sp in strategyPortfolio" :key="sp.id"
          class="flex items-center gap-3 p-3 rounded-xl bg-[#1a1208] border border-amber-800/15 cursor-pointer hover:border-amber-500/30 transition">
          <input type="checkbox" :value="sp.id" v-model="selectedIds"
            class="w-4 h-4 accent-cyan-400 cursor-pointer" />
          <div class="flex-1">
            <span class="text-xs font-medium text-amber-100">{{ sp.strategy_name }}</span>
            <span class="text-[10px] text-amber-600/50 ml-2">{{ sp.items.length }} 只 · 存于 {{ sp.saved_at }}</span>
          </div>
          <button class="text-[10px] text-red-500/50 hover:text-red-400 transition" @click.stop="emit('remove-strategy', sp.id)">删除</button>
        </label>
      </div>
      <div v-else class="py-6 text-center">
        <p class="text-xs text-amber-700/50 mb-2">暂无组合策略</p>
        <p class="text-[10px] text-amber-700/40">在「策略管理」中执行策略选股后，点击「📥 存入组合策略」即可在此查看</p>
      </div>

      <!-- 合并后的股票列表 -->
      <div v-if="combinedStocks.length" class="rounded-xl bg-[#0b1422] border border-cyan-500/15 overflow-hidden">
        <div class="px-4 py-2.5 border-b border-cyan-500/10 flex items-center gap-2">
          <span class="text-xs font-bold text-cyan-300">合并选股结果</span>
          <span class="text-[10px] text-cyan-500/50">{{ combinedStocks.length }} 只标的 · 来自 {{ selectedStrategyIds.length }} 个策略</span>
        </div>
        <div class="overflow-x-auto max-h-[400px] overflow-y-auto">
          <table class="w-full text-xs">
            <thead class="sticky top-0 bg-[#0b1422]">
              <tr class="text-left text-[10px] text-cyan-500/60 border-b border-cyan-500/10">
                <th class="px-3 py-2 font-medium">代码</th>
                <th class="px-3 py-2 font-medium">名称</th>
                <th class="px-3 py-2 font-medium text-right">现价</th>
                <th class="px-3 py-2 font-medium text-right">涨跌%</th>
                <th class="px-3 py-2 font-medium">入选策略</th>
                <th class="px-3 py-2 font-medium">评分</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in combinedStocks" :key="s.code"
                class="border-b border-cyan-900/10 last:border-b-0 hover:bg-cyan-500/5 transition">
                <td class="px-3 py-2 font-mono">{{ s.code }}</td>
                <td class="px-3 py-2 text-amber-100/90">{{ s.name }}</td>
                <td class="px-3 py-2 text-right font-mono">{{ s.price != null ? Number(s.price).toFixed(2) : '--' }}</td>
                <td class="px-3 py-2 text-right font-mono" :class="Number(s.pct) >= 0 ? 'text-red-400' : 'text-green-400'">
                  {{ s.pct != null ? (Number(s.pct) > 0 ? '+' : '') + Number(s.pct).toFixed(2) + '%' : '--' }}
                </td>
                <td class="px-3 py-2">
                  <div class="flex flex-wrap gap-1">
                    <span v-for="sn in s.strategy_names" :key="sn"
                      class="text-[9px] px-1.5 py-0.5 rounded bg-purple-500/10 text-purple-300 border border-purple-400/15 whitespace-nowrap">{{ sn }}</span>
                  </div>
                </td>
                <td class="px-3 py-2 text-right font-mono">
                  <span v-if="s.score" class="text-[10px] px-1.5 py-0.5 rounded-full bg-purple-500/15 text-purple-300 border border-purple-400/25">{{ s.score }}</span>
                  <span v-else class="text-amber-700/30">--</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 持仓概览 -->
    <div class="scroll-card p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-sm font-bold text-amber-100 flex items-center gap-2"><span>📊</span> 我的持仓</h3>
        <div class="flex items-center gap-3">
          <span v-if="holdingsIsAdmin" class="text-[10px] px-2 py-0.5 rounded-full bg-red-500/10 text-red-400/70 border border-red-500/20">管理员视角 · 可见全部用户</span>
          <button class="web3-btn text-xs !px-3 !py-1.5" @click="showForm = !showForm">
            {{ showForm ? '收起' : '+ 添加持仓' }}
          </button>
          <button class="text-[10px] px-2 py-1 rounded-lg border border-amber-800/20 text-amber-500/70 hover:text-amber-100 transition" @click="emit('load-holdings')">刷新</button>
        </div>
      </div>

      <!-- 添加/编辑持仓表单 -->
      <div v-if="showForm" class="mb-4 p-4 rounded-xl bg-[#1a1208] border border-amber-800/15">
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div>
            <label class="text-[10px] text-amber-600/60 mb-1 block">股票代码 *</label>
            <input v-model="holdingForm.stock_code" class="web3-input text-xs font-mono" placeholder="如 600519" />
          </div>
          <div>
            <label class="text-[10px] text-amber-600/60 mb-1 block">股票名称</label>
            <input v-model="holdingForm.stock_name" class="web3-input text-xs" placeholder="如 贵州茅台" />
          </div>
          <div>
            <label class="text-[10px] text-amber-600/60 mb-1 block">持仓股数</label>
            <input v-model.number="holdingForm.shares" type="number" min="0" class="web3-input text-xs font-mono" placeholder="100" />
          </div>
          <div>
            <label class="text-[10px] text-amber-600/60 mb-1 block">成本价</label>
            <input v-model.number="holdingForm.cost_price" type="number" step="0.001" min="0" class="web3-input text-xs font-mono" placeholder="1688.00" />
          </div>
          <div>
            <label class="text-[10px] text-amber-600/60 mb-1 block">买入日期</label>
            <input v-model="holdingForm.buy_date" type="date" class="web3-input text-xs font-mono" />
          </div>
          <div class="col-span-2">
            <label class="text-[10px] text-amber-600/60 mb-1 block">备注</label>
            <input v-model="holdingForm.notes" class="web3-input text-xs" placeholder="可选" />
          </div>
        </div>
        <div class="flex gap-2 mt-3">
          <button class="web3-btn text-xs !px-4 !py-2" @click="emit('submit-holding')">保存</button>
          <button class="text-xs px-3 py-2 rounded-lg border border-amber-800/20 text-amber-500/70 hover:text-amber-100 transition" @click="showForm = false">取消</button>
        </div>
      </div>

      <!-- 持仓列表 -->
      <div v-if="loadingHoldings" class="flex items-center justify-center gap-2 py-6">
        <span class="h-4 w-4 rounded-full border-2 border-amber-800/25 border-t-web3-accent animate-spin"></span>
        <span class="text-xs text-amber-700/50">加载持仓...</span>
      </div>
      <div v-else-if="myHoldings.length" class="overflow-x-auto rounded-xl bg-[#1a1208] border border-amber-800/15">
        <table class="w-full text-xs">
          <thead>
            <tr class="text-left text-[10px] text-amber-600/60 border-b border-amber-800/15">
              <th v-if="holdingsIsAdmin" class="px-3 py-2.5 font-medium">用户</th>
              <th class="px-3 py-2.5 font-medium">代码</th>
              <th class="px-3 py-2.5 font-medium">名称</th>
              <th class="px-3 py-2.5 font-medium text-right">股数</th>
              <th class="px-3 py-2.5 font-medium text-right">成本</th>
              <th class="px-3 py-2.5 font-medium">买入日期</th>
              <th class="px-3 py-2.5 font-medium">备注</th>
              <th class="px-3 py-2.5 font-medium text-center">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="h in myHoldings" :key="h.id" class="border-b border-amber-900/10 last:border-b-0 hover:bg-amber-500/5 transition">
              <td v-if="holdingsIsAdmin" class="px-3 py-2 text-amber-500/70">{{ h.username || h.user_id }}</td>
              <td class="px-3 py-2 font-mono">{{ h.stock_code }}</td>
              <td class="px-3 py-2 text-amber-100/90">{{ h.stock_name || '--' }}</td>
              <td class="px-3 py-2 text-right font-mono">{{ h.shares }}</td>
              <td class="px-3 py-2 text-right font-mono">{{ Number(h.cost_price).toFixed(3) }}</td>
              <td class="px-3 py-2 font-mono text-amber-200/60">{{ h.buy_date || '--' }}</td>
              <td class="px-3 py-2 text-amber-200/70 max-w-[160px] truncate">{{ h.notes || '--' }}</td>
              <td class="px-3 py-2 text-center">
                <button class="text-[10px] text-amber-500/70 hover:text-amber-300 transition mr-2" @click="emit('edit-holding', h)">编辑</button>
                <button class="text-[10px] text-red-500/60 hover:text-red-400 transition" @click="emit('remove-holding', h.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="py-8 text-center">
        <p class="text-xs text-amber-700/50 mb-2">暂无持仓数据</p>
        <p class="text-[10px] text-amber-700/40">点击「添加持仓」录入你的股票持仓</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// 组合管理 Tab —— 批 3 拆分第 4 块
// 状态仍由父级 views/quant/index.vue 持有（myHoldings 等在同页其它位置也被读取），
// 本组件只负责渲染 + 通过 emit 上抛动作，避免父页面继续膨胀。
// 样式依赖父页面根节点的 .quant-terminal 命名空间（非 scoped 全局主题）。
import { computed } from 'vue'

const props = defineProps({
  strategyPortfolio: { type: Array, default: () => [] },
  selectedStrategyIds: { type: Array, default: () => [] },
  combinedStocks: { type: Array, default: () => [] },
  holdingsIsAdmin: { type: Boolean, default: false },
  loadingHoldings: { type: Boolean, default: false },
  myHoldings: { type: Array, default: () => [] },
  holdingForm: { type: Object, default: () => ({}) },
  showHoldingForm: { type: Boolean, default: false },
})

const emit = defineEmits([
  'update:showHoldingForm',
  'update:selectedStrategyIds',
  'clear-strategies',
  'remove-strategy',
  'load-holdings',
  'submit-holding',
  'edit-holding',
  'remove-holding',
])

// 表单展开态用可写 computed 桥接 v-model:show-holding-form，避免子组件直接改 prop
// 勾选列表同理：数组型 v-model 必须走 computed 桥接，否则编译器直接报错
const selectedIds = computed({
  get: () => props.selectedStrategyIds,
  set: (v) => emit('update:selectedStrategyIds', v),
})

const showForm = computed({
  get: () => props.showHoldingForm,
  set: (v) => emit('update:showHoldingForm', v),
})
</script>

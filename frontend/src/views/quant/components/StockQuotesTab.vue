<template>
  <div>
    <!-- Market Breadth Stats -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-5" v-if="stockList.length">
      <div class="scroll-card p-3.5 flex items-center justify-between">
        <div>
          <div class="text-lg font-black matrix-text text-red-400">{{ breadth.up }}</div>
          <div class="text-[10px] text-amber-600/70 uppercase tracking-wider font-serif">上涨</div>
        </div>
        <div class="text-lg">📈</div>
      </div>
      <div class="scroll-card p-3.5 flex items-center justify-between">
        <div>
          <div class="text-lg font-black matrix-text text-green-400">{{ breadth.down }}</div>
          <div class="text-[10px] text-amber-600/70 uppercase tracking-wider font-serif">下跌</div>
        </div>
        <div class="text-lg">📉</div>
      </div>
      <div class="scroll-card p-3.5 flex items-center justify-between">
        <div>
          <div class="text-lg font-black matrix-text text-amber-100">{{ breadth.total }}</div>
          <div class="text-[10px] text-amber-600/70 uppercase tracking-wider font-serif">样本数</div>
        </div>
        <div class="text-lg">🗂</div>
      </div>
      <div class="scroll-card p-3.5 flex items-center justify-between">
        <div>
          <div class="text-lg font-black text-amber-400 scroll-title">{{ lastUpdate || '--' }}</div>
          <div class="text-[10px] text-amber-600/70 uppercase tracking-wider font-serif">更新时间</div>
        </div>
        <div class="text-lg">🕐</div>
      </div>
    </div>

    <!-- Search + Stock Table -->
    <div class="scroll-card overflow-hidden mb-5">
      <div class="p-4 border-b border-amber-800/20 flex flex-wrap items-center gap-3">
        <h3 class="text-sm font-bold text-amber-200 font-serif">A股实时行情 <span class="text-[10px] text-amber-600/70 ml-1">共 {{ stockTotal }} 只</span></h3>
        <input v-model="searchInput" placeholder="输入代码/名称搜索 (如 600519)"
          class="text-xs bg-[#22180c] border border-amber-800/20 rounded-lg px-3 py-1.5 text-amber-100 placeholder-amber-700/50 focus:outline-none focus:border-amber-400/30 w-full sm:w-48 sm:ml-auto" @keyup.enter="submitSearch" />
        <button @click="submitSearch" class="text-[10px] px-3 py-1.5 rounded-lg bg-[#332314] text-amber-400 border border-amber-400/20 hover:bg-amber-500/20 transition">
          查询
        </button>
      </div>
      <div class="overflow-x-auto">
        <table class="web3-table min-w-[980px]">
          <thead>
            <tr>
              <th>走势</th>
              <th @click="toggleStockSort('code')" class="cursor-pointer select-none hover:text-amber-300 transition">
                代码
                <span v-if="stockSortKey === 'code'" class="ml-0.5 text-[9px]">{{ stockSortDir === 'asc' ? '▲' : '▼' }}</span>
                <span v-else class="ml-0.5 text-amber-600/30 text-[9px]">↕</span>
              </th>
              <th @click="toggleStockSort('name')" class="cursor-pointer select-none hover:text-amber-300 transition">
                名称
                <span v-if="stockSortKey === 'name'" class="ml-0.5 text-[9px]">{{ stockSortDir === 'asc' ? '▲' : '▼' }}</span>
                <span v-else class="ml-0.5 text-amber-600/30 text-[9px]">↕</span>
              </th>
              <th @click="toggleStockSort('price')" class="cursor-pointer select-none hover:text-amber-300 transition">
                最新价
                <span v-if="stockSortKey === 'price'" class="ml-0.5 text-[9px]">{{ stockSortDir === 'asc' ? '▲' : '▼' }}</span>
                <span v-else class="ml-0.5 text-amber-600/30 text-[9px]">↕</span>
              </th>
              <th @click="toggleStockSort('change_pct')" class="cursor-pointer select-none hover:text-amber-300 transition">
                涨跌幅
                <span v-if="stockSortKey === 'change_pct'" class="ml-0.5 text-[9px]">{{ stockSortDir === 'asc' ? '▲' : '▼' }}</span>
                <span v-else class="ml-0.5 text-amber-600/30 text-[9px]">↕</span>
              </th>
              <th @click="toggleStockSort('change_amt')" class="cursor-pointer select-none hover:text-amber-300 transition">
                涨跌额
                <span v-if="stockSortKey === 'change_amt'" class="ml-0.5 text-[9px]">{{ stockSortDir === 'asc' ? '▲' : '▼' }}</span>
                <span v-else class="ml-0.5 text-amber-600/30 text-[9px]">↕</span>
              </th>
              <th @click="toggleStockSort('amount_wan')" class="cursor-pointer select-none hover:text-amber-300 transition">
                成交量(万)
                <span v-if="stockSortKey === 'amount_wan'" class="ml-0.5 text-[9px]">{{ stockSortDir === 'asc' ? '▲' : '▼' }}</span>
                <span v-else class="ml-0.5 text-amber-600/30 text-[9px]">↕</span>
              </th>
              <th @click="toggleStockSort('turnover')" class="cursor-pointer select-none hover:text-amber-300 transition">
                成交额(亿)
                <span v-if="stockSortKey === 'turnover'" class="ml-0.5 text-[9px]">{{ stockSortDir === 'asc' ? '▲' : '▼' }}</span>
                <span v-else class="ml-0.5 text-amber-600/30 text-[9px]">↕</span>
              </th>
              <th @click="toggleStockSort('amplitude_pct')" class="cursor-pointer select-none hover:text-amber-300 transition">
                振幅
                <span v-if="stockSortKey === 'amplitude_pct'" class="ml-0.5 text-[9px]">{{ stockSortDir === 'asc' ? '▲' : '▼' }}</span>
                <span v-else class="ml-0.5 text-amber-600/30 text-[9px]">↕</span>
              </th>
              <th @click="toggleStockSort('turnover_pct')" class="cursor-pointer select-none hover:text-amber-300 transition">
                手率
                <span v-if="stockSortKey === 'turnover_pct'" class="ml-0.5 text-[9px]">{{ stockSortDir === 'asc' ? '▲' : '▼' }}</span>
                <span v-else class="ml-0.5 text-amber-600/30 text-[9px]">↕</span>
              </th>
              <th @click="toggleStockSort('pe_ttm')" class="cursor-pointer select-none hover:text-amber-300 transition">
                市盈率
                <span v-if="stockSortKey === 'pe_ttm'" class="ml-0.5 text-[9px]">{{ stockSortDir === 'asc' ? '▲' : '▼' }}</span>
                <span v-else class="ml-0.5 text-amber-600/30 text-[9px]">↕</span>
              </th>
              <th>数据源</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in pagedStocks" :key="s.code || s.symbol" class="cursor-pointer hover:bg-[#22180c] transition"
              @click="openStock(s)">
              <td class="py-1">
                <svg v-if="sparkCache[s.symbol || s.code]" width="56" height="22" viewBox="0 0 56 22">
                  <polyline :points="sparkPoints(s.symbol || s.code)"
                    fill="none" stroke="#22d3ee" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                <span v-else class="text-amber-700/40 text-[10px]">--</span>
              </td>
              <td class="matrix-text text-xs">{{ s.code || s.symbol }}</td>
              <td class="text-xs"><a href="javascript:void(0)" @click.stop="openStock(s); scrollToStockDetail()" class="text-cyan-400 hover:text-cyan-300 hover:underline cursor-pointer">{{ s.name }}</a></td>
              <td class="matrix-text text-sm font-bold">{{ s.price?.toFixed(2) || s.close?.toFixed(2) || '--' }}</td>
              <td>
                <div class="flex items-center gap-1.5">
                  <span :class="(s.change_pct || s.changePercent || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
                    {{ (s.change_pct || s.changePercent || 0) >= 0 ? '+' : '' }}{{ (s.change_pct || s.changePercent || 0)?.toFixed(2) || '0.00' }}%
                  </span>
                  <div class="w-10 h-1 rounded-full bg-amber-800/20 overflow-hidden">
                    <div class="h-full rounded-full" :class="(s.change_pct || 0) >= 0 ? 'bg-red-400' : 'bg-green-400'"
                      :style="{ width: Math.min(Math.abs(s.change_pct || 0) / 3 * 100, 100) + '%', marginLeft: (s.change_pct || 0) >= 0 ? 'auto' : '0' }"></div>
                  </div>
                </div>
              </td>
              <td :class="(s.change_amt || s.change || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
                {{ (s.change_amt || s.change || 0) >= 0 ? '+' : '' }}{{ (s.change_amt || s.change || 0)?.toFixed(2) || '0.00' }}
              </td>
              <td class="text-amber-100/60 matrix-text text-xs">{{ formatVol(s.amount_wan || s.volume) }}</td>
              <td class="text-amber-100/60 matrix-text text-xs">{{ formatMoney(s.amount || s.turnover) }}</td>
              <td class="text-amber-100/60">{{ s.amplitude_pct || s.amplitude || '--' }}%</td>
              <td class="text-amber-100/60">{{ s.turnover_pct || s.turnoverRate || '--' }}%</td>
              <td class="text-amber-100/60">{{ s.pe_ttm || '--' }}</td>
              <td>
                <span v-if="s.source" class="text-[10px] px-1.5 py-0.5 rounded bg-[#261b0e] border border-amber-800/20 text-amber-400/80">{{ s.source }}</span>
                <span v-else class="text-amber-700/50 text-[10px]">--</span>
              </td>
              <td><button @click.stop="openStock(s)" class="text-[10px] px-2 py-1 rounded bg-purple-500/10 text-purple-400 border border-purple-400/20 hover:bg-purple-500/20">详情</button></td>
            </tr>
            <tr v-if="!stockList.length">
              <td colspan="13" class="text-center text-amber-700/50 py-8">暂无数据，点击查询获取行情</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="stockTotal > stockPageSize" class="mb-5">
      <Pagination v-model:page="stockPage" :page-size="stockPageSize" :total="stockTotal" />
    </div>

    <!-- Stock Detail Panel -->
    <div v-if="selectedStock" id="stock-detail-panel" class="scroll-card overflow-hidden">
      <div class="p-4 border-b border-amber-800/20 flex items-center justify-between">
        <div>
          <h3 class="text-sm font-bold text-amber-100">{{ selectedStock.name }} ({{ selectedStock.code || selectedStock.symbol }})</h3>
          <div class="flex items-center gap-3 mt-1">
            <span class="text-lg font-black matrix-text" :class="(selectedStock.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
              {{ selectedStock.price?.toFixed(2) || '--' }}
            </span>
            <span :class="['text-xs font-mono', (selectedStock.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400']">
              {{ (selectedStock.change_pct || 0) >= 0 ? '+' : '' }}{{ (selectedStock.change_pct || 0)?.toFixed(2) }}%
            </span>
          </div>
        </div>
        <button @click="selectedStock = null" class="text-amber-600/60 hover:text-amber-100 text-xs">关闭</button>
      </div>

      <!-- K-line Chart -->
      <div class="p-4 border-b border-amber-800/20">
        <div class="flex items-center gap-2 mb-3 flex-wrap">
          <button v-for="p in periods" :key="p.key"
            @click="fetchKline(selectedStock.symbol || selectedStock.code, p.key)"
            :class="['text-[10px] px-2.5 py-1 rounded-lg transition',
              klinePeriod === p.key ? 'bg-amber-500/15 text-amber-400 border border-amber-400/20' : 'text-amber-600/60 hover:text-amber-200']">
            {{ p.label }}
          </button>
          <span class="ml-auto flex items-center gap-2">
            <button @click="startBackfill"
              :class="['text-[10px] px-2.5 py-1 rounded-lg transition border',
                backfillRunning ? 'border-emerald-600/40 text-emerald-400 bg-emerald-500/10' : 'border-emerald-700/40 text-emerald-600/80 hover:bg-emerald-500/10 hover:text-emerald-400']">
              {{ backfillRunning ? '补全中 ' + backfillDone + '/' + backfillTotal : '补全全池历史' }}
            </button>
            <span v-if="klineSource" class="text-[10px] px-2.5 py-1 rounded-lg bg-[#261b0e] text-amber-500/70 border border-amber-800/20">
              数据源: <span class="text-amber-400">{{ klineSource }}</span>
            </span>
          </span>
        </div>
        <div ref="klineChartRef" class="w-full" style="height: 350px;"></div>
      </div>

      <!-- Stock Info Grid -->
      <div class="p-4 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
        <div v-for="info in stockInfoItems" :key="info.label" class="bg-[#1a1208] rounded-lg p-3 min-w-0">
          <div class="text-[10px] text-amber-600/60">{{ info.label }}</div>
          <div class="text-xs font-mono text-amber-100 mt-1 truncate" :title="info.value">{{ info.value }}</div>
        </div>
      </div>

      <!-- Tabs: 资金流向 / 龙虎榜 / 研报 -->
      <div class="p-4">
        <div class="flex gap-1 mb-3">
          <button @click="stockDetailTab = 'fund'" :class="['text-[10px] px-3 py-1.5 rounded-lg', stockDetailTab === 'fund' ? 'bg-[#332314] text-amber-400' : 'text-amber-600/60']">资金流向</button>
          <button @click="stockDetailTab = 'dragon'" :class="['text-[10px] px-3 py-1.5 rounded-lg', stockDetailTab === 'dragon' ? 'bg-[#332314] text-amber-400' : 'text-amber-600/60']">龙虎榜</button>
          <button @click="stockDetailTab = 'report'" :class="['text-[10px] px-3 py-1.5 rounded-lg', stockDetailTab === 'report' ? 'bg-[#332314] text-amber-400' : 'text-amber-600/60']">研报</button>
          <button @click="stockDetailTab = 'margin'" :class="['text-[10px] px-3 py-1.5 rounded-lg', stockDetailTab === 'margin' ? 'bg-[#332314] text-amber-400' : 'text-amber-600/60']">融资融券</button>
        </div>

        <div v-if="stockDetailTab === 'fund'">
          <FundFlowChart :code="selectedStock.code || selectedStock.symbol" />
        </div>
        <div v-if="stockDetailTab === 'dragon'">
          <DragonTigerTable :code="selectedStock.code || selectedStock.symbol" />
        </div>
        <div v-if="stockDetailTab === 'report'">
          <ReportsTable :code="selectedStock.code || selectedStock.symbol" />
        </div>
        <div v-if="stockDetailTab === 'margin'">
          <MarginTable :code="selectedStock.code || selectedStock.symbol" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// A股行情 Tab —— 批 3 拆分第 2 块（原 index.vue L44-267）
// 布局/交互自包含；页面级共享状态（stockList/stockTotal/lastUpdate）
// 经 props 传入，「选中股票跳转」与「搜索行情」等全局动作 emit 上抛。
// 样式依赖父页面根节点 .quant-terminal 命名空间（非 scoped 全局主题）。
// ============================================================
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import echarts from '@/utils/echarts'
import Pagination from '@/components/common/Pagination.vue'
import FundFlowChart from './FundFlowChart.vue'
import DragonTigerTable from './DragonTigerTable.vue'
import ReportsTable from './ReportsTable.vue'
import MarginTable from './MarginTable.vue'
import { getPyKline, getPyStockInfo, getPySparklines, startPyBackfill, getPyBackfillStatus } from '@/api/quant'
import { formatVol, formatMoney, marketSymbol } from './quoteUtils'

const props = defineProps({
  stockList: { type: Array, default: () => [] },
  stockTotal: { type: Number, default: 0 },
  lastUpdate: { type: String, default: '' },
})
const emit = defineEmits(['select-stock', 'search'])

// ===== 本地状态 =====
const searchInput = ref('')
const stockPage = ref(1)
const stockPageSize = ref(30)
const sparkCache = ref({})
const selectedStock = ref(null)
// 计算行情涨跌家数统计
const breadth = computed(() => {
  const total = props.stockList.length
  const up = props.stockList.filter(s => (s.change_pct || s.changePercent || 0) > 0).length
  return { total, up, down: total - up }
})
const stockSortKey = ref('')
const stockSortDir = ref('asc')
// 行情列表列头排序：同列在升/降序间切换，名称默认升序、其余默认降序
function toggleStockSort(key) {
  if (stockSortKey.value === key) { stockSortDir.value = stockSortDir.value === 'asc' ? 'desc' : 'asc'; return }
  stockSortKey.value = key
  stockSortDir.value = key === 'name' ? 'asc' : 'desc'
}
// 对当前股票列表做升降序（支持 code/name/change_percent/price/amount/turnover）
const stockSortFn = computed(() => {
  const k = stockSortKey.value
  const dir = stockSortDir.value === 'asc' ? 1 : -1
  // 字段别名映射：排序键 → 股票对象上可能的字段名
  const aliases = {
    price: ['price', 'close'],
    change_pct: ['change_pct', 'changePercent', 'change_percent'],
    change_amt: ['change_amt', 'change'],
    amount_wan: ['amount_wan', 'volume'],
    turnover: ['turnover', 'amount'],
    amplitude_pct: ['amplitude_pct', 'amplitude'],
    turnover_pct: ['turnover_pct', 'turnoverRate'],
    pe_ttm: ['pe_ttm', 'pe'],
  }
  const getField = (s) => {
    if (k === 'name') return s.name || ''
    if (k === 'code') return s.code || s.symbol || ''
    const list = aliases[k] || [k]
    for (const f of list) { if (s[f] != null && s[f] !== '') return Number(s[f]) || 0 }
    return 0
  }
  return (a, b) => {
    const av = getField(a)
    const bv = getField(b)
    if (typeof av === 'string') return dir * av.localeCompare(bv, 'zh-Hans-CN')
    return dir * (Number(av) - Number(bv))
  }
})
// 名称/涨跌幅/最新价/成交额 点击列头排序后的完整列表
const sortedStocks = computed(() => {
  if (!stockSortKey.value) return props.stockList
  return props.stockList.slice().sort(stockSortFn.value)
})
// 按分页参数截取当前页行情列表（先排序后分页）
const pagedStocks = computed(() => {
  const start = (stockPage.value - 1) * stockPageSize.value
  return sortedStocks.value.slice(start, start + stockPageSize.value)
})

// K线相关（详情面板；面板由 selectedStock 控制）
const klinePeriod = ref('daily')
const klineSource = ref('')
const periods = [
  { key: 'intraday', label: '分时' },
  { key: '5d', label: '五日' },
  { key: '1m', label: '1分' },
  { key: '5m', label: '5分' },
  { key: '15m', label: '15分' },
  { key: '30m', label: '30分' },
  { key: '60m', label: '60分' },
  { key: 'daily', label: '日线' },
  { key: 'weekly', label: '周线' },
  { key: 'monthly', label: '月线' },
]
const showMA = reactive({ '5': true, '10': true, '20': true, '60': true })
let intradayTimer = null
const stockDetailTab = ref('fund')
const stockInfoItems = ref([])
const klineChartRef = ref(null)
let klineChart = null

// ===== 迷你走势 / 搜索 / 选中 =====
// 根据缓存生成迷你走势图的坐标点串
function sparkPoints(code) {
  const arr = sparkCache.value[code]
  if (!arr || arr.length < 2) return ''
  const w = 56, h = 22, pad = 2
  const min = Math.min(...arr), max = Math.max(...arr)
  const span = (max - min) || 1
  return arr.map((v, i) => {
    const x = pad + (i * (w - pad * 2)) / (arr.length - 1)
    const y = h - pad - ((v - min) / span) * (h - pad * 2)
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}

// 缓存股票近 30 日收盘价，用于迷你走势图
function cacheSpark(code, closes) {
  if (!code || !closes?.length) return
  sparkCache.value[code] = closes.slice(-240)
}

// 批量预取列表股票的迷你走势数据
async function prefetchSparklines(list) {
  const syms = list.map(s => (s.symbol || (s.code ? `sh${s.code}` : ''))).filter(Boolean)
  if (!syms.length) return
  // 仅预取首屏可见的前 150 只（3 批）：全市场 5400+ 只串行 100+ 批会长达数十分钟，
  // 挤占浏览器同源 6 连接槽，饿死 fund-flow/limit-pools 等关键看板请求（公网隧道下尤甚）。
  // 后续分页股票不显示迷你走势小图，不影响行情数据本身。
  const PREFETCH_LIMIT = 150
  const limited = syms.slice(0, PREFETCH_LIMIT)
  // 分批请求，每批最多 50 个代码，避免 URL 过长导致请求被中止
  const batchSize = 50
  for (let i = 0; i < limited.length; i += batchSize) {
    const batch = limited.slice(i, i + batchSize)
    try {
      const res = await getPySparklines(batch)
      const d = res.data || {}
      for (const [sym, closes] of Object.entries(d)) {
        if (Array.isArray(closes) && closes.length) cacheSpark(sym, closes)
      }
    } catch (e) { if (import.meta.env.DEV) console.warn('sparklines batch fail (offset ' + i + ')', e) }
  }
}

// 请求父级执行搜索（全局 stockList 由父页面统一维护）
function submitSearch() {
  const q = searchInput.value.trim()
  if (!q) return
  emit('search', q)
}

// 点击行/名称/详情按钮：请求父级跳转个股详情页
function openStock(s) { emit('select-stock', s) }

// 点击股票名称后滚动到详情面板（保留兼容；实际跳转由父级 selectStock 完成）
function scrollToStockDetail() {
  nextTick(() => {
    const el = document.getElementById('stock-detail-panel')
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

// 拉取 K 线数据并渲染蜡烛图、成交量与均线（ECharts）
async function fetchKline(code, period) {
  klinePeriod.value = period
  const sym = code.startsWith('sh') || code.startsWith('sz') || code.startsWith('bj') ? code : `sh${code}`
  try {
    const daysMap = { intraday: 1, '5d': 5, '1m': 1, '5m': 5, '15m': 5, '30m': 10, '60m': 15, daily: 10000, weekly: 4000, monthly: 1600 }
    const res = await getPyKline(sym, daysMap[period] || 120, period)
    const data = res.data
    if (!data || !data.length) return
    klineSource.value = (data[0] && data[0].source) || ''
    if (period === 'daily' && data.length > 120) cacheSpark(sym, data.map(d => Number(d.close) || 0))

    await nextTick()
    if (!klineChartRef.value) return
    if (!klineChart) klineChart = echarts.init(klineChartRef.value)
    klineChart.clear()

    const isLine = period === 'intraday' || period === '5d'
    const dates = data.map(d => d.datetime || d.date)
    const ohlc = data.map(d => [d.open, d.close, d.low, d.high])
    const volumes = data.map(d => d.volume || 0)
    const closes = data.map(d => Number(d.close) || 0)

    function ma(n) {
      return closes.map((_, i) => {
        if (i < n - 1) return null
        let s = 0
        for (let j = i - n + 1; j <= i; j++) s += closes[j]
        return +(s / n).toFixed(2)
      })
    }

    const colors = data.map(d => (d.close >= d.open) ? '#ef4444' : '#22c55e')
    const maSeries = []
    if (!isLine && closes.length >= 5) {
      const configs = [
        { n: 5, name: 'MA5', color: '#f59e0b', dash: [4, 2] },
        { n: 10, name: 'MA10', color: '#3b82f6', dash: [4, 2] },
        { n: 20, name: 'MA20', color: '#a855f7', dash: [6, 2] },
        { n: 60, name: 'MA60', color: '#ec4899', dash: [2, 3] },
      ]
      for (const c of configs) {
        if (closes.length < c.n) continue
        maSeries.push({
          name: c.name, type: 'line', data: ma(c.n), xAxisIndex: 0, yAxisIndex: 0,
          smooth: true, showSymbol: false, lineStyle: { width: 1, color: c.color, type: c.dash },
          itemStyle: { color: c.color },
        })
      }
    }

    const barSeries = {
      type: 'bar', xAxisIndex: 1, yAxisIndex: 1, data: volumes, itemStyle: { color: colors },
    }
    const series = isLine
      ? [
          {
            name: '价格', type: 'line', xAxisIndex: 0, yAxisIndex: 0, data: closes,
            smooth: true, showSymbol: false, lineStyle: { width: 1.2, color: '#f59e0b' }, itemStyle: { color: '#f59e0b' },
          },
          {
            name: '均价', type: 'line', xAxisIndex: 0, yAxisIndex: 0,
            data: data.map(d => (d.avg ? Number(d.avg) : null)),
            smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#3b82f6', type: [4, 2] }, itemStyle: { color: '#3b82f6' },
          },
          barSeries,
        ]
      : [
          {
            type: 'candlestick',
            xAxisIndex: 0, yAxisIndex: 0,
            data: ohlc,
            itemStyle: { color: '#ef4444', color0: '#22c55e', borderColor: '#ef4444', borderColor0: '#22c55e' },
          },
          barSeries,
          ...maSeries,
        ]

    klineChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis', confine: true, axisPointer: { type: 'cross' }, backgroundColor: 'rgba(8,8,18,0.92)', borderColor: 'rgba(255,255,255,0.08)', textStyle: { color: '#ccc', fontSize: 10 } },
      legend: { top: 2, right: 6, textStyle: { color: '#888', fontSize: 10 }, itemWidth: 12, itemHeight: 8, data: isLine ? ['价格', '均价'] : maSeries.map(s => s.name) },
      grid: [{ left: '8%', right: '8%', top: '16%', bottom: '20%' }, { left: '8%', right: '8%', top: '75%', bottom: '10%' }],
      xAxis: [{ type: 'category', data: dates, gridIndex: 0, axisLine: { lineStyle: { color: '#333' } }, axisLabel: { color: '#666', fontSize: 10 } }, { type: 'category', gridIndex: 1, data: dates, axisLine: { lineStyle: { color: '#333' } }, axisLabel: { show: false } }],
      yAxis: [{ type: 'value', gridIndex: 0, scale: true, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } }, axisLabel: { color: '#666', fontSize: 10 } }, { type: 'value', gridIndex: 1, scale: true, splitLine: { show: false }, axisLabel: { show: false } }],
      series,
      dataZoom: [
        { type: 'inside', xAxisIndex: [0, 1] },
        { type: 'slider', xAxisIndex: [0, 1], height: 16, bottom: 0, borderColor: 'transparent', fillerColor: 'rgba(0,255,255,0.08)', handleStyle: { color: '#0ff' }, textStyle: { color: '#666' }, startValue: isLine ? Math.max(0, dates.length - 240) : Math.max(0, dates.length - (period === 'weekly' ? 52 : period === 'monthly' ? 24 : period === 'daily' ? 250 : 60)), endValue: Math.max(0, dates.length - 1) },
      ],
    }, true)
  } catch (e) {
    if (import.meta.env.DEV) console.warn('kline fail', e)
  }
}

// 全池历史数据补全（后台任务 + 轮询进度）
const backfillRunning = ref(false)
const backfillTotal = ref(0)
const backfillDone = ref(0)
let backfillTimer = null
async function startBackfill() {
  if (backfillRunning.value) return
  try {
    await startPyBackfill()
  } catch (e) { return }
  backfillRunning.value = true
  pollBackfill()
}
async function pollBackfill() {
  try {
    const res = await getPyBackfillStatus()
    const st = res.data || {}
    backfillTotal.value = st.total || 0
    backfillDone.value = st.done || 0
    if (!st.running) { backfillRunning.value = false; return }
    backfillTimer = setTimeout(pollBackfill, 3000)
  } catch (e) { backfillRunning.value = false }
}

// 拉取股票技术指标并组装详情条目
async function fetchStockInfo(code) {
  try {
  const sym = code.startsWith('sh') || code.startsWith('sz') || code.startsWith('bj') ? code : marketSymbol(code)
    const res = await getPyStockInfo(sym).catch(() => null)
    const info = res?.data?.quote || {}
    const ind = res?.data?.indicators || {}
    const f = (v, nd = 2) => (v === undefined || v === null || isNaN(v)) ? '--' : Number(v).toFixed(nd)
    stockInfoItems.value = [
      { label: '今开', value: f(info.open) },
      { label: '昨收', value: f((info.price || 0) - (info.change || 0)) },
      { label: '最高', value: f(info.high) },
      { label: '最低', value: f(info.low) },
      { label: '振幅', value: info.amplitude != null ? f(info.amplitude) + '%' : '--' },
      { label: '换手率', value: info.turnover_rate != null ? f(info.turnover_rate) + '%' : '--' },
      { label: '市盈率(TTM)', value: f(info.pe) },
      { label: '市净率', value: f(info.pb) },
      { label: 'MA5 / MA10', value: ind.ma5 != null ? `${f(ind.ma5)} / ${f(ind.ma10)}` : '--' },
      { label: 'MA20 / MA60', value: ind.ma20 != null ? `${f(ind.ma20)} / ${f(ind.ma60)}` : '--' },
      { label: 'RSI(14)', value: f(ind.rsi) },
      { label: 'MACD DIF', value: ind.macd?.dif != null ? f(ind.macd.dif, 3) : '--' },
      { label: 'MACD DEA', value: ind.macd?.dea != null ? f(ind.macd.dea, 3) : '--' },
      { label: 'MACD 柱', value: ind.macd?.hist != null ? f(ind.macd.hist, 3) : '--' },
      { label: '总市值(亿)', value: info.market_cap ? f(info.market_cap / 1e8) + '亿' : '--' },
      { label: '成交额(亿)', value: info.turnover ? f(info.turnover / 1e8) + '亿' : '--' },
      { label: '数据源', value: info.source || 'akshare' },
    ]
  } catch (e) { if (import.meta.env.DEV) console.warn('stock info fail', e) }
}

// ===== 生命周期 =====
// 列表被替换（加载/搜索/自动刷新）时回到第一页并重预取迷你走势
watch(() => props.stockList, (list) => {
  stockPage.value = 1
  prefetchSparklines(list)
})

function handleResize() { try { klineChart?.resize() } catch {} }

onMounted(() => {
  if (props.stockList.length) prefetchSparklines(props.stockList)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  try { klineChart?.dispose() } catch {}
})
</script>

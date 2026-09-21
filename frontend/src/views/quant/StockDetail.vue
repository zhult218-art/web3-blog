<template>
  <div class="min-h-screen px-4 md:px-6 py-8">
    <div class="mx-auto max-w-[1400px]">
      <!-- Header -->
      <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div class="flex items-center gap-3 min-w-0">
          <button @click="goBack" class="text-[10px] px-3 py-1.5 rounded-lg bg-[#0c1a2e] text-cyan-400 border border-cyan-400/20 hover:bg-cyan-500/20 transition flex items-center gap-1.5 flex-shrink-0">
            <span>←</span><span>返回</span>
          </button>
          <div class="min-w-0">
            <h1 class="text-xl font-bold text-cyan-300 scroll-title font-serif truncate">
              {{ stockName || rawCode || '--' }}
              <span class="text-sm text-slate-400 ml-2 font-mono">{{ rawCode }}</span>
            </h1>
            <div class="flex items-center gap-3 mt-1" v-if="quoteLoaded">
              <span class="text-lg font-black matrix-text" :class="(quote.change_percent || 0) >= 0 ? 'text-red-400' : 'text-green-400'">
                {{ quote.price != null ? Number(quote.price).toFixed(2) : '--' }}
              </span>
              <span :class="['text-xs font-mono', (quote.change_percent || 0) >= 0 ? 'text-red-400' : 'text-green-400']">
                {{ (quote.change_percent || 0) >= 0 ? '+' : '' }}{{ (quote.change_percent || 0)?.toFixed(2) }}%
              </span>
              <span class="text-[10px] text-slate-600 font-serif">{{ lastUpdate || '--' }}</span>
            </div>
          </div>
        </div>
        <button @click="reloadAll" class="text-[10px] px-3 py-1.5 rounded-lg bg-[#0c1a2e] text-cyan-400 border border-cyan-400/20 hover:bg-cyan-500/20 transition">
          刷新数据
        </button>
      </div>

      <!-- Tab Navigation -->
      <div class="scroll-tabs flex gap-1 mb-6 overflow-x-auto scrollbar-hide border-b border-amber-800/30 pb-1">
        <button v-for="tab in tabs" :key="tab.key"
          @click="activeTab = tab.key; onTabActivate(tab.key)"
          :class="['text-[11px] px-3 py-1.5 rounded-t-lg font-serif whitespace-nowrap transition',
            activeTab === tab.key ? 'text-cyan-300 border-b-2 border-amber-400 bg-cyan-500/5' : 'text-slate-500 hover:text-cyan-200']">
          {{ tab.label }}
        </button>
      </div>

      <!-- ===== Tab: 行情 (K线 + 实时行情 + 基本信息) ===== -->
      <div v-if="activeTab === 'quote'" class="space-y-5">
        <!-- K-line Chart -->
        <div class="scroll-card overflow-hidden">
          <div class="p-4 border-b border-cyan-800/30 flex items-center gap-2 flex-wrap">
            <h3 class="text-sm font-bold text-cyan-200 font-serif">K 线图</h3>
            <div class="flex items-center gap-1.5 ml-2 flex-wrap">
              <button v-for="p in periods" :key="p.key"
                @click="fetchKline(p.key)"
                :class="['text-[10px] px-2.5 py-1 rounded-lg transition',
                  klinePeriod === p.key ? 'bg-cyan-500/15 text-cyan-400 border border-cyan-400/20' : 'text-slate-500 hover:text-cyan-200']">
                {{ p.label }}
              </button>
            </div>
            <span v-if="klineSource" class="ml-auto text-[10px] px-2.5 py-1 rounded-lg bg-[#0d1726] text-amber-500/70 border border-cyan-800/30">
              数据源: <span class="text-cyan-400">{{ klineSource }}</span>
            </span>
          </div>
          <!-- 副图指标选择（多选可同时显示） -->
          <div class="px-4 py-2 border-b border-cyan-800/20 flex items-center gap-1.5 flex-wrap bg-[#0a1422]/40">
            <span class="text-[10px] text-slate-500 mr-1">副图(可多选):</span>
            <button v-for="ind in subIndicators" :key="ind.key"
              @click="toggleSub(ind.key)"
              :class="['text-[10px] px-2 py-0.5 rounded transition font-mono',
                selectedSubs.includes(ind.key) ? 'bg-fuchsia-500/20 text-fuchsia-300 border border-fuchsia-400/30' : 'text-slate-500 hover:text-cyan-200 border border-transparent']">
              {{ ind.label }}
            </button>
            <span class="text-[10px] text-slate-600 ml-2">BOLL 叠加在主图</span>
          </div>
          <div class="p-4">
            <div v-if="klineLoading" class="w-full flex items-center justify-center" style="height: 560px;">
              <div class="text-xs text-slate-500 animate-pulse">加载 K 线数据中...</div>
            </div>
            <div v-else-if="klineError" class="w-full flex flex-col items-center justify-center gap-3" style="height: 560px;">
              <div class="text-xs text-red-400/70">K 线加载失败: {{ klineError }}</div>
              <button @click="fetchKline(klinePeriod)" class="text-[10px] px-3 py-1.5 rounded-lg bg-[#0c1a2e] text-cyan-400 border border-cyan-400/20 hover:bg-cyan-500/20 transition">重试</button>
            </div>
            <div v-else ref="klineChartRef" class="w-full" style="height: 560px;"></div>
          </div>
        </div>

        <!-- 马尔科夫链走势预测 -->
        <div class="scroll-card overflow-hidden">
          <div class="p-4 border-b border-cyan-800/30 flex items-center justify-between">
            <h3 class="text-sm font-bold text-cyan-200 font-serif">🔮 技术面走势预测（马尔科夫链）</h3>
            <span class="text-[10px] text-slate-500">基于 MACD / RSI / KDJ / 均线 多状态转移</span>
          </div>
          <div class="p-4">
            <div v-if="!markovReady" class="text-xs text-slate-500 py-4 text-center">加载 K 线数据后生成预测…</div>
            <div v-else class="space-y-4">
              <!-- 预测结论 -->
              <div class="flex items-center gap-4">
                <div :class="['w-16 h-16 rounded-full flex flex-col items-center justify-center font-bold text-lg']" :style="markovBgStyle">
                  {{ markovEmoji }}
                </div>
                <div class="flex-1">
                  <div class="text-base font-bold" :style="{ color: markovColor }">{{ markovDirection }}</div>
                  <div class="text-[11px] text-slate-400 mt-0.5">置信度 <span class="font-mono" :style="{ color: markovColor }">{{ (markovConfidence * 100).toFixed(1) }}%</span> · 当前状态「{{ markovCurrentState }}」</div>
                  <div class="text-[11px] text-slate-500 mt-0.5">下一状态预测「{{ markovNextState }}」</div>
                </div>
              </div>
              <!-- 状态转移概率条 -->
              <div class="space-y-1.5">
                <div v-for="(s, i) in markovStateLabels" :key="i" class="flex items-center gap-2">
                  <span class="text-[10px] w-16 text-slate-400 flex-shrink-0">{{ s }}</span>
                  <div class="flex-1 h-4 rounded bg-[#0a1118] overflow-hidden relative">
                    <div class="h-full rounded transition-all duration-500"
                      :style="{ width: (markovProbs[i] * 100).toFixed(1) + '%', background: markovBarColors[i] }"></div>
                  </div>
                  <span class="text-[10px] font-mono w-12 text-right" :style="i === markovMaxIdx ? { color: markovColor } : {}">{{ (markovProbs[i] * 100).toFixed(1) }}%</span>
                </div>
              </div>
              <!-- 指标信号 -->
              <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-cyan-800/20">
                <div v-for="sig in markovSignals" :key="sig.name" class="rounded-lg bg-[#0a1118] p-2">
                  <div class="text-[10px] text-slate-500">{{ sig.name }}</div>
                  <div class="text-xs font-mono mt-0.5" :class="sig.bullish ? 'text-red-400' : sig.bearish ? 'text-green-400' : 'text-slate-400'">{{ sig.value }}</div>
                </div>
              </div>
              <div class="text-[10px] text-slate-600 pt-1">⚠️ 本预测基于历史技术指标状态转移概率，仅供参考，不构成投资建议。</div>
            </div>
          </div>
        </div>

        <!-- Real-time Quote + Basic Info -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <div class="scroll-card overflow-hidden">
            <div class="p-4 border-b border-cyan-800/30">
              <h3 class="text-sm font-bold text-cyan-200 font-serif">实时行情</h3>
            </div>
            <div class="p-4">
              <div v-if="quoteLoading" class="space-y-2">
                <div v-for="i in 6" :key="i" class="h-6 rounded bg-[#0a1118] animate-pulse"></div>
              </div>
              <div v-else-if="quoteError" class="flex flex-col items-center gap-3 py-6">
                <div class="text-xs text-red-400/70">行情加载超时或失败</div>
                <button @click="loadQuote" class="text-[10px] px-3 py-1.5 rounded-lg bg-[#0c1a2e] text-cyan-400 border border-cyan-400/20 hover:bg-cyan-500/20 transition">重试</button>
              </div>
              <div v-else class="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                <div v-for="item in quoteItems" :key="item.label" class="rounded-lg bg-[#0a1118] border border-cyan-800/20 p-2.5 min-w-0">
                  <div class="text-[10px] text-slate-400">{{ item.label }}</div>
                  <div class="text-xs font-mono text-slate-100 mt-0.5 truncate" :title="item.value">{{ item.value }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="scroll-card overflow-hidden">
            <div class="p-4 border-b border-cyan-800/30">
              <h3 class="text-sm font-bold text-cyan-200 font-serif">基本信息 / 技术指标</h3>
            </div>
            <div class="p-4">
              <div v-if="infoLoading" class="space-y-2">
                <div v-for="i in 6" :key="i" class="h-6 rounded bg-[#0a1118] animate-pulse"></div>
              </div>
              <div v-else-if="infoError" class="flex flex-col items-center gap-3 py-6">
                <div class="text-xs text-red-400/70">基本信息加载超时或失败</div>
                <button @click="loadStockInfo" class="text-[10px] px-3 py-1.5 rounded-lg bg-[#0c1a2e] text-cyan-400 border border-cyan-400/20 hover:bg-cyan-500/20 transition">重试</button>
              </div>
              <div v-else class="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                <div v-for="info in infoItems" :key="info.label" class="bg-[#0a1118] rounded-lg p-2.5 min-w-0">
                  <div class="text-[10px] text-slate-500">{{ info.label }}</div>
                  <div class="text-xs font-mono text-slate-100 mt-0.5 truncate" :title="info.value">{{ info.value }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== Component-based Tabs (资金流向/龙虎榜/研报/融资融券) ===== -->
      <div v-else-if="activeTab === 'fund'" class="scroll-card overflow-hidden">
        <div class="p-4 border-b border-cyan-800/30">
          <h3 class="text-sm font-bold text-cyan-200 font-serif">资金流向</h3>
        </div>
        <div class="p-4">
          <FundFlowChart :key="'fund-' + rawCode + '-' + reloadCounter" :code="rawCode" />
        </div>
      </div>

      <div v-else-if="activeTab === 'dragon'" class="scroll-card overflow-hidden">
        <div class="p-4 border-b border-cyan-800/30">
          <h3 class="text-sm font-bold text-cyan-200 font-serif">龙虎榜</h3>
        </div>
        <div class="p-4">
          <DragonTigerTable :key="'dragon-' + rawCode + '-' + reloadCounter" :code="rawCode" />
        </div>
      </div>

      <div v-else-if="activeTab === 'report'" class="scroll-card overflow-hidden">
        <div class="p-4 border-b border-cyan-800/30">
          <h3 class="text-sm font-bold text-cyan-200 font-serif">研报</h3>
        </div>
        <div class="p-4">
          <ReportsTable :key="'report-' + rawCode + '-' + reloadCounter" :code="rawCode" />
        </div>
      </div>

      <div v-else-if="activeTab === 'margin'" class="scroll-card overflow-hidden">
        <div class="p-4 border-b border-cyan-800/30">
          <h3 class="text-sm font-bold text-cyan-200 font-serif">融资融券</h3>
        </div>
        <div class="p-4">
          <MarginTable :key="'margin-' + rawCode + '-' + reloadCounter" :code="rawCode" />
        </div>
      </div>

      <!-- ===== Generic Data Tab (财务/估值/分红/股东/新闻/公告/大宗交易/限售解禁/概念板块) ===== -->
      <div v-else class="scroll-card overflow-hidden">
        <div class="p-4 border-b border-cyan-800/30 flex items-center justify-between">
          <h3 class="text-sm font-bold text-cyan-200 font-serif">{{ currentTabLabel }}</h3>
          <button @click="loadTabData(activeTab)" class="text-[10px] px-2.5 py-1 rounded-lg bg-[#0c1a2e] text-cyan-400 hover:text-cyan-300 border border-cyan-400/20 transition font-serif">刷新</button>
        </div>
        <div class="p-4">
          <!-- Loading skeleton -->
          <div v-if="tabMeta.loading" class="space-y-2">
            <div v-for="i in 5" :key="i" class="h-8 rounded bg-[#0a1118] animate-pulse"></div>
          </div>
          <!-- Error -->
          <div v-else-if="tabMeta.error" class="flex flex-col items-center gap-3 py-10">
            <div class="text-xs text-red-400/70">{{ tabMeta.error }}</div>
            <button @click="loadTabData(activeTab)" class="text-[10px] px-3 py-1.5 rounded-lg bg-[#0c1a2e] text-cyan-400 border border-cyan-400/20 hover:bg-cyan-500/20 transition">重试</button>
          </div>
          <!-- Empty -->
          <div v-else-if="tabMeta.empty" class="text-xs text-slate-600 py-10 text-center">暂无{{ currentTabLabel }}数据</div>
          <!-- Data rendering -->
          <div v-else>
            <!-- Array of objects → table -->
            <div v-if="tabMeta.kind === 'table'" class="overflow-x-auto">
              <table class="web3-table min-w-full">
                <thead>
                  <tr>
                    <th v-for="col in tabMeta.columns" :key="col">{{ colLabel(col) }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, i) in tabMeta.data" :key="i">
                    <td v-for="col in tabMeta.columns" :key="col" :class="isNumericField(col) ? 'font-mono' : ''">
                      {{ formatCell(row[col], col) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <!-- Object → key-value grid -->
            <div v-else-if="tabMeta.kind === 'kv'" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
              <div v-for="item in tabMeta.data" :key="item.label" class="rounded-lg bg-[#0a1118] border border-cyan-800/20 p-3 min-w-0">
                <div class="text-[10px] text-slate-400 uppercase tracking-wider truncate font-serif" :title="item.label">{{ item.label }}</div>
                <div class="text-xs font-mono text-slate-100 mt-1 break-all">{{ item.value }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import FundFlowChart from './components/FundFlowChart.vue'
import DragonTigerTable from './components/DragonTigerTable.vue'
import ReportsTable from './components/ReportsTable.vue'
import MarginTable from './components/MarginTable.vue'
import {
  getPyKline, getPyStockInfo, getPyQuote,
  getPyStockFinance, getPyStockHolders, getPyStockDividend, getPyStockBlockTrade,
  getStockValuation,
  getStockNews, getAnnouncements, getStockLockup, getConceptHot,
  getStockKlineVR,
} from '@/api/quant'

const route = useRoute()
const router = useRouter()

// 路由参数 code 为带前缀格式（如 sh600519）
const sym = computed(() => String(route.params.code || '').trim().toLowerCase())
const rawCode = computed(() => sym.value.replace(/^(sh|sz|bj)/, ''))

const tabs = [
  { key: 'quote', label: '行情' },
  { key: 'finance', label: '财务' },
  { key: 'valuation', label: '估值' },
  { key: 'dividend', label: '分红' },
  { key: 'holders', label: '股东' },
  { key: 'dragon', label: '龙虎榜' },
  { key: 'margin', label: '融资融券' },
  { key: 'fund', label: '资金流向' },
  { key: 'news', label: '新闻' },
  { key: 'report', label: '研报' },
  { key: 'announcement', label: '公告' },
  { key: 'block', label: '大宗交易' },
  { key: 'lockup', label: '限售解禁' },
  { key: 'concept', label: '概念板块' },
]
const activeTab = ref('quote')
const currentTabLabel = computed(() => (tabs.find(t => t.key === activeTab.value) || {}).label || '')

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
const klinePeriod = ref('daily')

// 副图指标
const subIndicators = [
  { key: 'MACD', label: 'MACD' },
  { key: 'KDJ', label: 'KDJ' },
  { key: 'RSI', label: 'RSI' },
  { key: 'BOLL', label: 'BOLL' },
  { key: 'WR', label: 'WR' },
  { key: 'CCI', label: 'CCI' },
  { key: 'OBV', label: 'OBV' },
  { key: 'DMI', label: 'DMI' },
  { key: 'VOL', label: 'VOL' },
]
const subIndicator = ref('MACD')
// 多选副图指标（可同时显示多个）
const selectedSubs = ref(['MACD'])
function toggleSub(key) {
  const i = selectedSubs.value.indexOf(key)
  if (i >= 0) {
    // 至少保留一个副图
    if (selectedSubs.value.length > 1) selectedSubs.value.splice(i, 1)
  } else {
    selectedSubs.value.push(key)
  }
  subIndicator.value = selectedSubs.value[0]
}
// 缓存最后一次 K 线数据，供副图切换时重算
const lastKlineData = ref(null)
let lastIsLine = false

// ===== K-line =====
const klineChartRef = ref(null)
let klineChart = null
const klineLoading = ref(false)
const klineError = ref('')
const klineSource = ref('')

// ---------- 技术指标计算工具 ----------
function sma(arr, n) {
  const out = []
  let s = 0
  for (let i = 0; i < arr.length; i++) {
    s += arr[i]
    if (i >= n) s -= arr[i - n]
    out.push(i >= n - 1 ? s / n : null)
  }
  return out
}
function ema(arr, n) {
  const k = 2 / (n + 1)
  const out = []
  let prev = null
  for (let i = 0; i < arr.length; i++) {
    if (prev === null) prev = arr[i]
    else prev = arr[i] * k + prev * (1 - k)
    out.push(prev)
  }
  return out
}
function macd(closes, fast = 12, slow = 26, signal = 9) {
  const ef = ema(closes, fast), es = ema(closes, slow)
  const dif = closes.map((_, i) => ef[i] - es[i])
  const dea = ema(dif, signal)
  const hist = dif.map((v, i) => (v - dea[i]) * 2)
  return { dif, dea, hist }
}
function kdj(data, n = 9, m1 = 3, m2 = 3) {
  const k = [], d = [], j = []
  let kPrev = 50, dPrev = 50
  for (let i = 0; i < data.length; i++) {
    const start = Math.max(0, i - n + 1)
    let hh = -Infinity, ll = Infinity
    for (let x = start; x <= i; x++) {
      if (data[x].high > hh) hh = data[x].high
      if (data[x].low < ll) ll = data[x].low
    }
    const rsv = hh === ll ? 50 : ((data[i].close - ll) / (hh - ll)) * 100
    const kCur = (rsv + (m1 - 1) * kPrev) / m1
    const dCur = (kCur + (m2 - 1) * dPrev) / m2
    const jCur = 3 * kCur - 2 * dCur
    k.push(kCur); d.push(dCur); j.push(jCur)
    kPrev = kCur; dPrev = dCur
  }
  return { k, d, j }
}
function rsi(closes, n) {
  const out = []
  for (let i = 0; i < closes.length; i++) {
    if (i < n) { out.push(null); continue }
    let gain = 0, loss = 0
    for (let x = i - n + 1; x <= i; x++) {
      const ch = closes[x] - closes[x - 1]
      if (ch >= 0) gain += ch; else loss -= ch
    }
    const avgGain = gain / n, avgLoss = loss / n
    const rs = avgLoss === 0 ? 100 : avgGain / avgLoss
    out.push(100 - 100 / (1 + rs))
  }
  return out
}
function boll(closes, n = 20, k = 2) {
  const mid = sma(closes, n)
  const up = [], dn = []
  for (let i = 0; i < closes.length; i++) {
    if (i < n - 1) { up.push(null); dn.push(null); continue }
    let s = 0
    for (let x = i - n + 1; x <= i; x++) s += (closes[x] - mid[i]) ** 2
    const std = Math.sqrt(s / n)
    up.push(mid[i] + k * std)
    dn.push(mid[i] - k * std)
  }
  return { up, mid, dn }
}
function wr(data, n = 14) {
  const out = []
  for (let i = 0; i < data.length; i++) {
    if (i < n - 1) { out.push(null); continue }
    let hh = -Infinity, ll = Infinity
    for (let x = i - n + 1; x <= i; x++) {
      if (data[x].high > hh) hh = data[x].high
      if (data[x].low < ll) ll = data[x].low
    }
    out.push(hh === ll ? 0 : ((hh - data[i].close) / (hh - ll)) * -100)
  }
  return out
}
function cci(data, n = 14) {
  const tp = data.map(d => (d.high + d.low + d.close) / 3)
  const ma = sma(tp, n)
  const out = []
  for (let i = 0; i < data.length; i++) {
    if (i < n - 1) { out.push(null); continue }
    let md = 0
    for (let x = i - n + 1; x <= i; x++) md += Math.abs(tp[x] - ma[i])
    md /= n
    out.push(md === 0 ? 0 : (tp[i] - ma[i]) / (0.015 * md))
  }
  return out
}
function obv(data) {
  const out = []
  let acc = 0
  for (let i = 0; i < data.length; i++) {
    if (i > 0) {
      const sign = data[i].close > data[i - 1].close ? 1 : data[i].close < data[i - 1].close ? -1 : 0
      acc += sign * (data[i].volume || 0)
    }
    out.push(acc)
  }
  return out
}
function dmi(data, n = 14) {
  const tr = [], pdm = [], mdm = []
  for (let i = 0; i < data.length; i++) {
    if (i === 0) { tr.push(0); pdm.push(0); mdm.push(0); continue }
    const h = data[i].high, l = data[i].low, pc = data[i - 1].close
    const trv = Math.max(h - l, Math.abs(h - pc), Math.abs(l - pc))
    const up = h - data[i - 1].high, down = data[i - 1].low - l
    const pdm_v = up > down && up > 0 ? up : 0
    const mdm_v = down > up && down > 0 ? down : 0
    tr.push(trv); pdm.push(pdm_v); mdm.push(mdm_v)
  }
  const trS = sma(tr, n), pdmS = sma(pdm, n), mdmS = sma(mdm, n)
  const pdi = [], mdi = [], dx = []
  for (let i = 0; i < data.length; i++) {
    const p = trS[i] ? (pdmS[i] / trS[i]) * 100 : 0
    const m = trS[i] ? (mdmS[i] / trS[i]) * 100 : 0
    pdi.push(p); mdi.push(m)
    dx.push((p + m) ? Math.abs(p - m) / (p + m) * 100 : 0)
  }
  const adx = sma(dx, n)
  return { pdi, mdi, adx }
}

// ============================================================
// 马尔科夫链走势预测
// 多指标综合状态 → 历史转移概率矩阵 → 下一状态预测
// ============================================================
const MARKOV_STATES = ['强空', '偏空', '震荡', '偏多', '强多']
const MARKOV_COLORS = ['#22c55e', '#4ade80', '#a3a3a3', '#f87171', '#ef4444']
const MARKOV_EMOJI = ['🔻', '↘', '➡️', '↗', '🔺']

function markovClassify(d, macdHist, rsi14, ma5, ma20, kdjJ) {
  // 综合打分：-2 ~ +2
  let score = 0
  if (macdHist > 0) score++
  else if (macdHist < 0) score--
  if (rsi14 > 55) score++
  else if (rsi14 < 45) score--
  if (ma5 != null && ma20 != null) {
    if (ma5 > ma20) score++
    else if (ma5 < ma20) score--
  }
  if (kdjJ != null) {
    if (kdjJ > 80) score++
    else if (kdjJ < 20) score--
  }
  // 映射到 0~4
  if (score <= -2) return 0
  if (score === -1) return 1
  if (score === 0) return 2
  if (score === 1) return 3
  return 4
}

const markovPrediction = computed(() => {
  const data = lastKlineData.value
  if (!data || data.length < 30) return null
  const closes = data.map(d => Number(d.close) || 0)
  const m = macd(closes)
  const r14 = rsi(closes, 14)
  const ma5Arr = sma(closes, 5)
  const ma20Arr = sma(closes, 20)
  const k = kdj(data)
  const states = []
  for (let i = 0; i < data.length; i++) {
    if (i < 26) { states.push(-1); continue } // 等 MACD 预热
    const s = markovClassify(data[i], m.hist[i], r14[i], ma5Arr[i], ma20Arr[i], k.j[i])
    states.push(s)
  }
  // 构建转移矩阵 5x5
  const N = 5
  const trans = Array.from({ length: N }, () => new Array(N).fill(0))
  for (let i = 1; i < states.length; i++) {
    const a = states[i - 1], b = states[i]
    if (a >= 0 && b >= 0) trans[a][b]++
  }
  // 归一化
  const probs = trans.map(row => {
    const sum = row.reduce((x, y) => x + y, 0)
    return sum > 0 ? row.map(x => x / sum) : new Array(N).fill(1 / N)
  })
  const current = states[states.length - 1]
  const nextProbs = probs[current] || new Array(N).fill(1 / N)
  let maxIdx = 0, maxP = 0
  nextProbs.forEach((p, i) => { if (p > maxP) { maxP = p; maxIdx = i } })

  // 方向与置信度
  const bullPower = (nextProbs[3] || 0) + (nextProbs[4] || 0) * 1.5
  const bearPower = (nextProbs[1] || 0) + (nextProbs[0] || 0) * 1.5
  let direction, color, emoji
  if (maxIdx >= 3 && bullPower > bearPower) {
    direction = maxIdx === 4 ? '强烈看涨' : '看涨'
    color = '#ef4444'; emoji = MARKOV_EMOJI[maxIdx]
  } else if (maxIdx <= 1 && bearPower > bullPower) {
    direction = maxIdx === 0 ? '强烈看跌' : '看跌'
    color = '#22c55e'; emoji = MARKOV_EMOJI[maxIdx]
  } else {
    direction = '震荡整理'
    color = '#a3a3a3'; emoji = '➡️'
  }

  // 指标信号明细
  const last = data.length - 1
  const signals = [
    { name: 'MACD 柱', value: m.hist[last] != null ? (m.hist[last] >= 0 ? '+' : '') + m.hist[last].toFixed(3) : '--', bullish: m.hist[last] > 0, bearish: m.hist[last] < 0 },
    { name: 'RSI(14)', value: r14[last] != null ? r14[last].toFixed(1) : '--', bullish: r14[last] > 50, bearish: r14[last] < 50 },
    { name: 'MA5/MA20', value: (ma5Arr[last] && ma20Arr[last]) ? `${ma5Arr[last].toFixed(1)}/${ma20Arr[last].toFixed(1)}` : '--', bullish: ma5Arr[last] > ma20Arr[last], bearish: ma5Arr[last] < ma20Arr[last] },
    { name: 'KDJ-J', value: k.j[last] != null ? k.j[last].toFixed(1) : '--', bullish: k.j[last] > 50, bearish: k.j[last] < 50 },
  ]

  return {
    current, maxIdx, nextProbs, maxP, direction, color, emoji, signals,
  }
})

const markovReady = computed(() => !!markovPrediction.value)
const markovDirection = computed(() => markovPrediction.value?.direction || '--')
const markovColor = computed(() => markovPrediction.value?.color || '#a3a3a3')
const markovEmoji = computed(() => markovPrediction.value?.emoji || '❓')
const markovConfidence = computed(() => markovPrediction.value?.maxP || 0)
const markovCurrentState = computed(() => MARKOV_STATES[markovPrediction.value?.current ?? 2] || '--')
const markovNextState = computed(() => MARKOV_STATES[markovPrediction.value?.maxIdx ?? 2] || '--')
const markovProbs = computed(() => markovPrediction.value?.nextProbs || [0, 0, 0, 0, 0])
const markovMaxIdx = computed(() => markovPrediction.value?.maxIdx ?? 2)
const markovStateLabels = computed(() => MARKOV_STATES)
const markovBarColors = computed(() => MARKOV_COLORS)
const markovSignals = computed(() => markovPrediction.value?.signals || [])
const markovBgStyle = computed(() => {
  const c = markovColor.value
  return { backgroundColor: c + '1a', border: `1px solid ${c}4d` }
})

// 各周期请求的根数（pyquant 语义：days = 该周期的 bar 数量）
const KLINE_DAYS_MAP = { intraday: 1, '5d': 5, '1m': 1, '5m': 5, '15m': 5, '30m': 10, '60m': 15, daily: 250, weekly: 156, monthly: 120 }

async function fetchKline(period) {
  klinePeriod.value = period
  if (!sym.value) return
  const daysMap = KLINE_DAYS_MAP
  klineLoading.value = true
  klineError.value = ''
  klineSource.value = ''
  try {
    // 优先 pyquant
    let res = await getPyKline(sym.value, daysMap[period] || 120, period)
    let data = res.data
    // pyquant 失败或空 → fallback 到 VR API（rawCode 6位）
    if ((!data || !data.length) && rawCode.value) {
      try {
        const vrPeriodMap = { daily: 'day', weekly: 'week', monthly: 'month', '60m': 'min60', '30m': 'min30', '15m': 'min15', '5m': 'min5', '1m': 'min1', intraday: 'min5', '5d': 'min30' }
        const vrRes = await getStockKlineVR(rawCode.value, vrPeriodMap[period] || 'day')
        const vrData = vrRes?.data?.data || vrRes?.data
        if (Array.isArray(vrData) && vrData.length) {
          data = vrData.map(d => ({
            date: d.date || d.day, datetime: d.datetime || d.date,
            open: d.open, close: d.close, high: d.high, low: d.low,
            volume: d.volume, turnover: d.turnover, source: 'vibe-research'
          }))
          klineSource.value = 'vibe-research'
        }
      } catch (vrErr) { console.warn('VR kline fallback fail', vrErr) }
    }
    if (!data || !data.length) {
      klineLoading.value = false
      klineError.value = '该股票暂无 K 线数据（pyquant 与 VR 均无返回）'
      return
    }
    if (!klineSource.value) klineSource.value = (data[0] && data[0].source) || 'pyquant'
    // 缓存原始数据，副图切换时无需重新请求
    lastKlineData.value = data
    lastIsLine = period === 'intraday' || period === '5d'
    // 必须先关闭 loading（v-if 加载态），v-else 的图表容器才会渲染，
    // 否则 nextTick 后 ref 仍为 null 导致静默早退、图表空白
    klineLoading.value = false
    await nextTick()
    renderKlineChart()
  } catch (e) {
    console.warn('kline fail', e)
    klineError.value = e?.message || '网络错误'
  } finally {
    klineLoading.value = false
  }
}

// 渲染 K 线 + 成交量 + 副图指标（三网格）
function renderKlineChart() {
  const data = lastKlineData.value
  if (!data || !data.length || !klineChartRef.value) return
  if (!klineChart) klineChart = echarts.init(klineChartRef.value)
  klineChart.clear()

  const isLine = lastIsLine
  const period = klinePeriod.value
  const dates = data.map(d => d.datetime || d.date)
  const ohlc = data.map(d => [d.open, d.close, d.low, d.high])
  const volumes = data.map(d => d.volume || 0)
  const closes = data.map(d => Number(d.close) || 0)
  const colors = data.map(d => (Number(d.close) >= Number(d.open)) ? '#ef4444' : '#22c55e')
  const barColorFn = (p) => colors[p.dataIndex] || '#666'

  // MA 均线
  function ma(n) {
    return closes.map((_, i) => {
      if (i < n - 1) return null
      let s = 0
      for (let j = i - n + 1; j <= i; j++) s += closes[j]
      return +(s / n).toFixed(2)
    })
  }
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

  // BOLL 叠加在主图
  let bollSeries = []
  if (!isLine && closes.length >= 20) {
    const b = boll(closes, 20, 2)
    bollSeries = [
      { name: 'BOLL上', type: 'line', data: b.up, xAxisIndex: 0, yAxisIndex: 0, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#f97316' }, itemStyle: { color: '#f97316' } },
      { name: 'BOLL中', type: 'line', data: b.mid, xAxisIndex: 0, yAxisIndex: 0, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#f59e0b', type: [4, 2] }, itemStyle: { color: '#f59e0b' } },
      { name: 'BOLL下', type: 'line', data: b.dn, xAxisIndex: 0, yAxisIndex: 0, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#f97316' }, itemStyle: { color: '#f97316' } },
    ]
  }

  const barSeries = {
    type: 'bar', xAxisIndex: 1, yAxisIndex: 1, data: volumes, itemStyle: { color: barColorFn },
  }

  // ===== 副图指标（多选，每个指标独立网格） =====
  const activeSubs = selectedSubs.value
  const subCount = activeSubs.length
  const subAxisLabel = { show: true, color: '#666', fontSize: 9 }
  // 网格布局百分比（基于容器高度）
  const MAIN_TOP = 10, MAIN_BOTTOM = 52       // 主图：top 10%, bottom 52%
  const VOL_TOP = 52, VOL_BOTTOM = 70          // 成交量：top 52%, bottom 70%
  const SUB_AREA_TOP = 70, SUB_AREA_BOTTOM = 88 // 副图区域：70%~88%
  const SUB_GAP = 1.5
  const subH = subCount > 0 ? (SUB_AREA_BOTTOM - SUB_AREA_TOP - SUB_GAP * (subCount - 1)) / subCount : 0

  // 计算每个副图的 grid 定义
  const subGrids = activeSubs.map((_, i) => {
    const top = SUB_AREA_TOP + i * (subH + SUB_GAP)
    const bottom = 100 - (top + subH)
    return { left: '8%', right: '8%', top: top + '%', bottom: bottom + '%' }
  })

  // 生成单个指标的系列（gridIndex 动态指定）
  function buildSubSeries(key, gi) {
    const ser = [], leg = []
    if (key === 'MACD') {
      const m = macd(closes)
      ser.push(
        { name: 'DIF', type: 'line', data: m.dif, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#f59e0b' }, itemStyle: { color: '#f59e0b' } },
        { name: 'DEA', type: 'line', data: m.dea, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#3b82f6' }, itemStyle: { color: '#3b82f6' } },
        { name: 'MACD', type: 'bar', data: m.hist, xAxisIndex: gi, yAxisIndex: gi, itemStyle: { color: p => (p.value >= 0 ? '#ef4444' : '#22c55e') } },
      ); leg.push('DIF', 'DEA', 'MACD')
    } else if (key === 'KDJ') {
      const k = kdj(data)
      ser.push(
        { name: 'K', type: 'line', data: k.k, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#f59e0b' }, itemStyle: { color: '#f59e0b' } },
        { name: 'D', type: 'line', data: k.d, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#3b82f6' }, itemStyle: { color: '#3b82f6' } },
        { name: 'J', type: 'line', data: k.j, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#a855f7' }, itemStyle: { color: '#a855f7' } },
      ); leg.push('K', 'D', 'J')
    } else if (key === 'RSI') {
      const r6 = rsi(closes, 6), r12 = rsi(closes, 12), r24 = rsi(closes, 24)
      ser.push(
        { name: 'RSI6', type: 'line', data: r6, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#f59e0b' }, itemStyle: { color: '#f59e0b' } },
        { name: 'RSI12', type: 'line', data: r12, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#3b82f6' }, itemStyle: { color: '#3b82f6' } },
        { name: 'RSI24', type: 'line', data: r24, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#a855f7' }, itemStyle: { color: '#a855f7' } },
      ); leg.push('RSI6', 'RSI12', 'RSI24')
    } else if (key === 'BOLL') {
      const b = boll(closes, 20, 2)
      const width = b.up.map((u, i) => (u != null && b.dn[i] != null ? +(u - b.dn[i]).toFixed(3) : null))
      ser.push({ name: 'BOLL宽度', type: 'line', data: width, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#f97316' }, itemStyle: { color: '#f97316' }, areaStyle: { color: 'rgba(249,115,22,0.15)' } })
      leg.push('BOLL宽度')
    } else if (key === 'WR') {
      ser.push({ name: 'WR14', type: 'line', data: wr(data, 14), xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#ec4899' }, itemStyle: { color: '#ec4899' } })
      leg.push('WR14')
    } else if (key === 'CCI') {
      ser.push({ name: 'CCI14', type: 'line', data: cci(data, 14), xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#22d3ee' }, itemStyle: { color: '#22d3ee' } })
      leg.push('CCI14')
    } else if (key === 'OBV') {
      ser.push({ name: 'OBV', type: 'line', data: obv(data), xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#a855f7' }, itemStyle: { color: '#a855f7' }, areaStyle: { color: 'rgba(168,85,247,0.15)' } })
      leg.push('OBV')
    } else if (key === 'DMI') {
      const d = dmi(data, 14)
      ser.push(
        { name: 'PDI', type: 'line', data: d.pdi, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#ef4444' }, itemStyle: { color: '#ef4444' } },
        { name: 'MDI', type: 'line', data: d.mdi, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#22c55e' }, itemStyle: { color: '#22c55e' } },
        { name: 'ADX', type: 'line', data: d.adx, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#3b82f6' }, itemStyle: { color: '#3b82f6' } },
      ); leg.push('PDI', 'MDI', 'ADX')
    } else {
      // VOL：成交量均线
      const vma5 = sma(volumes, 5), vma10 = sma(volumes, 10)
      ser.push(
        { name: 'VOL', type: 'bar', data: volumes, xAxisIndex: gi, yAxisIndex: gi, itemStyle: { color: barColorFn } },
        { name: 'VMA5', type: 'line', data: vma5, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#f59e0b' }, itemStyle: { color: '#f59e0b' } },
        { name: 'VMA10', type: 'line', data: vma10, xAxisIndex: gi, yAxisIndex: gi, smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#3b82f6' }, itemStyle: { color: '#3b82f6' } },
      ); leg.push('VOL', 'VMA5', 'VMA10')
    }
    return { ser, leg }
  }

  const subSeries = []
  const subLegend = []
  activeSubs.forEach((key, i) => {
    const gi = 2 + i  // gridIndex: 0=主图, 1=成交量, 2..N=副图
    const { ser, leg } = buildSubSeries(key, gi)
    subSeries.push(...ser)
    subLegend.push(...leg)
  })

  const priceSeries = isLine
    ? [
        { name: '价格', type: 'line', xAxisIndex: 0, yAxisIndex: 0, data: closes,
          smooth: true, showSymbol: false, lineStyle: { width: 1.2, color: '#f59e0b' }, itemStyle: { color: '#f59e0b' } },
        { name: '均价', type: 'line', xAxisIndex: 0, yAxisIndex: 0,
          data: data.map(d => (d.avg ? Number(d.avg) : null)),
          smooth: true, showSymbol: false, lineStyle: { width: 1, color: '#3b82f6', type: [4, 2] }, itemStyle: { color: '#3b82f6' } },
        ...bollSeries,
      ]
    : [
        { type: 'candlestick', xAxisIndex: 0, yAxisIndex: 0, data: ohlc,
          itemStyle: { color: '#ef4444', color0: '#22c55e', borderColor: '#ef4444', borderColor0: '#22c55e' } },
        ...maSeries,
        ...bollSeries,
      ]

  const series = [...priceSeries, barSeries, ...subSeries]

  const legendData = isLine ? ['价格', '均价'] : [...maSeries.map(s => s.name), ...bollSeries.map(s => s.name), ...subLegend]

  // 构建 grid / xAxis / yAxis 数组（2 + subCount 个网格）
  const grids = [
    { left: '8%', right: '8%', top: MAIN_TOP + '%', bottom: MAIN_BOTTOM + '%' },
    { left: '8%', right: '8%', top: VOL_TOP + '%', bottom: (100 - VOL_BOTTOM) + '%' },
    ...subGrids,
  ]
  const xAxes = grids.map((g, i) => ({
    type: 'category', data: dates, gridIndex: i,
    axisLine: { lineStyle: { color: '#333' } },
    axisLabel: i === grids.length - 1 ? { color: '#666', fontSize: 10 } : { show: false },
  }))
  const yAxes = grids.map((g, i) => {
    if (i === 0) return { type: 'value', gridIndex: 0, scale: true, splitLine: { lineStyle: { color: 'rgba(34,211,238,0.05)' } }, axisLabel: { color: '#666', fontSize: 10 } }
    if (i === 1) return { type: 'value', gridIndex: 1, scale: true, splitLine: { show: false }, axisLabel: { show: false } }
    return { type: 'value', gridIndex: i, scale: true, splitLine: { lineStyle: { color: 'rgba(34,211,238,0.04)' } }, axisLabel: subAxisLabel }
  })
  const allAxisIdx = grids.map((_, i) => i)

  klineChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis', confine: true, axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(8,12,24,0.92)', borderColor: 'rgba(34,211,238,0.2)',
      textStyle: { color: '#ccc', fontSize: 10 },
    },
    legend: {
      top: 2, right: 6, textStyle: { color: '#888', fontSize: 10 },
      itemWidth: 12, itemHeight: 8, data: legendData, type: 'scroll',
    },
    grid: grids,
    xAxis: xAxes,
    yAxis: yAxes,
    series,
    dataZoom: [
      { type: 'inside', xAxisIndex: allAxisIdx },
      {
        type: 'slider', xAxisIndex: allAxisIdx, height: 14, bottom: 0,
        borderColor: 'transparent', fillerColor: 'rgba(0,255,255,0.08)',
        handleStyle: { color: '#0ff' }, textStyle: { color: '#666' },
        startValue: isLine ? Math.max(0, dates.length - 240) : Math.max(0, dates.length - (period === 'weekly' ? 52 : period === 'monthly' ? 24 : period === 'daily' ? 250 : 60)),
        endValue: Math.max(0, dates.length - 1),
      },
    ],
  }, true)
}

// ===== Real-time Quote =====
const quote = ref({})
const quoteLoaded = ref(false)
const quoteLoading = ref(false)
const quoteError = ref('')
const stockName = ref('')
const lastUpdate = ref('')

const quoteItems = computed(() => {
  const q = quote.value || {}
  const f = (v, nd = 2) => (v === undefined || v === null || isNaN(v)) ? '--' : Number(v).toFixed(nd)
  return [
    { label: '最新价', value: q.price != null ? f(q.price) : '--' },
    { label: '涨跌幅', value: q.change_percent != null ? f(q.change_percent) + '%' : '--' },
    { label: '涨跌额', value: q.change != null ? f(q.change) : '--' },
    { label: '今开', value: q.open != null ? f(q.open) : '--' },
    { label: '最高', value: q.high != null ? f(q.high) : '--' },
    { label: '最低', value: q.low != null ? f(q.low) : '--' },
    { label: '昨收', value: q.pre_close != null ? f(q.pre_close) : '--' },
    { label: '成交量(手)', value: q.volume != null ? (q.volume / 100).toFixed(0) : '--' },
    { label: '成交额', value: q.turnover != null ? (q.turnover / 1e8).toFixed(2) + '亿' : '--' },
    { label: '振幅', value: q.amplitude != null ? f(q.amplitude) + '%' : '--' },
    { label: '换手率', value: q.turnover_rate != null ? f(q.turnover_rate) + '%' : '--' },
    { label: '市盈率(TTM)', value: q.pe != null ? f(q.pe) : '--' },
    { label: '市净率', value: q.pb != null ? f(q.pb) : '--' },
    { label: '总市值', value: q.market_cap != null ? (q.market_cap / 1e8).toFixed(2) + '亿' : '--' },
  ]
})

async function loadQuote() {
  if (!sym.value) return
  quoteLoading.value = true
  quoteError.value = ''
  try {
    const res = await getPyQuote(sym.value)
    const d = res.data || {}
    quote.value = d
    quoteLoaded.value = true
    if (d.name) stockName.value = d.name
    lastUpdate.value = new Date().toLocaleTimeString()
  } catch (e) {
    console.warn('quote fail', e)
    quoteError.value = e?.message || '请求超时'
  } finally {
    quoteLoading.value = false
  }
}

// ===== Stock Info / Indicators =====
const infoItems = ref([])
const infoLoading = ref(false)
const infoError = ref('')

async function loadStockInfo() {
  if (!sym.value) return
  infoLoading.value = true
  infoError.value = ''
  try {
    const res = await getPyStockInfo(sym.value)
    const info = res.data?.quote || {}
    const ind = res.data?.indicators || {}
    if (info.name) stockName.value = info.name
    const f = (v, nd = 2) => (v === undefined || v === null || isNaN(v)) ? '--' : Number(v).toFixed(nd)
    infoItems.value = [
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
  } catch (e) {
    console.warn('stock info fail', e)
    infoError.value = e?.message || '请求超时'
  } finally {
    infoLoading.value = false
  }
}

// ===== Generic tab data loading (VR APIs) =====
// tabMeta: keyed by tab key, holds { loading, error, data, kind, columns, empty, loaded, component }
const tabMetas = reactive({})
for (const t of tabs) {
  tabMetas[t.key] = { loading: false, error: '', data: null, kind: 'table', columns: [], empty: false, loaded: false, component: null }
}
function ensureTabMeta(key) {
  if (!tabMetas[key]) {
    tabMetas[key] = { loading: false, error: '', data: null, kind: 'table', columns: [], empty: false, loaded: false, component: null }
  }
  return tabMetas[key]
}
const tabMeta = computed(() => tabMetas[activeTab.value])

const NUMERIC_HINTS = ['price', 'pct', 'percent', 'change', 'net', 'amount', 'rate', 'ratio', 'pe', 'pb', 'value', 'share', 'stock', 'hold', 'cap', 'turnover', 'volume']

// 英文字段 → 中文表头（通用数据表展示用）
const FIELD_LABELS = {
  period: '报告期', revenue: '营业总收入', revenue_yoy: '营收同比', net_profit: '净利润',
  net_profit_yoy: '净利同比', deduct_profit: '扣非净利润', deduct_yoy: '扣非同比',
  eps: '每股收益', bvps: '每股净资产', op_cf_ps: '每股经营现金流', roe: 'ROE',
  gross_margin: '毛利率', net_margin: '净利率', debt_ratio: '资产负债率',
  current_ratio: '流动比率', inventory_days: '存货周转天数',
  date: '日期', holder_num: '股东户数', change: '户数增减', change_ratio: '增减比例(%)',
  avg_shares: '户均持股(股)', avg_market_cap_wan: '户均市值(万)', range_change_pct: '区间涨跌幅(%)',
  pe_ttm: '市盈率TTM', pb: '市净率', dividend_yield: '股息率',
  name: '名称', code: '代码', title: '标题', change_percent: '涨跌幅(%)',
}
const colLabel = col => FIELD_LABELS[col] || col

function isNumericField(col) {
  const k = String(col || '').toLowerCase()
  return NUMERIC_HINTS.some(h => k.includes(h))
}

function formatCell(v, col) {
  if (v === null || v === undefined || v === '') return '--'
  if (typeof v === 'number') {
    return isNumericField(col) ? v.toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : String(v)
  }
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

// 把任意返回归一化为 { kind, data, columns }
function normalizeData(payload, tabKey) {
  const meta = ensureTabMeta(tabKey)
  // 概念板块：{ concepts: [{name, change_pct, ...}] } 或 [{name, change_pct}]
  if (tabKey === 'concept') {
    const arr = Array.isArray(payload) ? payload : (payload?.concepts || payload?.data || [])
    if (!Array.isArray(arr) || !arr.length) return { kind: 'table', data: [], columns: [] }
    return { kind: 'table', data: arr, columns: pickColumns(arr) }
  }
  // 数组 → 表格
  if (Array.isArray(payload) && payload.length) {
    return { kind: 'table', data: payload, columns: pickColumns(payload) }
  }
  // 对象：可能是 { list: [...] } 或单条 KV
  if (payload && typeof payload === 'object') {
    // 找到第一个数组字段
    for (const k of Object.keys(payload)) {
      if (Array.isArray(payload[k]) && payload[k].length) {
        return { kind: 'table', data: payload[k], columns: pickColumns(payload[k]) }
      }
    }
    // KV 模式
    const kv = Object.entries(payload)
      .filter(([k, v]) => v !== null && v !== undefined && typeof v !== 'object')
      .map(([k, v]) => ({ label: k, value: String(v) }))
    if (kv.length) return { kind: 'kv', data: kv, columns: [] }
    // 复杂对象 → 整体 KV
    const kvAll = Object.entries(payload)
      .map(([k, v]) => ({ label: k, value: typeof v === 'object' ? JSON.stringify(v) : String(v) }))
    if (kvAll.length) return { kind: 'kv', data: kvAll, columns: [] }
  }
  return { kind: 'table', data: [], columns: [] }
}

function pickColumns(arr) {
  if (!arr || !arr.length) return []
  // 取所有对象 key 的并集，按出现频率排序，最多 8 列
  const counter = {}
  for (const item of arr) {
    if (item && typeof item === 'object') {
      for (const k of Object.keys(item)) {
        counter[k] = (counter[k] || 0) + 1
      }
    }
  }
  return Object.entries(counter)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 8)
    .map(([k]) => k)
}

async function loadTabData(tabKey) {
  const meta = ensureTabMeta(tabKey)
  // 组件型 tab：不在此加载（组件自加载），仅标记已激活
  if (tabKey === 'fund' || tabKey === 'dragon' || tabKey === 'report' || tabKey === 'margin') {
    meta.component = tabKey
    meta.loaded = true
    meta.loading = false
    meta.empty = false
    return
  }
  const code = rawCode.value
  if (!code) return
  meta.loading = true
  meta.error = ''
  meta.empty = false
  meta.data = null
  meta.columns = []
  try {
    let res
    switch (tabKey) {
      // 财务/股东/分红/大宗走 pyquant（Supabase 落库优先，规避上游间歇风控）
      case 'finance': res = await getPyStockFinance(sym.value); break
      case 'valuation': res = await getStockValuation(code); break
      case 'dividend': res = await getPyStockDividend(sym.value); break
      case 'holders': res = await getPyStockHolders(sym.value); break
      case 'news': res = await getStockNews(code); break
      case 'announcement': res = await getAnnouncements(code); break
      case 'block': res = await getPyStockBlockTrade(sym.value); break
      case 'lockup': res = await getStockLockup(code); break
      case 'concept': res = await getConceptHot(code); break
      default: return
    }
    const payload = res?.data
    const norm = normalizeData(payload, tabKey)
    meta.kind = norm.kind
    meta.data = norm.data
    meta.columns = norm.columns
    meta.empty = !norm.data || !norm.data.length
    meta.loaded = true
  } catch (e) {
    console.warn(`${tabKey} load fail`, e)
    const msg = e?.message || '网络错误'
    meta.error = /timeout|超时/i.test(msg) ? '请求超时，请重试' : msg
  } finally {
    meta.loading = false
  }
}

function onTabActivate(tabKey) {
  const meta = ensureTabMeta(tabKey)
  if (!meta.loaded) loadTabData(tabKey)
}

// ===== Top-level reload =====
const reloadCounter = ref(0)
function reloadAll() {
  reloadCounter.value++
  loadQuote()
  loadStockInfo()
  fetchKline(klinePeriod.value)
  // 重置所有 VR tab 元数据，激活时重新加载
  for (const k of Object.keys(tabMetas)) {
    tabMetas[k].loaded = false
    tabMetas[k].data = null
    tabMetas[k].error = ''
    tabMetas[k].empty = false
  }
  // 重新加载当前 tab（组件型 tab 通过 :key 变化强制重挂载重载）
  loadTabData(activeTab.value)
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/quant')
}

// ===== Lifecycle =====
// 交易时间自动刷新实时行情（每 30s）
let quoteTimer = null

// 后台预热其余周期 K 线：后端有 5min 内存缓存，预热后切换周期秒回
let klineWarmupDone = false
async function warmupKlinePeriods() {
  if (klineWarmupDone || !sym.value) return
  klineWarmupDone = true
  for (const p of ['weekly', 'monthly', '60m']) {
    try { await getPyKline(sym.value, KLINE_DAYS_MAP[p] || 120, p) } catch (e) { /* 预热失败不影响页面 */ }
  }
}

function isTradeTime() {
  const now = new Date()
  const h = now.getHours(), m = now.getMinutes(), d = now.getDay()
  if (d === 0 || d === 6) return false
  const t = h * 60 + m
  return (t >= 540 && t <= 690) || (t >= 780 && t <= 1140) // 9:00-11:30, 13:00-19:00
}

onMounted(async () => {
  // 行情/基本信息/K线并行加载，单个失败不影响其他
  await Promise.allSettled([loadQuote(), loadStockInfo(), fetchKline('daily')])
  // 窗口尺寸变化时重绘 K 线
  window.addEventListener('resize', handleResize)
  // 交易时间内每 30s 自动刷新实时行情
  quoteTimer = setInterval(() => {
    if (isTradeTime()) loadQuote()
  }, 30000)
  // 首屏渲染完成后，空闲预热周线/月线/60分钟（fire-and-forget）
  warmupKlinePeriods()
})

function handleResize() {
  if (klineChart) klineChart.resize()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (klineChart) { klineChart.dispose(); klineChart = null }
  if (quoteTimer) { clearInterval(quoteTimer); quoteTimer = null }
})

// 切回行情 tab 时若 K 线已渲染，重绘一次
watch(activeTab, (tab) => {
  if (tab === 'quote' && klineChart) {
    nextTick(() => klineChart && klineChart.resize())
  }
})

// 切换副图指标时重算重绘（复用缓存数据，不发请求）
watch(selectedSubs, () => {
  if (lastKlineData.value && klineChart) renderKlineChart()
}, { deep: true })
</script>

<style scoped>
.scroll-card {
  background: linear-gradient(150deg, #0c1422 0%, #0a111d 55%, #0d1726 100%);
  border: 1px solid rgba(34, 211, 238, 0.14);
  border-radius: 12px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.35);
}
:deep(.web3-table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 11px;
  color: #c7d2fe;
}
:deep(.web3-table th) {
  padding: 6px 10px;
  text-align: left;
  font-weight: 600;
  color: #fbbf24;
  border-bottom: 1px solid rgba(120, 170, 220, 0.18);
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
}
:deep(.web3-table td) {
  padding: 6px 10px;
  border-bottom: 1px solid rgba(120, 170, 220, 0.08);
  white-space: nowrap;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
}
:deep(.web3-table tr:hover td) {
  background: rgba(34, 211, 238, 0.04);
}
.scrollbar-hide::-webkit-scrollbar { display: none; }
.scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
.matrix-text {
  font-family: 'Courier New', 'Consolas', monospace;
  letter-spacing: 0.04em;
}
</style>

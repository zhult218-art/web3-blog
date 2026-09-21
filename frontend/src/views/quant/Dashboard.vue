<template>
  <div class="qdash">
    <!-- ══════════ 顶部：返回 + 标题 + 数据源状态 ══════════ -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <button class="text-xs text-gray-500 hover:text-cyan-300 transition flex items-center gap-1" @click="router.back()">
          <span class="inline-block w-0 h-0 border-y-[4px] border-y-transparent border-r-[6px] border-r-current"></span>返回量化
        </button>
        <h1 class="text-sm font-bold tracking-[0.3em] text-cyan-200/80">量化交易监控大屏</h1>
      </div>
      <span class="text-[10px] text-gray-600 font-mono">{{ sourceLabel }} · {{ stamp }}</span>
    </div>

    <!-- ══════════ 1. 顶部通栏指标卡片行 ══════════ -->
    <div class="kpi-row">
      <div v-for="(d, i) in kpiDefs" :key="d.label" class="kpi" :class="{ alarm: d.alarm && d.alarm(account) }">
        <div class="label">{{ d.label }}</div>
        <div class="value" :class="d.cls ? d.cls(account[d.key]) : ''">
          {{ d.fmt(account[d.key]) }}<small v-if="d.unit">{{ d.unit }}</small>
        </div>
        <div class="foot">{{ d.footFmt ? d.footFmt(account) : '' }}</div>
      </div>
    </div>

    <!-- ══════════ 2~5. 主区四象限 ══════════ -->
    <div class="main-grid mt-3">
      <!-- 左列 -->
      <div class="flex flex-col gap-3 min-w-0">
        <!-- 左上：净值对比大图 -->
        <div class="panel">
          <div class="panel-hd"><span class="dot"></span><b>净值曲线 · STRATEGY vs 上证指数</b></div>
          <div ref="navEl" class="h-[300px]"></div>
        </div>
        <!-- 左下：实时持仓表 -->
        <div class="panel">
          <div class="panel-hd"><span class="dot"></span><b>实时持仓 · POSITIONS</b><span class="sub">{{ positions.length }} 只</span></div>
          <div class="max-h-[208px] overflow-y-auto">
            <table class="w-full text-[11px]">
              <thead><tr>
                <th class="text-left">标的代码</th><th>方向</th><th>仓位占比</th><th>成本价</th>
                <th>最新价</th><th>浮盈浮亏</th><th>止损价</th><th>止盈价</th>
              </tr></thead>
              <tbody>
                <tr v-if="!positions.length"><td colspan="8" class="empty">当前空仓</td></tr>
                <!-- 键控 code，Vue diff 只更新变化单元格，不整表重建 -->
                <tr v-for="p in positions" :key="p.code">
                  <td class="text-left"><span class="text-[#8fb6d0]">{{ p.code }}</span><span class="text-[#556677] ml-1.5">{{ p.name }}</span></td>
                  <td><span class="tag" :class="{ short: p.direction === '做空' }">{{ p.direction }}</span></td>
                  <td>{{ p.weight }}%</td>
                  <td>{{ p.costPrice.toFixed(2) }}</td>
                  <td>{{ p.lastPrice.toFixed(2) }}</td>
                  <td :class="p.floatPnl > 0 ? 'up' : p.floatPnl < 0 ? 'down' : ''">
                    {{ p.floatPnl > 0 ? '+' : '' }}{{ fmtMoney(p.floatPnl) }} ({{ p.floatPnlPct > 0 ? '+' : '' }}{{ p.floatPnlPct }}%)
                  </td>
                  <td class="text-[#7d8fa1]">{{ p.stopLoss.toFixed(2) }}</td>
                  <td class="text-[#7d8fa1]">{{ p.takeProfit.toFixed(2) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 右列 -->
      <div class="flex flex-col gap-3 min-w-0">
        <!-- 右上：策略统计两小图 -->
        <div class="flex gap-3">
          <div class="panel flex-1 min-w-0">
            <div class="panel-hd"><span class="dot"></span><b>{{ tradesLabel }}</b></div>
            <div ref="barEl" class="h-[172px]"></div>
          </div>
          <div class="panel flex-1 min-w-0">
            <div class="panel-hd"><span class="dot"></span><b>月度收益占比</b></div>
            <div ref="pieEl" class="h-[172px]"></div>
          </div>
        </div>
        <!-- 右下：交易日志 -->
        <div class="panel">
          <div class="panel-hd"><span class="dot"></span><b>交易日志 · EVENT LOG</b><span class="sub">保留最新 50 条</span></div>
          <div ref="logEl" class="h-[168px] overflow-y-auto bg-black/50 px-3 py-2 text-[11px] leading-[1.9]">
            <div v-for="l in logs" :key="l.seq" class="whitespace-nowrap">
              <span class="t">{{ l.time }}</span>
              <span :class="logCls[l.cls]">[{{ l.tag }}]</span>
              <span :class="logCls[l.cls]">{{ l.msg }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ══════════ 6. 底部通栏：音频可视化波浪 ══════════ -->
    <div class="panel relative mt-3">
      <div class="absolute top-1.5 right-2.5 z-10 flex gap-1.5">
        <button v-for="m in waveModes" :key="m.id" :class="['ctl', { on: waveMode === m.id }]" @click="setWaveMode(m.id)">{{ m.label }}</button>
      </div>
      <canvas ref="waveEl" class="w-full h-[88px] block"></canvas>
      <!-- Audio 标签音源：把 src 换成你的音频地址即可 -->
      <audio ref="audioEl" crossorigin="anonymous"></audio>
    </div>

    <!-- ══════════ 7. 底部看板：涨停池连板分层（duanxianxia 风格） ══════════ -->
    <div class="panel mt-3">
      <div class="panel-hd">
        <span class="dot" style="background:#f87171;box-shadow:0 0 8px #f87171"></span>
        <b>涨停池 · 连板梯队</b>
        <span class="sub">{{ poolBoards.date }} · 共 {{ poolBoards.total }} 只涨停 · 封单 {{ fmtSeal(poolBoards.seal_total) }} · 成交 {{ fmtAmount(poolBoards.amount_total) }}</span>
      </div>
      <div class="board-grid">
        <div v-for="b in poolBoards.boards" :key="b.lbc" class="board-col" :class="{ active: b.count > 0 }">
          <div class="board-hd">
            <span class="board-label">{{ b.label }}</span>
            <span class="board-count">{{ b.count }}</span>
          </div>
          <div class="board-stocks">
            <div v-for="s in b.stocks.slice(0, 20)" :key="s.code" class="stock-chip" :title="`${s.name} ${s.pct}% 封单${fmtSeal(s.seal)}`">
              <span class="sc-name">{{ s.name }}</span>
              <span class="sc-seal">{{ fmtSeal(s.seal) }}</span>
            </div>
            <div v-if="!b.stocks.length" class="board-empty">—</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ══════════ 8. 板块涨跌 + 最新研报 ══════════ -->
    <div class="bottom-grid mt-3">
      <!-- 板块涨跌排行 -->
      <div class="panel">
        <div class="panel-hd"><span class="dot"></span><b>板块涨跌 · 行业热力</b><span class="sub">{{ sectorList.length }} 个行业</span></div>
        <div class="sector-list">
          <div v-for="s in sectorList" :key="s.name || s.label" class="sector-row">
            <span class="sec-name">{{ s.name || s.label }}</span>
            <div class="sec-bar">
              <div class="sec-bar-fill" :class="(s.pct || s.change_percent || 0) >= 0 ? 'up' : 'down'"
                   :style="{ width: Math.min(Math.abs(s.pct || s.change_percent || 0) * 8, 100) + '%' }"></div>
            </div>
            <span class="sec-pct" :class="(s.pct || s.change_percent || 0) >= 0 ? 'up' : 'down'">
              {{ (s.pct || s.change_percent || 0) >= 0 ? '+' : '' }}{{ (s.pct || s.change_percent || 0).toFixed ? (s.pct || s.change_percent || 0).toFixed(2) : (s.pct || s.change_percent || 0) }}%
            </span>
          </div>
          <div v-if="!sectorList.length" class="board-empty" style="padding:24px">暂无板块数据</div>
        </div>
      </div>
      <!-- 最新研报 -->
      <div class="panel">
        <div class="panel-hd"><span class="dot" style="background:#a78bfa;box-shadow:0 0 8px #a78bfa"></span><b>最新券商研报</b><span class="sub">{{ reportList.length }} 篇</span></div>
        <div class="report-list">
          <a v-for="r in reportList" :key="r.report_id" :href="r.url" target="_blank" class="report-item">
            <div class="rep-top">
              <span class="rep-stock">{{ r.stock_name || r.stock_code }}</span>
              <span class="rep-rating" :class="ratingCls(r.rating)">{{ r.rating || '—' }}</span>
              <span class="rep-org">{{ r.org_name }}</span>
            </div>
            <div class="rep-title">{{ r.title }}</div>
            <div class="rep-meta">
              <span v-if="r.target_price">目标价 {{ r.target_price }}</span>
              <span>{{ r.publish_date }}</span>
            </div>
          </a>
          <div v-if="!reportList.length" class="board-empty" style="padding:24px">暂无研报数据</div>
        </div>
      </div>
    </div>

    <!-- ══════════ 9. 财经快讯滚动 ══════════ -->
    <div class="panel mt-3">
      <div class="panel-hd"><span class="dot" style="background:#fbbf24;box-shadow:0 0 8px #fbbf24"></span><b>财经快讯 · 7×24</b><span class="sub">{{ newsList.length }} 条</span></div>
      <div class="news-list">
        <div v-for="n in newsList" :key="n.id" class="news-item">
          <span class="news-time">{{ n.timestamp }}</span>
          <span class="news-tag" :class="'senti-' + (n.sentiment || 'neutral')">{{ n.category }}</span>
          <span class="news-title">{{ n.title }}</span>
        </div>
        <div v-if="!newsList.length" class="board-empty" style="padding:24px">暂无快讯</div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 量化交易监控大屏（Vue 版）
// 数据约定与单文件版一致：渲染层零业务计算，全部数值由
// MockEngine（模拟"后端"）或 FastAPI 接口快照提供。
// 接真实后端：把 CONFIG.DATA_SOURCE 改为 'api' 即可，前端零改动。
// ============================================================
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getPyLimitPoolBoards, getPySectors, getPyReportsLatest, getPyNews } from '@/api/quant'

const router = useRouter()

/* ═══【一】配置常量区 —— 二次修改只动这里 ═══ */
const CONFIG = {
  DATA_SOURCE: 'api',                           // 'mock' | 'api'（api = quant-py-service 真实 A 股行情）
  API_URL: '/pyquant/api/quant/dashboard/snapshot',
  POLL_INTERVAL_MS: 1500,
  LOG_KEEP: 50,
  ALARM: { dayPnlPct: 2.0, maxDrawdownPct: 8.0 },
}

/* ═══【二】模拟数据引擎（扮演"后端"，指标全部算好放快照） ═══ */
const MockEngine = (() => {
  let equity = 1000000
  const dayOpenEquity = equity
  let logSeq = 0
  // 净值序列：首帧先随机游走出 120 天历史
  const navSeries = []
  {
    let s = 1, b = 1
    const today = Date.now()
    for (let i = 119; i >= 0; i--) {
      s = +(s * (1 + (Math.random() * 0.0085 - 0.004))).toFixed(4)
      b = +(b * (1 + (Math.random() * 0.0042 - 0.002))).toFixed(4)
      navSeries.push({ date: new Date(today - i * 86400000).toISOString().slice(0, 10), strategy: s, benchmark: b })
    }
  }
  const rnd = (a, b) => a + Math.random() * (b - a)
  const now = () => new Date().toTimeString().slice(0, 8)

  // 持仓池（多/空混合），价格动态微调
  const posState = [
    { code: '510300.SH', name: '沪深300ETF', dir: 'long' },
    { code: '600519.SH', name: '贵州茅台', dir: 'long' },
    { code: '300750.SZ', name: '宁德时代', dir: 'long' },
    { code: 'IF2603.CFE', name: 'IF股指期货', dir: 'short' },
    { code: '159915.SZ', name: '创业板ETF', dir: 'short' },
  ].map(p => {
    const cost = +(8 + Math.random() * 300).toFixed(2)
    return { ...p, weight: 8 + Math.random() * 14, cost, last: +(cost * (0.97 + Math.random() * 0.07)).toFixed(2) }
  })

  const trades = []
  while (trades.length < 30) trades.push(+rnd(-2200, 3600).toFixed(0))
  const monthly = [
    { month: '2026-04', profit: 3.2 }, { month: '2026-05', profit: -1.4 },
    { month: '2026-06', profit: 5.8 }, { month: '2026-07', profit: 2.1 },
    { month: '2026-08', profit: -0.7 }, { month: '2026-09', profit: 4.6 },
  ]

  function makeLog() {
    const pool = [
      ['open', '开仓信号', () => `多头开仓 510300.SH @ ${rnd(3.8, 4.1).toFixed(3)} 信号源:MA金叉`],
      ['open', '开仓信号', () => `空头开仓 IF2603.CFE @ ${rnd(3880, 3960).toFixed(1)} 信号源:跌破布林下轨`],
      ['close', '平仓信号', () => `平仓 600519.SH 获利了结 浮盈 +${rnd(0.4, 3.2).toFixed(2)}%`],
      ['stop', '止损触发', () => `止损触发 300750.SZ @ ${rnd(168, 176).toFixed(2)} 账户风控截断`],
      ['take', '止盈触发', () => `止盈触发 159915.SZ @ ${rnd(2.1, 2.4).toFixed(3)} 到达目标位`],
      ['warn', '仓位告警', () => `单标的仓位 ${rnd(16, 22).toFixed(1)}% 超过阈值 15%`],
      ['info', '心跳', () => `策略心跳正常 延迟 ${rnd(8, 46).toFixed(0)}ms 行情源:CTP`],
      ['error', '异常检测', () => `行情快照时间戳漂移 ${rnd(120, 900).toFixed(0)}ms 已自动重连`],
    ]
    const [cls, tag, gen] = pool[Math.floor(Math.random() * pool.length)]
    return { seq: ++logSeq, time: now(), cls, tag, msg: gen() }
  }

  function buildSnapshot(newLogs) {
    const dayPnl = +(equity - dayOpenEquity).toFixed(2)
    const dayPnlPct = +(dayPnl / dayOpenEquity * 100).toFixed(2)
    const first = navSeries[0].strategy
    return {
      account: {
        totalEquity: equity, dayPnl, dayPnlPct,
        totalReturnPct: +((navSeries[navSeries.length - 1].strategy / first - 1) * 100).toFixed(2),
        winRate: +rnd(52, 68).toFixed(1),
        sharpe: +rnd(1.2, 2.6).toFixed(2),
        maxDrawdown: +rnd(3, 9).toFixed(2),
        positionCount: posState.length,
        availableCash: +(equity * rnd(0.12, 0.3)).toFixed(2),
      },
      nav: navSeries.slice(-120),
      positions: posState.map(p => {
        const last = +(p.last * (1 + rnd(-0.003, 0.0032))).toFixed(p.cost > 100 ? 2 : 3)
        const sign = p.dir === 'long' ? 1 : -1
        return {
          code: p.code, name: p.name, direction: p.dir === 'long' ? '做多' : '做空',
          weight: +p.weight.toFixed(1), costPrice: p.cost, lastPrice: last,
          floatPnl: +((last - p.cost) * 1000 * sign).toFixed(0),
          floatPnlPct: +((last - p.cost) / p.cost * 100 * sign).toFixed(2),
          stopLoss: +(p.cost * 0.94).toFixed(2), takeProfit: +(p.cost * 1.16).toFixed(2),
        }
      }),
      trades: (Math.random() < 0.35 ? (trades.shift(), trades.push(+rnd(-2200, 3600).toFixed(0)), trades) : trades).slice(-30),
      monthly,
      logs: newLogs,
      stamp: now(),
    }
  }

  function tick() {
    const last = navSeries[navSeries.length - 1]
    const date = new Date().toISOString().slice(0, 10)
    const s = +(last.strategy * (1 + rnd(-0.004, 0.0045))).toFixed(4)
    const b = +(last.benchmark * (1 + rnd(-0.002, 0.0022))).toFixed(4)
    equity = +(equity * (1 + rnd(-0.0025, 0.0028))).toFixed(2)
    if (last.date !== date) navSeries.push({ date, strategy: s, benchmark: b })
    else navSeries[navSeries.length - 1] = { date, strategy: s, benchmark: b }
    if (navSeries.length > 160) navSeries.shift()
    return buildSnapshot([makeLog()])
  }
  const seedLogs = () => Array.from({ length: 12 }, makeLog)
  return { tick, seedLogs }
})()

/* ═══【三】数据获取层 ═══ */
let lastLogSeq = 0
async function getSnapshot() {
  if (CONFIG.DATA_SOURCE === 'api') {
    try {
      const res = await fetch(CONFIG.API_URL, { cache: 'no-store' })
      const json = await res.json()
      return json.data ? json.data : json
    } catch (e) { console.warn('[quant] 拉取后端失败，回落模拟数据：', e.message) }
  }
  return MockEngine.tick()
}

/* ═══【四】渲染状态 ═══ */
const fmtMoney = v => Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const account = reactive({
  totalEquity: 0, dayPnl: 0, dayPnlPct: 0, totalReturnPct: 0,
  winRate: 0, sharpe: 0, maxDrawdown: 0, positionCount: 0, availableCash: 0,
})
const positions = ref([])
const logs = ref([])
const stamp = ref('')
const sourceLabel = computed(() => CONFIG.DATA_SOURCE === 'api' ? 'LIVE · A股实时' : '模拟数据')
const tradesLabel = ref('单笔盈亏 · 近30笔')

// KPI 卡片配置（阈值告警在渲染层判断，数值全部来自快照）
const kpiDefs = [
  { key: 'totalEquity', label: '账户总净值', fmt: fmtMoney },
  { key: 'dayPnl', label: '当日盈亏', fmt: v => (v > 0 ? '+' : '') + fmtMoney(v),
    cls: v => v > 0 ? 'up' : v < 0 ? 'down' : '', footFmt: a => (a.dayPnlPct > 0 ? '+' : '') + a.dayPnlPct + '%',
    alarm: a => Math.abs(a.dayPnlPct) >= CONFIG.ALARM.dayPnlPct },
  { key: 'totalReturnPct', label: '累计收益率', unit: '%', fmt: v => (v > 0 ? '+' : '') + v.toFixed(2), cls: v => v > 0 ? 'up' : v < 0 ? 'down' : '' },
  { key: 'winRate', label: '策略胜率', unit: '%', fmt: v => v.toFixed(1) },
  { key: 'sharpe', label: '夏普比率', fmt: v => v.toFixed(2) },
  { key: 'maxDrawdown', label: '最大回撤', unit: '%', fmt: v => '-' + v.toFixed(2),
    cls: () => '', alarm: a => a.maxDrawdown >= CONFIG.ALARM.maxDrawdownPct },
  { key: 'positionCount', label: '当前持仓数量', unit: '只', fmt: v => String(v) },
  { key: 'availableCash', label: '可用资金', fmt: fmtMoney },
]

const logCls = { info: 'lg-info', open: 'lg-open', close: 'lg-close', stop: 'lg-stop', take: 'lg-take', warn: 'lg-warn', error: 'lg-error' }

/* —— ECharts —— */
const navEl = ref(null), barEl = ref(null), pieEl = ref(null), logEl = ref(null)
let navChart, barChart, pieChart

function initCharts() {
  navChart = echarts.init(navEl.value)
  navChart.setOption({
    animationDuration: 600,
    legend: { top: 6, right: 10, textStyle: { color: '#6f8aa0', fontSize: 10 }, itemWidth: 14, itemHeight: 2, icon: 'rect' },
    grid: { left: 58, right: 16, top: 34, bottom: 24 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(8,14,20,.92)', borderColor: 'rgba(56,189,248,.25)', textStyle: { color: '#b8cadb', fontSize: 11 } },
    xAxis: { type: 'category', data: [], axisLine: { lineStyle: { color: 'rgba(56,189,248,.2)' } }, axisLabel: { color: '#4d6478', fontSize: 9 }, axisTick: { show: false } },
    yAxis: { type: 'value', scale: true, splitLine: { lineStyle: { color: 'rgba(255,255,255,.04)' } }, axisLabel: { color: '#4d6478', fontSize: 9 } },
    series: [
      { name: '策略净值', type: 'line', showSymbol: false, smooth: true, data: [],
        lineStyle: { width: 2, color: '#22d3ee', shadowColor: 'rgba(34,211,238,.5)', shadowBlur: 8 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(34,211,238,.16)' }, { offset: 1, color: 'rgba(34,211,238,0)' }]) } },
      { name: '上证指数', type: 'line', showSymbol: false, smooth: true, data: [], lineStyle: { width: 1.2, color: '#64748b', type: 'dashed' } },
    ],
  })
  barChart = echarts.init(barEl.value)
  barChart.setOption({
    grid: { left: 44, right: 8, top: 14, bottom: 20 },
    tooltip: { backgroundColor: 'rgba(8,14,20,.92)', borderColor: 'rgba(56,189,248,.25)', textStyle: { color: '#b8cadb', fontSize: 10 } },
    xAxis: { type: 'category', data: [], axisLabel: { show: false }, axisLine: { lineStyle: { color: 'rgba(56,189,248,.2)' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,.04)' } }, axisLabel: { color: '#4d6478', fontSize: 9 } },
    series: [{ type: 'bar', data: [], barWidth: '62%', itemStyle: { borderRadius: 2, color: p => p.value >= 0 ? 'rgba(52,211,153,.85)' : 'rgba(248,113,113,.85)' } }],
  })
  pieChart = echarts.init(pieEl.value)
  pieChart.setOption({
    tooltip: { backgroundColor: 'rgba(8,14,20,.92)', borderColor: 'rgba(56,189,248,.25)', textStyle: { color: '#b8cadb', fontSize: 10 } },
    legend: { bottom: 0, textStyle: { color: '#4d6478', fontSize: 9 }, itemWidth: 8, itemHeight: 8 },
    series: [{ type: 'pie', radius: ['46%', '70%'], center: ['50%', '44%'], label: { show: false }, data: [],
      itemStyle: { borderColor: '#0b0d12', borderWidth: 2 }, color: ['#22d3ee', '#34d399', '#3b82f6', '#a78bfa', '#f0abfc', '#64748b'] }],
  })
}

function renderAll(snap) {
  Object.assign(account, snap.account)
  positions.value = snap.positions
  stamp.value = snap.stamp || ''
  if (snap.tradesLabel) tradesLabel.value = snap.tradesLabel
  navChart.setOption({
    xAxis: { data: snap.nav.map(x => x.date.slice(5)) },
    series: [{ data: snap.nav.map(x => x.strategy) }, { data: snap.nav.map(x => x.benchmark) }],
  })
  barChart.setOption({ xAxis: { data: snap.trades.map((_, i) => i + 1) }, series: [{ data: snap.trades }] })
  pieChart.setOption({ series: [{ data: snap.monthly.map(m => ({ name: m.month.slice(2), value: Math.abs(m.profit) })) }] })
  // 日志增量追加（按 seq 去重）+ 自动滚底 + 保留 50 条
  let changed = false
  for (const l of snap.logs || []) {
    if (l.seq <= lastLogSeq) continue
    lastLogSeq = l.seq; logs.value.push(l); changed = true
  }
  if (changed) {
    if (logs.value.length > CONFIG.LOG_KEEP) logs.value = logs.value.slice(-CONFIG.LOG_KEEP)
    nextTick(() => { const el = logEl.value; if (el) el.scrollTop = el.scrollHeight })
  }
}

/* ═══【五】底部看板数据（涨停池/板块/研报/快讯，30s 轮询） ═══ */
const poolBoards = ref({ date: '', total: 0, seal_total: 0, amount_total: 0, boards: [] })
const sectorList = ref([])
const reportList = ref([])
const newsList = ref([])
const boardLoading = ref(false)

async function fetchBottomData() {
  if (boardLoading.value) return
  boardLoading.value = true
  try {
    const [pb, sec, rep, nw] = await Promise.all([
      getPyLimitPoolBoards().catch(() => ({ data: { boards: [] } })),
      getPySectors().catch(() => ({ data: [] })),
      getPyReportsLatest(8).catch(() => ({ data: [] })),
      getPyNews(12).catch(() => ({ data: [] })),
    ])
    if (pb?.data?.boards) poolBoards.value = pb.data
    // 板块接口可能返回 dict {name: pct} 或 array，统一转成 [{name, pct}] 并排序
    const sd = sec?.data
    if (sd) {
      let arr = []
      if (Array.isArray(sd)) {
        arr = sd.map(s => ({ name: s.name || s.label, pct: Number(s.pct ?? s.change_percent ?? 0) }))
      } else if (typeof sd === 'object') {
        arr = Object.entries(sd).map(([name, pct]) => ({ name, pct: Number(pct) }))
      }
      sectorList.value = arr.sort((a, b) => b.pct - a.pct).slice(0, 12)
    }
    if (Array.isArray(rep?.data)) reportList.value = rep.data
    if (Array.isArray(nw?.data)) newsList.value = nw.data
  } catch (e) { /* 静默 */ }
  finally { boardLoading.value = false }
}
const fmtSeal = v => {
  if (!v) return '0'
  const yi = v / 1e8
  if (yi >= 1) return yi.toFixed(2) + '亿'
  const wan = v / 1e4
  return wan.toFixed(0) + '万'
}
const fmtAmount = v => {
  if (!v) return '0'
  const yi = v / 1e8
  if (yi >= 1) return yi.toFixed(1) + '亿'
  return (v / 1e4).toFixed(0) + '万'
}
const ratingCls = r => {
  if (!r) return ''
  if (/买入|增持|推荐|强烈/.test(r)) return 'r-buy'
  if (/卖出|减持|回避/.test(r)) return 'r-sell'
  if (/中性|持有|同步/.test(r)) return 'r-hold'
  return ''
}

/* ═══【六】音频可视化波浪（Web Audio + Canvas，逻辑同单文件版） ═══ */
const waveEl = ref(null), audioEl = ref(null)
const waveModes = [
  { id: 'demo', label: '演示波形' },
  { id: 'mic', label: '麦克风' },
  { id: 'audio', label: 'Audio 标签' },
]
const waveMode = ref('demo')
let waveCtx, waveCanvas, actx, analyser, freq, smoothed = [], nodes = [], stream = null, srcEl = null, rafId = 0

function teardownWave() {
  if (stream) { stream.getTracks().forEach(t => t.stop()); stream = null }
  if (srcEl) { try { srcEl.disconnect() } catch (e) {} srcEl = null }
  nodes.forEach(n => { try { n.stop && n.stop(); n.disconnect && n.disconnect() } catch (e) {} })
  nodes = []
}
function initAudioCtx() {
  if (actx) return
  actx = new (window.AudioContext || window.webkitAudioContext)()
  analyser = actx.createAnalyser()
  analyser.fftSize = 512
  analyser.smoothingTimeConstant = 0.82
  freq = new Uint8Array(analyser.frequencyBinCount)
}
// 演示音源：三个失谐振荡器 + 低频增益调制（静音，仅喂分析器）
function startDemoOsc() {
  initAudioCtx(); teardownWave()
  const g = actx.createGain(); g.gain.value = 0.5; g.connect(analyser); nodes.push(g)
  ;[110, 220, 331].forEach((f, i) => {
    const o = actx.createOscillator(), og = actx.createGain()
    o.frequency.value = f; og.gain.value = 0.16 - i * 0.04
    const lfo = actx.createOscillator(), lg = actx.createGain()
    lfo.frequency.value = 0.07 + i * 0.05; lg.gain.value = 0.1
    lfo.connect(lg); lg.connect(og.gain); o.connect(og); og.connect(g)
    o.start(); lfo.start(); nodes.push(o, og, lfo, lg)
  })
}
function setWaveMode(m) {
  waveMode.value = m
  teardownWave()
  if (m === 'demo') startDemoOsc()
  else if (m === 'mic') {
    initAudioCtx()
    navigator.mediaDevices.getUserMedia({ audio: true })
      .then(s => { stream = s; srcEl = actx.createMediaStreamSource(s); srcEl.connect(analyser) })
      .catch(() => setWaveMode('demo'))   // 权限被拒自动回落
  } else if (m === 'audio') {
    initAudioCtx()
    const audio = audioEl.value
    if (!audio.src) { startDemoOsc() }    // 未配置音频地址时回落演示振荡器
    else { srcEl = actx.createMediaElementSource(audio); srcEl.connect(analyser); srcEl.connect(actx.destination); audio.play().catch(() => {}) }
  }
}
function waveLoop() {
  const canvas = waveCanvas, ctx = waveCtx
  if (!canvas || !ctx) return
  const W = canvas.width, H = canvas.height, mid = H / 2
  ctx.clearRect(0, 0, W, H)
  // 取波形：demo 合成全宽平滑曲线；实音源走对数频率映射
  const N = 96
  let raw = new Array(N).fill(0)
  if (waveMode.value === 'demo' || !analyser) {
    const t = performance.now() / 1000
    for (let i = 0; i < N; i++) {
      const k = i / N
      raw[i] = Math.max(0, Math.min(1,
        0.32 + 0.22 * Math.sin(t * 1.1 + k * 9.4)
            + 0.18 * Math.sin(t * 2.3 + k * 21.0 + 1.7)
            + 0.14 * Math.sin(t * 0.6 + k * 33.0 + 4.2)
            + (Math.random() - 0.5) * 0.08))
    }
  } else {
    analyser.getByteFrequencyData(freq)
    for (let i = 0; i < N; i++) raw[i] = freq[Math.floor(Math.pow(i / N, 1.8) * freq.length * 0.8)] / 255
  }
  if (!smoothed.length) smoothed = raw.slice()
  for (let i = 0; i < N; i++) smoothed[i] += (raw[i] - smoothed[i]) * 0.25
  // 渐变 蓝→青→绿→粉紫；上下镜像对称填充 + 柔光
  const grad = ctx.createLinearGradient(0, 0, W, 0)
  grad.addColorStop(0, 'rgba(59,130,246,.75)'); grad.addColorStop(0.33, 'rgba(34,211,238,.8)')
  grad.addColorStop(0.66, 'rgba(52,211,153,.8)'); grad.addColorStop(1, 'rgba(232,121,249,.8)')
  ctx.beginPath(); ctx.moveTo(0, mid)
  for (let i = 0; i < N; i++) ctx.lineTo(i / (N - 1) * W, mid - smoothed[i] * mid * 0.92)
  for (let i = N - 1; i >= 0; i--) ctx.lineTo(i / (N - 1) * W, mid + smoothed[i] * mid * 0.92)
  ctx.closePath()
  ctx.fillStyle = grad; ctx.shadowColor = 'rgba(34,211,238,.35)'; ctx.shadowBlur = 14
  ctx.globalAlpha = 0.8; ctx.fill()
  ctx.shadowBlur = 0; ctx.globalAlpha = 0.35
  ctx.strokeStyle = grad; ctx.lineWidth = 1; ctx.beginPath(); ctx.moveTo(0, mid); ctx.lineTo(W, mid); ctx.stroke()
  ctx.globalAlpha = 1
  rafId = requestAnimationFrame(waveLoop)
}
function fitWave() {
  if (!waveCanvas) return
  waveCanvas.width = waveCanvas.clientWidth * devicePixelRatio
  waveCanvas.height = waveCanvas.clientHeight * devicePixelRatio
}

/* ═══【六】生命周期 ═══ */
let timer = 0, fetching = false, bottomTimer = 0
const onResize = () => { navChart?.resize(); barChart?.resize(); pieChart?.resize(); fitWave() }

onMounted(async () => {
  if (CONFIG.DATA_SOURCE === 'mock') for (const l of MockEngine.seedLogs()) logs.value.push(l)
  await nextTick()
  initCharts()
  renderAll(await getSnapshot())
  // fetching 守卫：上一轮请求未返回时跳过本轮，防止慢接口下轮询堆积
  timer = setInterval(async () => {
    if (document.hidden || fetching) return
    fetching = true
    try { renderAll(await getSnapshot()) } finally { fetching = false }
  }, CONFIG.POLL_INTERVAL_MS)
  // 底部看板数据（涨停池/板块/研报/快讯）30s 轮询
  fetchBottomData()
  bottomTimer = setInterval(() => { if (!document.hidden) fetchBottomData() }, 30000)
  waveCanvas = waveEl.value; waveCtx = waveCanvas.getContext('2d')
  fitWave(); setWaveMode('demo'); waveLoop()
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  clearInterval(timer); clearInterval(bottomTimer); cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
  teardownWave(); actx?.close().catch(() => {})
  navChart?.dispose(); barChart?.dispose(); pieChart?.dispose()
})
</script>

<style scoped>
/* ─── 深色金融大屏：纯黑底 + 低饱和青蓝辉光 + 等宽字体 ─── */
.qdash {
  --cyan: #22d3ee; --green: #34d399; --red: #f87171;
  --line: rgba(56, 189, 248, .14);
  min-height: 100vh;
  padding: 16px;
  background:
    radial-gradient(ellipse 70% 30% at 50% -5%, rgba(34, 211, 238, .06), transparent 60%),
    #05070b;
  color: #cbd5e1;
  font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, monospace;
  font-size: 12px;
}
/* ─── 面板 ─── */
.panel {
  background: rgba(10, 16, 24, .72);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: 0 0 22px rgba(34, 211, 238, .05), inset 0 0 30px rgba(34, 211, 238, .015);
  overflow: hidden;
}
.panel-hd {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-bottom: 1px solid rgba(56, 189, 248, .08);
}
.panel-hd .dot { width: 6px; height: 6px; border-radius: 50%; background: var(--cyan); box-shadow: 0 0 8px var(--cyan); }
.panel-hd b { color: #a8c7dc; font-size: 12px; letter-spacing: 2px; font-weight: 600; }
.panel-hd .sub { margin-left: auto; color: #556677; font-size: 10px; }

/* ─── KPI 卡片行 ─── */
.kpi-row { display: grid; grid-template-columns: repeat(8, 1fr); gap: 10px; }
.kpi {
  background: rgba(10, 16, 24, .72); border: 1px solid var(--line); border-radius: 10px;
  padding: 10px 12px; min-width: 0; box-shadow: 0 0 18px rgba(34, 211, 238, .05);
}
.kpi .label { color: #556677; font-size: 10px; letter-spacing: 1px; white-space: nowrap; }
.kpi .value { margin-top: 5px; font-size: 19px; font-weight: 700; color: #e2edf4; white-space: nowrap; }
.kpi .value small { font-size: 11px; color: #556677; font-weight: 400; margin-left: 2px; }
.kpi .foot { margin-top: 3px; font-size: 10px; color: #556677; white-space: nowrap; min-height: 13px; }
.kpi .value.up { color: var(--green); text-shadow: 0 0 12px rgba(52, 211, 153, .35); }
.kpi .value.down { color: var(--red); text-shadow: 0 0 12px rgba(248, 113, 113, .35); }
@keyframes alarmBlink {
  0%, 100% { border-color: rgba(248, 113, 113, .85); box-shadow: 0 0 20px rgba(248, 113, 113, .28); }
  50% { border-color: rgba(248, 113, 113, .18); box-shadow: none; }
}
.kpi.alarm { animation: alarmBlink .85s infinite; }

/* ─── 布局 ─── */
.main-grid { display: grid; grid-template-columns: 58fr 42fr; }
@media (max-width: 1100px) { .kpi-row { grid-template-columns: repeat(4, 1fr); } .main-grid { grid-template-columns: 1fr; } }
@media (max-width: 560px) { .kpi-row { grid-template-columns: repeat(2, 1fr); } }

/* ─── 持仓表 ─── */
table th {
  position: sticky; top: 0; z-index: 2; background: #070c12;
  color: #6f8aa0; font-weight: 500; text-align: right;
  padding: 7px 10px; border-bottom: 1px solid rgba(56, 189, 248, .12); white-space: nowrap;
}
table td {
  padding: 7px 10px; text-align: right; border-bottom: 1px solid rgba(255, 255, 255, .03);
  color: #b8cadb; white-space: nowrap;
}
tbody tr:hover td { background: rgba(34, 211, 238, .035); }
td.up, .up { color: var(--green); }
td.down, .down { color: var(--red); }
.tag {
  display: inline-block; padding: 1px 7px; border-radius: 4px; font-size: 10px;
  border: 1px solid rgba(52, 211, 153, .35); color: var(--green);
}
.tag.short { border-color: rgba(248, 113, 113, .35); color: var(--red); }
.empty { text-align: center; color: #556677; padding: 26px 0; }

/* ─── 日志配色 ─── */
.t { color: #3d5568; margin-right: 8px; }
.lg-info { color: #c9d6e2; }
.lg-open { color: var(--green); }
.lg-close { color: #7d8fa1; }
.lg-stop { color: var(--red); text-shadow: 0 0 8px rgba(248, 113, 113, .25); }
.lg-take { color: var(--cyan); }
.lg-warn { color: #fbbf24; }
.lg-error { color: #c084fc; }
.lg-info, .lg-open, .lg-close, .lg-stop, .lg-take, .lg-warn, .lg-error { margin-right: 8px; }

/* ─── 波浪控制按钮 ─── */
.ctl {
  font-family: inherit; font-size: 10px; letter-spacing: 1px;
  color: #7f97ab; background: rgba(10, 16, 24, .85); cursor: pointer;
  border: 1px solid rgba(56, 189, 248, .22); border-radius: 5px; padding: 3px 9px;
  transition: all .25s;
}
.ctl:hover { color: var(--cyan); border-color: rgba(34, 211, 238, .5); }
.ctl.on { color: #06202a; background: var(--cyan); border-color: var(--cyan); box-shadow: 0 0 12px rgba(34, 211, 238, .4); }

::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-thumb { background: rgba(34, 211, 238, .18); border-radius: 3px; }

/* ─── 底部看板：涨停池连板分层 ─── */
.board-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  padding: 10px;
}
.board-col {
  background: rgba(8, 14, 22, .6);
  border: 1px solid rgba(56, 189, 248, .08);
  border-radius: 8px;
  min-height: 120px;
  display: flex;
  flex-direction: column;
}
.board-col.active {
  border-color: rgba(248, 113, 113, .35);
  background: rgba(248, 113, 113, .03);
}
.board-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px;
  border-bottom: 1px solid rgba(248, 113, 113, .12);
}
.board-label { color: #e8a7a7; font-size: 11px; font-weight: 600; letter-spacing: 1px; }
.board-count {
  color: #f87171; font-size: 15px; font-weight: 700;
  text-shadow: 0 0 8px rgba(248, 113, 113, .4);
}
.board-stocks {
  padding: 6px 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  flex: 1;
  align-content: flex-start;
  max-height: 320px;
  overflow-y: auto;
}
.stock-chip {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  background: rgba(248, 113, 113, .08);
  border: 1px solid rgba(248, 113, 113, .18);
  border-radius: 5px;
  padding: 3px 6px;
  line-height: 1.3;
  min-width: 44px;
}
.sc-name { color: #f0b4b4; font-size: 10px; font-weight: 500; }
.sc-seal { color: #f87171; font-size: 9px; opacity: .8; }
.board-empty { color: #3a4a5a; font-size: 11px; text-align: center; padding: 16px 0; width: 100%; }
@media (max-width: 900px) { .board-grid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 560px) { .board-grid { grid-template-columns: repeat(2, 1fr); } }

/* ─── 底部双列：板块 + 研报 ─── */
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
@media (max-width: 900px) { .bottom-grid { grid-template-columns: 1fr; } }

.sector-list { padding: 8px 12px; max-height: 360px; overflow-y: auto; }
.sector-row {
  display: grid;
  grid-template-columns: 80px 1fr 60px;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  border-bottom: 1px solid rgba(255, 255, 255, .03);
}
.sec-name { color: #9fb4c4; font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sec-bar { height: 14px; background: rgba(255, 255, 255, .03); border-radius: 3px; overflow: hidden; }
.sec-bar-fill { height: 100%; border-radius: 3px; transition: width .4s; }
.sec-bar-fill.up { background: linear-gradient(90deg, rgba(248, 113, 113, .3), rgba(248, 113, 113, .85)); }
.sec-bar-fill.down { background: linear-gradient(90deg, rgba(52, 211, 153, .3), rgba(52, 211, 153, .85)); }
.sec-pct { text-align: right; font-size: 11px; font-weight: 600; font-family: inherit; }
.sec-pct.up { color: var(--red); }
.sec-pct.down { color: var(--green); }

.report-list { padding: 8px 12px; max-height: 360px; overflow-y: auto; }
.report-item {
  display: block;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, .03);
  text-decoration: none;
  cursor: pointer;
}
.report-item:hover { background: rgba(167, 139, 250, .04); }
.rep-top { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; }
.rep-stock { color: #c4b5fd; font-size: 11px; font-weight: 600; }
.rep-rating {
  font-size: 10px; padding: 1px 6px; border-radius: 3px;
  border: 1px solid currentColor;
}
.rep-rating.r-buy { color: #f87171; }
.rep-rating.r-sell { color: #34d399; }
.rep-rating.r-hold { color: #60a5fa; }
.rep-org { color: #556677; font-size: 10px; margin-left: auto; }
.rep-title { color: #b8cadb; font-size: 11px; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.rep-meta { display: flex; justify-content: space-between; margin-top: 3px; color: #4d6478; font-size: 10px; }

/* ─── 财经快讯 ─── */
.news-list { padding: 8px 12px; max-height: 300px; overflow-y: auto; }
.news-item {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 5px 0;
  border-bottom: 1px solid rgba(255, 255, 255, .025);
}
.news-time { color: #4d6478; font-size: 10px; font-family: inherit; white-space: nowrap; }
.news-tag {
  font-size: 9px; padding: 1px 5px; border-radius: 3px; white-space: nowrap;
}
.news-tag.senti-positive { color: #f87171; background: rgba(248, 113, 113, .1); }
.news-tag.senti-negative { color: #34d399; background: rgba(52, 211, 153, .1); }
.news-tag.senti-neutral, .news-tag { color: #60a5fa; background: rgba(96, 165, 250, .1); }
.news-title { color: #b8cadb; font-size: 11px; line-height: 1.4; flex: 1; }
</style>

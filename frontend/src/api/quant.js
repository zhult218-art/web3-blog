// ============================================================
// 量化交易接口（quant-service，走网关 /api）
// 包含两部分接口：
// 1. quant-service：策略管理 / 股票基础接口
// 2. quant-py-service（py）：经 /pyquant 前缀直连 9006 端口的统一行情网关
//    （原 Vibe-Research 8900 直连的 /vr 通道已废弃，A 股扩展数据统一由 py-service 提供）
// ============================================================
import request from './request'

// quant-service strategies
// 股票列表/看板首次调用会触发 Java 后端拉取 Python 全市场数据（冷启动可达 30s+），超时放宽到 60s
// 策略列表冷启动也可能慢（Java → quant-service 首次初始化），同样放宽
// 临时：trae-preview 环境下 axios XHR 对 /api/strategy/list 会异常挂起（原生 fetch 正常），
// 先用原生 fetch 绕过，待排查 axios 代理层问题后回退
// 必须带超时且覆盖到响应体读完：fetch 在收到响应头即 resolve，
// 若提前清掉定时器，body 卡住会让 res.json() 永久挂起并占用连接池槽位
async function fetchOnce(url, timeout) {
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), timeout)
  try {
    const res = await fetch(url, { signal: ctrl.signal })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    return await res.json()
  } finally {
    clearTimeout(timer)
  }
}

// 在途请求去重：tab 切换时 onTabClick 与 watch(activeTab) 会同时触发同一个列表请求，
// 共享同一次 fetch 可避免重复请求与随之而来的取消噪音（net::ERR_ABORTED）
const _inflight = new Map()

export async function fetchJson(url, timeout = 15000) {
  if (_inflight.has(url)) return _inflight.get(url)
  const p = (async () => {
    try {
      return await fetchOnce(url, timeout)
    } catch (e) {
      // 超时/被取消属于可恢复抖动（后端冷启动、上游限流）：放宽超时重试一次再放弃
      if (e && (e.name === 'AbortError' || e.name === 'TimeoutError')) {
        return await fetchOnce(url, Math.max(timeout * 2, 30000))
      }
      throw e
    } finally {
      _inflight.delete(url)
    }
  })()
  _inflight.set(url, p)
  return p
}
export const getQuantList = params => {
  const qs = new URLSearchParams(params).toString()
  return fetchJson(`/api/strategy/list?${qs}`)
}
export const createStrategy = data => request.post('/strategy', data)
export const updateStrategy = (id, data) => request.put(`/strategy/${id}`, data)
export const deleteStrategy = id => request.delete(`/strategy/${id}`)
// 运行策略：suppressError 关闭拦截器的全局 toast，由调用方按业务语义给出
// 精确提示（如「公共策略仅管理员可操作」），避免与拦截器 toast 重复弹出
export const runStrategy = id => request.post(`/strategy/${id}/run`, null, { suppressError: true })
export const getQuantLog = params => request.get('/quant/logs', { params })
export const getStockList = () => request.get('/stock/list', { timeout: 60000 })
export const getStockDetail = symbol => request.get(`/stock/${symbol}/detail`, { timeout: 60000 })
export const getStockKline = (symbol, params) => request.get(`/stock/${symbol}/kline`, { params, timeout: 60000 })
export const getStockDashboard = () => request.get('/stock/dashboard', { timeout: 60000 })

// A 股扩展数据（统一走 py-service /pyquant，替代原 Vibe-Research 8900 直连）
// 估值/公告/限售解禁/概念板块走 data-ext 模块，行情/财务/资金等走 market 模块
export const getStockQuote = codes => py('/api/quant/data-ext/tencent/quote', { params: { codes: Array.isArray(codes) ? codes.join(',') : codes } })
// K 线（py 统一网关）。入参同时兼容旧 Vibe-Research 周期名与 py 周期名：
// day/week/month/min60... → daily/weekly/monthly/60m...
export const getStockKlineVR = (code, period = 'day') => {
  const p = {
    day: 'daily', daily: 'daily', week: 'weekly', weekly: 'weekly',
    month: 'monthly', monthly: 'monthly',
    min60: '60m', '60m': '60m', min30: '30m', '30m': '30m',
    min15: '15m', '15m': '15m', min5: '5m', '5m': '5m', min1: '1m', '1m': '1m',
  }[period] || 'daily'
  return py(`/api/quant/market/kline/${code}`, { params: { days: 120, period: p } })
}
export const getMarketTurnoverTop = (limit = 30) => py('/api/quant/market/quotes', { params: { limit } })
export const getStockFinance = code => py(`/api/quant/market/finance/${code}`)
export const getStockValuation = code => py('/api/quant/data-ext/valuation/full', { params: { code } })
export const getStockDividend = code => py(`/api/quant/market/dividend/${code}`)
export const getStockBlockTrade = code => py(`/api/quant/market/block-trade/${code}`)
export const getStockHolders = code => py(`/api/quant/market/holders/${code}`)
// 龙虎榜：东财数据中心（近 N 日上榜记录 + 最新买卖席位 + 机构净买），单位万元
export const getDragonTiger = (code, days = 30) => py(`/api/quant/market/dragon-tiger/${code}`, { params: { days } })
// 限售解禁（需交易日；默认取当日）
export const getStockLockup = code => py('/api/quant/data-ext/lockup-expiry', { params: { code, trade_date: new Date().toISOString().slice(0, 10) } })
// 个股所属概念板块（东财）
export const getConceptHot = code => py('/api/quant/data-ext/concept-blocks', { params: { code } })
export const getSectorBlocks = () => py('/api/quant/market/sectors')
export const getIndustryList = (top = 20) => py('/api/quant/market/sectors')
export const getReports = (code, params = {}) => py('/api/quant/research/reports', { params: { code, ...params } })
export const getAnnouncements = code => py('/api/quant/data-ext/announcements', { params: { code } })
export const getMarginData = code => py(`/api/quant/market/margin/${code}`)
export const getFundFlow = code => py(`/api/quant/market/fund-flow/${code}`)
export const getPortfolioData = () => py('/api/quant/portfolio')
export const getStockNews = code => py('/api/quant/data-ext/news/stock', { params: { code } })

// quant-py-service 统一数据网关 (proxy to 9006 via /pyquant/)
// Python 服务获取行情数据较慢（全市场快照冷启可达数十秒），超时放宽到 60s
// 默认走同源 /pyquant 前缀（Vite 代理→9006），内网穿透公网访问同样可用；
// 仅在需要跨机部署时用 VITE_QUANT_API 覆盖（旧默认值 http://localhost:9006
// 在公网访问时会打到访客自己电脑，必须避免）。
const PY_BASE = (import.meta.env?.VITE_QUANT_API || '/pyquant').replace(/\/$/, '')
const py = (path, config) => {
  return request.request({ ...config, url: `${PY_BASE}${path}`, method: config?.method || 'get', baseURL: '', timeout: config?.timeout || 60000 })
}

export const getPyQuotes = (page = 0, pageSize = 30) => py('/api/quant/market/quotes', { params: { page, page_size: pageSize } })
export const getPyQuote = symbol => py(`/api/quant/market/quote/${symbol}`)
export const getPyKline = (symbol, days = 60, period = 'daily') => py(`/api/quant/market/kline/${symbol}`, { params: { days, period } })
export const getPyStockInfo = symbol => py(`/api/quant/market/info/${symbol}`)
export const getPySearch = q => py('/api/quant/market/search', { params: { q } })
export const getPySparklines = codes => py('/api/quant/market/sparklines', { params: { codes: codes.join(',') } })
export const getPySectors = () => py('/api/quant/market/sectors')
export const getPyNews = (limit = 20) => py('/api/quant/market/news', { params: { limit } })
// eodhd 全球财经新闻（美股/全球代码如 AAPL；A股代码该源返回空）
export const getPyGlobalNews = (symbol = '', limit = 30) => py('/api/quant/market/news/global', { params: { symbol, limit } })
export const getPyStrategies = () => py('/api/quant/strategies')
export const getPyStrategy = id => py(`/api/quant/strategies/${id}`)
export const runPyBacktest = data => py('/api/quant/backtest/run', { method: 'post', data })
export const getPyBacktestStatus = taskId => py(`/api/quant/backtest/status/${taskId}`)
export const getPyBacktestResult = taskId => py(`/api/quant/backtest/result/${taskId}`)
export const runPyBacktestQuick = data => py('/api/quant/backtest/quick', { method: 'post', data })
// 策略选股：内置策略条件筛选 / 自定义 Python 代码筛选
// build_pool 优化后 80 只候选股并行拉 K 线（16 workers + 8s per-stock 超时），总耗时应 < 60s
export const getPyScreenStrategy = (id, params) => py(`/api/quant/strategies/${id}/screen`, { params, timeout: 120000 })
export const getPyScreenCustom = data => py('/api/quant/screen/custom', { method: 'post', data, timeout: 120000 })
// 行情同步告警（NoticePanel 轮询用；trae-preview 爆发期 axios 挂起，改原生 fetch）
export const getPyMarketAlerts = () => fetchJson(`${PY_BASE}/api/quant/market/alerts`)
// 服务端默认 LLM 配置状态（不暴露 api_key）
export const getPyLlmStatus = () => py('/api/quant/ai/llm-status')
// AI 深度投研完整流水线包含约 15 次 LLM 调用，超时放宽到 10 分钟
export const getPyAiAnalyze = (symbol, llmConfig) => py('/api/quant/ai/analyze', { method: 'post', data: { symbol, include_ai: true, llm_config: llmConfig || null }, timeout: 600000 })
export const getPyLlmProviders = () => py('/api/quant/ai/llm-providers')
export const getPyModels = (base_url, api_key) => py('/api/quant/ai/models', { method: 'post', data: { base_url, api_key } })
export const getPyDashboard = () => py('/api/quant/dashboard')
export const getPyPortfolio = () => py('/api/quant/portfolio')
export const getPyLimitPools = () => py('/api/quant/market/limit-pools')
export const getPyBoardProgress = () => py('/api/quant/market/board-progress')
// 涨停池按连板数分层（首板/1进2/.../6进7），duanxianxia 风格
export const getPyLimitPoolBoards = () => py('/api/quant/market/limit-pool-boards')
export const getPySectorTrends = (days = 30, top = 8) => py('/api/quant/market/sector-trends', { params: { days, top }, timeout: 30000 })
export const getPyFundFlow = () => py('/api/quant/market/fund-flow')
export const getPyReports = (params) => py('/api/quant/research/reports', { params })
export const getPyReportsLatest = (limit = 20) => py('/api/quant/research/reports/latest', { params: { limit } })
export const getPyRecommend = (limit = 10, force = false) => py('/api/quant/recommend', { params: { limit, force }, timeout: 120000 })
// 多策略选股推荐（8种内置策略分组选股，force=true 强制重新计算）
export const getPyRecommendStrategies = (force = false) => py('/api/quant/recommend/strategies', { params: { force }, timeout: 120000 })
// 数据补全（全池/单支）与状态查询
export const startPyBackfill = (symbol) => symbol ? py(`/api/quant/data/backfill/${symbol}`, { method: 'post' }) : py('/api/quant/data/backfill', { method: 'post' })
export const getPyBackfillStatus = () => py('/api/quant/data/backfill/status')
// 个股基本面历史（Supabase 落库优先 + 实时兜底写穿，规避上游间歇风控）
export const getPyStockFinance = symbol => py(`/api/quant/market/finance/${symbol}`)
export const getPyStockHolders = symbol => py(`/api/quant/market/holders/${symbol}`)
export const getPyStockMargin = symbol => py(`/api/quant/market/margin/${symbol}`)
export const getPyStockFundFlow = symbol => py(`/api/quant/market/fund-flow/${symbol}`)
export const getPyStockDividend = symbol => py(`/api/quant/market/dividend/${symbol}`)
export const getPyStockBlockTrade = symbol => py(`/api/quant/market/block-trade/${symbol}`)
// 基本面历史采集（全池/单股手动触发 + 状态）
export const startPyFundHistoryCollect = (symbol = 'all') => py(`/api/quant/data/fund-history/collect?symbol=${symbol}`, { method: 'post' })
export const getPyFundHistoryStatus = () => py('/api/quant/data/fund-history/status')

// 用户持仓管理（quant-py-service，JWT 鉴权）
// 普通用户只能看/改自己的持仓，管理员可查看所有用户
export const getMyHoldings = () => py('/api/quant/my-holdings', { timeout: 15000 })
export const saveHolding = data => py('/api/quant/my-holdings', { method: 'post', data, timeout: 15000 })
export const deleteHolding = id => py(`/api/quant/my-holdings/${id}`, { method: 'delete', timeout: 15000 })

// ============================================================
// 同花顺金融数据 API（HiThink-Tech/Financial-API，付费源，数据质量最优）
// 走 /pyquant 代理到 quant-py-service 的 /api/quant/hithink/* 端点
// ============================================================
export const getHithinkStatus = () => py('/api/quant/hithink/status')
export const getHithinkQuote = symbols => py('/api/quant/hithink/quote', { params: { symbols: Array.isArray(symbols) ? symbols.join(',') : symbols } })
export const getHithinkKline = (symbol, days = 120, adjust = 'forward') => py(`/api/quant/hithink/kline/${symbol}`, { params: { days, adjust } })
export const getHithinkDragonTiger = (tradeDate = '', boardType = 'all') => py('/api/quant/hithink/dragon-tiger', { params: { trade_date: tradeDate, board_type: boardType } })
export const getHithinkLimitUp = (tradeDate = '') => py('/api/quant/hithink/limit-up', { params: { trade_date: tradeDate } })
export const getHithinkLimitDown = (tradeDate = '') => py('/api/quant/hithink/limit-down', { params: { trade_date: tradeDate } })
export const getHithinkLimitLadder = () => py('/api/quant/hithink/limit-ladder')
export const getHithinkHotStocks = (period = '24h') => py('/api/quant/hithink/hot-stocks', { params: { period } })
export const getHithinkSkyrocket = (period = '24h') => py('/api/quant/hithink/skyrocket', { params: { period } })
export const getHithinkSearch = query => py('/api/quant/hithink/search', { params: { query } })

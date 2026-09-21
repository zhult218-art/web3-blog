// ============================================================
// 量化交易接口（quant-service，走网关 /api）
// 包含三部分接口：
// 1. quant-service：策略管理 / 股票基础接口
// 2. Vibe-Research（vr）：经 /vr 前缀直连 8900 端口的 A 股扩展数据
// 3. quant-py-service（py）：经 /pyquant 前缀直连 9006 端口的统一行情网关
// ============================================================
import request from './request'

// Vibe-Research A股数据接口 (direct proxy to port 8900 via /vr/)
// 行情聚合接口冷启动较慢，超时放宽到 60s，避免前端误报"请求超时"
const vr = (path, config) => {
  const full = `/vr${path}`
  return request.request({ ...config, url: full, method: config?.method || 'get', baseURL: '', timeout: config?.timeout || 60000 })
}

// quant-service strategies
// 股票列表/看板首次调用会触发 Java 后端拉取 Python 全市场数据（冷启动可达 30s+），超时放宽到 60s
export const getQuantList = params => request.get('/strategy/list', { params })
export const createStrategy = data => request.post('/strategy', data)
export const updateStrategy = (id, data) => request.put(`/strategy/${id}`, data)
export const deleteStrategy = id => request.delete(`/strategy/${id}`)
export const runStrategy = id => request.post(`/strategy/${id}/run`)
export const getQuantLog = params => request.get('/quant/logs', { params })
export const getStockList = () => request.get('/stock/list', { timeout: 60000 })
export const getStockDetail = symbol => request.get(`/stock/${symbol}/detail`, { timeout: 60000 })
export const getStockKline = (symbol, params) => request.get(`/stock/${symbol}/kline`, { params, timeout: 60000 })
export const getStockDashboard = () => request.get('/stock/dashboard', { timeout: 60000 })

// Vibe-Research endpoints (direct to 8900)
export const getMarketOverview = () => vr('/market/overview')
export const getMarketEmotion = () => vr('/market/emotion')
export const getMarketTurnoverTop = () => vr('/market/turnover-top')
export const getIndices = () => vr('/indices')
export const getStockQuote = codes => vr('/quote', { params: { codes: codes.join(',') } })
export const getStockKlineVR = (code, period = 'day') => {
  const catMap = { day: 4, week: 5, month: 6, min60: 11, min30: 10, min15: 9, min5: 8, min1: 7 }
  const cat = catMap[period] || 4
  return vr('/kline', { params: { code, category: cat, offset: 120 } })
}
export const getStockFinance = code => vr('/finance', { params: { code } })
export const getStockValuation = code => vr('/valuation', { params: { code } })
export const getStockDividend = code => vr('/dividend', { params: { code } })
export const getStockBlockTrade = code => vr('/block-trade', { params: { code } })
export const getStockHolders = code => vr('/holders', { params: { code } })
export const getDragonTiger = (code, days) => vr('/dragon-tiger', { params: { code, days } })
export const getStockLockup = code => vr('/lockup', { params: { code } })
export const getConceptHot = (code) => vr('/hot-concepts', code ? { params: { code } } : {})
export const getIndustryList = (top = 20) => vr('/industry', { params: { top } })
export const getSectorBlocks = () => vr('/blocks')
export const getReports = code => vr('/reports', { params: { code } })
export const getAnnouncements = code => vr('/announcements', { params: { code } })
export const getMarginData = code => vr('/margin', { params: { code } })
export const getFundFlow = code => vr('/fund-flow', { params: { code } })
export const getGlobalIndices = () => vr('/global/indices')
export const getGlobalStock = (code, market) => vr('/global/stock', { params: { code, market } })
export const getRadarData = () => vr('/radar')
export const getPortfolioData = () => vr('/portfolio')
export const getChatResponse = (message) => vr('/chat', { method: 'post', data: { message } })
export const getStockNews = code => vr('/news', { params: { code } })

// quant-py-service 统一数据网关 (proxy to 9006 via /pyquant/)
// Python 服务获取行情数据较慢（全市场快照冷启可达数十秒），超时放宽到 60s
// 行情/K线直连 pyquant 后端(9006)绕过 vite /pyquant 代理；部署环境可用 VITE_QUANT_API 覆盖
const PY_BASE = (import.meta.env?.VITE_QUANT_API || 'http://localhost:9006').replace(/\/$/, '')
const py = (path, config) => {
  return request.request({ ...config, url: `${PY_BASE}${path}`, method: config?.method || 'get', timeout: config?.timeout || 60000 })
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
// 策略选股：内置策略条件筛选 / 自定义 Python 代码筛选（6s 沙箱，放宽超时）
export const getPyScreenStrategy = (id, params) => py(`/api/quant/strategies/${id}/screen`, { params, timeout: 180000 })
export const getPyScreenCustom = data => py('/api/quant/screen/custom', { method: 'post', data, timeout: 180000 })
// 行情同步告警（NoticePanel 轮询用）
export const getPyMarketAlerts = () => py('/api/quant/market/alerts')
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

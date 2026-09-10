// ============================================================
// 量化交易接口（quant-service，走网关 /api）
// 包含三部分接口：
// 1. quant-service：策略管理 / 股票基础接口
// 2. Vibe-Research（vr）：经 /vr 前缀直连 8900 端口的 A 股扩展数据
// 3. quant-py-service（py）：经 /pyquant 前缀直连 9006 端口的统一行情网关
// ============================================================
import request from './request'

// Vibe-Research A股数据接口 (direct proxy to port 8900 via /vr/)
const vr = (path, config) => {
  const full = `/vr${path}`
  return request.request({ ...config, url: full, method: config?.method || 'get', baseURL: '' })
}

// quant-service strategies
export const getQuantList = params => request.get('/strategy/list', { params })
export const createStrategy = data => request.post('/strategy', data)
export const updateStrategy = (id, data) => request.put(`/strategy/${id}`, data)
export const deleteStrategy = id => request.delete(`/strategy/${id}`)
export const runStrategy = id => request.post(`/strategy/${id}/run`)
export const getQuantLog = params => request.get('/quant/logs', { params })
export const getStockList = () => request.get('/stock/list')
export const getStockDetail = symbol => request.get(`/stock/${symbol}/detail`)
export const getStockKline = (symbol, params) => request.get(`/stock/${symbol}/kline`, { params })
export const getStockDashboard = () => request.get('/stock/dashboard')

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
// Python 服务获取行情数据较慢，超时设为 30s
const py = (path, config) => {
  const full = `/pyquant${path}`
  return request.request({ ...config, url: full, method: config?.method || 'get', baseURL: '', timeout: 30000 })
}

export const getPyQuotes = () => py('/api/quant/market/quotes')
export const getPyQuote = symbol => py(`/api/quant/market/quote/${symbol}`)
export const getPyKline = (symbol, days = 60, period = 'daily') => py(`/api/quant/market/kline/${symbol}`, { params: { days, period } })
export const getPyStockInfo = symbol => py(`/api/quant/market/info/${symbol}`)
export const getPySearch = q => py('/api/quant/market/search', { params: { q } })
export const getPySparklines = codes => py('/api/quant/market/sparklines', { params: { codes: codes.join(',') } })
export const getPySectors = () => py('/api/quant/market/sectors')
export const getPyNews = (limit = 20) => py('/api/quant/market/news', { params: { limit } })
export const getPyStrategies = () => py('/api/quant/strategies')
export const getPyStrategy = id => py(`/api/quant/strategies/${id}`)
export const runPyBacktest = data => py('/api/quant/backtest/run', { method: 'post', data })
export const getPyBacktestStatus = taskId => py(`/api/quant/backtest/status/${taskId}`)
export const getPyBacktestResult = taskId => py(`/api/quant/backtest/result/${taskId}`)
export const runPyBacktestQuick = data => py('/api/quant/backtest/quick', { method: 'post', data })
export const getPyAiAnalyze = symbol => py('/api/quant/ai/analyze', { method: 'post', data: { symbol, include_ai: true } })
export const getPyDashboard = () => py('/api/quant/dashboard')
export const getPyPortfolio = () => py('/api/quant/portfolio')

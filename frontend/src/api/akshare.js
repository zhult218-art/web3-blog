// AkShare A股数据服务
// 统一通过 Vite 代理 (/vr → Vibe-Research 8900 /api) 获取真实行情数据
// 参考: https://akshare.akfamily.xyz/
import request from '@/api/request'

const BASE_URL = '/vr'

// 请求工具：复用统一 axios 实例（含 token 注入 / 401 续期 / 错误兜底）
// 通过 per-request baseURL 覆盖默认的 '/api'，指向 '/vr'
// suppressError: 行情接口失败不应弹 toast 刷屏，由调用方自行处理
async function call(url, params = {}) {
  const data = await request.get(url, {
    baseURL: BASE_URL,
    params,
    suppressError: true,
    timeout: 20000,
  })
  return data?.data ?? null
}

// ============================================================
// 指数数据 (上证、深证、创业板、科创板) → 真实来源 /vr/indices
// ============================================================
export async function getIndexQuotes() {
  return (await call('/indices')) || []
}

// ============================================================
// A股实时行情 (全部股票) → 真实来源 /vr/market/turnover-top
// ============================================================
export async function getStockList(page = 1, size = 50) {
  return (await call('/market/turnover-top', { limit: size })) || []
}

// ============================================================
// 个股详情 → 真实来源 /vr/quote?codes=
// ============================================================
export async function getStockQuote(code) {
  const data = await call('/quote', { codes: code })
  return data?.[code] || null
}

// ============================================================
// K线数据 → 真实来源 /vr/kline (category=4 日线)
// ============================================================
export async function getKlineData(code, period = 'daily', count = 100) {
  const category = { daily: 4, weekly: 5, monthly: 6 }[period] || 4
  return (await call('/kline', { code, category, offset: count })) || []
}

// ============================================================
// 全球市场指数 → 真实来源 /vr/global/indices
// ============================================================
export async function getGlobalIndices() {
  return (await call('/global/indices')) || []
}

// ============================================================
// 市场情绪 → 真实来源 /vr/market/emotion
// ============================================================
export async function getMarketEmotion() {
  return await call('/market/emotion')
}

// ============================================================
// 热门概念板块 → 真实来源 /vr/hot-concepts
// ============================================================
export async function getHotConcepts() {
  return (await call('/hot-concepts', { code: '000001' })) || []
}

// ============================================================
// 行业板块排行 → 真实来源 /vr/industry
// ============================================================
export async function getIndustryList() {
  const data = await call('/industry', { top: 10 })
  return data?.top || []
}

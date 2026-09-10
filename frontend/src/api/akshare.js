// Akshare A鑲℃暟鎹湇鍔?// 缁熶竴閫氳繃 Vite 浠ｇ悊 (/vr 鈫?Vibe-Research 8900 /api) 鑾峰彇鐪熷疄琛屾儏鏁版嵁
// 鍙傝€? https://akshare.akfamily.xyz/

const BASE_URL = '/vr'

// 璇锋眰宸ュ叿
async function request(url, params = {}) {
  const query = new URLSearchParams(params).toString()
  const response = await fetch(`${BASE_URL}${url}?${query}`)
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }
  return response.json()
}

// ============================================================
// 鎸囨暟鏁版嵁 (涓婅瘉銆佹繁璇併€佸垱涓氭澘銆佺鍒?0) 鈥?鐪熷疄鏉ユ簮 /vr/indices
// ============================================================
export async function getIndexQuotes() {
  const data = await request('/indices')
  return data?.data || []
}

// ============================================================
// A鑲″疄鏃惰鎯?(鍏ㄩ儴鑲＄エ) 鈥?鐪熷疄鏉ユ簮 /vr/market/turnover-top
// ============================================================
export async function getStockList(page = 1, size = 50) {
  const data = await request('/market/turnover-top', { limit: size })
  return data?.data || []
}

// ============================================================
// 涓偂璇︽儏 鈥?鐪熷疄鏉ユ簮 /vr/quote?codes=
// ============================================================
export async function getStockQuote(code) {
  const data = await request('/quote', { codes: code })
  return data?.data?.[code] || null
}

// ============================================================
// K绾挎暟鎹?鈥?鐪熷疄鏉ユ簮 /vr/kline (category=4 鏃ョ嚎)
// ============================================================
export async function getKlineData(code, period = 'daily', count = 100) {
  const category = { daily: 4, weekly: 5, monthly: 6 }[period] || 4
  const data = await request('/kline', { code, category, offset: count })
  return data?.data || []
}

// ============================================================
// 鍏ㄧ悆甯傚満鎸囨暟 鈥?鐪熷疄鏉ユ簮 /vr/global/indices
// ============================================================
export async function getGlobalIndices() {
  const data = await request('/global/indices')
  return data?.data || []
}

// ============================================================
// 甯傚満鎯呯华 鈥?鐪熷疄鏉ユ簮 /vr/market/emotion
// ============================================================
export async function getMarketEmotion() {
  const data = await request('/market/emotion')
  return data?.data || null
}

// ============================================================
// 鐑棬姒傚康鏉垮潡 鈥?鐪熷疄鏉ユ簮 /vr/hot-concepts
// ============================================================
export async function getHotConcepts() {
  const data = await request('/hot-concepts', { code: '000001' })
  return data?.data || []
}

// ============================================================
// 琛屼笟鏉垮潡鎺掕 鈥?鐪熷疄鏉ユ簮 /vr/industry
// ============================================================
export async function getIndustryList() {
  const data = await request('/industry', { top: 10 })
  return data?.data?.top || []
}


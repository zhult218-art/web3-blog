// ============================================================
// API Token 中转站接口（ai-proxy-service :8093，走网关 /api）
// 渠道管理 / 令牌管理 / 模型倍率 / 用量统计 / 智能看板 / 请求日志
// ============================================================
import request from './request'

// ---------------- 智能看板 ----------------
// 看板聚合：总览 + 14 天趋势 + 模型分布 + 用户排行 + 时段热度 + 错误分布
export function getAiDashboard() {
  return request.get('/admin/ai/dashboard')
}

// 智能洞察：规则引擎输出的运营建议与风险告警
export function getAiInsights() {
  return request.get('/admin/ai/insights')
}

// 总览指标（今日/昨日 + 环比）
export function getAiOverview() {
  return request.get('/admin/ai/stats/overview')
}

// 按用户聚合（近 30 天用量排行）
export function getAiStatsByUser() {
  return request.get('/admin/ai/stats/by-user')
}

// 按模型聚合（近 30 天）
export function getAiStatsByModel() {
  return request.get('/admin/ai/stats/by-model')
}

// 按日趋势
export function getAiTrend(days = 14) {
  return request.get('/admin/ai/stats/trend', { params: { days } })
}

// 时段热度（近 24h）
export function getAiHourly() {
  return request.get('/admin/ai/stats/hourly')
}

// 错误分布（近 24h）
export function getAiErrors() {
  return request.get('/admin/ai/stats/errors')
}

// ---------------- 渠道管理 ----------------
export function getChannels() {
  return request.get('/admin/ai/channels')
}

export function createChannel(data) {
  // data: { name, baseUrl, apiSecret(明文,服务端AES加密), models:[], groupName, weight, status, balance }
  return request.post('/admin/ai/channels', data)
}

export function updateChannel(id, data) {
  return request.put(`/admin/ai/channels/${id}`, data)
}

export function deleteChannel(id) {
  return request.delete(`/admin/ai/channels/${id}`)
}

// 连通测试：GET {base}/models 最小探针
export function testChannel(id) {
  return request.post(`/admin/ai/channels/${id}/test`)
}

// ---------------- 令牌管理 ----------------
export function getTokens(params) {
  // params: { page, size, userId? }
  return request.get('/admin/ai/tokens', { params })
}

export function createToken(data) {
  // data: { userId, name, expiredAt?, quota?, modelWhitelist?, ipWhitelist? }
  // 返回 data.fullToken 仅此一次展示
  return request.post('/admin/ai/tokens', data)
}

export function updateToken(id, data) {
  // data: { status?, name?, expiredAt?, modelWhitelist?, ipWhitelist? }
  return request.put(`/admin/ai/tokens/${id}`, data)
}

export function topupToken(id, amount) {
  return request.post(`/admin/ai/tokens/${id}/topup`, { amount })
}

export function deleteToken(id) {
  return request.delete(`/admin/ai/tokens/${id}`)
}

// ---------------- 模型倍率 ----------------
export function getModels() {
  return request.get('/admin/ai/models')
}

export function createModel(data) {
  return request.post('/admin/ai/models', data)
}

export function updateModel(id, data) {
  // data: { rate?, completionRate?, status? }
  return request.put(`/admin/ai/models/${id}`, data)
}

export function deleteModel(id) {
  return request.delete(`/admin/ai/models/${id}`)
}

// ---------------- 请求日志 ----------------
export function getAiLogs(params) {
  // params: { page, size, userId?, tokenId?, model?, statusCode?, startDate?, endDate? }
  return request.get('/admin/ai/logs', { params })
}

// ============================================================
// 用户侧中转站门户（ai-proxy-service，需登录）
// ============================================================

// 公开模型与倍率列表（门户定价表）
export function getUserModels() {
  return request.get('/user/ai/models')
}

// 我的令牌分页列表
export function getMyTokens(params) {
  return request.get('/user/ai/tokens', { params })
}

// 创建我的令牌：{ name, quota?, expiredAt?, modelLimit? } → data.fullToken 仅此一次
export function createMyToken(data) {
  return request.post('/user/ai/tokens', data)
}

// 更新我的令牌：{ name?, status? }
export function updateMyToken(id, data) {
  return request.put(`/user/ai/tokens/${id}`, data)
}

// 删除我的令牌
export function deleteMyToken(id) {
  return request.delete(`/user/ai/tokens/${id}`)
}

// 兑换码核销到我的令牌：{ code }
export function redeemToMyToken(id, code) {
  return request.post(`/user/ai/tokens/${id}/redeem`, { code })
}

// 我的用量汇总（近 30 天）
export function getMyAiStats() {
  return request.get('/user/ai/stats')
}

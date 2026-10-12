// ============================================================
// OSINT 信息收集接口（统一 py-service，经 Vite /osintapi 代理 → /api/osint）
// 页面路由本身是 /osint，代理前缀用 /osintapi 错开。
// ============================================================
import request from './request'

const oi = (path, config) =>
  request.request({
    ...config,
    url: `/osintapi${path}`,
    method: config?.method || 'get',
    baseURL: '',
    timeout: config?.timeout || 30000,
  })

// 能力检测：可用模块 / API Key 状态
export const getCapabilities = (cfg) => oi('/capabilities', cfg)

// 模块列表
export const getModules = () => oi('/modules')

// 创建扫描任务
export const createScan = (target, modules = []) =>
  oi('/scans', { method: 'post', data: { target, modules }, timeout: 30000 })

// 扫描任务列表
export const listScans = () => oi('/scans')

// 扫描任务状态
export const getScan = (jid) => oi(`/scans/${jid}`)

// 扫描任务摘要
export const getScanSummary = (jid) => oi(`/scans/${jid}/summary`)

// 扫描日志
export const getScanLogs = (jid, offset = 0) =>
  oi(`/scans/${jid}/logs`, { params: { offset } })

// 停止扫描
export const stopScan = (jid) => oi(`/scans/${jid}/stop`, { method: 'post' })

// WebSocket 实时日志
export const scanWsUrl = (jid) => {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${window.location.host}/osintapi/scans/${jid}/ws`
}

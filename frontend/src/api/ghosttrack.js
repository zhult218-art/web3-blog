// ============================================================
// GhostTrack 网络追踪接口（py-service 9006 的 ghosttrack 模块）
// 经 Vite 代理 /gtapi → /api/ghosttrack（baseURL 置空避免叠加 /api）
// suppressError 关闭全局 toast，由页面展示后端返回的具体错误信息
// ============================================================
import request from './request'

const BASE = '/gtapi'
const opts = { baseURL: '', timeout: 120000, suppressError: true }

// 检测站点清单（前端展示「将检测哪些站点」）
export const gtSites = () => request.get(`${BASE}/sites`, opts)
// IP 追踪：ip 留空则查询调用方自己的公网 IP
export const gtIp = data => request.post(`${BASE}/ip`, data, opts)
// 手机号归属解析（服务端本地库，纯离线）
export const gtPhone = data => request.post(`${BASE}/phone`, data, opts)
// 用户名跨站存在性检测（并发探测白名单站点）
export const gtUsername = data => request.post(`${BASE}/username`, data, opts)

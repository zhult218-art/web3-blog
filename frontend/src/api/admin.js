// ============================================================
// 管理后台接口（admin-service，走网关 /api）
// 仪表盘 / 操作日志 / 访问统计
// ============================================================
import request from './request'

// ───────── 服务注册表（前端硬编码兜底，admin-service 挂了也能显示） ─────────
export const SERVICE_REGISTRY = [
  { name: 'gateway',          label: 'API 网关',     port: 8080, core: true },
  { name: 'user-service',     label: '用户服务',     port: 8081, core: true },
  { name: 'blog-service',     label: '博客服务',     port: 8082, core: true },
  { name: 'forum-service',    label: '论坛服务',     port: 8083, core: false },
  { name: 'shop-service',     label: '商城服务',     port: 8084, core: false },
  { name: 'media-service',    label: '媒体服务',     port: 8085, core: false },
  { name: 'quant-service',    label: '量化服务',     port: 8086, core: false },
  { name: 'tool-service',     label: '工具服务',     port: 8087, core: false },
  { name: 'software-service', label: '软件服务',     port: 8088, core: false },
  { name: 'resource-service', label: '资源服务',     port: 8089, core: false },
  { name: 'ai-proxy-service', label: 'AI 代理',      port: 8093, core: false },
  { name: 'jarvis-service',   label: 'Jarvis',       port: 9001, core: false },
  { name: 'admin-service',    label: '管理服务',     port: 9002, core: true },
]

// 用注册表初始化服务列表（状态未知时标 STOPPED，前端可随时启动）
export function getDefaultServices() {
  return SERVICE_REGISTRY.map(s => ({ ...s, status: 'UNKNOWN', _loading: false }))
}

// 合并后端返回的健康数据与注册表（保证列表永远不为空）
export function mergeServices(healthData) {
  const map = {}
  if (Array.isArray(healthData)) {
    healthData.forEach(s => { map[s.name] = s })
  }
  return SERVICE_REGISTRY.map(reg => {
    const live = map[reg.name]
    return {
      ...reg,
      status: live?.status || 'STOPPED',
      _loading: false,
    }
  })
}

// 仪表盘总览：用户数、文章数、订单数、营收、今日访问、服务状态
export function getDashboard() {
  return request.get('/admin/dashboard')
}

// 最近管理操作日志（管理员行为审计）
export function getAdminLogs(limit = 50) {
  return request.get('/admin/logs', { params: { limit } })
}

// 访问上报：IP / 经纬度 / 位置信息（页面打开时由 App.vue 调用）
export function reportVisit(data) {
  return request.post('/admin/visit', data)
}

// 分页查询访问记录
export function getVisitList(params) {
  return request.get('/admin/visit/list', { params })
}

// 访问统计汇总：累计 / 今日 / 7日独立 IP 等
export function getVisitSummary() {
  return request.get('/admin/visit/summary')
}

// 真实在线人数：近 10 分钟内有访问记录的去重 IP 数（admin-service 实时统计）
export function getOnlineCount() {
  return request.get('/admin/visit/online')
}

// ───────── 服务健康与管理 ─────────
// 所有服务健康状态（管理员）
export function getServicesHealth() {
  return request.get('/admin/services/health')
}
// 服务状态 Map（无需登录，导航栏用）
export function getServicesStatus() {
  return request.get('/admin/services/status')
}
// 启动服务
export function startService(name) {
  return request.post(`/admin/services/${name}/start`)
}
// 停止服务
export function stopService(name) {
  return request.post(`/admin/services/${name}/stop`)
}

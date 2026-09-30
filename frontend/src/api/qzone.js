// ============================================================
// 时光回溯 · QQ 空间备份接口（qzone-service 9012，经 Vite /qzapi 代理）
// 不写死任何主机与端口：本地走 /qzapi 前缀，公网经 SakuraFrp 同源隧道同样可达。
// （页面路由本身是 /qzone，代理前缀必须与之错开，否则页面请求会被代理吞掉）
// ============================================================
import request from './request'

const qz = (path, config) =>
  request.request({
    ...config,
    url: `/qzapi${path}`,
    method: config?.method || 'get',
    baseURL: '',
    timeout: config?.timeout || 30000,
  })

// 新建扫码会话，返回 { sid, qr_png(dataURL) }
export const startQr = () => qz('/qr/start', { method: 'post' })

// 轮询扫码状态：waiting / scanned / success / expired / canceled
export const qrStatus = (sid) => qz('/qr/status', { params: { sid } })

// 开始后台抓取（workers：并发线程 1-3）
export const startJob = (sid, interval, workers = 2) =>
  qz('/job/start', { method: 'post', data: { sid, interval, workers } })

// 任务进度
export const getJob = (jid) => qz(`/job/${jid}`)

// 任务内说说分页预览
export const getJobMoments = (jid, offset = 0, limit = 50) => qz(`/job/${jid}/moments`, { params: { offset, limit } })

// 取消 / 删除任务
export const cancelJob = (jid) => qz(`/job/${jid}/cancel`, { method: 'post' })
export const deleteJob = (jid) => qz(`/job/${jid}`, { method: 'delete' })

// 下载地址（直接走浏览器同源请求，jid 不可猜测；临时数据 24h 后失效）
export const downloadUrl = (jid, format) => `/qzapi/job/${jid}/download?format=${format}`

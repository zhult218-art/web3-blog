// ============================================================
// 美甲小铺接口（nail-service，走网关 /api）
// 店铺资料 / 作品 / 点赞 / 预约
// 说明：资料与作品的读写失败时页面会回退 localStorage 兜底，
//       故带 suppressError 静默；预约类失败由全局拦截器 toast
// ============================================================
import request from './request'

// 店铺资料
export const getNailProfile = () => request.get('/nail/profile', { suppressError: true })
// 更新店铺资料（店主/管理员，整对象更新）
export const updateNailProfile = data => request.put('/nail/profile', data, { suppressError: true })
// 作品列表
export const getNailWorks = () => request.get('/nail/works', { suppressError: true })
// 新增作品（店主/管理员）
export const createNailWork = data => request.post('/nail/work', data, { suppressError: true })
// 点赞（无需登录，前端 fire-and-forget）
export const likeNailWork = id => request.put(`/nail/work/${id}/like`, null, { suppressError: true })
// 删除作品（店主/管理员）
export const deleteNailWork = id => request.delete(`/nail/work/${id}`, { suppressError: true })
// 创建预约（公开，游客可提交）
export const createNailBooking = data => request.post('/nail/booking', data)
// 预约列表（店主/管理员）
export const getNailBookings = () => request.get('/nail/booking/list')
// 修改预约状态（status: PENDING | CONFIRMED | DONE | CANCELLED）
export const updateBookingStatus = (id, status) => request.put(`/nail/booking/${id}/status`, null, { params: { status } })
// 用户端：凭手机号查询自己的预约
export const getMyNailBookings = phone => request.get('/nail/booking/my', { params: { phone }, suppressError: true })
// 用户端：凭手机号取消自己的预约（仅 PENDING/CONFIRMED 可取消）
export const cancelMyNailBooking = (id, phone) => request.put(`/nail/booking/${id}/cancel`, null, { params: { phone } })

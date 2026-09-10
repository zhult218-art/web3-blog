// ============================================================
// 软件库接口（software-service，走网关 /api）
// ============================================================
import request from './request'

// 分页查询软件列表
export const getSoftwareList = params => request.get('/software/list', { params })
// 软件详情
export const getSoftwareDetail = id => request.get(`/software/${id}`)
// 新增软件
export const createSoftware = data => request.post('/software', data)
// 更新软件
export const updateSoftware = (id, data) => request.put(`/software/${id}`, data)
// 删除软件
export const deleteSoftware = id => request.delete(`/software/${id}`)

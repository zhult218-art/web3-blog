// ============================================================
// 运维脚本 / 工具接口（走网关 /api）
// ============================================================
import request from './request'

// 分页查询工具/脚本列表
export const getToolList = params => request.get('/script/list', { params })
// 工具/脚本详情
export const getScriptDetail = id => request.get(`/script/${id}`)
// 新增脚本
export const createScript = data => request.post('/script', data)
// 更新脚本
export const updateScript = (id, data) => request.put(`/script/${id}`, data)
// 删除脚本
export const deleteScript = id => request.delete(`/script/${id}`)

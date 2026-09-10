// ============================================================
// 资源中心接口（走网关 /api）
// 文件上传 / 资源列表 / 下载 / 管理
// ============================================================
import request from './request'

// 分页查询资源列表
export const getResourceList = params => request.get('/resource/list', { params })
// 上传资源文件（FormData：file / title / category / description）
export const uploadResource = formData => request.post('/resource/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
// 生成资源下载链接（返回绝对地址，供 <a href> 直接使用）
export const downloadResource = filename => `/api/resource/download/${encodeURIComponent(filename)}`
// 更新资源信息
export const updateResource = (id, data) => request.put(`/resource/${id}`, data)
// 删除资源
export const deleteResource = id => request.delete(`/resource/${id}`)

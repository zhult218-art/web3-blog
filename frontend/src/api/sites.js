// ============================================================
// 分享网站接口（tool-service，走网关 /api）
// ============================================================
import request from './request'

// 分享网站列表（onlyVisible=true 仅返回显示中的站点）
export const getSiteList = params => request.get('/site/list', { params })
// 分享网站分类列表
export const getSiteCategories = () => request.get('/site/categories')
// 新增分享网站（管理员）
export const createSite = data => request.post('/site', data)
// 更新分享网站（管理员）
export const updateSite = (id, data) => request.put(`/site/${id}`, data)
// 删除分享网站（管理员）
export const deleteSite = id => request.delete(`/site/${id}`)
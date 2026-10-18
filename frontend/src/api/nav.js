// ============================================================
// 导航数据接口（招募亭 /jobs 与 赏金猎人 /earn）
// 读取类走 blog-service 的 MySQL（持久化，跨设备/跨浏览器一致）；
// 页面在接口不可用时回退到内置数据 src/data/navData.json，避免白屏。
// 管理端接口需要管理员 JWT（request 拦截器自动注入 Authorization 头）。
// ============================================================
import request from './request'

/**
 * 公开：读取指定导航页的分类与条目
 * @param {'jobs'|'earn'} page 页面标识
 */
export const getNavPage = page =>
  request.get(`/blog/nav/${page}`, { suppressError: true })

/** 管理端：指定页面的全部分类（含未启用） */
export const getAdminNavCategories = page =>
  request.get(`/blog/admin/nav/${page}/categories`)

/** 管理端：指定页面的全部条目（含未启用） */
export const getAdminNavLinks = page =>
  request.get(`/blog/admin/nav/${page}/links`)

/** 管理端：新增分类 */
export const createNavCategory = data =>
  request.post('/blog/admin/nav/category', data)

/** 管理端：编辑分类 */
export const updateNavCategory = (id, data) =>
  request.put(`/blog/admin/nav/category/${id}`, data)

/** 管理端：删除分类（连同该分类下的条目） */
export const deleteNavCategory = id =>
  request.delete(`/blog/admin/nav/category/${id}`)

/** 管理端：新增条目 */
export const createNavLink = data =>
  request.post('/blog/admin/nav/link', data)

/** 管理端：编辑条目 */
export const updateNavLink = (id, data) =>
  request.put(`/blog/admin/nav/link/${id}`, data)

/** 管理端：删除条目 */
export const deleteNavLink = id =>
  request.delete(`/blog/admin/nav/link/${id}`)

/** 管理端：一键抓取官网图标并持久化 */
export const fetchNavIcons = page =>
  request.post(`/blog/admin/nav/${page}/fetch-icons`)

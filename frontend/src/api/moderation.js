// ============================================================
// 社区内容审核接口（forum-service，走网关 /api）
// 帖子 / 评论的管理端列表、通过、驳回、删除，仅 ROLE_ADMIN 可调
// ============================================================
import request from './request'

// ── 帖子 ──
// 全量帖子分页（含 pending/rejected）
export const getAdminPosts = params => request.get('/post/admin/list', { params })
export const approvePost = id => request.post(`/post/admin/${id}/approve`)
export const rejectPost = id => request.post(`/post/admin/${id}/reject`)
export const deleteAdminPost = id => request.delete(`/post/admin/${id}`)
// 批量操作：action = approve / reject / delete
export const batchPosts = (ids, action) => request.post('/post/admin/batch', { ids, action })

// ── 评论 ──
// 全量评论分页（含 pending/rejected，附带所属帖子标题）
export const getAdminComments = params => request.get('/comment/admin/list', { params })
export const approveComment = id => request.post(`/comment/admin/${id}/approve`)
export const rejectComment = id => request.post(`/comment/admin/${id}/reject`)
export const deleteAdminComment = id => request.delete(`/comment/admin/${id}`)
// 批量操作：action = approve / reject / delete
export const batchComments = (ids, action) => request.post('/comment/admin/batch', { ids, action })

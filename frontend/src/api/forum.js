// ============================================================
// 社区论坛接口（forum-service，走网关 /api）
// 帖子 / 评论 / 统计
// ============================================================
import request from './request'

// 分页查询帖子列表
export const getForumList = params => request.get('/post/list', { params })
// getForumList 的语义别名（历史命名兼容）
export const getPostList = params => request.get('/post/list', { params })
// 帖子详情
export const getForumDetail = id => request.get(`/post/${id}`)
// 发布新帖
export const createPost = data => request.post('/post', data)
// 点赞帖子
export const likePost = id => request.post(`/post/${id}/like`)
// 删除帖子
export const deletePost = id => request.delete(`/post/${id}`)
// 查询评论列表（targetId + targetType 区分评论对象类型）
export const getCommentList = (targetId, targetType, params) => request.get('/comment/list', { params: { targetId, targetType, ...params } })
// 发表评论
export const createComment = data => request.post('/comment', data)
// 删除评论
export const deleteComment = id => request.delete(`/comment/${id}`)
// 帖子统计（发布数 / 点赞数等）
export const getPostStats = () => request.get('/post/stats')

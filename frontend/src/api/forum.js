// ============================================================
// 社区论坛接口（forum-service，走网关 /api）
// 帖子 / 评论 / 统计
// ============================================================
import request from './request'

// trae-preview 环境 axios XHR 异常挂起，列表/统计改用原生 fetch；
// 必须带超时且覆盖到响应体读完：fetch 在收到响应头即 resolve，
// 若提前清掉定时器，body 卡住会让 res.json() 永久挂起并占用连接池槽位
async function fetchJson(url, timeout = 15000) {
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), timeout)
  try {
    const res = await fetch(url, { signal: ctrl.signal })
    return await res.json()
  } finally {
    clearTimeout(timer)
  }
}

// 分页查询帖子列表
export const getForumList = params => {
  const qs = new URLSearchParams(params).toString()
  return fetchJson(`/api/post/list?${qs}`)
}
// getForumList 的语义别名（历史命名兼容）
export const getPostList = params => getForumList(params)
// 帖子详情
export const getForumDetail = id => request.get(`/post/${id}`)
// 发布新帖
export const createPost = data => request.post('/post', data)
// 点赞帖子
export const likePost = id => request.post(`/post/${id}/like`)
// 检查是否已点赞帖子
export const checkPostLike = id => request.get(`/post/${id}/like/check`, { suppressError: true })
// 收藏帖子
export const favoritePost = id => request.post(`/post/${id}/favorite`)
// 检查是否已收藏帖子
export const checkPostFavorite = id => request.get(`/post/${id}/favorite/check`, { suppressError: true })
// 点赞评论
export const likeComment = id => request.post(`/comment/${id}/like`)
// 删除帖子
export const deletePost = id => request.delete(`/post/${id}`)
// 查询评论列表（targetId + targetType 区分评论对象类型）
export const getCommentList = (targetId, targetType, params) => request.get('/comment/list', { params: { targetId, targetType, ...params } })
// 发表评论
export const createComment = data => request.post('/comment', data)
// 删除评论
export const deleteComment = id => request.delete(`/comment/${id}`)
// 帖子统计（发布数 / 点赞数等）
export const getPostStats = () => fetchJson('/api/post/stats')

// ============================================================
// 群聊接口（forum-service，走网关 /api）
// trae-preview 环境 axios XHR 对这些请求会异常挂起 15s+（原生 fetch 毫秒级返回），
// 故全部走原生 fetch：手动注入 JWT、401 静默续期一次后重放（对齐 axios 拦截器）、
// 默认 10s 快速超时——避免群聊初始化被 axios 超时 + 3s/6s/9s 重试拖到分钟级
// ============================================================
import { useAuthStore } from '@/stores/modules/auth'

async function chatFetch(path, { method = 'GET', body, formData, timeout = 10000 } = {}) {
  const auth = useAuthStore()
  // 中止定时器必须覆盖到响应体读完：fetch 在收到响应头即 resolve，
  // 若此时清掉定时器，body 流式传输卡住会让 res.json() 永久挂起
  const doFetch = async () => {
    const headers = {}
    if (auth.token) headers.Authorization = `Bearer ${auth.token}`
    if (body !== undefined) headers['Content-Type'] = 'application/json'
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), timeout)
    try {
      const res = await fetch(`/api${path}`, {
        method,
        headers,
        // FormData 由浏览器自动带 multipart boundary，不能手动设 Content-Type
        body: formData || (body !== undefined ? JSON.stringify(body) : undefined),
        signal: ctrl.signal,
      })
      const data = await res.json().catch(() => null)
      return { status: res.status, ok: res.ok, data }
    } finally {
      clearTimeout(timer)
    }
  }
  let r = await doFetch()
  if (r.status === 401 && auth.token) {
    // token 过期：静默续期一次后重放
    const recovered = await auth.tryRefresh()
    if (recovered) r = await doFetch()
  }
  if (!r.ok) {
    const err = new Error(r.data?.message || r.data?.detail || `请求失败(${r.status})`)
    err.status = r.status
    // 兼容 axios 风格的 e?.response?.data?.message 读取
    err.response = { status: r.status, data: r.data }
    throw err
  }
  return r.data
}

// 聊天开关状态（公开：{ enabled: true/false }；init 链路 6s 快速失败，配合后台重试）
export const getChatEnabled = () => chatFetch('/chat/enabled', { timeout: 6000 })
// 管理员设置聊天开关
export const setChatEnabled = enabled => chatFetch('/chat/enabled', { method: 'PUT', body: { enabled } })

// 群列表（含当前用户加入状态；init 链路 6s 快速失败）
export const getChatRooms = () => chatFetch('/chat/rooms', { timeout: 6000 })
// 申请加入群
export const joinChatRoom = roomId => chatFetch(`/chat/rooms/${roomId}/join`, { method: 'POST' })
// 退出群
export const leaveChatRoom = roomId => chatFetch(`/chat/rooms/${roomId}/leave`, { method: 'POST' })
// 自己在某群的状态
export const getMyChatStatus = roomId => chatFetch(`/chat/rooms/${roomId}/my-status`)
// 群成员
export const getChatMembers = roomId => chatFetch(`/chat/rooms/${roomId}/members`)
// 历史消息
export const getChatMessages = (roomId, limit = 100) => chatFetch(`/chat/rooms/${roomId}/messages?limit=${limit}`)

// 管理员：待审批列表
export const getPendingApplications = () => chatFetch('/chat/applications')
// 审批通过
export const approveApplication = id => chatFetch(`/chat/applications/${id}/approve`, { method: 'POST' })
// 审批拒绝
export const rejectApplication = id => chatFetch(`/chat/applications/${id}/reject`, { method: 'POST' })

// ========== 图片上传 ==========
// multipart 上传图片，返回 { url }
export const uploadChatImage = file => {
  const fd = new FormData()
  fd.append('file', file)
  return chatFetch('/chat/upload', { method: 'POST', formData: fd, timeout: 60000 })
}

// ========== 群公告 ==========
export const getAnnouncements = roomId => chatFetch(`/chat/rooms/${roomId}/announcements`)
export const createAnnouncement = (roomId, data) => chatFetch(`/chat/rooms/${roomId}/announcements`, { method: 'POST', body: data })
export const updateAnnouncement = (id, data) => chatFetch(`/chat/announcements/${id}`, { method: 'PUT', body: data })
export const deleteAnnouncement = id => chatFetch(`/chat/announcements/${id}`, { method: 'DELETE' })

// ========== 群相册 ==========
export const getAlbums = roomId => chatFetch(`/chat/rooms/${roomId}/albums`)
export const createAlbum = (roomId, name) => chatFetch(`/chat/rooms/${roomId}/albums`, { method: 'POST', body: { name } })
export const renameAlbum = (id, name) => chatFetch(`/chat/albums/${id}`, { method: 'PUT', body: { name } })
export const deleteAlbum = id => chatFetch(`/chat/albums/${id}`, { method: 'DELETE' })

// ========== 照片 ==========
export const getPhotos = (roomId, albumId) =>
  chatFetch(`/chat/rooms/${roomId}/photos${albumId ? `?albumId=${albumId}` : ''}`)
export const addPhoto = (roomId, data) => chatFetch(`/chat/rooms/${roomId}/photos`, { method: 'POST', body: data })
export const movePhoto = (id, albumId) => chatFetch(`/chat/photos/${id}/move`, { method: 'PUT', body: { albumId } })
export const deletePhoto = id => chatFetch(`/chat/photos/${id}`, { method: 'DELETE' })

// ========== 群通知 ==========
export const getNotifications = roomId => chatFetch(`/chat/rooms/${roomId}/notifications`)
export const createNotification = (roomId, data) => chatFetch(`/chat/rooms/${roomId}/notifications`, { method: 'POST', body: data })
export const deleteNotification = id => chatFetch(`/chat/notifications/${id}`, { method: 'DELETE' })

// ========== 聊天记录搜索 ==========
export const searchMessages = (roomId, keyword) =>
  chatFetch(`/chat/rooms/${roomId}/search?keyword=${encodeURIComponent(keyword)}`)

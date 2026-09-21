import request from './request'

// 群列表（含当前用户加入状态）
export const getChatRooms = () => request.get('/chat/rooms')
// 申请加入群
export const joinChatRoom = roomId => request.post(`/chat/rooms/${roomId}/join`)
// 退出群
export const leaveChatRoom = roomId => request.post(`/chat/rooms/${roomId}/leave`)
// 自己在某群的状态
export const getMyChatStatus = roomId => request.get(`/chat/rooms/${roomId}/my-status`)
// 群成员
export const getChatMembers = roomId => request.get(`/chat/rooms/${roomId}/members`)
// 历史消息
export const getChatMessages = (roomId, limit = 100) => request.get(`/chat/rooms/${roomId}/messages`, { params: { limit } })

// 管理员：待审批列表
export const getPendingApplications = () => request.get('/chat/applications')
// 审批通过
export const approveApplication = id => request.post(`/chat/applications/${id}/approve`)
// 审批通知
export const rejectApplication = id => request.post(`/chat/applications/${id}/reject`)

// ========== 图片上传 ==========
// multipart 上传图片，返回 { url }
export const uploadChatImage = file => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/chat/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}

// ========== 群公告 ==========
export const getAnnouncements = roomId => request.get(`/chat/rooms/${roomId}/announcements`)
export const createAnnouncement = (roomId, data) => request.post(`/chat/rooms/${roomId}/announcements`, data)
export const updateAnnouncement = (id, data) => request.put(`/chat/announcements/${id}`, data)
export const deleteAnnouncement = id => request.delete(`/chat/announcements/${id}`)

// ========== 群相册 ==========
export const getAlbums = roomId => request.get(`/chat/rooms/${roomId}/albums`)
export const createAlbum = (roomId, name) => request.post(`/chat/rooms/${roomId}/albums`, { name })
export const renameAlbum = (id, name) => request.put(`/chat/albums/${id}`, { name })
export const deleteAlbum = id => request.delete(`/chat/albums/${id}`)

// ========== 照片 ==========
export const getPhotos = (roomId, albumId) =>
  request.get(`/chat/rooms/${roomId}/photos`, { params: albumId ? { albumId } : {} })
export const addPhoto = (roomId, data) => request.post(`/chat/rooms/${roomId}/photos`, data)
export const movePhoto = (id, albumId) => request.put(`/chat/photos/${id}/move`, { albumId })
export const deletePhoto = id => request.delete(`/chat/photos/${id}`)

// ========== 群通知 ==========
export const getNotifications = roomId => request.get(`/chat/rooms/${roomId}/notifications`)
export const createNotification = (roomId, data) => request.post(`/chat/rooms/${roomId}/notifications`, data)
export const deleteNotification = id => request.delete(`/chat/notifications/${id}`)

// ========== 聊天记录搜索 ==========
export const searchMessages = (roomId, keyword) =>
  request.get(`/chat/rooms/${roomId}/search`, { params: { keyword } })

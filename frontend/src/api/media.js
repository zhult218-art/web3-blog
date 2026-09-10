// ============================================================
// 媒体中心接口（media-service，走网关 /api）
// 音乐 / 视频 / 书籍
// ============================================================
import request from './request'

// 分页查询音乐列表
export const getMusicList = params => request.get('/music/list', { params })
// 分页查询视频列表
export const getVideoList = params => request.get('/video/list', { params })
// 新增音乐
export const createMusic = data => request.post('/music', data)
// 分页查询书籍列表
export const getBookList = params => request.get('/book/list', { params })
// 书籍详情
export const getBookDetail = id => request.get(`/book/${id}`)
// 书籍章节目录
export const getBookChapters = id => request.get(`/book/${id}/chapters`)
// 书籍单章内容
export const getBookChapter = (id, chapterNo) => request.get(`/book/${id}/chapter/${chapterNo}`)
// 新增书籍
export const createBook = data => request.post('/book', data)
// 更新书籍
export const updateBook = (id, data) => request.put(`/book/${id}`, data)
// 删除书籍
export const deleteBook = id => request.delete(`/book/${id}`)
// 上传视频（FormData 文件上传）
export const uploadMedia = (file, config) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/video/upload', formData, { ...config })
}

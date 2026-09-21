// ============================================================
// 相册接口（photo-service，走网关 /api）
// 照片列表 / 上传 / 点赞 / 删除
// ============================================================
import request from './request'

// 分页查询照片列表
export const getPhotoList = params => request.get('/photo/list', { params })
// 新增照片记录（管理员）
export const createPhoto = data => request.post('/photo', data)
// 上传照片文件（管理员，FormData 上传；baseURL 已是 /api，勿手动设置 Content-Type）
export const uploadPhoto = (file, config) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/photo/upload', formData, { ...config })
}
// 点赞照片（返回最新点赞数）
export const likePhoto = id => request.put(`/photo/${id}/like`)
// 删除照片（管理员）
export const deletePhoto = id => request.delete(`/photo/${id}`)

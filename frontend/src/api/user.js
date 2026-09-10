// ============================================================
// 用户中心接口（user-service，走网关 /api）
// 登录 / 注册 / 资料 / 验证码 / OAuth / 后台用户管理 / 权限管理
// ============================================================
import request from './request'

// ───────── 基础认证 ─────────
// 密码登录：成功返回 { token, user }
export function login(username, password, captchaId, captchaCode) {
  return request.post('/user/login', { username, password, captchaId, captchaCode })
}
// 注册：成功返回 { token, user }
export function register(username, password, nickname, email) {
  return request.post('/user/register', { username, password, nickname, email })
}
// 获取图形验证码 { captchaId, image }
export const getCaptcha = () => request.get('/user/captcha')

// ───────── 邮箱验证码登录 ─────────
// 发送邮箱验证码（返回 { devCode } 当 mailEnabled=false 时）
export const sendEmailCode = (data) => request.post('/user/email/code', data)
// 邮箱验证码登录（成功自动注册）
export const emailLogin = (data) => request.post('/user/email/login', data)

// ───────── 手机验证码登录 ─────────
// 发送手机验证码
export const sendPhoneCode = (data) => request.post('/user/phone/code', data)
// 手机验证码登录（成功自动注册）
export const phoneLogin = (data) => request.post('/user/phone/login', data)

// ───────── Google OAuth ─────────
// 获取 Google 授权 URL
export const getGoogleUrl = () => request.get('/user/oauth/google/url')

// ───────── Supabase 免密登录（Magic Link）───
// 用 Supabase 访问令牌（后端经 JWKS 验签识别邮箱）换取本应用 JWT
export const supabaseLogin = data => request.post('/user/supabase/exchange', data)

// ───────── 绑定邮箱/手机 ─────────
// 发送绑定邮箱验证码
export const sendBindEmailCode = data => request.post('/user/bind/email/code', data)
// 绑定邮箱
export const bindEmail = data => request.post('/user/bind/email', data)
// 发送绑定手机验证码
export const sendBindPhoneCode = data => request.post('/user/bind/phone/code', data)
// 绑定手机
export const bindPhone = data => request.post('/user/bind/phone', data)

// ───────── 用户资料 ─────────
// 获取当前登录用户资料
export const getUserInfo = () => request.get('/user/profile')
// 用户总数（统计卡片使用）
export const getUserCount = () => request.get('/user/count')
// 更新当前用户资料
export const updateProfile = data => request.put('/user/profile', data)
// 更新头像
export const updateAvatar = avatar => request.put('/user/profile/avatar', { avatar })
// 选择异世界职业
export const chooseClass = userClass => request.put('/user/profile/class', { userClass })

// ───────── 后台用户管理 ─────────
// 后台：用户分页列表（支持 keyword 搜索）
export const getUserList = params => request.get('/user/list', { params })
// 后台：启用/禁用用户（status: 0 禁用 / 1 正常）
export const updateUserStatus = (id, status) => request.put(`/user/${id}/status`, null, { params: { status } })
// 后台：删除用户
export const deleteUser = id => request.delete(`/user/${id}`)
// 后台：创建用户（可指定角色 role: USER | ADMIN）
export const createUser = (data, role) => request.post('/user', data, { params: role ? { role } : {} })
// 后台：更新用户（可指定角色）
export const updateUser = (id, data, role) => request.put(`/user/${id}`, data, { params: role ? { role } : {} })
// 后台：查询用户权限项数组
export const getUserPermissions = id => request.get(`/user/${id}/permissions`)
// 会话续期：依赖后端 HttpOnly Cookie（页面刷新后静默恢复登录态）
export const refreshSession = () => request.post('/user/refresh', null, { suppressError: true, _isRefresh: true })
// 退出登录：作废后端 Redis 会话并清除 HttpOnly Cookie
export const logoutRemote = () => request.post('/user/logout', null, { suppressError: true })
// 后台：保存用户权限项数组
export const updateUserPermissions = (id, permissions) => request.put(`/user/${id}/permissions`, permissions)

// ============================================================
// 用户认证 Store（Pinia）
// 管理登录态 token / 用户信息，token 仅存内存（HttpOnly Cookie 负责持久会话）
// 刷新页面后由 tryRefresh() 经后端 /user/refresh 静默恢复
// 被登录页、导航栏、路由守卫及 App.vue 使用
// ============================================================
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  login as apiLogin,
  register as apiRegister,
  getUserInfo,
  updateProfile as apiUpdateProfile,
  emailLogin as apiEmailLogin,
  phoneLogin as apiPhoneLogin,
  refreshSession,
  logoutRemote
} from '@/api/user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const user = ref(null)

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value)

  // 更新用户信息（内存态）
  function setUser(value) {
    user.value = value
  }

  // 通用完成登录逻辑：接收后端返回的 { token, user } payload
  function setSession(payload) {
    if (!payload || !payload.token) {
      throw new Error('登录失败：未获取到令牌')
    }
    token.value = payload.token
    setUser(payload.user || null)
  }

  // 从响应中提取业务数据并校验 code
  function extractData(res) {
    const body = res.data !== undefined ? res : { data: res }
    if (body.code !== undefined && body.code !== 200) {
      throw new Error(body.message || '请求失败')
    }
    return body.data
  }

  // 密码登录（支持图形验证码）
  async function login(username, password, captchaId, captchaCode) {
    const res = await apiLogin(username, password, captchaId, captchaCode)
    const payload = extractData(res)
    setSession(payload)
    try { await fetchProfile() } catch {}
    return res
  }

  // 邮箱验证码登录
  async function emailLoginAction(email, code) {
    const res = await apiEmailLogin({ email, code })
    const payload = extractData(res)
    setSession(payload)
    try { await fetchProfile() } catch {}
    return res
  }

  // 手机验证码登录
  async function phoneLoginAction(phone, code) {
    const res = await apiPhoneLogin({ phone, code })
    const payload = extractData(res)
    setSession(payload)
    try { await fetchProfile() } catch {}
    return res
  }

  // 注册：成功后自动登录
  async function register(username, password, nickname, email) {
    const res = await apiRegister(username, password, nickname, email)
    const payload = extractData(res)
    setSession(payload)
    try { await fetchProfile() } catch {}
    return res
  }

  // OAuth 回调：直接使用 access_token 完成登录（无需再次请求后端）
  function completeWithToken(accessToken) {
    token.value = accessToken
    fetchProfile()
  }

  // 拉取当前用户资料
  async function fetchProfile() {
    if (!token.value) return user.value
    try {
      const res = await getUserInfo()
      setUser(res.data || res)
      return user.value
    } catch {
      return user.value
    }
  }

  // 会话续期：页面刷新后经 HttpOnly Cookie 静默恢复登录态
  async function tryRefresh() {
    if (token.value) return true
    try {
      const res = await refreshSession()
      const body = res && res.data !== undefined ? res : { data: res }
      if (body.code === 200 && body.data && body.data.token) {
        setSession(body.data)
        if (!body.data.user) {
          await fetchProfile()
        }
        return true
      }
    } catch {
      // 会话已过期：保持未登录态
    }
    return false
  }

  // 强制清除登录态（401 且续期失败时由 request 拦截器调用）
  function reset() {
    token.value = ''
    setUser(null)
  }

  // 更新用户资料并合并到本地缓存
  async function updateUser(data) {
    const res = await apiUpdateProfile(data)
    const updated = res.data || res
    setUser({ ...user.value, ...updated })
    return res
  }

  // 退出登录：通知后端作废会话并清除 HttpOnly Cookie，再清空本地登录态
  async function logout() {
    try {
      await logoutRemote()
    } catch {
      // 后端不可达时也要清理本地登录态
    }
    reset()
  }

  return {
    token, user, isLoggedIn,
    login, emailLogin: emailLoginAction, phoneLogin: phoneLoginAction,
    register, completeWithToken, setSession,
    tryRefresh, fetchProfile, reset, updateUser, logout
  }
})
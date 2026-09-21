// ============================================================
// Axios 请求封装（全局唯一实例，被 src/api/ 下所有接口模块使用）
// - baseURL 由 VITE_API_BASE 控制，默认 '/api'：
//     · 开发环境：由 Vite 代理转发到后端网关
//     · Cloudflare Pages 部署时：在控制台设 VITE_API_BASE 为后端绝对地址
//       （例如 https://api.your-domain.com），未设置时前端走相对 /api 会返回 404
// - 请求拦截器：从 auth Store（内存）读取 token 注入 Authorization 头
// - 响应拦截器：剥离响应体直接返回 data；出错时全局 Toast 提示；
//   401 时先经 HttpOnly Cookie 静默续期一次并重试原请求，续期失败才登出
// - 网络错误降级：部署到 Cloudflare Pages 但未配后端时，避免 toast 刷屏
// ============================================================
import axios from 'axios'
import router from '@/router'
import { useAuthStore } from '@/stores/modules/auth'

// 节流：同类网络错误在 5 秒内只弹一次 toast，避免后端不可用时刷屏
let lastNetErrorAt = 0
const NET_ERROR_THROTTLE_MS = 5000

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

// 请求拦截器：从 auth Store 读取 token 并注入 Authorization 头
request.interceptors.request.use(
  config => {
    const auth = useAuthStore()
    if (auth.token) {
      config.headers.Authorization = `Bearer ${auth.token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：成功时直接返回业务数据；失败时 Toast 提示、401 静默续期后重试
request.interceptors.response.use(
  response => response.data,
  async error => {
    const cfg = error.config || {}
    const is401 = error.response?.status === 401

    // 会话过期恢复：仅对普通业务请求触发一次，经刷新请求成功则重放原请求
    if (is401 && !cfg._isRefresh && !cfg._retried) {
      cfg._retried = true
      const auth = useAuthStore()
      const recovered = await auth.tryRefresh()
      if (recovered) {
        return request(cfg)
      }
    }

    if (!cfg.suppressError && !cfg._isRefresh) {
      // 网络错误降级：没有 response 的错误（连接失败/超时/未配后端）节流提示，
      // 避免在 Cloudflare Pages 部署但后端未上线时弹一堆 toast 刷屏
      const isNetError = !error.response
      if (isNetError) {
        const now = Date.now()
        if (now - lastNetErrorAt < NET_ERROR_THROTTLE_MS) return Promise.reject(error)
        lastNetErrorAt = now
      }
      const message = isNetError
        ? '网络异常：服务未上线或不可达'
        : (error.response?.data?.message || error.message || '请求失败')
      import('@/stores/modules/toast').then(({ useToastStore }) => {
        useToastStore().error(message)
      })
    }

    // 续期失败/未续期的 401：清空登录态并跳转登录页
    if (is401 && !cfg._isRefresh && !cfg._retried) {
      const auth = useAuthStore()
      auth.reset()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request
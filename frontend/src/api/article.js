// ============================================================
// 文章（书籍）接口封装 —— 供书桌模块（BookListModal / BookReader）使用
// 统一 baseURL 管理：修改 API_BASE 即可切换域名
//   开发环境走 Vite 代理 /api → 网关；生产环境同源 /api
// ============================================================
import axios from 'axios'
import { useAuthStore } from '@/stores/modules/auth'

// ⭐ 后续修改域名只需改这里（例如 'https://api.example.com/api'）
const API_BASE = import.meta.env.VITE_API_BASE || '/api'

const http = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
})

// 自动注入登录 token
http.interceptors.request.use(config => {
  try {
    const auth = useAuthStore()
    if (auth.token) config.headers.Authorization = `Bearer ${auth.token}`
  } catch {}
  return config
})

// 统一拆包后端信封 { code, data, message }
http.interceptors.response.use(
  res => res.data,
  err => Promise.reject(err)
)

/**
 * 获取文章（书籍）列表
 * 复用 GET /article/list，只取 id / title / cover / summary 字段
 * @param {object} params { page, size, keyword, category }
 * @returns {Promise<{records: Array, total: number}>}
 */
export async function fetchArticleList(params = {}) {
  const res = await http.get('/article/list', {
    params: { page: 1, size: 50, ...params },
  })
  // 兼容 { data: { records } } 与 { data: [] } 两种返回结构
  const payload = res?.data ?? res
  const records = payload?.records ?? (Array.isArray(payload) ? payload : [])
  return {
    records: records.map(a => ({
      id: a.id,
      title: a.title,
      cover: a.cover || a.coverImage || '',
      summary: a.summary || a.description || '',
    })),
    total: payload?.total ?? records.length,
  }
}

/**
 * 获取文章（书籍）详情 markdown 原文
 * GET /article/{id}
 * @param {number|string} id 文章 ID
 * @returns {Promise<{id:number, title:string, content:string, cover:string}>}
 */
export async function fetchArticleDetail(id) {
  const res = await http.get(`/article/${id}`)
  // 后端返回 { data: { article: {...} } } 或 { data: {...} }
  const payload = res?.data ?? res
  const article = payload?.article ?? payload
  return {
    id: article.id,
    title: article.title || '',
    cover: article.cover || article.coverImage || '',
    content: article.content || '',   // markdown 原文
    summary: article.summary || '',
  }
}

export default { fetchArticleList, fetchArticleDetail, API_BASE }

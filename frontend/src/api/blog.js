// ============================================================
// 博客文章接口
// 读取类（列表/详情/分类/标签/归档/统计/随机/相关/热门）优先从
// Supabase 读取（blog_articles，配合 RLS 只读已发布），失败或为空时
// 回退到 blog-service（走网关 /api）。
// 管理/写操作（增删改、点赞、收藏、设置、公告、友链）仍走后端。
// ============================================================
import request from './request'
import { supabase } from '@/lib/supabase/client'

// 后端信封：{ code, data, message }（与 request 拦截器返回形状一致）
const ok = data => ({ code: 200, message: 'success', data })

// Supabase 行（snake_case）→ 前端 VO（camelCase，含旧字段兼容）
function mapArticle(r) {
  const tags = Array.isArray(r.tags) ? r.tags : []
  return {
    id: Number(r.id),
    title: r.title,
    slug: r.slug,
    summary: r.summary || '',
    content: r.content || '',
    category: r.category || '',
    tags: tags.join(',') || '',
    tagsArr: tags,
    cover: r.cover_url || '',
    coverImage: r.cover_url || '',
    createdAt: r.created_at,
    updatedAt: r.updated_at,
    createTime: r.created_at,
    updateTime: r.updated_at,
    author: r.author_name || '',
    authorName: r.author_name || '',
    viewCount: r.views ?? 0,
    likeCount: r.likes ?? 0,
    isTop: r.is_top === true,
  }
}

const kw = v => String(v || '').replace(/[%']/g, '')

// 分页查询文章列表（支持 page/size/keyword/category/tag/month）
export const getBlogList = async params => {
  const p = { ...(params || {}) }
  try {
    let q = supabase.from('blog_articles').select('*', { count: 'exact' }).eq('published', true)
    if (p.keyword) {
      const k = kw(p.keyword)
      if (k) q = q.or(`title.ilike.%${k}%,summary.ilike.%${k}%,content.ilike.%${k}%`)
    }
    if (p.category) q = q.eq('category', String(p.category))
    if (p.tag) q = q.contains('tags', [String(p.tag)])
    if (p.month) {
      const [y, m] = String(p.month).split('-')
      if (y && m) {
        q = q.gte('created_at', `${y}-${m}-01T00:00:00Z`)
        q = q.lt('created_at', new Date(Date.UTC(Number(y), Number(m), 1)).toISOString())
      }
    }
    const page = Number(p.page) || 1
    const size = Number(p.size) || 10
    q = q.order('created_at', { ascending: false }).range((page - 1) * size, page * size - 1)
    const { data, count, error } = await q
    if (error) throw error
    const records = (data || []).map(mapArticle)
    if (!records.length) throw new Error('empty')
    return ok({ records, total: count ?? records.length, page, size })
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogList supabase 不可用，回退后端', e?.message || e)
    return request.get('/article/list', { params: p })
  }
}

// 查询文章详情（返回 { article, prevArticle, nextArticle }，与后端一致）
export const getBlogDetail = async id => {
  try {
    const { data: a, error } = await supabase
      .from('blog_articles')
      .select('*')
      .eq('published', true)
      .eq('id', Number(id))
      .limit(1)
      .maybeSingle()
    if (error) throw error
    if (!a) throw new Error('empty')
    const article = mapArticle(a)
    const [prev, next] = await Promise.all([
      supabase
        .from('blog_articles')
        .select('id, title')
        .eq('published', true)
        .lt('created_at', a.created_at)
        .order('created_at', { ascending: false })
        .limit(1)
        .maybeSingle(),
      supabase
        .from('blog_articles')
        .select('id, title')
        .eq('published', true)
        .gt('created_at', a.created_at)
        .order('created_at', { ascending: true })
        .limit(1)
        .maybeSingle(),
    ])
    return ok({
      article,
      prevArticle: prev.data || null,
      nextArticle: next.data || null,
    })
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogDetail supabase 不可用，回退后端', e?.message || e)
    return request.get(`/article/${id}`)
  }
}

// 相关推荐（同分类、浏览量优先）
export const getBlogRelated = async (id, limit = 5) => {
  try {
    const { data: self, error: e1 } = await supabase
      .from('blog_articles')
      .select('category')
      .eq('id', Number(id))
      .limit(1)
      .maybeSingle()
    if (e1) throw e1
    let target = { category: '' }
    if (self?.category) {
      const { data: same } = await supabase
        .from('blog_articles')
        .select('*')
        .eq('published', true)
        .eq('category', self.category)
        .neq('id', Number(id))
        .order('views', { ascending: false })
        .limit(Number(limit) || 5)
      if (same?.length) target = same
      else target = { related: 'empty', rows: [] }
    }
    const rows = Array.isArray(target) ? target : []
    if (!rows.length) throw new Error('empty')
    return ok(rows.map(mapArticle))
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogRelated supabase 不可用，回退后端', e?.message || e)
    return request.get(`/article/${id}/related`, { params: { limit } })
  }
}

// 随机一篇（保证返回的顶层带 id，供 layout/右键/控制台跳转）
export const getBlogRandom = async () => {
  try {
    const { data: ids, error } = await supabase
      .from('blog_articles')
      .select('id')
      .eq('published', true)
    if (error) throw error
    if (!ids?.length) throw new Error('empty')
    const pick = ids[Math.floor(Math.random() * ids.length)]
    const { data: a, error: e2 } = await supabase
      .from('blog_articles')
      .select('*')
      .eq('id', pick.id)
      .single()
    if (e2) throw e2
    return ok(mapArticle(a))
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogRandom supabase 不可用，回退后端', e?.message || e)
    return request.get('/article/random')
  }
}

// 归档（按年月分组，倒序）
export const getBlogArchives = async () => {
  try {
    const { data, error } = await supabase
      .from('blog_articles')
      .select('created_at')
      .eq('published', true)
    if (error) throw error
    const counts = {}
    ;(data || []).forEach(r => {
      const month = String(r.created_at || '').slice(0, 7)
      if (month) counts[month] = (counts[month] || 0) + 1
    })
    const rows = Object.entries(counts)
      .map(([month, count]) => ({ month, count }))
      .sort((x, y) => y.month.localeCompare(x.month))
    if (!rows.length) throw new Error('empty')
    return ok(rows)
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogArchives supabase 不可用，回退后端', e?.message || e)
    return request.get('/article/archives')
  }
}

// 分类列表 [{ name, count }]
export const getBlogCategories = async () => {
  try {
    const { data, error } = await supabase
      .from('blog_articles')
      .select('category')
      .eq('published', true)
    if (error) throw error
    const counts = {}
    ;(data || []).forEach(r => {
      if (r.category) counts[r.category] = (counts[r.category] || 0) + 1
    })
    const rows = Object.entries(counts)
      .map(([name, count]) => ({ name, count }))
      .sort((x, y) => y.count - x.count || x.name.localeCompare(y.name))
    if (!rows.length) throw new Error('empty')
    return ok(rows)
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogCategories supabase 不可用，回退后端', e?.message || e)
    return request.get('/article/categories')
  }
}

// 标签列表（含出现次数）
export const getBlogTags = async () => {
  try {
    const { data, error } = await supabase
      .from('blog_articles')
      .select('tags')
      .eq('published', true)
    if (error) throw error
    const counts = {}
    ;(data || []).forEach(r => {
      if (Array.isArray(r.tags)) {
        r.tags.forEach(t => { if (t) counts[String(t)] = (counts[String(t)] || 0) + 1 })
      }
    })
    const rows = Object.entries(counts)
      .map(([name, count]) => ({ name, count }))
      .sort((x, y) => y.count - x.count || x.name.localeCompare(y.name))
    if (!rows.length) throw new Error('empty')
    return ok(rows)
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogTags supabase 不可用，回退后端', e?.message || e)
    return request.get('/article/tags')
  }
}

// 站点资讯（文章总数/全站字数/最后更新）
export const getBlogStats = async () => {
  try {
    const { data, error } = await supabase
      .from('blog_articles')
      .select('content, updated_at, created_at')
      .eq('published', true)
    if (error) throw error
    const rows = data || []
    const totalArticles = rows.length
    const totalWords = rows.reduce((s, r) => s + (r.content?.length || 0), 0)
    const lastUpdated = rows.reduce((m, r) => {
      const t = r.updated_at || r.created_at
      return t && (!m || t > m) ? t : m
    }, null)
    return ok({
      totalArticles,
      totalPosts: totalArticles,
      totalWords,
      lastUpdated,
      lastUpdate: lastUpdated,
    })
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogStats supabase 不可用，回退后端', e?.message || e)
    return request.get('/article/stats')
  }
}

// 最近更新
export const getBlogRecent = async (limit = 5) => {
  try {
    const { data, error } = await supabase
      .from('blog_articles')
      .select('id, title, updated_at, created_at')
      .eq('published', true)
      .order('updated_at', { ascending: false })
      .limit(Number(limit) || 5)
    if (error) throw error
    const rows = (data || []).map(r => ({
      id: Number(r.id),
      title: r.title,
      createdAt: r.created_at,
      updatedAt: r.updated_at,
    }))
    if (!rows.length) throw new Error('empty')
    return ok(rows)
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogRecent supabase 不可用，回退后端', e?.message || e)
    return request.get('/article/recent', { params: { limit } })
  }
}

// 热门文章榜（首页使用）
export const getHotArticles = async (limit = 5) => {
  try {
    const { data, error } = await supabase
      .from('blog_articles')
      .select('*')
      .eq('published', true)
      .order('views', { ascending: false })
      .limit(Number(limit) || 5)
    if (error) throw error
    const rows = (data || []).map(mapArticle)
    if (!rows.length) throw new Error('empty')
    return ok(rows.map(({ content, ...rest }) => rest))
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getHotArticles supabase 不可用，回退后端', e?.message || e)
    return request.get('/article/hot', { params: { limit } })
  }
}

// ---------- 以下均为后端（blog-service / forum-service 公共接口）----------

// 新建文章
export const createBlog = data => request.post('/article', data)
// 更新文章
export const updateBlog = (id, data) => request.put(`/article/${id}`, data)
// 删除文章
export const deleteBlog = id => request.delete(`/article/${id}`)
// 管理端：全量文章列表（含未发布，仅后端）
export const getAdminBlogList = params => request.get('/article/list', { params })
// 点赞文章
export const likeBlog = id => request.post(`/article/${id}/like`)
// 收藏/取消收藏文章
export const favoriteBlog = id => request.post(`/article/${id}/favorite`)
// 检查是否已收藏
export const checkBlogFavorite = id => request.get(`/article/${id}/favorite/check`, { suppressError: true })
// 检查是否已点赞
export const checkBlogLike = id => request.get(`/article/${id}/like/check`, { suppressError: true })
// 获取当前用户收藏的文章
export const getUserFavorites = params => request.get('/article/user/favorites', { params })
// 获取当前用户点赞的文章
export const getUserLikes = params => request.get('/article/user/likes', { params })
// 友链列表
export const getBlogLinks = (group = '') => request.get('/blog/links', { params: { group } })
// 申请友链
export const applyBlogLink = data => request.post('/blog/links/apply', data)
// 当前公告
export const getBlogNotice = () => request.get('/blog/notice')
// 公开公告列表（历史公告，启用中倒序）
export const getBlogNotices = () => request.get('/blog/notice/list')
// 站点公开配置（作者/打赏/页脚友链/ICP）
export const getBlogSettings = () => request.get('/blog/settings')
// 管理端：友链全部列表（含待审核）
export const getAdminLinks = () => request.get('/blog/admin/links')
// 管理端：友链编辑/审核
export const updateAdminLink = (id, data) => request.put(`/blog/admin/links/${id}`, data)
// 管理端：友链新增
export const createAdminLink = data => request.post('/blog/admin/links', data)
// 管理端：友链删除
export const deleteAdminLink = id => request.delete(`/blog/admin/links/${id}`)
// 管理端：公告保存/新增
export const saveAdminNotice = data => request.put('/blog/admin/notice', data)
// 管理端：公告列表
export const getAdminNotices = () => request.get('/blog/admin/notice/list')
// 管理端：公告删除
export const deleteAdminNotice = id => request.delete(`/blog/admin/notice/${id}`)
// 管理端：站点配置批量保存
export const saveAdminSettings = data => request.put('/blog/admin/settings', data)
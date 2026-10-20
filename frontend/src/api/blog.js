// ============================================================
// 博客文章接口
// 读取类（列表/详情/分类/标签/归档/统计/随机/相关/热门）优先从
// blog-service（走网关 /api，本地 MySQL，毫秒级）读取，失败或为空时
// 回退到 Supabase（blog_articles，云端镜像；国内网络可达性差，仅兜底）。
// 管理/写操作（增删改、点赞、收藏、设置、公告、友链）仍走后端。
// ============================================================
import request from './request'
import { supabase } from '@/lib/supabase/client'

// 后端信封：{ code, data, message }（与 request 拦截器返回形状一致）
const ok = data => ({ code: 200, message: 'success', data })

// RSS 订阅源地址：由 blog-service 动态生成（/blog/feed.xml）。
// 生产环境后端为绝对地址（VITE_API_BASE），故此处按同一基准拼接，保证 feed 链接可直达。
const API_BASE = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '')
export const FEED_URL = `${API_BASE}/blog/feed.xml`

// ============================================================
// 元数据共享缓存（列表页 / 侧栏 / 归档页共享同一份数据）
// - in-flight 去重：同一时刻多个组件发起相同请求只打一次网络
// - TTL 内直接命中；过期先返回旧值并后台静默刷新（stale-while-revalidate）
// - sessionStorage 持久化：刷新页面 / 从详情页返回时秒开
// ============================================================
const SS_PREFIX = 'blog_meta:'
const metaStore = new Map() // key -> { t, v }
const inflight = new Map() // key -> Promise
const refreshing = new Set()

function restore(key) {
  if (metaStore.has(key)) return
  try {
    const raw = sessionStorage.getItem(SS_PREFIX + key)
    if (raw) {
      const o = JSON.parse(raw)
      if (o && o.t && o.v) metaStore.set(key, o)
    }
  } catch { /* 隐私模式等 */ }
}

function persist(key) {
  const o = metaStore.get(key)
  if (!o) return
  try { sessionStorage.setItem(SS_PREFIX + key, JSON.stringify(o)) } catch { /* 配额满等 */ }
}

// 后台静默刷新，不阻塞调用方、不产生未处理异常
function refreshInBg(key, loader) {
  if (refreshing.has(key) || inflight.has(key)) return
  refreshing.add(key)
  Promise.resolve().then(loader)
    .then(v => { metaStore.set(key, { t: Date.now(), v }); persist(key) })
    .catch(() => {})
    .then(() => refreshing.delete(key))
}

function withCache(key, ttl, loader) {
  restore(key)
  const hit = metaStore.get(key)
  if (hit) {
    if (Date.now() - hit.t >= ttl) refreshInBg(key, loader)
    return Promise.resolve(hit.v)
  }
  if (inflight.has(key)) return inflight.get(key)
  const p = Promise.resolve().then(loader)
    .then(v => {
      metaStore.set(key, { t: Date.now(), v })
      persist(key)
      inflight.delete(key)
      return v
    }, e => { inflight.delete(key); throw e })
  inflight.set(key, p)
  return p
}

/** 清除元数据缓存（不传 key 清全部；管理员写操作后调用） */
export function clearBlogMetaCache(keys) {
  try {
    if (!keys) {
      Array.from(sessionStorage)
        .filter(([k]) => k.startsWith(SS_PREFIX))
        .forEach(([k]) => sessionStorage.removeItem(k))
    } else {
      keys.forEach(k => sessionStorage.removeItem(SS_PREFIX + k))
    }
  } catch { /* ignore */ }
  if (!keys) metaStore.clear()
  else keys.forEach(k => metaStore.delete(k))
}

// 文章缺少封面时，按主题匹配本地高质感 SVG（统一品红/电光青视觉）
// 无关键词命中时按 id 稳定取图，保证同一文章每次封面一致
const FALLBACK_COVER_IDS = [34, 35, 36, 37, 38, 39]
function fallbackCover(r) {
  const s = `${r.title || ''} ${r.category || ''}`
  let id
  if (/博客|搭建|入门|portal/i.test(s)) id = 34          // 传送门圆环
  else if (/supabase|数据库|接入|api|浏览器|后端/i.test(s)) id = 37 // 浏览器窗口
  else if (/代码|编程|开发|技术|vue|前端|工程/i.test(s)) id = 35 // 代码尖括号
  else if (/defi|金融|币|交易|流动性/i.test(s)) id = 36  // DeFi 三币互联
  else if (/nft|数字藏品|藏品|艺术/i.test(s)) id = 38    // NFT 三卡叠
  else if (/安全|加密|防护|隐私|盾/i.test(s)) id = 39    // 盾牌勾
  else id = FALLBACK_COVER_IDS[Math.abs(Number(r.id) || 0) % FALLBACK_COVER_IDS.length]
  return `/images/blog/cover-${id}.svg`
}

// Supabase 行（snake_case）→ 前端 VO（camelCase，含旧字段兼容）
function mapArticle(r) {
  const tags = Array.isArray(r.tags) ? r.tags : []
  const cover = r.cover_url || fallbackCover(r)
  return {
    id: Number(r.id),
    title: r.title,
    slug: r.slug,
    summary: r.summary || '',
    content: r.content || '',
    category: r.category || '',
    tags: tags.join(',') || '',
    tagsArr: tags,
    cover,
    coverImage: cover,
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

// 列表页精简列：不拉取正文（content 是最大的载荷），阅读时长在卡片侧按摘要估算
const LIST_COLS = 'id,title,slug,summary,category,tags,cover_url,author_name,created_at,updated_at,views,likes,is_top'

// 分页查询文章列表（支持 page/size/keyword/category/tag/month）
// 后端优先；请求失败或返回空列表时回退 Supabase 云端镜像
const fetchBlogList = async params => {
  const p = { ...(params || {}) }
  try {
    const res = await request.get('/article/list', { params: p })
    if (!res?.data?.records?.length) throw new Error('empty')
    return res
  } catch (be) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogList 后端不可用，回退 Supabase', be?.message || be)
  }
  const page = Number(p.page) || 1
  const size = Number(p.size) || 10
  try {
    let q = supabase.from('blog_articles').select(LIST_COLS, { count: 'exact' }).eq('published', true)
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
    q = q.order('created_at', { ascending: false }).range((page - 1) * size, page * size - 1)
    const { data, count, error } = await q
    if (error) throw error
    return ok({ records: (data || []).map(mapArticle), total: count ?? (data || []).length, page, size })
  } catch (e) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogList supabase 也不可用', e?.message || e)
    return ok({ records: [], total: 0, page, size })
  }
}

// 列表短时缓存（60s SWR）：从详情页返回 / 重复进入时秒开，过期后台静默刷新
// key 含全部查询参数，避免不同筛选互相串数据
const listCacheKey = p => 'list:' + JSON.stringify({
  page: Number(p.page) || 1,
  size: Number(p.size) || 10,
  keyword: p.keyword || '',
  category: p.category || '',
  tag: p.tag || '',
  month: p.month || '',
})
export const getBlogList = (params) => {
  const p = params || {}
  return withCache(listCacheKey(p), MIN, () => fetchBlogList(p))
}

// 查询文章详情（返回 { article, prevArticle, nextArticle }，与后端一致）
// 后端优先；失败或文章不存在时回退 Supabase
export const getBlogDetail = async id => {
  try {
    const res = await request.get(`/article/${id}`)
    if (!res?.data?.article) throw new Error('empty')
    return res
  } catch (be) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogDetail 后端不可用，回退 Supabase', be?.message || be)
  }
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
}

// 相关推荐（同分类、浏览量优先）
// 后端优先；失败或无结果时回退 Supabase
export const getBlogRelated = async (id, limit = 5) => {
  try {
    const res = await request.get(`/article/${id}/related`, { params: { limit } })
    const rows = res?.data
    if (!Array.isArray(rows) || !rows.length) throw new Error('empty')
    return res
  } catch (be) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogRelated 后端不可用，回退 Supabase', be?.message || be)
  }
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
  return ok(rows.map(mapArticle))
}

// 随机一篇（保证返回的顶层带 id，供 layout/右键/控制台跳转）
// 后端优先；失败时回退 Supabase
export const getBlogRandom = async () => {
  try {
    const res = await request.get('/article/random')
    if (!res?.data?.id) throw new Error('empty')
    return res
  } catch (be) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogRandom 后端不可用，回退 Supabase', be?.message || be)
  }
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
}

// 归档（按年月分组，倒序）
// 后端优先；失败或为空时回退 Supabase
const fetchArchives = async () => {
  try {
    const res = await request.get('/article/archives')
    if (!res?.data?.length) throw new Error('empty')
    return res
  } catch (be) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogArchives 后端不可用，回退 Supabase', be?.message || be)
  }
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
  return ok(
    Object.entries(counts)
      .map(([month, count]) => ({ month, count }))
      .sort((x, y) => y.month.localeCompare(x.month))
  )
}

// 分类列表 [{ name, count }]
// 后端优先；失败或为空时回退 Supabase
const fetchCategories = async () => {
  try {
    const res = await request.get('/article/categories')
    if (!res?.data?.length) throw new Error('empty')
    return res
  } catch (be) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogCategories 后端不可用，回退 Supabase', be?.message || be)
  }
  const { data, error } = await supabase
    .from('blog_articles')
    .select('category')
    .eq('published', true)
  if (error) throw error
  const counts = {}
  ;(data || []).forEach(r => {
    if (r.category) counts[r.category] = (counts[r.category] || 0) + 1
  })
  return ok(
    Object.entries(counts)
      .map(([name, count]) => ({ name, count }))
      .sort((x, y) => y.count - x.count || x.name.localeCompare(y.name))
  )
}

// 标签列表（含出现次数）
// 后端优先；失败或为空时回退 Supabase
const fetchTags = async () => {
  try {
    const res = await request.get('/article/tags')
    if (!res?.data?.length) throw new Error('empty')
    return res
  } catch (be) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogTags 后端不可用，回退 Supabase', be?.message || be)
  }
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
  return ok(
    Object.entries(counts)
      .map(([name, count]) => ({ name, count }))
      .sort((x, y) => y.count - x.count || x.name.localeCompare(y.name))
  )
}

// 站点资讯（文章总数/全站字数/最后更新）
// 后端优先；失败时回退 Supabase
const fetchStats = async () => {
  try {
    const res = await request.get('/article/stats')
    if (!res?.data || res.data.totalArticles == null) throw new Error('empty')
    return res
  } catch (be) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogStats 后端不可用，回退 Supabase', be?.message || be)
  }
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
}

// 最近更新
// 后端优先；失败或为空时回退 Supabase
const fetchRecent = async (limit = 5) => {
  try {
    const res = await request.get('/article/recent', { params: { limit } })
    if (!res?.data?.length) throw new Error('empty')
    return res
  } catch (be) {
    if (import.meta.env.DEV) console.warn('[blog] getBlogRecent 后端不可用，回退 Supabase', be?.message || be)
  }
  const { data, error } = await supabase
    .from('blog_articles')
    .select('id, title, updated_at, created_at')
    .eq('published', true)
    .order('updated_at', { ascending: false })
    .limit(Number(limit) || 5)
  if (error) throw error
  return ok((data || []).map(r => ({
    id: Number(r.id),
    title: r.title,
    createdAt: r.created_at,
    updatedAt: r.updated_at,
  })))
}

// 热门文章榜（首页使用）
// 后端优先；失败或为空时回退 Supabase
export const getHotArticles = async (limit = 5) => {
  try {
    const res = await request.get('/article/hot', { params: { limit } })
    if (!res?.data?.length) throw new Error('empty')
    return res
  } catch (be) {
    if (import.meta.env.DEV) console.warn('[blog] getHotArticles 后端不可用，回退 Supabase', be?.message || be)
  }
  const { data, error } = await supabase
    .from('blog_articles')
    .select('*')
    .eq('published', true)
    .order('views', { ascending: false })
    .limit(Number(limit) || 5)
  if (error) throw error
  return ok((data || []).map(mapArticle).map(({ content, ...rest }) => rest))
}

// ============================================================
// 缓存包装：所有元数据读取统一走 withCache
// ============================================================
const MIN = 60 * 1000
export const getBlogCategories = () => withCache('categories', 10 * MIN, fetchCategories)
export const getBlogTags = () => withCache('tags', 10 * MIN, fetchTags)
export const getBlogArchives = () => withCache('archives', 10 * MIN, fetchArchives)
export const getBlogRecent = (limit = 5) =>
  withCache(`recent:${Number(limit) || 5}`, 3 * MIN, () => fetchRecent(limit))
// 统计需拉取全站正文，最重：缓存 30 分钟（SWR 后台静默更新）
export const getBlogStats = () => withCache('stats', 30 * MIN, fetchStats)
export const getBlogSettings = () => withCache('settings', 10 * MIN, fetchSettings)

/** 悬停「博客」导航时预热：分类 + 统计 + 列表页 chunk，点击即秒开 */
export function prefetchBlogMeta() {
  try {
    getBlogCategories()
    getBlogStats()
    import('@/views/blog/index.vue')
  } catch { /* ignore */ }
}

// ---------- 以下均为后端（blog-service / forum-service 公共接口）----------

// 写操作成功后清空元数据缓存（简单稳妥，下个页面自动重取）
const bumpMeta = p => p.then(
  r => { clearBlogMetaCache(); return r },
  e => { throw e }
)
// 新建文章
export const createBlog = data => bumpMeta(request.post('/article', data))
// 更新文章
export const updateBlog = (id, data) => bumpMeta(request.put(`/article/${id}`, data))
// 删除文章
export const deleteBlog = id => bumpMeta(request.delete(`/article/${id}`))
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
// trae-preview 环境 axios XHR 在页面加载爆发期会被挤占挂起，NoticePanel 全局轮询改用原生 fetch；
// 中止定时器覆盖到响应体读完，防止 body 卡住导致 res.json() 永久挂起
export const getBlogNotices = async () => {
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), 15000)
  try {
    const res = await fetch('/api/blog/notice/list', { signal: ctrl.signal })
    if (!res.ok) throw new Error(`公告列表请求失败 (${res.status})`)
    return await res.json()
  } finally {
    clearTimeout(timer)
  }
}
// 站点公开配置（作者/打赏/页脚友链/ICP）
const fetchSettings = () => request.get('/blog/settings')
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
export const saveAdminSettings = data => bumpMeta(request.put('/blog/admin/settings', data))
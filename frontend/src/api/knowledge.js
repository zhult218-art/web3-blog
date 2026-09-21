// ============================================================
// 知识库 API —— 数据存放在 Supabase（表：kb_entries）
// 设计：
//   - 读取：匿名只读 published=true（RLS 保证）
//   - 写入：站主用 Supabase Auth 登录后可全部 CRUD（RLS authenticated）
//   - 成长模型：stage = seedling/growing/mastered（🌱→🌿→🌳）
//   - 复习调度：艾宾浩斯 / SM-2 简化算法，next_review 到期即进入"今日复习"
//   - 发布到博客：站主可把 kb_entry 直接写入 blog_articles 表
// ============================================================
import { supabase } from '@/lib/supabase/client'
// 初始化 SQL（Vite ?raw 把文件内容作为字符串导入，供一键复制）
import initSqlRaw from '../../../supabase/0001_knowledge_and_blog_seed.sql?raw'

export const INIT_SQL = initSqlRaw

const TABLE = 'kb_entries'

export function mapRow(r) {
  if (!r) return null
  return {
    id: r.id,
    title: r.title || '',
    slug: r.slug || '',
    summary: r.summary || '',
    content: r.content || '',
    category: r.category || '',
    subcategory: r.subcategory || '',
    tags: Array.isArray(r.tags) ? r.tags : [],
    stage: r.stage || 'seedling',
    source: r.source || '',
    sourceUrl: r.source_url || '',
    difficulty: r.difficulty ?? 1,
    viewCount: r.view_count ?? 0,
    reviewCount: r.review_count ?? 0,
    reviewInterval: r.review_interval ?? 1,
    easeFactor: Number(r.ease_factor ?? 2.5),
    nextReview: r.next_review || null,
    lastReview: r.last_review || null,
    createdAt: r.created_at,
    updatedAt: r.updated_at,
  }
}

// 判断 Supabase 中 kb_entries 表是否已创建（用于引导用户执行 SQL 初始化）
export async function isTableReady() {
  try {
    const { error } = await supabase.from(TABLE).select('id').limit(1)
    if (!error) return true
    // 表不存在的典型错误："Could not find the table 'public.kb_entries' in the schema cache"
    return !/kb_entries/.test(String(error.message || ''))
  } catch {
    return false
  }
}

/**
 * 查询知识条目（已发布）
 * @param {{category?:string, subcategory?:string, stage?:string, keyword?:string, due?:boolean}} opts
 *   due=true 只取 next_review <= now 的到期复习条目
 */
export async function listEntries(opts = {}) {
  let q = supabase.from(TABLE).select('*').eq('published', true)
  if (opts.due) q = q.lte('next_review', new Date().toISOString())
  if (opts.category) q = q.eq('category', opts.category)
  if (opts.subcategory) q = q.eq('subcategory', opts.subcategory)
  if (opts.stage) q = q.eq('stage', opts.stage)
  if (opts.keyword) {
    const k = String(opts.keyword).replace(/[%']/g, '')
    q = q.or(`title.ilike.%${k}%,summary.ilike.%${k}%,content.ilike.%${k}%`)
  }
  q = q.order(opts.due ? 'next_review' : 'updated_at', { ascending: !!opts.due })
  const { data, error } = await q
  if (error) throw error
  return (data || []).map(mapRow)
}

/** 知识详情（已发布） */
export async function getEntry(id) {
  const { data, error } = await supabase
    .from(TABLE).select('*').eq('id', Number(id)).eq('published', true).maybeSingle()
  if (error) throw error
  return mapRow(data)
}

/** 今日应复习条目数量 */
export async function getDueCount() {
  const now = new Date().toISOString()
  const { count, error } = await supabase
    .from(TABLE).select('id', { count: 'exact', head: true })
    .eq('published', true).lte('next_review', now)
  if (error) return 0
  return count || 0
}

/** 按分类聚合的成长统计：每类的总数/各阶段数量 */
export async function getGrowthStats() {
  const { data, error } = await supabase.from(TABLE).select('category,stage').eq('published', true)
  if (error) throw error
  const byCat = {}
  let total = 0, mastered = 0
  for (const r of data || []) {
    total++
    if (r.stage === 'mastered') mastered++
    if (!byCat[r.category]) byCat[r.category] = { total: 0, seedling: 0, growing: 0, mastered: 0 }
    byCat[r.category].total++
    byCat[r.category][r.stage || 'seedling']++
  }
  return { total, mastered, masteryRate: total ? Math.round((mastered / total) * 100) : 0, byCat }
}

// ============ 站主管理（需 Supabase Auth 登录） ============
export async function ownerLogin(email, password) {
  const { data, error } = await supabase.auth.signInWithPassword({ email, password })
  if (error) throw error
  return data.user
}
export function ownerLogout() { return supabase.auth.signOut() }
export function getOwnerUser() { return supabase.auth.getUser() }
export function onAuthChange(cb) {
  const { data } = supabase.auth.onAuthStateChange((_e, session) => cb(session?.user || null))
  return () => data.subscription.unsubscribe()
}

/** 管理态：全部条目（含草稿） */
export async function adminListEntries() {
  const { data, error } = await supabase.from(TABLE).select('*').order('updated_at', { ascending: false })
  if (error) throw error
  return (data || []).map(mapRow)
}

/** 新建条目 */
export async function createEntry(payload) {
  const row = toRow(payload)
  const { data, error } = await supabase.from(TABLE).insert(row).select().single()
  if (error) throw error
  return mapRow(data)
}

/** 更新条目 */
export async function updateEntry(id, payload) {
  const row = toRow(payload)
  const { data, error } = await supabase.from(TABLE).update(row).eq('id', id).select().single()
  if (error) throw error
  return mapRow(data)
}

/** 删除条目 */
export async function deleteEntry(id) {
  const { error } = await supabase.from(TABLE).delete().eq('id', id)
  if (error) throw error
}

/** 推进成长阶段（🌱→🌿→🌳） */
export async function advanceStage(entry) {
  const next = entry.stage === 'seedling' ? 'growing' : entry.stage === 'growing' ? 'mastered' : 'seedling'
  return updateEntry(entry.id, { ...entry, stage: next })
}

/**
 * 艾宾浩斯 / SM-2 简化复习调度
 * @param {object} entry 当前条目
 * @param {number} quality 自评质量 0-5（0 完全忘记，5 轻松回忆）
 *   - quality < 3：间隔重置为 1 天，EF 降低，不算"记住"
 *   - quality >= 3：间隔按 EF 增长，EF 微调
 */
export async function scheduleReview(entry, quality) {
  let { reviewCount: rc = 0, reviewInterval: iv = 1, easeFactor: ef = 2.5 } = entry
  ef = Math.max(1.3, ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)))
  if (quality < 3) {
    rc = 0
    iv = 1
  } else {
    rc += 1
    if (rc === 1) iv = 1
    else if (rc === 2) iv = 3
    else iv = Math.round(iv * ef)
  }
  const next = new Date(Date.now() + iv * 24 * 3600 * 1000)
  const { data, error } = await supabase.from(TABLE).update({
    review_count: rc,
    review_interval: iv,
    ease_factor: ef,
    next_review: next.toISOString(),
    last_review: new Date().toISOString(),
  }).eq('id', entry.id).select().single()
  if (error) throw error
  return mapRow(data)
}

/**
 * 把知识条目发布为博客文章（直接写入 Supabase blog_articles，站主操作）
 * 返回新博客 id
 */
export async function publishToBlog(entry, opts = {}) {
  const slug = opts.slug || entry.slug || `kb-${entry.id}`
  const row = {
    title: opts.title || entry.title,
    slug,
    summary: opts.summary || entry.summary || entry.content.slice(0, 120),
    content: opts.content || wrapAsBlog(entry),
    category: opts.category || entry.category || '软件工程',
    tags: opts.tags || (Array.isArray(entry.tags) ? entry.tags : []),
    author_name: opts.author || '极光星途',
    cover_url: opts.coverUrl || null,
    is_top: opts.isTop ? true : false,
    published: opts.published !== false,
    views: 0, likes: 0,
  }
  const { data, error } = await supabase.from('blog_articles').insert(row).select('id,slug').single()
  if (error) throw error
  return data
}

/** 将 kb_entry 内容转换为博客正文格式（加头部信息 + 原内容） */
function wrapAsBlog(entry) {
  const head = [
    `> 📚 本文由知识库条目「${entry.title}」整理发布`,
    entry.summary ? `> ${entry.summary}` : '',
    entry.subcategory ? `> 分类：${entry.category} · ${entry.subcategory}` : `> 分类：${entry.category}`,
    '',
    '---',
    '',
  ].filter(Boolean).join('\n')
  return head + (entry.content || '')
}

/** 导出单条为 Markdown 文本 */
export function entryToMarkdown(entry) {
  const lines = [
    `# ${entry.title}`,
    '',
    entry.summary ? `> ${entry.summary}` : '',
    '',
    `**分类**：${entry.category}${entry.subcategory ? ' · ' + entry.subcategory : ''}`,
    `**难度**：${'★'.repeat(entry.difficulty)}${'☆'.repeat(5 - entry.difficulty)}`,
    entry.tags?.length ? `**标签**：${entry.tags.map(t => '#' + t).join(' ')}` : '',
    entry.source ? `**来源**：${entry.source}${entry.sourceUrl ? ' - ' + entry.sourceUrl : ''}` : '',
    '',
    '---',
    '',
    entry.content || '',
  ]
  return lines.filter(l => l !== undefined).join('\n')
}

/** 批量导出知识库为单一 Markdown 文件（用于备份） */
export async function exportAllToMarkdown() {
  const list = await listEntries()
  const parts = list.map(e => {
    return `# ${e.title}\n\n> 分类：${e.category}${e.subcategory ? ' · ' + e.subcategory : ''}\n\n${e.summary || ''}\n\n${e.content || ''}\n\n---\n\n`
  })
  return `# 我的知识库 全量导出\n\n> 导出时间：${new Date().toLocaleString('zh-CN')}\n> 共 ${list.length} 条\n\n---\n\n` + parts.join('\n')
}

function toRow(p) {
  return {
    title: p.title,
    slug: p.slug || String(p.title || '').slice(0, 60),
    summary: p.summary || '',
    content: p.content || '',
    category: p.category || '软件工程',
    subcategory: p.subcategory || '',
    tags: Array.isArray(p.tags) ? p.tags : String(p.tags || '').split(',').map(s => s.trim()).filter(Boolean),
    stage: p.stage || 'seedling',
    source: p.source || '原创',
    source_url: p.sourceUrl || '',
    difficulty: Number(p.difficulty) || 1,
    published: p.published !== false,
    ...(p.nextReview ? { next_review: p.nextReview } : {}),
  }
}

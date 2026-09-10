// ============================================================
// useSupabase — Vue 组合式封装
// 提供：Auth（注册/登录/登出/当前用户）+ 数据读取（music/blog/demo）
// 用法：
//   import { useSupabase } from '@/composables/useSupabase'
//   const { supabase, user, signIn, signUp, signOut, fetchMusic, fetchBlog } = useSupabase()
// ============================================================
import { ref } from 'vue'
import { supabase } from '@/lib/supabase/client'

const user = ref(null)
const initializing = ref(true)

async function loadSession() {
  const { data } = await supabase.auth.getSession()
  user.value = data?.session?.user ?? null
  initializing.value = false
}

// 初始化时加载一次会话
if (initializing.value) {
  loadSession()
}

// 监听认证状态变化（登录/登出时自动同步）
supabase.auth.onAuthStateChange((_event, session) => {
  user.value = session?.user ?? null
})

async function signUp(email, password, meta = {}) {
  return supabase.auth.signUp({ email, password, options: { data: meta } })
}

async function signIn(email, password) {
  return supabase.auth.signInWithPassword({ email, password })
}

// 邮箱免密登录（Magic Link）：Supabase 发一封带签名链接的邮件，
// 用户点开后自动登录。此方式不依赖后端 JavaMail，SMTP 在
// Supabase Dashboard → Auth → Email → SMTP 配置（可用 Resend/Brevo）。
async function signInWithMagicLink(email, options = {}) {
  return supabase.auth.signInWithOtp({ email, options })
}

async function signOut() {
  return supabase.auth.signOut()
}

async function fetchRows(table, opts = {}) {
  let q = supabase.from(table).select(opts.select || '*')
  if (opts.order) q = q.order(opts.order.by, { ascending: opts.order.ascending ?? true })
  if (opts.limit) q = q.limit(opts.limit)
  if (opts.eq) {
    for (const [k, v] of Object.entries(opts.eq)) q = q.eq(k, v)
  }
  return q
}

// 便捷读取：音乐馆
function fetchMusic(limit = 50) {
  return fetchRows('music_tracks', { order: { by: 'id', ascending: true }, limit })
}

// 便捷读取：博客（只取已发布）
function fetchBlog(limit = 20) {
  return fetchRows('blog_articles', { eq: { published: true }, order: { by: 'created_at', ascending: false }, limit })
}

// 便捷读取：示例表
function fetchDemo(limit = 50) {
  return fetchRows('demo_items', { limit })
}

export function useSupabase() {
  return {
    supabase,
    user,
    initializing,
    signUp,
    signIn,
    signInWithMagicLink,
    signOut,
    fetchRows,
    fetchMusic,
    fetchBlog,
    fetchDemo,
  }
}

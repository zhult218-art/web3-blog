// ============================================================
// Supabase 浏览器端客户端
// 说明：Vue/Vite SPA 没有真正的"服务端"，浏览器直连 Supabase 用
// publishable key（配合 Row Level Security，前端只能操作自己允许的数据）。
// 用法：
//   import { supabase } from '@/lib/supabase/client'
//   const { data } = await supabase.from('music').select('*')
// ============================================================
import { createClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY

if (!supabaseUrl || !supabaseKey) {
  // 仅在开发时提示：忘记在 frontend/.env.local 里配 VITE_SUPABASE_*
  console.warn('[supabase] 缺少 VITE_SUPABASE_URL / VITE_SUPABASE_PUBLISHABLE_KEY，请检查 frontend/.env.local')
}

export const supabase = createClient(supabaseUrl, supabaseKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
  },
})

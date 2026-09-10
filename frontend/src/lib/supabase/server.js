// ============================================================
// Supabase 服务端客户端（Node / 脚本 / SSR）
// 说明：浏览器端无法安全持有 secret key，只能在 Node 服务端使用。
// 本文件供后端脚本、迁移工具等 Node 环境调用（配合 SERVICE/secret key）。
// 运行时通过环境变量读取：SUPABASE_URL、SUPABASE_SECRET_KEY
// 注意：Vue 浏览器端请使用 ./client.js，不要用本文件。
// ============================================================
import { createClient } from '@supabase/supabase-js'

const supabaseUrl = process.env.SUPABASE_URL
const supabaseKey = process.env.SUPABASE_SECRET_KEY // 服务端密钥，绝不进浏览器

if (!supabaseUrl || !supabaseKey) {
  throw new Error('[supabase/server] 缺少环境变量 SUPABASE_URL 或 SUPABASE_SECRET_KEY')
}

export const supabaseAdmin = createClient(supabaseUrl, supabaseKey, {
  auth: { autoRefreshToken: false, persistSession: false },
})

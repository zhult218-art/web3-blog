// ============================================================
// Supabase 浏览器端客户端
// 说明：Vue/Vite SPA 没有真正的"服务端"，浏览器直连 Supabase 用
// publishable key（配合 Row Level Security，前端只能操作自己允许的数据）。
// 用法：
//   import { supabase } from '@/lib/supabase/client'
//   const { data } = await supabase.from('music').select('*')
//
// 熔断机制：国内网络访问 supabase.co 可能被重置（ERR_CONNECTION_RESET）。
// REST/Storage 请求一旦出现网络层失败，进入 COOLDOWN_MS 冷却期，期间
// 直接快速 reject（不再发请求刷控制台红字），调用方既有 try/catch 会
// 自动回退到 Java 后端接口；冷却结束后放行一次请求试探，成功即恢复。
// /auth/ 请求不参与熔断（登录失败需要真实反馈，且仅用户主动操作时触发）。
// ============================================================
import { createClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY

if (!supabaseUrl || !supabaseKey) {
  // 仅在开发时提示：忘记在 frontend/.env.local 里配 VITE_SUPABASE_*
  console.warn('[supabase] 缺少 VITE_SUPABASE_URL / VITE_SUPABASE_PUBLISHABLE_KEY，请检查 frontend/.env.local')
}

// ---- 熔断器（localStorage 跨标签页共享冷却状态）----
const DOWN_KEY = 'supabase_circuit_down_until'
const COOLDOWN_MS = 30 * 60 * 1000 // 网络失败后冷却 30 分钟（冷却期内不再发注定失败的请求）
const PROBE_INTERVAL_MS = 60 * 1000 // 冷却期内每 60s 放行一次探测请求，成功即提前恢复
let downUntil = 0
let lastProbeAt = 0
try {
  downUntil = Number(localStorage.getItem(DOWN_KEY)) || 0
} catch { /* 隐私模式等场景忽略 */ }

function circuitOpen() {
  return Date.now() < downUntil
}

function tripCircuit() {
  downUntil = Date.now() + COOLDOWN_MS
  try { localStorage.setItem(DOWN_KEY, String(downUntil)) } catch { /* ignore */ }
}

function clearCircuit() {
  downUntil = 0
  try { localStorage.removeItem(DOWN_KEY) } catch { /* ignore */ }
}

/** 手动重置熔断（用户点"重试"时调用，立即放行下一次请求） */
export function resetSupabaseCircuit() { clearCircuit(); lastProbeAt = 0 }

/** 当前是否处于熔断冷却期（页面用来展示友好提示而非原始报错） */
export function isSupabaseCircuitOpen() { return circuitOpen() }

// 自定义 fetch：冷却期内短路，失败时熔断
async function guardedFetch(input, init) {
  const url = typeof input === 'string' ? input : input?.url || ''
  const isAuthCall = url.includes('/auth/')
  if (!isAuthCall && circuitOpen()) {
    // 半开探测：冷却超过 60s 放行一次试探网络是否恢复
    if (Date.now() - lastProbeAt < PROBE_INTERVAL_MS) {
      // 冷却期：不发起网络请求，直接以"网络错误"语义拒绝（无控制台红字）
      throw new TypeError('Supabase circuit open (cooldown)')
    }
    lastProbeAt = Date.now()
  }
  try {
    const res = await fetch(input, init)
    // 能收到任何 HTTP 响应（含 4xx/5xx）说明网络可达，解除熔断
    if (!isAuthCall && downUntil) clearCircuit()
    return res
  } catch (err) {
    // 网络层失败（连接重置/被拒/断网）→ 仅 REST/Storage 熔断；auth 保持直连
    if (!isAuthCall) tripCircuit()
    throw err
  }
}

export const supabase = createClient(supabaseUrl, supabaseKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
  },
  global: {
    fetch: guardedFetch,
  },
})

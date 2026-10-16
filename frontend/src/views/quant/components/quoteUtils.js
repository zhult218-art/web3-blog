// ============================================================
// 量化行情通用小工具（父页面 index.vue 与行情类子组件共用）
// 2026-10-16 批 3 拆分：从 index.vue 移出，避免父子重复实现。
// ============================================================

// 数值格式化为万/亿可读形式
export function formatVol(v) {
  if (!v && v !== 0) return '--'
  if (v > 10000) return (v / 10000).toFixed(1) + '万'
  if (v > 100000000) return (v / 100000000).toFixed(2) + '亿'
  return v.toString()
}

// 金额格式化为亿/元可读形式
export function formatMoney(v) {
  if (!v && v !== 0) return '--'
  if (v > 100000000) return (v / 100000000).toFixed(2) + '亿'
  return v.toString()
}

// 智能市场前缀：6→沪(sh)，0/3→深(sz)，4/8→北(bj)；已带前缀则原样返回
export function marketSymbol(raw) {
  const code = String(raw || '').trim().toLowerCase()
  if (!code) return ''
  if (/^(sh|sz|bj)/.test(code)) return code
  const digits = code.replace(/\D/g, '')
  if (digits.startsWith('6')) return `sh${digits}`
  if (digits.startsWith('4') || digits.startsWith('8')) return `bj${digits}`
  if (/^[035]/.test(digits)) return `sz${digits}`
  return `sh${digits}`
}

// ============================================================
// 日期时间格式化工具
// 各页面统一走此处，避免 formatDate 在 20+ 文件中各自实现、语义不一
// 所有函数对 null / undefined / 空串 / 非法日期均安全，统一返回 fallback
// ============================================================

// 内部：解析为合法 Date，非法返回 null
function toDate(v) {
  if (v === null || v === undefined || v === '') return null
  const t = new Date(v)
  return isNaN(t.getTime()) ? null : t
}

// 中文短日期，如 2026/9/20
export function formatDayCN(d, fallback = '') {
  const t = toDate(d)
  return t ? t.toLocaleDateString('zh-CN') : fallback
}

// 中文日期时间，如 2026/9/20 14:30:00
export function formatDateTimeCN(d, fallback = '') {
  const t = toDate(d)
  return t ? t.toLocaleString('zh-CN') : fallback
}

// 中文日期时间（精确到分钟），如 2026/9/20 14:30
export function formatDateTimeMinute(d, fallback = '') {
  const t = toDate(d)
  return t ? t.toLocaleString('zh-CN').slice(0, 16) : fallback
}

// ISO 串截取日期部分，如 2026-09-20
export function formatDay(d, fallback = '') {
  return toDate(d) ? String(d).slice(0, 10) : fallback
}

// ISO 串截取到分钟，如 2026-09-20 14:30
export function formatMinute(d, fallback = '') {
  return toDate(d) ? String(d).slice(0, 16).replace('T', ' ') : fallback
}

// 相对时间：刚刚 / N分钟前 / N小时前 / N天前，超过 7 天回退为短日期
export function formatRelative(d, fallback = '') {
  const t = toDate(d)
  if (!t) return fallback
  const diff = (Date.now() - t.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`
  return formatDayCN(d, fallback)
}
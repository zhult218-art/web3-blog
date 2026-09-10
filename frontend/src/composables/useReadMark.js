// ============================================================
// 已读标记（localStorage）
// 记录用户已读文章 id，供列表页展示"未读红点"
// ============================================================
const KEY = 'blog_read_ids'

export function useReadMark() {
  function readIds() {
    try {
      return JSON.parse(localStorage.getItem(KEY) || '[]')
    } catch {
      return []
    }
  }

  function isRead(id) {
    return readIds().includes(String(id))
  }

  function markRead(id) {
    const list = readIds()
    if (!list.includes(String(id))) {
      list.push(String(id))
      localStorage.setItem(KEY, JSON.stringify(list))
    }
  }

  return { isRead, markRead }
}

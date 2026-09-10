// ============================================================
// 浏览器直接下载工具
// - 同源（或可 CORS 拉取）的文件：fetch 为 blob 后经 <a download> 触发下载
// - 跨域受限等失败场景：回退为带 download 属性的锚点新窗口打开
// ============================================================

// 从 URL 推断文件名（decode + 去查询参数）
function nameFromUrl(url) {
  try {
    const path = new URL(url, window.location.href).pathname
    const last = path.split('/').pop()
    return last ? decodeURIComponent(last) : ''
  } catch {
    return ''
  }
}

/**
 * 触发浏览器下载
 * @param {string} url 文件地址（相对路径走 Vite 代理即同源）
 * @param {string} [fallbackName] 备用文件名（URL 无法推断时使用）
 */
export async function downloadFile(url, fallbackName = '') {
  if (!url) throw new Error('缺少下载地址')
  const name = nameFromUrl(url) || fallbackName || 'download'
  try {
    const resp = await fetch(url)
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
    const blob = await resp.blob()
    const objUrl = URL.createObjectURL(blob)
    triggerAnchor(objUrl, name)
    setTimeout(() => URL.revokeObjectURL(objUrl), 5000)
    return true
  } catch (e) {
    // blob 拉取失败（跨域/404 等）：退化为锚点直链，交由浏览器处理
    triggerAnchor(url, name, true)
    return false
  }
}

function triggerAnchor(href, name, newTab = false) {
  const a = document.createElement('a')
  a.href = href
  a.download = name
  if (newTab) {
    a.target = '_blank'
    a.rel = 'noopener'
  }
  document.body.appendChild(a)
  a.click()
  a.remove()
}

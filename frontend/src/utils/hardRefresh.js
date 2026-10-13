// ============================================================
// 强制清除所有浏览器缓存并重新加载
// 清理：localStorage / sessionStorage / cookies / Cache Storage /
//       Service Worker / IndexedDB
// 重载：location.replace() + 时间戳绕过 HTTP 缓存
// 供首页刷新按钮、右键菜单共用
// ============================================================

export async function hardRefresh() {
  try {
    // 1. localStorage + sessionStorage
    localStorage.clear()
    sessionStorage.clear()

    // 2. cookies（当前域 + 父域，覆盖 path=/ 和 domain= 两种写法）
    document.cookie.split(';').forEach(c => {
      const eq = c.indexOf('=')
      const name = eq > -1 ? c.slice(0, eq).trim() : c.trim()
      if (!name) return
      const exp = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`
      document.cookie = exp
      document.cookie = `${exp};domain=${location.hostname}`
      document.cookie = `${exp};domain=.${location.hostname}`
    })

    // 3. Cache Storage（Service Worker 管理的 HTTP 缓存）
    if (window.caches) {
      const keys = await caches.keys()
      await Promise.all(keys.map(k => caches.delete(k)))
    }

    // 4. 注销 Service Worker
    if (navigator.serviceWorker) {
      const regs = await navigator.serviceWorker.getRegistrations()
      await Promise.all(regs.map(r => r.unregister()))
    }

    // 5. IndexedDB（遍历并删除所有数据库，清除离线缓存数据）
    if (window.indexedDB && indexedDB.databases) {
      const dbs = indexedDB.databases()
      if (dbs && dbs.length) {
        await Promise.all(dbs.map(db => {
          if (!db.name) return Promise.resolve()
          return new Promise(resolve => {
            const req = indexedDB.deleteDatabase(db.name)
            req.onsuccess = resolve
            req.onerror = resolve
            req.onblocked = resolve
          })
        }))
      }
    }
  } catch {
    // 忽略清除失败，继续重载
  } finally {
    // location.replace 不留浏览器历史；保留原 query 参数与 hash，仅追加 _t 时间戳绕过 HTTP 缓存。
    // 此前用 location.pathname 会丢弃原 query（如 ?redirect=xxx），破坏带参数路由的刷新。
    const url = new URL(location.href)
    url.searchParams.set('_t', String(Date.now()))
    location.replace(url.pathname + url.search + url.hash)
  }
}

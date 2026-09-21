// ============================================================
// quantCache —— 量化数据前端缓存（模块级单例，跨路由存活）
//
// 解决的问题：
//  1. 返回免刷新：stale-while-revalidate，旧数据立即渲染，后台静默更新
//  2. 防缓存雪崩：每次写入 TTL 加 ±15% 随机抖动，错峰过期
//  3. 防缓存穿透：空结果以短 TTL（negTtl）缓存，重复 miss 不再打穿
//  4. 防缓存击穿：同一 key 刷新合并为同一个 in-flight Promise
// ============================================================

const store = new Map()
// entry: { data, expireAt, fetchedAt, inflight, negative }

const JITTER = 0.15 // ±15%

function jittered(ttl) {
  return Math.round(ttl * (1 - JITTER + Math.random() * JITTER * 2))
}

function isEmptyValue(v) {
  if (v == null) return true
  if (Array.isArray(v)) return v.length === 0
  if (typeof v === 'object') return Object.keys(v).length === 0
  return false
}

/**
 * SWR 取数
 * @param {string} key 缓存键
 * @param {() => Promise<any>} fetcher 真实取数函数
 * @param {object} opts
 * @param {number} opts.ttl       正常 TTL（毫秒），默认 60s
 * @param {number} opts.negTtl    空结果 TTL（毫秒），默认 15s
 * @param {(v:any)=>boolean} opts.isEmpty 空结果判定
 * @param {boolean} opts.force    强制刷新
 * @returns {Promise<any>}
 */
export function swr(key, fetcher, opts = {}) {
  const ttl = opts.ttl ?? 60000
  const negTtl = opts.negTtl ?? 15000
  const isEmpty = opts.isEmpty ?? isEmptyValue
  const now = Date.now()
  const e = store.get(key)

  if (!opts.force && e && !e.negative && now < e.expireAt) {
    return Promise.resolve(e.data) // ① 新鲜：直接返回
  }

  if (!opts.force && e && e.inflight) {
    if (now < e.expireAt || e.data !== undefined) {
      // ② 有旧数据：先返回旧数据，刷新已在进行（stale-while-revalidate）
      if (e.data !== undefined) return Promise.resolve(e.data)
      return e.inflight // ③ 首次加载中：复用 in-flight（请求合并，防击穿）
    }
  }

  if (!opts.force && e && e.data !== undefined && now >= e.expireAt) {
    // ④ 过期但有旧数据（含负缓存过期）：立即返回旧值，后台刷新一次
    if (!e.inflight) {
      e.inflight = doFetch(key, fetcher, ttl, negTtl, isEmpty)
        .finally(() => { if (store.get(key)) store.get(key).inflight = null })
    }
    return Promise.resolve(e.data)
  }

  // ⑤ 首次加载 / 强制刷新
  const p = doFetch(key, fetcher, ttl, negTtl, isEmpty)
  if (e) e.inflight = p
  else store.set(key, { data: undefined, expireAt: now, fetchedAt: now, inflight: p, negative: false })
  return p
}

async function doFetch(key, fetcher, ttl, negTtl, isEmpty) {
  try {
    const data = await fetcher()
    const negative = isEmpty(data)
    store.set(key, {
      data,
      negative,
      expireAt: Date.now() + jittered(negative ? negTtl : ttl), // 抖动 TTL：防雪崩；空值短缓存：防穿透
      fetchedAt: Date.now(),
      inflight: null,
    })
    return data
  } catch (err) {
    const old = store.get(key)
    if (old && old.data !== undefined) {
      // 刷新失败但有旧数据：继续沿用（延长一个抖动 TTL），不清空
      old.expireAt = Date.now() + jittered(ttl)
      old.inflight = null
      return old.data
    }
    if (old) old.inflight = null
    throw err
  }
}

/** 手动失效（刷新按钮场景：force=true 下次取数强刷） */
export function invalidate(key) {
  const e = store.get(key)
  if (e) e.expireAt = 0
}

export function clearQuantCache() {
  store.clear()
}

export function cacheStats() {
  const now = Date.now()
  return [...store.entries()].map(([key, e]) => ({
    key,
    fresh: now < e.expireAt,
    negative: !!e.negative,
    age: e.fetchedAt ? now - e.fetchedAt : null,
  }))
}

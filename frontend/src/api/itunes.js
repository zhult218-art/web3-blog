// ============================================================
// 免费音乐源：iTunes Search API（无需 Key、无配额限制）
// · 搜索用 JSONP（script 标签），天然绕过浏览器跨域限制
// · 每首歌带 30s 官方试听 previewUrl（m4a）
// · previewUrl 经 vite 代理 /itunes-audio 转为同源，
//   Web Audio 可视化（AudioWave）才能拿到实时频谱
// ============================================================

let jsonpSeq = 0

// JSONP 请求（iTunes Search API 原生支持 callback 参数）
function jsonp(url, timeout = 8000) {
  return new Promise((resolve, reject) => {
    const cb = `itunesCb_${Date.now()}_${++jsonpSeq}`
    const script = document.createElement('script')
    const cleanup = () => {
      clearTimeout(timer)
      delete window[cb]
      script.remove()
    }
    const timer = setTimeout(() => { cleanup(); reject(new Error('iTunes 接口超时')) }, timeout)
    window[cb] = data => { cleanup(); resolve(data) }
    script.src = `${url}&callback=${cb}`
    script.onerror = () => { cleanup(); reject(new Error('iTunes 接口请求失败')) }
    document.head.appendChild(script)
  })
}

// 试听地址转本站同源代理（audio-ssl.itunes.apple.com → /itunes-audio）
function toProxyUrl(previewUrl) {
  try {
    const u = new URL(previewUrl)
    return '/itunes-audio' + u.pathname + u.search
  } catch {
    return previewUrl
  }
}

// 把 iTunes 结果映射为播放器通用曲目结构
function mapResult(r) {
  return {
    id: 'it-' + r.trackId,
    title: r.trackName,
    artist: r.artistName,
    album: r.collectionName || '',
    category: r.primaryGenreName || '音乐',
    duration: Math.round((r.trackTimeMillis || 210000) / 1000),
    cover: (r.artworkUrl100 || '').replace('100x100', '600x600'),
    url: toProxyUrl(r.previewUrl),
    neteaseId: '',
    source: 'itunes',
  }
}

// 关键字搜索（默认返回带试听地址的前 limit 首）
export async function searchItunes(term, limit = 20) {
  const url = `https://itunes.apple.com/search?term=${encodeURIComponent(term)}&media=music&entity=song&limit=${limit}`
  const data = await jsonp(url)
  return (data.results || []).filter(r => r.previewUrl && r.trackName).map(mapResult)
}

// 云端精选歌单（八九十年代为主）：页面默认直接可播，无需登录/Key
// term 关键词命中 iTunes 八九十年代华语/外语金曲
export const ITUNES_PRESET = [
  { label: '80s 华语金曲', term: '80s chinese golden hits' },
  { label: '90s 经典情歌', term: '90s love songs mandarin' },
  { label: '邓丽君', term: 'teresa teng' },
  { label: '张国荣', term: 'leslie cheung' },
  { label: 'Beyond', term: 'beyond band hong kong' },
  { label: '谭咏麟', term: 'alan tam' },
  { label: '80s 英文金曲', term: '80s english pop hits' },
  { label: '90s 摇滚', term: '90s rock classics' },
  { label: '罗大佑', term: 'lo ta yu' },
  { label: '李宗盛', term: 'jonathan lee' },
  { label: '凤飞飞', term: 'feng fei fei' },
  { label: '蔡琴', term: 'tsai chin' },
]

// 拉取某个精选歌单
export const getItunesPlaylist = (item, limit = 12) => searchItunes(item.term, limit)

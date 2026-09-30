// ============================================================
// 音乐接口
// 1) 网易云歌单代理（走网关 /netease → 本机 NeteaseCloudMusicApi :3000）
// 2) 站点曲库后端（media-service :8085，经网关 /music/* 直出）
// 后端未启动时统一给 8s 超时 + isMusicServiceDown 友好降级
// ============================================================
import request from './request'

// 音乐接口专用超时（毫秒）：避免后端未启动时页面长时间无响应
const MUSIC_TIMEOUT = 8000

const musicGet = (url, config = {}) => request.get(url, { timeout: MUSIC_TIMEOUT, ...config })
const musicPost = (url, data, config = {}) => request.post(url, data, { timeout: MUSIC_TIMEOUT, ...config })
const musicDelete = (url, config = {}) => request.delete(url, { timeout: MUSIC_TIMEOUT, ...config })

// 判断错误是否源于音乐服务不可用（超时 / 网关 5xx / 无响应）
export function isMusicServiceDown(err) {
  if (!err) return false
  if (err.code === 'ECONNABORTED') return true // 请求超时
  if (err.response?.status >= 500) return true // 网关无法转发（服务未启动）
  if (!err.response) return true               // 网络错误 / 无响应
  return false
}

// ============================================================
// 网易云接口（走网关 /netease）
// ============================================================

// 歌单详情（含曲目列表）
export const getNeteasePlaylist = (id, limit = 60) => musicGet(`/netease/playlist/detail?id=${id}&limit=${limit}`)
// 单曲播放地址（br=码率：128000 标准 / 192000 较高 / 320000 极高 / 999000 无损）
export const getNeteaseSongUrl = (id, br = 320000) => musicGet(`/netease/song/url?id=${id}&br=${br}`)
// 歌词
export const getNeteaseLyric = id => musicGet(`/netease/lyric?id=${id}`)
// 搜索（keywords 关键字，limit 数量）
export const searchNetease = (keywords, limit = 20) => musicGet(`/netease/search?keywords=${encodeURIComponent(keywords)}&limit=${limit}`)

// ============================================================
// 站点曲库后端接口（走网关 /music/*）
// 鉴权：游客可访问详情/流式/热榜/搜索；收藏/历史需登录（Authorization 由 request 拦截器自动注入）
// ============================================================

// 切换收藏（返回 { liked: true/false }）
export const toggleLike = id => musicPost(`/music/${id}/like`)

// 查询当前用户对某首歌的收藏态
export const getLikeStatus = id => musicGet(`/music/${id}/like`)

// 当前用户收藏列表（分页）
export const getLikedList = (page = 1, size = 20) => musicGet(`/music/liked`, { params: { page, size } })

// 热榜（按 play_count 倒序）
export const getHotSongs = (limit = 20) => musicGet(`/music/hot`, { params: { limit } })

// 本地搜索（按歌名/歌手关键字）
export const searchMusic = (keyword, page = 1, size = 20) => musicGet(`/music/search`, { params: { keyword, page, size } })

// 播放次数 +1 埋点（登录用户顺手写历史；游客仅累加播放数）
export const recordPlay = id => musicPost(`/music/${id}/play`)

// 当前用户播放历史（分页）
export const getHistory = (page = 1, size = 20) => musicGet(`/music/history`, { params: { page, size } })

// 清空当前用户播放历史
export const clearHistory = () => musicDelete(`/music/history`)

// 流式播放 URL（支持 HTTP Range 206 拖拽进度条秒级 seek）
// 直接返回相对路径，<audio src> 会带同源 cookie，后端透传上游 Range
export const streamUrl = id => `/music/${id}/stream`

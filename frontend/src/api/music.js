// ============================================================
// 音乐接口（网易云歌单代理，走网关 /netease → 本机 NeteaseCloudMusicApi）
// 后端服务位于 backend/netease-music-api（端口 3000），未启动时请求会失败：
// 这里统一给音乐请求加 8s 超时，并提供 isMusicServiceDown 供组件展示友好提示
// ============================================================
import request from './request'

// 音乐接口专用超时（毫秒）：避免后端未启动时页面长时间无响应
const MUSIC_TIMEOUT = 8000

const musicGet = (url, config = {}) => request.get(url, { timeout: MUSIC_TIMEOUT, ...config })

// 判断错误是否源于音乐服务不可用（超时 / 网关 5xx / 无响应）
export function isMusicServiceDown(err) {
  if (!err) return false
  if (err.code === 'ECONNABORTED') return true // 请求超时
  if (err.response?.status >= 500) return true // 网关无法转发（服务未启动）
  if (!err.response) return true               // 网络错误 / 无响应
  return false
}

// 歌单详情（含曲目列表）
export const getNeteasePlaylist = (id, limit = 60) => musicGet(`/netease/playlist/detail?id=${id}&limit=${limit}`)
// 单曲播放地址（br=码率）
export const getNeteaseSongUrl = id => musicGet(`/netease/song/url?id=${id}&br=320000`)
// 歌词
export const getNeteaseLyric = id => musicGet(`/netease/lyric?id=${id}`)
// 搜索（keywords 关键字，limit 数量）
export const searchNetease = (keywords, limit = 20) => musicGet(`/netease/search?keywords=${encodeURIComponent(keywords)}&limit=${limit}`)

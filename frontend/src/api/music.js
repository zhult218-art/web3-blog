// ============================================================
// 音乐接口（网易云歌单代理，走网关 /netease → 本机 NeteaseCloudMusicApi）
// ============================================================
import request from './request'

// 歌单详情（含曲目列表）
export const getNeteasePlaylist = (id, limit = 60) => request.get(`/netease/playlist/detail?id=${id}&limit=${limit}`)
// 单曲播放地址（br=码率）
export const getNeteaseSongUrl = id => request.get(`/netease/song/url?id=${id}&br=320000`)
// 歌词
export const getNeteaseLyric = id => request.get(`/netease/lyric?id=${id}`)
// 搜索（keywords 关键字，limit 数量）
export const searchNetease = (keywords, limit = 20) => request.get(`/netease/search?keywords=${encodeURIComponent(keywords)}&limit=${limit}`)
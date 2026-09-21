// ============================================================
// 免费音乐源：Audius 公开 API（去中心化音乐平台）
// · 无需申请 Key、无需登录，接口与音频内容节点均已开放 CORS
// · 曲目为【完整音频】而非 30s 试听片段，可直接进全局播放器
// · JSON 接口统一走同源代理 /audius-api（见 vite.config.js / nginx.conf），
//   避免第三方域名直连抖动；音频流用返回的签名地址（内容节点带
//   Access-Control-Allow-Origin: *），播放器会标记 cors 以便 Web Audio 取谱
// ============================================================
import axios from 'axios'

// 同源代理前缀（开发环境由 Vite 转发，生产由 Nginx 转发到 api.audius.co）
const API_BASE = '/audius-api'
// Audius 要求携带的调用方标识（非密钥，仅用于统计）
const APP_NAME = 'Web3Blog'
// 兜底直连域名：代理不可用时仍能拿到数据
const FALLBACK_ORIGIN = 'https://api.audius.co'

const http = axios.create({ baseURL: API_BASE, timeout: 12000 })

async function getJson(path, params = {}) {
  try {
    const res = await http.get(path, { params: { app_name: APP_NAME, ...params } })
    return res.data
  } catch (e) {
    // 代理层异常时退回直连（Audius 自身允许跨域）
    const res = await axios.get(`${FALLBACK_ORIGIN}${path}`, {
      params: { app_name: APP_NAME, ...params },
      timeout: 12000,
    })
    return res.data
  }
}

// 把 Audius 曲目映射为播放器通用结构
// cors=true 表示音频响应带 CORS 头，播放器会设置 crossOrigin 以保留可视化频谱
function mapTrack(t) {
  const streamUrl = t.stream?.url
    || `${FALLBACK_ORIGIN}/v1/tracks/${t.id}/stream?app_name=${APP_NAME}`
  return {
    id: 'ad-' + t.id,
    title: t.title || '未命名曲目',
    artist: t.user?.name || 'Audius 音乐人',
    album: t.album || '',
    category: t.genre || 'Audius',
    duration: Math.round(t.duration || 0),
    cover: t.artwork?.['480x480'] || t.artwork?.['150x150'] || '',
    url: streamUrl,
    source: 'audius',
    cors: true,
  }
}

// 只保留真正可播放的曲目（Audius 部分曲目受版权限制不可流播）
const playable = list => (list || []).filter(t => t && t.is_streamable !== false)

// 关键字搜索完整曲目
export async function searchAudius(keyword, limit = 20) {
  const data = await getJson('/v1/tracks/search', { query: keyword, limit })
  return playable(data?.data).map(mapTrack)
}

// 热门榜单（time: week | month | allTime；genre 可选）
export async function getAudiusTrending(limit = 12, genre = '') {
  const params = { limit, time: 'week' }
  if (genre) params.genre = genre
  const data = await getJson('/v1/tracks/trending', params)
  return playable(data?.data).map(mapTrack)
}

// 首页默认展示的免费曲库分区（Audius 流派）
export const AUDIUS_PRESET = [
  { label: '本周热榜', genre: '' },
  { label: '电子', genre: 'Electronic' },
  { label: '嘻哈', genre: 'Hip-Hop/Rap' },
  { label: '氛围', genre: 'Ambient' },
  { label: '摇滚', genre: 'Rock' },
  { label: '爵士', genre: 'Jazz' },
]

export default { searchAudius, getAudiusTrending, AUDIUS_PRESET }
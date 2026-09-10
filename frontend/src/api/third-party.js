// ============================================================
// 第三方 API 工具中心接口（网关代理 /j8y /showapi /shanhe）
// 密钥由网关服务端注入，前端不暴露任何 key
// 注意：使用独立 axios 实例（不挂载本站 401 登出拦截器），
// 第三方平台返回 401/403 时不会影响本站登录态
// ============================================================
import axios from 'axios'

const tp = axios.create({ baseURL: '/api', timeout: 20000 })

// ---------- FreeAPI (j8y.cn) ----------
// 通用调用：api_path + 任意参数透传
export const j8yApi = async (path, params = {}) => {
  const res = await tp.get('/j8y/api/gateway.php', { params: { api_path: path, ...params } })
  return res.data
}

// 业务封装
export const j8yIpLookup = ip => j8yApi('ip-lookup', { ip })
export const j8yQqInfo = qq => j8yApi('cxqq', { qq })
export const j8yCat = () => j8yApi('cat')
export const j8yHistory = () => j8yApi('history')
export const j8yDeltaPwd = () => j8yApi('sjzmm')
export const j8yDouyin = url => j8yApi('dyqsy', { url })
export const j8yWeather = city => j8yApi('weather', { city })
export const j8yWyMusic = params => j8yApi('wy_music', params)
export const j8yQsyy = url => j8yApi('qsyy', { url })
export const j8yTxtp = url => j8yApi('txtp', { url })

// ---------- 万维易源 ShowAPI ----------
// 通用调用：{product}-{id} 如 6-1 / 105-35 / 872-1
export const showapiApi = async (point, params = {}) => {
  const res = await tp.get(`/showapi/${point}`, { params })
  return res.data
}

export const showapiPhone = num => showapiApi('6-1', { num })
export const showapiFxList = () => showapiApi('105-35')
export const showapiFxRate = code => showapiApi('105-30', { code })
export const showapiZodiac = params => showapiApi('872-1', params)

// ---------- 山河云 (shanhe.kim) ----------
// 通用调用：接口名 + 参数（apikey 可选透传）
export const shanheApi = async (name, params = {}) => {
  const res = await tp.get(`/shanhe/API/${encodeURIComponent(name)}.php`, { params })
  return res.data
}

export const shanheZodiac = name => shanheApi('星座', { name, type: 'json' })
export const shanheWeather = city => shanheApi('天气', { city, type: 'json' })
export const shanheLunar = () => shanheApi('农历', { type: 'json' })
export const shanheHistory = num => shanheApi('历史上的今天', { type: 'json', num })
export const shanheYiyan = () => shanheApi('一言', { type: 'json' })
export const shanheWeibo = () => shanheApi('微博热榜', { type: 'json' })
export const shanheZhihu = () => shanheApi('知乎热榜', { type: 'json' })
export const shanheFishCal = (style, apikey) => shanheApi('摸鱼日历', { style, apikey })
export const shanheWallpaper = apikey => shanheApi('随机壁纸', { apikey })
export const shanheBing = () => shanheApi('必应壁纸', { type: 'json' })
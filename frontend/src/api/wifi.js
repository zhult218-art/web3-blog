// ============================================================
// WiFi 安全审计接口（统一 py-service，经 Vite /wifiapi 代理 → /api/wifi）
// 页面路由本身是 /wifi，代理前缀必须错开（用 /wifiapi），否则页面请求会被吞。
// ============================================================
import request from './request'
import { useAuthStore } from '@/stores/modules/auth'

// WebSocket 无法携带 Authorization 头：改带 ?token=<JWT>（后端 core/auth.py 校验）
const wsTokenQuery = () => {
  try {
    const t = useAuthStore().token
    return t ? `?token=${encodeURIComponent(t)}` : ''
  } catch {
    return ''
  }
}

const wf = (path, config) =>
  request.request({
    ...config,
    url: `/wifiapi${path}`,
    method: config?.method || 'get',
    baseURL: '',
    timeout: config?.timeout || 30000,
  })

// 能力检测：平台 / 权限 / 外部命令 / 网卡 / 引擎可用性
export const getCapabilities = (cfg) => wf('/capabilities', cfg)

// 周边网络扫描
export const scanNetworks = (iface, cfg) => wf('/scan', { params: { iface }, ...cfg })

// 无线网卡信息
export const getInterfaces = (cfg) => wf('/interfaces', cfg)

// Windows：已存配置文件
export const listProfiles = (cfg) => wf('/profiles', cfg)
export const getProfile = (name) => wf(`/profiles/${encodeURIComponent(name)}`)

// 攻击引擎（Linux：wifiphisher / wifite）
export const getEngines = () => wf('/engines')

// ── Linux 引擎会话 ──
export const createSession = (engine, args = []) =>
  wf('/sessions', { method: 'post', data: { engine, args } })
export const listSessions = () => wf('/sessions')
export const getSession = (sid) => wf(`/sessions/${sid}`)
export const getSessionLogs = (sid, offset = 0) =>
  wf(`/sessions/${sid}/logs`, { params: { offset } })
export const stopSession = (sid) => wf(`/sessions/${sid}/stop`, { method: 'post' })
export const sessionWsUrl = (sid) => {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${window.location.host}/wifiapi/sessions/${sid}/ws${wsTokenQuery()}`
}

// ── Windows 邪恶孪生热点 ──
export const getRogueStatus = () => wf('/rogue/status')
export const startRogue = (ssid, key, template) =>
  wf('/rogue/start', { method: 'post', data: { ssid, key, template }, timeout: 180000 })
export const stopRogue = () => wf('/rogue/stop', { method: 'post', timeout: 30000 })

// ── WPA 离线破解 ──
export const getHashcatInfo = () => wf('/crack/hashcat')
export const createCrackJob = (hashText, wordlistPath, engine = 'auto', hashPath = '',
  crackMode = 'dictionary', rulePath = '', mask = '') =>
  wf('/crack/jobs', {
    method: 'post',
    data: {
      hash_text: hashText,
      hash_path: hashPath,
      wordlist_path: wordlistPath,
      engine,
      crack_mode: crackMode,
      rule_path: rulePath,
      mask,
    },
    timeout: 30000,
  })
export const uploadCrackJob = (formData, onProgress) =>
  wf('/crack/upload', {
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000,
    onUploadProgress: onProgress,
  })
export const listCrackJobs = () => wf('/crack/jobs')
export const stopCrackJob = (jid) => wf(`/crack/jobs/${jid}/stop`, { method: 'post' })
export const crackWsUrl = (jid) => {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${window.location.host}/wifiapi/crack/jobs/${jid}/ws${wsTokenQuery()}`
}

// ============================================================
// 服务健康状态 Composable
// 轮询所有微服务的运行状态，供导航栏与管理面板使用
// API 失败时使用前端注册表兜底，状态标 UNKNOWN
// 基础设施（MySQL/Redis/Nacos 等）通过端口探测或依赖推断获取状态
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getServicesStatus, SERVICE_REGISTRY } from '@/api/admin'

const serviceStatus = ref({})
const loading = ref(false)
let timer = null
let viewerCount = 0

// 初始化为注册表所有服务 UNKNOWN（导航栏也能显示）
SERVICE_REGISTRY.forEach(s => { serviceStatus.value[s.name] = 'UNKNOWN' })

// HTTP 探测已下沉到 admin-service 后端统一 TCP 探测（getStatusMap 直接返回
// nacos/quant-py/netease 状态），浏览器不再直连这些端口发请求——
// 探测失败或超时的 abort 会在控制台刷 net::ERR_ABORTED 红字且无法被 JS 屏蔽。
// 这里仅保留"后端旧版本未返回该键"时的直接探测兜底。
const INFRA_HTTP = [
  { name: 'nacos', url: 'http://127.0.0.1:8848/nacos/' },
  { name: 'quant-py', url: 'http://127.0.0.1:9006/' },
  { name: 'netease', url: 'http://127.0.0.1:3000/' },
]

// 非HTTP服务：从依赖的微服务状态推断
const INFRA_INFERRED = [
  { name: 'mysql', deps: ['user-service', 'blog-service', 'forum-service'] },
  { name: 'redis', deps: ['user-service', 'blog-service'] },
  { name: 'rabbitmq', deps: ['forum-service', 'shop-service', 'media-service'] },
  { name: 'supabase', deps: ['blog-service'] },
]

async function probeInfraServices(javaStatus) {
  const infraStatus = {}
  // 兜底直连只在本机访问（localhost / 127.0.0.1）时执行：
  // 内网穿透公网访客的浏览器探测 127.0.0.1 只会打到访客自己的电脑，
  // 产生无意义的 ERR_CONNECTION_REFUSED 与错误的"服务已停止"状态。
  const isLocal = ['localhost', '127.0.0.1', ''].includes(window.location.hostname)
  // HTTP 探测（仅后端未下发该服务状态、且当前为本机访问时兜底直连）
  await Promise.all(INFRA_HTTP.map(async (s) => {
    if (javaStatus[s.name]) { infraStatus[s.name] = javaStatus[s.name]; return }
    if (!isLocal) { infraStatus[s.name] = 'UNKNOWN'; return }
    try {
      const ctrl = new AbortController()
      const tid = setTimeout(() => ctrl.abort(), 2000)
      await fetch(s.url, { mode: 'no-cors', signal: ctrl.signal })
      infraStatus[s.name] = 'RUNNING'
      clearTimeout(tid)
    } catch {
      infraStatus[s.name] = 'STOPPED'
    }
  }))
  // 依赖推断：任一依赖的微服务 RUNNING 即认为基础设施在运行
  for (const s of INFRA_INFERRED) {
    const anyUp = s.deps.some(d => javaStatus[d] === 'RUNNING')
    infraStatus[s.name] = anyUp ? 'RUNNING' : 'STOPPED'
  }
  return infraStatus
}

async function fetchStatus() {
  try {
    const res = await getServicesStatus()
    // res 经响应拦截器已是后端 ApiResponse body（{ code, data, message }）。
    // 兼容直返状态 map 的旧形态：无 code 字段时把 res 视作 data 本体。
    const body = res && res.data !== undefined ? res : { data: res }
    // 后端业务异常（code !== 200，data 多为 null）：保持现状，不覆盖已知状态
    if (body.code !== undefined && body.code !== 200) return
    const javaStatus = body.data
    // 响应非对象（网关返回 HTML/字符串等异常体）：保持现状，避免清空导航栏服务
    if (!javaStatus || typeof javaStatus !== 'object' || Array.isArray(javaStatus)) return
    // 并行探测基础设施
    const infraStatus = await probeInfraServices(javaStatus)
    // 合并而非覆盖：仅在响应中明确给出的服务状态覆盖旧值，未提及的服务保留上次状态。
    // 这样单次异常/部分响应不会清空已知服务状态，导航栏不再"掉服务"。
    const merged = { ...serviceStatus.value }
    for (const name of Object.keys(javaStatus)) merged[name] = javaStatus[name]
    for (const name of Object.keys(infraStatus)) merged[name] = infraStatus[name]
    serviceStatus.value = merged
  } catch {
    // API 不可用时保持当前状态（注册表兜底已初始化）
  }
}

export function useServiceHealth() {
  onMounted(() => {
    viewerCount++
    if (viewerCount === 1) {
      fetchStatus()
      timer = setInterval(fetchStatus, 30000) // 30秒轮询
    }
  })

  onBeforeUnmount(() => {
    viewerCount--
    if (viewerCount <= 0) {
      viewerCount = 0
      if (timer) { clearInterval(timer); timer = null }
    }
  })

  return { serviceStatus, loading, refresh: fetchStatus }
}

/** 判断某服务是否运行中（导航栏可见性用）。
 *  仅在明确确认停止时才判定为"未运行"；UNKNOWN / 未探测到 / admin-service 不可达
 *  均视为可用，避免健康检查单次抖动或 admin 不可达导致导航栏服务"消失"。
 *  真正 STOPPED 才隐藏入口，此时用户点击也只会得到错误，隐藏更合理。 */
export function isServiceUp(name) {
  const s = serviceStatus.value[name]
  return s !== 'STOPPED' && s !== 'DOWN' && s !== 'TERMINATED'
}

/** 判断某服务是否状态未知（admin-service 不可达） */
export function isServiceUnknown(name) {
  return serviceStatus.value[name] === 'UNKNOWN'
}

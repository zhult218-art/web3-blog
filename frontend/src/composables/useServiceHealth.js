// ============================================================
// 服务健康状态 Composable
// 轮询所有微服务的运行状态，供导航栏与管理面板使用
// API 失败时使用前端注册表兜底，状态标 UNKNOWN
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getServicesStatus, SERVICE_REGISTRY } from '@/api/admin'

const serviceStatus = ref({})
const loading = ref(false)
let timer = null
let viewerCount = 0

// 初始化为注册表所有服务 UNKNOWN（导航栏也能显示）
SERVICE_REGISTRY.forEach(s => { serviceStatus.value[s.name] = 'UNKNOWN' })

async function fetchStatus() {
  try {
    const res = await getServicesStatus()
    serviceStatus.value = res.data || res || {}
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

/** 判断某服务是否运行中 */
export function isServiceUp(name) {
  return serviceStatus.value[name] === 'RUNNING'
}

/** 判断某服务是否状态未知（admin-service 不可达） */
export function isServiceUnknown(name) {
  return serviceStatus.value[name] === 'UNKNOWN'
}

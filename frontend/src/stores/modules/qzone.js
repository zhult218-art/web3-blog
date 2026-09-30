// ============================================================
// 时光回溯 · QQ 空间备份 —— 跨页面任务状态 Store
// 抓取任务在 qzone-service 后台线程执行，与页面生命周期无关；
// 这里负责：会话/任务状态跨页面（及刷新）恢复 + 全局轮询 + 悬浮进度条数据源。
// ============================================================
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getJob, cancelJob } from '@/api/qzone'

export const useQzoneStore = defineStore('qzone', () => {
  const sid = ref('') // 扫码会话 id
  const loginUser = ref(null) // { uin, name, avatar }
  const jid = ref('') // 抓取任务 id
  const job = ref(null) // 服务端任务状态（running/done/failed/canceled）
  const badgeHidden = ref(false) // 用户主动收起悬浮进度条

  let timer = null
  let announced = '' // 已播报过的终态，避免重复 toast

  const running = computed(() => job.value?.status === 'running')
  const finished = computed(() => ['done', 'failed', 'canceled'].includes(job.value?.status))

  function persist() {
    try {
      if (sid.value) sessionStorage.setItem('qz_sid', sid.value)
      else sessionStorage.removeItem('qz_sid')
      if (loginUser.value) sessionStorage.setItem('qz_user', JSON.stringify(loginUser.value))
      else sessionStorage.removeItem('qz_user')
      if (jid.value) sessionStorage.setItem('qz_jid', jid.value)
      else sessionStorage.removeItem('qz_jid')
    } catch {
      /* 隐私模式等场景忽略 */
    }
  }

  // App 启动/进入页面时恢复上一次会话与任务（切页不丢、刷新也能续看进度）
  function restore() {
    if (jid.value || sid.value) return
    try {
      sid.value = sessionStorage.getItem('qz_sid') || ''
      const u = sessionStorage.getItem('qz_user')
      loginUser.value = u ? JSON.parse(u) : null
      jid.value = sessionStorage.getItem('qz_jid') || ''
      if (jid.value) startPolling()
    } catch {
      /* ignore */
    }
  }

  function setLogin(newSid, user) {
    sid.value = newSid
    loginUser.value = user
    persist()
  }

  function clearLogin() {
    sid.value = ''
    loginUser.value = null
    persist()
  }

  function setJobId(id) {
    jid.value = id
    job.value = null
    announced = ''
    badgeHidden.value = false
    persist()
    startPolling()
  }

  function startPolling() {
    stopPolling()
    if (!jid.value) return
    poll()
    timer = setInterval(poll, 1500)
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  async function poll() {
    if (!jid.value) return stopPolling()
    try {
      const j = await getJob(jid.value)
      job.value = j
      if (j.status !== 'running') {
        stopPolling()
        if (announced !== j.status) {
          announced = j.status
          // 延迟引入 toast store，避免循环依赖
          import('@/stores/modules/toast').then(({ useToastStore }) => {
            const toast = useToastStore()
            if (j.status === 'done') toast.success(`空间回溯完成，共备份 ${j.fetched} 条说说，可前往下载`)
            else if (j.status === 'canceled') toast.warning('抓取任务已取消，已抓数据可导出')
            else if (j.status === 'failed') toast.error('空间抓取失败：' + (j.error || '未知错误'))
          })
        }
      }
    } catch {
      // 404：任务已被清理或服务重启 —— 静默结束跟踪
      stopPolling()
      jid.value = ''
      job.value = null
      persist()
    }
  }

  async function cancel() {
    if (!jid.value || !running.value) return
    try {
      await cancelJob(jid.value)
    } catch {
      /* ignore */
    }
  }

  function clearJob() {
    stopPolling()
    jid.value = ''
    job.value = null
    announced = ''
    persist()
  }

  return {
    sid,
    loginUser,
    jid,
    job,
    badgeHidden,
    running,
    finished,
    restore,
    setLogin,
    clearLogin,
    setJobId,
    startPolling,
    stopPolling,
    cancel,
    clearJob,
  }
})

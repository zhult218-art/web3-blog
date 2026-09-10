<template>
  <div class="min-h-screen py-8 px-6">
    <div class="max-w-7xl mx-auto">
      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-white motions-reveal" v-reveal>
          <span class="text-gradient-cyber">Admin</span> Dashboard
        </h1>
        <p class="mt-1 text-sm text-gray-500 motions-reveal delay-100" v-reveal>系统概览 · 实时监控</p>
      </div>

      <!-- Stats Grid -->
      <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4 mb-8">
        <div v-for="(stat, i) in stats" :key="stat.label"
          class="glass-panel p-5 motions-reveal" v-reveal :class="`delay-${(i+1)*80}`">
          <div class="flex items-center justify-between mb-3">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center text-lg"
              :class="stat.iconBg">
              <span>{{ stat.icon }}</span>
            </div>
            <span v-if="stat.change" class="web3-badge" :class="stat.badgeClass">{{ stat.change }}</span>
          </div>
          <div class="text-2xl font-black text-white matrix-text">{{ stat.value }}</div>
          <div class="text-[11px] text-gray-500 mt-1 tracking-wider uppercase">{{ stat.label }}</div>
        </div>
      </div>

      <!-- Main Grid -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Service Status -->
        <div class="lg:col-span-2 glass-panel p-6 motions-reveal delay-400" v-reveal>
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-bold text-white flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></span>
              Service Health
            </h3>
            <button @click="refreshServices" class="text-[11px] text-purple-400 hover:text-purple-300 transition">刷新</button>
          </div>
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <div v-for="svc in services" :key="svc.name"
              class="flex items-center justify-between px-3 py-2.5 rounded-xl bg-[#0c0c22] border border-white/[0.04] group">
              <div class="flex items-center gap-3">
                <span class="w-2 h-2 rounded-full" :class="svc.status === 'RUNNING' ? 'bg-green-400 shadow-[0_0_6px_rgba(16,185,129,0.5)]' : 'bg-red-400'"></span>
                <div>
                  <div class="text-xs font-medium text-white/80">{{ svc.label || svc.name }}</div>
                  <div class="text-[10px] text-gray-500">:{{ svc.port }} · {{ svc.status }}</div>
                </div>
              </div>
              <div class="flex items-center gap-1">
                <span v-if="svc.core" class="text-[9px] px-1.5 py-0.5 rounded bg-amber-400/10 text-amber-300 border border-amber-400/20">基础</span>
                <button v-if="svc.status === 'STOPPED' && !svc.core"
                  @click="handleStart(svc.name)" :disabled="svc._loading"
                  class="text-[10px] px-2 py-1 rounded bg-green-400/10 text-green-300 border border-green-400/20 hover:bg-green-400/20 transition disabled:opacity-40">
                  {{ svc._loading ? '...' : '启动' }}
                </button>
                <button v-if="svc.status === 'RUNNING' && !svc.core"
                  @click="handleStop(svc.name)" :disabled="svc._loading"
                  class="text-[10px] px-2 py-1 rounded bg-red-400/10 text-red-300 border border-red-400/20 hover:bg-red-400/20 transition disabled:opacity-40">
                  {{ svc._loading ? '...' : '停止' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Quick Stats -->
        <div class="glass-panel p-6 motions-reveal delay-500" v-reveal>
          <h3 class="text-sm font-bold text-white mb-4">System Info</h3>
          <div class="space-y-3">
            <div class="flex justify-between items-center py-2 border-b border-white/[0.04]">
              <span class="text-xs text-gray-500">运行时间</span>
              <span class="text-xs text-white/70 matrix-text">72h 15m</span>
            </div>
            <div class="flex justify-between items-center py-2 border-b border-white/[0.04]">
              <span class="text-xs text-gray-500">CPU 使用率</span>
              <span class="text-xs text-white/70 matrix-text">23.4%</span>
            </div>
            <div class="flex justify-between items-center py-2 border-b border-white/[0.04]">
              <span class="text-xs text-gray-500">内存使用</span>
              <span class="text-xs text-white/70 matrix-text">4.2 / 8 GB</span>
            </div>
            <div class="flex justify-between items-center py-2 border-b border-white/[0.04]">
              <span class="text-xs text-gray-500">Redis 命中率</span>
              <span class="text-xs text-green-400 matrix-text">98.7%</span>
            </div>
            <div class="flex justify-between items-center py-2">
              <span class="text-xs text-gray-500">MQ 队列</span>
              <span class="text-xs text-white/70 matrix-text">12 条</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Recent Logs -->
      <div class="mt-6 glass-panel p-6 motions-reveal delay-600" v-reveal>
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-bold text-white">Recent Operations</h3>
          <button class="text-[11px] text-purple-400 hover:text-purple-300 transition">View All</button>
        </div>
        <div class="overflow-x-auto">
          <table class="web3-table">
            <thead>
              <tr>
                <th>Operator</th>
                <th>Module</th>
                <th>Action</th>
                <th>Target</th>
                <th>Time</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in recentLogs" :key="log.id">
                <td class="text-white/80">{{ log.operatorName }}</td>
                <td><span class="web3-badge-purple">{{ log.module }}</span></td>
                <td class="text-white/60">{{ log.action }}</td>
                <td class="text-white/50">{{ log.targetName || log.targetId }}</td>
                <td class="text-gray-500 matrix-text text-xs">{{ formatTime(log.createdAt) }}</td>
              </tr>
              <tr v-if="!recentLogs.length">
                <td colspan="5" class="text-center text-gray-600 py-8">暂无操作日志</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 后台首页：核心指标卡片 + 服务状态 + 最近操作日志，
// 数据优先来自接口，失败时使用内置 mock
// ====================================================
import { ref, onMounted } from 'vue'
import { getDashboard, getAdminLogs, getVisitSummary, getServicesHealth, startService, stopService, getDefaultServices, mergeServices } from '@/api/admin'
import { confirm as dlgConfirm, alert as dlgAlert } from '@/composables/useDialog'

// 核心指标卡片（用户/文章/订单/营收/今日访问）— 全部来自接口
const stats = ref([
  { label: 'Total Users', value: '--', icon: '👤', iconBg: 'bg-purple-500/15', badgeClass: 'web3-badge-green', change: '' },
  { label: 'Articles', value: '--', icon: '📝', iconBg: 'bg-cyan-500/15', badgeClass: 'web3-badge-green', change: '' },
  { label: 'Orders', value: '--', icon: '🛒', iconBg: 'bg-orange-500/15', badgeClass: 'web3-badge-purple', change: '' },
  { label: 'Revenue', value: '--', icon: '💰', iconBg: 'bg-green-500/15', badgeClass: 'web3-badge-green', change: '' },
  { label: '今日访问', value: '--', icon: '📡', iconBg: 'bg-teal-500/15' },
])

// 微服务运行状态列表 — 初始化即带注册表兜底
const services = ref(getDefaultServices())

// 最近操作日志列表 — 来自接口
const recentLogs = ref([])

// 格式化为 HH:mm:ss 时间
function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

// 挂载时依次加载仪表盘统计、服务状态、操作日志与访问汇总
onMounted(async () => {
  try {
    const res = await getDashboard()
    if (res.data) {
      const d = res.data
      stats.value[0].value = (d.totalUsers || 0).toLocaleString()
      stats.value[1].value = (d.totalArticles || 0).toString()
      stats.value[2].value = (d.totalOrders || 0).toLocaleString()
      stats.value[3].value = '¥' + ((d.totalRevenue || 0) / 1000).toFixed(0) + 'K'
      if (d.todayVisits || d.totalUsers) stats.value[4].value = (d.todayVisits || 0).toLocaleString()
      if (d.serviceStatus) {
        services.value = mergeServices(Object.entries(d.serviceStatus).map(([name, status]) => ({ name, status, label: '', port: 0, core: false })))
      }
    }
  } catch (e) {
    console.warn('Admin dashboard API unavailable:', e.message)
  }

  try {
    const res = await getAdminLogs(10)
    if (res.data && res.data.length) {
      recentLogs.value = res.data
    }
  } catch (e) {
    console.warn('Admin logs API unavailable:', e.message)
  }

  try {
    const res = await getVisitSummary()
    if (res.data && res.data.today != null) {
      stats.value[4].value = res.data.today.toLocaleString()
    }
  } catch {}

  // 加载服务健康状态
  await refreshServices()
})

async function refreshServices() {
  try {
    const res = await getServicesHealth()
    if (res.data) {
      services.value = mergeServices(res.data)
    }
  } catch (e) {
    console.warn('Service health API unavailable, using registry fallback:', e.message)
  }
}

async function handleStart(name) {
  const svc = services.value.find(s => s.name === name)
  if (!svc) return
  svc._loading = true
  try {
    const res = await startService(name)
    const msg = res.data?.message || res.message || '操作完成'
    await dlgAlert(msg)
    await refreshServices()
  } catch (e) {
    await dlgAlert(e?.response?.data?.message || e?.message || '操作失败')
  } finally { svc._loading = false }
}

async function handleStop(name) {
  if (!(await dlgConfirm('确定停止该服务？'))) return
  const svc = services.value.find(s => s.name === name)
  if (!svc) return
  svc._loading = true
  try {
    const res = await stopService(name)
    const msg = res.data?.message || res.message || '操作完成'
    await dlgAlert(msg)
    await refreshServices()
  } catch (e) {
    await dlgAlert(e?.response?.data?.message || e?.message || '操作失败')
  } finally { svc._loading = false }
}
</script>

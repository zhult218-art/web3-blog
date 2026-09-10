<template>
  <div class="min-h-screen py-8 px-6">
    <div class="max-w-5xl mx-auto">
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-white motions-reveal" v-reveal>
          <span class="text-gradient-cyber">Service</span> Management
        </h1>
        <p class="mt-1 text-sm text-gray-500 motions-reveal delay-100" v-reveal>微服务健康监控 · 启停控制</p>
      </div>

      <!-- 控制栏 -->
      <div class="flex items-center justify-between mb-6 motions-reveal delay-200" v-reveal>
        <div class="flex items-center gap-3">
          <span class="text-xs text-gray-500">自动刷新</span>
          <button @click="toggleAutoRefresh" class="relative w-10 h-5 rounded-full transition-colors"
            :class="autoRefresh ? 'bg-green-500/30' : 'bg-white/10'">
            <span class="absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-all"
              :class="autoRefresh ? 'left-5' : 'left-0.5'"></span>
          </button>
          <span class="text-[11px] text-gray-600">30秒</span>
        </div>
        <button @click="refresh" class="web3-btn text-xs !px-4">
          {{ loading ? '刷新中...' : '刷新状态' }}
        </button>
      </div>

      <!-- 服务列表 -->
      <div class="space-y-3 motions-reveal delay-300" v-reveal>
        <div v-for="svc in services" :key="svc.name"
          class="glass-panel p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
          :class="svc.status === 'RUNNING' ? 'border-green-400/10' : 'border-red-400/10'">
          <div class="flex items-center gap-4">
            <!-- 状态灯 -->
            <div class="relative">
              <span class="w-3 h-3 rounded-full block" :class="svc.status === 'RUNNING' ? 'bg-green-400 shadow-[0_0_8px_rgba(16,185,129,0.6)]' : 'bg-red-400 shadow-[0_0_8px_rgba(239,68,68,0.5)]'"></span>
              <span v-if="svc.status === 'RUNNING'" class="absolute inset-0 rounded-full bg-green-400 animate-ping opacity-30"></span>
            </div>
            <!-- 信息 -->
            <div>
              <div class="flex items-center gap-2">
                <span class="text-sm font-bold text-white">{{ svc.label }}</span>
                <span class="text-[10px] px-1.5 py-0.5 rounded bg-[#12122e] text-gray-500 font-mono">:{{ svc.port }}</span>
                <span v-if="svc.core" class="text-[9px] px-1.5 py-0.5 rounded bg-amber-400/10 text-amber-300 border border-amber-400/20 font-bold">CORE</span>
              </div>
              <div class="text-[11px] text-gray-500 mt-0.5 font-mono">{{ svc.name }}</div>
            </div>
          </div>
          <!-- 操作 -->
          <div class="flex items-center gap-3">
            <span class="text-xs font-medium" :class="svc.status === 'RUNNING' ? 'text-green-400' : 'text-red-400'">
              {{ svc.status === 'RUNNING' ? '运行中' : '已停止' }}
            </span>
            <div class="flex gap-2">
              <button v-if="svc.status === 'STOPPED' && !svc.core"
                @click="handleStart(svc)" :disabled="svc._loading"
                class="svc-btn svc-btn-start disabled:opacity-40">
                <svg v-if="!svc._loading" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/></svg>
                <span v-else class="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin"></span>
                启动
              </button>
              <button v-if="svc.status === 'RUNNING' && !svc.core"
                @click="handleStop(svc)" :disabled="svc._loading"
                class="svc-btn svc-btn-stop disabled:opacity-40">
                <svg v-if="!svc._loading" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M2 4.75A.75.75 0 012.75 4h14.5a.75.75 0 010 1.5H2.75A.75.75 0 012 4.75zM2 10a.75.75 0 01.75-.75h14.5a.75.75 0 010 1.5H2.75A.75.75 0 012 10zm0 5.25a.75.75 0 01.75-.75h14.5a.75.75 0 010 1.5H2.75a.75.75 0 01-.75-.75z" clip-rule="evenodd"/></svg>
                <span v-else class="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin"></span>
                停止
              </button>
              <span v-if="svc.core" class="text-[10px] text-amber-300/60 self-center">不可停止</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 统计 -->
      <div class="mt-6 glass-panel p-4 flex items-center justify-between motions-reveal delay-400" v-reveal>
        <div class="flex items-center gap-6">
          <div class="text-xs text-gray-500">总计 <span class="text-white font-bold">{{ services.length }}</span> 个服务</div>
          <div class="text-xs text-green-400">运行 <span class="font-bold">{{ services.filter(s => s.status === 'RUNNING').length }}</span></div>
          <div class="text-xs text-red-400">停止 <span class="font-bold">{{ services.filter(s => s.status === 'STOPPED').length }}</span></div>
        </div>
        <div class="text-[10px] text-gray-600">上次刷新: {{ lastRefresh }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getServicesHealth, startService, stopService, getDefaultServices, mergeServices } from '@/api/admin'
import { confirm as dlgConfirm, alert as dlgAlert } from '@/composables/useDialog'

const services = ref(getDefaultServices())
const loading = ref(false)
const autoRefresh = ref(true)
const lastRefresh = ref('--')
let timer = null

async function refresh() {
  loading.value = true
  try {
    const res = await getServicesHealth()
    if (res.data) services.value = mergeServices(res.data)
    lastRefresh.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    lastRefresh.value = new Date().toLocaleTimeString('zh-CN') + ' (离线)'
  }
  loading.value = false
}

function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value
  if (timer) { clearInterval(timer); timer = null }
  if (autoRefresh.value) timer = setInterval(refresh, 30000)
}

async function handleStart(svc) {
  svc._loading = true
  try {
    const res = await startService(svc.name)
    const msg = res.data?.message || res.message || '操作完成'
    await dlgAlert(msg)
    await refresh()
  } catch (e) { await dlgAlert(e?.response?.data?.message || '操作失败') }
  finally { svc._loading = false }
}

async function handleStop(svc) {
  if (!(await dlgConfirm(`确定停止 ${svc.label}？`))) return
  svc._loading = true
  try {
    const res = await stopService(svc.name)
    const msg = res.data?.message || res.message || '操作完成'
    await dlgAlert(msg)
    await refresh()
  } catch (e) { await dlgAlert(e?.response?.data?.message || '操作失败') }
  finally { svc._loading = false }
}

onMounted(() => { refresh(); if (autoRefresh.value) timer = setInterval(refresh, 30000) })
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.svc-btn {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] font-medium border transition-all;
}
.svc-btn-start {
  @apply bg-green-400/10 border-green-400/20 text-green-300 hover:bg-green-400/20 hover:border-green-400/30;
}
.svc-btn-stop {
  @apply bg-red-400/10 border-red-400/20 text-red-300 hover:bg-red-400/20 hover:border-red-400/30;
}
</style>

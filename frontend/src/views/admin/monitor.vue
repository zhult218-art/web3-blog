<template>
  <!-- v-reveal 挂在整页根节点：挂载时整页与视口相交，一次性为内部所有
       .motions-reveal 元素加上 .visible，既保留渐显动效又保证内容一定可见 -->
  <div class="min-h-screen py-8 px-6" v-reveal>
    <div class="max-w-7xl mx-auto">
      <!-- ==================== 页头 ==================== -->
      <div class="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div>
          <h1 class="text-2xl font-bold text-white motions-reveal">
            <span class="text-gradient-cyber">系统</span>监控
          </h1>
          <p class="mt-1 text-sm text-gray-500 motions-reveal delay-100">全局资源 · 服务 · 中间件 · 安全态势</p>
        </div>
        <div class="flex items-center gap-3 motions-reveal delay-200">
          <span class="text-xs text-gray-500">自动刷新</span>
          <button @click="toggleAutoRefresh" class="relative w-10 h-5 rounded-full transition-colors"
            :class="autoRefresh ? 'bg-green-500/30' : 'bg-white/10'" title="开关 5 秒自动刷新">
            <span class="absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-all"
              :class="autoRefresh ? 'left-5' : 'left-0.5'"></span>
          </button>
          <span class="text-[11px] text-gray-600">5秒</span>
          <button @click="refreshAll" class="web3-btn text-xs !px-4">{{ loading ? '刷新中...' : '刷新' }}</button>
          <span class="text-[10px] matrix-text whitespace-nowrap" :class="connError ? 'text-rose-400' : 'text-gray-500'">
            {{ connError ? '连接失败' : '最后更新 ' + lastUpdate }}
          </span>
        </div>
      </div>

      <!-- ==================== 告警横幅 ==================== -->
      <div v-if="alerts.length" class="space-y-2 mb-5">
        <div v-for="(a, i) in alerts" :key="i" class="alert-ribbon"
          :class="a.level === 'critical' ? 'alert-critical' : 'alert-warn'">
          <span class="font-bold flex-shrink-0">{{ a.level === 'critical' ? '严重' : '警告' }} · {{ a.title || '系统告警' }}</span>
          <span class="opacity-80 min-w-0">{{ a.detail || '' }}</span>
        </div>
      </div>

      <!-- ==================== 1. 系统总览 ==================== -->
      <section class="mb-8">
        <h2 class="section-title motions-reveal">系统总览</h2>

        <!-- 骨架屏（首次加载） -->
        <div v-if="!loaded" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4 mb-4">
          <div v-for="n in 6" :key="n" class="skeleton h-24"></div>
        </div>
        <!-- 指标卡 -->
        <div v-else class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4 mb-4">
          <div class="glass-panel p-4">
            <div class="stat-label">CPU 占用率</div>
            <div class="stat-value" :class="pctClass(cpu.percent)">{{ fmtPct(cpu.percent) }}</div>
            <div class="stat-sub matrix-text">
              {{ cpu.count != null ? cpu.count + ' 核' : '—' }}<template v-if="cpu.freq_mhz != null"> · {{ Math.round(cpu.freq_mhz) }} MHz</template>
            </div>
            <div v-if="cpuLoadText" class="stat-sub matrix-text">load {{ cpuLoadText }}</div>
            <div v-if="cpuCores.length" class="flex items-end gap-[2px] h-5 mt-2">
              <span v-for="(pc, pi) in cpuCores" :key="pi" class="core-bar" :style="{ height: coreBarHeight(pc) }" :title="fmtPct(pc)"></span>
            </div>
          </div>

          <div class="glass-panel p-4">
            <div class="stat-label">物理内存</div>
            <div class="stat-value" :class="pctClass(memory.percent, 75, 90)">{{ fmtPct(memory.percent) }}</div>
            <div class="stat-sub matrix-text">{{ fmtMB(memory.used_mb) }} / {{ fmtMB(memory.total_mb) }}</div>
            <div class="stat-sub matrix-text">可用 {{ fmtMB(memory.available_mb) }}</div>
          </div>

          <div class="glass-panel p-4" :class="commitLow ? 'border-rose-400/30' : ''">
            <div class="stat-label">提交内存 Commit</div>
            <div class="stat-value" :class="commitLow ? 'text-rose-400' : pctClass(commit.percent, 80, 92)">{{ fmtPct(commit.percent) }}</div>
            <div class="stat-sub matrix-text">余量 <span :class="commitLow ? 'text-rose-400 font-bold' : 'text-emerald-400'">{{ fmtGB(commit.headroom_gb) }}</span> / 上限 {{ fmtGB(commit.limit_gb) }}</div>
            <div class="stat-sub matrix-text">已提交 {{ fmtGB(commit.committed_gb) }}</div>
          </div>

          <div class="glass-panel p-4">
            <div class="stat-label">GPU 占用率</div>
            <div class="stat-value" :class="gpuAvailable ? pctClass(gpu.util_percent) : 'text-gray-500'">
              {{ gpuAvailable ? fmtPct(gpu.util_percent) : '不可用' }}
            </div>
            <div class="stat-sub matrix-text break-words">{{ gpu.name || '—' }}</div>
            <div v-if="gpuAvailable" class="stat-sub matrix-text">显存 {{ fmtMB(gpu.dedicated_mb) }}</div>
          </div>

          <div class="glass-panel p-4">
            <div class="stat-label">系统运行时长</div>
            <div class="stat-value text-white">{{ fmtUptimeHours(host.uptime_hours) }}</div>
            <div class="stat-sub matrix-text">启动 {{ fmtBootTime(host.boot_time) }}</div>
          </div>

          <div class="glass-panel p-4">
            <div class="stat-label">CPU 型号</div>
            <div class="text-sm font-bold text-white/90 leading-snug mt-1 break-words">{{ host.cpu_model || '—' }}</div>
            <div class="stat-sub matrix-text break-words">{{ host.platform || host.os || '—' }}</div>
          </div>
        </div>

        <!-- 实时趋势折线图 -->
        <div class="glass-panel p-4 mb-4">
          <h3 class="text-sm font-bold text-white mb-2">
            实时资源趋势 <span class="text-gray-500 text-[11px] font-normal">CPU % / 内存 %（最近 {{ trendCount }} 个采样点）</span>
          </h3>
          <div ref="trendChart" class="h-56"></div>
        </div>

        <!-- 磁盘 + 网络 -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div class="glass-panel p-4">
            <h3 class="text-sm font-bold text-white mb-3">磁盘使用</h3>
            <div v-if="diskList.length" class="space-y-3">
              <div v-for="(d, di) in diskList" :key="di">
                <div class="flex items-center justify-between text-xs mb-1 gap-2">
                  <span class="text-white/80 matrix-text truncate" :title="d.mount">{{ d.mount || '—' }}</span>
                  <span class="text-gray-500 matrix-text whitespace-nowrap">{{ fmtGB(d.used_gb) }} / {{ fmtGB(d.total_gb) }} · {{ fmtPct(d.percent) }}</span>
                </div>
                <div class="bar-track"><div class="bar-fill" :class="barColor(d.percent)" :style="{ width: barWidth(d.percent) }"></div></div>
              </div>
            </div>
            <div v-else class="empty-hint">暂无磁盘数据</div>
          </div>

          <div class="glass-panel p-4">
            <h3 class="text-sm font-bold text-white mb-3">网络速率</h3>
            <div class="grid grid-cols-2 gap-3">
              <div class="net-box">
                <div class="stat-label">上行</div>
                <div class="text-lg font-bold matrix-text text-cyan-300">{{ fmtRate(net.sent_kbps) }}</div>
                <div class="stat-sub">累计 {{ fmtBytes(net.bytes_sent) }}</div>
              </div>
              <div class="net-box">
                <div class="stat-label">下行</div>
                <div class="text-lg font-bold matrix-text text-violet-300">{{ fmtRate(net.recv_kbps) }}</div>
                <div class="stat-sub">累计 {{ fmtBytes(net.bytes_recv) }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ==================== 2. 各服务资源 ==================== -->
      <section class="mb-8">
        <div class="flex flex-wrap items-baseline justify-between gap-2 mb-3">
          <h2 class="section-title motions-reveal !mb-0">各服务资源</h2>
          <span class="text-xs text-gray-400 matrix-text">{{ svcSummary }}</span>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <!-- 分组表 -->
          <div class="lg:col-span-2 space-y-4">
            <div v-for="g in serviceGroups" :key="g.kind" class="glass-panel overflow-hidden">
              <div class="px-4 py-2.5 border-b border-white/[0.06] flex items-center justify-between">
                <h3 class="text-sm font-bold text-white">{{ g.label }}</h3>
                <span class="text-[10px] text-gray-500">{{ g.items.length }} 个 · {{ upCount(g) }} 运行中</span>
              </div>
              <div class="overflow-x-auto">
                <table class="web3-table min-w-[680px]">
                  <thead>
                    <tr><th>服务</th><th>状态</th><th>内存</th><th>CPU</th><th>线程</th><th>运行时长</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="s in g.items" :key="s.name">
                      <td>
                        <div class="flex items-center gap-2">
                          <span class="text-white/90 font-medium">{{ s.label || s.name || '—' }}</span>
                          <span class="text-[10px] px-1.5 py-0.5 rounded bg-[#12122e] text-gray-500 font-mono">:{{ s.port ?? '—' }}</span>
                        </div>
                      </td>
                      <td>
                        <span class="dot" :class="s.up ? 'dot-up' : 'dot-down'"></span>
                        <span class="text-[11px] ml-1" :class="s.up ? 'text-emerald-400' : 'text-rose-400'">{{ s.up ? '运行中' : '已停止' }}</span>
                      </td>
                      <td class="matrix-text text-xs">{{ fmtMB(s.mem_mb) }}</td>
                      <td class="matrix-text text-xs">{{ fmtPct(s.cpu_percent) }}</td>
                      <td class="matrix-text text-xs">{{ s.threads ?? '—' }}</td>
                      <td class="matrix-text text-xs whitespace-nowrap">{{ fmtUptime(s.uptime_s) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div v-if="!serviceGroups.length" class="glass-panel p-8 text-center text-gray-600 text-xs">暂无服务数据</div>
          </div>

          <!-- 服务内存占比 -->
          <div class="glass-panel p-4">
            <h3 class="text-sm font-bold text-white mb-2">服务内存占比 <span class="text-gray-500 text-[11px] font-normal">Top 10</span></h3>
            <div ref="svcMemChart" class="h-72"></div>
            <div v-if="!svcMemHasData" class="empty-hint !py-2">暂无数据</div>
          </div>
        </div>
      </section>

      <!-- ==================== 3. 中间件 ==================== -->
      <section class="mb-8">
        <h2 class="section-title motions-reveal">中间件</h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div v-for="(m, mi) in middlewareItems" :key="mi" class="glass-panel p-4"
            :class="m.up ? 'border-emerald-400/10' : 'border-rose-400/10'">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2 min-w-0">
                <span class="dot" :class="m.up ? 'dot-up' : 'dot-down'"></span>
                <span class="text-sm font-bold text-white truncate">{{ mwLabel(m.name) }}</span>
                <span class="text-[10px] px-1.5 py-0.5 rounded bg-[#12122e] text-gray-500 font-mono flex-shrink-0">:{{ m.port ?? '—' }}</span>
              </div>
              <span class="text-[11px] flex-shrink-0" :class="m.up ? 'text-emerald-400' : 'text-rose-400'">{{ m.up ? '在线' : '离线' }}</span>
            </div>
            <div class="grid grid-cols-3 gap-2 mb-3">
              <div>
                <div class="stat-label">内存</div>
                <div class="text-xs text-white/80 matrix-text">{{ fmtMB(m.mem_mb) }}</div>
              </div>
              <div>
                <div class="stat-label">CPU</div>
                <div class="text-xs text-white/80 matrix-text">{{ fmtPct(m.cpu_percent) }}</div>
              </div>
              <div>
                <div class="stat-label">版本</div>
                <div class="text-xs text-white/80 matrix-text truncate" :title="m.version">{{ m.version || '—' }}</div>
              </div>
            </div>
            <div v-if="detailRows(m).length" class="grid grid-cols-2 gap-x-3 gap-y-1.5 border-t border-white/[0.06] pt-3">
              <div v-for="r in detailRows(m)" :key="r.label" class="flex items-center justify-between text-[11px] gap-2">
                <span class="text-gray-500 flex-shrink-0">{{ r.label }}</span>
                <span class="text-white/80 matrix-text truncate" :title="String(r.value)">{{ r.value }}</span>
              </div>
            </div>
            <div v-if="m.error" class="mt-2 text-[11px] text-rose-400/80">⚠ {{ m.error }}</div>
          </div>
          <div v-if="!middlewareItems.length" class="glass-panel p-8 text-center text-gray-600 text-xs md:col-span-3">暂无中间件数据</div>
        </div>
      </section>

      <!-- ==================== 4. 安全态势 ==================== -->
      <section class="mb-8">
        <h2 class="section-title motions-reveal">安全态势</h2>
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
          <div v-for="c in secSummaryCards" :key="c.label" class="glass-panel p-4">
            <div class="text-2xl font-black matrix-text text-white">{{ c.value }}</div>
            <div class="text-[11px] text-gray-500 mt-1 tracking-wider uppercase">{{ c.label }}</div>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <!-- 被拦截 IP -->
          <div class="glass-panel p-4">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-sm font-bold text-white">被拦截 IP</h3>
              <span class="text-[10px] text-gray-500">{{ blocklist.length }} 个</span>
            </div>
            <div v-if="blocklist.length" class="flex flex-wrap gap-1.5 max-h-56 overflow-y-auto pr-1">
              <span v-for="(ip, bi) in blocklist" :key="bi" class="tag-chip">{{ ip }}</span>
            </div>
            <div v-else class="empty-hint">暂无记录</div>
          </div>

          <!-- 被限流 IP -->
          <div class="glass-panel p-4">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-sm font-bold text-white">被限流 IP</h3>
              <span class="text-[10px] text-gray-500">{{ rateLimited.length }} 个</span>
            </div>
            <div v-if="rateLimited.length" class="space-y-1.5 max-h-56 overflow-y-auto pr-1">
              <div v-for="(r, ri) in rateLimited" :key="ri" class="rl-row">
                <span class="matrix-text text-[11px] text-white/80">{{ r.ip || '—' }}</span>
                <span class="web3-badge-purple flex-shrink-0">{{ r.bucket || '—' }}</span>
                <span class="matrix-text text-[11px] text-amber-300/90 flex-shrink-0">{{ fmtTok(r.tokens) }}</span>
                <span v-if="r.scope" class="text-[10px] text-gray-500 flex-shrink-0">{{ r.scope === 'global' ? '全局' : '路径' }}</span>
              </div>
            </div>
            <div v-else class="empty-hint">暂无记录</div>
          </div>

          <!-- 安全事件时间线 -->
          <div class="glass-panel p-4">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-sm font-bold text-white">最近安全事件</h3>
              <span class="text-[10px] text-gray-500">{{ secEvents.length }} 条</span>
            </div>
            <div v-if="secEvents.length" class="space-y-2 max-h-56 overflow-y-auto pr-1">
              <div v-for="(e, ei) in secEvents" :key="ei" class="ev-row">
                <span class="ev-badge flex-shrink-0" :class="e.type === 'ip_block' ? 'ev-badge-red' : 'ev-badge-amber'">{{ eventTypeLabel(e.type) }}</span>
                <span class="matrix-text text-[11px] text-white/80 flex-shrink-0">{{ e.ip || '—' }}</span>
                <span class="matrix-text text-[10px] text-gray-500 flex-shrink-0">{{ fmtTsStr(e.ts) }}</span>
                <span class="text-[11px] text-gray-400 min-w-0 truncate" :title="e.detail">{{ e.detail || '' }}</span>
              </div>
            </div>
            <div v-else class="empty-hint">暂无记录</div>
          </div>
        </div>
      </section>

      <!-- ==================== 5. 异常与日志 ==================== -->
      <section class="mb-4">
        <h2 class="section-title motions-reveal">异常与日志</h2>

        <!-- JVM 崩溃表 -->
        <div class="glass-panel overflow-hidden mb-4">
          <div class="px-4 py-2.5 border-b border-white/[0.06] flex items-center justify-between">
            <h3 class="text-sm font-bold text-white">近期 JVM 崩溃</h3>
            <span class="text-[10px] text-gray-500">{{ crashes.length }} 条</span>
          </div>
          <div class="overflow-x-auto">
            <table class="web3-table min-w-[560px]">
              <thead>
                <tr><th>服务</th><th>时间</th><th>原因</th></tr>
              </thead>
              <tbody>
                <tr v-for="(c, ci) in crashes" :key="ci">
                  <td class="text-white/80">{{ c.service || '—' }}</td>
                  <td class="matrix-text text-xs text-gray-500 whitespace-nowrap">{{ fmtTsStr(c.ts) }}</td>
                  <td class="text-white/60 text-xs" :title="c.reason">{{ truncate(c.reason, 80) }}</td>
                </tr>
                <tr v-if="!crashes.length"><td colspan="3" class="text-center text-gray-600 py-6 text-xs">暂无崩溃记录</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 日志尾部 -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div class="glass-panel p-4">
            <div class="flex items-center justify-between mb-2 gap-2">
              <h3 class="text-sm font-bold text-white">Watchdog 日志尾部</h3>
              <span class="text-[10px] text-gray-500 matrix-text truncate" :title="watchdogPath">{{ watchdogPath || '' }}</span>
            </div>
            <pre v-if="watchdogExists && watchdogTail.length" class="log-pre">{{ watchdogTail.join('\n') }}</pre>
            <div v-else class="empty-hint !py-8">暂无日志</div>
          </div>

          <div class="glass-panel p-4">
            <div class="flex items-center justify-between mb-2 gap-2">
              <h3 class="text-sm font-bold text-white">滚动重启日志尾部</h3>
              <span class="text-[10px] text-gray-500 matrix-text truncate" :title="rollPath">{{ rollPath || '' }}</span>
            </div>
            <pre v-if="rollExists && rollTail.length" class="log-pre">{{ rollTail.join('\n') }}</pre>
            <div v-else class="empty-hint !py-8">暂无日志</div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 管理后台 · 系统监控页（py-service 9006 的 monitor 模块，经 /monitapi 代理）
// 五个区块：系统总览 / 各服务资源 / 中间件 / 安全态势 / 异常与日志
// 轮询：overview 5s（区块 1-3 + 趋势），security + events 15s
// 全部数值字段可能为 null，统一由格式化函数降级为 "—"，任何异常都不抛出
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'

// 本地独立实例：请求走同源 /monitapi，短超时；不复用全局 request，
// 避免监控接口的失败提示污染全局 Toast
const http = axios.create({ timeout: 8000 })

// ---------------- 原始数据 ----------------
const sysInfo = ref(null) // overview.system
const svcInfo = ref(null) // overview.services
const mwInfo = ref(null)  // overview.middleware
const secInfo = ref(null) // /security
const evInfo = ref(null)  // /events

// ---------------- 页面状态 ----------------
const autoRefresh = ref(true)
const loading = ref(false)
const loaded = ref(false)     // overview 是否完成过首次尝试（决定骨架屏）
const connError = ref(false)  // overview 最近一次是否失败 -> 顶栏"连接失败"
const lastUpdate = ref('--')

// ---------------- 安全取值封装（模板中避免可选链依赖） ----------------
const system = computed(() => sysInfo.value || {})
const cpu = computed(() => system.value.cpu || {})
const memory = computed(() => system.value.memory || {})
const commit = computed(() => system.value.commit || {})
const gpu = computed(() => system.value.gpu || {})
const host = computed(() => system.value.host || {})
const net = computed(() => system.value.net || {})
const alerts = computed(() => (Array.isArray(system.value.alerts) ? system.value.alerts : []))
const diskList = computed(() => (Array.isArray(system.value.disk) ? system.value.disk : []))
const cpuCores = computed(() => (Array.isArray(cpu.value.per_core) ? cpu.value.per_core : []))

// GPU 是否可用（available 缺省视为可用）
const gpuAvailable = computed(() => gpu.value.available !== false)

// Commit 余量是否偏低：余量占比 < 10% 或已用 >= 90%
const commitLow = computed(() => {
  const c = commit.value
  const limit = numOrNull(c.limit_gb)
  const head = numOrNull(c.headroom_gb)
  if (limit != null && limit > 0 && head != null && head / limit < 0.1) return true
  const p = numOrNull(c.percent)
  return p != null && p >= 90
})

// CPU load_avg 文本
const cpuLoadText = computed(() => {
  const l = cpu.value.load_avg
  if (!Array.isArray(l) || !l.length) return ''
  return l.map(x => (numOrNull(x) ?? 0).toFixed(2)).join(' / ')
})

// 服务汇总行
const svcSummary = computed(() => {
  const s = svcInfo.value?.summary || {}
  const up = numOrNull(s.up) ?? 0
  const total = numOrNull(s.total) ?? 0
  return `${up}/${total} 运行中 · 合计内存 ${fmtMB(s.mem_mb)}`
})

// 服务按 kind 分组（组内按内存降序）
const KIND_LABELS = { java: 'Java 微服务', node: 'Node', python: 'Python', vite: 'Vite' }
const KIND_ORDER = ['java', 'node', 'python', 'vite']
const serviceGroups = computed(() => {
  const list = Array.isArray(svcInfo.value?.services) ? svcInfo.value.services : []
  const map = {}
  for (const s of list) {
    const k = s.kind || 'other'
    if (!map[k]) map[k] = []
    map[k].push(s)
  }
  const known = KIND_ORDER.filter(k => map[k])
  const others = Object.keys(map).filter(k => !KIND_ORDER.includes(k))
  return [...known, ...others].map(k => ({
    kind: k,
    label: KIND_LABELS[k] || (k === 'other' ? '其他' : k),
    items: map[k].slice().sort((a, b) => (numOrNull(b.mem_mb) ?? 0) - (numOrNull(a.mem_mb) ?? 0))
  }))
})

// 中间件列表
const middlewareItems = computed(() => (Array.isArray(mwInfo.value?.items) ? mwInfo.value.items : []))

// 安全态势
const secSummaryCards = computed(() => {
  const s = secInfo.value?.summary || {}
  return [
    { label: '被拦截 IP', value: fmtCount(s.blocked_ips) },
    { label: '被限流 IP', value: fmtCount(s.rate_limited_ips) },
    { label: '安全事件', value: fmtCount(s.events) },
    { label: '扫描日志行', value: fmtCount(s.log_lines_scanned) }
  ]
})
const blocklist = computed(() => (Array.isArray(secInfo.value?.blocklist) ? secInfo.value.blocklist : []))
const rateLimited = computed(() => (Array.isArray(secInfo.value?.rate_limited) ? secInfo.value.rate_limited : []))
const secEvents = computed(() => (Array.isArray(secInfo.value?.events) ? secInfo.value.events : []))

// 异常与日志
const crashes = computed(() => (Array.isArray(evInfo.value?.crashes) ? evInfo.value.crashes : []))
const watchdogExists = computed(() => !!evInfo.value?.watchdog?.exists)
const watchdogTail = computed(() => (Array.isArray(evInfo.value?.watchdog?.tail) ? evInfo.value.watchdog.tail : []))
const watchdogPath = computed(() => evInfo.value?.watchdog?.path || '')
const rollExists = computed(() => !!evInfo.value?.roll?.exists)
const rollTail = computed(() => (Array.isArray(evInfo.value?.roll?.tail) ? evInfo.value.roll.tail : []))
const rollPath = computed(() => evInfo.value?.roll?.path || '')

// ---------------- 格式化工具（全部容错，null 一律 "—"） ----------------
function numOrNull(v) {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}
function fmtPct(v, d = 1) {
  const n = numOrNull(v)
  return n == null ? '—' : n.toFixed(d) + '%'
}
function fmtMB(v) {
  const n = numOrNull(v)
  if (n == null) return '—'
  return Math.abs(n) >= 1024 ? (n / 1024).toFixed(2) + ' GB' : n.toFixed(0) + ' MB'
}
function fmtGB(v, d = 2) {
  const n = numOrNull(v)
  return n == null ? '—' : n.toFixed(d) + ' GB'
}
function fmtCount(v) {
  const n = numOrNull(v)
  return n == null ? '—' : n.toLocaleString('zh-CN')
}
function fmtTok(v) {
  const n = numOrNull(v)
  return n == null ? '—' : n.toFixed(1)
}
function fmtRate(v) {
  const n = numOrNull(v)
  if (n == null) return '—'
  return Math.abs(n) >= 1024 ? (n / 1024).toFixed(2) + ' MB/s' : n.toFixed(1) + ' KB/s'
}
function fmtBytes(v) {
  const n = numOrNull(v)
  if (n == null) return '—'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let x = n
  while (x >= 1024 && i < units.length - 1) { x /= 1024; i++ }
  return x.toFixed(i === 0 ? 0 : 2) + ' ' + units[i]
}
// 秒 -> "2天3小时" / "3小时5分钟" / "45秒"
function fmtUptime(sec) {
  const n = numOrNull(sec)
  if (n == null || n < 0) return '—'
  const s = Math.floor(n)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (d > 0) return d + '天' + h + '小时'
  if (h > 0) return h + '小时' + m + '分钟'
  if (m > 0) return m + '分钟'
  return s + '秒'
}
function fmtUptimeHours(hours) {
  const n = numOrNull(hours)
  return n == null ? '—' : fmtUptime(n * 3600)
}
function fmtBootTime(ts) {
  const n = numOrNull(ts)
  if (n == null) return '—'
  const d = new Date(n > 1e12 ? n : n * 1000)
  return isNaN(d.getTime()) ? '—' : d.toLocaleString('zh-CN', { hour12: false })
}
function fmtTsStr(v) {
  if (v == null || v === '') return '—'
  const s = String(v)
  return s.includes('T') ? s.replace('T', ' ').slice(5, 19) : s
}
function pctClass(v, warn = 70, crit = 90) {
  const n = numOrNull(v)
  if (n == null) return 'text-gray-400'
  if (n >= crit) return 'text-rose-400'
  if (n >= warn) return 'text-amber-300'
  return 'text-emerald-400'
}
function barWidth(v) {
  const n = numOrNull(v)
  return (n == null ? 0 : Math.max(0, Math.min(100, n))) + '%'
}
function barColor(v) {
  const n = numOrNull(v)
  if (n == null) return 'bg-gray-600'
  if (n >= 90) return 'bg-rose-400'
  if (n >= 70) return 'bg-amber-400'
  return 'bg-gradient-to-r from-cyan-400 to-violet-400'
}
function coreBarHeight(v) {
  const n = numOrNull(v)
  return Math.max(8, Math.min(100, n == null ? 8 : n)) + '%'
}
function truncate(str, max = 80) {
  if (str == null || str === '') return '—'
  const s = String(str)
  return s.length > max ? s.slice(0, max) + '…' : s
}
function upCount(g) { return g.items.filter(i => i.up).length }
function mwLabel(name) {
  if (!name) return '—'
  const s = String(name)
  if (/mysql/i.test(s)) return 'MySQL'
  if (/redis/i.test(s)) return 'Redis'
  if (/nacos/i.test(s)) return 'Nacos'
  return s
}
function eventTypeLabel(t) {
  if (t === 'ip_block') return 'IP 拦截'
  if (t === 'rate_limit') return '限流'
  return t || '事件'
}
// 中间件 detail 子字段展开（MySQL / Redis / Nacos 通用，存在即展示）
function detailRows(item) {
  const d = item && item.detail
  if (!d || typeof d !== 'object') return []
  const rows = []
  if (d.connections !== undefined) rows.push({ label: '连接数', value: fmtCount(d.connections) })
  if (d.threads_running !== undefined) rows.push({ label: '运行线程', value: fmtCount(d.threads_running) })
  if (d.uptime_s !== undefined) rows.push({ label: 'Uptime', value: fmtUptime(d.uptime_s) })
  if (d.slow_queries !== undefined) rows.push({ label: '慢查询', value: fmtCount(d.slow_queries) })
  if (d.questions !== undefined) rows.push({ label: '累计查询', value: fmtCount(d.questions) })
  if (d.used_memory_human !== undefined) rows.push({ label: '已用内存', value: d.used_memory_human })
  else if (d.used_memory_mb !== undefined) rows.push({ label: '已用内存', value: fmtMB(d.used_memory_mb) })
  if (d.connected_clients !== undefined) rows.push({ label: '客户端数', value: fmtCount(d.connected_clients) })
  if (d.total_keys !== undefined) rows.push({ label: '键数量', value: fmtCount(d.total_keys) })
  if (d.registered_services !== undefined) rows.push({ label: '已注册服务', value: fmtCount(d.registered_services) })
  return rows
}

// ---------------- ECharts ----------------
const trendChart = ref(null)
const svcMemChart = ref(null)
let chartTrend = null
let chartSvc = null

// 折线图滚动缓冲：最近 ~40 个采样点
const TREND_MAX = 40
const trendLabels = ref([])
const trendCpu = []
const trendMem = []
const trendCount = computed(() => trendLabels.value.length)
const svcMemHasData = ref(false)

function initCharts() {
  if (trendChart.value) { chartTrend = echarts.init(trendChart.value); updateTrendChart() }
  if (svcMemChart.value) { chartSvc = echarts.init(svcMemChart.value); updateSvcChart() }
}
function pushTrend(sys) {
  const cpuPct = numOrNull(sys?.cpu?.percent)
  const memPct = numOrNull(sys?.memory?.percent)
  trendLabels.value.push(new Date().toLocaleTimeString('zh-CN', { hour12: false }))
  trendCpu.push(cpuPct)
  trendMem.push(memPct)
  if (trendLabels.value.length > TREND_MAX) {
    trendLabels.value.shift(); trendCpu.shift(); trendMem.shift()
  }
  updateTrendChart()
}
function updateTrendChart() {
  if (!chartTrend) return
  chartTrend.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { data: ['CPU %', '内存 %'], textStyle: { color: '#9ca3af', fontSize: 10 }, top: 0 },
    grid: { left: 42, right: 20, top: 30, bottom: 24 },
    xAxis: { type: 'category', boundaryGap: false, data: trendLabels.value, axisLine: { lineStyle: { color: '#374151' } }, axisLabel: { color: '#9ca3af', fontSize: 9 } },
    yAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#1f293766' } }, axisLabel: { color: '#9ca3af', fontSize: 9, formatter: '{value}%' } },
    series: [
      { name: 'CPU %', type: 'line', smooth: true, showSymbol: false, connectNulls: true, data: trendCpu, itemStyle: { color: '#22d3ee' }, areaStyle: { color: 'rgba(34,211,238,0.10)' } },
      { name: '内存 %', type: 'line', smooth: true, showSymbol: false, connectNulls: true, data: trendMem, itemStyle: { color: '#a78bfa' }, areaStyle: { color: 'rgba(167,139,250,0.10)' } }
    ]
  })
}
function updateSvcChart() {
  if (!chartSvc) return
  const list = (Array.isArray(svcInfo.value?.services) ? svcInfo.value.services : []).filter(s => numOrNull(s.mem_mb) != null)
  const top = list.slice().sort((a, b) => numOrNull(b.mem_mb) - numOrNull(a.mem_mb)).slice(0, 10).reverse()
  svcMemHasData.value = top.length > 0
  chartSvc.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      formatter: ps => ps.map((p) => {
        const row = top[p.dataIndex] || {}
        return `${p.marker}${row.label || row.name}: ${fmtMB(row.mem_mb)}`
      }).join('')
    },
    grid: { left: 84, right: 26, top: 10, bottom: 20 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#1f293766' } }, axisLabel: { color: '#9ca3af', fontSize: 9 } },
    yAxis: { type: 'category', data: top.map(s => s.label || s.name || '—'), axisLabel: { color: '#9ca3af', fontSize: 10 }, axisLine: { lineStyle: { color: '#374151' } } },
    series: [{
      type: 'bar',
      barWidth: 14,
      data: top.map(s => Math.round(numOrNull(s.mem_mb) ?? 0)),
      itemStyle: { borderRadius: 7, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#7c3aed' }, { offset: 1, color: '#22d3ee' }]) }
    }]
  })
}
function onResize() {
  if (chartTrend) chartTrend.resize()
  if (chartSvc) chartSvc.resize()
}

// ---------------- 轮询 ----------------
let ctlOverview = null
let ctlSec = null
let ctlEvents = null
let tOverview = null
let tSec = null

function isCanceled(e) {
  return !!(e && (e.code === 'ERR_CANCELED' || axios.isCancel(e)))
}

// 主接口：一次拿齐 system / services / middleware
async function pollOverview() {
  if (ctlOverview) ctlOverview.abort() // 取消被本轮取代的在途请求
  ctlOverview = new AbortController()
  try {
    const res = await http.get('/monitapi/overview', { signal: ctlOverview.signal })
    const data = res.data || {}
    sysInfo.value = data.system || null
    svcInfo.value = data.services || null
    mwInfo.value = data.middleware || null
    pushTrend(data.system || {})
    updateSvcChart()
    connError.value = false
    lastUpdate.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (e) {
    if (!isCanceled(e)) connError.value = true // 失败静默，仅置顶栏状态
  } finally {
    loaded.value = true
  }
}

async function pollSecurity() {
  if (ctlSec) ctlSec.abort()
  ctlSec = new AbortController()
  try {
    const res = await http.get('/monitapi/security', { signal: ctlSec.signal })
    secInfo.value = res.data || null
  } catch (e) {
    // 静默：安全接口失败不打断页面，保留上一轮数据
    isCanceled(e)
  }
}

async function pollEvents() {
  if (ctlEvents) ctlEvents.abort()
  ctlEvents = new AbortController()
  try {
    const res = await http.get('/monitapi/events', { signal: ctlEvents.signal })
    evInfo.value = res.data || null
  } catch (e) {
    // 静默：日志接口失败不打断页面，保留上一轮数据
    isCanceled(e)
  }
}

function startTimers() {
  stopTimers()
  tOverview = setInterval(pollOverview, 5000)
  tSec = setInterval(() => { pollSecurity(); pollEvents() }, 15000)
}
function stopTimers() {
  if (tOverview) { clearInterval(tOverview); tOverview = null }
  if (tSec) { clearInterval(tSec); tSec = null }
}

// 手动刷新：立即拉取全部接口
async function refreshAll() {
  loading.value = true
  try {
    await Promise.all([pollOverview(), pollSecurity(), pollEvents()])
  } finally {
    loading.value = false
  }
}

function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) startTimers()
  else stopTimers()
}

onMounted(async () => {
  window.addEventListener('resize', onResize)
  await nextTick()
  initCharts()
  refreshAll()
  if (autoRefresh.value) startTimers()
})
onBeforeUnmount(() => {
  stopTimers()
  window.removeEventListener('resize', onResize)
  if (ctlOverview) ctlOverview.abort()
  if (ctlSec) ctlSec.abort()
  if (ctlEvents) ctlEvents.abort()
  if (chartTrend) { chartTrend.dispose(); chartTrend = null }
  if (chartSvc) { chartSvc.dispose(); chartSvc = null }
})
</script>

<style scoped>
.section-title {
  @apply text-base font-bold text-white mb-3 flex items-center gap-2;
}
.section-title::before {
  content: '';
  display: block;
  width: 3px;
  height: 15px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--color-accent), var(--color-primary));
  flex-shrink: 0;
}
.stat-label { @apply text-[11px] text-gray-500 uppercase tracking-wider; }
.stat-value { @apply text-2xl font-black mt-1; font-family: var(--font-mono); letter-spacing: 0.05em; }
.stat-sub { @apply text-[10px] text-gray-600 mt-1; }

.alert-ribbon { @apply flex items-center gap-2 px-4 py-2.5 rounded-xl border text-xs; }
.alert-critical { @apply border-rose-400/40 bg-rose-400/10 text-rose-200; }
.alert-warn { @apply border-amber-400/40 bg-amber-400/10 text-amber-200; }

.bar-track { @apply h-1.5 rounded-full bg-white/[0.06] overflow-hidden; }
.bar-fill { @apply h-full rounded-full transition-all duration-500; }

.core-bar { @apply rounded-sm bg-cyan-400/60; flex: 1 1 0; min-width: 2px; }

.net-box { @apply rounded-xl bg-[#0c0c22] border border-white/[0.04] px-3 py-2.5; }

.dot { @apply w-2.5 h-2.5 rounded-full inline-block; }
.dot-up { @apply bg-emerald-400; box-shadow: 0 0 8px rgba(16, 185, 129, 0.6); }
.dot-down { @apply bg-rose-400; box-shadow: 0 0 8px rgba(239, 68, 68, 0.5); }

.tag-chip { @apply inline-flex items-center px-2 py-0.5 rounded-md text-[10px] bg-rose-400/10 border border-rose-400/20 text-rose-300; font-family: var(--font-mono); letter-spacing: 0.05em; }

.rl-row { @apply flex items-center gap-2 px-2.5 py-1.5 rounded-lg bg-[#0c0c22] border border-white/[0.04]; }
.ev-row { @apply flex items-center gap-2 px-2.5 py-1.5 rounded-lg bg-[#0c0c22] border border-white/[0.04]; }
.ev-badge { @apply px-1.5 py-0.5 rounded text-[10px] font-medium border; }
.ev-badge-red { @apply bg-rose-400/10 border-rose-400/20 text-rose-300; }
.ev-badge-amber { @apply bg-amber-400/10 border-amber-400/20 text-amber-300; }

.log-pre {
  @apply m-0 p-3 rounded-lg bg-[#0b0b1e] border border-white/[0.06] text-[10px] leading-relaxed overflow-auto text-gray-400;
  height: 220px;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: var(--font-mono);
  letter-spacing: 0.05em;
}
.empty-hint { @apply text-center text-gray-600 text-xs py-6; }
</style>

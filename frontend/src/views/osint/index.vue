<template>
  <div class="osint-page">
    <!-- 顶部 Banner -->
    <div class="osint-hero">
      <div class="hero-content">
        <h1 class="hero-title">🔍 OSINT 信息收集</h1>
        <p class="hero-subtitle">移植自 SpiderFoot 的事件驱动扫描引擎 · DNS / WHOIS / 子域名 / 端口扫描 / IP 地理定位 / Shodan</p>
      </div>
      <div class="hero-glow"></div>
    </div>

    <div class="osint-body">
      <!-- 左侧：扫描配置 -->
      <div class="scan-panel">
        <div class="panel-header">
          <span class="panel-icon">⚙️</span>
          <span class="panel-title">扫描配置</span>
        </div>

        <!-- 目标输入 -->
        <div class="form-group">
          <label class="form-label">扫描目标</label>
          <div class="target-input-row">
            <input
              v-model="target"
              type="text"
              class="target-input"
              placeholder="域名 example.com / IP 1.2.3.4 / Email user@domain.com / CIDR 10.0.0.0/24"
              @keyup.enter="startScan"
              :disabled="scanning"
            />
            <button
              class="btn-scan"
              @click="startScan"
              :disabled="!target.trim() || scanning"
            >
              <span v-if="!scanning">▶ 开始扫描</span>
              <span v-else class="loading-dots">扫描中</span>
            </button>
          </div>
          <div class="target-hint">支持域名、IP、Email、CIDR 网段</div>
        </div>

        <!-- 模块选择 -->
        <div class="form-group">
          <label class="form-label">
            扫描模块
            <button class="btn-toggle-all" @click="toggleAllModules">
              {{ allSelected ? '取消全选' : '全选' }}
            </button>
          </label>
          <div class="module-grid">
            <label
              v-for="mod in modules"
              :key="mod.name"
              class="module-chip"
              :class="{ active: selectedModules.includes(mod.name), disabled: !mod.available }"
            >
              <input
                type="checkbox"
                :value="mod.name"
                v-model="selectedModules"
                :disabled="!mod.available"
              />
              <span class="chip-icon">{{ mod.category === 'DNS' ? '🌐' : mod.category === 'Public Registries' ? '📋' : mod.category === 'Reconnaissance' ? '📡' : mod.category === 'Geolocation' ? '📍' : '🔎' }}</span>
              <span class="chip-name">{{ mod.title }}</span>
              <span v-if="!mod.available" class="chip-badge">需 Key</span>
            </label>
          </div>
        </div>

        <!-- 能力检测 -->
        <div class="capabilities-box" v-if="caps">
          <div class="cap-row">
            <span class="cap-label">可用模块</span>
            <span class="cap-value">{{ caps.module_count }} 个</span>
          </div>
          <div class="cap-row">
            <span class="cap-label">Shodan API</span>
            <span class="cap-value" :class="{ ok: caps.env_keys.shodan, warn: !caps.env_keys.shodan }">
              {{ caps.env_keys.shodan ? '已配置' : '未配置（SHODAN_API_KEY）' }}
            </span>
          </div>
        </div>

        <!-- 停止按钮 -->
        <button v-if="scanning" class="btn-stop" @click="stopScan">
          ⏹ 停止扫描
        </button>
      </div>

      <!-- 右侧：扫描结果 -->
      <div class="results-panel">
        <div class="panel-header">
          <span class="panel-icon">📊</span>
          <span class="panel-title">扫描结果</span>
          <span v-if="currentJob" class="job-status" :class="currentJob.status">
            {{ statusText(currentJob.status) }}
          </span>
        </div>

        <!-- 空状态 -->
        <div v-if="!currentJob && results.length === 0" class="empty-state">
          <div class="empty-icon">🎯</div>
          <p>输入目标并开始扫描</p>
          <p class="empty-hint">结果将实时显示在此处</p>
        </div>

        <!-- 统计概览 -->
        <div v-if="resultStats.total > 0" class="stats-bar">
          <div class="stat-item">
            <span class="stat-num">{{ resultStats.total }}</span>
            <span class="stat-label">总发现</span>
          </div>
          <div v-for="(count, type) in resultStats.byType" :key="type" class="stat-item">
            <span class="stat-num">{{ count }}</span>
            <span class="stat-label">{{ typeLabel(type) }}</span>
          </div>
        </div>

        <!-- 实时日志 -->
        <div v-if="logs.length > 0" class="log-section">
          <div class="log-header" @click="showLogs = !showLogs">
            <span>📜 实时日志 ({{ logs.length }})</span>
            <span class="toggle">{{ showLogs ? '▼' : '▶' }}</span>
          </div>
          <div v-show="showLogs" class="log-list">
            <div v-for="(line, i) in logs.slice(-100)" :key="i" class="log-line">
              {{ line }}
            </div>
          </div>
        </div>

        <!-- 结果列表 -->
        <div v-if="groupedResults.length > 0" class="result-groups">
          <div v-for="group in groupedResults" :key="group.type" class="result-group">
            <div class="group-header" @click="toggleGroup(group.type)">
              <span class="group-icon">{{ typeIcon(group.type) }}</span>
              <span class="group-title">{{ typeLabel(group.type) }}</span>
              <span class="group-count">{{ group.items.length }}</span>
              <span class="toggle">{{ expandedGroups.has(group.type) ? '▼' : '▶' }}</span>
            </div>
            <div v-show="expandedGroups.has(group.type)" class="group-items">
              <div v-for="(item, i) in group.items" :key="i" class="result-item">
                <span class="item-module">{{ item.module }}</span>
                <span class="item-data">{{ item.data }}</span>
                <span v-if="item.source" class="item-source">← {{ item.source }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { getCapabilities, getModules, createScan, stopScan, scanWsUrl } from '@/api/osint'

// ── 状态 ──
const target = ref('')
const modules = ref([])
const selectedModules = ref([])
const caps = ref(null)
const scanning = ref(false)
const currentJob = ref(null)
const results = ref([])
const logs = ref([])
const showLogs = ref(true)
const expandedGroups = reactive(new Set())

let ws = null

// ── 计算属性 ──
const allSelected = computed(() =>
  modules.value.length > 0 &&
  modules.value.filter(m => m.available).every(m => selectedModules.value.includes(m.name))
)

const resultStats = computed(() => {
  const byType = {}
  for (const r of results.value) {
    if (r.type === 'ROOT') continue
    byType[r.type] = (byType[r.type] || 0) + 1
  }
  return { total: results.value.filter(r => r.type !== 'ROOT').length, byType }
})

const groupedResults = computed(() => {
  const groups = {}
  for (const r of results.value) {
    if (r.type === 'ROOT') continue
    if (!groups[r.type]) groups[r.type] = { type: r.type, items: [] }
    groups[r.type].items.push(r)
  }
  return Object.values(groups).sort((a, b) => b.items.length - a.items.length)
})

// ── 方法 ──
function typeLabel(type) {
  const labels = {
    IP_ADDRESS: 'IPv4 地址',
    IPV6_ADDRESS: 'IPv6 地址',
    INTERNET_NAME: '主机名',
    DOMAIN_NAME: '域名',
    DOMAIN_WHOIS: 'WHOIS 记录',
    DOMAIN_REGISTRAR: '注册商',
    NETBLOCK_OWNER: '网段',
    TCP_PORT_OPEN: '开放端口',
    TCP_PORT_OPEN_BANNER: '服务 Banner',
    IP_INFO: 'IP 信息',
    SUBDOMAIN: '子域名',
    EMAIL: 'Email',
    SHODAN_RESULT: 'Shodan 结果',
    RAW_DNS_RECORDS: 'DNS 记录',
  }
  return labels[type] || type
}

function typeIcon(type) {
  const icons = {
    IP_ADDRESS: '🔢',
    IPV6_ADDRESS: '🔢',
    INTERNET_NAME: '🌐',
    DOMAIN_NAME: '🌍',
    DOMAIN_WHOIS: '📋',
    DOMAIN_REGISTRAR: '🏢',
    NETBLOCK_OWNER: '📶',
    TCP_PORT_OPEN: '🚪',
    TCP_PORT_OPEN_BANNER: '📡',
    IP_INFO: '📍',
    SUBDOMAIN: '🔗',
    EMAIL: '✉️',
    SHODAN_RESULT: '🔎',
    RAW_DNS_RECORDS: '📜',
  }
  return icons[type] || '📌'
}

function statusText(status) {
  return { pending: '等待中', running: '扫描中', done: '完成', stopped: '已停止', failed: '失败' }[status] || status
}

function toggleGroup(type) {
  if (expandedGroups.has(type)) {
    expandedGroups.delete(type)
  } else {
    expandedGroups.add(type)
  }
}

function toggleAllModules() {
  if (allSelected.value) {
    selectedModules.value = []
  } else {
    selectedModules.value = modules.value.filter(m => m.available).map(m => m.name)
  }
}

async function loadCapabilities() {
  try {
    const [capsRes, modsRes] = await Promise.all([getCapabilities(), getModules()])
    caps.value = capsRes.data
    modules.value = modsRes.data.modules
    // 默认全选可用模块
    selectedModules.value = modules.value.filter(m => m.available).map(m => m.name)
  } catch (e) {
    // 静默处理
  }
}

async function startScan() {
  if (!target.value.trim() || scanning.value) return

  // 清空上次结果
  results.value = []
  logs.value = []
  expandedGroups.clear()

  try {
    const res = await createScan(target.value.trim(), selectedModules.value)
    currentJob.value = res.data
    scanning.value = true
    connectWs(res.data.jid)
  } catch (e) {
    // request.js 全局 toast
  }
}

async function stopCurrentScan() {
  if (!currentJob.value) return
  try {
    await stopScan(currentJob.value.jid)
  } catch (e) {
    // ignore
  }
}

function connectWs(jid) {
  if (ws) {
    ws.close()
    ws = null
  }

  const url = scanWsUrl(jid)
  ws = new WebSocket(url)

  ws.onmessage = (evt) => {
    try {
      const msg = JSON.parse(evt.data)
      if (msg.type === 'log') {
        logs.value.push(msg.line)
      } else if (msg.type === 'result') {
        results.value.push(msg.data)
        // 自动展开新类型
        if (!expandedGroups.has(msg.data.type) && msg.data.type !== 'ROOT') {
          expandedGroups.add(msg.data.type)
        }
      } else if (msg.type === 'status') {
        if (currentJob.value) {
          currentJob.value.status = msg.status
        }
        if (['done', 'failed', 'stopped'].includes(msg.status)) {
          scanning.value = false
        }
      }
    } catch (e) {
      // ignore parse error
    }
  }

  ws.onerror = () => {
    scanning.value = false
  }

  ws.onclose = () => {
    scanning.value = false
  }
}

// ── 生命周期 ──
onMounted(() => {
  loadCapabilities()
})

onUnmounted(() => {
  if (ws) {
    ws.close()
    ws = null
  }
})
</script>

<style scoped>
.osint-page {
  min-height: 100vh;
  background: #0a0e1a;
  color: #e0e6f0;
  font-family: 'Segoe UI', system-ui, sans-serif;
}

/* Hero */
.osint-hero {
  position: relative;
  padding: 40px 32px 28px;
  background: linear-gradient(135deg, #0d1529 0%, #0a0e1a 100%);
  border-bottom: 1px solid rgba(0, 255, 200, 0.15);
  overflow: hidden;
}
.hero-content { position: relative; z-index: 1; }
.hero-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  background: linear-gradient(90deg, #00ffc8, #4dabf7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.hero-subtitle { font-size: 13px; color: #7a8499; margin: 0; line-height: 1.6; }
.hero-glow {
  position: absolute; top: -60px; right: -40px;
  width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(0,255,200,0.08), transparent 70%);
  pointer-events: none;
}

/* Body */
.osint-body {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 20px;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}
@media (max-width: 900px) {
  .osint-body { grid-template-columns: 1fr; }
}

/* Panels */
.scan-panel, .results-panel {
  background: rgba(20, 28, 48, 0.6);
  border: 1px solid rgba(60, 80, 120, 0.3);
  border-radius: 12px;
  padding: 20px;
  backdrop-filter: blur(8px);
}
.panel-header {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 20px; padding-bottom: 12px;
  border-bottom: 1px solid rgba(60, 80, 120, 0.2);
}
.panel-icon { font-size: 20px; }
.panel-title { font-size: 16px; font-weight: 600; }

/* Form */
.form-group { margin-bottom: 20px; }
.form-label {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 13px; color: #8a94a8; margin-bottom: 8px;
}
.target-input-row { display: flex; gap: 8px; }
.target-input {
  flex: 1; padding: 10px 14px;
  background: rgba(10, 14, 26, 0.8);
  border: 1px solid rgba(60, 80, 120, 0.4);
  border-radius: 8px; color: #e0e6f0; font-size: 13px;
  outline: none; transition: border 0.2s;
}
.target-input:focus { border-color: #00ffc8; }
.target-input:disabled { opacity: 0.5; }
.target-hint { font-size: 11px; color: #5a6478; margin-top: 6px; }

.btn-scan {
  padding: 10px 20px;
  background: linear-gradient(135deg, #00ffc8, #00d4a8);
  color: #0a0e1a; border: none; border-radius: 8px;
  font-weight: 600; font-size: 13px; cursor: pointer;
  white-space: nowrap; transition: all 0.2s;
}
.btn-scan:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,255,200,0.3); }
.btn-scan:disabled { opacity: 0.4; cursor: not-allowed; }

.btn-toggle-all {
  padding: 2px 10px; font-size: 11px;
  background: rgba(60, 80, 120, 0.3); border: 1px solid rgba(60, 80, 120, 0.4);
  border-radius: 4px; color: #8a94a8; cursor: pointer;
}

/* Module chips */
.module-grid { display: flex; flex-direction: column; gap: 6px; }
.module-chip {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 8px; cursor: pointer;
  background: rgba(10, 14, 26, 0.5);
  border: 1px solid rgba(60, 80, 120, 0.2);
  transition: all 0.15s; font-size: 13px;
}
.module-chip:hover { border-color: rgba(0, 255, 200, 0.3); }
.module-chip.active {
  background: rgba(0, 255, 200, 0.08);
  border-color: rgba(0, 255, 200, 0.4);
}
.module-chip.disabled { opacity: 0.45; cursor: not-allowed; }
.module-chip input { display: none; }
.chip-icon { font-size: 14px; }
.chip-name { flex: 1; }
.chip-badge {
  font-size: 10px; padding: 1px 6px; border-radius: 3px;
  background: rgba(255, 180, 0, 0.15); color: #ffb400;
}

/* Capabilities */
.capabilities-box {
  margin-top: 16px; padding: 12px;
  background: rgba(10, 14, 26, 0.5); border-radius: 8px;
  border: 1px solid rgba(60, 80, 120, 0.2);
}
.cap-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 12px; }
.cap-label { color: #7a8499; }
.cap-value.ok { color: #00ffc8; }
.cap-value.warn { color: #ffb400; }

.btn-stop {
  width: 100%; margin-top: 16px; padding: 10px;
  background: rgba(255, 80, 80, 0.15); color: #ff6464;
  border: 1px solid rgba(255, 80, 80, 0.3); border-radius: 8px;
  font-size: 13px; cursor: pointer; transition: all 0.2s;
}
.btn-stop:hover { background: rgba(255, 80, 80, 0.25); }

/* Job status */
.job-status {
  margin-left: auto; padding: 2px 10px; border-radius: 4px;
  font-size: 11px; font-weight: 600;
}
.job-status.running { background: rgba(0, 255, 200, 0.15); color: #00ffc8; }
.job-status.done { background: rgba(80, 200, 120, 0.15); color: #50c878; }
.job-status.stopped { background: rgba(255, 180, 0, 0.15); color: #ffb400; }
.job-status.failed { background: rgba(255, 80, 80, 0.15); color: #ff5050; }

/* Empty state */
.empty-state {
  text-align: center; padding: 60px 20px; color: #5a6478;
}
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-hint { font-size: 12px; margin-top: 4px; }

/* Stats bar */
.stats-bar {
  display: flex; flex-wrap: wrap; gap: 8px;
  margin-bottom: 16px; padding: 12px;
  background: rgba(10, 14, 26, 0.5); border-radius: 8px;
}
.stat-item { display: flex; flex-direction: column; align-items: center; min-width: 80px; }
.stat-num { font-size: 20px; font-weight: 700; color: #00ffc8; }
.stat-label { font-size: 10px; color: #7a8499; margin-top: 2px; }

/* Logs */
.log-section { margin-bottom: 16px; }
.log-header {
  display: flex; justify-content: space-between; cursor: pointer;
  padding: 8px 12px; background: rgba(10, 14, 26, 0.5);
  border-radius: 8px; font-size: 12px; color: #8a94a8;
}
.log-list {
  max-height: 200px; overflow-y: auto; margin-top: 4px;
  padding: 8px; background: rgba(5, 8, 16, 0.6);
  border-radius: 8px; font-family: 'Consolas', monospace;
}
.log-line { font-size: 11px; color: #6a7488; line-height: 1.6; word-break: break-all; }

/* Result groups */
.result-groups { display: flex; flex-direction: column; gap: 12px; }
.result-group {
  border: 1px solid rgba(60, 80, 120, 0.25); border-radius: 8px; overflow: hidden;
}
.group-header {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; cursor: pointer;
  background: rgba(20, 28, 48, 0.4); transition: background 0.15s;
}
.group-header:hover { background: rgba(30, 38, 58, 0.5); }
.group-icon { font-size: 14px; }
.group-title { font-size: 13px; font-weight: 600; }
.group-count {
  padding: 1px 8px; border-radius: 10px;
  background: rgba(0, 255, 200, 0.1); color: #00ffc8;
  font-size: 11px;
}
.toggle { margin-left: auto; font-size: 10px; color: #5a6478; }

.group-items { max-height: 400px; overflow-y: auto; }
.result-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px; border-bottom: 1px solid rgba(30, 40, 60, 0.3);
  font-size: 12px;
}
.result-item:last-child { border-bottom: none; }
.item-module {
  font-size: 10px; padding: 1px 6px; border-radius: 3px;
  background: rgba(60, 80, 120, 0.2); color: #8a94a8;
  white-space: nowrap;
}
.item-data { color: #c0c8d8; word-break: break-all; }
.item-source { font-size: 10px; color: #5a6478; white-space: nowrap; }

/* Loading dots */
.loading-dots::after {
  content: ''; animation: dots 1.5s steps(4) infinite;
}
@keyframes dots {
  0% { content: ''; } 25% { content: '.'; } 50% { content: '..'; } 75% { content: '...'; }
}

/* Scrollbar */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(60, 80, 120, 0.3); border-radius: 3px; }
</style>

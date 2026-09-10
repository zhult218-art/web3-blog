<template>
  <div class="min-h-screen">
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
      <h1 class="text-xl font-bold text-white"><span class="text-gradient-cyber">Access</span> Traffic</h1>
      <button class="web3-btn text-xs !px-4 !py-2" @click="loadAll">⟳ 刷新</button>
    </div>

    <!-- Summary Cards -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div v-for="c in summaryCards" :key="c.label" class="glass-panel p-4">
        <div class="text-2xl font-black matrix-text text-white">{{ c.value }}</div>
        <div class="text-[11px] text-gray-500 mt-1 tracking-wider uppercase">{{ c.label }}</div>
      </div>
    </div>

    <!-- Visit List -->
    <div class="glass-panel overflow-hidden">
      <div class="p-4 border-b border-white/[0.06] flex items-center justify-between">
        <h3 class="text-sm font-bold text-white">访问记录（含 IP / 经纬度 / 位置）</h3>
        <span class="text-[10px] text-gray-600 matrix-text">{{ list.length }} 条</span>
      </div>
      <div class="overflow-x-auto">
        <table class="web3-table min-w-[900px]">
          <thead>
            <tr><th>IP</th><th>位置</th><th>ISP</th><th>经纬度</th><th>访问页面</th><th>设备</th><th>时间</th></tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="text-center py-10"><Loading text="加载中..." /></td>
            </tr>
            <tr v-else-if="!list.length">
              <td colspan="7" class="text-center text-gray-600 py-10">暂无访问记录（打开网站主页面即自动统计）</td>
            </tr>
            <tr v-for="v in list" :key="v.id">
              <td class="text-white/80 font-mono text-xs">{{ v.ip || '--' }}</td>
              <td>
                <span v-if="v.city || v.region" class="web3-badge-purple whitespace-nowrap">{{ [v.country, v.region, v.city].filter(Boolean).join(' · ') }}</span>
                <span v-else class="text-gray-600 text-xs">未知位置</span>
              </td>
              <td class="text-white/50 text-xs">{{ v.isp || '--' }}</td>
              <td class="text-white/50 font-mono text-xs whitespace-nowrap">
                <template v-if="v.latitude != null && v.longitude != null">{{ Number(v.latitude).toFixed(4) }}, {{ Number(v.longitude).toFixed(4) }}</template>
                <template v-else>--</template>
              </td>
              <td class="text-white/50 text-xs max-w-[160px] truncate" :title="v.pagePath">{{ v.pagePath || '/' }}</td>
              <td class="text-gray-500 text-[11px] max-w-[180px] truncate" :title="v.userAgent">{{ parseUA(v.userAgent) }}</td>
              <td class="text-gray-500 text-xs matrix-text whitespace-nowrap">{{ formatTime(v.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="mt-4" v-if="total > 0">
      <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="fetchList" />
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 流量统计：访问记录列表（分页）+ 访问汇总卡片，
// 含 User-Agent 解析与时间格式化
// ====================================================
import { ref, computed, onMounted } from 'vue'
import { getVisitList, getVisitSummary } from '@/api/admin'
import Pagination from '@/components/common/Pagination.vue'
import Loading from '@/components/common/Loading.vue'

const list = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)
const summary = ref({ total: 0, today: 0, uniqueIps7d: 0, geoLocated7d: 0 })

// 访问汇总指标卡片（总量/今日/7日独立IP/地理定位）
const summaryCards = computed(() => [
  { label: '累计访问', value: summary.value.total },
  { label: '今日访问', value: summary.value.today },
  { label: '7日独立IP', value: summary.value.uniqueIps7d },
  { label: '7日定位数', value: summary.value.geoLocated7d }
])

// 格式化访问时间
function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { hour12: false })
}

// 解析 User-Agent 提取浏览器与操作系统信息
function parseUA(ua) {
  if (!ua) return '--'
  if (/Mobile|Android|iPhone/i.test(ua)) return '📱 移动端'
  if (/Windows/i.test(ua)) return '🖥️ Windows'
  if (/Macintosh/i.test(ua)) return '🍎 macOS'
  if (/Linux/i.test(ua)) return '🐧 Linux'
  return '🌐 其他'
}

// 按分页参数加载访问记录列表
async function fetchList() {
  loading.value = true
  try {
    const res = await getVisitList({ page: page.value, size: size.value })
    const d = res.data || {}
    list.value = d.records || []
    total.value = d.total || 0
  } catch { list.value = [] }
  finally { loading.value = false }
}

// 加载访问汇总统计
async function loadSummary() {
  try {
    const res = await getVisitSummary()
    summary.value = res.data || summary.value
  } catch {}
}

// 同时刷新访问列表与汇总统计
function loadAll() {
  fetchList()
  loadSummary()
}

// 挂载时加载全部流量数据
onMounted(loadAll)
</script>
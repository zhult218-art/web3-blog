<template>
  <div class="min-h-screen py-8 px-6">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-cyber">A股</span> 量化看板</h1>
        <button class="web3-btn text-xs !px-4 !py-2" @click="openModal()">+ 新建策略</button>
      </div>

      <!-- Market Stats -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="glass-panel p-4" v-for="s in marketStats" :key="s.label">
          <div class="text-lg font-bold" :class="s.color">{{ s.value }}</div>
          <div class="text-[11px] text-gray-500 uppercase tracking-wider">{{ s.label }}</div>
        </div>
      </div>

      <!-- Stock Grid -->
      <div class="glass-panel overflow-hidden mb-6">
        <div class="p-4 border-b border-white/[0.06]">
          <h3 class="text-sm font-bold text-white">市场行情</h3>
        </div>
        <div class="overflow-x-auto">
          <table class="web3-table">
            <thead>
              <tr>
                <th>代码</th>
                <th>名称</th>
                <th>最新价</th>
                <th>涨跌幅</th>
                <th>涨跌额</th>
                <th>成交量(手)</th>
                <th>振幅</th>
                <th>换手率</th>
                <th>数据源</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="stock in stockList" :key="stock.symbol">
                <td class="matrix-text text-xs">{{ stock.symbol }}</td>
                <td class="text-white/80">{{ stock.name }}</td>
                <td class="matrix-text text-sm font-bold">{{ stock.price }}</td>
                <td :class="getChangeClass(stock.changePercent)">{{ stock.changePercent }}%</td>
                <td :class="getChangeClass(stock.change)">{{ stock.change > 0 ? '+' : '' }}{{ stock.change }}</td>
                <td class="text-white/60 matrix-text text-xs">{{ formatVolume(stock.volume) }}</td>
                <td class="text-white/60">{{ stock.amplitude }}%</td>
                <td class="text-white/60">{{ stock.turnoverRate }}%</td>
                <td><span class="text-[10px] px-1.5 py-0.5 rounded bg-[#10102a] border border-white/[0.06] text-cyan-400/80">{{ stock.source || '--' }}</span></td>
              </tr>
              <tr v-if="!stockList.length && !loaded">
                <td colspan="8" class="text-center py-8">
                  <span class="inline-flex items-center gap-2 text-xs text-gray-500">
                    <span class="h-4 w-4 rounded-full border-2 border-white/15 border-t-web3-accent animate-spin"></span>
                    加载中...
                  </span>
                </td>
              </tr>
              <tr v-else-if="!stockList.length">
                <td colspan="8" class="text-center text-gray-600 py-8">暂无行情数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Strategies -->
      <div class="glass-panel overflow-hidden">
        <div class="p-4 border-b border-white/[0.06]">
          <h3 class="text-sm font-bold text-white">量化策略</h3>
        </div>
        <div class="overflow-x-auto">
          <table class="web3-table min-w-[860px]">
            <thead>
              <tr>
                <th>ID</th>
                <th>策略</th>
                <th>状态</th>
                <th>收益率</th>
                <th>风险</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in strategies" :key="s.id">
                <td class="matrix-text text-xs">#{{ s.id }}</td>
                <td>
                  <div class="text-white/80">{{ s.name }}</div>
                  <div class="text-gray-600 text-[11px] max-w-[180px] truncate" v-if="s.description" :title="s.description">{{ s.description }}</div>
                </td>
                <td><span :class="s.status === 'ACTIVE' ? 'web3-badge-green' : 'web3-badge-orange'">{{ s.status || '-' }}</span></td>
                <td :class="(s.returns || 0) >= 0 ? 'text-green-400' : 'text-red-400'">{{ s.returns ?? '-' }}%</td>
                <td class="text-white/60">{{ s.riskLevel || '-' }}</td>
                <td class="text-gray-500 text-xs matrix-text">{{ formatDate(s.createdAt) }}</td>
                <td>
                  <div class="flex items-center gap-2">
                    <button class="text-cyan-400 hover:text-cyan-300 text-[11px]" title="运行" @click="handleRun(s)">▶ 运行</button>
                    <button class="text-purple-400 hover:text-purple-300 text-[11px]" title="编辑" @click="openModal(s)">编辑</button>
                    <button class="text-red-400 hover:text-red-300 text-[11px]" title="删除" @click="removeStrategy(s)">删除</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!strategies.length && loaded">
                <td colspan="7" class="text-center text-gray-600 py-8">暂无策略，点击“+ 新建策略”创建</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Strategy Modal -->
    <div v-if="modalOpen" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="modalOpen = false"></div>
      <div class="relative w-full max-w-lg glass-panel p-6">
        <h3 class="text-sm font-bold text-white mb-4">{{ form.id ? '编辑策略' : '新建策略' }}</h3>
        <div class="space-y-4">
          <div>
            <label class="text-[11px] text-gray-500 uppercase tracking-wider block mb-1.5">策略名称 *</label>
            <input v-model="form.name" class="web3-input" placeholder="如：双均线趋势跟随" />
          </div>
          <div>
            <label class="text-[11px] text-gray-500 uppercase tracking-wider block mb-1.5">风险等级</label>
            <input v-model="form.riskLevel" class="web3-input" placeholder="LOW / MEDIUM / HIGH" />
          </div>
          <div>
            <label class="text-[11px] text-gray-500 uppercase tracking-wider block mb-1.5">策略描述</label>
            <textarea v-model="form.description" rows="2" class="web3-input" placeholder="策略逻辑简介"></textarea>
          </div>
          <div>
            <label class="text-[11px] text-gray-500 uppercase tracking-wider block mb-1.5">策略代码</label>
            <textarea v-model="form.code" rows="4" class="web3-input font-mono text-xs" placeholder="import numpy as np..."></textarea>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button class="web3-btn-outline text-xs !py-2" @click="modalOpen = false">取消</button>
          <button class="web3-btn text-xs !py-2" :disabled="saving" @click="saveStrategy">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 量化管理：市场行情总览 + 策略列表 CRUD，
// 行情来自 pyquant 网关，失败时使用 mock 数据
// ====================================================
import { ref, onMounted } from 'vue'
import { useToastStore } from '@/stores/modules/toast'
import { getPyQuotes, getQuantList, createStrategy, updateStrategy, deleteStrategy, runStrategy } from '@/api/quant'

const toast = useToastStore()

const stockList = ref([])
const strategies = ref([])
const loaded = ref(false)
const modalOpen = ref(false)
const saving = ref(false)
const form = ref({})
// 市场统计卡片（股票数/平均涨跌幅等）
const marketStats = ref([
  { label: 'Total Stocks', value: '-', color: 'text-white' },
  { label: 'Avg Change', value: '-', color: 'text-white' },
  { label: 'Active Strategies', value: '-', color: 'text-green-400' },
  { label: 'Win Rate', value: '-', color: 'text-cyan-400' },
])

// 格式化日期时间为「年-月-日 时:分」，空值显示「-」
function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN').slice(0, 16) : '-' }
// 按数值正负返回涨红跌绿的样式类
function getChangeClass(val) {
  if (val > 0) return 'text-red-400'
  if (val < 0) return 'text-green-400'
  return 'text-gray-400'
}

// 成交量格式化为万/亿可读形式
function formatVolume(v) {
  if (!v) return '-'
  if (v > 100000) return (v / 10000).toFixed(1) + '万'
  return v.toString()
}

// 读取行情涨跌幅字段
function getChangePercent(q) {
  const v = q.change_percent
  return typeof v === 'number' ? v : parseFloat(q.changePercent || 0)
}

// 读取行情最新价
function getPrice(q) {
  return typeof q.price === 'number' ? q.price : parseFloat(q.price || 0)
}

// 读取行情成交量
function getVolume(q) {
  const v = q.volume
  return typeof v === 'number' ? v : parseFloat(q.volume || 0)
}

// 打开策略编辑弹窗并回填数据
function openModal(strategy) {
  form.value = strategy ? { id: strategy.id, name: strategy.name, riskLevel: strategy.riskLevel, description: strategy.description, code: strategy.code } : {}
  modalOpen.value = true
}

// 保存策略（新建或更新）
async function saveStrategy() {
  if (!form.value.name?.trim()) {
    toast.warning('请填写策略名称')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await updateStrategy(form.value.id, form.value)
      toast.success('策略已更新')
    } else {
      await createStrategy({ ...form.value, status: 'DRAFT' })
      toast.success('策略已创建')
    }
    modalOpen.value = false
    await loadStrategies()
  } catch (e) {
    toast.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 删除策略（二次确认后执行）
async function removeStrategy(s) {
  try {
    await deleteStrategy(s.id)
    toast.success('策略「' + s.name + '」已删除')
    await loadStrategies()
  } catch (e) {
    toast.error(e?.message || '删除失败')
  }
}

// 触发运行指定策略
async function handleRun(s) {
  try {
    await runStrategy(s.id)
    toast.success('策略「' + s.name + '」已开始运行')
    await loadStrategies()
  } catch (e) {
    toast.error(e?.message || '运行失败')
  }
}

// 加载策略列表并计算运行状态统计
async function loadStrategies() {
  try {
    const res = await getQuantList({ page: 1, size: 100 })
    if (res?.data?.records) {
      strategies.value = res.data.records
      const active = strategies.value.filter(s => s.status === 'ACTIVE').length
      marketStats.value[2].value = active + '/' + strategies.value.length
      const avgRet = strategies.value.length
        ? strategies.value.reduce((a, s) => a + (s.returns || 0), 0) / strategies.value.length
        : 0
      marketStats.value[3].value = avgRet.toFixed(1) + '%'
    }
  } catch { /* fallback */ }
}

// 加载行情总览与市场统计（接口失败回退 mock 数据）
async function loadDashboard() {
  try {
    const res = await getPyQuotes()
    const quotes = res?.data
    if (Array.isArray(quotes) && quotes.length) {
      stockList.value = quotes.map(q => ({
        symbol: q.symbol,
        name: q.name,
        price: getPrice(q),
        change: q.change,
        changePercent: getChangePercent(q),
        volume: getVolume(q),
        amplitude: q.amplitude,
        turnoverRate: q.turnover_rate,
      }))
      const total = quotes.length
      const avg = quotes.reduce((a, q) => a + getChangePercent(q), 0) / total
      marketStats.value[0].value = total + ' 只'
      marketStats.value[1].value = (avg > 0 ? '+' : '') + avg.toFixed(2) + '%'
      marketStats.value[1].color = avg >= 0 ? 'text-red-400' : 'text-green-400'
    }
  } catch { /* fallback */ }

  await loadStrategies()

  if (!stockList.value.length) {
    stockList.value = [
      { symbol: 'sh600519', name: '贵州茅台', price: '1688.00', change: 12.50, changePercent: 0.74, volume: 15800, amplitude: 1.2, turnoverRate: 0.35 },
      { symbol: 'sz000858', name: '五粮液', price: '142.30', change: -1.80, changePercent: -1.25, volume: 42500, amplitude: 2.1, turnoverRate: 1.12 },
      { symbol: 'sh601318', name: '中国平安', price: '48.65', change: 0.85, changePercent: 1.78, volume: 89200, amplitude: 2.5, turnoverRate: 0.48 },
    ]
  }
}

// 挂载时加载仪表盘数据
onMounted(() => {
  loadDashboard().finally(() => { loaded.value = true })
})
</script>
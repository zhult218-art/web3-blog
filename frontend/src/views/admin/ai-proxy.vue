<template>
  <div class="min-h-screen py-8 px-6">
    <div class="max-w-7xl mx-auto">
      <!-- 标题 + Tab 导航 -->
      <div class="flex items-center justify-between mb-5 flex-wrap gap-3">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-cyber">API 中转站</span> · OpenAI 兼容网关</h1>
        <div class="flex gap-1 bg-[#0e0e26] border border-white/[0.06] rounded-lg p-1">
          <button v-for="t in tabs" :key="t.key" @click="switchTab(t.key)"
            class="px-3 py-1.5 text-xs rounded-md transition-all"
            :class="tab === t.key ? 'bg-web3-accent/20 text-web3-accent font-bold' : 'text-gray-400 hover:text-white'">
            {{ t.label }}
          </button>
        </div>
      </div>

      <!-- ==================== Tab 1：智能看板 ==================== -->
      <div v-show="tab === 'dashboard'">
        <!-- 总览卡片 -->
        <div class="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4 mb-6">
          <div class="glass-panel p-4" v-for="c in overviewCards" :key="c.label">
            <div class="text-lg font-bold" :class="c.color">{{ c.value }}</div>
            <div class="text-[11px] text-gray-500 uppercase tracking-wider mt-1">{{ c.label }}</div>
            <div v-if="c.sub" class="text-[10px] mt-1" :class="c.growth >= 0 ? 'text-emerald-400/80' : 'text-rose-400/80'">
              较昨日 {{ c.sub }}
            </div>
          </div>
        </div>

        <!-- 图表区 -->
        <div class="grid lg:grid-cols-3 gap-4 mb-6">
          <div class="glass-panel p-4 lg:col-span-2">
            <h3 class="text-sm font-bold text-white mb-2">近 14 天趋势 <span class="text-gray-500 text-[11px] font-normal">请求数 / 消耗额度</span></h3>
            <div ref="trendChart" class="h-64"></div>
          </div>
          <div class="glass-panel p-4">
            <h3 class="text-sm font-bold text-white mb-2">模型消耗分布 <span class="text-gray-500 text-[11px] font-normal">近 30 天</span></h3>
            <div ref="modelPie" class="h-64"></div>
          </div>
        </div>

        <div class="grid lg:grid-cols-3 gap-4 mb-6">
          <div class="glass-panel p-4 lg:col-span-2">
            <h3 class="text-sm font-bold text-white mb-2">用户用量排行 <span class="text-gray-500 text-[11px] font-normal">近 30 天 TOP 10</span></h3>
            <div ref="userBar" class="h-64"></div>
          </div>
          <!-- 智能洞察 -->
          <div class="glass-panel p-4">
            <h3 class="text-sm font-bold text-white mb-3">🤖 智能洞察 <span class="text-gray-500 text-[11px] font-normal">实时规则引擎</span></h3>
            <div class="space-y-2 max-h-64 overflow-y-auto pr-1">
              <div v-for="(ins, i) in insights" :key="i"
                class="rounded-lg px-3 py-2 border"
                :class="{
                  'border-emerald-400/20 bg-emerald-400/[0.05]': ins.level === 'success',
                  'border-cyan-400/20 bg-cyan-400/[0.05]': ins.level === 'info',
                  'border-amber-400/25 bg-amber-400/[0.05]': ins.level === 'warn',
                  'border-rose-400/25 bg-rose-400/[0.05]': ins.level === 'danger'
                }">
                <div class="text-xs font-bold text-white/90">{{ ins.icon }} {{ ins.title }}</div>
                <div v-if="ins.detail" class="text-[11px] text-gray-400 mt-1 leading-relaxed">{{ ins.detail }}</div>
              </div>
              <div v-if="!insights.length && loadedDashboard" class="text-center py-6 text-xs text-gray-600">暂无洞察数据</div>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== Tab 2：渠道管理 ==================== -->
      <div v-show="tab === 'channels'">
        <div class="flex justify-between items-center mb-4">
          <p class="text-xs text-gray-500">上游 AI 平台接入配置；同一模型可配多个渠道自动分流，失败自动切换下一候选。</p>
          <button class="web3-btn text-xs !px-4 !py-2" @click="openChannelModal()">+ 新建渠道</button>
        </div>
        <div class="glass-panel overflow-hidden">
          <div class="overflow-x-auto">
            <table class="web3-table w-full">
              <thead>
                <tr><th>渠道</th><th>Base URL</th><th>Key</th><th>可用模型</th><th>分组</th><th>权重</th><th>余额</th><th>状态</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="ch in channels" :key="ch.id">
                  <td class="text-white/90">{{ ch.name }} <span class="text-gray-600">#{{ ch.id }}</span></td>
                  <td class="matrix-text text-xs text-white/50 max-w-[220px] truncate">{{ ch.baseUrl }}</td>
                  <td class="matrix-text text-xs">{{ ch.maskedKey || '(未配置)' }}</td>
                  <td class="max-w-[200px]"><span v-for="m in parseModels(ch.models)" :key="m" class="inline-block text-[10px] mr-1 my-0.5 px-1.5 py-0.5 rounded bg-[#10102a] border border-white/[0.06] text-cyan-400/80">{{ m }}</span></td>
                  <td class="text-white/60 text-xs">{{ ch.groupName || 'default' }}</td>
                  <td class="matrix-text text-xs">{{ ch.weight ?? 1 }}</td>
                  <td class="matrix-text text-xs">{{ ch.balance != null ? '¥' + ch.balance : '--' }}</td>
                  <td><span :class="ch.status === 1 ? 'text-emerald-400' : 'text-rose-400'" class="text-xs">{{ ch.status === 1 ? '● 启用' : '○ 停用' }}</span></td>
                  <td class="whitespace-nowrap">
                    <button class="text-xs text-cyan-300 hover:text-cyan-100 mr-2" @click="runTest(ch)">测试</button>
                    <button class="text-xs text-gray-400 hover:text-white mr-2" @click="openChannelModal(ch)">编辑</button>
                    <button class="text-xs text-rose-400 hover:text-rose-200" @click="removeChannel(ch)">删除</button>
                    <span v-if="testResults[ch.id]" class="ml-2 text-[10px]" :class="testResults[ch.id].ok ? 'text-emerald-400' : 'text-amber-400'">{{ testResults[ch.id].message }}</span>
                  </td>
                </tr>
                <tr v-if="!channels.length"><td colspan="9" class="text-center text-gray-600 py-8 text-xs">暂无渠道，点击右上角新建</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- ==================== Tab 3：令牌管理 ==================== -->
      <div v-show="tab === 'tokens'">
        <div class="flex justify-between items-center mb-4">
          <p class="text-xs text-gray-500">完整令牌仅创建时展示一次，库中只存哈希与 8 位前缀。</p>
          <button class="web3-btn text-xs !px-4 !py-2" @click="openTokenModal()">+ 发放令牌</button>
        </div>
        <div class="glass-panel overflow-hidden">
          <div class="overflow-x-auto">
            <table class="web3-table w-full">
              <thead>
                <tr><th>ID</th><th>名称</th><th>User</th><th>Key</th><th>总额度</th><th>已用</th><th>剩余</th><th>过期时间</th><th>状态</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="t in tokens" :key="t.id">
                  <td class="matrix-text text-xs">{{ t.id }}</td>
                  <td class="text-white/80 text-xs">{{ t.name }}</td>
                  <td class="matrix-text text-xs">{{ t.userId }}</td>
                  <td class="matrix-text text-xs text-white/50">{{ t.keyMasked }}</td>
                  <td class="matrix-text text-xs">{{ t.quota == null ? '∞ 不限' : fmtQuota(t.quota) }}</td>
                  <td class="matrix-text text-xs">{{ fmtQuota(t.usedQuota) }}</td>
                  <td class="matrix-text text-xs" :class="remainingClass(t)">{{ t.remaining == null ? '∞' : fmtQuota(t.remaining) }}</td>
                  <td class="text-white/50 text-xs">{{ t.expiredAt ? String(t.expiredAt).slice(0, 16).replace('T', ' ') : '永不' }}</td>
                  <td><span :class="t.status === 1 ? 'text-emerald-400' : 'text-rose-400'" class="text-xs">{{ t.status === 1 ? '● 启用' : '○ 停用' }}</span></td>
                  <td class="whitespace-nowrap">
                    <button class="text-xs text-cyan-300 hover:text-cyan-100 mr-2" @click="topupPrompt(t)">充值</button>
                    <button class="text-xs text-amber-300 hover:text-amber-100 mr-2" @click="toggleToken(t)">{{ t.status === 1 ? '停用' : '启用' }}</button>
                    <button class="text-xs text-rose-400 hover:text-rose-200" @click="removeToken(t)">删除</button>
                  </td>
                </tr>
                <tr v-if="!tokens.length"><td colspan="10" class="text-center text-gray-600 py-8 text-xs">暂无令牌</td></tr>
              </tbody>
            </table>
          </div>
          <div class="p-3 flex items-center justify-between border-t border-white/[0.06]">
            <span class="text-[11px] text-gray-500">共 {{ tokenTotal }} 枚</span>
            <div class="flex gap-2">
              <button class="web3-btn text-[11px] !px-3 !py-1" :disabled="tokenPage <= 1" @click="loadTokens(tokenPage - 1)">上一页</button>
              <button class="web3-btn text-[11px] !px-3 !py-1" :disabled="tokenPage * 20 >= tokenTotal" @click="loadTokens(tokenPage + 1)">下一页</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== Tab 4：模型倍率 ==================== -->
      <div v-show="tab === 'models'">
        <div class="flex justify-between items-center mb-4">
          <p class="text-xs text-gray-500">消耗额度 = 模型倍率 × (提示 tokens + 补全 tokens × 补全倍率)，修改后立即生效。</p>
          <button class="web3-btn text-xs !px-4 !py-2" @click="openModelModal()">+ 新增模型</button>
        </div>
        <div class="glass-panel overflow-hidden">
          <table class="web3-table w-full">
            <thead><tr><th>模型</th><th>倍率</th><th>补全倍率</th><th>分组</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="m in models" :key="m.id">
                <td class="matrix-text text-sm">{{ m.modelName }}</td>
                <td><input v-model.number="m.rate" type="number" step="0.01" min="0" class="w-20 bg-[#10102a] border border-white/[0.08] rounded px-2 py-1 text-xs matrix-text focus:border-web3-accent outline-none" /></td>
                <td><input v-model.number="m.completionRate" type="number" step="0.01" min="0" class="w-20 bg-[#10102a] border border-white/[0.08] rounded px-2 py-1 text-xs matrix-text focus:border-web3-accent outline-none" /></td>
                <td class="text-white/60 text-xs">{{ m.groupName || 'default' }}</td>
                <td>
                  <select v-model.number="m.status" class="bg-[#10102a] border border-white/[0.08] rounded px-2 py-1 text-xs outline-none">
                    <option :value="1">启用</option><option :value="0">停用</option>
                  </select>
                </td>
                <td class="whitespace-nowrap">
                  <button class="text-xs text-emerald-300 hover:text-emerald-100 mr-2" @click="saveModel(m, $event)">保存</button>
                  <button class="text-xs text-rose-400 hover:text-rose-200" @click="removeModel(m)">删除</button>
                </td>
              </tr>
              <tr v-if="!models.length"><td colspan="6" class="text-center text-gray-600 py-8 text-xs">暂无模型倍率记录</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ==================== Tab 5：请求日志 ==================== -->
      <div v-show="tab === 'logs'">
        <div class="glass-panel p-4 mb-4 grid grid-cols-2 md:grid-cols-6 gap-3">
          <input v-model="logFilter.userId" placeholder="User ID" class="filter-input" />
          <input v-model="logFilter.tokenId" placeholder="Token ID" class="filter-input" />
          <input v-model="logFilter.model" placeholder="模型名（模糊）" class="filter-input" />
          <select v-model="logFilter.statusCode" class="filter-input">
            <option value="">全部状态</option>
            <option value="200">200 成功</option>
            <option value="401">401 鉴权失败</option>
            <option value="403">403 无权限/额度</option>
            <option value="503">503 上游错误</option>
          </select>
          <input v-model="logFilter.startDate" type="date" class="filter-input" />
          <div class="flex gap-2">
            <input v-model="logFilter.endDate" type="date" class="filter-input" />
            <button class="web3-btn text-xs !px-3 whitespace-nowrap" @click="loadLogs(1)">查询</button>
          </div>
        </div>
        <div class="glass-panel overflow-hidden">
          <div class="overflow-x-auto">
            <table class="web3-table w-full">
              <thead>
                <tr><th>时间</th><th>RequestID</th><th>User/Token</th><th>模型</th><th>渠道</th><th>方式</th><th>Prompt</th><th>Completion</th><th>倍率</th><th>消耗</th><th>耗时</th><th>状态</th></tr>
              </thead>
              <tbody>
                <tr v-for="l in logs" :key="l.id">
                  <td class="matrix-text text-[11px]">{{ shortTime(l.createdAt) }}</td>
                  <td class="matrix-text text-[10px] text-white/30 max-w-[110px] truncate">{{ l.requestId }}</td>
                  <td class="matrix-text text-[11px]">{{ l.userId ?? '-' }} / {{ l.tokenId ?? '-' }}</td>
                  <td class="text-white/70 text-xs">{{ l.model || '-' }}</td>
                  <td class="matrix-text text-xs">{{ l.channelId ?? '兜底' }}</td>
                  <td class="text-xs" :class="l.stream === 1 ? 'text-violet-300' : 'text-gray-500'">{{ l.stream === 1 ? 'SSE' : 'JSON' }}</td>
                  <td class="matrix-text text-[11px]">{{ l.promptTokens }}</td>
                  <td class="matrix-text text-[11px]">{{ l.completionTokens }}</td>
                  <td class="matrix-text text-[11px]">{{ l.rate }}</td>
                  <td class="matrix-text text-[11px] text-amber-300/90">{{ l.quotaUsed }}</td>
                  <td class="matrix-text text-[11px]">{{ l.latencyMs }}ms</td>
                  <td>
                    <span class="text-[10px] px-1.5 py-0.5 rounded border"
                      :class="l.statusCode === 200 ? 'text-emerald-400 border-emerald-400/20 bg-emerald-400/[0.05]'
                        : l.statusCode >= 500 ? 'text-rose-400 border-rose-400/20 bg-rose-400/[0.05]'
                        : 'text-amber-400 border-amber-400/20 bg-amber-400/[0.05]'">{{ l.statusCode }}</span>
                    <span v-if="l.errorMsg" class="text-[10px] text-gray-500 ml-1" :title="l.errorMsg">⚠</span>
                  </td>
                </tr>
                <tr v-if="!logs.length"><td colspan="12" class="text-center text-gray-600 py-8 text-xs">暂无日志记录</td></tr>
              </tbody>
            </table>
          </div>
          <div class="p-3 flex items-center justify-between border-t border-white/[0.06]">
            <span class="text-[11px] text-gray-500">共 {{ logTotal }} 条</span>
            <div class="flex gap-2">
              <button class="web3-btn text-[11px] !px-3 !py-1" :disabled="logPage <= 1" @click="loadLogs(logPage - 1)">上一页</button>
              <button class="web3-btn text-[11px] !px-3 !py-1" :disabled="logPage * 20 >= logTotal" @click="loadLogs(logPage + 1)">下一页</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 渠道弹窗 -->
  <teleport to="body">
    <div v-if="showChannelModal" class="fixed inset-0 z-[100] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4" @click.self="showChannelModal = false">
      <div class="glass-panel w-full max-w-lg p-6 space-y-3">
        <h3 class="text-base font-bold text-white">{{ editingChannel?.id ? '编辑渠道 #' + editingChannel.id : '新建渠道' }}</h3>
        <label class="block text-xs text-gray-400">名称 *<input v-model="channelForm.name" class="modal-input" placeholder="DeepSeek 官方" /></label>
        <label class="block text-xs text-gray-400">Base URL *<input v-model="channelForm.baseUrl" class="modal-input" placeholder="https://api.deepseek.com/v1" /></label>
        <label class="block text-xs text-gray-400">API Key {{ editingChannel?.id ? '（留空=不修改）' : '*' }}<input v-model="channelForm.apiSecret" class="modal-input" placeholder="sk-..." /></label>
        <label class="block text-xs text-gray-400">可用模型（每行一个）<textarea v-model="channelForm.modelsText" rows="3" class="modal-input" placeholder="deepseek-chat&#10;deepseek-reasoner"></textarea></label>
        <div class="grid grid-cols-3 gap-3">
          <label class="block text-xs text-gray-400">分组<input v-model="channelForm.groupName" class="modal-input" placeholder="default" /></label>
          <label class="block text-xs text-gray-400">权重<input v-model.number="channelForm.weight" type="number" min="1" class="modal-input" /></label>
          <label class="block text-xs text-gray-400">状态<select v-model.number="channelForm.status" class="modal-input"><option :value="1">启用</option><option :value="0">停用</option></select></label>
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <button class="web3-btn text-xs !px-4" @click="showChannelModal = false">取消</button>
          <button class="web3-btn text-xs !px-4" @click="saveChannel">保存</button>
        </div>
      </div>
    </div>

    <!-- 发放令牌弹窗 -->
    <div v-if="showTokenModal" class="fixed inset-0 z-[100] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4" @click.self="showTokenModal = false">
      <div class="glass-panel w-full max-w-md p-6 space-y-3">
        <h3 class="text-base font-bold text-white">发放新令牌</h3>
        <label class="block text-xs text-gray-400">归属用户 ID *<input v-model.number="tokenForm.userId" type="number" class="modal-input" placeholder="web3_user 的 userId" /></label>
        <label class="block text-xs text-gray-400">备注名<input v-model="tokenForm.name" class="modal-input" placeholder="我的第一枚令牌" /></label>
        <div class="grid grid-cols-2 gap-3">
          <label class="block text-xs text-gray-400">额度上限（空=不限）<input v-model="tokenForm.quota" type="number" step="0.0001" min="0" class="modal-input" placeholder="10.00" /></label>
          <label class="block text-xs text-gray-400">过期时间（空=永久）<input v-model="tokenForm.expiredAt" type="datetime-local" class="modal-input" /></label>
        </div>
        <label class="block text-xs text-gray-400">模型白名单（每行一个，空=全部）<textarea v-model="tokenForm.whitelistText" rows="2" class="modal-input" placeholder="deepseek-chat"></textarea></label>
        <div class="flex justify-end gap-2 pt-2">
          <button class="web3-btn text-xs !px-4" @click="showTokenModal = false">取消</button>
          <button class="web3-btn text-xs !px-4" @click="saveToken">生成</button>
        </div>
      </div>
    </div>

    <!-- 完整令牌一次性展示弹窗 -->
    <div v-if="revealedToken" class="fixed inset-0 z-[110] bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div class="glass-panel w-full max-w-md p-6 space-y-4 border-web3-accent/30">
        <h3 class="text-base font-bold text-amber-300">⚠ 请立即保存你的完整令牌</h3>
        <p class="text-xs text-gray-400 leading-relaxed">这是唯一一次展示机会，关闭后只能看到前缀。请复制并妥善保管：</p>
        <code class="block matrix-text text-xs break-all bg-black/40 border border-white/10 rounded-lg p-3 text-emerald-300 select-all">{{ revealedToken }}</code>
        <div class="flex justify-end gap-2">
          <button class="web3-btn text-xs !px-4" @click="copyRevealed">📋 复制</button>
          <button class="web3-btn text-xs !px-4" @click="revealedToken = null">我已保存</button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
// ============================================================
// 管理后台 · API 中转站页（ai-proxy-service :8093）
// 智能看板（总览/趋势/分布/排行/洞察）+ 渠道/令牌/模型倍率/日志 四大管理 Tab
// ============================================================
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import {
  getAiDashboard, getAiInsights,
  getChannels, createChannel, updateChannel, deleteChannel, testChannel,
  getTokens, createToken, updateToken, topupToken, deleteToken,
  getModels, createModel, updateModel, deleteModel,
  getAiLogs
} from '@/api/aiproxy'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm, prompt as dlgPrompt } from '@/composables/useDialog'

const toast = useToastStore()

const tabs = [
  { key: 'dashboard', label: '📊 智能看板' },
  { key: 'channels', label: '🛰️ 渠道管理' },
  { key: 'tokens', label: '🔑 令牌管理' },
  { key: 'models', label: '📐 模型倍率' },
  { key: 'logs', label: '📜 请求日志' }
]
const tab = ref('dashboard')

// ---------------- 看板状态 ----------------
const overview = ref({})
const trendRows = ref([])
const modelDist = ref([])
const topUsers = ref([])
const insights = ref([])
const loadedDashboard = ref(false)
const trendChart = ref(null)
const modelPie = ref(null)
const userBar = ref(null)
let chartInstances = []

const overviewCards = computed(() => {
  const o = overview.value
  const growthText = v => (v == null ? '' : `${v > 0 ? '+' : ''}${v}%`)
  return [
    { label: '今日请求数', value: o.todayRequests ?? 0, color: 'text-cyan-300', sub: growthText(o.requestGrowthPct), growth: o.requestGrowthPct },
    { label: '今日消耗额度', value: Number(o.todayQuota ?? 0).toFixed(2), color: 'text-amber-300', sub: growthText(o.quotaGrowthPct), growth: o.quotaGrowthPct },
    { label: '今日 Tokens', value: fmtNum(o.todayTokens ?? 0), color: 'text-violet-300' },
    { label: '活跃令牌（今日）', value: o.activeTokensToday ?? 0, color: 'text-emerald-300' },
    { label: '令牌 总数/启用', value: `${o.tokenTotal ?? 0} / ${o.tokenActive ?? 0}`, color: 'text-sky-300' },
    { label: '渠道 启用/总数', value: `${o.channelEnabled ?? 0} / ${o.channelTotal ?? 0}`, color: 'text-fuchsia-300', sub: o.avgLatency ? `均耗 ${Math.round(o.avgLatency)}ms` : '' }
  ]
})

async function loadDashboard() {
  try {
    const res = await getAiDashboard()
    overview.value = res.data.overview || {}
    trendRows.value = res.data.trend || []
    modelDist.value = res.data.modelDist || []
    topUsers.value = res.data.topUsers || []
    const ins = await getAiInsights()
    insights.value = ins.data || []
    loadedDashboard.value = true
    await nextTick()
    renderCharts()
  } catch (e) {
    console.error('dashboard load failed', e)
  }
}

function renderCharts() {
  chartInstances.forEach(c => c.dispose())
  chartInstances = []
  if (trendChart.value) {
    const c1 = echarts.init(trendChart.value)
    c1.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis' },
      legend: { data: ['请求数', '消耗额度'], textStyle: { color: '#9ca3af', fontSize: 10 }, top: 0 },
      grid: { left: 42, right: 46, top: 28, bottom: 22 },
      xAxis: { type: 'category', data: trendRows.value.map(r => String(r.date).slice(5)), axisLine: { lineStyle: { color: '#374151' } }, axisLabel: { color: '#9ca3af', fontSize: 9 } },
      yAxis: [
        { type: 'value', splitLine: { lineStyle: { color: '#1f293766' } }, axisLabel: { color: '#9ca3af', fontSize: 9 } },
        { type: 'value', splitLine: { show: false }, axisLabel: { color: '#fbbf24aa', fontSize: 9 } }
      ],
      series: [
        { name: '请求数', type: 'line', smooth: true, data: trendRows.value.map(r => r.requests), itemStyle: { color: '#22d3ee' }, areaStyle: { color: 'rgba(34,211,238,0.12)' } },
        { name: '消耗额度', type: 'line', yAxisIndex: 1, smooth: true, data: trendRows.value.map(r => Number(r.quota)), itemStyle: { color: '#f59e0b' } }
      ]
    })
    chartInstances.push(c1)
  }
  if (modelPie.value) {
    const c2 = echarts.init(modelPie.value)
    c2.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, textStyle: { color: '#9ca3af', fontSize: 9 }, itemWidth: 10, itemHeight: 10 },
      series: [{
        type: 'pie', radius: ['38%', '62%'], center: ['50%', '44%'],
        label: { show: false },
        data: modelDist.value.slice(0, 8).map((r, i) => ({
          name: r.model, value: Math.round(Number(r.quota) * 10000) / 10000 || r.requests,
          itemStyle: { color: ['#22d3ee', '#a78bfa', '#f59e0b', '#34d399', '#f472b6', '#60a5fa', '#fb7185', '#facc15'][i % 8] }
        }))
      }]
    })
    chartInstances.push(c2)
  }
  if (userBar.value) {
    const c3 = echarts.init(userBar.value)
    const rows = [...topUsers.value].reverse()
    c3.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis', formatter: ps => ps.map(p => `${p.marker}用户 #${rows[p.dataIndex].userId}<br/>消耗 ${p.value} · ${rows[p.dataIndex].requests} 次`).join('') },
      grid: { left: 64, right: 24, top: 10, bottom: 24 },
      xAxis: { type: 'value', splitLine: { lineStyle: { color: '#1f293766' } }, axisLabel: { color: '#9ca3af', fontSize: 9 } },
      yAxis: { type: 'category', data: rows.map(r => '#' + r.userId), axisLabel: { color: '#9ca3af', fontSize: 10 }, axisLine: { lineStyle: { color: '#374151' } } },
      series: [{ type: 'bar', barWidth: 14, data: rows.map(r => Math.round(Number(r.quota) * 10000) / 10000), itemStyle: { borderRadius: 7, color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#7c3aed' }, { offset: 1, color: '#22d3ee' }]) } }]
    })
    chartInstances.push(c3)
  }
}
const onResize = () => chartInstances.forEach(c => c.resize())

// ---------------- 渠道 ----------------
const channels = ref([])
const showChannelModal = ref(false)
const editingChannel = ref(null)
const channelForm = reactive({ name: '', baseUrl: '', apiSecret: '', modelsText: '', groupName: 'default', weight: 1, status: 1 })
const testResults = reactive({})

async function loadChannels() {
  const res = await getChannels()
  channels.value = res.data || []
}
function openChannelModal(ch) {
  editingChannel.value = ch || null
  channelForm.name = ch?.name || ''
  channelForm.baseUrl = ch?.baseUrl || ''
  channelForm.apiSecret = ''
  channelForm.modelsText = ch ? parseModels(ch.models).join('\n') : ''
  channelForm.groupName = ch?.groupName || 'default'
  channelForm.weight = ch?.weight ?? 1
  channelForm.status = ch?.status ?? 1
  showChannelModal.value = true
}
async function saveChannel() {
  if (!channelForm.name || !channelForm.baseUrl) { toast.error('名称与 Base URL 必填'); return }
  const payload = {
    name: channelForm.name,
    baseUrl: channelForm.baseUrl.replace(/\/+$/, ''),
    models: channelForm.modelsText.split('\n').map(s => s.trim()).filter(Boolean),
    groupName: channelForm.groupName || 'default',
    weight: channelForm.weight || 1,
    status: channelForm.status
  }
  if (channelForm.apiSecret.trim()) payload.apiSecret = channelForm.apiSecret.trim()
  if (editingChannel.value?.id) {
    await updateChannel(editingChannel.value.id, payload)
    toast.success('渠道已更新')
  } else {
    if (!payload.apiSecret) { toast.error('请填写上游 API Key'); return }
    await createChannel(payload)
    toast.success('渠道已创建')
  }
  showChannelModal.value = false
  loadChannels()
}
async function removeChannel(ch) {
  if (!(await dlgConfirm(`确认删除渠道「${ch.name}」？`))) return
  await deleteChannel(ch.id)
  toast.success('已删除')
  loadChannels()
}
async function runTest(ch) {
  testResults[ch.id] = { ok: false, message: '测试中…' }
  try {
    const res = await testChannel(ch.id)
    testResults[ch.id] = res.data
  } catch (e) {
    testResults[ch.id] = { ok: false, message: '测试请求失败' }
  }
}
function parseModels(json) {
  if (!json) return []
  try { return JSON.parse(json) } catch { return [] }
}

// ---------------- 令牌 ----------------
const tokens = ref([])
const tokenTotal = ref(0)
const tokenPage = ref(1)
const showTokenModal = ref(false)
const revealedToken = ref('')
const tokenForm = reactive({ userId: null, name: '', quota: '', expiredAt: '', whitelistText: '' })

async function loadTokens(page = 1) {
  const res = await getTokens({ page, size: 20 })
  tokens.value = res.data.records || []
  tokenTotal.value = Number(res.data.total || 0)
  tokenPage.value = page
}
function openTokenModal() {
  Object.assign(tokenForm, { userId: null, name: '', quota: '', expiredAt: '', whitelistText: '' })
  showTokenModal.value = true
}
async function saveToken() {
  if (!tokenForm.userId) { toast.error('请填写归属用户 ID'); return }
  const payload = {
    userId: tokenForm.userId,
    name: tokenForm.name || '默认令牌',
    modelWhitelist: tokenForm.whitelistText.split('\n').map(s => s.trim()).filter(Boolean)
  }
  if (tokenForm.quota !== '' && tokenForm.quota != null) payload.quota = tokenForm.quota
  if (tokenForm.expiredAt) payload.expiredAt = tokenForm.expiredAt
  const res = await createToken(payload)
  showTokenModal.value = false
  revealedToken.value = res.data.fullToken
  loadTokens(1)
}
function copyRevealed() {
  navigator.clipboard.writeText(revealedToken.value).then(() => toast.success('已复制到剪贴板'))
}
async function toggleToken(t) {
  await updateToken(t.id, { status: t.status === 1 ? 0 : 1 })
  toast.success(t.status === 1 ? '令牌已停用' : '令牌已启用')
  loadTokens(tokenPage.value)
}
async function removeToken(t) {
  if (!(await dlgConfirm(`确认删除令牌 #${t.id}（${t.name}）？此操作不可恢复。`))) return
  await deleteToken(t.id)
  toast.success('已删除')
  loadTokens(tokenPage.value)
}
async function topupPrompt(t) {
  const input = await dlgPrompt(`为令牌 #${t.id}（${t.name}）充值额度，当前剩余 ${t.remaining ?? '∞'}：`, { defaultValue: '10.00' })
  if (!input) return
  await topupToken(t.id, input)
  toast.success('充值成功')
  loadTokens(tokenPage.value)
}

// ---------------- 模型倍率 ----------------
const models = ref([])
async function loadModels() {
  const res = await getModels()
  models.value = res.data || []
}
async function saveModel(m) {
  await updateModel(m.id, { rate: m.rate, completionRate: m.completionRate, status: m.status })
  toast.success(`${m.modelName} 倍率已保存`)
}
async function removeModel(m) {
  if (!(await dlgConfirm(`确认删除模型倍率「${m.modelName}」？`))) return
  await deleteModel(m.id)
  toast.success('已删除')
  loadModels()
}
async function openModelModal() {
  const name = await dlgPrompt('新增模型名称：')
  if (!name) return
  const rate = (await dlgPrompt('主倍率（默认 1）：', { defaultValue: '1' })) || '1'
  await createModel({ modelName: name.trim(), rate })
  toast.success('已新增')
  loadModels()
}

// ---------------- 日志 ----------------
const logs = ref([])
const logTotal = ref(0)
const logPage = ref(1)
const logFilter = reactive({ userId: '', tokenId: '', model: '', statusCode: '', startDate: '', endDate: '' })

async function loadLogs(page = 1) {
  const params = { page, size: 20 }
  if (logFilter.userId) params.userId = logFilter.userId
  if (logFilter.tokenId) params.tokenId = logFilter.tokenId
  if (logFilter.model) params.model = logFilter.model
  if (logFilter.statusCode !== '') params.statusCode = logFilter.statusCode
  if (logFilter.startDate) params.startDate = logFilter.startDate
  if (logFilter.endDate) params.endDate = logFilter.endDate
  const res = await getAiLogs(params)
  logs.value = res.data.records || []
  logTotal.value = Number(res.data.total || 0)
  logPage.value = page
}

// ---------------- 工具 ----------------
function fmtQuota(v) {
  if (v == null) return '-'
  const n = Number(v)
  return n >= 1000 ? fmtNum(n) : n.toFixed(n % 1 === 0 ? 0 : 2)
}
function fmtNum(n) {
  if (n >= 1e6) return (n / 1e6).toFixed(1) + 'M'
  if (n >= 1e3) return (n / 1e3).toFixed(1) + 'k'
  return String(Math.round(Number(n) || 0))
}
function remainingClass(t) {
  if (t.remaining == null) return 'text-gray-400'
  const ratio = Number(t.remaining) / (Number(t.quota) || 1)
  return ratio <= 0.05 ? 'text-rose-400' : ratio <= 0.2 ? 'text-amber-400' : 'text-emerald-300'
}
function shortTime(ts) {
  if (!ts) return '-'
  return String(ts).replace('T', ' ').slice(5, 16)
}

function switchTab(key) {
  tab.value = key
  if (key === 'dashboard') loadDashboard()
  if (key === 'channels') loadChannels()
  if (key === 'tokens') loadTokens(1)
  if (key === 'models') loadModels()
  if (key === 'logs') loadLogs(1)
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chartInstances.forEach(c => c.dispose())
})
</script>

<style scoped>
.filter-input {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 12px;
  color: #e5e7eb;
  outline: none;
  width: 100%;
}
.filter-input:focus { border-color: rgba(34, 211, 238, 0.5); }
.modal-input {
  display: block;
  margin-top: 4px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 7px 10px;
  font-size: 12px;
  color: #e5e7eb;
  outline: none;
  width: 100%;
}
.modal-input:focus { border-color: rgba(34, 211, 238, 0.5); }
</style>

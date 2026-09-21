<template>
  <!-- 策略选股弹窗：内置策略 / 自定义代码（受限沙箱） -->
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-[90] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" @click.self="close">
      <div class="w-full max-w-3xl max-h-[88vh] flex flex-col rounded-2xl border border-white/10 bg-[#0a0a18]/95 backdrop-blur-2xl shadow-2xl overflow-hidden">
        <div class="flex items-center justify-between px-5 py-4 border-b border-white/10 shrink-0">
          <h3 class="text-sm font-bold text-amber-100 flex items-center gap-2"><span>🎯</span> 策略选股</h3>
          <button @click="close" class="text-gray-500 hover:text-white transition text-sm px-1" title="关闭">✕</button>
        </div>

        <div class="px-5 py-4 border-b border-white/10 shrink-0">
          <div class="flex items-center gap-2 mb-3">
            <button @click="mode = 'builtin'; error = ''"
              :class="['text-[11px] px-3 py-1.5 rounded-lg transition',
                mode === 'builtin' ? 'bg-purple-500/20 text-purple-300 border border-purple-400/30' : 'border border-white/10 text-gray-400 hover:text-white']">
              内置策略
            </button>
            <button @click="mode = 'custom'; error = ''"
              :class="['text-[11px] px-3 py-1.5 rounded-lg transition',
                mode === 'custom' ? 'bg-purple-500/20 text-purple-300 border border-purple-400/30' : 'border border-white/10 text-gray-400 hover:text-white']">
              自定义代码
            </button>
            <span class="ml-auto text-[10px] text-amber-600/40">数据池: 全市场快照（akshare/同花顺）· 仅供研究</span>
          </div>

          <template v-if="mode === 'builtin'">
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 items-end">
              <div class="sm:col-span-2">
                <label class="text-[10px] text-amber-600/60 mb-1 block">策略条件</label>
                <select v-model="builtinId" class="web3-input text-xs">
                  <option v-for="s in builtinStrategyOptions" :key="s.id" :value="s.id">{{ s.name }} — {{ s.desc }}</option>
                </select>
              </div>
              <div class="flex gap-3 items-end">
                <div class="flex-1">
                  <label class="text-[10px] text-amber-600/60 mb-1 block">返回数量</label>
                  <input v-model.number="limit" type="number" min="1" max="50" class="web3-input text-xs font-mono" />
                </div>
                <button class="web3-btn text-xs !px-4 !py-2.5 shrink-0" :disabled="running" @click="runScreen">
                  {{ running ? '筛选中...' : '开始筛选' }}
                </button>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="flex items-center gap-3 mb-2">
              <span class="text-[10px] text-amber-600/60">自定义条件代码（受限沙箱，6s 超时）</span>
              <div class="flex gap-3 items-end ml-auto">
                <label class="text-[10px] text-amber-600/60 block">返回数量
                  <input v-model.number="limit" type="number" min="1" max="50" class="web3-input text-xs font-mono !mt-1 w-20" />
                </label>
                <button class="web3-btn text-xs !px-4 !py-2" :disabled="running" @click="runScreen">
                  {{ running ? '运行中...' : '运行筛选' }}
                </button>
              </div>
            </div>
            <textarea v-model="customCode" spellcheck="false"
              class="web3-input text-xs font-mono !min-h-[110px] resize-none leading-relaxed"
              placeholder="def filter_stock(d):&#10;    return d['ma5'] > d['ma20'] and d['pct'] < 5&#10;&#10;def score_stock(d):&#10;    return d['pct'] * 2"></textarea>
            <p class="text-[10px] text-amber-700/40 mt-1">可选字段: code/name/price/pct/turnover_rate/pe/pb/cap/closes/rsi/ma5/ma20/ma60/dif/dea/boll_low/boll_up</p>
          </template>
        </div>

        <div class="flex-1 overflow-y-auto px-5 py-4">
          <p v-if="error" class="text-xs text-red-400 bg-red-500/5 border border-red-500/20 rounded-lg px-3 py-2.5 mb-3">{{ error }}</p>

          <div v-if="running" class="py-10 flex flex-col items-center gap-3">
            <div class="w-6 h-6 rounded-full border-2 border-amber-400/30 border-t-amber-400 animate-spin"></div>
            <span class="text-[10px] text-amber-600/40">正在全市场扫描（冷启快照可能需要数十秒）...</span>
          </div>

          <template v-else-if="result">
            <div class="flex items-center gap-3 mb-3">
              <h4 class="text-xs text-amber-100 font-bold">{{ result.strategy_name || '自定义筛选' }}</h4>
              <span class="text-[10px] text-amber-600/40">候选池 {{ result.pool_size || 0 }} 只 · 命中 {{ (result.items || []).length }} 只 · {{ result.generated_at }}</span>
              <span v-if="result.source" class="text-[10px] px-1.5 py-0.5 rounded-full bg-amber-500/10 text-amber-500/70 border border-amber-500/20 ml-auto">{{ result.source }}</span>
            </div>

            <div v-if="(result.items || []).length" class="rounded-xl bg-[#1a1208] border border-amber-800/15 overflow-hidden">
              <div class="overflow-x-auto">
                <table class="w-full text-xs">
                  <thead>
                    <tr class="text-left text-[10px] text-amber-600/60 border-b border-amber-800/15">
                      <th class="px-3 py-2.5 font-medium">代码</th>
                      <th class="px-3 py-2.5 font-medium">名称</th>
                      <th class="px-3 py-2.5 font-medium text-right">现价</th>
                      <th class="px-3 py-2.5 font-medium text-right">涨跌%</th>
                      <th class="px-3 py-2.5 font-medium text-right">评分</th>
                      <th class="px-3 py-2.5 font-medium">信号日期</th>
                      <th class="px-3 py-2.5 font-medium">入选理由</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(it, i) in result.items" :key="it.code + i"
                      class="border-b border-amber-900/10 last:border-b-0 hover:bg-amber-500/5 transition">
                      <td class="px-3 py-2 font-mono">{{ it.code }}</td>
                      <td class="px-3 py-2 text-amber-100/90">{{ it.name }}</td>
                      <td class="px-3 py-2 text-right font-mono">{{ it.price != null ? Number(it.price).toFixed(2) : '--' }}</td>
                      <td class="px-3 py-2 text-right font-mono" :class="Number(it.pct) >= 0 ? 'text-red-400' : 'text-green-400'">
                        {{ it.pct != null ? (Number(it.pct) > 0 ? '+' : '') + Number(it.pct).toFixed(2) + '%' : '--' }}
                      </td>
                      <td class="px-3 py-2 text-right font-mono">
                        <span class="text-[10px] px-1.5 py-0.5 rounded-full bg-purple-500/15 text-purple-300 border border-purple-400/25">{{ it.score }}</span>
                      </td>
                      <td class="px-3 py-2 font-mono text-amber-200/60">{{ it.signal_date || '--' }}</td>
                      <td class="px-3 py-2 text-amber-200/70 max-w-[240px]">
                        <div class="flex flex-wrap gap-1">
                          <span v-for="(r, ri) in (Array.isArray(it.reasons) ? it.reasons : [it.reasons])" :key="ri"
                            class="text-[9px] px-1.5 py-0.5 rounded bg-[#332314] text-amber-500/80 border border-amber-500/10 whitespace-nowrap">{{ r }}</span>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div v-else class="text-xs text-amber-700/40 py-6 text-center rounded-xl bg-[#1a1208] border border-amber-800/15">无符合条件标的</div>
          </template>

          <div v-else class="text-xs text-amber-700/40 py-10 text-center">选择策略条件后点击「开始筛选」，从全市场快照中选出当前符合策略买点的标的</div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
// StrategyScreenModal：从 quant/index.vue 抽出的策略选股弹窗
// 内置策略 / 自定义代码（受限沙箱），调用 quant-py-service 的筛选接口
// 通过 v-model 双向同步：open（弹窗显示）、lastResult（父组件展示"上次筛选"提示）
import { ref, watch, onMounted } from 'vue'
import { getPyStrategies, getPyScreenStrategy, getPyScreenCustom } from '@/api/quant'

const props = defineProps({
  // 控制弹窗显示（v-model）
  open: { type: Boolean, default: false },
  // 上次筛选结果（v-model:lastResult），父组件用于显示提示文本
  lastResult: { type: Object, default: null }
})

const emit = defineEmits(['update:open', 'update:lastResult'])

// 弹窗内部状态
const mode = ref('builtin')
const builtinId = ref('moving_avg')
const limit = ref(20)
const force = ref(false)
const customCode = ref('def filter_stock(d):\n    return d["ma5"] > d["ma20"] and d["pct"] < 5\n\ndef score_stock(d):\n    return min(100, d["ma5"] / d["ma20"] * 100)')
const running = ref(false)
const error = ref('')
const result = ref(null)
const builtinStrategyOptions = ref([
  { id: 'moving_avg', name: '双均线策略', desc: 'MA5 上穿 MA20' },
  { id: 'macd_signal', name: 'MACD 信号策略', desc: 'DIF 上穿 DEA + 量能确认' },
  { id: 'bollinger', name: '布林带策略', desc: '收盘破下轨（均值回归）' },
  { id: 'rsi_meanrev', name: 'RSI 均值回归', desc: 'RSI < 30 买入' },
  { id: 'bse_smallcap', name: '北证50 小市值动量', desc: '北交所小市值高动量' },
  { id: 'grid_okx', name: 'OKX 网格交易', desc: '区间内自动低买高卖' },
])

// 挂载时拉取后端内置策略列表，覆盖默认列表
async function loadBuiltinStrategies() {
  try {
    const res = await getPyStrategies()
    if (Array.isArray(res.data) && res.data.length) {
      builtinStrategyOptions.value = res.data.map(s => ({ id: s.id, name: s.name, desc: s.description || '' }))
    }
  } catch {}
}

// 关闭弹窗：清状态并通知父组件
function close() {
  result.value = null
  error.value = ''
  emit('update:open', false)
}

// 弹窗打开时重置结果/错误
watch(() => props.open, (v) => {
  if (v) {
    result.value = null
    error.value = ''
  }
})

// 执行筛选：内置走 getPyScreenStrategy；自定义走 getPyScreenCustom
async function runScreen() {
  if (running.value) return
  running.value = true
  error.value = ''
  result.value = null
  try {
    let res, data
    if (mode.value === 'builtin') {
      res = await getPyScreenStrategy(builtinId.value, { limit: limit.value, force: force.value })
      data = res.data || {}
    } else {
      if (!customCode.value.trim() || !customCode.value.includes('filter_stock')) {
        throw new Error('代码需定义 def filter_stock(data) -> bool')
      }
      res = await getPyScreenCustom({ code: customCode.value, limit: limit.value })
      data = res.data || {}
    }
    if (data.error) {
      error.value = String(data.error)
    } else {
      result.value = data
      emit('update:lastResult', data)
    }
  } catch (e) {
    error.value = e.message || '选股失败'
  } finally {
    running.value = false
  }
}

onMounted(() => {
  loadBuiltinStrategies()
})
</script>

<style scoped>
/* amber 家族通配命中（与父组件保持视觉一致） */
[class*="text-amber-100"], [class*="text-amber-200"] { color: #dbe7f3 !important; }
[class*="text-amber-300"], [class*="text-amber-400"] { color: #3ae2ee !important; }
[class*="text-amber-500"], [class*="text-amber-600"] { color: #8fc0d9 !important; }
[class*="text-amber-700"] { color: #6d8fb0 !important; }
[class*="bg-[#332314]"] { background: #101c30 !important; }
[class*="bg-[#1a1208]"] { background: #0b1422 !important; }
[class*="border-amber-800"] { border-color: rgba(120, 170, 220, 0.16) !important; }
[class*="border-amber-400"] { border-color: rgba(34, 211, 238, 0.40) !important; }
[class*="font-serif"] { font-family: 'SF Pro Text', 'PingFang SC', 'Microsoft YaHei', sans-serif !important; }
</style>

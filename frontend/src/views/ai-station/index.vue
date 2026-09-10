<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <!-- Hero -->
      <PageBack label="返回工具集" to="/tools" class="mb-5" />
      <section class="glass-panel p-8 mb-6 relative overflow-hidden cyber-scanline">
        <div class="absolute -top-24 -right-24 w-72 h-72 rounded-full bg-web3-primary/20 blur-3xl pointer-events-none"></div>
        <h1 class="text-3xl md:text-4xl font-bold text-gradient-cyber">AI 中转站</h1>
        <p class="mt-3 text-sm text-gray-400 max-w-2xl leading-relaxed">
          一个密钥，调用全部主流大模型。创建属于你的 API 令牌，按量计费、透明倍率，
          兑换码即可充值。兼容 OpenAI 接口协议，一行配置接入任意应用。
        </p>
        <div class="mt-5 flex flex-wrap items-center gap-3">
          <code class="text-xs bg-black/40 text-web3-accent px-3 py-2 rounded-lg font-mono">POST {{ origin }}/api/v1/chat/completions</code>
          <button class="web3-btn py-2 px-4 text-sm" @click="createOpen = true">+ 创建令牌</button>
        </div>
      </section>

      <!-- Usage stats -->
      <section class="grid grid-cols-2 gap-4 mb-6 lg:grid-cols-4">
        <div v-for="s in statCards" :key="s.label" class="glass-panel p-5">
          <p class="text-xs text-gray-500">{{ s.label }}</p>
          <p class="mt-2 text-xl font-bold text-white">{{ s.value }}</p>
        </div>
      </section>

      <div class="grid grid-cols-1 gap-6 lg:grid-cols-5">
        <!-- My tokens -->
        <section class="lg:col-span-3 glass-panel p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-white">🔑 我的令牌</h3>
            <button class="text-xs text-web3-accent hover:underline" @click="loadTokens">刷新</button>
          </div>

          <div v-if="tokens.length" class="space-y-3">
            <div v-for="t in tokens" :key="t.id" class="glass-panel-sm p-4">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-sm font-semibold text-white flex items-center gap-2">
                    {{ t.name }}
                    <span :class="['text-[10px] px-1.5 py-0.5 rounded', t.status === 1 ? 'bg-green-500/20 text-green-400' : 'bg-gray-500/20 text-gray-400']">{{ t.status === 1 ? '启用' : '停用' }}</span>
                  </p>
                  <p class="text-[11px] text-gray-500 font-mono mt-1 truncate">{{ t.keyMasked }}</p>
                  <p class="text-[11px] text-gray-500 mt-1">
                    调用 {{ t.requestCount || 0 }} 次 · 已用 {{ formatQuota(t.usedQuota) }} / {{ t.unlimitedQuota === 1 ? '∞' : formatQuota(t.quota) }}
                  </p>
                  <div v-if="t.quota" class="mt-2 h-1.5 w-full bg-white/5 rounded-full overflow-hidden">
                    <div class="h-full bg-gradient-to-r from-web3-primary to-web3-accent" :style="{ width: quotaPercent(t) + '%' }"></div>
                  </div>
                </div>
                <div class="flex flex-col items-end gap-1 shrink-0">
                  <button class="text-xs text-gray-400 hover:text-white" @click="toggle(t)">{{ t.status === 1 ? '停用' : '启用' }}</button>
                  <button class="text-xs text-web3-accent hover:underline" @click="openRedeem(t)">兑换码</button>
                  <button class="text-xs text-red-400 hover:text-red-300" @click="remove(t)">删除</button>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="!loadingTokens" class="text-center text-gray-500 py-12">
            <p class="text-3xl mb-2">🗝️</p>
            <p class="text-sm">还没有令牌，点击右上角创建</p>
          </div>
          <div v-else><Loading /></div>
        </section>

        <!-- Models pricing + redeem -->
        <section class="lg:col-span-2 space-y-6">
          <div class="glass-panel p-6">
            <h3 class="text-lg font-semibold text-white mb-4">💰 模型与倍率</h3>
            <div v-if="models.length" class="space-y-2 max-h-80 overflow-y-auto pr-1">
              <div v-for="m in models" :key="m.modelName" class="flex items-center justify-between glass-panel-sm px-3 py-2">
                <div class="min-w-0">
                  <p class="text-xs font-mono text-gray-200 truncate">{{ m.modelName }}</p>
                  <p class="text-[10px] text-gray-500">{{ m.tags || m.remark || '通用模型' }}</p>
                </div>
                <span class="text-xs text-web3-accent font-semibold shrink-0 ml-3">×{{ m.completionRate ?? m.modelRate ?? 1 }}</span>
              </div>
            </div>
            <p v-else class="text-sm text-gray-500 text-center py-8">暂无可用模型</p>
          </div>

          <div class="glass-panel p-6">
            <h3 class="text-lg font-semibold text-white mb-4">📡 快速接入</h3>
            <pre class="text-[11px] leading-relaxed text-gray-300 bg-black/40 rounded-xl p-4 overflow-x-auto font-mono">curl {{ origin }}/api/v1/chat/completions \
  -H "Authorization: Bearer sk-你的令牌" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [{"role":"user","content":"hi"}]
  }'</pre>
            <p class="text-[11px] text-gray-500 mt-3">额度单位：500000 = $1，按模型倍率折算扣费。</p>
          </div>
        </section>
      </div>
    </div>

    <!-- Create token modal -->
    <div v-if="createOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="createOpen = false"></div>
      <div class="relative glass-panel p-6 w-full max-w-md neon-border">
        <h3 class="text-lg font-semibold text-white mb-4">创建令牌</h3>
        <div class="space-y-3">
          <input v-model="form.name" class="web3-input w-full" placeholder="令牌名称（如：我的应用）" />
          <input v-model.number="form.dollars" type="number" min="0.5" step="0.5" class="web3-input w-full" placeholder="初始额度（美元，如 1）" />
          <input v-model="form.expiredAt" class="web3-input w-full" placeholder="过期时间（可选，yyyy-MM-dd HH:mm）" />
          <p class="text-[11px] text-gray-500">创建后仅展示一次完整密钥，请妥善保存。</p>
        </div>
        <div class="mt-5 flex gap-3 justify-end">
          <button class="px-4 py-2 text-sm text-gray-400 hover:text-white" @click="createOpen = false">取消</button>
          <button class="web3-btn py-2 px-5 text-sm" :disabled="creating" @click="createToken">{{ creating ? '创建中...' : '创建' }}</button>
        </div>
      </div>
    </div>

    <!-- Full token modal (once) -->
    <div v-if="fullToken" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="fullToken = ''"></div>
      <div class="relative glass-panel p-6 w-full max-w-md neon-border">
        <h3 class="text-lg font-semibold text-white mb-2">🎉 令牌创建成功</h3>
        <p class="text-xs text-red-300 mb-3">以下密钥仅此一次展示，关闭后无法再查看：</p>
        <code class="block text-xs bg-black/40 text-green-300 p-3 rounded-lg font-mono break-all select-all">{{ fullToken }}</code>
        <div class="mt-4 flex gap-3 justify-end">
          <button class="px-4 py-2 text-sm text-gray-300 hover:text-white border border-white/10 rounded-lg" @click="copyFullToken">复制</button>
          <button class="web3-btn py-2 px-5 text-sm" @click="fullToken = ''; createOpen = false">我已保存</button>
        </div>
      </div>
    </div>

    <!-- Redeem modal -->
    <div v-if="redeemTarget" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="redeemTarget = null"></div>
      <div class="relative glass-panel p-6 w-full max-w-md">
        <h3 class="text-lg font-semibold text-white mb-1">兑换码充值</h3>
        <p class="text-xs text-gray-500 mb-4">充值到令牌：{{ redeemTarget.name }}</p>
        <input v-model="redeemCode" class="web3-input w-full" placeholder="输入 24 位兑换码" />
        <div class="mt-5 flex gap-3 justify-end">
          <button class="px-4 py-2 text-sm text-gray-400 hover:text-white" @click="redeemTarget = null">取消</button>
          <button class="web3-btn py-2 px-5 text-sm" :disabled="redeeming" @click="doRedeem">{{ redeeming ? '核销中...' : '核销' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// AI 中转站门户页（用户侧）
// 模型定价 / 我的令牌管理（创建一次性展示密钥）/ 兑换码自助充值 / 用量统计
// ====================================================
import { ref, computed, onMounted } from 'vue'
import { getUserModels, getMyTokens, createMyToken, updateMyToken, deleteMyToken, redeemToMyToken, getMyAiStats } from '@/api/aiproxy'
import { useToastStore } from '@/stores/modules/toast'
import Loading from '@/components/common/Loading.vue'
import PageBack from '@/components/PageBack.vue'
import { confirm as dlgConfirm } from '@/composables/useDialog'

const toast = useToastStore()
const origin = window.location.origin

const models = ref([])
const tokens = ref([])
const loadingTokens = ref(true)
const stats = ref(null)
const createOpen = ref(false)
const creating = ref(false)
const form = ref({ name: '', dollars: 1, expiredAt: '' })
const fullToken = ref('')
const redeemTarget = ref(null)
const redeemCode = ref('')
const redeeming = ref(false)

// 额度整数单位 → 美元显示
function formatQuota(q) {
  if (q == null) return '-'
  return '$' + (Number(q) / 500000).toFixed(2)
}

function quotaPercent(t) {
  const used = Number(t.usedQuota || 0)
  const total = Number(t.quota || 0)
  return total <= 0 ? 100 : Math.min(100, Math.round((used / total) * 100))
}

const statCards = computed(() => [
  { label: '近 30 天调用', value: stats.value?.requestCount ?? '-' },
  { label: '输入 Token', value: stats.value?.promptTokens ?? '-' },
  { label: '输出 Token', value: stats.value?.completionTokens ?? '-' },
  { label: '消耗额度', value: stats.value ? formatQuota(stats.value.quotaUsed) : '-' }
])

async function loadModels() {
  try {
    const res = await getUserModels()
    models.value = res.data || res || []
  } catch { models.value = [] }
}

async function loadTokens() {
  loadingTokens.value = true
  try {
    const res = await getMyTokens({ page: 1, size: 50 })
    tokens.value = res.data?.records || []
  } catch { tokens.value = [] }
  finally { loadingTokens.value = false }
}

async function loadStats() {
  try {
    const res = await getMyAiStats()
    stats.value = res.data || res
  } catch { stats.value = null }
}

async function createToken() {
  creating.value = true
  try {
    const quota = Math.max(1, Math.round(Number(form.value.dollars || 1) * 500000))
    const payload = { name: form.value.name || '我的令牌', quota }
    if (form.value.expiredAt && form.value.expiredAt.trim()) {
      payload.expiredAt = form.value.expiredAt.trim().replace(' ', 'T')
    }
    const res = await createMyToken(payload)
    fullToken.value = res.data?.fullToken || ''
    createOpen.value = false
    form.value = { name: '', dollars: 1, expiredAt: '' }
    await loadTokens()
  } catch {
    /* 错误由全局拦截器提示 */
  } finally {
    creating.value = false
  }
}

async function copyFullToken() {
  try {
    await navigator.clipboard.writeText(fullToken.value)
    toast.success('已复制到剪贴板')
  } catch { toast.error('复制失败，请手动选择复制') }
}

async function toggle(t) {
  try {
    await updateMyToken(t.id, { status: t.status === 1 ? 0 : 1 })
    t.status = t.status === 1 ? 0 : 1
  } catch {}
}

async function remove(t) {
  if (!(await dlgConfirm(`确认删除令牌「${t.name}」？该操作不可恢复`))) return
  try {
    await deleteMyToken(t.id)
    tokens.value = tokens.value.filter(x => x.id !== t.id)
    toast.success('已删除')
  } catch {}
}

function openRedeem(t) {
  redeemTarget.value = t
  redeemCode.value = ''
}

async function doRedeem() {
  if (!redeemCode.value.trim()) { toast.warning('请输入兑换码'); return }
  redeeming.value = true
  try {
    await redeemToMyToken(redeemTarget.value.id, redeemCode.value.trim())
    toast.success('兑换成功，额度已充值')
    redeemTarget.value = null
    await Promise.all([loadTokens(), loadStats()])
  } catch {
    /* 失败原因由全局拦截器提示 */
  } finally {
    redeeming.value = false
  }
}

onMounted(() => {
  loadModels()
  loadTokens()
  loadStats()
})
</script>

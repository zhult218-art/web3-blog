<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <PageBack label="返回商城" to="/shop" class="mb-5" />
      <h1 class="text-3xl font-bold mb-2 text-gradient-cyber">支付收银台</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">官方支付 · Alipay · WeChat Pay</p>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <!-- 订单信息 -->
        <div class="glass-panel p-6 cyber-scanline relative overflow-hidden">
          <h3 class="text-lg font-semibold text-white flex items-center gap-2"><span>📋</span>订单信息</h3>
          <div class="holo-bar mt-4 mb-5"></div>
          <div class="space-y-4">
            <input v-model="orderId" class="web3-input" placeholder="输入订单ID查询状态" />
            <button class="web3-btn w-full py-2.5" :disabled="loading" @click="loadStatus">{{ loading ? '查询中...' : '查询订单' }}</button>
            <div v-if="orderInfo" class="glass-panel-sm p-4 space-y-2">
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-400">订单编号</span>
                <span class="text-xs text-gray-300 font-mono">{{ orderInfo.orderNo || orderInfo.id }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-400">订单金额</span>
                <span class="text-lg font-bold text-web3-accent">¥{{ orderInfo.amount }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-400">订单状态</span>
                <span :class="['text-sm font-semibold', statusClass(orderInfo.status)]">{{ statusLabels[orderInfo.status] || orderInfo.status }}</span>
              </div>
              <p v-if="orderInfo.paidAt" class="text-xs text-gray-500">支付时间：{{ formatTime(orderInfo.paidAt) }}</p>
              <p v-if="orderInfo.tradeNo" class="text-xs text-gray-500 truncate">交易号：{{ orderInfo.tradeNo }}</p>
            </div>
          </div>
        </div>

        <!-- 商品选择 + 创建订单 + 支付 -->
        <div class="glass-panel p-6">
          <h3 class="text-lg font-semibold text-white flex items-center gap-2"><span>🛒</span>选择商品创建订单</h3>
          <div class="holo-bar mt-4 mb-5"></div>
          <div class="space-y-4">
            <div v-if="productsLoading" class="text-xs text-gray-500 text-center py-4">加载商品中...</div>
            <div v-else-if="!products.length" class="text-xs text-gray-500 text-center py-4">暂无商品，请先在后台添加商品</div>
            <div v-else class="space-y-2 max-h-60 overflow-y-auto">
              <div v-for="p in products" :key="p.id"
                @click="selectedProduct = p"
                :class="['glass-panel-sm p-3 cursor-pointer transition border',
                  selectedProduct?.id === p.id ? 'border-blue-400/40 bg-blue-500/5' : 'border-transparent hover:border-blue-400/20']">
                <div class="flex items-center justify-between">
                  <div class="min-w-0">
                    <p class="text-sm text-white truncate">{{ p.name }}</p>
                    <p class="text-[10px] text-gray-500">库存 {{ p.stock ?? p.stockCount ?? '--' }}</p>
                  </div>
                  <span class="text-sm font-bold text-web3-accent">¥{{ p.price }}</span>
                </div>
              </div>
            </div>

            <div v-if="selectedProduct" class="flex items-center gap-3">
              <label class="text-xs text-gray-400">数量</label>
              <input v-model.number="orderQuantity" type="number" min="1" class="web3-input w-20" />
              <span class="text-sm text-gray-400">合计：</span>
              <span class="text-lg font-bold text-web3-accent">¥{{ (Number(selectedProduct.price) * orderQuantity).toFixed(2) }}</span>
            </div>

            <button class="web3-btn w-full py-2.5" :disabled="creatingOrder || !selectedProduct" @click="createOrderFromProduct">
              {{ creatingOrder ? '创建中...' : '创建订单' }}
            </button>

            <div v-if="orderId" class="border-t border-gray-700/30 pt-4 space-y-2">
              <p class="text-xs text-gray-400 text-center">订单已创建，点击下方按钮发起支付</p>
              <div class="grid grid-cols-2 gap-2">
                <button class="web3-btn-sm py-2 bg-blue-500/20 hover:bg-blue-500/30" :disabled="paying || !payable" @click="alipay">💙 支付宝</button>
                <button class="web3-btn-sm py-2 bg-green-500/20 hover:bg-green-500/30" :disabled="paying || !payable" @click="wechat">💚 微信支付</button>
              </div>
              <p v-if="orderInfo && !payable" class="text-xs text-gray-500 text-center">
                {{ statusLabels[orderInfo.status] || orderInfo.status }}，无需支付
              </p>
            </div>
            <router-link to="/profile/orders" class="block text-center text-xs text-web3-accent hover:underline mt-2">查看我的订单 →</router-link>
          </div>
        </div>
      </div>

      <!-- 微信支付收款码弹层：展示个人收款码 + 引导添加联系方式 -->
      <div v-if="wechatQrVisible" class="mt-6 glass-panel p-6 neon-border relative">
        <button class="absolute top-3 right-3 text-gray-400 hover:text-white text-xl" @click="closeWechatQr" aria-label="关闭">×</button>
        <h3 class="text-lg font-semibold text-white mb-4 flex items-center gap-2"><span>💚</span>微信扫码支付</h3>
        <div class="flex flex-col items-center gap-4">
          <div class="bg-white rounded-2xl p-3 shadow-lg">
            <img src="/images/pay-wechat.jpg" alt="微信收款码" class="w-64 h-64 object-contain rounded-xl" />
          </div>
          <div class="text-center space-y-1.5 max-w-sm">
            <p class="text-sm text-white font-semibold">请用微信扫描上方收款码完成付款</p>
            <p class="text-xs text-gray-400">付款金额：¥{{ wechatPayAmount }}</p>
            <div class="mt-3 p-3 rounded-xl bg-cyan-500/10 border border-cyan-400/30">
              <p class="text-xs text-cyan-300 font-semibold mb-1">付款完成后，请添加以下联系方式进行后续操作</p>
              <p class="text-sm text-white font-mono select-all">微信：zlt194253038</p>
              <p class="text-[11px] text-gray-400 mt-1">添加后告知订单号 {{ orderId || '（请先创建订单）' }} 即可完成核销</p>
            </div>
            <p class="text-[11px] text-gray-500 mt-2">人工核销模式：付款后联系商家确认，订单状态将在确认后更新为「已支付」</p>
          </div>
          <button class="web3-btn-sm px-6 py-2 mt-1" @click="closeWechatQr">我已完成付款</button>
        </div>
      </div>

      <!-- 支付结果 -->
      <div v-if="payResult" class="mt-6 glass-panel p-6 neon-border">
        <h3 class="text-lg font-semibold text-white mb-3 flex items-center gap-2"><span>📋</span>下单响应</h3>
        <pre class="text-xs text-gray-300 bg-black/40 rounded-xl p-5 overflow-x-auto max-h-80 font-mono leading-relaxed">{{ typeof payResult === 'string' ? payResult : JSON.stringify(payResult, null, 2) }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 支付收银台页：
// - 支付宝：对接官方 SDK，后端返回 form HTML，新窗口自动提交跳转支付宝收银台
// - 微信：个人收款码模式（无商户号），展示本地收款码图片 + 引导添加联系方式
//         付款后人工核销，商家确认后通过后台改订单状态为 PAID
// ====================================================
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail, createAlipay, getProductList, createOrder, getPayStatus } from '@/api/shop'
import { useToastStore } from '@/stores/modules/toast'
import PageBack from '@/components/PageBack.vue'
import { formatDateTimeCN as formatTime } from '@/utils/date.js'

const route = useRoute()
const toast = useToastStore()
const orderId = ref('')
const orderInfo = ref(null)
const payResult = ref(null)
const loading = ref(false)
const paying = ref(false)
const products = ref([])
const productsLoading = ref(false)
const selectedProduct = ref(null)
const orderQuantity = ref(1)
const creatingOrder = ref(false)

// 微信收款码弹层状态
const wechatQrVisible = ref(false)
const wechatPayAmount = ref('0.00')
// 支付宝轮询（支付宝仍走 SDK，支付后轮询订单状态）
let pollTimer = null
let pollStart = 0

const statusLabels = {
  'PENDING': '待支付',
  'PAID': '已支付',
  'SHIPPED': '已发货',
  'COMPLETED': '已完成',
  'CANCELLED': '已取消'
}

const payable = computed(() => !orderInfo.value || orderInfo.value.status === 'PENDING')

function statusClass(s) {
  if (s === 'PAID' || s === 'COMPLETED') return 'text-green-400'
  if (s === 'PENDING') return 'text-yellow-400'
  if (s === 'CANCELLED') return 'text-red-400'
  return 'text-gray-400'
}


// 拉取订单详情
async function loadStatus(id = orderId.value) {
  const target = String(id || '').trim()
  if (!target) { toast.warning('请输入订单ID'); return }
  orderId.value = target
  loading.value = true
  try {
    const res = await getOrderDetail(target)
    orderInfo.value = res?.data || res
    payResult.value = null
  } catch {
    orderInfo.value = null
  } finally {
    loading.value = false
  }
}

// 发起支付宝支付：后端返回完整 form HTML，新窗口自动提交跳转
async function alipay() {
  if (!orderId.value) { toast.warning('请先输入订单ID'); return }
  if (!payable.value) { toast.warning('当前订单状态不可支付'); return }
  paying.value = true
  try {
    const res = await createAlipay(orderId.value)
    const d = res?.data || res
    payResult.value = d
    const formHtml = d?.formHtml
    if (!formHtml) {
      toast.error('支付宝下单失败：未返回支付表单')
      return
    }
    // 新窗口写入 form HTML，自动 submit 跳转到支付宝收银台
    const w = window.open('', '_blank')
    if (!w) {
      // 浏览器拦截了新窗口，降级为当前页 document.write
      toast.warning('新窗口被拦截，将在当前页跳转')
      document.open()
      document.write(formHtml)
      document.close()
      return
    }
    w.document.open()
    w.document.write(formHtml)
    w.document.close()
    toast.success('已打开支付宝收银台，完成支付后请回到本页')
    // 支付宝同步 return_url 跳回后，前端会重新加载；这里也开始轮询以防 return_url 未配置
    startPolling()
  } catch (e) {
    /* 失败原因由全局拦截器 Toast 提示 */
  } finally {
    paying.value = false
  }
}

// 发起微信支付：展示个人收款码图片 + 引导联系方式（无商户号，人工核销模式）
async function wechat() {
  if (!orderId.value) { toast.warning('请先输入订单ID'); return }
  if (!payable.value) { toast.warning('当前订单状态不可支付'); return }
  // 先拉取订单金额用于展示
  const amount = orderInfo.value?.amount
    ? Number(orderInfo.value.amount).toFixed(2)
    : (selectedProduct.value
        ? (Number(selectedProduct.value.price) * orderQuantity.value).toFixed(2)
        : '0.00')
  wechatPayAmount.value = amount
  wechatQrVisible.value = true
  toast.info('请扫码付款后添加微信 zlt194253038 完成核销')
}

// 关闭微信收款码弹层
function closeWechatQr() {
  wechatQrVisible.value = false
}

// 轮询订单状态，PAID 即停止并提示（仅支付宝使用，微信走人工核销）
function startPolling() {
  stopPolling()
  pollStart = Date.now()
  pollTimer = setInterval(async () => {
    // 超过 10 分钟停止轮询
    if (Date.now() - pollStart > 10 * 60 * 1000) {
      stopPolling()
      return
    }
    try {
      const res = await getPayStatus(orderId.value)
      const d = res?.data || res
      if (d?.status === 'PAID') {
        stopPolling()
        toast.success('支付成功！订单已完成')
        await loadStatus()
      }
    } catch { /* 忽略单次失败，继续轮询 */ }
  }, 2000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

// 拉取商品列表
async function loadProducts() {
  productsLoading.value = true
  try {
    const res = await getProductList({ page: 1, size: 10 })
    const d = res?.data || res
    products.value = d?.list || d?.records || d || []
  } catch {
    products.value = []
  } finally {
    productsLoading.value = false
  }
}

// 选择商品后创建订单
async function createOrderFromProduct() {
  if (!selectedProduct.value) { toast.warning('请先选择商品'); return }
  creatingOrder.value = true
  try {
    const res = await createOrder({
      productId: selectedProduct.value.id,
      quantity: orderQuantity.value
    })
    const d = res?.data || res
    const newId = d?.orderId || d?.id || d?.orderNo
    if (newId) {
      toast.success('订单创建成功')
      orderId.value = String(newId)
      await loadStatus(newId)
    } else {
      toast.error('订单创建失败')
    }
  } finally {
    creatingOrder.value = false
  }
}

onMounted(() => {
  loadProducts()
  const qid = route.query.orderId
  if (qid) loadStatus(qid)
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.web3-btn-sm {
  border-radius: 8px;
  font-size: 12px;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.2s;
}
.web3-btn-sm:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>

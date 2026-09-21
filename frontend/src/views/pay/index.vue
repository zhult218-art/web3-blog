<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <PageBack label="返回商城" to="/shop" class="mb-5" />
      <h1 class="text-3xl font-bold mb-2 text-gradient-cyber">支付收银台</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">Sandbox Payment Testing · Alipay · WeChat Pay</p>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <!-- Order Info Card -->
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
            </div>
          </div>
        </div>

        <!-- 商品选择 + 创建订单 -->
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
              <p class="text-xs text-gray-400 text-center">订单已创建，点击下方按钮支付</p>
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

      <!-- Result panel -->
      <div v-if="payResult" class="mt-6 glass-panel p-6 neon-border">
        <h3 class="text-lg font-semibold text-white mb-3 flex items-center gap-2"><span>✅</span>支付结果</h3>
        <pre class="text-xs text-gray-300 bg-black/40 rounded-xl p-5 overflow-x-auto max-h-80 font-mono leading-relaxed">{{ typeof payResult === 'string' ? payResult : JSON.stringify(payResult, null, 2) }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 支付收银台页：沙箱模拟支付宝/微信支付
// 支持 /pay?orderId=xxx 直达（购物车结算/我的订单跳转），
// 支付后重新拉取订单状态；仅 PENDING 状态可发起支付
// ====================================================
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail, createAlipay, createWechatPay, getProductList, createOrder } from '@/api/shop'
import { useToastStore } from '@/stores/modules/toast'
import PageBack from '@/components/PageBack.vue'

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

const statusLabels = {
  'PENDING': '待支付',
  'PAID': '已支付',
  'SHIPPED': '已发货',
  'COMPLETED': '已完成',
  'CANCELLED': '已取消'
}

// 仅待支付订单允许发起支付
const payable = computed(() => !orderInfo.value || orderInfo.value.status === 'PENDING')

function statusClass(s) {
  if (s === 'PAID' || s === 'COMPLETED') return 'text-green-400'
  if (s === 'PENDING') return 'text-yellow-400'
  if (s === 'CANCELLED') return 'text-red-400'
  return 'text-gray-400'
}

function formatTime(d) { return d ? new Date(d).toLocaleString('zh-CN') : '' }

// 拉取订单详情（含金额与归属校验，后端仅允许查自己的订单）
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

async function pay(channel) {
  if (!orderId.value) { toast.warning('请先输入订单ID'); return }
  if (!payable.value) { toast.warning('当前订单状态不可支付'); return }
  paying.value = true
  try {
    const res = channel === 'alipay' ? await createAlipay(orderId.value) : await createWechatPay(orderId.value)
    payResult.value = res?.data || res
    toast.success(channel === 'alipay' ? '支付宝支付成功' : '微信支付成功')
    await loadStatus()
  } catch {
    /* 失败原因由全局拦截器 Toast 提示（已支付/已取消/无权限等） */
  } finally {
    paying.value = false
  }
}

const alipay = () => pay('alipay')
const wechat = () => pay('wechat')

// 拉取商品列表（右侧面板商品选择使用）
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

// 选择商品后创建订单，成功后自动填入 orderId 并拉取订单详情
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

// 进入收银台：加载商品列表；若携带 orderId 路由参数则直达订单状态
onMounted(() => {
  loadProducts()
  const qid = route.query.orderId
  if (qid) loadStatus(qid)
})
</script>

<style scoped>
/* 收银台右侧支付按钮：保持与 web3-btn 一致的玻璃拟态风格 */
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

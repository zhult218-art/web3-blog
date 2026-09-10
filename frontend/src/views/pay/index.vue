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

        <!-- Payment Methods -->
        <div class="glass-panel p-6">
          <h3 class="text-lg font-semibold text-white flex items-center gap-2"><span>💳</span>支付方式</h3>
          <div class="holo-bar mt-4 mb-5"></div>
          <div class="space-y-4">
            <button class="w-full glass-panel-sm p-4 flex items-center justify-between hover:border-blue-400/30 transition group cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed" :disabled="paying || !payable" @click="alipay">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-blue-500/20 flex items-center justify-center text-lg">💙</div>
                <div class="text-left">
                  <p class="text-sm font-semibold text-white group-hover:text-blue-300 transition">支付宝</p>
                  <p class="text-[10px] text-gray-500">沙箱环境模拟支付</p>
                </div>
              </div>
              <span class="text-gray-600 group-hover:text-blue-400 transition">→</span>
            </button>

            <button class="w-full glass-panel-sm p-4 flex items-center justify-between hover:border-green-400/30 transition group cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed" :disabled="paying || !payable" @click="wechat">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-green-500/20 flex items-center justify-center text-lg">💚</div>
                <div class="text-left">
                  <p class="text-sm font-semibold text-white group-hover:text-green-300 transition">微信支付</p>
                  <p class="text-[10px] text-gray-500">沙箱环境模拟支付</p>
                </div>
              </div>
              <span class="text-gray-600 group-hover:text-green-400 transition">→</span>
            </button>

            <p v-if="orderInfo && !payable" class="text-xs text-gray-500 text-center">
              {{ statusLabels[orderInfo.status] || orderInfo.status }}，无需支付
            </p>
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
import { getOrderDetail, createAlipay, createWechatPay } from '@/api/shop'
import { useToastStore } from '@/stores/modules/toast'
import PageBack from '@/components/PageBack.vue'

const route = useRoute()
const toast = useToastStore()
const orderId = ref('')
const orderInfo = ref(null)
const payResult = ref(null)
const loading = ref(false)
const paying = ref(false)

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

// 从路由参数直达收银台（?orderId=）
onMounted(() => {
  const qid = route.query.orderId
  if (qid) loadStatus(qid)
})
</script>

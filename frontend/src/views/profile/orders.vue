<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <PageBack label="返回个人中心" to="/profile" class="mb-5" />
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold bg-gradient-to-r from-[#a855f7] to-[#06b6d4] bg-clip-text text-transparent">我的订单</h1>
        <span class="text-xs text-gray-500">{{ orders.length }} 笔订单</span>
      </div>
      <div v-if="orders.length" class="space-y-4">
        <div v-for="order in orders" :key="order.id" class="glass-panel p-4 md:p-5">
          <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <!-- Order Info -->
            <div class="min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <p class="text-sm text-white font-semibold">订单 #{{ order.id }}</p>
                <span :class="['text-xs px-2 py-0.5 rounded-full border font-medium', badgeClass(order.status)]">{{ statusLabels[order.status] || order.status }}</span>
              </div>
              <p class="text-xs text-gray-500 mt-1 font-mono">{{ order.orderNo || '' }}</p>
              <p class="text-xs text-gray-400 mt-1">金额: <span class="text-[#a855f7] font-semibold">¥{{ order.amount }}</span></p>
              <p class="text-xs text-gray-600 mt-0.5">{{ formatDate(order.createdAt) }}</p>
            </div>

            <!-- Actions -->
            <div class="flex items-center gap-2 md:shrink-0 flex-wrap">
              <button
                v-if="order.status === 'PAID'"
                class="text-xs px-3.5 py-2 rounded-lg bg-cyan-500/10 text-cyan-400 border border-cyan-400/20 hover:bg-cyan-500/20 hover:border-cyan-400/40 transition"
                @click="goPay(order)">查看支付</button>
              <button
                v-if="order.status === 'PENDING'"
                class="text-xs px-3.5 py-2 rounded-lg bg-[#a855f7]/15 text-[#a855f7] border border-[#a855f7]/30 hover:bg-[#a855f7]/25 hover:border-[#a855f7]/50 transition"
                @click="goPay(order)">去支付</button>
              <button
                v-if="order.status === 'PENDING'"
                class="text-xs px-3.5 py-2 rounded-lg bg-red-500/10 text-red-400 border border-red-400/20 hover:bg-red-500/20 transition"
                :disabled="acting === order.id"
                @click="cancel(order)">{{ acting === order.id ? '取消中...' : '取消订单' }}</button>
              <button
                class="text-xs px-3.5 py-2 rounded-lg bg-gray-500/10 text-gray-400 border border-white/10 hover:bg-red-500/15 hover:text-red-400 hover:border-red-400/30 transition"
                :disabled="acting === order.id"
                @click="remove(order)">{{ acting === order.id ? '删除中...' : '删除' }}</button>
            </div>
          </div>
        </div>
      </div>
      <div v-else-if="!loading" class="text-center text-gray-500 py-20">
        <p class="text-4xl mb-4">📋</p>
        <p>暂无订单</p>
        <router-link to="/shop" class="web3-btn mt-4 inline-block">去购物</router-link>
      </div>
      <div v-else><Loading /></div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 我的订单页：加载并展示当前用户的订单列表
// 待支付订单支持跳转收银台与自助取消（取消自动回补库存）
// ====================================================
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList, cancelOrder, deleteOrder } from '@/api/shop'
import PageBack from '@/components/PageBack.vue'
import { useToastStore } from '@/stores/modules/toast'
import { confirm } from '@/composables/useDialog'
import Loading from '@/components/common/Loading.vue'

const router = useRouter()
const toast = useToastStore()
const orders = ref([]); const loading = ref(true); const acting = ref(null)

const statusLabels = {
  'PENDING': '待支付',
  'PAID': '已支付',
  'SHIPPED': '已发货',
  'COMPLETED': '已完成',
  'CANCELLED': '已取消'
}

function badgeClass(s) {
  if (s === 'PAID' || s === 'COMPLETED') return 'bg-green-500/10 text-green-400 border-green-400/25'
  if (s === 'PENDING') return 'bg-yellow-500/10 text-yellow-400 border-yellow-400/25'
  if (s === 'CANCELLED') return 'bg-red-500/10 text-red-400 border-red-400/25'
  return 'bg-gray-500/10 text-gray-400 border-gray-400/25'
}

// 本地化格式化订单时间
function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN') : '' }

function goPay(order) { router.push(`/pay?orderId=${order.id}`) }

// 自助取消：成功后本地更新状态；失败原因由全局拦截器提示
async function cancel(order) {
  acting.value = order.id
  try {
    await cancelOrder(order.id)
    order.status = 'CANCELLED'
    toast.success('订单已取消')
  } catch {
    await load()
  } finally {
    acting.value = null
  }
}

// 删除订单：二次确认后逻辑删除并刷新列表
async function remove(order) {
  if (!(await confirm(`确认删除订单 #${order.id}？删除后不可恢复。`))) return
  acting.value = order.id
  try {
    await deleteOrder(order.id)
    toast.success('订单已删除')
    await load()
  } catch {
    await load()
  } finally {
    acting.value = null
  }
}

async function load() {
  try {
    const res = await getOrderList({ page: 1, size: 50 })
    orders.value = res.data?.records || res.data || []
  } catch { orders.value = [] }
  finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen py-8 px-4 md:px-6">
    <div class="max-w-7xl mx-auto">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-purple">Order</span> Management</h1>
        <div class="flex gap-2 overflow-x-auto">
          <button v-for="f in filters" :key="f.value" @click="activeFilter = f.value; page = 1; fetchOrders()"
            :class="['px-3 py-1.5 rounded-lg text-xs font-medium border transition whitespace-nowrap',
              activeFilter === f.value ? 'bg-purple-500/15 border-purple-400/25 text-white' : 'border-white/[0.06] text-gray-500 hover:text-gray-300']">
            {{ f.label }}
          </button>
        </div>
      </div>
      <div class="glass-panel overflow-hidden">
        <div class="overflow-x-auto">
          <table class="web3-table min-w-[820px]">
            <thead><tr><th>订单号</th><th>用户ID</th><th>商品ID</th><th>金额</th><th>状态</th><th>订单编号</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="8" class="text-center py-10"><Loading text="加载中..." /></td>
              </tr>
              <tr v-else-if="!list.length">
                <td colspan="8" class="text-center text-gray-600 py-10">暂无订单</td>
              </tr>
              <tr v-for="o in list" :key="o.id">
                <td class="matrix-text text-xs">#{{ o.id }}</td>
                <td class="text-white/80">{{ o.userId ?? '-' }}</td>
                <td class="text-white/60">{{ o.productId ?? '-' }}</td>
                <td class="text-white/80 font-medium">¥{{ o.amount }}</td>
                <td><span :class="statusClass(o.status)">{{ statusLabel(o.status) }}</span></td>
                <td class="text-gray-500 text-xs matrix-text">{{ o.orderNo || '-' }}</td>
                <td class="text-gray-500 text-xs matrix-text">{{ formatDate(o.createdAt) }}</td>
                <td>
                  <div class="flex gap-2 whitespace-nowrap">
                    <template v-if="o.status === 'PENDING'">
                      <button @click="setStatus(o, 'PAID')" class="text-xs text-green-400 hover:text-green-300">标记已付</button>
                    </template>
                    <template v-if="o.status === 'PAID'">
                      <button @click="setStatus(o, 'COMPLETED')" class="text-xs text-cyan-400 hover:text-cyan-300">完成</button>
                    </template>
                    <template v-if="o.status !== 'CANCELLED' && o.status !== 'COMPLETED'">
                      <button @click="setStatus(o, 'CANCELLED')" class="text-xs text-red-400 hover:text-red-300">取消</button>
                    </template>
                    <span v-if="o.status === 'CANCELLED' || o.status === 'COMPLETED'" class="text-xs text-gray-600">已终态</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="mt-4" v-if="total > 0">
        <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="fetchOrders" />
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 订单管理：订单列表（状态筛选 + 分页），
// 支持手动更新订单状态
// ====================================================
import { ref, onMounted } from 'vue'
import { getAdminOrderList, updateOrderStatus } from '@/api/shop'
import Pagination from '@/components/common/Pagination.vue'
import Loading from '@/components/common/Loading.vue'
import { useToastStore } from '@/stores/modules/toast'
import { formatDateTimeCN } from '@/utils/date'

const toast = useToastStore()
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const activeFilter = ref('')
// 订单状态筛选配置
const filters = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'PENDING' },
  { label: '已支付', value: 'PAID' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

// 格式化订单时间，空值显示为「-」
function formatDate(d) { return formatDateTimeCN(d, '-') }
// 订单状态对应的徽章样式类
function statusClass(s) {
  const map = { PAID: 'web3-badge-green', PENDING: 'web3-badge-orange', COMPLETED: 'web3-badge-cyan', CANCELLED: 'web3-badge-red' }
  return map[s] || 'web3-badge-purple'
}
// 订单状态中文文案
function statusLabel(s) {
  const map = { PAID: '已支付', PENDING: '待支付', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[s] || s
}

// 按分页与状态筛选条件加载订单列表
async function fetchOrders() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (activeFilter.value) params.status = activeFilter.value
    const res = await getAdminOrderList(params)
    const data = res.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } catch { list.value = [] }
  finally { loading.value = false }
}

// 更新指定订单的状态
async function setStatus(o, s) {
  try {
    await updateOrderStatus(o.id, s)
    o.status = s
    toast.success('状态已更新')
  } catch { toast.error('操作失败') }
}

// 挂载时加载订单列表
onMounted(fetchOrders)
</script>
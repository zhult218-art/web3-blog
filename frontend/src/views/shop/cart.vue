<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-4xl">
      <PageBack label="返回商城" to="/shop" class="mb-5" />
      <h1 class="text-2xl font-bold mb-6">购物车</h1>
      <div v-if="cart.items.length" class="space-y-4">
        <div v-for="item in cart.items" :key="item.id" class="glass-panel p-4 flex items-center justify-between">
          <div class="flex items-center gap-4">
            <div class="w-16 h-16 bg-gradient-to-br from-web3-primary/30 to-web3-accent/30 rounded flex items-center justify-center text-2xl">🛍</div>
            <div>
              <h4 class="font-semibold text-white">{{ item.name }}</h4>
              <p class="text-sm text-web3-accent">¥{{ item.price }} × {{ item.quantity }}</p>
            </div>
          </div>
          <button class="text-red-400 hover:text-red-300 text-sm" @click="cart.remove(item.id)">删除</button>
        </div>
        <div class="glass-panel p-4 flex justify-between items-center">
          <span class="text-gray-300">合计: <span class="text-xl font-bold text-web3-accent">¥{{ cart.totalPrice.toFixed(2) }}</span></span>
          <button class="web3-btn" @click="checkout" :disabled="checking">{{ checking ? '处理中...' : '去结算' }}</button>
        </div>
      </div>
      <div v-else class="text-center text-gray-500 py-20">
        <p class="text-4xl mb-4">🛒</p>
        <p>购物车为空</p>
        <router-link to="/shop" class="web3-btn mt-4 inline-block">去逛逛</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 购物车页：展示购物车商品，支持结算下单
// ====================================================
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/modules/cart'
import { createOrder } from '@/api/shop'
import { useToastStore } from '@/stores/modules/toast'
import PageBack from '@/components/PageBack.vue'

const router = useRouter()
const cart = useCartStore()
const toast = useToastStore()
const checking = ref(false)

// 结算：遍历购物车逐项下单；成功项从购物车移除并跳转收银台，
// 失败项保留在购物车（具体原因由全局拦截器 Toast 提示，如登录过期/库存不足）
async function checkout() {
  if (!cart.items.length) return
  checking.value = true
  const failed = []
  const total = cart.items.length
  let firstOrderId = null
  try {
    for (const item of [...cart.items]) {
      try {
        const res = await createOrder({ productId: item.id, quantity: item.quantity })
        const order = res?.data || res
        if (order?.id && !firstOrderId) firstOrderId = order.id
        cart.remove(item.id)
      } catch {
        failed.push(item.name)
      }
    }
    if (!firstOrderId) {
      toast.error(`下单失败：${failed.join('、')}`)
      return
    }
    toast.success(failed.length ? `部分下单成功，失败：${failed.join('、')}` : '下单成功！')
    router.push(`/pay?orderId=${firstOrderId}`)
  } finally {
    checking.value = false
  }
}
</script>

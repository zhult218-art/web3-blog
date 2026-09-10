// ============================================================
// 购物车 Store（Pinia）
// 商品列表持久化到 localStorage（web3_cart），刷新不丢失
// 被商城列表页、详情页与购物车页使用
// ============================================================
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCartStore = defineStore('cart', () => {
  // 初始化：读取本地缓存（容错解析）
  let saved = []
  try {
    saved = JSON.parse(localStorage.getItem('web3_cart') || '[]')
  } catch {
    saved = []
  }
  if (!Array.isArray(saved)) saved = []
  const items = ref(saved)

  // 购物车商品总件数
  const totalCount = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
  // 购物车商品总金额
  const totalPrice = computed(() => items.value.reduce((s, i) => s + i.price * i.quantity, 0))

  // 持久化到 localStorage
  function save() {
    localStorage.setItem('web3_cart', JSON.stringify(items.value))
  }

  // 加入购物车：已存在则累加数量
  function add(product, quantity = 1) {
    const exist = items.value.find(i => i.id === product.id)
    if (exist) exist.quantity += quantity
    else items.value.push({ ...product, quantity })
    save()
  }

  // 查询某商品在购物车中的数量
  function getItemQuantity(productId) {
    const item = items.value.find(i => i.id === productId)
    return item ? item.quantity : 0
  }

  // 移除商品
  function remove(productId) {
    items.value = items.value.filter(i => i.id !== productId)
    save()
  }

  // 清空购物车
  function clear() {
    items.value = []
    save()
  }

  return { items, totalCount, totalPrice, add, remove, clear, getItemQuantity }
})

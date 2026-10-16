<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <PageHeader data-voice-target title="在线商城" subtitle="Online Shop · Digital Goods · Instant Delivery"
        :crumbs="[{ label: '首页', to: '/' }, { label: '在线商城' }]">
        <template #actions>
          <router-link to="/shop/cart" class="web3-btn text-sm relative">
            🛒 购物车
            <span v-if="cart.totalCount" class="absolute -top-1 -right-1 min-w-4 h-4 px-1 bg-pink-500 rounded-full text-xs flex items-center justify-center">{{ cart.totalCount }}</span>
          </router-link>
        </template>
      </PageHeader>

      <div class="flex gap-2 mb-6 flex-wrap">
        <button v-for="cat in categories" :key="cat" class="web3-pill" :class="{ 'web3-pill-active': selectedCat === cat }"
          @click="setCategory(cat)">
          {{ cat }}
        </button>
      </div>

      <div v-if="list.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        <div v-for="item in list" :key="item.id" v-tilt class="glass-panel overflow-hidden group cursor-pointer" @click="goDetail(item)">
          <div class="h-44 bg-gradient-to-br from-web3-primary/30 to-web3-accent/30 flex items-center justify-center text-4xl overflow-hidden">
            <!-- 命中品牌词典优先展示官方图标（主源品牌色 → 旧版单色 → 浅色底），否则展示封面，再回退 🛍 -->
            <div v-if="iconSrc(item)"
              class="w-24 h-24 rounded-2xl bg-[rgba(255,255,255,0.92)] flex items-center justify-center group-hover:scale-105 transition-transform duration-500">
              <img :src="iconSrc(item)" :alt="item.name" class="w-12 h-12 object-contain" @error="onIconError(item)" />
            </div>
            <img v-else-if="item.cover && !coverFailed.has(item.id)" :src="item.cover"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" @error="coverFailed.add(item.id)" />
            <span v-else>🛍</span>
          </div>
          <div class="p-5">
            <div class="flex items-center gap-2 mb-2">
              <span v-if="item.category" class="text-[10px] px-2 py-0.5 rounded bg-cyan-500/10 text-cyan-400 border border-cyan-400/20">{{ item.category }}</span>
              <span v-if="item.stock <= 0" class="text-[10px] px-2 py-0.5 rounded bg-red-500/10 text-red-400 border border-red-400/20">售罄</span>
            </div>
            <h3 class="font-semibold text-white group-hover:text-cyan-300 transition line-clamp-1">{{ item.name }}</h3>
            <p class="mt-1 text-xs text-gray-400 line-clamp-2">{{ item.description }}</p>
            <div class="mt-3 flex items-center justify-between">
              <span class="text-lg font-bold text-web3-accent">¥{{ item.price }}</span>
              <div class="flex gap-2" @click.stop>
                <button class="web3-btn text-xs px-3 py-1" @click="addToCart(item)">
            {{ cart.getItemQuantity(item.id) > 0 ? `已加(${cart.getItemQuantity(item.id)})` : '加购' }}
          </button>
                <button class="text-xs border border-cyan-400/30 text-cyan-400 rounded px-3 py-1 hover:bg-cyan-400/10" @click="buyNow(item)">购买</button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else-if="!loaded" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        <SkeletonCard v-for="i in 8" :key="i" :lines="2" />
      </div>
      <EmptyState v-else text="暂无商品" />
      <div class="mt-6" v-if="total > 0">
        <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="load" />
      </div>
    </div>

    <!-- 立即购买收款码弹窗：创建订单后展示个人收款码 + 联系方式 -->
    <transition name="fade">
      <div v-if="payModalVisible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4" @click.self="closePayModal">
        <div class="glass-panel p-6 max-w-sm w-full relative">
          <button class="absolute top-3 right-3 text-gray-400 hover:text-white text-xl" @click="closePayModal" aria-label="关闭">×</button>
          <h3 class="text-lg font-bold text-white mb-1 flex items-center gap-2"><span>💚</span>微信扫码支付</h3>
          <p class="text-xs text-gray-500 mb-4">订单已创建，请扫码完成付款</p>
          <div class="flex flex-col items-center gap-3">
            <div class="bg-white rounded-2xl p-3 shadow-lg">
              <img src="/images/pay-wechat.jpg" alt="微信收款码" class="w-56 h-56 object-contain rounded-xl" />
            </div>
            <div class="text-center space-y-1 w-full">
              <p class="text-sm text-white">付款金额：<span class="text-lg font-bold text-web3-accent">¥{{ payModalAmount }}</span></p>
              <p class="text-[11px] text-gray-500 truncate">订单号：{{ payModalOrderNo || '--' }}</p>
              <div class="mt-2 p-2.5 rounded-xl bg-cyan-500/10 border border-cyan-400/30">
                <p class="text-xs text-cyan-300 font-semibold mb-0.5">付款完成后添加联系方式</p>
                <p class="text-sm text-white font-mono select-all">微信：zlt194253038</p>
              </div>
            </div>
            <div class="flex gap-2 w-full mt-1">
              <button class="web3-btn-sm flex-1 py-2" @click="goPayPage">去收银台</button>
              <button class="flex-1 py-2 text-xs border border-white/15 text-gray-300 rounded-lg hover:bg-white/5" @click="closePayModal">关闭</button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
// ====================================================
// 在线商城页：商品列表（分类筛选 + 分页）、
// 加入购物车与立即购买
// ====================================================
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProductList, createOrder } from '@/api/shop'
import { useCartStore } from '@/stores/modules/cart'
import { useToastStore } from '@/stores/modules/toast'
import { usePagedList } from '@/composables/usePagedList'
import { useBrandIcon } from '@/composables/useBrandIcon'
import PageHeader from '@/components/common/PageHeader.vue'
import Pagination from '@/components/common/Pagination.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const cart = useCartStore()
const toast = useToastStore()
// 分页列表状态（页码 / 分类 / 加载 / 空状态）统一由 usePagedList 管理
const { list, page, size, total, loaded, selectedCat, load, setCategory } = usePagedList(getProductList, { pageSize: 12 })
const categories = ref(['全部', '电子产品', '软件服务', '教程课程', '数字艺术', '开发工具', '其他'])
// 品牌图标两级降级：0=主源(simpleicons 品牌色) 1=旧版源(jsdelivr 单色) 2=🛍 回退。
// 商城图标容器为浅色底，单色 SVG 无需 invert
const { src: iconSrc, onError: onIconError } = useBrandIcon()
// 封面加载失败的商品 id 集合，失败后回退 🛍
const coverFailed = ref(new Set())

// 跳转商品详情页
function goDetail(item) {
  router.push(`/shop/${item.id}`)
}

// 加入购物车并提示
function addToCart(item) {
  cart.add(item)
  toast.success('已加入购物车')
}

// 立即购买：创建订单后弹出个人收款码弹窗 + 联系方式引导（人工核销模式）
const payModalVisible = ref(false)
const payModalAmount = ref('0.00')
const payModalOrderNo = ref('')
const payModalOrderId = ref(null)

async function buyNow(item) {
  try {
    const res = await createOrder({ productId: item.id, quantity: 1 })
    const d = res?.data || res || {}
    payModalAmount.value = Number(d.amount || item.price || 0).toFixed(2)
    payModalOrderNo.value = d.orderNo || d.id || ''
    payModalOrderId.value = d.id || null
    payModalVisible.value = true
    toast.info('订单已创建，请扫码付款后联系商家核销')
  } catch (e) {
    /* 全局拦截器已提示 */
  }
}

function closePayModal() {
  payModalVisible.value = false
}

function goPayPage() {
  const id = payModalOrderId.value
  closePayModal()
  if (id) router.push({ path: '/pay', query: { orderId: id } })
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen px-4 md:px-6 py-6">
    <div class="mx-auto max-w-6xl">
      <!-- Back -->
      <button class="text-gray-400 hover:text-white mb-6 flex items-center gap-1.5 text-xs transition-colors group" @click="$router.back()">
        <svg class="w-4 h-4 group-hover:-translate-x-0.5 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
        返回商城
      </button>

      <div v-if="product" class="space-y-8">
        <!-- Main Product Area -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <!-- Left: 3D Viewer / Image -->
          <div class="space-y-4">
            <!-- 3D Viewer -->
            <div class="glass-panel overflow-hidden relative">
              <div class="aspect-square bg-gradient-to-br from-web3-primary/10 to-web3-accent/10 relative">
                <!-- Model Viewer -->
                <model-viewer
                  v-if="modelUrl"
                  :src="modelUrl"
                  :alt="product.name"
                  camera-controls
                  auto-rotate
                  shadow-intensity="1"
                  shadow-softness="1"
                  exposure="1"
                  environment-image="neutral"
                  camera-orbit="45deg 55deg 2m"
                  min-camera-orbit="auto auto 0.5m"
                  max-camera-orbit="Infinity Infinity 10m"
                  interaction-prompt="none"
                  style="width: 100%; height: 100%; background: transparent;"
                  @load="modelLoaded = true"
                ></model-viewer>

                <!-- Fallback: product cover image -->
                <div v-else-if="product.cover" class="w-full h-full">
                  <img :src="product.cover" :alt="product.name" class="w-full h-full object-cover" />
                </div>

                <!-- Fallback: 3D interactive cube when no model or cover -->
                <div v-else class="w-full h-full flex flex-col items-center justify-center relative" @mousedown="startDrag" @mousemove="onDrag" @mouseup="stopDrag" @mouseleave="stopDrag">
                  <div class="scene" :style="{ transform: `rotateX(${rotateX}deg) rotateY(${rotateY}deg)` }">
                    <div class="cube">
                      <div class="cube-face front">
                        <span class="text-5xl">🛍️</span>
                        <span class="text-[10px] text-white/60 mt-2">{{ product.name }}</span>
                      </div>
                      <div class="cube-face back">
                        <span class="text-4xl">📦</span>
                        <span class="text-[10px] text-white/60 mt-2">库存 {{ product.stock }}</span>
                      </div>
                      <div class="cube-face right">
                        <span class="text-4xl">💰</span>
                        <span class="text-[10px] text-white/60 mt-2">¥{{ product.price }}</span>
                      </div>
                      <div class="cube-face left">
                        <span class="text-4xl">⭐</span>
                        <span class="text-[10px] text-white/60 mt-2">精选好物</span>
                      </div>
                      <div class="cube-face top">
                        <span class="text-4xl">🎁</span>
                        <span class="text-[10px] text-white/60 mt-2">品质保证</span>
                      </div>
                      <div class="cube-face bottom">
                        <span class="text-4xl">✨</span>
                        <span class="text-[10px] text-white/60 mt-2">限时特惠</span>
                      </div>
                    </div>
                  </div>
                  <p class="text-[10px] text-gray-600 mt-6 matrix-text">拖拽旋转 · 360° 全景预览</p>
                </div>

                <!-- 3D Controls overlay -->
                <div v-if="modelUrl && modelLoaded" class="absolute bottom-3 left-3 flex gap-1.5">
                  <button @click="resetCamera" class="px-2.5 py-1 rounded-lg bg-black/50 backdrop-blur-sm text-[10px] text-white/70 border border-white/10 hover:bg-black/70 transition">
                    重置视角
                  </button>
                  <button @click="toggleAutoRotate" class="px-2.5 py-1 rounded-lg bg-black/50 backdrop-blur-sm text-[10px] text-white/70 border border-white/10 hover:bg-black/70 transition">
                    {{ autoRotate ? '停止旋转' : '自动旋转' }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Image thumbnails (if available) -->
            <div v-if="productImages.length > 1" class="flex gap-2 overflow-x-auto pb-1">
              <button v-for="(img, i) in productImages" :key="i"
                @click="activeImage = i"
                :class="['flex-shrink-0 w-16 h-16 rounded-lg border-2 overflow-hidden transition-all',
                  activeImage === i ? 'border-cyan-400/60' : 'border-white/10 hover:border-white/20']">
                <img :src="img" class="w-full h-full object-cover" />
              </button>
            </div>
          </div>

          <!-- Right: Product Info -->
          <div class="space-y-6">
            <!-- Category & Title -->
            <div>
              <span class="text-xs text-cyan-400 border border-cyan-400/30 rounded px-2.5 py-1">{{ product.category || '未分类' }}</span>
              <h1 class="text-2xl md:text-3xl font-black text-white mt-3 leading-tight">{{ product.name }}</h1>
              <div class="holo-bar mt-4 w-32"></div>
            </div>

            <!-- Price -->
            <div class="glass-panel-sm p-4 flex items-baseline gap-3">
              <span class="text-3xl font-black text-web3-accent">¥{{ product.price }}</span>
              <span class="text-sm text-gray-500 line-through" v-if="product.originalPrice">¥{{ product.originalPrice }}</span>
              <span class="text-[10px] px-2 py-0.5 rounded bg-red-500/15 text-red-400 border border-red-400/20" v-if="product.originalPrice">
                省 ¥{{ (product.originalPrice - product.price).toFixed(0) }}
              </span>
            </div>

            <!-- Description -->
            <div class="glass-panel-sm p-4">
              <h3 class="text-xs font-bold text-gray-300 mb-2 flex items-center gap-1.5">
                <svg class="w-3.5 h-3.5 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                商品介绍
              </h3>
              <p class="text-sm text-gray-400 leading-relaxed">{{ product.description || '暂无详细介绍' }}</p>
            </div>

            <!-- Specs -->
            <div class="glass-panel-sm p-4" v-if="productSpecs.length">
              <h3 class="text-xs font-bold text-gray-300 mb-3 flex items-center gap-1.5">
                <svg class="w-3.5 h-3.5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
                规格参数
              </h3>
              <div class="grid grid-cols-2 gap-2">
                <div v-for="spec in productSpecs" :key="spec.label" class="flex items-center gap-2 py-1.5 px-2.5 rounded-lg bg-[#0c0c22]">
                  <span class="text-[10px] text-gray-500 w-16 flex-shrink-0">{{ spec.label }}</span>
                  <span class="text-xs text-gray-300">{{ spec.value }}</span>
                </div>
              </div>
            </div>

            <!-- Stock & Quantity -->
            <div class="flex items-center gap-6">
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-500">数量</span>
                <div class="flex items-center gap-1 border border-white/10 rounded-lg overflow-hidden">
                  <button class="w-9 h-9 flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/5 transition" @click="quantity = Math.max(1, quantity - 1)">−</button>
                  <span class="w-12 text-center text-sm text-white font-medium">{{ quantity }}</span>
                  <button class="w-9 h-9 flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/5 transition" @click="quantity++">+</button>
                </div>
              </div>
              <span class="text-[11px] text-gray-600">库存 {{ product.stock }} 件</span>
            </div>

            <!-- Action Buttons -->
            <div class="flex gap-3 pt-2">
              <button class="flex-1 py-3.5 rounded-xl font-medium text-sm transition-all duration-300 bg-gradient-to-r from-purple-500 to-cyan-500 text-white hover:shadow-[0_0_30px_rgba(168,85,247,0.3)] active:scale-[0.98]" @click="addToCart">
                加入购物车
              </button>
              <button class="flex-1 py-3.5 rounded-xl font-medium text-sm border border-cyan-400/50 text-cyan-400 hover:bg-cyan-400/10 transition-all active:scale-[0.98]" @click="buyNow">
                立即购买
              </button>
            </div>

            <!-- Features -->
            <div class="grid grid-cols-3 gap-3 pt-2">
              <div class="text-center py-3 rounded-xl bg-[#0c0c22] border border-white/[0.04]">
                <div class="text-lg mb-1">🔒</div>
                <span class="text-[10px] text-gray-500">安全交易</span>
              </div>
              <div class="text-center py-3 rounded-xl bg-[#0c0c22] border border-white/[0.04]">
                <div class="text-lg mb-1">⚡</div>
                <span class="text-[10px] text-gray-500">即时发货</span>
              </div>
              <div class="text-center py-3 rounded-xl bg-[#0c0c22] border border-white/[0.04]">
                <div class="text-lg mb-1">🛡️</div>
                <span class="text-[10px] text-gray-500">售后保障</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Detailed Description Section -->
        <div class="glass-panel p-6 md:p-8">
          <h3 class="text-lg font-bold text-white mb-6 flex items-center gap-2">
            <svg class="w-5 h-5 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
            详细介绍
          </h3>
          <div class="prose-content text-gray-300 text-sm leading-relaxed whitespace-pre-wrap">{{ detailedDescription }}</div>
        </div>

        <!-- Reviews / Comments -->
        <div class="glass-panel p-6 md:p-8">
          <h3 class="text-lg font-bold text-white mb-6 flex items-center gap-2">
            <svg class="w-5 h-5 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"/></svg>
            用户评价
          </h3>
          <div v-if="reviews.length" class="space-y-4">
            <div v-for="review in reviews" :key="review.id" class="py-4 border-b border-white/[0.04] last:border-0">
              <div class="flex items-center gap-2 mb-2">
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500/20 to-cyan-500/20 flex items-center justify-center text-xs font-bold text-white/70">
                  {{ (review.authorName || 'U')[0] }}
                </div>
                <span class="text-sm text-white">{{ review.authorName || '匿名' }}</span>
                <div class="flex items-center gap-0.5 ml-1">
                  <svg v-for="s in 5" :key="s" class="w-3 h-3" :class="s <= review.rating ? 'text-amber-400 fill-amber-400' : 'text-gray-700'" viewBox="0 0 24 24"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                </div>
                <span class="text-[10px] text-gray-600 ml-auto">{{ formatDate(review.createdAt) }}</span>
              </div>
              <p class="text-sm text-gray-400 leading-relaxed pl-10">{{ review.content }}</p>
            </div>
          </div>
          <div v-else class="py-10 text-center">
            <div class="text-3xl mb-3 opacity-15">⭐</div>
            <p class="text-xs text-gray-600">暂无评价，成为第一个评价的人吧</p>
          </div>
        </div>
      </div>

      <div v-else class="py-20 text-center"><Loading /></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, createOrder } from '@/api/shop'
import { useCartStore } from '@/stores/modules/cart'
import { useToastStore } from '@/stores/modules/toast'
import Loading from '@/components/common/Loading.vue'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const toast = useToastStore()
const product = ref(null)
const quantity = ref(1)
const activeImage = ref(0)
const modelLoaded = ref(false)
const autoRotate = ref(true)

// 3D cube rotation
const rotateX = ref(-15)
const rotateY = ref(25)
const isDragging = ref(false)
let lastX = 0, lastY = 0

// Model viewer ref
const modelUrl = computed(() => product.value?.modelUrl || product.value?.model3dUrl || '')
const productImages = computed(() => {
  const images = []
  if (product.value?.cover) images.push(product.value.cover)
  if (product.value?.coverImage) images.push(product.value.coverImage)
  if (product.value?.imageUrls) {
    try {
      const urls = typeof product.value.imageUrls === 'string' ? JSON.parse(product.value.imageUrls) : product.value.imageUrls
      if (Array.isArray(urls)) images.push(...urls)
    } catch {}
  }
  return images
})

const productSpecs = computed(() => {
  if (!product.value) return []
  const specs = []
  if (product.value.category) specs.push({ label: '分类', value: product.value.category })
  if (product.value.stock !== undefined && product.value.stock !== null) specs.push({ label: '库存', value: product.value.stock + ' 件' })
  if (product.value.sales !== undefined && product.value.sales !== null) specs.push({ label: '销量', value: product.value.sales + ' 件' })
  if (product.value.status) specs.push({ label: '状态', value: product.value.status === 'active' ? '在售' : product.value.status })
  return specs
})

const detailedDescription = computed(() => {
  if (!product.value) return ''
  return product.value.detailDescription || product.value.description || '暂无详细介绍'
})

const reviews = ref([])

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function startDrag(e) {
  isDragging.value = true
  lastX = e.clientX
  lastY = e.clientY
}

function onDrag(e) {
  if (!isDragging.value) return
  const dx = e.clientX - lastX
  const dy = e.clientY - lastY
  rotateY.value += dx * 0.5
  rotateX.value -= dy * 0.5
  rotateX.value = Math.max(-60, Math.min(60, rotateX.value))
  lastX = e.clientX
  lastY = e.clientY
}

function stopDrag() {
  isDragging.value = false
}

function resetCamera() {
  rotateX.value = -15
  rotateY.value = 25
}

function toggleAutoRotate() {
  autoRotate.value = !autoRotate.value
}

async function addToCart() {
  if (!product.value) return
  cart.add(product.value, quantity.value)
  toast.success('已加入购物车')
}

async function buyNow() {
  try {
    await createOrder({ productId: product.value.id, quantity: quantity.value })
    toast.success('下单成功!')
    router.push('/pay/success')
  } catch { toast.error('请先登录') }
}

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    const res = await getProductDetail(id)
    product.value = res.data || res
  } catch { product.value = null }
})
</script>

<style scoped>
/* 3D Cube styles */
.scene {
  width: 160px;
  height: 160px;
  perspective: 600px;
  transform-style: preserve-3d;
}
.cube {
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  transition: transform 0.1s ease-out;
}
.cube-face {
  position: absolute;
  width: 160px;
  height: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(12,12,45,0.9), rgba(20,20,60,0.8));
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  backface-visibility: visible;
}
.cube-face.front  { transform: translateZ(80px); }
.cube-face.back   { transform: rotateY(180deg) translateZ(80px); }
.cube-face.right  { transform: rotateY(90deg) translateZ(80px); }
.cube-face.left   { transform: rotateY(-90deg) translateZ(80px); }
.cube-face.top    { transform: rotateX(90deg) translateZ(80px); }
.cube-face.bottom { transform: rotateX(-90deg) translateZ(80px); }
</style>

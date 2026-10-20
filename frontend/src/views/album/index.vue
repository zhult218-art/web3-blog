<template>
  <div class="album-page relative min-h-screen px-4 md:px-6 py-10 overflow-hidden">
    <!-- 漂浮粒子背景 -->
    <canvas ref="fxCanvas" class="fx-canvas"></canvas>
    <!-- 点赞爱心粒子层 -->
    <div ref="heartLayer" class="heart-layer"></div>

    <div class="relative z-10 mx-auto max-w-6xl">
      <!-- 页头：统一 PageHeader（面包屑 + 渐变标题 + 视图切换/上传） -->
      <PageHeader
        title="相册集"
        :subtitle="total ? `Album · 记录瞬间 · 共 ${total} 张` : 'Album · 记录瞬间'"
        :crumbs="[{ label: '首页', to: '/' }, { label: '相册' }]"
      >
        <template #actions>
          <div class="flex flex-wrap items-center gap-2.5">
            <!-- 视图切换：统一 web3-pill -->
            <button v-for="m in VIEW_MODES" :key="m.key"
              class="web3-pill" :class="{ 'web3-pill-active': viewMode === m.key }"
              :title="m.label" @click="viewMode = m.key">
              {{ m.icon }} {{ m.label }}
            </button>
            <!-- 有 upload:photo 权限（管理员恒可）：压缩开关 + 上传 -->
            <template v-if="canUploadPhoto">
              <label class="flex items-center gap-1.5 text-[11px] text-gray-500 cursor-pointer select-none" title="上传时自动压缩图片体积">
                <input v-model="compressOn" type="checkbox" class="accent-cyan-400 w-3.5 h-3.5" @change="saveCompressPref" />
                压缩优化
              </label>
              <button class="web3-btn text-xs !px-4 !py-1.5" :disabled="uploading" @click="fileInput?.click()">
                上传照片
              </button>
            </template>
          </div>
        </template>
      </PageHeader>

      <!-- 隐藏选图入口 -->
      <input ref="fileInput" type="file" accept="image/*" multiple class="hidden" @change="onFilesChange" />

      <!-- 上传进度 / 加载态 -->
      <div v-if="uploading" class="glass-panel-sm p-4 mb-6">
        <div class="flex items-center gap-3 mb-2">
          <Loading size="sm" />
          <span class="text-xs text-gray-400">正在上传第 {{ uploadDone + 1 }} / {{ uploadTotal }} 张照片…</span>
        </div>
        <div class="h-1 rounded-full bg-white/5 overflow-hidden">
          <div class="h-full bg-gradient-to-r from-cyan-400 to-purple-400 transition-all" :style="{ width: uploadPercent + '%' }" />
        </div>
      </div>

      <!-- 照片墙：三视图（key 切换时重放入场动画） -->
      <div v-if="photos.length" :key="viewMode">

        <!-- 🪟 瀑布流：大小图标交错 -->
        <div v-if="viewMode === 'masonry'" class="columns-1 sm:columns-2 lg:columns-3 gap-4">
          <div v-for="(photo, i) in photos" :key="photo.id" v-tilt="7"
            class="photo-card break-inside-avoid mb-4 glass-panel-sm overflow-hidden group cursor-pointer"
            :style="{ animationDelay: (i % 12) * 55 + 'ms' }"
            @click="openPreview(i)">
            <div v-if="brokenIds.includes(photo.id)" class="w-full aspect-[4/3] bg-gradient-to-br" :class="gradientOf(photo)" />
            <img v-else :src="photo.url" :alt="photo.title" loading="lazy"
              class="w-full block group-hover:scale-[1.03] transition duration-500" @error="brokenIds.push(photo.id)" />
            <div class="p-3 flex items-center justify-between gap-2">
              <div class="min-w-0">
                <p class="text-sm text-white truncate group-hover:text-cyan-300 transition">{{ photo.title }}</p>
                <p class="text-[10px] text-gray-500 mt-1">{{ formatDate(photo.createdAt) }} · {{ photo.uploader || '匿名' }}</p>
              </div>
              <div class="flex items-center gap-1 flex-shrink-0">
                <button class="p-1.5 rounded-full hover:bg-white/5 transition disabled:cursor-default"
                  :class="isLiked(photo) ? 'text-pink-400' : 'text-gray-500 hover:text-pink-300'"
                  :disabled="isLiked(photo)" :title="isLiked(photo) ? '已点赞' : '点赞'" @click.stop="like(photo, $event)">
                  <svg viewBox="0 0 24 24" class="w-4 h-4" :fill="isLiked(photo) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                </button>
                <span :key="photo.likes" class="like-pop text-xs" :class="isLiked(photo) ? 'text-pink-400' : 'text-gray-500'">{{ photo.likes || 0 }}</span>
                <button v-if="isAdmin" title="编辑信息" class="p-1.5 rounded-full text-gray-600 hover:text-cyan-300 hover:bg-white/5 transition opacity-0 group-hover:opacity-100" @click.stop="openEdit(photo)">
                  <svg viewBox="0 0 24 24" class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </button>
                <button v-if="isAdmin" title="删除照片"
                  class="p-1.5 rounded-full text-gray-600 hover:text-red-400 hover:bg-white/5 transition opacity-0 group-hover:opacity-100"
                  @click.stop="removePhoto(photo)">
                  <svg viewBox="0 0 24 24" class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- ▦ 网格：整齐统一存放 -->
        <div v-else-if="viewMode === 'grid'" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
          <div v-for="(photo, i) in photos" :key="photo.id" v-tilt="7"
            class="photo-card relative rounded-xl overflow-hidden group cursor-pointer aspect-square border border-white/[0.06] bg-[#0e0e26]"
            :style="{ animationDelay: (i % 12) * 45 + 'ms' }"
            @click="openPreview(i)">
            <div v-if="brokenIds.includes(photo.id)" class="w-full h-full bg-gradient-to-br" :class="gradientOf(photo)" />
            <img v-else :src="photo.url" :alt="photo.title" loading="lazy"
              class="w-full h-full object-cover group-hover:scale-110 transition duration-700" @error="brokenIds.push(photo.id)" />
            <!-- 管理员操作 -->
            <div v-if="isAdmin" class="absolute top-2 right-2 flex gap-1 opacity-0 group-hover:opacity-100 transition">
              <button title="编辑信息" class="w-7 h-7 rounded-lg bg-black/60 text-gray-300 hover:text-cyan-300 flex items-center justify-center text-xs" @click.stop="openEdit(photo)">✎</button>
              <button title="删除照片" class="w-7 h-7 rounded-lg bg-black/60 text-gray-300 hover:text-red-400 flex items-center justify-center text-xs" @click.stop="removePhoto(photo)">🗑</button>
            </div>
            <!-- 悬浮信息 -->
            <div class="absolute inset-x-0 bottom-0 p-2.5 pt-8 bg-gradient-to-t from-black/85 via-black/35 to-transparent translate-y-2 opacity-0 group-hover:translate-y-0 group-hover:opacity-100 transition duration-300">
              <p class="text-xs text-white truncate">{{ photo.title }}</p>
              <div class="flex items-center justify-between mt-1">
                <span class="text-[10px] text-gray-400">{{ formatDate(photo.createdAt) }}</span>
                <span class="text-[10px] text-pink-300">♥ {{ photo.likes || 0 }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- ☰ 列表：紧凑行式浏览 -->
        <div v-else class="space-y-3">
          <div v-for="(photo, i) in photos" :key="photo.id"
            class="photo-card flex items-center gap-4 glass-panel-sm p-3 group cursor-pointer"
            :style="{ animationDelay: (i % 12) * 40 + 'ms' }"
            @click="openPreview(i)">
            <div v-if="brokenIds.includes(photo.id)" class="w-28 h-20 rounded-lg flex-shrink-0 bg-gradient-to-br" :class="gradientOf(photo)" />
            <img v-else :src="photo.url" :alt="photo.title" loading="lazy"
              class="w-28 h-20 rounded-lg object-cover flex-shrink-0" @error="brokenIds.push(photo.id)" />
            <div class="flex-1 min-w-0">
              <p class="text-sm text-white truncate group-hover:text-cyan-300 transition">{{ photo.title }}</p>
              <p class="text-xs text-gray-500 mt-1 truncate">{{ photo.description || '暂无描述' }}</p>
              <p class="text-[10px] text-gray-600 mt-1">{{ formatDate(photo.createdAt) }} · {{ photo.uploader || '匿名' }}</p>
            </div>
            <div class="flex items-center gap-1 flex-shrink-0">
              <button class="p-1.5 rounded-full hover:bg-white/5 transition disabled:cursor-default"
                :class="isLiked(photo) ? 'text-pink-400' : 'text-gray-500 hover:text-pink-300'"
                :disabled="isLiked(photo)" :title="isLiked(photo) ? '已点赞' : '点赞'" @click.stop="like(photo, $event)">
                <svg viewBox="0 0 24 24" class="w-4 h-4" :fill="isLiked(photo) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
              </button>
              <span :key="photo.likes" class="like-pop text-xs" :class="isLiked(photo) ? 'text-pink-400' : 'text-gray-500'">{{ photo.likes || 0 }}</span>
              <button v-if="isAdmin" title="编辑信息" class="p-1.5 rounded-full text-gray-600 hover:text-cyan-300 hover:bg-white/5 transition" @click.stop="openEdit(photo)">✎</button>
              <button v-if="isAdmin" title="删除照片" class="p-1.5 rounded-full text-gray-600 hover:text-red-400 hover:bg-white/5 transition" @click.stop="removePhoto(photo)">🗑</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-else-if="!loaded" class="py-16"><Loading text="照片加载中…" /></div>

      <!-- 空态（含接口失败兜底）：统一 EmptyState 组件 -->
      <EmptyState
        v-else
        text="暂无照片，快来记录第一个瞬间吧"
        d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
      >
        <template v-if="canUploadPhoto" #action>
          <button class="web3-btn text-xs !px-5 !py-2.5" :disabled="uploading" @click="fileInput?.click()">
            上传第一张照片
          </button>
        </template>
      </EmptyState>

      <!-- 加载更多 -->
      <div v-if="photos.length && photos.length < total" class="mt-4 text-center">
        <button class="web3-btn text-xs !px-5" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
      </div>
    </div>

    <!-- 大图查看器 -->
    <PhotoLightbox v-if="showPreview" :photos="photos" :start-index="previewIndex"
      :liked-ids="likedIds" :is-admin="isAdmin"
      @close="showPreview = false" @like="like" @edit="editFromLightbox" />

    <!-- 编辑照片信息弹窗（管理员） -->
    <Modal v-model="showEdit" title="编辑照片信息">
      <div class="space-y-4">
        <div>
          <label class="text-xs text-gray-400 mb-1 block">标题</label>
          <input v-model="editForm.title" class="web3-input text-sm" placeholder="照片标题" @keyup.enter="saveEdit" />
        </div>
        <div>
          <label class="text-xs text-gray-400 mb-1 block">描述</label>
          <textarea v-model="editForm.description" class="web3-input text-sm !min-h-[70px] resize-none" placeholder="描述一下这张照片…" />
        </div>
        <div class="flex justify-end gap-2">
          <button class="text-xs text-gray-400 hover:text-white px-4" @click="showEdit = false">取消</button>
          <button class="web3-btn text-xs !px-5" :disabled="savingEdit" @click="saveEdit">
            {{ savingEdit ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>

    <!-- 上传信息表单弹窗 -->
    <Modal v-model="showUploadForm" title="完善照片信息">
      <div class="space-y-4">
        <p class="text-xs text-gray-500">已成功上传 {{ pendingItems.length }} 张照片，填写标题与描述后发布。</p>
        <div>
          <label class="text-xs text-gray-400 mb-1 block">标题 <span class="text-gray-600">（多张时自动编号，留空使用文件名）</span></label>
          <input v-model="form.title" class="web3-input text-sm" placeholder="照片标题" @keyup.enter="submitUploadForm" />
        </div>
        <div>
          <label class="text-xs text-gray-400 mb-1 block">描述</label>
          <textarea v-model="form.description" class="web3-input text-sm !min-h-[70px] resize-none" placeholder="描述一下这张照片…" />
        </div>
        <div class="flex justify-end gap-2">
          <button class="text-xs text-gray-400 hover:text-white px-4" @click="showUploadForm = false">取消</button>
          <button class="web3-btn text-xs !px-5" :disabled="publishing" @click="submitUploadForm">
            {{ publishing ? '发布中…' : '发布' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
// ====================================================
// 相册页：三视图（瀑布流/网格/列表）· 大图查看器（缩放/编辑/导出）
// 上传压缩优化 · 点赞爱心粒子 · 漂浮粒子背景 · 卡片 3D 倾斜
// ====================================================
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { getPhotoList, createPhoto, uploadPhoto, likePhoto, updatePhoto, deletePhoto } from '@/api/album'
import { useAuthStore } from '@/stores/modules/auth'
import { hasPerm } from '@/utils/permissions'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm } from '@/composables/useDialog'
import { formatDayCN, formatDayCN as formatDate } from '@/utils/date'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import PhotoLightbox from '@/components/album/PhotoLightbox.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const auth = useAuthStore()
const toast = useToastStore()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
// 上传照片入口：管理员恒可；普通用户需被授予 upload:photo 权限（用户管理后台配置）
const canUploadPhoto = computed(() => hasPerm(auth.user, 'upload:photo'))

const PAGE_SIZE = 60
const photos = ref([])
const page = ref(1)
const total = ref(0)
const loaded = ref(false)
const loadingMore = ref(false)

// 破图兜底：记录加载失败的图片 id，渲染时降级为渐变色块
const brokenIds = ref([])

// 破图占位渐变色池（按 id 稳定取色）
const GRADIENTS = [
  'from-purple-500/40 to-pink-500/40',
  'from-cyan-500/40 to-blue-500/40',
  'from-green-500/40 to-emerald-500/40',
  'from-blue-500/40 to-indigo-500/40',
  'from-pink-500/40 to-rose-500/40',
  'from-violet-500/40 to-purple-500/40'
]
function gradientOf(photo) { return GRADIENTS[photo.id % GRADIENTS.length] }

// ---------- 视图模式（瀑布流 / 网格 / 列表，本地记忆） ----------
const VIEW_MODES = [
  { key: 'masonry', icon: '🪟', label: '瀑布流' },
  { key: 'grid', icon: '▦', label: '网格' },
  { key: 'list', icon: '☰', label: '列表' }
]
const viewMode = ref(readView())
function readView() {
  const v = localStorage.getItem('album_view')
  return VIEW_MODES.some(m => m.key === v) ? v : 'masonry'
}
watch(viewMode, v => localStorage.setItem('album_view', v))

// ---------- 列表加载 ----------
async function fetchPhotos() {
  try {
    const res = await getPhotoList({ page: page.value, size: PAGE_SIZE })
    const d = res?.data || {}
    const records = d.records || []
    photos.value = page.value === 1 ? records : photos.value.concat(records)
    total.value = d.total || 0
  } catch {
    // 请求拦截器已全局 toast，这里仅回退空态，不崩页
    if (page.value > 1) page.value--
    else { photos.value = []; total.value = 0 }
  } finally {
    loaded.value = true
  }
}

async function loadMore() {
  loadingMore.value = true
  page.value++
  await fetchPhotos()
  loadingMore.value = false
}

// ---------- 点赞（乐观更新 + 爱心粒子爆发） ----------
const LIKED_KEY = 'album_liked'
const likedIds = ref(readLiked())
const heartLayer = ref(null)

function readLiked() {
  try { return JSON.parse(localStorage.getItem(LIKED_KEY)) || [] } catch { return [] }
}
function saveLiked() { localStorage.setItem(LIKED_KEY, JSON.stringify(likedIds.value)) }
function isLiked(photo) { return likedIds.value.includes(photo.id) }

async function like(photo, evt) {
  if (isLiked(photo)) return
  likedIds.value.push(photo.id)
  saveLiked()
  photo.likes = (photo.likes || 0) + 1
  if (evt?.clientX) spawnHearts(evt.clientX, evt.clientY)
  try {
    const res = await likePhoto(photo.id)
    if (typeof res?.data === 'number') photo.likes = res.data
  } catch {
    likedIds.value = likedIds.value.filter(id => id !== photo.id)
    saveLiked()
    photo.likes = Math.max(0, (photo.likes || 1) - 1)
  }
}

// 在点击位置生成爆开的爱心粒子
function spawnHearts(x, y, n = 10) {
  const layer = heartLayer.value
  if (!layer) return
  for (let i = 0; i < n; i++) {
    const s = document.createElement('span')
    s.className = 'album-heart'
    s.textContent = '♥'
    s.style.cssText = `left:${x}px;top:${y}px;font-size:${10 + Math.random() * 12}px;` +
      `--dx:${(Math.random() - 0.5) * 140}px;--dy:${-(40 + Math.random() * 110)}px;` +
      `--rot:${(Math.random() - 0.5) * 120}deg;--c:${['#f472b6', '#fb7185', '#f9a8d4', '#c4b5fd'][i % 4]};`
    layer.appendChild(s)
    s.addEventListener('animationend', () => s.remove(), { once: true })
  }
}

// ---------- 大图查看器 ----------
const showPreview = ref(false)
const previewIndex = ref(-1)

function openPreview(i) { previewIndex.value = i; showPreview.value = true }
function editFromLightbox(photo) {
  showPreview.value = false
  openEdit(photo)
}

// ---------- 编辑照片信息（管理员） ----------
const showEdit = ref(false)
const savingEdit = ref(false)
const editForm = reactive({ id: null, title: '', description: '' })

function openEdit(photo) {
  editForm.id = photo.id
  editForm.title = photo.title || ''
  editForm.description = photo.description || ''
  showEdit.value = true
}

async function saveEdit() {
  if (savingEdit.value) return
  if (!editForm.title.trim()) { toast.warning('标题不能为空'); return }
  savingEdit.value = true
  try {
    const res = await updatePhoto(editForm.id, {
      title: editForm.title.trim(),
      description: editForm.description.trim()
    })
    const updated = res?.data
    const target = photos.value.find(p => p.id === editForm.id)
    if (target && updated) {
      target.title = updated.title
      target.description = updated.description
    }
    showEdit.value = false
    toast.success('照片信息已更新')
  } catch {
    toast.error('保存失败，请重试')
  } finally {
    savingEdit.value = false
  }
}

// ---------- 漂浮粒子背景 ----------
const fxCanvas = ref(null)
let pRaf = 0
let pResize = null
function initParticles() {
  const canvas = fxCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const DPR = Math.min(window.devicePixelRatio || 1, 2)
  let w = 0, h = 0
  const COLORS = ['34,211,238', '168,85,247', '244,114,182', '110,231,183']
  const parts = []
  function resize() {
    w = canvas.clientWidth; h = canvas.clientHeight
    canvas.width = w * DPR; canvas.height = h * DPR
    ctx.setTransform(DPR, 0, 0, DPR, 0, 0)
  }
  resize()
  pResize = resize
  window.addEventListener('resize', pResize)
  for (let i = 0; i < 42; i++) {
    parts.push({
      x: Math.random() * w, y: Math.random() * h,
      r: 0.8 + Math.random() * 2.2,
      vx: (Math.random() - 0.5) * 0.25,
      vy: -(0.08 + Math.random() * 0.3),
      c: COLORS[i % COLORS.length],
      a: 0.12 + Math.random() * 0.3,
      tw: Math.random() * Math.PI * 2
    })
  }
  function tick() {
    ctx.clearRect(0, 0, w, h)
    for (const p of parts) {
      p.x += p.vx; p.y += p.vy; p.tw += 0.02
      if (p.y < -10) { p.y = h + 10; p.x = Math.random() * w }
      if (p.x < -10) p.x = w + 10
      if (p.x > w + 10) p.x = -10
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(${p.c},${p.a * (0.6 + 0.4 * Math.sin(p.tw))})`
      ctx.shadowColor = `rgba(${p.c},0.8)`
      ctx.shadowBlur = 8
      ctx.fill()
    }
    pRaf = requestAnimationFrame(tick)
  }
  tick()
}
onBeforeUnmount(() => {
  cancelAnimationFrame(pRaf)
  if (pResize) window.removeEventListener('resize', pResize)
})

// ---------- 上传（仅管理员）：压缩优化 → 逐张上传 → 填写信息 → 入库 ----------
const fileInput = ref(null)
const uploading = ref(false)
const uploadDone = ref(0)
const uploadTotal = ref(0)
const uploadPercent = ref(0)
const showUploadForm = ref(false)
const publishing = ref(false)
const pendingItems = ref([]) // 已上传待入库：{ url, width, height, filename }
const form = reactive({ title: '', description: '' })

// 压缩开关（本地记忆，默认开启）
const compressOn = ref(localStorage.getItem('album_compress') !== '0')
function saveCompressPref() { localStorage.setItem('album_compress', compressOn.value ? '1' : '0') }

function onFilesChange(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  if (files.length) uploadFiles(files)
}

function loadImageEl(url) {
  return new Promise((resolve, reject) => {
    const im = new Image()
    im.onload = () => resolve(im)
    im.onerror = reject
    im.src = url
  })
}

// 客户端压缩：长边 ≤2560、JPEG 质量 0.85；gif/小图/PNG 未变小则保留原文件
async function compressImage(file) {
  if (!compressOn.value || file.type === 'image/gif' || file.size < 300 * 1024) {
    const s = await readImageSize(file)
    return { file, width: s.width, height: s.height, compressed: false, saved: 0 }
  }
  try {
    const url = URL.createObjectURL(file)
    const img = await loadImageEl(url)
    URL.revokeObjectURL(url)
    const MAX = 2560
    let w = img.naturalWidth, h = img.naturalHeight
    if (Math.max(w, h) > MAX) {
      const k = MAX / Math.max(w, h)
      w = Math.round(w * k); h = Math.round(h * k)
    }
    const cv = document.createElement('canvas')
    cv.width = w; cv.height = h
    cv.getContext('2d').drawImage(img, 0, 0, w, h)
    const keepPng = file.type === 'image/png'
    const type = keepPng ? 'image/png' : 'image/jpeg'
    const blob = await new Promise(r => cv.toBlob(r, type, 0.85))
    if (!blob || blob.size >= file.size) {
      return { file, width: img.naturalWidth, height: img.naturalHeight, compressed: false, saved: 0 }
    }
    const name = file.name.replace(/\.[^.]+$/, keepPng ? '.png' : '.jpg')
    return {
      file: new File([blob], name, { type }),
      width: w, height: h, compressed: true,
      saved: file.size - blob.size
    }
  } catch {
    const s = await readImageSize(file)
    return { file, width: s.width, height: s.height, compressed: false, saved: 0 }
  }
}

async function uploadFiles(files) {
  uploading.value = true
  uploadTotal.value = files.length
  uploadDone.value = 0
  pendingItems.value = []
  let savedBytes = 0
  let compressedCount = 0
  for (const file of files) {
    uploadPercent.value = 0
    try {
      // 客户端压缩后再上传（可关闭）
      const up = await compressImage(file)
      const res = await uploadPhoto(up.file, {
        onUploadProgress: evt => {
          if (evt.total) uploadPercent.value = Math.round((evt.loaded / evt.total) * 100)
        }
      })
      const url = res?.data?.url
      if (!url) throw new Error('missing url')
      pendingItems.value.push({ url, width: up.width, height: up.height, filename: up.file.name })
      if (up.compressed) { compressedCount++; savedBytes += up.saved }
    } catch {
      toast.error(`「${file.name}」上传失败，已跳过`)
    } finally {
      uploadDone.value++
    }
  }
  uploading.value = false
  if (compressedCount) {
    toast.success(`已优化 ${compressedCount} 张照片，节省 ${(savedBytes / 1024).toFixed(0)} KB`)
  }
  if (!pendingItems.value.length) { toast.warning('没有照片上传成功'); return }
  form.title = ''
  form.description = ''
  showUploadForm.value = true
}

// 预读图片自然宽高，供 createPhoto 入库
function readImageSize(file) {
  return new Promise(resolve => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => { resolve({ width: img.naturalWidth, height: img.naturalHeight }); URL.revokeObjectURL(url) }
    img.onerror = () => { resolve({ width: 0, height: 0 }); URL.revokeObjectURL(url) }
    img.src = url
  })
}

// 批量入库，新照片插到列表最前
async function submitUploadForm() {
  if (publishing.value) return
  publishing.value = true
  try {
    const created = []
    for (let i = 0; i < pendingItems.value.length; i++) {
      const item = pendingItems.value[i]
      const base = form.title.trim()
      const title = base ? (pendingItems.value.length > 1 ? `${base} ${i + 1}` : base) : item.filename.replace(/\.[^.]+$/, '')
      const res = await createPhoto({ title, description: form.description.trim(), url: item.url, width: item.width, height: item.height })
      if (res?.data) created.push(res.data)
    }
    photos.value = [...created.reverse(), ...photos.value]
    total.value += created.length
    showUploadForm.value = false
    toast.success(`已发布 ${created.length} 张照片`)
  } catch {
    toast.error('照片入库失败，请重试')
  } finally {
    publishing.value = false
  }
}

// ---------- 删除（仅管理员） ----------
async function removePhoto(photo) {
  if (!(await dlgConfirm(`确定删除照片「${photo.title}」？删除后不可恢复`, { tone: 'danger', okText: '删除照片' }))) return
  try {
    await deletePhoto(photo.id)
    photos.value = photos.value.filter(p => p.id !== photo.id)
    total.value = Math.max(0, total.value - 1)
    toast.success('删除成功')
  } catch {
    toast.error('删除失败')
  }
}

// 日期本地化
onMounted(() => {
  fetchPhotos()
  initParticles()
})
</script>

<style scoped>
/* 漂浮粒子画布铺满页面底层 */
.fx-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
  opacity: 0.75;
}
.heart-layer {
  position: fixed;
  inset: 0;
  z-index: 2500;
  pointer-events: none;
  overflow: hidden;
}

/* 照片卡片：入场动画（3D 倾斜由全局 v-tilt 指令统一提供，--rx/--ry 方案已合并删除） */
.photo-card {
  animation: card-in 0.55s ease backwards;
}
.photo-card:hover {
  box-shadow: 0 14px 38px -14px rgba(34, 211, 238, 0.28);
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(18px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* 点赞数字变化弹跳 */
.like-pop { display: inline-block; animation: like-pop 0.35s ease; }
@keyframes like-pop { 40% { transform: scale(1.4) } 100% { transform: scale(1) } }
</style>

<style>
/* 点赞爱心粒子（JS 动态创建，需全局样式） */
.album-heart {
  position: fixed;
  z-index: 2600;
  pointer-events: none;
  color: var(--c, #f472b6);
  text-shadow: 0 0 8px var(--c, #f472b6);
  animation: album-heart-fly 0.9s ease-out forwards;
}
@keyframes album-heart-fly {
  0% { transform: translate(-50%, -50%) scale(0.6) rotate(0deg); opacity: 1; }
  100% { transform: translate(calc(-50% + var(--dx)), calc(-50% + var(--dy))) rotate(var(--rot)) scale(0.2); opacity: 0; }
}
</style>

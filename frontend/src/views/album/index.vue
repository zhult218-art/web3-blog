<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <!-- 页头 -->
      <div class="flex items-end justify-between mb-8">
        <div>
          <h1 class="text-3xl font-bold text-gradient-cyber mb-2">相册集</h1>
          <p class="text-sm text-gray-500 matrix-text">Album · 记录瞬间</p>
        </div>
        <button v-if="isAdmin" class="web3-btn text-xs !px-4 !py-1.5" :disabled="uploading" @click="fileInput?.click()">
          上传照片
        </button>
      </div>

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

      <!-- 瀑布流照片墙 -->
      <div v-if="photos.length" class="columns-1 sm:columns-2 lg:columns-3 gap-4">
        <div v-for="(photo, i) in photos" :key="photo.id"
          class="break-inside-avoid mb-4 glass-panel-sm overflow-hidden group cursor-pointer hover:border-cyan-400/30 transition"
          @click="openPreview(i)">
          <!-- 图片（加载失败时降级为渐变色块） -->
          <div v-if="brokenIds.includes(photo.id)" class="w-full aspect-[4/3] bg-gradient-to-br" :class="gradientOf(photo)" />
          <img v-else :src="photo.url" :alt="photo.title" loading="lazy"
            class="w-full block group-hover:scale-[1.02] transition duration-300" @error="brokenIds.push(photo.id)" />
          <!-- 卡片信息：标题 / 日期 / 点赞 / 删除 -->
          <div class="p-3 flex items-center justify-between gap-2">
            <div class="min-w-0">
              <p class="text-sm text-white truncate group-hover:text-cyan-300 transition">{{ photo.title }}</p>
              <p class="text-[10px] text-gray-500 mt-1">{{ formatDate(photo.createdAt) }} · {{ photo.uploader || '匿名' }}</p>
            </div>
            <div class="flex items-center gap-1 flex-shrink-0">
              <button class="p-1.5 rounded-full hover:bg-white/5 transition disabled:cursor-default"
                :class="isLiked(photo) ? 'text-pink-400' : 'text-gray-500 hover:text-pink-300'"
                :disabled="isLiked(photo)" :title="isLiked(photo) ? '已点赞' : '点赞'" @click.stop="like(photo)">
                <svg viewBox="0 0 24 24" class="w-4 h-4" :fill="isLiked(photo) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
              </button>
              <span class="text-xs" :class="isLiked(photo) ? 'text-pink-400' : 'text-gray-500'">{{ photo.likes || 0 }}</span>
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

      <!-- 加载中 -->
      <div v-else-if="!loaded" class="py-16"><Loading text="照片加载中…" /></div>

      <!-- 空态（含接口失败兜底） -->
      <div v-else class="py-16 text-center">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-[#0e0e26] border border-white/[0.06] mb-4">
          <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </div>
        <p class="text-sm text-gray-500">暂无照片，快来记录第一个瞬间吧</p>
      </div>

      <!-- 加载更多 -->
      <div v-if="photos.length && photos.length < total" class="mt-4 text-center">
        <button class="web3-btn text-xs !px-5" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
      </div>

      <!-- 大图预览弹窗 -->
      <Modal v-model="showPreview" class="preview-modal" :title="current?.title || '图片预览'">
        <template v-if="current">
          <div class="relative">
            <div v-if="brokenIds.includes(current.id)" class="w-full aspect-video rounded-lg bg-gradient-to-br" :class="gradientOf(current)" />
            <img v-else :src="current.url" :alt="current.title"
              class="w-full max-h-[60vh] object-contain rounded-lg bg-black/40" @error="brokenIds.push(current.id)" />
            <!-- 左右切换按钮 -->
            <button v-if="previewIndex > 0" title="上一张"
              class="absolute left-2 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-black/60 text-white hover:bg-black/85 transition flex items-center justify-center"
              @click.stop="stepPreview(-1)">←</button>
            <button v-if="previewIndex < photos.length - 1" title="下一张"
              class="absolute right-2 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-black/60 text-white hover:bg-black/85 transition flex items-center justify-center"
              @click.stop="stepPreview(1)">→</button>
          </div>
          <div class="mt-3">
            <p class="text-sm text-white">{{ current.title }}</p>
            <p v-if="current.description" class="text-xs text-gray-400 mt-1 leading-relaxed">{{ current.description }}</p>
            <p class="text-[10px] text-gray-500 mt-2">
              上传者：{{ current.uploader || '匿名' }} · {{ formatDate(current.createdAt) }} · {{ previewIndex + 1 }} / {{ photos.length }}
            </p>
          </div>
        </template>
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
  </div>
</template>

<script setup>
// ====================================================
// 相册页：真实照片墙（瀑布流浏览 / 大图预览 / 点赞 / 管理员上传与删除）
// ====================================================
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { getPhotoList, createPhoto, uploadPhoto, likePhoto, deletePhoto } from '@/api/album'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm } from '@/composables/useDialog'
import { formatDayCN } from '@/utils/date'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'

const auth = useAuthStore()
const toast = useToastStore()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')

const PAGE_SIZE = 60
const photos = ref([])
const page = ref(1)
const total = ref(0)
const loaded = ref(false)
const loadingMore = ref(false)

// 破图兜底：记录加载失败的图片 id，渲染时降级为渐变色块
const brokenIds = ref([])

// 破图占位渐变色池（沿用旧版色块思路，按 id 稳定取色）
const GRADIENTS = [
  'from-purple-500/40 to-pink-500/40',
  'from-cyan-500/40 to-blue-500/40',
  'from-green-500/40 to-emerald-500/40',
  'from-blue-500/40 to-indigo-500/40',
  'from-pink-500/40 to-rose-500/40',
  'from-violet-500/40 to-purple-500/40'
]
function gradientOf(photo) { return GRADIENTS[photo.id % GRADIENTS.length] }

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

// ---------- 点赞（乐观更新，同一照片本浏览器仅可赞一次） ----------
const LIKED_KEY = 'album_liked'
const likedIds = ref(readLiked())

function readLiked() {
  try { return JSON.parse(localStorage.getItem(LIKED_KEY)) || [] } catch { return [] }
}
function saveLiked() { localStorage.setItem(LIKED_KEY, JSON.stringify(likedIds.value)) }
function isLiked(photo) { return likedIds.value.includes(photo.id) }

async function like(photo) {
  if (isLiked(photo)) return
  likedIds.value.push(photo.id)
  saveLiked()
  photo.likes = (photo.likes || 0) + 1
  try {
    const res = await likePhoto(photo.id)
    if (typeof res?.data === 'number') photo.likes = res.data
  } catch {
    likedIds.value = likedIds.value.filter(id => id !== photo.id)
    saveLiked()
    photo.likes = Math.max(0, (photo.likes || 1) - 1)
  }
}

// ---------- 大图预览 ----------
const showPreview = ref(false)
const previewIndex = ref(-1)
const current = computed(() => photos.value[previewIndex.value] || null)

function openPreview(i) { previewIndex.value = i; showPreview.value = true }
function stepPreview(delta) {
  const next = previewIndex.value + delta
  if (next >= 0 && next < photos.value.length) previewIndex.value = next
}

// 键盘：←/→ 切换、Esc 关闭；组件卸载时移除监听
function onKeydown(e) {
  if (!showPreview.value) return
  if (e.key === 'ArrowLeft') stepPreview(-1)
  else if (e.key === 'ArrowRight') stepPreview(1)
  else if (e.key === 'Escape') showPreview.value = false
}
window.addEventListener('keydown', onKeydown)
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))

// ---------- 上传（仅管理员）：选图 → 逐张上传 → 填写信息 → 入库插最前 ----------
const fileInput = ref(null)
const uploading = ref(false)
const uploadDone = ref(0)
const uploadTotal = ref(0)
const uploadPercent = ref(0)
const showUploadForm = ref(false)
const publishing = ref(false)
const pendingItems = ref([]) // 已上传待入库：{ url, width, height, filename }
const form = reactive({ title: '', description: '' })

function onFilesChange(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  if (files.length) uploadFiles(files)
}

async function uploadFiles(files) {
  uploading.value = true
  uploadTotal.value = files.length
  uploadDone.value = 0
  pendingItems.value = []
  for (const file of files) {
    uploadPercent.value = 0
    try {
      // FormData 上传：request 封装 baseURL 已是 /api，无需手动设置 Content-Type
      const res = await uploadPhoto(file, {
        onUploadProgress: evt => {
          if (evt.total) uploadPercent.value = Math.round((evt.loaded / evt.total) * 100)
        }
      })
      const url = res?.data?.url
      if (!url) throw new Error('missing url')
      const size = await readImageSize(file)
      pendingItems.value.push({ url, width: size.width, height: size.height, filename: file.name })
    } catch {
      toast.error(`「${file.name}」上传失败，已跳过`)
    } finally {
      uploadDone.value++
    }
  }
  uploading.value = false
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
  if (!(await dlgConfirm(`确定删除照片「${photo.title}」？删除后不可恢复`))) return
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
function formatDate(d) { return formatDayCN(d, '') }

onMounted(fetchPhotos)
</script>

<style scoped>
/* 预览弹窗加宽以展示大图 */
.preview-modal :deep(.glass-panel) {
  max-width: 56rem;
}
</style>

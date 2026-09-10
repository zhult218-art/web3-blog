<template>
  <div class="min-h-screen py-8 px-6">
    <div class="max-w-5xl mx-auto">
      <h1 class="text-2xl font-bold text-white mb-1 motions-reveal" v-reveal>
        <span class="text-gradient-cyber">媒体</span>素材管理
      </h1>
      <p class="text-sm text-gray-500 mb-8">音乐、视频、书籍 · Media Asset Management</p>

      <!-- Tabs -->
      <div class="flex gap-2 mb-6">
        <button v-for="tab in tabs" :key="tab.key" @click="switchTab(tab.key)"
          :class="['px-4 py-2 rounded-xl text-sm font-medium transition-all border',
            activeTab === tab.key ? 'bg-purple-500/15 border-purple-400/25 text-white' : 'border-white/[0.06] text-gray-500 hover:text-gray-300']">
          <span class="mr-1.5">{{ tab.icon }}</span>{{ tab.label }}
        </button>
      </div>

      <!-- Upload / Add Form -->
      <div class="glass-panel p-6 mb-8">
        <h3 class="text-sm font-bold text-white mb-4 flex items-center gap-2">
          <span class="text-base">{{ tabIcon }}</span>
          {{ activeTab === 'music' ? '上传音乐' : activeTab === 'video' ? '上传视频' : '添加书籍' }}
        </h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="text-xs text-gray-400 mb-1 block">名称 <span class="text-red-400">*</span></label>
            <input v-model="form.title" class="web3-input text-sm"
              :placeholder="activeTab === 'music' ? '歌曲名称' : activeTab === 'video' ? '视频标题' : '书名'" />
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">{{ activeTab === 'book' ? '作者' : '艺术家' }}</label>
            <input v-model="form.artist" class="web3-input text-sm"
              :placeholder="activeTab === 'book' ? '作者名称' : '艺术家名称'" />
          </div>
          <div v-if="activeTab === 'book'">
            <label class="text-xs text-gray-400 mb-1 block">朝代</label>
            <input v-model="form.dynasty" class="web3-input text-sm" placeholder="如: 春秋" />
          </div>
          <div v-if="activeTab === 'book'">
            <label class="text-xs text-gray-400 mb-1 block">分类</label>
            <input v-model="form.tags" class="web3-input text-sm" placeholder="如: 经典, 诗文" />
          </div>
          <div v-else>
            <label class="text-xs text-gray-400 mb-1 block">分类标签</label>
            <input v-model="form.tags" class="web3-input text-sm" placeholder="如: 流行, 电子（逗号分隔）" />
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">{{ activeTab === 'book' ? '类别细分' : '封面图片URL' }}</label>
            <input v-model="form.cover" class="web3-input text-sm"
              :placeholder="activeTab === 'book' ? '如: 经部, 子部' : 'https://...'" />
          </div>
          <div class="md:col-span-2">
            <label class="text-xs text-gray-400 mb-1 block">简介</label>
            <textarea v-model="form.description" class="web3-input text-sm !min-h-[60px] resize-none" placeholder="内容简介..."></textarea>
          </div>
          <div v-if="activeTab !== 'book'" class="md:col-span-2">
            <label class="text-xs text-gray-400 mb-1 block">媒体文件 <span class="text-red-400">*</span></label>
            <div class="border-2 border-dashed border-white/10 rounded-2xl p-6 text-center cursor-pointer hover:border-purple-400/40 transition"
              :class="{ 'border-purple-400/50 bg-purple-500/5': isDragging }"
              @dragover.prevent="isDragging = true"
              @dragleave.prevent="isDragging = false"
              @drop.prevent="onDrop"
              @click="$refs.fileInput.click()">
              <div v-if="!selectedFile">
                <span class="text-3xl block mb-2">{{ tabIcon }}</span>
                <p class="text-sm text-gray-400">拖拽文件到此处或<span class="text-purple-400">点击选择</span></p>
                <p class="text-[10px] text-gray-600 mt-1">{{ activeTab === 'music' ? '支持 MP3, WAV, OGG, FLAC' : '支持 MP4, WebM, OGV' }}</p>
              </div>
              <div v-else class="space-y-1">
                <span class="text-2xl block">📄</span>
                <p class="text-sm text-white font-semibold">{{ selectedFile.name }}</p>
                <p class="text-xs text-gray-500">{{ formatSize(selectedFile.size) }}</p>
                <button class="text-xs text-red-400 mt-1" @click.stop="selectedFile = null">移除</button>
              </div>
              <input ref="fileInput" type="file" class="hidden"
                :accept="activeTab === 'music' ? 'audio/*' : 'video/*'"
                @change="onFileChange" />
            </div>
          </div>
        </div>
        <div class="flex items-center justify-between mt-4">
          <span v-if="uploadError" class="text-xs text-red-400">{{ uploadError }}</span>
          <span v-else></span>
          <button class="web3-btn px-8 flex items-center gap-2" :disabled="!canSubmit || uploading" @click="submitUpload">
            <span v-if="uploading" class="inline-block h-3.5 w-3.5 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
            {{ uploading ? '处理中...' : activeTab === 'book' ? '添加书籍' : '上传入库' }}
          </button>
        </div>
      </div>

      <!-- Existing Assets List -->
      <div class="glass-panel p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-bold text-white">
            已上传素材 ({{ filteredList.length }})
          </h3>
          <button class="text-xs text-cyan-400 hover:text-cyan-300" @click="refreshList">
            <span class="inline-flex items-center gap-1">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h5M20 20v-5h-5M5.07 11a7 7 0 0113.07-2M18.93 13a7 7 0 01-13.07 2"/></svg>
              刷新
            </span>
          </button>
        </div>
        <div v-if="listLoading" class="py-8"><Loading text="加载素材中..." /></div>
        <div v-else-if="filteredList.length" class="space-y-2">
          <div v-for="item in filteredList" :key="item.id"
            class="flex items-center gap-4 p-3 rounded-xl border border-white/[0.04] hover:border-white/[0.08] transition">
            <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-500/20 to-cyan-500/20 flex items-center justify-center text-lg flex-shrink-0">
              {{ tabIcon }}
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm text-white truncate">{{ item.title }}</p>
              <p class="text-[11px] text-gray-500">{{ itemSub(item) }}</p>
            </div>
            <button class="text-xs text-cyan-400/80 hover:text-cyan-300 px-2 py-1 whitespace-nowrap" @click="openEdit(item)">编辑</button>
            <button class="text-xs text-red-400/70 hover:text-red-300 px-2 py-1" @click="deleteItem(item.id)">删除</button>
          </div>
        </div>
        <div v-else class="py-12 text-center">
          <span class="text-4xl block mb-3 opacity-15">{{ tabIcon }}</span>
          <p class="text-xs text-gray-600">暂无素材，请{{ activeTab === 'book' ? '添加' : '上传' }}</p>
        </div>
      </div>
    </div>

    <!-- Edit Modal -->
    <Modal v-model="showEdit" :title="editTitle">
      <div class="space-y-4">
        <div>
          <label class="text-xs text-gray-400 block mb-1">名称 <span class="text-red-400">*</span></label>
          <input v-model="editForm.title" class="web3-input text-sm" placeholder="素材名称" />
        </div>
        <div v-if="activeTab === 'music'">
          <label class="text-xs text-gray-400 block mb-1">艺术家</label>
          <input v-model="editForm.artist" class="web3-input text-sm" placeholder="艺术家名称" />
        </div>
        <div v-if="activeTab === 'book'">
          <label class="text-xs text-gray-400 block mb-1">作者</label>
          <input v-model="editForm.artist" class="web3-input text-sm" placeholder="作者名称" />
        </div>
        <div v-if="activeTab === 'book'" class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-xs text-gray-400 block mb-1">朝代</label>
            <input v-model="editForm.dynasty" class="web3-input text-sm" placeholder="如: 春秋" />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">分类</label>
            <input v-model="editForm.tags" class="web3-input text-sm" placeholder="如: 经典" />
          </div>
        </div>
        <div v-if="activeTab !== 'book'">
          <label class="text-xs text-gray-400 block mb-1">资源URL</label>
          <input v-model="editForm.url" class="web3-input text-sm" placeholder="https://... 或 /video/file/xxx" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">{{ activeTab === 'book' ? '类别细分' : '封面URL' }}</label>
          <input v-model="editForm.cover" class="web3-input text-sm" :placeholder="activeTab === 'book' ? '如: 经部' : 'https://...'" />
        </div>
        <div v-if="activeTab !== 'book'">
          <label class="text-xs text-gray-400 block mb-1">分类标签</label>
          <input v-model="editForm.tags" class="web3-input text-sm" placeholder="如: 流行, 电子" />
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button class="web3-btn-outline text-xs !px-5 !py-2" @click="showEdit = false">取消</button>
          <button class="web3-btn text-xs !px-5 !py-2 flex items-center gap-2" :disabled="savingEdit" @click="saveEdit">
            <span v-if="savingEdit" class="inline-block h-3 w-3 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
            {{ savingEdit ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
// ====================================================
// 媒体管理：音乐/视频/图书三类素材的
// 上传（拖拽）、编辑与删除管理
// ====================================================
import { ref, computed, onMounted } from 'vue'
import { useToastStore } from '@/stores/modules/toast'
import { getMusicList, getVideoList, getBookList, createBook, updateBook, deleteBook, uploadMedia, createMusic } from '@/api/media'
import { uploadResource } from '@/api/resources'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import { confirm as dlgConfirm } from '@/composables/useDialog'

const toast = useToastStore()
const activeTab = ref('music')
const isDragging = ref(false)
const uploading = ref(false)
const uploadError = ref('')
const selectedFile = ref(null)
const form = ref({ title: '', artist: '', dynasty: '', tags: '', cover: '', description: '' })
const musicList = ref([])
const videoList = ref([])
const bookList = ref([])
const listLoading = ref(false)
const showEdit = ref(false)
const savingEdit = ref(false)
const editForm = ref({ id: null, title: '', artist: '', dynasty: '', url: '', cover: '', tags: '' })

// 素材类型 tab 配置
const tabs = [
  { key: 'music', label: '音乐管理', icon: '🎵' },
  { key: 'video', label: '视频管理', icon: '🎬' },
  { key: 'book', label: '书籍管理', icon: '📚' }
]

// 当前 tab 对应的素材列表
const filteredList = computed(() => {
  if (activeTab.value === 'music') return musicList.value
  if (activeTab.value === 'video') return videoList.value
  return bookList.value
})

// 当前 tab 图标
const tabIcon = computed(() => tabs.find(t => t.key === activeTab.value)?.icon || '📄')

// 编辑弹窗标题（按当前 tab 区分素材类型）
const editTitle = computed(() => {
  const map = { music: '编辑音乐', video: '编辑视频', book: '编辑书籍' }
  return map[activeTab.value] || '编辑素材'
})

// 上传表单是否满足提交条件
const canSubmit = computed(() => {
  if (activeTab.value === 'book') return form.value.title.trim()
  return form.value.title.trim() && selectedFile.value
})

// 字节数格式化为可读大小
function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

// 生成列表条目的副标题信息
function itemSub(item) {
  if (activeTab.value === 'music') return `${item.artist || '未知'} · ${item.tags || '未分类'}`
  if (activeTab.value === 'video') return `${item.tags || '未分类'} · ${item.duration ? formatDuration(item.duration) : '时长未知'}`
  return `${item.author || '佚名'}${item.dynasty ? ' · ' + item.dynasty : ''} · 共 ${item.chapterCount ?? 0} 章`
}

// 时长格式化为 m:ss
function formatDuration(secs) {
  const n = Number(secs)
  if (!isFinite(n)) return '--:--'
  const m = Math.floor(n / 60)
  const s = Math.floor(n % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

// 选择素材文件并记录
function onFileChange(e) { selectedFile.value = e.target.files?.[0] || null }

// 拖拽释放素材文件到上传区
function onDrop(e) {
  isDragging.value = false
  selectedFile.value = e.dataTransfer?.files?.[0] || null
}

// 提交素材上传：按 tab 类型走不同接口
async function submitUpload() {
  if (!canSubmit.value) return
  uploading.value = true
  uploadError.value = ''
  try {
    if (activeTab.value === 'book') {
      await createBook({
        title: form.value.title,
        author: form.value.artist,
        dynasty: form.value.dynasty,
        category: form.value.tags,
        categorySub: form.value.cover,
        description: form.value.description,
        chapterCount: 0
      })
      toast.success('书籍添加成功')
    } else if (activeTab.value === 'video') {
      const fd = new FormData()
      fd.append('file', selectedFile.value)
      fd.append('title', form.value.title)
      fd.append('description', form.value.description || '')
      fd.append('tags', form.value.tags || '')
      fd.append('cover', form.value.cover || '')
      await uploadMedia(fd)
      toast.success('视频上传入库成功')
    } else {
      const res = await uploadResource(fdForMusic())
      const mediaUrl = res?.data?.downloadUrl || res?.data?.url || res?.data?.path || ''
      await createMusic({
        title: form.value.title,
        artist: form.value.artist,
        url: mediaUrl,
        cover: form.value.cover,
        tags: form.value.tags,
        description: form.value.description
      })
      toast.success('音乐上传入库成功')
    }
    form.value = { title: '', artist: '', dynasty: '', tags: '', cover: '', description: '' }
    selectedFile.value = null
    loadList()
  } catch (e) {
    uploadError.value = '上传失败: ' + (e?.message || '网络错误')
  } finally {
    uploading.value = false
  }
}

// 构造音乐上传所需的 FormData
function fdForMusic() {
  const fd = new FormData()
  fd.append('file', selectedFile.value)
  fd.append('category', '音频')
  fd.append('description', [form.value.artist, form.value.tags, form.value.description].filter(Boolean).join(' | '))
  return fd
}

// 按当前 tab 加载对应类型的素材列表
function loadList() {
  listLoading.value = true
  const p = activeTab.value === 'music'
    ? getMusicList({ page: 1, size: 50 }).then(res => {
        const data = res.data?.records || res.data || []
        musicList.value = data.map(t => ({ ...t, id: t.id || t.musicId, tags: t.tags || t.category || '未分类' }))
      })
    : activeTab.value === 'video'
      ? getVideoList({ page: 1, size: 50 }).then(res => {
          const data = res.data?.records || res.data || []
          videoList.value = data.map(t => ({ ...t, id: t.id || t.videoId, tags: t.tags || t.category || '未分类' }))
        })
      : getBookList({ page: 1, size: 50 }).then(res => {
          const data = res.data?.records || res.data || []
          bookList.value = data.map(t => ({ ...t, id: t.id || t.bookId, tags: t.tags || t.category || '未分类' }))
        })
  p.catch(() => {}).finally(() => { listLoading.value = false })
}

// 刷新素材列表
function refreshList() { loadList() }

// 切换素材类型 tab 并加载对应列表
function switchTab(key) {
  if (activeTab.value === key) return
  activeTab.value = key
  selectedFile.value = null
  uploadError.value = ''
  openEditClose()
  loadList()
}

// 关闭编辑弹窗
function openEditClose() { showEdit.value = false }

// 打开编辑弹窗并回填素材数据
function openEdit(item) {
  editForm.value = {
    id: item.id,
    title: item.title || '',
    artist: item.artist || item.author || '',
    dynasty: item.dynasty || '',
    url: item.url || '',
    cover: item.cover || item.categorySub || '',
    tags: (item.tags || item.category || '').replace(/^未分类$/, '')
  }
  showEdit.value = true
}

// 保存素材编辑内容
async function saveEdit() {
  if (!editForm.value.title.trim()) { toast.warning('名称不能为空'); return }
  savingEdit.value = true
  try {
    const { default: request } = await import('@/api/request')
    if (activeTab.value === 'book') {
      await updateBook(editForm.value.id, {
        title: editForm.value.title,
        author: editForm.value.artist,
        dynasty: editForm.value.dynasty,
        category: editForm.value.tags,
        categorySub: editForm.value.cover
      })
    } else {
      const endpoint = activeTab.value === 'music' ? `/music/${editForm.value.id}` : `/video/${editForm.value.id}`
      await request.put(endpoint, {
        title: editForm.value.title,
        artist: editForm.value.artist,
        url: editForm.value.url,
        cover: editForm.value.cover,
        tags: editForm.value.tags
      })
    }
    toast.success('保存成功')
    showEdit.value = false
    loadList()
  } catch {
    toast.error('保存失败')
  } finally {
    savingEdit.value = false
  }
}

// 删除素材（二次确认后按类型调用删除接口）
async function deleteItem(id) {
  if (!(await dlgConfirm('确定删除该素材？'))) return
  try {
    if (activeTab.value === 'book') {
      await deleteBook(id)
    } else {
      const { default: request } = await import('@/api/request')
      const endpoint = activeTab.value === 'music' ? `/music/${id}` : `/video/${id}`
      await request.delete(endpoint)
    }
    toast.success('删除成功')
    if (activeTab.value !== 'book') loadList()
    else loadList()
  } catch {
    toast.error('删除失败，请稍后重试')
  }
}

// 挂载时加载默认素材列表
onMounted(loadList)
</script>
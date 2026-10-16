<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <PageHeader title="资源中心" subtitle="File Sharing · Downloads · Categories"
        :crumbs="[{ label: '首页', to: '/' }, { label: '资源分享' }]">
        <template #actions>
          <router-link to="/upload" class="web3-btn text-xs !px-4 !py-1.5">上传资源</router-link>
        </template>
      </PageHeader>

      <div class="flex gap-2 mb-6 flex-wrap">
        <button v-for="cat in ['全部','文档','图片','视频','音频','压缩包','其他']" :key="cat" class="web3-pill"
          :class="{ 'web3-pill-active': selectedCat === cat }" @click="setCategory(cat)">{{ cat }}</button>
      </div>

      <!-- Table-like list for resources -->
      <div v-if="list.length" class="space-y-2">
        <div class="hidden md:grid grid-cols-12 gap-4 px-4 py-2 text-[11px] text-gray-600 uppercase tracking-wider">
          <span class="col-span-5">文件名</span>
          <span class="col-span-2">分类</span>
          <span class="col-span-2">大小</span>
          <span class="col-span-2">下载</span>
          <span class="col-span-1"></span>
        </div>
        <div v-for="item in list" :key="item.id"
          @click="openDetail(item)"
          class="glass-panel-sm grid grid-cols-1 md:grid-cols-12 gap-3 md:gap-4 p-4 items-center hover:border-cyan-400/30 transition cursor-pointer group">
          <div class="md:col-span-5 flex items-center gap-3">
            <span class="text-lg flex-shrink-0">{{ fileIcon(item.contentType) }}</span>
            <div class="min-w-0">
              <h4 class="font-semibold text-white text-sm truncate group-hover:text-cyan-300 transition">{{ item.title }}</h4>
              <p class="text-[11px] text-gray-500 truncate">{{ item.originalFilename || item.filename }}</p>
            </div>
          </div>
          <div class="md:col-span-2">
            <span class="text-xs text-gray-400">{{ item.category || '未分类' }}</span>
          </div>
          <div class="md:col-span-2">
            <span class="text-xs text-gray-500">{{ formatSize(item.size) }}</span>
            <p class="text-[10px] text-gray-600">{{ item.downloadCount || 0 }} downloads</p>
          </div>
          <div class="md:col-span-2">
            <span class="text-[10px] text-gray-600">{{ formatDate(item.createdAt) }}</span>
          </div>
          <div class="md:col-span-1 text-right flex items-center gap-2">
            <button v-if="isAdmin" class="text-[11px] text-cyan-400 hover:text-cyan-300" @click.stop="editItem(item)">编辑</button>
            <button v-if="isAdmin" class="text-[11px] text-red-400 hover:text-red-300" @click.stop="deleteItem(item)">删除</button>
            <button class="web3-btn text-[11px] !px-3 !py-1.5" @click.stop="download(item)">
              下载
            </button>
          </div>
        </div>
      </div>
      <div v-else-if="!loaded" class="space-y-2">
        <div v-for="i in 6" :key="i" class="glass-panel-sm flex items-center gap-4 p-4">
          <div class="skeleton h-8 w-8 rounded-lg"></div>
          <div class="flex-1 space-y-2">
            <div class="skeleton h-4 w-1/3 rounded"></div>
            <div class="skeleton h-3 w-1/5 rounded"></div>
          </div>
          <div class="skeleton h-8 w-20 rounded-lg"></div>
        </div>
      </div>
      <EmptyState v-else text="暂无资源"
        d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />

      <div class="mt-6" v-if="total > 0">
        <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="load" />
      </div>

      <!-- Detail Modal -->
      <Modal v-model="showDetail" :title="selectedResource?.title || '资源详情'">
        <div v-if="selectedResource" class="space-y-4">
          <div class="flex items-center gap-3">
            <span class="text-2xl">{{ fileIcon(selectedResource.contentType) }}</span>
            <div>
              <h3 class="text-white font-semibold">{{ selectedResource.title }}</h3>
              <span class="text-xs text-gray-500">{{ selectedResource.category || '未分类' }}</span>
            </div>
          </div>
          <p v-if="selectedResource.description" class="text-sm text-white/70">{{ selectedResource.description }}</p>
          <div class="grid grid-cols-2 gap-3 text-sm">
            <div class="glass-panel rounded-lg p-3">
              <span class="text-white/50 text-xs">文件名</span>
              <p class="text-white/80 truncate text-sm mt-0.5">{{ selectedResource.originalFilename || selectedResource.filename }}</p>
            </div>
            <div class="glass-panel rounded-lg p-3">
              <span class="text-white/50 text-xs">大小</span>
              <p class="text-white/80 text-sm mt-0.5">{{ formatSize(selectedResource.size) }}</p>
            </div>
            <div class="glass-panel rounded-lg p-3">
              <span class="text-white/50 text-xs">下载次数</span>
              <p class="text-white/80 text-sm mt-0.5">{{ selectedResource.downloadCount || 0 }}</p>
            </div>
            <div class="glass-panel rounded-lg p-3">
              <span class="text-white/50 text-xs">上传时间</span>
              <p class="text-white/80 text-sm mt-0.5">{{ formatDate(selectedResource.createdAt) }}</p>
            </div>
          </div>
          <button @click="download(selectedResource)" class="w-full py-2.5 rounded-lg bg-indigo-500/20 hover:bg-indigo-500/30 text-indigo-300 transition-colors text-sm">
            下载资源
          </button>
        </div>
      </Modal>

      <!-- Edit Modal -->
      <Modal v-model="showEditModal" :title="'编辑资源'">
        <div class="space-y-4">
          <div>
            <label class="text-xs text-gray-400 mb-1 block">资源标题 <span class="text-red-400">*</span></label>
            <input v-model="form.title" class="web3-input text-sm" placeholder="资源标题" />
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">分类</label>
            <select v-model="form.category" class="web3-input text-sm">
              <option value="">未分类</option>
              <option>文档</option><option>图片</option><option>视频</option><option>音频</option><option>压缩包</option><option>其他</option>
            </select>
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">描述</label>
            <textarea v-model="form.description" class="web3-input text-sm !min-h-[60px] resize-none" placeholder="描述"></textarea>
          </div>
          <div class="flex justify-end gap-2">
            <button class="text-xs text-gray-400 hover:text-white px-4" @click="showEditModal = false">取消</button>
            <button class="web3-btn text-xs !px-5" :disabled="!form.title.trim()" @click="submitEdit">保存</button>
          </div>
        </div>
      </Modal>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 资源下载页：资源列表 + 分类筛选 + 分页，
// 支持下载、编辑与删除资源
// ====================================================
import { ref, reactive, computed, onMounted } from 'vue'
import { getResourceList, updateResource, deleteResource, recordResourceDownload } from '@/api/resources'
import { useToastStore } from '@/stores/modules/toast'
import { useAuthStore } from '@/stores/modules/auth'
import { downloadFile } from '@/utils/download'
import { formatDayCN } from '@/utils/date'
import { usePagedList } from '@/composables/usePagedList'
import PageHeader from '@/components/common/PageHeader.vue'
import Pagination from '@/components/common/Pagination.vue'
import Modal from '@/components/common/Modal.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { confirm as dlgConfirm } from '@/composables/useDialog'

// 分页列表状态（页码 / 分类 / 加载 / 空状态）统一由 usePagedList 管理
const { list, page, size, total, loaded, selectedCat, load, setCategory, removeWhere } = usePagedList(getResourceList, { pageSize: 15 })
const toast = useToastStore()
const auth = useAuthStore()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const showEditModal = ref(false)
const editingItem = ref(null)
const showDetail = ref(false)
const selectedResource = ref(null)
const form = reactive({ title: '', category: '', description: '' })

// 下载资源：先调用后端计数接口，再触发浏览器下载
async function download(item) {
  if (!item?.downloadUrl) { toast.warning('暂无下载链接'); return }
  try {
    await recordResourceDownload(item.id).catch(() => {})
    item.downloadCount = (item.downloadCount || 0) + 1
    const direct = await downloadFile(item.downloadUrl, item.title)
    if (direct) toast.success('已开始下载')
  } catch {
    toast.error('下载失败，请检查下载链接')
  }
}

// 字节数格式化为 B/KB/MB 可读大小
function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

// 本地化格式化资源日期
function formatDate(d) { return formatDayCN(d, '') }

// 根据文件类型返回对应图标
function fileIcon(type) {
  if (!type) return '📄'
  if (type.includes('image')) return '🖼'
  if (type.includes('video')) return '🎬'
  if (type.includes('audio')) return '🎵'
  if (type.includes('zip') || type.includes('rar') || type.includes('tar')) return '📦'
  if (type.includes('pdf')) return '📕'
  return '📄'
}

// 打开资源详情弹窗
function openDetail(item) { selectedResource.value = item; showDetail.value = true }

// 打开编辑资源弹窗并回填数据
function editItem(item) { editingItem.value = item; Object.assign(form, { title: item.title, category: item.category || '', description: item.description || '' }); showEditModal.value = true }

// 提交资源编辑并刷新列表
async function submitEdit() {
  if (!form.title.trim()) return
  try { await updateResource(editingItem.value.id, { ...form }); toast.success('更新成功'); showEditModal.value = false; load() }
  catch { toast.error('更新失败') }
}

// 删除指定资源并刷新列表
async function deleteItem(item) {
  if (!(await dlgConfirm(`确定删除资源 "${item.title}"?`))) return
  try { await deleteResource(item.id); removeWhere(x => x.id === item.id); toast.success('删除成功') }
  catch { toast.error('删除失败') }
}

onMounted(load)
</script>

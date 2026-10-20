<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto w-full max-w-[1800px] lg:w-[85%]">
      <PageHeader title="软件资源" subtitle="Software Downloads · Cross-Platform · Developer Tools"
        :crumbs="[{ label: '首页', to: '/' }, { label: '软件下载' }]">
        <template #actions>
          <button v-if="isAdmin" class="web3-btn text-xs !px-4 !py-2" @click="openForm">+ 上传软件</button>
        </template>
      </PageHeader>

      <div class="flex gap-2 mb-6 flex-wrap">
        <button v-for="cat in catOptions" :key="cat" class="web3-pill" :class="{ 'web3-pill-active': selectedCat === cat }"
          @click="setCategory(cat)">{{ cat }}</button>
      </div>

      <div v-if="list.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <div v-for="item in list" :key="item.id" v-tilt
          class="glass-panel p-5 group cursor-pointer hover:border-purple-400/30">
          <div class="flex items-start gap-4">
            <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-purple-500/20 to-cyan-500/20 flex items-center justify-center text-2xl flex-shrink-0 group-hover:scale-110 transition-transform duration-300">
              <!-- 命中品牌词典则展示官方图标：主源(品牌色) 404 → 旧版源(单色反白) → 名称首字母 -->
              <img v-if="iconSrc(item)" :src="iconSrc(item)" :alt="item.name"
                :style="iconStage(item.id) === 1 ? 'filter:invert(1)' : ''"
                class="w-8 h-8 object-contain" @error="onIconError(item)" />
              <span v-else>{{ (item.name || '?')[0]?.toUpperCase() }}</span>
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-semibold text-white text-sm group-hover:text-cyan-300 transition">{{ item.name }}</h3>
              <span class="text-[11px] text-gray-500">{{ item.version || 'v1.0.0' }}</span>
            </div>
          </div>
          <p class="mt-3 text-xs text-gray-400 line-clamp-2">{{ item.description }}</p>

          <div class="mt-3 flex items-center gap-2 flex-wrap">
            <span v-if="item.os" class="text-[10px] px-1.5 py-0.5 rounded border border-white/10 text-gray-500">{{ item.os }}</span>
            <span class="text-[10px] px-1.5 py-0.5 rounded border border-white/10 text-gray-500">{{ item.downloadCount || 0 }} downloads</span>
            <span v-if="item.size" class="text-[10px] text-gray-600">{{ formatSize(item.size) }}</span>
          </div>

          <div class="mt-4 flex items-center gap-3">
            <button class="web3-btn text-xs !px-4 !py-1.5" @click.stop="download(item)">
              下载
            </button>
            <button class="web3-btn-outline text-xs !px-4 !py-1.5" @click.stop="showDetail(item)">
              详情
            </button>
            <div v-if="isAdmin" class="ml-auto flex gap-1 opacity-0 group-hover:opacity-100 transition">
              <button class="text-[11px] text-cyan-400" @click.stop="editItem(item)">编辑</button>
              <button class="text-[11px] text-red-400" @click.stop="deleteItem(item)">删除</button>
            </div>
          </div>
        </div>
      </div>
      <div v-else-if="!loaded" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <SkeletonCard v-for="i in 6" :key="i" :has-image="false" :lines="4" />
      </div>
      <EmptyState v-else text="暂无软件" />

      <div class="mt-6" v-if="total > 0">
        <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="load" />
      </div>

      <!-- Create/Edit Form -->
      <Modal v-model="showForm" :title="editingItem ? '编辑软件' : '上传软件'">
        <div v-if="showForm" class="space-y-4">
          <div>
            <label class="text-xs text-gray-400 mb-1 block">软件名称 <span class="text-red-400">*</span></label>
            <input v-model="formData.name" class="web3-input text-sm" placeholder="如: VS Code" />
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="text-xs text-gray-400 mb-1 block">版本</label>
              <input v-model="formData.version" class="web3-input text-sm" placeholder="v1.0.0" />
            </div>
            <div>
              <label class="text-xs text-gray-400 mb-1 block">分类</label>
              <input v-model="formData.category" class="web3-input text-sm" list="software-cat-options" placeholder="输入或选择分类" />
              <datalist id="software-cat-options">
                <option v-for="c in catOptions.slice(1)" :key="c" :value="c" />
              </datalist>
            </div>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="text-xs text-gray-400 mb-1 block">系统</label>
              <input v-model="formData.os" class="web3-input text-sm" placeholder="Windows/Mac/Linux" />
            </div>
            <div>
              <label class="text-xs text-gray-400 mb-1 block">文件大小 (MB)</label>
              <input v-model.number="formData.size" type="number" class="web3-input text-sm" placeholder="0" />
            </div>
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">描述</label>
            <textarea v-model="formData.description" class="web3-input text-sm !min-h-[60px] resize-none" placeholder="软件描述"></textarea>
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">下载链接</label>
            <input v-model="formData.downloadUrl" class="web3-input text-sm" placeholder="https://..." />
          </div>
          <div class="flex justify-end gap-2 pt-2">
            <button class="text-xs text-gray-400 hover:text-white px-4" @click="showForm = false">取消</button>
            <button class="web3-btn text-xs !px-5" :disabled="!formData.name.trim() || submitting" @click="submitForm">
              {{ submitting ? '保存中...' : (editingItem ? '更新' : '上传') }}
            </button>
          </div>
        </div>
      </Modal>

      <Modal v-model="showModal" :title="detailItem?.name">
        <div v-if="detailItem" class="space-y-4">
          <!-- Header with icon and version badge -->
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-500/20 to-cyan-500/20 flex items-center justify-center text-xl flex-shrink-0">
              <img v-if="iconSrc(detailItem)" :src="iconSrc(detailItem)" :alt="detailItem.name"
                :style="iconStage(detailItem.id) === 1 ? 'filter:invert(1)' : ''"
                class="w-7 h-7 object-contain" @error="onIconError(detailItem)" />
              <span v-else>{{ (detailItem.name || '?')[0]?.toUpperCase() }}</span>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <h3 class="text-white font-semibold">{{ detailItem.name }}</h3>
                <span class="text-[10px] px-1.5 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-400/20">{{ detailItem.version || 'v1.0.0' }}</span>
              </div>
              <span class="text-xs text-gray-500">{{ detailItem.category || '工具' }}</span>
            </div>
          </div>

          <p v-if="detailItem.description" class="text-sm text-gray-400">{{ detailItem.description }}</p>

          <!-- Info cards -->
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
            <div class="glass-panel-sm p-3">
              <span class="text-gray-500">版本</span>
              <p class="text-white mt-1">{{ detailItem.version || 'v1.0.0' }}</p>
            </div>
            <div class="glass-panel-sm p-3">
              <span class="text-gray-500">系统</span>
              <p class="text-white mt-1">{{ detailItem.os || '跨平台' }}</p>
            </div>
            <div class="glass-panel-sm p-3">
              <span class="text-gray-500">大小</span>
              <p class="text-white mt-1">{{ formatSize(detailItem.size || 0) }}</p>
            </div>
            <div class="glass-panel-sm p-3">
              <span class="text-gray-500">下载量</span>
              <p class="text-white mt-1">{{ detailItem.downloadCount || 0 }}</p>
            </div>
          </div>

          <!-- Screenshot / Preview placeholder -->
          <div class="glass-panel-sm rounded-lg p-4 flex items-center justify-center border border-dashed border-white/10">
            <div class="text-center">
              <svg class="w-8 h-8 text-gray-600 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
              <p class="text-xs text-gray-500">软件截图预览</p>
            </div>
          </div>

          <!-- Changelog section -->
          <div v-if="detailItem.changelog">
            <h4 class="text-xs text-gray-500 mb-2">更新日志</h4>
            <div class="glass-panel-sm rounded-lg p-3 text-xs text-gray-400 whitespace-pre-line max-h-32 overflow-y-auto">{{ detailItem.changelog }}</div>
          </div>

          <button class="web3-btn w-full block text-center" @click="download(detailItem)">下载文件</button>
        </div>
      </Modal>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 软件中心页：软件列表 + 分类筛选 + 详情弹窗 + 下载，
// 支持管理员增删改软件信息
// ====================================================
import { ref, reactive, computed, onMounted } from 'vue'
import { getSoftwareList, createSoftware, updateSoftware, deleteSoftware, recordSoftwareDownload } from '@/api/software'
import { useToastStore } from '@/stores/modules/toast'
import { useAuthStore } from '@/stores/modules/auth'
import { downloadFile } from '@/utils/download'
import { usePagedList } from '@/composables/usePagedList'
import { useBrandIcon } from '@/composables/useBrandIcon'
import PageHeader from '@/components/common/PageHeader.vue'
import Pagination from '@/components/common/Pagination.vue'
import Modal from '@/components/common/Modal.vue'
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { confirm as dlgConfirm } from '@/composables/useDialog'

// 分页列表状态（页码 / 分类 / 加载 / 空状态）统一由 usePagedList 管理
const { list, page, size, total, loaded, selectedCat, load, setCategory, removeWhere } = usePagedList(getSoftwareList, { pageSize: 9 })
// 分类 Tab 数据驱动：从全量软件提取去重，避免写死分类与实际数据不匹配
const catOptions = ref(['全部'])
const showModal = ref(false); const detailItem = ref(null)
const showForm = ref(false); const editingItem = ref(null); const submitting = ref(false)
const formData = reactive({ name: '', version: 'v1.0.0', category: '', os: '', size: 0, description: '', downloadUrl: '' })
const toast = useToastStore()
const auth = useAuthStore()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
// 品牌图标两级降级：0=主源(simpleicons 品牌色) 1=旧版源(jsdelivr 单色) 2=首字母回退
const { stage: iconStage, src: iconSrc, onError: onIconError } = useBrandIcon()

// 下载软件：先调用后端计数接口，再触发浏览器下载
async function download(item) {
  if (!item?.downloadUrl) { toast.warning('暂无下载链接'); return }
  try {
    await recordSoftwareDownload(item.id).catch(() => {})
    item.downloadCount = (item.downloadCount || 0) + 1
    const direct = await downloadFile(item.downloadUrl, item.name)
    if (direct) toast.success('已开始下载')
  } catch {
    toast.error('下载失败，请检查下载链接')
  }
}

// 字节数格式化为 KB/MB 可读大小
function formatSize(bytes) {
  if (!bytes) return '0 B'
  const mb = bytes / 1048576
  if (mb >= 1) return mb.toFixed(1) + ' MB'
  return (bytes / 1024).toFixed(1) + ' KB'
}

// 打开软件详情弹窗
function showDetail(item) { detailItem.value = item; showModal.value = true }

// 重置表单为初始空值
function resetForm() { Object.assign(formData, { name: '', version: 'v1.0.0', category: '', os: '', size: 0, description: '', downloadUrl: '' }); editingItem.value = null }

// 打开新建软件表单
function openForm() { resetForm(); showForm.value = true }

// 打开编辑软件表单并回填数据
function editItem(item) { editingItem.value = item; Object.assign(formData, { name: item.name, version: item.version || 'v1.0.0', category: item.category || '', os: item.os || '', size: item.size || 0, description: item.description || '', downloadUrl: item.downloadUrl || '' }); showForm.value = true }

// 提交表单：新增或更新软件并刷新列表
async function submitForm() {
  if (!formData.name.trim()) return
  submitting.value = true
  try {
    if (editingItem.value) { await updateSoftware(editingItem.value.id, { ...formData }); toast.success('更新成功') }
    else { await createSoftware({ ...formData }); toast.success('上传成功') }
    showForm.value = false; load()
  } catch { toast.error('操作失败') }
  finally { submitting.value = false }
}

// 删除指定软件并刷新列表
async function deleteItem(item) {
  if (!(await dlgConfirm(`确定删除 "${item.name}"?`))) return
  try { await deleteSoftware(item.id); removeWhere(x => x.id === item.id); toast.success('删除成功') }
  catch { toast.error('删除失败') }
}

// 从全量软件提取分类去重，驱动筛选 Tab 与上传表单的 datalist
async function loadCats() {
  try {
    const res = await getSoftwareList({ page: 1, size: 500 })
    const cats = [...new Set((res.data?.records || []).map(i => i.category).filter(Boolean))]
    if (cats.length) catOptions.value = ['全部', ...cats]
  } catch { /* 分类提取失败时保留默认'全部' */ }
}

onMounted(() => { load(); loadCats() })
</script>

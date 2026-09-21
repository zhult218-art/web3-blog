<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <h1 class="text-3xl font-bold mb-2 text-gradient-cyber">软件资源</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">Software Downloads · Cross-Platform · Developer Tools</p>

      <div class="flex items-center justify-between mb-6">
        <div class="flex gap-2 flex-wrap">
          <button v-for="cat in catOptions" :key="cat"
            @click="selectedCat = cat; page=1; fetch()"
            :class="['px-3 py-1.5 rounded-full text-xs transition', selectedCat === cat ? 'bg-purple-500/30 text-purple-200 border border-purple-400/30' : 'border border-white/10 text-gray-400 hover:text-white']">{{ cat }}</button>
        </div>
        <button v-if="isAdmin" class="web3-btn text-xs !px-4 !py-2" @click="openForm">+ 上传软件</button>
      </div>

      <div v-if="list.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <div v-for="item in list" :key="item.id"
          class="glass-panel p-5 group cursor-pointer hover:border-purple-400/30 transition-all duration-300 hover:-translate-y-1">
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
      <div v-else-if="!loaded" class="py-10"><Loading /></div>
      <div v-else class="py-16 text-center">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-[#0e0e26] border border-white/[0.06] mb-4">
          <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 009.586 13H7"/></svg>
        </div>
        <p class="text-sm text-gray-500">暂无软件</p>
      </div>

      <div class="mt-6" v-if="total > 0">
        <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="fetch" />
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
          <p class="text-sm text-gray-400">{{ detailItem.description }}</p>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div class="glass-panel-sm p-3"><span class="text-gray-500">版本</span><p class="text-white mt-1">{{ detailItem.version || 'v1.0.0' }}</p></div>
            <div class="glass-panel-sm p-3"><span class="text-gray-500">系统</span><p class="text-white mt-1">{{ detailItem.os || '跨平台' }}</p></div>
            <div class="glass-panel-sm p-3"><span class="text-gray-500">大小</span><p class="text-white mt-1">{{ formatSize(detailItem.size || 0) }}</p></div>
            <div class="glass-panel-sm p-3"><span class="text-gray-500">下载量</span><p class="text-white mt-1">{{ detailItem.downloadCount || 0 }}</p></div>
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
import { resolveBrandIcon, resolveBrandIconLegacy } from '@/utils/brandIcons'
import Pagination from '@/components/common/Pagination.vue'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import { confirm as dlgConfirm } from '@/composables/useDialog'

const list = ref([]); const page = ref(1); const size = ref(9); const total = ref(0); const loaded = ref(false)
const selectedCat = ref('全部')
// 分类 Tab 数据驱动：从全量软件提取去重，避免写死分类与实际数据不匹配
const catOptions = ref(['全部'])
const showModal = ref(false); const detailItem = ref(null)
const showForm = ref(false); const editingItem = ref(null); const submitting = ref(false)
const formData = reactive({ name: '', version: 'v1.0.0', category: '', os: '', size: 0, description: '', downloadUrl: '' })
const toast = useToastStore()
const auth = useAuthStore()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
// 品牌图标两级降级：0=主源(simpleicons 品牌色) 1=旧版源(jsdelivr 单色) 2=首字母回退
const iconStages = ref(new Map())
function iconStage(id) { return iconStages.value.get(id) || 0 }
function iconSrc(item) {
  const st = iconStage(item?.id)
  if (st === 0) return resolveBrandIcon(item?.name)
  if (st === 1) return resolveBrandIconLegacy(item?.name)
  return ''
}
function onIconError(item) { iconStages.value.set(item.id, iconStage(item.id) + 1) }

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
    showForm.value = false; fetch()
  } catch { toast.error('操作失败') }
  finally { submitting.value = false }
}

// 删除指定软件并刷新列表
async function deleteItem(item) {
  if (!(await dlgConfirm(`确定删除 "${item.name}"?`))) return
  try { await deleteSoftware(item.id); list.value = list.value.filter(x => x.id !== item.id); toast.success('删除成功') }
  catch { toast.error('删除失败') }
}

// 按分页与分类筛选条件加载软件列表
function fetch() {
  const params = { page: page.value, size: size.value }
  if (selectedCat.value !== '全部') params.category = selectedCat.value
  getSoftwareList(params).then(res => { const d = res.data || {}; list.value = d.records || []; total.value = d.total || 0 }).finally(() => { loaded.value = true })
}

// 从全量软件提取分类去重，驱动筛选 Tab 与上传表单的 datalist
async function loadCats() {
  try {
    const res = await getSoftwareList({ page: 1, size: 500 })
    const cats = [...new Set((res.data?.records || []).map(i => i.category).filter(Boolean))]
    if (cats.length) catOptions.value = ['全部', ...cats]
  } catch { /* 分类提取失败时保留默认'全部' */ }
}

onMounted(() => { fetch(); loadCats() })
</script>

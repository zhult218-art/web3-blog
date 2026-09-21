<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-4xl">
      <PageBack label="返回资源库" to="/resources" class="mb-5" />
      <h1 class="text-3xl font-bold mb-2 text-gradient-cyber">资源上传</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">Upload Files · Share Resources</p>

      <div class="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <!-- Upload form -->
        <div class="lg:col-span-2 glass-panel p-6">
          <h3 class="text-lg font-semibold text-white mb-4">上传文件</h3>
          <div class="space-y-4">
            <div>
              <label class="text-xs text-gray-400 mb-1 block">资源标题 <span class="text-red-400">*</span></label>
              <input v-model="title" class="web3-input" placeholder="输入资源标题" />
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label class="text-xs text-gray-400 mb-1 block">分类</label>
                <select v-model="category" class="web3-input">
                  <option value="">选择分类</option>
                  <option>文档</option><option>图片</option><option>视频</option><option>音频</option><option>压缩包</option><option>其他</option>
                </select>
              </div>
              <div>
                <label class="text-xs text-gray-400 mb-1 block">描述</label>
                <input v-model="description" class="web3-input" placeholder="简短描述" />
              </div>
            </div>
            <!-- Drop zone -->
            <div
              class="border-2 border-dashed border-white/10 rounded-2xl p-8 text-center cursor-pointer hover:border-purple-400/40 transition"
              :class="{ 'border-purple-400/50 bg-purple-500/5': isDragging }"
              @dragover.prevent="isDragging = true"
              @dragleave="isDragging = false"
              @drop.prevent="onDrop"
              @click="$refs.fileInput.click()"
            >
              <div v-if="!file" class="space-y-2">
                <span class="text-3xl">📤</span>
                <p class="text-sm text-gray-400">拖拽文件到此处或<span class="text-purple-400">点击选择</span></p>
                <p class="text-[10px] text-gray-600">支持任意文件类型，最大 200MB</p>
              </div>
              <div v-else class="space-y-1">
                <span class="text-2xl">📄</span>
                <p class="text-sm text-white font-semibold">{{ file.name }}</p>
                <p class="text-xs text-gray-500">{{ formatSize(file.size) }}</p>
                <button class="text-xs text-red-400 mt-1" @click.stop="file = null; isDragging = false">移除</button>
              </div>
              <input ref="fileInput" type="file" class="hidden" @change="onFileChange" />
            </div>
            <button class="web3-btn w-full py-3 flex items-center justify-center gap-2" :disabled="!file || !title || uploading"
              @click="submit">
              <span v-if="uploading" class="inline-block h-4 w-4 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
              {{ uploading ? '上传中...' : '上传文件' }}
            </button>
          </div>
        </div>

        <!-- Recent uploads sidebar -->
        <div class="glass-panel p-5">
          <h3 class="text-sm font-semibold text-white mb-3 flex items-center gap-2"><span>📋</span>最近上传</h3>
          <div class="space-y-2">
            <div v-for="item in recentList" :key="item.id"
              class="rounded-xl border border-white/5 p-3 hover:border-cyan-400/20 transition text-sm">
              <p class="font-medium text-white truncate">{{ item.title }}</p>
              <p class="text-[10px] text-gray-500 mt-1">{{ item.category }} · {{ formatSize(item.size) }}</p>
              <a href="javascript:void(0)" class="text-[11px] text-cyan-400 mt-1.5 inline-block hover:underline"
                @click.prevent="downloadRecent(item)">下载</a>
            </div>
            <div v-if="!recentList.length" class="text-xs text-gray-600 py-4 text-center">暂无上传记录</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 资源上传页：拖拽/选择文件上传资源，
// 并展示近期上传记录
// ====================================================
import { ref, onMounted } from 'vue'
import { uploadResource, getResourceList, recordResourceDownload } from '@/api/resources'
import { useToastStore } from '@/stores/modules/toast'
import { downloadFile } from '@/utils/download'
import PageBack from '@/components/PageBack.vue'

const toast = useToastStore()
const title = ref(''); const category = ref(''); const description = ref('')
const file = ref(null); const isDragging = ref(false); const uploading = ref(false)
const recentList = ref([])

// 字节数格式化为 B/KB/MB 可读大小
function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

// 选择文件后记录到本地状态
function onFileChange(e) { file.value = e.target.files?.[0] || null }
// 拖拽释放文件到上传区
function onDrop(e) {
  isDragging.value = false
  file.value = e.dataTransfer?.files?.[0] || null
}

// 上传文件与表单信息，成功后清空并刷新近期列表
async function submit() {
  if (!file.value || !title.value) return
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', file.value)
    form.append('title', title.value)
    form.append('category', category.value)
    form.append('description', description.value)
    await uploadResource(form)
    toast.success('上传成功!')
    title.value = ''; category.value = ''; description.value = ''; file.value = null
    loadRecent()
  } catch { toast.error('上传失败') }
  finally { uploading.value = false }
}

// 加载近期上传的资源记录
function loadRecent() {
  getResourceList({ page: 1, size: 10 }).then(res => { recentList.value = (res.data?.records || []).slice(0, 10) })
}

// 浏览器直接下载近期上传的文件
async function downloadRecent(item) {
  if (!item?.downloadUrl) { toast.warning('暂无下载链接'); return }
  try {
    await recordResourceDownload(item.id).catch(() => {})
    const direct = await downloadFile(item.downloadUrl, item.title)
    if (direct) toast.success('已开始下载')
  } catch {
    toast.error('下载失败，请检查下载链接')
  }
}

onMounted(loadRecent)
</script>

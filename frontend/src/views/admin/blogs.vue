<template>
  <div class="min-h-screen py-8 px-4 md:px-6">
    <div class="max-w-7xl mx-auto">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-cyber">Blog</span> Management</h1>
        <div class="flex flex-col sm:flex-row gap-3">
          <input v-model="search" type="text" placeholder="搜索文章..." class="web3-input w-full sm:w-64" @keyup.enter="fetchBlogs" />
          <button class="web3-btn text-xs !px-4 !py-2" @click="fetchBlogs">搜索</button>
          <button class="web3-btn text-xs !px-4 !py-2" @click="openCreate">+ 新建文章</button>
        </div>
      </div>
      <div class="glass-panel overflow-hidden">
        <div class="overflow-x-auto">
          <table class="web3-table min-w-[820px]">
            <thead><tr><th>ID</th><th>标题</th><th>分类</th><th>浏览</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="7" class="text-center py-10"><Loading text="加载中..." /></td>
              </tr>
              <tr v-else-if="!list.length">
                <td colspan="7" class="text-center text-gray-600 py-10">暂无数据</td>
              </tr>
              <tr v-for="a in list" :key="a.id">
                <td class="matrix-text text-xs">#{{ a.id }}</td>
                <td class="text-white/80 font-medium max-w-xs truncate">
                  <span class="mr-1">{{ a.isTop === 1 ? '📌' : '' }}</span>{{ a.title }}
                </td>
                <td><span class="web3-badge-purple whitespace-nowrap">{{ a.category || '-' }}</span></td>
                <td class="text-white/60 matrix-text text-xs">{{ a.viewCount || 0 }}</td>
                <td><span :class="isPublished(a) ? 'web3-badge-green' : 'web3-badge-orange'">{{ isPublished(a) ? 'Published' : 'Draft' }}</span></td>
                <td class="text-gray-500 text-xs matrix-text">{{ formatDate(a.createdAt) }}</td>
                <td>
                  <div class="flex gap-2 whitespace-nowrap">
                    <button class="text-xs text-cyan-400 hover:text-cyan-300" @click="openEdit(a)">编辑</button>
                    <button class="text-xs text-yellow-400 hover:text-yellow-300" @click="toggleTop(a)" :title="a.isTop === 1 ? '取消置顶' : '置顶'">{{ a.isTop === 1 ? '取消置顶' : '置顶' }}</button>
                    <button class="text-xs text-emerald-400 hover:text-emerald-300" @click="toggleStatus(a)">{{ isPublished(a) ? '下架' : '发布' }}</button>
                    <button class="text-xs text-red-400 hover:text-red-300" @click="doDeleteBlog(a)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="mt-4" v-if="total > 0">
        <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="fetchBlogs" />
      </div>
    </div>

    <Modal v-model="showModal" :title="editing ? '编辑文章' : '新建文章'">
      <div class="space-y-4">
        <div>
          <label class="text-xs text-gray-400 block mb-1">标题 <span class="text-red-400">*</span></label>
          <input v-model="form.title" class="web3-input text-sm" placeholder="文章标题" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">摘要</label>
          <input v-model="form.summary" class="web3-input text-sm" placeholder="文章摘要（列表展示）" />
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="text-xs text-gray-400 block mb-1">分类</label>
            <input v-model="form.category" class="web3-input text-sm" placeholder="如: 技术 / 随笔" />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">标签（逗号分隔）</label>
            <input v-model="form.tags" class="web3-input text-sm" placeholder="Vue3, Web3, 随笔" />
          </div>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="text-xs text-gray-400 block mb-1">封面URL</label>
            <input v-model="form.cover" class="web3-input text-sm" placeholder="https://..." />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">状态</label>
            <select v-model="form.status" class="web3-input text-sm w-full">
              <option value="PUBLISHED">已发布 (PUBLISHED)</option>
              <option value="DRAFT">草稿 (DRAFT)</option>
            </select>
          </div>
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">内容 <span class="text-red-400">*</span></label>
          <textarea v-model="form.content" rows="8" class="web3-input text-sm w-full resize-y" placeholder="Markdown 内容..."></textarea>
        </div>
        <label class="flex items-center gap-2 text-sm text-white/70 cursor-pointer select-none">
          <input type="checkbox" v-model="form.isTop" class="accent-purple-500" />
          <span>置顶文章</span>
        </label>
        <div class="flex justify-end gap-3 pt-2">
          <button class="web3-btn-outline text-xs !px-5 !py-2" @click="showModal = false">取消</button>
          <button class="web3-btn text-xs !px-5 !py-2 flex items-center gap-2" :disabled="submitting" @click="submitBlog">
            <span v-if="submitting" class="inline-block h-3 w-3 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
// ====================================================
// 文章管理：文章列表（搜索 + 分页），
// 支持新建/编辑/删除文章
// ====================================================
import { ref, onMounted } from 'vue'
import { getAdminBlogList, deleteBlog, createBlog, updateBlog } from '@/api/blog'
import Pagination from '@/components/common/Pagination.vue'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm } from '@/composables/useDialog'

const toast = useToastStore()
const list = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const search = ref('')
const loading = ref(false)
const showModal = ref(false)
const editing = ref(null)
const submitting = ref(false)
const form = ref({ title: '', summary: '', category: '', tags: '', cover: '', status: 'PUBLISHED', isTop: 0, content: '' })

// 本地化格式化日期
function formatDate(d) { return d ? new Date(d).toLocaleDateString('zh-CN') : '' }

// 判断文章是否为已发布（status 为字符串）
function isPublished(a) { return String(a.status || '').toUpperCase() === 'PUBLISHED' }

// 组织提交给后端的表单载荷（isTop 布尔 → 0/1）
function buildPayload() {
  return {
    title: form.value.title,
    summary: form.value.summary,
    category: form.value.category,
    tags: form.value.tags,
    cover: form.value.cover,
    status: form.value.status,
    isTop: form.value.isTop ? 1 : 0,
    content: form.value.content,
  }
}

// 按分页与搜索关键字加载文章列表
async function fetchBlogs() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (search.value.trim()) params.keyword = search.value.trim()
    const res = await getAdminBlogList(params)
    const data = res.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } catch { list.value = [] }
  finally { loading.value = false }
}

// 打开新建文章弹窗
function openCreate() {
  editing.value = null
  form.value = { title: '', summary: '', category: '', tags: '', cover: '', status: 'PUBLISHED', isTop: 0, content: '' }
  showModal.value = true
}

// 打开编辑弹窗并回填文章数据
function openEdit(a) {
  editing.value = a
  form.value = {
    title: a.title || '',
    summary: a.summary || '',
    category: a.category || '',
    tags: a.tags || '',
    cover: a.cover || '',
    status: (a.status || 'PUBLISHED').toUpperCase(),
    isTop: a.isTop === 1 ? 1 : 0,
    content: a.content || '',
  }
  showModal.value = true
}

// 提交创建或更新文章
async function submitBlog() {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    toast.warning('标题和内容必填'); return
  }
  submitting.value = true
  try {
    const payload = buildPayload()
    if (editing.value) {
      await updateBlog(editing.value.id, payload)
      toast.success('文章已更新')
    } else {
      await createBlog(payload)
      toast.success('文章已创建')
    }
    showModal.value = false
    fetchBlogs()
  } catch { toast.error('保存失败') }
  finally { submitting.value = false }
}

// 切换置顶状态（快速操作，不改动其它字段）
async function toggleTop(a) {
  try {
    await updateBlog(a.id, { isTop: a.isTop === 1 ? 0 : 1 })
    a.isTop = a.isTop === 1 ? 0 : 1
    toast.success(a.isTop === 1 ? '已置顶' : '已取消置顶')
  } catch { toast.error('操作失败') }
}

// 发布/下架切换
async function toggleStatus(a) {
  const target = isPublished(a) ? 'DRAFT' : 'PUBLISHED'
  if (!(await dlgConfirm(target === 'PUBLISHED' ? `发布文章 "${a.title}"?` : `将文章 "${a.title}" 下架?`))) return
  try {
    await updateBlog(a.id, { status: target })
    a.status = target
    toast.success(target === 'PUBLISHED' ? '已发布' : '已下架')
  } catch { toast.error('操作失败') }
}

// 删除文章（二次确认后执行）
async function doDeleteBlog(a) {
  if (!(await dlgConfirm(`确定删除文章 "${a.title}"?`))) return
  try {
    await deleteBlog(a.id)
    list.value = list.value.filter(x => x.id !== a.id)
    toast.success('删除成功')
  } catch { toast.error('删除失败') }
}

// 挂载时加载文章列表
onMounted(fetchBlogs)
</script>
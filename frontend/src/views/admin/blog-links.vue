<template>
  <div class="min-h-screen py-8 px-4 md:px-6">
    <div class="max-w-7xl mx-auto">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-cyber">Friend</span> Links</h1>
        <button class="web3-btn text-xs !px-4 !py-2" @click="openCreate">+ 新增友链</button>
      </div>

      <div class="glass-panel overflow-hidden">
        <div class="overflow-x-auto">
          <table class="web3-table min-w-[860px]">
            <thead><tr><th>ID</th><th>站点</th><th>链接</th><th>分组</th><th>排序</th><th>状态</th><th>申请时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-if="loading"><td colspan="8" class="text-center py-10"><Loading text="加载中..." /></td></tr>
              <tr v-else-if="!list.length"><td colspan="8" class="text-center text-gray-600 py-10">暂无数据</td></tr>
              <tr v-for="l in list" :key="l.id">
                <td class="matrix-text text-xs">#{{ l.id }}</td>
                <td class="text-white/80 font-medium max-w-[160px]">
                  <div class="flex items-center gap-2">
                    <img v-if="l.avatar" :src="l.avatar" class="w-6 h-6 rounded object-cover" alt="" />
                    <span class="truncate">{{ l.name }}</span>
                  </div>
                </td>
                <td class="text-gray-500 text-xs max-w-[180px] truncate"><a :href="l.url" target="_blank" class="hover:text-cyan-300">{{ l.url }}</a></td>
                <td class="text-gray-500 text-xs">{{ l.groupName || 'default' }}</td>
                <td class="matrix-text text-xs">{{ l.sort ?? 0 }}</td>
                <td>
                  <span :class="statusClass(l.status)">{{ statusLabel(l.status) }}</span>
                </td>
                <td class="text-gray-500 text-xs matrix-text">{{ formatDate(l.createdAt) }}</td>
                <td>
                  <div class="flex gap-2 whitespace-nowrap text-xs">
                    <button v-if="l.status === 'PENDING'" class="text-green-400 hover:text-green-300" @click="audit(l, 'PUBLISHED')">通过</button>
                    <button v-if="l.status !== 'PENDING'" class="text-orange-400 hover:text-orange-300" @click="audit(l, 'PENDING')">驳回</button>
                    <button class="text-cyan-400 hover:text-cyan-300" @click="openEdit(l)">编辑</button>
                    <button class="text-red-400 hover:text-red-300" @click="doDelete(l)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <Modal v-model="showModal" :title="editing ? '编辑友链' : '新增友链'">
      <div class="space-y-4">
        <div>
          <label class="text-xs text-gray-400 block mb-1">站点名称 <span class="text-red-400">*</span></label>
          <input v-model="form.name" class="web3-input text-sm" placeholder="站点名称" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">链接 <span class="text-red-400">*</span></label>
          <input v-model="form.url" class="web3-input text-sm" placeholder="https://" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">头像</label>
          <input v-model="form.avatar" class="web3-input text-sm" placeholder="https://... 留空显示首字母" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">描述</label>
          <input v-model="form.description" class="web3-input text-sm" placeholder="一句话描述" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs text-gray-400 block mb-1">分组</label>
            <input v-model="form.groupName" class="web3-input text-sm" placeholder="default" />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">排序</label>
            <input v-model.number="form.sort" type="number" class="web3-input text-sm" />
          </div>
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">状态</label>
          <select v-model="form.status" class="web3-input text-sm">
            <option value="PUBLISHED">PUBLISHED 已发布</option>
            <option value="PENDING">PENDING 待审核</option>
            <option value="DISABLED">DISABLED 禁用</option>
          </select>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button class="web3-btn-outline text-xs !px-5 !py-2" @click="showModal = false">取消</button>
          <button class="web3-btn text-xs !px-5 !py-2" :disabled="submitting" @click="submitLink">
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
// ====================================================
// 友链管理：全量列表（含 PENDING 审核）、新增/编辑/删除/排序
// ====================================================
import { ref, onMounted } from 'vue'
import { getAdminLinks, updateAdminLink, createAdminLink, deleteAdminLink } from '@/api/blog'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm } from '@/composables/useDialog'

const toast = useToastStore()
const list = ref([])
const loading = ref(false)
const showModal = ref(false)
const editing = ref(null)
const submitting = ref(false)
const form = ref({ name: '', url: '', avatar: '', description: '', groupName: 'default', sort: 0, status: 'PUBLISHED' })

function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN') : '' }
function statusLabel(s) { return { PUBLISHED: '已发布', PENDING: '待审核', DISABLED: '已禁用' }[s] || s }
function statusClass(s) {
  return s === 'PUBLISHED' ? 'web3-badge-green' : s === 'PENDING' ? 'web3-badge-orange' : 'web3-badge-purple'
}

async function fetchLinks() {
  loading.value = true
  try { list.value = (await getAdminLinks()) || [] }
  catch {}
  finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  form.value = { name: '', url: '', avatar: '', description: '', groupName: 'default', sort: 0, status: 'PUBLISHED' }
  showModal.value = true
}

function openEdit(l) {
  editing.value = l
  form.value = { ...l }
  showModal.value = true
}

async function audit(l, status) {
  try {
    await updateAdminLink(l.id, { ...l, status })
    toast.success(status === 'PUBLISHED' ? '已通过审核' : '已驳回')
    fetchLinks()
  } catch {}
}

async function submitLink() {
  if (!form.value.name?.trim() || !form.value.url?.trim()) { toast.error('名称与链接必填'); return }
  submitting.value = true
  try {
    if (editing.value) await updateAdminLink(editing.value.id, { ...form.value })
    else await createAdminLink({ ...form.value })
    showModal.value = false
    toast.success('保存成功')
    fetchLinks()
  } catch {}
  finally { submitting.value = false }
}

async function doDelete(l) {
  if (!(await dlgConfirm(`确认删除友链「${l.name}」？`))) return
  try {
    await deleteAdminLink(l.id)
    toast.success('已删除')
    fetchLinks()
  } catch {}
}

onMounted(fetchLinks)
</script>
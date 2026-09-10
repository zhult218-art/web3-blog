<template>
  <div class="min-h-screen py-8 px-4 md:px-6">
    <div class="max-w-5xl mx-auto">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-cyber">Site</span> Notice</h1>
        <button class="web3-btn text-xs !px-4 !py-2" @click="openCreate">+ 新增公告</button>
      </div>

      <div class="glass-panel overflow-hidden">
        <table class="web3-table min-w-[540px]">
          <thead><tr><th>ID</th><th>内容</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-if="loading"><td colspan="5" class="text-center py-10"><Loading text="加载中..." /></td></tr>
            <tr v-else-if="!list.length"><td colspan="5" class="text-center text-gray-600 py-10">暂无公告</td></tr>
            <tr v-for="n in list" :key="n.id">
              <td class="matrix-text text-xs">#{{ n.id }}</td>
              <td class="text-white/80 max-w-[300px]"><span class="truncate block">{{ n.content }}</span></td>
              <td>
                <span :class="n.enabled === 1 ? 'web3-badge-green' : 'web3-badge-orange'">{{ n.enabled === 1 ? '启用中' : '已停用' }}</span>
              </td>
              <td class="text-gray-500 text-xs matrix-text">{{ formatDate(n.updatedAt || n.createdAt) }}</td>
              <td>
                <div class="flex gap-2 whitespace-nowrap text-xs">
                  <button class="text-cyan-400 hover:text-cyan-300" @click="openEdit(n)">编辑</button>
                  <button class="text-red-400 hover:text-red-300" @click="doDelete(n)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <Modal v-model="showModal" :title="editing ? '编辑公告' : '新增公告'">
      <div class="space-y-4">
        <div>
          <label class="text-xs text-gray-400 block mb-1">公告内容 <span class="text-red-400">*</span></label>
          <textarea v-model="form.content" rows="4" class="web3-input text-sm w-full resize-y" placeholder="公告内容..."></textarea>
        </div>
        <label class="flex items-center gap-2 cursor-pointer text-sm text-gray-300">
          <input v-model="form.enabled" type="checkbox" class="w-4 h-4 accent-purple-500" />
          启用（前台顶部公告条展示）
        </label>
        <div class="flex justify-end gap-3 pt-2">
          <button class="web3-btn-outline text-xs !px-5 !py-2" @click="showModal = false">取消</button>
          <button class="web3-btn text-xs !px-5 !py-2" :disabled="submitting" @click="submit">
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
// ====================================================
// 公告管理：列表 / 新增 / 编辑 / 启用开关 / 删除
// ====================================================
import { ref, onMounted } from 'vue'
import { getAdminNotices, saveAdminNotice, deleteAdminNotice } from '@/api/blog'
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
const form = ref({ content: '', enabled: true })

function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN') : '' }

async function fetchNotices() {
  loading.value = true
  try {
    const res = await getAdminNotices()
    list.value = res?.data ?? res ?? []
  }
  catch {}
  finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  form.value = { content: '', enabled: true }
  showModal.value = true
}

function openEdit(n) {
  editing.value = n
  form.value = { id: n.id, content: n.content, enabled: n.enabled === 1 }
  showModal.value = true
}

async function submit() {
  if (!form.value.content?.trim()) { toast.error('公告内容不能为空'); return }
  submitting.value = true
  try {
    const res = await saveAdminNotice({ id: editing.value?.id, content: form.value.content.trim(), enabled: form.value.enabled ? 1 : 0 })
    if (res?.code && res.code !== 200) { toast.error(res.message || '保存失败'); return }
    showModal.value = false
    toast.success('保存成功')
    fetchNotices()
  } catch (e) {
    toast.error(e?.response?.data?.message || '保存失败')
  }
  finally { submitting.value = false }
}

async function doDelete(n) {
  if (!(await dlgConfirm(`确认删除公告「${n.content.slice(0, 20)}」？`))) return
  try {
    await deleteAdminNotice(n.id)
    toast.success('已删除')
    fetchNotices()
  } catch {}
}

onMounted(fetchNotices)
</script>
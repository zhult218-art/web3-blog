<template>
  <div class="min-h-screen py-8 px-4 md:px-6">
    <div class="max-w-7xl mx-auto">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-cyber">分享网站</span> 管理</h1>
        <button class="web3-btn text-xs !px-4 !py-2" @click="openCreate">+ 新增网站</button>
      </div>

      <div class="glass-panel overflow-hidden">
        <div class="overflow-x-auto">
          <table class="web3-table min-w-[860px]">
            <thead><tr><th>ID</th><th>站点</th><th>链接</th><th>分类</th><th>排序</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-if="loading"><td colspan="8" class="text-center py-10"><Loading text="加载中..." /></td></tr>
              <tr v-else-if="!list.length"><td colspan="8" class="text-center text-gray-600 py-10">暂无数据</td></tr>
              <tr v-for="s in list" :key="s.id">
                <td class="matrix-text text-xs">#{{ s.id }}</td>
                <td class="text-white/80 font-medium max-w-[160px]">
                  <div class="flex items-center gap-2">
                    <span class="w-6 h-6 flex items-center justify-center text-sm">{{ s.icon || '🔗' }}</span>
                    <span class="truncate">{{ s.name }}</span>
                  </div>
                </td>
                <td class="text-gray-500 text-xs max-w-[220px] truncate"><a :href="s.url" target="_blank" class="hover:text-cyan-300">{{ s.url }}</a></td>
                <td class="text-gray-500 text-xs">{{ s.category || '-' }}</td>
                <td class="matrix-text text-xs">{{ s.sort ?? 0 }}</td>
                <td>
                  <button :class="s.status === 1 ? 'web3-badge-green' : 'web3-badge-purple'" class="!cursor-pointer" @click="toggle(s)">
                    {{ s.status === 1 ? '显示' : '隐藏' }}
                  </button>
                </td>
                <td class="text-gray-500 text-xs matrix-text">{{ formatDate(s.createdAt) }}</td>
                <td>
                  <div class="flex gap-2 whitespace-nowrap text-xs">
                    <button class="text-cyan-400 hover:text-cyan-300" @click="openEdit(s)">编辑</button>
                    <button class="text-red-400 hover:text-red-300" @click="doDelete(s)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <Modal v-model="showModal" :title="editing ? '编辑网站' : '新增网站'">
      <div class="space-y-4">
        <div>
          <label class="text-xs text-gray-400 block mb-1">站点名称 <span class="text-red-400">*</span></label>
          <input v-model="form.name" class="web3-input text-sm" placeholder="站点名称" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">链接 <span class="text-red-400">*</span></label>
          <input v-model="form.url" class="web3-input text-sm" placeholder="https://" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs text-gray-400 block mb-1">分类</label>
            <input v-model="form.category" class="web3-input text-sm" placeholder="如：研究 / 视频 / 编码助手" />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">图标</label>
            <input v-model="form.icon" class="web3-input text-sm" placeholder="emoji 或图片地址" />
          </div>
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">描述</label>
          <input v-model="form.description" class="web3-input text-sm" placeholder="一句话描述" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs text-gray-400 block mb-1">排序</label>
            <input v-model.number="form.sort" type="number" class="web3-input text-sm" />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">状态</label>
            <select v-model.number="form.status" class="web3-input text-sm">
              <option :value="1">显示</option>
              <option :value="0">隐藏</option>
            </select>
          </div>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button class="web3-btn-outline text-xs !px-5 !py-2" @click="showModal = false">取消</button>
          <button class="web3-btn text-xs !px-5 !py-2" :disabled="submitting" @click="submitSite">
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
// ====================================================
// 分享网站管理：全量列表（含隐藏），新增/编辑/删除/显示切换
// ====================================================
import { ref, onMounted } from 'vue'
import { getSiteList, createSite, updateSite, deleteSite } from '@/api/sites'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm } from '@/composables/useDialog'
import { formatDateTimeCN } from '@/utils/date'

const toast = useToastStore()
const list = ref([])
const loading = ref(false)
const showModal = ref(false)
const editing = ref(null)
const submitting = ref(false)
const form = ref({ name: '', url: '', category: '', description: '', icon: '', sort: 0, status: 1 })

// 日期格式化（统一走 utils/date）
function formatDate(d) { return formatDateTimeCN(d, '') }

async function fetchSites() {
  loading.value = true
  try {
    const res = await getSiteList({ onlyVisible: false })
    list.value = res.data || []
  } catch {}
  finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  form.value = { name: '', url: '', category: '', description: '', icon: '', sort: 0, status: 1 }
  showModal.value = true
}

function openEdit(s) {
  editing.value = s
  form.value = { ...s, status: s.status ?? 1, sort: s.sort ?? 0 }
  showModal.value = true
}

async function toggle(s) {
  try {
    await updateSite(s.id, { ...s, status: s.status === 1 ? 0 : 1 })
    s.status = s.status === 1 ? 0 : 1
    toast.success(s.status === 1 ? '已显示' : '已隐藏')
  } catch {}
}

async function submitSite() {
  if (!form.value.name?.trim() || !form.value.url?.trim()) { toast.error('名称与链接必填'); return }
  submitting.value = true
  try {
    if (editing.value) await updateSite(editing.value.id, { ...form.value })
    else await createSite({ ...form.value })
    showModal.value = false
    toast.success('保存成功')
    fetchSites()
  } catch {}
  finally { submitting.value = false }
}

async function doDelete(s) {
  if (!(await dlgConfirm(`确认删除「${s.name}」？`))) return
  try {
    await deleteSite(s.id)
    toast.success('已删除')
    fetchSites()
  } catch {}
}

onMounted(fetchSites)
</script>
<template>
  <div class="min-h-screen py-8 px-4 md:px-6">
    <div class="max-w-7xl mx-auto">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 class="text-xl font-bold text-white"><span class="text-gradient-purple">User</span> Management</h1>
        <div class="flex flex-col sm:flex-row gap-3">
          <input v-model="search" type="text" placeholder="搜索用户..." class="web3-input w-full sm:w-64" @keyup.enter="fetchUsers" />
          <button class="web3-btn text-xs !px-4 !py-2" @click="fetchUsers">搜索</button>
          <button class="web3-btn text-xs !px-4 !py-2" @click="openCreate">+ 新增用户</button>
        </div>
      </div>
      <div class="glass-panel overflow-hidden">
        <div class="overflow-x-auto">
          <table class="web3-table min-w-[760px]">
            <thead><tr><th>ID</th><th>用户名</th><th>昵称</th><th>邮箱</th><th>角色</th><th>状态</th><th>权限</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="9" class="text-center py-10"><Loading text="加载中..." /></td>
              </tr>
              <tr v-else-if="!list.length">
                <td colspan="9" class="text-center text-gray-600 py-10">暂无数据</td>
              </tr>
              <tr v-for="u in list" :key="u.id">
                <td class="matrix-text text-xs">#{{ u.id }}</td>
                <td class="text-white/80 font-medium">{{ u.username }}</td>
                <td class="text-white/60">{{ u.nickname || '-' }}</td>
                <td class="text-gray-500 text-xs">{{ u.email || '-' }}</td>
                <td><span :class="u.role === 'ADMIN' ? 'web3-badge-orange' : 'web3-badge-purple'">{{ u.role }}</span></td>
                <td><span :class="u.status === 1 ? 'web3-badge-green' : 'web3-badge-red'">{{ u.status === 1 ? '正常' : '禁用' }}</span></td>
                <td>
                  <button v-if="u.role !== 'ADMIN'" @click="openPerms(u)"
                    class="text-[11px] px-2 py-0.5 rounded-md border border-white/10 text-gray-400 hover:text-cyan-300 hover:border-cyan-400/40 transition whitespace-nowrap">
                    {{ permSummary(u) || '默认权限' }}
                  </button>
                  <span v-else class="text-[11px] text-gray-600">全部</span>
                </td>
                <td class="text-gray-500 text-xs matrix-text">{{ formatDate(u.createdAt) }}</td>
                <td>
                  <div class="flex gap-2 whitespace-nowrap">
                    <button @click="openEdit(u)" class="text-xs text-cyan-400 hover:text-cyan-300">编辑</button>
                    <button @click="toggleUserStatus(u)" class="text-xs text-purple-400 hover:text-purple-300">{{ u.status === 1 ? '禁用' : '启用' }}</button>
                    <button @click="deleteUser(u)" class="text-xs text-red-400 hover:text-red-300">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="mt-4" v-if="total > 0">
        <Pagination v-model:page="page" :page-size="size" :total="total" @update:page="fetchUsers" />
      </div>
    </div>

    <Modal v-model="showPermModal" :title="`权限设置 - ${permTarget?.username || ''}`">
      <div class="space-y-4">
        <p class="text-xs text-gray-500">勾选该用户可以浏览的界面；不勾选任何服务权限时，该用户默认只能浏览「默认内容」（首页/社区/媒体/更多）。</p>
        <div v-for="group in PERMISSIONS" :key="group.group" class="space-y-2">
          <p class="text-[11px] text-gray-500 uppercase tracking-widest matrix-text">{{ group.group }}</p>
          <div class="grid grid-cols-2 gap-2">
            <label v-for="item in group.items" :key="item.code"
              class="flex items-center gap-2 px-3 py-2 rounded-lg border border-white/[0.06] bg-[#0c0c22] hover:border-cyan-400/30 transition cursor-pointer"
              :class="{ 'border-cyan-400/40 bg-cyan-400/[0.06]': permChecked.includes(item.code) }">
              <input type="checkbox" :value="item.code" v-model="permChecked" class="accent-cyan-400" />
              <span class="text-xs text-gray-300">{{ item.label }}</span>
            </label>
          </div>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button class="web3-btn-outline text-xs !px-5 !py-2" @click="showPermModal = false">取消</button>
          <button class="web3-btn text-xs !px-5 !py-2 flex items-center gap-2" :disabled="permSaving" @click="savePerms">
            <span v-if="permSaving" class="inline-block h-3 w-3 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
            {{ permSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>

    <Modal v-model="showModal" :title="editing ? '编辑用户' : '新增用户'">
      <div class="space-y-4">
        <div>
          <label class="text-xs text-gray-400 block mb-1">用户名 <span class="text-red-400" v-if="!editing">*</span></label>
          <input v-model="form.username" class="web3-input text-sm" :disabled="editing" placeholder="登录用户名" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">昵称</label>
          <input v-model="form.nickname" class="web3-input text-sm" placeholder="显示昵称" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">邮箱</label>
          <input v-model="form.email" class="web3-input text-sm" placeholder="user@example.com" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">密码 <span class="text-red-400" v-if="!editing">*</span><span v-else class="text-gray-600">（留空则不修改）</span></label>
          <input v-model="form.password" type="password" class="web3-input text-sm" placeholder="至少 6 位" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-xs text-gray-400 block mb-1">角色</label>
            <select v-model="form.role" class="web3-input text-sm">
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
          <div v-if="editing">
            <label class="text-xs text-gray-400 block mb-1">状态</label>
            <select v-model="form.status" class="web3-input text-sm">
              <option :value="1">正常</option>
              <option :value="0">禁用</option>
            </select>
          </div>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button class="web3-btn-outline text-xs !px-5 !py-2" @click="showModal = false">取消</button>
          <button class="web3-btn text-xs !px-5 !py-2 flex items-center gap-2" :disabled="submitting" @click="submitUser">
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
// 用户管理：用户列表（搜索 + 分页），
// 支持新建/编辑/启停用/删除与权限配置
// ====================================================
import { ref, onMounted } from 'vue'
import { getUserList, updateUserStatus, deleteUser as apiDeleteUser, createUser, updateUser, getUserPermissions, updateUserPermissions } from '@/api/user'
import { PERMISSIONS, DEFAULT_PERMS, permLabel } from '@/utils/permissions'
import Pagination from '@/components/common/Pagination.vue'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm } from '@/composables/useDialog'
import { formatDayCN } from '@/utils/date'

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
const form = ref({ username: '', nickname: '', email: '', password: '', role: 'USER', status: 1 })

const showPermModal = ref(false)
const permTarget = ref(null)
const permChecked = ref([])
const permSaving = ref(false)

// 汇总显示用户前台权限标签
function permSummary(u) {
  if (!Array.isArray(u.permissions) || !u.permissions.length) return ''
  const codes = u.permissions.filter(c => !DEFAULT_PERMS.includes(c))
  const labels = codes.slice(0, 2).map(permLabel)
  return labels.length ? labels.join('、') + (codes.length > 2 ? ` +${codes.length - 2}` : '') : ''
}

// 打开权限编辑弹窗并加载用户已有权限
async function openPerms(u) {
  permTarget.value = u
  permChecked.value = []
  try {
    const res = await getUserPermissions(u.id)
    const perms = res.data || []
    permChecked.value = [...perms]
  } catch { toast.error('加载权限失败') }
  showPermModal.value = true
}

// 保存用户权限配置
async function savePerms() {
  if (permSaving.value) return
  permSaving.value = true
  try {
    await updateUserPermissions(permTarget.value.id, permChecked.value)
    permTarget.value.permissions = [...permChecked.value]
    toast.success('权限已更新')
    showPermModal.value = false
  } catch { toast.error('保存失败') }
  finally { permSaving.value = false }
}

// 本地化格式化日期
function formatDate(d) { return formatDayCN(d, '') }

// 按分页与搜索关键字加载用户列表
async function fetchUsers() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (search.value.trim()) params.keyword = search.value.trim()
    const res = await getUserList(params)
    const data = res.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } catch { list.value = [] }
  finally { loading.value = false }
}

// 打开新建用户弹窗
function openCreate() {
  editing.value = null
  form.value = { username: '', nickname: '', email: '', password: '', role: 'USER', status: 1 }
  showModal.value = true
}

// 打开编辑弹窗并回填用户数据
function openEdit(u) {
  editing.value = u
  form.value = { username: u.username, nickname: u.nickname || '', email: u.email || '', password: '', role: u.role || 'USER', status: u.status === 1 ? 1 : 0 }
  showModal.value = true
}

// 提交创建或更新用户
async function submitUser() {
  if (submitting.value) return
  if (!editing.value && (!form.value.username.trim() || !form.value.password)) {
    toast.warning('用户名和密码必填'); return
  }
  submitting.value = true
  try {
    if (editing.value) {
      const payload = { nickname: form.value.nickname, email: form.value.email }
      if (form.value.password) payload.password = form.value.password
      if (form.value.status !== editing.value.status) payload.status = form.value.status
      await updateUser(editing.value.id, payload, form.value.role)
      toast.success('用户已更新')
    } else {
      await createUser({ username: form.value.username, password: form.value.password, nickname: form.value.nickname, email: form.value.email }, form.value.role)
      toast.success('用户已创建')
    }
    showModal.value = false
    fetchUsers()
  } catch { toast.error('保存失败') }
  finally { submitting.value = false }
}

// 启用/禁用用户账号
async function toggleUserStatus(u) {
  try {
    await updateUserStatus(u.id, u.status === 1 ? 0 : 1)
    u.status = u.status === 1 ? 0 : 1
    toast.success('状态已更新')
  } catch { toast.error('操作失败') }
}

// 删除用户（二次确认后执行）
async function deleteUser(u) {
  if (!(await dlgConfirm(`确定删除用户 ${u.username}?`))) return
  try {
    await apiDeleteUser(u.id)
    list.value = list.value.filter(x => x.id !== u.id)
    toast.success('删除成功')
  } catch { toast.error('删除失败') }
}

// 挂载时加载用户列表
onMounted(fetchUsers)
</script>
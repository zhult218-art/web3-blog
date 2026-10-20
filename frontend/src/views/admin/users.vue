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
            <thead><tr><th>ID</th><th>用户名</th><th>昵称</th><th>邮箱</th><th>角色</th><th>状态</th><th>实名</th><th>权限</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="10" class="text-center py-10"><Loading text="加载中..." /></td>
              </tr>
              <tr v-else-if="!list.length">
                <td colspan="10" class="text-center text-gray-600 py-10">暂无数据</td>
              </tr>
              <tr v-for="u in list" :key="u.id">
                <td class="matrix-text text-xs">#{{ u.id }}</td>
                <td class="text-white/80 font-medium">{{ u.username }}</td>
                <td class="text-white/60">{{ u.nickname || '-' }}</td>
                <td class="text-gray-500 text-xs">{{ u.email || '-' }}</td>
                <td><span :class="u.role === 'ADMIN' ? 'web3-badge-orange' : 'web3-badge-purple'">{{ u.role }}</span></td>
                <td><span :class="u.status === 1 ? 'web3-badge-green' : 'web3-badge-red'">{{ u.status === 1 ? '正常' : '禁用' }}</span></td>
                <td>
                  <span v-if="u.verified === 2" class="web3-badge-green whitespace-nowrap">✅ 已认证</span>
                  <template v-else-if="u.verified === 1">
                    <span class="web3-badge-orange whitespace-nowrap">⏳ 待审核</span>
                    <div class="flex gap-1.5 mt-1">
                      <button @click="auditVerify(u, 2)" class="text-[10px] px-1.5 py-0.5 rounded border border-emerald-400/30 text-emerald-300 hover:bg-emerald-400/10">通过</button>
                      <button @click="auditVerify(u, 3)" class="text-[10px] px-1.5 py-0.5 rounded border border-red-400/30 text-red-300 hover:bg-red-400/10">拒绝</button>
                    </div>
                  </template>
                  <span v-else-if="u.verified === 3" class="web3-badge-red whitespace-nowrap">❌ 已拒绝</span>
                  <span v-else class="text-[11px] text-gray-600 whitespace-nowrap">未认证</span>
                </td>
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
        <p class="text-xs text-gray-500">默认内容注册即拥有（不可收回）；互动权限默认勾选，取消勾选即收回；上传/服务权限需勾选授权。保存后用户重新登录或会话续期时生效。</p>
        <div v-for="group in PERMISSIONS" :key="group.group" class="space-y-2">
          <p class="text-[11px] text-gray-500 uppercase tracking-widest matrix-text">{{ group.group }}</p>
          <div class="grid grid-cols-2 gap-2">
            <label v-for="item in group.items" :key="item.code"
              class="flex items-center gap-2 px-3 py-2 rounded-lg border border-white/[0.06] bg-[#0c0c22] hover:border-cyan-400/30 transition cursor-pointer"
              :class="{
                'border-cyan-400/40 bg-cyan-400/[0.06]': permChecked.includes(item.code),
                'opacity-60 cursor-not-allowed': isLockedPerm(item.code)
              }">
              <input type="checkbox" :value="item.code" v-model="permChecked" class="accent-cyan-400"
                :disabled="isLockedPerm(item.code)" />
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
import { getUserList, updateUserStatus, deleteUser as apiDeleteUser, createUser, updateUser, getUserPermissions, updateUserPermissions, auditUserVerification } from '@/api/user'
import { PERMISSIONS, DEFAULT_PERMS, REVOCABLE_PERMS, permLabel } from '@/utils/permissions'
import Pagination from '@/components/common/Pagination.vue'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm } from '@/composables/useDialog'
import { formatDayCN } from '@/utils/date'
import { formatDayCN as formatDate } from '@/utils/date.js'

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

// 汇总显示用户前台权限标签（正向授权 + 已收回的互动权限）
function permSummary(u) {
  if (!Array.isArray(u.permissions) || !u.permissions.length) return ''
  const granted = u.permissions.filter(c => c && !c.startsWith('-') && !DEFAULT_PERMS.includes(c)).map(permLabel)
  const revoked = u.permissions.filter(c => c && c.startsWith('-')).map(c => permLabel(c.slice(1)))
  const parts = []
  if (granted.length) parts.push(granted.slice(0, 2).join('、') + (granted.length > 2 ? ` +${granted.length - 2}` : ''))
  if (revoked.length) parts.push('已限:' + revoked.slice(0, 2).join('、') + (revoked.length > 2 ? ` +${revoked.length - 2}` : ''))
  return parts.join(' ')
}

// 默认权限码 checkbox 锁定为勾选（注册即拥有，收回无效果）
function isLockedPerm(code) {
  return DEFAULT_PERMS.includes(code)
}

// 由存储的权限串（正向码 + "-code" 负向码）还原弹窗勾选状态：
// 默认权限恒勾选；互动权限默认勾选、被收回（-code）则不勾选；上传/服务权限勾选=显式授权
function storedToChecked(stored) {
  const set = new Set(stored || [])
  const checked = []
  for (const g of PERMISSIONS) {
    for (const it of g.items) {
      if (DEFAULT_PERMS.includes(it.code) || (REVOCABLE_PERMS.includes(it.code) && !set.has('-' + it.code))) {
        checked.push(it.code)
      } else if (set.has(it.code)) {
        checked.push(it.code)
      }
    }
  }
  return checked
}

// 由弹窗勾选状态生成存储权限串：
// 默认权限不落库（人人恒有）；互动权限取消勾选 → "-code"；上传/服务权限勾选 → "code"
function checkedToStored(checked) {
  const set = new Set(checked)
  const out = []
  for (const g of PERMISSIONS) {
    for (const it of g.items) {
      if (DEFAULT_PERMS.includes(it.code)) continue
      if (REVOCABLE_PERMS.includes(it.code)) {
        if (!set.has(it.code)) out.push('-' + it.code)
      } else if (set.has(it.code)) {
        out.push(it.code)
      }
    }
  }
  return out
}

// 打开权限编辑弹窗并加载用户已有权限
async function openPerms(u) {
  permTarget.value = u
  permChecked.value = []
  try {
    const res = await getUserPermissions(u.id)
    permChecked.value = storedToChecked(res.data || [])
  } catch { toast.error('加载权限失败') }
  showPermModal.value = true
}

// 保存用户权限配置
async function savePerms() {
  if (permSaving.value) return
  permSaving.value = true
  try {
    const stored = checkedToStored(permChecked.value)
    await updateUserPermissions(permTarget.value.id, stored)
    permTarget.value.permissions = stored
    toast.success('权限已更新')
    showPermModal.value = false
  } catch { toast.error('保存失败') }
  finally { permSaving.value = false }
}

// 本地化格式化日期

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

// 实名认证审核（2=通过 3=拒绝）
async function auditVerify(u, verified) {
  const action = verified === 2 ? '通过' : '拒绝'
  if (!(await dlgConfirm(`确定${action} ${u.nickname || u.username} 的实名认证申请？`, {
    title: '实名认证审核', okText: `确认${action}`, tone: verified === 3 ? 'danger' : undefined
  }))) return
  try {
    await auditUserVerification(u.id, verified)
    u.verified = verified
    toast.success(`已${action}该用户的实名认证`)
  } catch { toast.error('审核操作失败') }
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
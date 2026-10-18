<template>
  <div class="min-h-screen py-8 px-4 md:px-6">
    <div class="max-w-7xl mx-auto">
      <!-- 标题 + 页面切换 -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <h1 class="text-xl font-bold text-white">
          <span class="text-gradient-cyber">Nav</span> Links 导航维护
        </h1>
        <div class="flex items-center gap-2 flex-wrap">
          <div class="flex rounded-lg overflow-hidden border border-white/10">
            <button v-for="p in PAGES" :key="p.key" @click="switchPage(p.key)"
              class="px-4 py-2 text-xs transition-colors"
              :class="pageKey === p.key ? 'bg-cyan-500/20 text-cyan-300' : 'text-gray-400 hover:text-gray-200'">
              {{ p.label }}
            </button>
          </div>
          <button class="web3-btn-outline text-xs !px-4 !py-2" :disabled="fetchingIcons" @click="doFetchIcons">
            {{ fetchingIcons ? '抓取中...' : '抓取官网图标' }}
          </button>
          <button class="web3-btn-outline text-xs !px-4 !py-2" @click="openCatCreate">+ 新增分类</button>
          <button class="web3-btn text-xs !px-4 !py-2" @click="openCreate">+ 新增条目</button>
        </div>
      </div>

      <!-- 分类管理 -->
      <div class="glass-panel p-4 mb-6">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-sm font-semibold text-white/80">分类（共 {{ cats.length }} 个）</h2>
          <span class="text-[11px] text-gray-500">分类决定条目在页面上的分组与主题色，键值一经创建不建议修改</span>
        </div>
        <div class="flex flex-wrap gap-2">
          <div v-for="c in cats" :key="c.id"
            class="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-white/10 bg-white/[0.03] text-xs">
            <span>{{ c.icon }}</span>
            <span class="text-gray-200">{{ c.name }}</span>
            <span class="matrix-text text-[10px] text-gray-500">{{ c.catKey }}</span>
            <span class="text-[10px] px-1.5 rounded" :class="c.enabled ? 'bg-green-500/15 text-green-300' : 'bg-gray-500/15 text-gray-400'">
              {{ c.enabled ? '启用' : '停用' }}
            </span>
            <button class="text-cyan-400 hover:text-cyan-300" @click="openCatEdit(c)">改</button>
            <button class="text-red-400 hover:text-red-300" @click="doDeleteCat(c)">删</button>
          </div>
          <p v-if="!cats.length" class="text-xs text-gray-600">暂无分类，请先新增分类</p>
        </div>
      </div>

      <!-- 过滤 -->
      <div class="flex flex-col sm:flex-row gap-3 mb-4">
        <select v-model="filterCat" class="web3-input text-sm sm:max-w-[220px]">
          <option value="">全部分类</option>
          <option v-for="c in cats" :key="c.id" :value="c.catKey">{{ c.name }}</option>
        </select>
        <input v-model="keyword" class="web3-input text-sm sm:max-w-[320px]" placeholder="搜索名称 / 标签 / 说明..." />
      </div>

      <!-- 条目表 -->
      <div class="glass-panel overflow-hidden">
        <div class="overflow-x-auto">
          <table class="web3-table min-w-[1040px]">
            <thead>
              <tr>
                <th>ID</th><th>图标</th><th>名称</th><th>链接</th><th>分类</th>
                <th>标签</th><th>详细说明</th><th>排序</th><th>状态</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="10" class="text-center py-10"><Loading text="加载中..." /></td></tr>
              <tr v-else-if="!filtered.length"><td colspan="10" class="text-center text-gray-600 py-10">暂无数据</td></tr>
              <tr v-for="l in filtered" :key="l.id">
                <td class="matrix-text text-xs">#{{ l.id }}</td>
                <td>
                  <NavIcon :name="l.name" :url="l.url" :icon-url="l.iconUrl || ''" :size="30" radius="8px"
                    :color="catColor(l.catKey).color" />
                </td>
                <td class="text-white/80 font-medium max-w-[150px] truncate">{{ l.name }}</td>
                <td class="text-gray-500 text-xs max-w-[170px] truncate">
                  <a :href="l.url" target="_blank" rel="noopener" class="hover:text-cyan-300">{{ l.url }}</a>
                </td>
                <td class="text-gray-400 text-xs">{{ catName(l.catKey) }}</td>
                <td class="text-gray-400 text-xs max-w-[150px] truncate">{{ l.tag || '-' }}</td>
                <td class="text-gray-500 text-xs max-w-[260px]">
                  <span class="line-clamp-2" :title="l.description">{{ l.description || '-' }}</span>
                </td>
                <td class="matrix-text text-xs">{{ l.sort ?? 0 }}</td>
                <td>
                  <span :class="l.enabled ? 'web3-badge-green' : 'web3-badge-purple'">{{ l.enabled ? '已启用' : '已停用' }}</span>
                </td>
                <td>
                  <div class="flex gap-2 whitespace-nowrap text-xs">
                    <button class="text-cyan-400 hover:text-cyan-300" @click="openEdit(l)">编辑</button>
                    <button class="text-orange-400 hover:text-orange-300" @click="toggleEnabled(l)">{{ l.enabled ? '停用' : '启用' }}</button>
                    <button class="text-red-400 hover:text-red-300" @click="doDelete(l)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <p class="text-[11px] text-gray-600 mt-4 leading-relaxed">
        说明：条目与分类均存储在 MySQL 数据库（nav_link / nav_category），换设备、换浏览器均保持一致。
        图标优先使用「图标地址」字段；留空时前端会自动尝试该条目的官网 favicon，仍取不到则回退为首字母色块。
        点击上方「抓取官网图标」可为当前页面中尚无图标的条目批量抓取并写库。
      </p>
    </div>

    <!-- 条目编辑弹窗 -->
    <Modal v-model="showModal" :title="editing ? '编辑条目' : '新增条目'">
      <div class="space-y-4">
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs text-gray-400 block mb-1">名称 <span class="text-red-400">*</span></label>
            <input v-model="form.name" class="web3-input text-sm" placeholder="企业 / 平台名称" />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">所属分类 <span class="text-red-400">*</span></label>
            <select v-model="form.catKey" class="web3-input text-sm">
              <option v-for="c in cats" :key="c.id" :value="c.catKey">{{ c.name }}</option>
            </select>
          </div>
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">链接 <span class="text-red-400">*</span></label>
          <input v-model="form.url" class="web3-input text-sm" placeholder="https://" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">标签</label>
          <input v-model="form.tag" class="web3-input text-sm" placeholder="短标签，如「电网」「提交漏洞获赏金」" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">详细说明（鼠标悬停卡片时展示）</label>
          <textarea v-model="form.description" rows="3" class="web3-input text-sm"
            placeholder="一句话介绍：做什么的、怎么赚钱 / 招什么岗、门槛等"></textarea>
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">图标地址</label>
          <div class="flex items-center gap-3">
            <input v-model="form.iconUrl" class="web3-input text-sm flex-1" placeholder="留空则自动取官网 favicon 并回退首字母" />
            <NavIcon :name="form.name" :url="form.url" :icon-url="form.iconUrl || ''" :size="34" radius="9px"
              :color="catColor(form.catKey).color" />
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs text-gray-400 block mb-1">排序（小的在前）</label>
            <input v-model.number="form.sort" type="number" class="web3-input text-sm" />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">状态</label>
            <select v-model.number="form.enabled" class="web3-input text-sm">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button class="web3-btn-outline text-xs !px-5 !py-2" @click="showModal = false">取消</button>
          <button class="web3-btn text-xs !px-5 !py-2" :disabled="submitting" @click="submitLink">
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>

    <!-- 分类编辑弹窗 -->
    <Modal v-model="showCatModal" :title="editingCat ? '编辑分类' : '新增分类'">
      <div class="space-y-4">
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs text-gray-400 block mb-1">分类键 <span class="text-red-400">*</span></label>
            <input v-model="catForm.catKey" class="web3-input text-sm" :disabled="!!editingCat" placeholder="如 bounty（英文小写）" />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">图标</label>
            <input v-model="catForm.icon" class="web3-input text-sm" placeholder="一个 emoji，如 🔐" />
          </div>
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">分类名称 <span class="text-red-400">*</span></label>
          <input v-model="catForm.name" class="web3-input text-sm" placeholder="如 安全众测·漏洞赏金" />
        </div>
        <div>
          <label class="text-xs text-gray-400 block mb-1">分类说明</label>
          <textarea v-model="catForm.description" rows="2" class="web3-input text-sm" placeholder="该分类是什么、怎么赚钱"></textarea>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="text-xs text-gray-400 block mb-1">排序</label>
            <input v-model.number="catForm.sort" type="number" class="web3-input text-sm" />
          </div>
          <div>
            <label class="text-xs text-gray-400 block mb-1">状态</label>
            <select v-model.number="catForm.enabled" class="web3-input text-sm">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
        </div>
        <div class="flex justify-end gap-3 pt-2">
          <button class="web3-btn-outline text-xs !px-5 !py-2" @click="showCatModal = false">取消</button>
          <button class="web3-btn text-xs !px-5 !py-2" :disabled="submitting" @click="submitCat">
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup>
// ====================================================
// 导航数据维护：招募亭（jobs）与赏金猎人（earn）的平台/企业条目与分类
// 数据统一落 MySQL（nav_link / nav_category），支持增删改、排序、启停与图标抓取
// 分类主题色沿用前端 catColorMap，新增分类自动落到默认主题色
// ====================================================
import { ref, computed, onMounted } from 'vue'
import {
  getAdminNavCategories, getAdminNavLinks,
  createNavCategory, updateNavCategory, deleteNavCategory,
  createNavLink, updateNavLink, deleteNavLink, fetchNavIcons
} from '@/api/nav'
import Modal from '@/components/common/Modal.vue'
import Loading from '@/components/common/Loading.vue'
import NavIcon from '@/components/common/NavIcon.vue'
import { useToastStore } from '@/stores/modules/toast'
import { confirm as dlgConfirm } from '@/composables/useDialog'

/** 可维护的页面：招募亭 / 赏金猎人 */
const PAGES = [
  { key: 'jobs', label: '冒险者招募亭' },
  { key: 'earn', label: '赏金猎人' }
]

// 分类主题色（与公开页 catColorMap 保持一致，键名不同也不影响展示）
const CAT_COLORS = {
  central: { color: '#67e8f9' }, tech: { color: '#a78bfa' }, foreign: { color: '#60a5fa' },
  bank: { color: '#fbbf24' }, inst: { color: '#34d399' }, defense: { color: '#fb7185' },
  auto: { color: '#22d3ee' }, bio: { color: '#e879f9' },
  bounty: { color: '#fbbf24' }, web3: { color: '#a78bfa' }, ossbounty: { color: '#22d3ee' },
  freelance: { color: '#60a5fa' }, microtask: { color: '#34d399' }, ailabel: { color: '#2dd4bf' },
  creator: { color: '#fb7185' }, knowledge: { color: '#fb923c' }, affiliate: { color: '#e879f9' },
  ecommerce: { color: '#a3e635' }, gig: { color: '#facc15' }, lab: { color: '#f87171' }
}
function catColor(key) { return CAT_COLORS[key] || { color: '#7c3aed' } }

const toast = useToastStore()

const pageKey = ref('jobs')
const cats = ref([])
const links = ref([])
const loading = ref(false)
const filterCat = ref('')
const keyword = ref('')
const fetchingIcons = ref(false)
const submitting = ref(false)

const showModal = ref(false)
const editing = ref(null)
const form = ref(emptyForm())

const showCatModal = ref(false)
const editingCat = ref(null)
const catForm = ref(emptyCatForm())

function emptyForm() {
  return { page: pageKey.value, catKey: '', name: '', url: '', tag: '', description: '', iconUrl: '', sort: 0, enabled: 1 }
}
function emptyCatForm() {
  return { page: pageKey.value, catKey: '', name: '', icon: '', description: '', sort: 0, enabled: 1 }
}

// 取响应信封中的数据（拦截器返回 {code,message,data}，也兼容直接返回数组）
function unwrap(res) {
  if (Array.isArray(res)) return res
  return (res && res.data) || []
}

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return links.value.filter(l => {
    if (filterCat.value && l.catKey !== filterCat.value) return false
    if (!kw) return true
    return (l.name || '').toLowerCase().includes(kw)
      || (l.tag || '').toLowerCase().includes(kw)
      || (l.description || '').toLowerCase().includes(kw)
  })
})

function catName(key) {
  const c = cats.value.find(x => x.catKey === key)
  return c ? c.name : key
}

async function fetchAll() {
  loading.value = true
  try {
    const [catRes, linkRes] = await Promise.all([
      getAdminNavCategories(pageKey.value),
      getAdminNavLinks(pageKey.value)
    ])
    cats.value = unwrap(catRes)
    links.value = unwrap(linkRes)
  } catch { /* 拦截器已提示错误 */ }
  finally { loading.value = false }
}

async function switchPage(key) {
  if (pageKey.value === key) return
  pageKey.value = key
  filterCat.value = ''
  keyword.value = ''
  await fetchAll()
}

/* ── 条目 ── */
function openCreate() {
  if (!cats.value.length) { toast.error('请先创建至少一个分类'); return }
  editing.value = null
  form.value = { ...emptyForm(), catKey: filterCat.value || cats.value[0].catKey, sort: links.value.length }
  showModal.value = true
}
function openEdit(l) {
  editing.value = l
  form.value = { ...l, page: pageKey.value }
  showModal.value = true
}
async function submitLink() {
  if (!form.value.name?.trim() || !form.value.url?.trim()) { toast.error('名称与链接必填'); return }
  if (!form.value.catKey) { toast.error('请选择所属分类'); return }
  submitting.value = true
  try {
    if (editing.value) await updateNavLink(editing.value.id, { ...form.value })
    else await createNavLink({ ...form.value })
    showModal.value = false
    toast.success('保存成功')
    fetchAll()
  } catch { /* 拦截器已提示错误 */ }
  finally { submitting.value = false }
}
async function toggleEnabled(l) {
  try {
    await updateNavLink(l.id, { ...l, page: pageKey.value, enabled: l.enabled ? 0 : 1 })
    toast.success(l.enabled ? '已停用' : '已启用')
    fetchAll()
  } catch { /* 拦截器已提示错误 */ }
}
async function doDelete(l) {
  if (!(await dlgConfirm(`确认删除「${l.name}」？`))) return
  try {
    await deleteNavLink(l.id)
    toast.success('已删除')
    fetchAll()
  } catch { /* 拦截器已提示错误 */ }
}

/* ── 分类 ── */
function openCatCreate() {
  editingCat.value = null
  catForm.value = { ...emptyCatForm(), sort: cats.value.length }
  showCatModal.value = true
}
function openCatEdit(c) {
  editingCat.value = c
  catForm.value = { ...c, page: pageKey.value }
  showCatModal.value = true
}
async function submitCat() {
  if (!catForm.value.catKey?.trim() || !catForm.value.name?.trim()) { toast.error('分类键与名称必填'); return }
  submitting.value = true
  try {
    if (editingCat.value) await updateNavCategory(editingCat.value.id, { ...catForm.value })
    else await createNavCategory({ ...catForm.value })
    showCatModal.value = false
    toast.success('保存成功')
    fetchAll()
  } catch { /* 拦截器已提示错误 */ }
  finally { submitting.value = false }
}
async function doDeleteCat(c) {
  if (!(await dlgConfirm(`确认删除分类「${c.name}」？该分类下的所有条目会一并删除。`))) return
  try {
    await deleteNavCategory(c.id)
    toast.success('已删除')
    fetchAll()
  } catch { /* 拦截器已提示错误 */ }
}

/* ── 图标批量抓取 ── */
async function doFetchIcons() {
  fetchingIcons.value = true
  try {
    const res = await fetchNavIcons(pageKey.value)
    const r = (res && res.data) || res || {}
    toast.success(`已抓取 ${r.updated ?? 0} / ${r.total ?? 0} 个图标`)
    fetchAll()
  } catch { /* 拦截器已提示错误 */ }
  finally { fetchingIcons.value = false }
}

onMounted(fetchAll)
</script>

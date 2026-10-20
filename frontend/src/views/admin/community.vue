<template>
  <div class="community-page">
    <div class="flex items-center justify-between mb-5 flex-wrap gap-3">
      <div>
        <h2 class="text-xl font-bold text-white">社区内容管理</h2>
        <p class="text-xs text-gray-500 mt-1">审核与删除社区广场的帖子和评论，普通用户发布的内容默认进入待审核</p>
      </div>
      <button class="web3-btn text-xs" @click="loadList">刷新</button>
    </div>

    <!-- Tabs -->
    <div class="tabs">
      <button class="tab" :class="{ active: tab === 'post' }" @click="switchTab('post')">📝 帖子</button>
      <button class="tab" :class="{ active: tab === 'comment' }" @click="switchTab('comment')">💬 评论</button>
    </div>

    <!-- Filters -->
    <div class="filters">
      <div class="chips">
        <button v-for="s in statusOptions" :key="s.value" class="chip"
          :class="{ active: status === s.value }" @click="changeStatus(s.value)">
          {{ s.label }}
        </button>
      </div>
      <form class="search-box" @submit.prevent="onSearch">
        <input v-model="keywordInput" type="text" :placeholder="tab === 'post' ? '搜索标题或正文…' : '搜索评论内容…'" />
        <button type="submit" class="web3-btn text-xs">搜索</button>
        <button type="button" class="web3-btn ghost text-xs" @click="resetKeyword" v-if="keyword">清除</button>
      </form>
    </div>

    <!-- Batch action bar -->
    <transition name="bar-slide">
      <div v-if="selectedIds.length" class="batch-bar">
        <div class="batch-info">
          <span class="batch-count">已选 {{ selectedIds.length }} 项</span>
          <button class="batch-link" @click="togglePageAll(!allSelected)">
            {{ allSelected ? '取消本页全选' : '全选本页' }}
          </button>
          <button class="batch-link" @click="clearSelection">清空选择</button>
        </div>
        <div class="batch-actions">
          <button class="batch-btn approve" :disabled="batchBusy" @click="doBatch('approve')">✓ 批量通过</button>
          <button class="batch-btn reject" :disabled="batchBusy" @click="doBatch('reject')">✕ 批量驳回</button>
          <button class="batch-btn delete" :disabled="batchBusy" @click="doBatch('delete')">🗑 批量删除</button>
        </div>
      </div>
    </transition>

    <!-- Post table -->
    <div v-if="tab === 'post'" class="table-wrap">
      <table class="mod-table">
        <thead>
          <tr>
            <th class="col-check"><input type="checkbox" :checked="allSelected"
              :disabled="!list.length" @change="togglePageAll($event.target.checked)" /></th>
            <th>标题 / 正文</th><th>分类</th><th>作者</th><th>状态</th>
            <th class="num">赞/评/览</th><th>发布时间</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in list" :key="p.id" :class="{ 'row-selected': selectedIds.includes(String(p.id)) }">
            <td class="col-check"><input type="checkbox" :checked="selectedIds.includes(String(p.id))"
              @change="toggleSelect(String(p.id))" /></td>
            <td class="cell-content">
              <div class="title-link" @click="openDetail(p)">{{ p.title || '（无标题）' }}</div>
              <div class="snippet">{{ stripMd(p.content) }}</div>
              <div v-if="p.isPinned === 1" class="pin-tag">置顶</div>
            </td>
            <td>{{ p.category || '-' }}</td>
            <td>{{ p.authorName || (p.authorId ? '用户' + p.authorId : '匿名') }}</td>
            <td><span class="status-badge" :class="p.status">{{ statusLabel(p.status) }}</span></td>
            <td class="num muted">{{ p.likeCount ?? 0 }} / {{ p.replyCount ?? 0 }} / {{ p.viewCount ?? 0 }}</td>
            <td class="muted nowrap">{{ formatTime(p.createdAt) }}</td>
            <td>
              <div class="row-actions">
                <button v-if="p.status !== 'published'" class="act approve" @click="doApprove(p)">通过</button>
                <button v-if="p.status !== 'rejected'" class="act reject" @click="doReject(p)">驳回</button>
                <button class="act delete" @click="doDelete(p)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!list.length" class="empty-state"><div class="empty-icon">🗂️</div><p>没有符合条件的帖子</p></div>
    </div>

    <!-- Comment table -->
    <div v-else class="table-wrap">
      <table class="mod-table">
        <thead>
          <tr>
            <th class="col-check"><input type="checkbox" :checked="allSelected"
              :disabled="!list.length" @change="togglePageAll($event.target.checked)" /></th>
            <th>评论内容</th><th>所属帖子</th><th>评论者</th><th>状态</th><th>时间</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in list" :key="c.id" :class="{ 'row-selected': selectedIds.includes(String(c.id)) }">
            <td class="col-check"><input type="checkbox" :checked="selectedIds.includes(String(c.id))"
              @change="toggleSelect(String(c.id))" /></td>
            <td class="cell-content">
              <div class="snippet comment-text" @click="openDetail(c)">{{ c.content }}</div>
              <div v-if="c.replyToName" class="reply-tag">回复 @{{ c.replyToName }}</div>
            </td>
            <td class="muted post-title">{{ c.postTitle || ('帖子#' + c.targetId) }}</td>
            <td>{{ c.authorName || (c.authorId ? '用户' + c.authorId : '匿名') }}</td>
            <td><span class="status-badge" :class="c.status">{{ statusLabel(c.status) }}</span></td>
            <td class="muted nowrap">{{ formatTime(c.createdAt) }}</td>
            <td>
              <div class="row-actions">
                <button v-if="c.status !== 'published'" class="act approve" @click="doApprove(c)">通过</button>
                <button v-if="c.status !== 'rejected'" class="act reject" @click="doReject(c)">驳回</button>
                <button class="act delete" @click="doDelete(c)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!list.length" class="empty-state"><div class="empty-icon">💬</div><p>没有符合条件的评论</p></div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="total > size">
      <button class="web3-btn text-xs" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span class="muted text-xs">{{ page }} / {{ totalPages }} （共 {{ total }} 条）</span>
      <button class="web3-btn text-xs" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
    </div>

    <!-- Detail modal -->
    <div v-if="detail" class="modal-mask" @click.self="detail = null">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ detail.title || '评论详情' }}</h3>
          <button class="modal-close" @click="detail = null">✕</button>
        </div>
        <div class="modal-meta muted">
          <span>{{ detail.authorName || '匿名' }}</span>
          <span v-if="detail.category">分类：{{ detail.category }}</span>
          <span>{{ formatTime(detail.createdAt) }}</span>
          <span class="status-badge" :class="detail.status">{{ statusLabel(detail.status) }}</span>
        </div>
        <pre class="modal-body">{{ detail.content }}</pre>
      </div>
    </div>

    <transition name="toast">
      <div v-if="toast" class="toast" :class="toast.type">{{ toast.text }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  getAdminPosts, approvePost, rejectPost, deleteAdminPost, batchPosts,
  getAdminComments, approveComment, rejectComment, deleteAdminComment, batchComments
} from '@/api/moderation'
import { confirm as dlgConfirm } from '@/composables/useDialog'
import { formatTime24 as formatTime } from '@/utils/date.js'

const STATUS = {
  pending: { label: '待审核', badge: 'pending' },
  published: { label: '已通过', badge: 'published' },
  rejected: { label: '已驳回', badge: 'rejected' }
}
const statusOptions = [
  { value: 'pending', label: '待审核' },
  { value: 'published', label: '已通过' },
  { value: 'rejected', label: '已驳回' },
  { value: 'all', label: '全部' }
]

const tab = ref('post')
const status = ref('pending')
const keywordInput = ref('')
const keyword = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const detail = ref(null)
const toast = ref(null)
// 跨页保留已选 ID（字符串，与后端 Long 序列化形式一致）；切 Tab 时清空
const selectedIds = ref([])
const batchBusy = ref(false)
let toastTimer = null

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
// 当前页是否全部选中
const allSelected = computed(() =>
  list.value.length > 0 && list.value.every(i => selectedIds.value.includes(String(i.id)))
)

function statusLabel(s) { return STATUS[s]?.label || s || '-' }
function stripMd(text) {
  if (!text) return ''
  return String(text).replace(/[#>*`!\[\]()]/g, '').replace(/\s+/g, ' ').slice(0, 80)
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value, status: status.value, keyword: keyword.value }
    const fn = tab.value === 'post' ? getAdminPosts : getAdminComments
    const res = await fn(params)
    const data = res.data || {}
    list.value = data.records || []
    total.value = Number(data.total) || 0
    // 当前页被清空时自动回退一页
    if (!list.value.length && page.value > 1 && total.value > 0) {
      page.value -= 1
      await loadList()
    }
  } catch (e) {
    showToast(e?.response?.data?.message || '加载失败，请确认已以管理员身份登录', 'error')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function switchTab(t) {
  if (tab.value === t) return
  tab.value = t
  page.value = 1
  clearSelection()
  loadList()
}
function changeStatus(s) {
  if (status.value === s) return
  status.value = s
  page.value = 1
  loadList()
}
function changePage(p) {
  page.value = p
  loadList()
}
function onSearch() {
  keyword.value = keywordInput.value.trim()
  page.value = 1
  loadList()
}
function resetKeyword() {
  keywordInput.value = ''
  keyword.value = ''
  page.value = 1
  loadList()
}

function openDetail(item) { detail.value = item }

async function doApprove(item) {
  const word = tab.value === 'post' ? '帖子' : '评论'
  const ok = await dlgConfirm(`确定通过该${word}吗？通过后将在社区公开展示。`, {
    title: '审核通过', okText: '通过'
  })
  if (!ok) return
  try {
    await (tab.value === 'post' ? approvePost(item.id) : approveComment(item.id))
    showToast('已通过', 'ok')
    await loadList()
  } catch (e) { showToast(e?.response?.data?.message || '操作失败', 'error') }
}

async function doReject(item) {
  const word = tab.value === 'post' ? '帖子' : '评论'
  const ok = await dlgConfirm(`确定驳回该${word}吗？驳回后普通用户将无法看到。`, {
    title: '审核驳回', okText: '驳回'
  })
  if (!ok) return
  try {
    await (tab.value === 'post' ? rejectPost(item.id) : rejectComment(item.id))
    showToast('已驳回', 'ok')
    await loadList()
  } catch (e) { showToast(e?.response?.data?.message || '操作失败', 'error') }
}

async function doDelete(item) {
  const word = tab.value === 'post' ? '帖子' : '评论'
  const ok = await dlgConfirm(`确定删除该${word}吗？删除后社区中立即不可见，且无法在前台恢复。`, {
    title: `删除${word}`, okText: '删除', tone: 'danger'
  })
  if (!ok) return
  try {
    await (tab.value === 'post' ? deleteAdminPost(item.id) : deleteAdminComment(item.id))
    showToast('已删除', 'ok')
    await loadList()
  } catch (e) { showToast(e?.response?.data?.message || '删除失败', 'error') }
}

function showToast(text, type = 'ok') {
  toast.value = { text, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = null }, 2500)
}

// ── 批量选择（可跨页累积） ──
function toggleSelect(id) {
  const i = selectedIds.value.indexOf(id)
  if (i >= 0) selectedIds.value.splice(i, 1)
  else selectedIds.value.push(id)
}
function togglePageAll(checked) {
  const pageIds = list.value.map(i => String(i.id))
  if (checked) {
    selectedIds.value = Array.from(new Set([...selectedIds.value, ...pageIds]))
  } else {
    selectedIds.value = selectedIds.value.filter(id => !pageIds.includes(id))
  }
}
function clearSelection() { selectedIds.value = [] }

const BATCH_META = {
  approve: { title: '批量通过', okText: '全部通过', danger: false,
    msg: (n, w) => `确定将选中的 ${n} 条${w}全部通过吗？通过后将在社区公开展示。` },
  reject: { title: '批量驳回', okText: '全部驳回', danger: false,
    msg: (n, w) => `确定将选中的 ${n} 条${w}全部驳回吗？驳回后普通用户将无法看到。` },
  delete: { title: '批量删除', okText: '全部删除', danger: true,
    msg: (n, w) => `确定删除选中的 ${n} 条${w}吗？删除后社区中立即不可见，且无法在前台恢复。` }
}

async function doBatch(action) {
  const ids = selectedIds.value
  if (!ids.length) return
  const word = tab.value === 'post' ? '帖子' : '评论'
  const meta = BATCH_META[action]
  const ok = await dlgConfirm(meta.msg(ids.length, word), {
    title: meta.title, okText: meta.okText, tone: meta.danger ? 'danger' : 'default'
  })
  if (!ok) return
  batchBusy.value = true
  try {
    const res = await (tab.value === 'post' ? batchPosts(ids, action) : batchComments(ids, action))
    const affected = res.data?.affected ?? ids.length
    const doneText = { approve: '已通过', reject: '已驳回', delete: '已删除' }[action]
    showToast(`${doneText} ${affected} 条${word}`, 'ok')
    clearSelection()
    await loadList()
  } catch (e) {
    showToast(e?.response?.data?.message || '批量操作失败', 'error')
  } finally {
    batchBusy.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.community-page { padding: 0.25rem; }

.tabs { display: flex; gap: 0.5rem; margin-bottom: 0.9rem; }
.tab {
  padding: 0.45rem 1.1rem; border-radius: 10px; font-size: 0.85rem;
  color: rgba(255,255,255,0.45); background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.07); cursor: pointer; transition: all .2s;
}
.tab.active {
  color: #fff; background: linear-gradient(135deg, var(--color-primary-soft), var(--color-accent-soft));
  border-color: var(--color-primary-border); box-shadow: 0 0 14px var(--color-glow-soft);
}

.filters { display: flex; justify-content: space-between; gap: 1rem; flex-wrap: wrap; margin-bottom: 0.9rem; }
.chips { display: flex; gap: 0.4rem; flex-wrap: wrap; }
.chip {
  padding: 0.3rem 0.85rem; border-radius: 999px; font-size: 0.76rem;
  color: rgba(255,255,255,0.5); background: transparent;
  border: 1px solid rgba(255,255,255,0.1); cursor: pointer; transition: all .2s;
}
.chip:hover { color: #fff; border-color: rgba(255,255,255,0.3); }
.chip.active { color: #fff; border-color: var(--color-primary); background: rgba(124,108,240,0.15); }

.search-box { display: flex; gap: 0.4rem; }
.search-box input {
  width: 220px; padding: 0.35rem 0.7rem; border-radius: 8px; font-size: 0.8rem;
  background: rgba(14,14,38,0.8); border: 1px solid rgba(255,255,255,0.08); color: #fff; outline: none;
}
.search-box input:focus { border-color: var(--color-primary-border); }
.web3-btn.ghost { background: transparent; }

.table-wrap {
  background: rgba(15, 18, 40, 0.5); border: 1px solid rgba(255,255,255,0.06);
  border-radius: 14px; overflow-x: auto;
}
.col-check { width: 38px; text-align: center; }
.col-check input[type="checkbox"] {
  width: 14px; height: 14px; accent-color: #7c6cf0; cursor: pointer; vertical-align: middle;
}
.mod-table tr.row-selected { background: rgba(124,108,240,0.08); }
.mod-table tr.row-selected:hover { background: rgba(124,108,240,0.12); }

.batch-bar {
  display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap;
  margin-bottom: 0.7rem; padding: 0.6rem 1rem; border-radius: 12px;
  background: linear-gradient(135deg, rgba(124,108,240,0.16), rgba(124,108,240,0.06));
  border: 1px solid rgba(124,108,240,0.35);
  box-shadow: 0 6px 24px rgba(124,108,240,0.15);
}
.batch-info { display: flex; align-items: center; gap: 0.9rem; }
.batch-count { font-size: 0.82rem; font-weight: 600; color: #c7d2fe; }
.batch-link {
  background: none; border: none; font-size: 0.75rem; color: rgba(255,255,255,0.5);
  cursor: pointer; padding: 0; text-decoration: underline; text-underline-offset: 3px;
}
.batch-link:hover { color: #fff; }
.batch-actions { display: flex; gap: 0.5rem; }
.batch-btn {
  padding: 0.35rem 0.9rem; border-radius: 8px; font-size: 0.76rem; cursor: pointer;
  border: 1px solid; transition: all .2s;
}
.batch-btn:disabled { opacity: .5; cursor: not-allowed; }
.batch-btn.approve { background: rgba(52,211,153,0.12); color: #34d399; border-color: rgba(52,211,153,0.35); }
.batch-btn.reject { background: rgba(245,158,11,0.12); color: #fbbf24; border-color: rgba(245,158,11,0.35); }
.batch-btn.delete { background: rgba(239,68,68,0.12); color: #f87171; border-color: rgba(239,68,68,0.35); }
.batch-btn.approve:not(:disabled):hover { background: rgba(52,211,153,0.22); }
.batch-btn.reject:not(:disabled):hover { background: rgba(245,158,11,0.22); }
.batch-btn.delete:not(:disabled):hover { background: rgba(239,68,68,0.22); }
.bar-slide-enter-active, .bar-slide-leave-active { transition: all .22s ease; }
.bar-slide-enter-from, .bar-slide-leave-to { opacity: 0; transform: translateY(-6px); }
.mod-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; min-width: 820px; }
.mod-table th {
  text-align: left; padding: 0.7rem 0.9rem; color: rgba(255,255,255,0.4);
  font-weight: 500; font-size: 0.74rem; border-bottom: 1px solid rgba(255,255,255,0.06);
  white-space: nowrap;
}
.mod-table td { padding: 0.7rem 0.9rem; border-bottom: 1px solid rgba(255,255,255,0.04); vertical-align: top; }
.mod-table tbody tr:hover { background: rgba(255,255,255,0.02); }
.num { text-align: right; white-space: nowrap; }
.nowrap { white-space: nowrap; }
.muted { color: rgba(255,255,255,0.4); }

.cell-content { max-width: 320px; }
.title-link { color: #fff; font-weight: 600; cursor: pointer; margin-bottom: 3px; }
.title-link:hover { color: var(--color-primary); }
.snippet { color: rgba(255,255,255,0.4); font-size: 0.75rem; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.comment-text { cursor: pointer; color: rgba(255,255,255,0.75); }
.comment-text:hover { color: #fff; }
.pin-tag, .reply-tag {
  display: inline-block; margin-top: 4px; padding: 1px 7px; border-radius: 6px; font-size: 0.68rem;
  background: rgba(245,158,11,0.15); color: #fbbf24; border: 1px solid rgba(245,158,11,0.3);
}
.reply-tag { background: rgba(124,108,240,0.12); color: #a5b4fc; border-color: rgba(124,108,240,0.3); }
.post-title { max-width: 160px; }

.status-badge {
  display: inline-block; padding: 2px 9px; border-radius: 999px; font-size: 0.7rem; white-space: nowrap;
}
.status-badge.pending { background: rgba(245,158,11,0.15); color: #fbbf24; border: 1px solid rgba(245,158,11,0.3); }
.status-badge.published { background: rgba(52,211,153,0.12); color: #34d399; border: 1px solid rgba(52,211,153,0.3); }
.status-badge.rejected { background: rgba(239,68,68,0.12); color: #f87171; border: 1px solid rgba(239,68,68,0.3); }

.row-actions { display: flex; gap: 0.35rem; flex-wrap: wrap; }
.act {
  padding: 0.25rem 0.65rem; border-radius: 7px; font-size: 0.72rem; cursor: pointer;
  background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: rgba(255,255,255,0.7);
}
.act:hover { color: #fff; border-color: rgba(255,255,255,0.3); }
.act.approve { background: rgba(52,211,153,0.12); color: #34d399; border-color: rgba(52,211,153,0.3); }
.act.reject { background: rgba(245,158,11,0.12); color: #fbbf24; border-color: rgba(245,158,11,0.3); }
.act.delete { background: rgba(239,68,68,0.12); color: #f87171; border-color: rgba(239,68,68,0.3); }

.empty-state { display: flex; flex-direction: column; align-items: center; gap: 0.8rem; padding: 3.5rem 0; color: rgba(255,255,255,0.35); }
.empty-icon { font-size: 2.6rem; opacity: .55; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 1rem; margin-top: 1rem; }
.pagination button:disabled { opacity: 0.35; cursor: not-allowed; }

.modal-mask {
  position: fixed; inset: 0; background: rgba(4,4,12,0.7); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center; z-index: 100; padding: 1.5rem;
}
.modal {
  width: min(680px, 100%); max-height: 80vh; display: flex; flex-direction: column;
  background: #0e0e26; border: 1px solid rgba(255,255,255,0.1); border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.5);
}
.modal-head { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.2rem; border-bottom: 1px solid rgba(255,255,255,0.06); }
.modal-head h3 { color: #fff; font-size: 1rem; font-weight: 700; }
.modal-close { background: none; border: none; color: rgba(255,255,255,0.5); cursor: pointer; font-size: 1rem; }
.modal-meta { display: flex; gap: 0.9rem; flex-wrap: wrap; padding: 0.7rem 1.2rem 0; font-size: 0.74rem; align-items: center; }
.modal-body {
  padding: 0.8rem 1.2rem 1.2rem; margin: 0.8rem 1.2rem 1.2rem; overflow-y: auto;
  color: rgba(255,255,255,0.8); font-size: 0.85rem; line-height: 1.75; white-space: pre-wrap;
  word-break: break-word; font-family: inherit;
  background: rgba(255,255,255,0.03); border-radius: 10px;
}

.toast {
  position: fixed; bottom: 28px; left: 50%; transform: translateX(-50%);
  padding: 0.55rem 1.3rem; border-radius: 10px; font-size: 0.82rem; z-index: 200;
  background: rgba(16,185,129,0.15); color: #34d399; border: 1px solid rgba(52,211,153,0.4);
  box-shadow: 0 8px 30px rgba(0,0,0,0.4);
}
.toast.error { background: rgba(239,68,68,0.15); color: #f87171; border-color: rgba(239,68,68,0.4); }
.toast-enter-active, .toast-leave-active { transition: all .25s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(10px); }
</style>

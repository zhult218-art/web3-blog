<template>
  <section class="comment-panel">
    <!-- Header -->
    <header v-if="showTitle" class="cp-header">
      <div class="cp-title-row">
        <span class="cp-title-icon">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.6" d="M7.5 8.25h9m-9 3.75h5.25M3.75 6.75A2.25 2.25 0 016 4.5h12a2.25 2.25 0 012.25 2.25v6.75A2.25 2.25 0 0118 15.75H9.75l-4.286 3.572A.75.75 0 014.5 18.75V6.75z"/></svg>
        </span>
        <h3 class="cp-title">评论</h3>
        <span v-if="total !== null" class="cp-count">{{ total }}</span>
      </div>
      <p class="cp-subtitle">留下你的想法，与作者和其他读者一起交流</p>
    </header>

    <!-- Editor -->
    <div class="cp-editor">
      <template v-if="!auth.isLoggedIn">
        <div class="cp-guest-row">
          <input v-model="form.authorName" placeholder="昵称（必填）" class="cp-input" maxlength="32" />
          <input v-model="form.email" placeholder="邮箱（可选，仅用于回复通知）" class="cp-input" maxlength="64" />
        </div>
      </template>
      <div v-else class="cp-user-row">
        <span class="cp-avatar cp-avatar-sm">
          {{ (auth.user?.nickname || auth.user?.username || 'U')[0].toUpperCase() }}
        </span>
        <span class="cp-username">{{ auth.user?.nickname || auth.user?.username }}</span>
        <span class="cp-badge">已登录</span>
      </div>

      <textarea v-model="form.content" placeholder="写下你的评论，支持 Markdown 语法…" rows="4" class="cp-textarea" maxlength="1000"></textarea>

      <div class="cp-editor-foot">
        <span class="cp-hint">
          <svg class="w-3 h-3 inline -mt-0.5 mr-1 opacity-70" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
          支持 Markdown · {{ form.content.length }}/1000
        </span>
        <button class="cp-submit" :disabled="submitting || !form.content.trim()" @click="submit">
          <svg v-if="!submitting" class="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/></svg>
          {{ submitting ? '提交中…' : '发表评论' }}
        </button>
      </div>
    </div>

    <!-- List -->
    <div class="cp-list">
      <div v-if="loading" class="cp-loading">
        <span class="cp-spinner"></span>
        <span>评论加载中…</span>
      </div>
      <template v-else>
        <div v-for="c in comments" :key="c.id" class="cp-item">
          <span class="cp-avatar">{{ (c.authorName || '匿')[0].toUpperCase() }}</span>
          <div class="cp-item-body">
            <div class="cp-item-head">
              <span class="cp-name">{{ c.authorName || '匿名' }}</span>
              <time class="cp-time">{{ formatDate(c.createdAt) }}</time>
              <div class="cp-actions">
                <button class="cp-act" @click="openReply(c)">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6"/></svg>
                  回复
                </button>
                <button v-if="canDelete(c)" class="cp-act cp-act-danger" @click="remove(c.id)">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M1 7h22M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3"/></svg>
                </button>
              </div>
            </div>
            <div class="cp-content comment-md" v-html="renderMarkdown(c.content || '')"></div>

            <!-- nested replies -->
            <div v-if="c.children && c.children.length" class="cp-replies">
              <div v-for="r in c.children" :key="r.id" class="cp-reply">
                <span class="cp-avatar cp-avatar-sm">{{ (r.authorName || '匿')[0].toUpperCase() }}</span>
                <div class="cp-item-body">
                  <div class="cp-item-head">
                    <span class="cp-name">{{ r.authorName || '匿名' }}</span>
                    <span v-if="r.replyToName" class="cp-reply-to">
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6"/></svg>
                      <span class="text-purple-400">{{ r.replyToName }}</span>
                    </span>
                    <time class="cp-time">{{ formatDate(r.createdAt) }}</time>
                    <div class="cp-actions">
                      <button class="cp-act" @click="openReply(r, c)">回复</button>
                      <button v-if="canDelete(r)" class="cp-act cp-act-danger" @click="remove(r.id)">删除</button>
                    </div>
                  </div>
                  <div class="cp-content comment-md" v-html="renderMarkdown(r.content || '')"></div>
                </div>
              </div>
            </div>

            <!-- reply input -->
            <div v-if="replyingTo === c.id" class="cp-reply-input">
              <textarea v-model="replyText" :placeholder="`回复 ${replyToName || '该评论'}…`" rows="2"
                class="cp-textarea cp-textarea-sm" maxlength="1000"></textarea>
              <div class="cp-reply-foot">
                <button class="cp-ghost-btn" @click="cancelReply">取消</button>
                <button class="cp-submit cp-submit-sm" :disabled="submittingReply || !replyText.trim()" @click="submitReply(c.id)">
                  {{ submittingReply ? '提交中…' : '回复' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="!comments.length" class="cp-empty">
          <svg class="w-10 h-10 mb-2 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7.5 8.25h9m-9 3.75h5.25M3.75 6.75A2.25 2.25 0 016 4.5h12a2.25 2.25 0 012.25 2.25v6.75A2.25 2.25 0 0118 15.75H9.75l-4.286 3.572A.75.75 0 014.5 18.75V6.75z"/></svg>
          <p>还没有评论，快来抢沙发～</p>
        </div>
      </template>
    </div>

    <!-- Load more -->
    <div v-if="hasMore" class="cp-loadmore">
      <button class="cp-loadmore-btn" @click="loadMore">
        <svg class="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3"/></svg>
        加载更多评论
      </button>
    </div>
  </section>
</template>

<script setup>
// ============================================================
// 通用评论面板（CommentPanel）
// 复用 forum-service /comment 接口，targetType 区分评论对象
// 未登录用户可匿名评论（昵称必填）
// ============================================================
import { ref, computed, onMounted, watch } from 'vue'
import { getCommentList, createComment, deleteComment } from '@/api/forum'
import { renderMarkdown } from '@/utils/markdown'
import { formatMinute } from '@/utils/date'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'

const props = defineProps({
  targetId: { type: [String, Number], required: true },
  targetType: { type: String, default: 'ARTICLE' },
  showTitle: { type: Boolean, default: false }
})
const emit = defineEmits(['count'])

const auth = useAuthStore()
const toast = useToastStore()

const comments = ref([])
const total = ref(null)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const submitting = ref(false)
const form = ref({ authorName: '', email: '', content: '' })

// 楼中楼回复状态（始终挂在顶级评论 id 上，replyToName 用于展示"回复 xxx"）
const replyingTo = ref(null)
const replyText = ref('')
const replyToName = ref('')
const submittingReply = ref(false)

const hasMore = computed(() => comments.value.length < (total.value || 0))

// 日期格式化（统一走 utils/date）
function formatDate(d) { return formatMinute(d, '') }

function canDelete(c) {
  if (auth.user?.role === 'ADMIN') return true
  return c.authorId && auth.user && String(c.authorId) === String(auth.user.id)
}

// 打开某个评论的回复框；无论点击顶级评论还是其子回复，都挂在顶级评论 id 下（保持两级楼中楼）
function openReply(comment, topComment) {
  const top = topComment || comment
  replyingTo.value = top.id
  replyToName.value = comment.authorName || '该评论'
  replyText.value = ''
}

function cancelReply() {
  replyingTo.value = null
  replyText.value = ''
  replyToName.value = ''
}

// 提交楼中楼回复：parentId 指向顶级评论，replyToName 指向被回复人
async function submitReply(topId) {
  const content = replyText.value.trim()
  if (!content) return
  const isLoggedIn = auth.isLoggedIn
  if (!isLoggedIn) {
    if (!form.value.authorName.trim()) { toast.error('请填写昵称'); return }
  }
  submittingReply.value = true
  try {
    await createComment({
      targetId: props.targetId,
      targetType: props.targetType,
      content,
      parentId: topId,
      replyToName: replyToName.value === '该评论' ? null : replyToName.value,
      authorName: isLoggedIn ? (auth.user?.nickname || auth.user?.username) : form.value.authorName.trim()
    })
    replyText.value = ''
    replyingTo.value = null
    page.value = 1
    await fetchList()
    toast.success('回复成功')
  } catch {
    toast.error('回复失败')
  } finally {
    submittingReply.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getCommentList(props.targetId, props.targetType, { page: page.value, size: size.value })
    const records = res?.data?.records ?? res?.records ?? []
    comments.value = page.value === 1 ? records : [...comments.value, ...records]
    total.value = res?.data?.total ?? res?.total ?? null
    emit('count', total.value)
  } catch {
    comments.value = []
    total.value = 0
    emit('count', 0)
  } finally {
    loading.value = false
  }
}

async function submit() {
  const content = form.value.content.trim()
  if (!content) return
  const isLoggedIn = auth.isLoggedIn
  if (!isLoggedIn) {
    if (!form.value.authorName.trim()) { toast.error('请填写昵称'); return }
  }
  submitting.value = true
  try {
    await createComment({
      targetId: props.targetId,
      targetType: props.targetType,
      content,
      authorName: isLoggedIn ? (auth.user?.nickname || auth.user?.username) : form.value.authorName.trim()
    })
    form.value.content = ''
    page.value = 1
    await fetchList()
    toast.success('评论成功')
  } catch {
    toast.error('评论失败')
  } finally {
    submitting.value = false
  }
}

async function remove(id) {
  try {
    await deleteComment(id)
    // 可能是顶级评论或楼内回复，先尝试顶层，再在每家 children 中查找
    const removed = comments.value.some((c, i) => {
      if (String(c.id) === String(id)) { comments.value.splice(i, 1); return true }
      if (c.children) {
        const idx = c.children.findIndex(r => String(r.id) === String(id))
        if (idx >= 0) { c.children.splice(idx, 1); return true }
      }
      return false
    })
    if (removed && total.value != null) total.value = Math.max(0, total.value - 1)
    emit('count', total.value)
    toast.success('已删除')
  } catch { /* 拦截器已提示 */ }
}

function loadMore() {
  page.value += 1
  fetchList()
}

watch(() => props.targetId, () => { page.value = 1; fetchList() })
onMounted(fetchList)
</script>

<style scoped>
.comment-panel {
  @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm p-6 md:p-7;
}

/* ---------- Header ---------- */
.cp-header { @apply mb-5; }
.cp-title-row { @apply flex items-center gap-2.5; }
.cp-title-icon {
  @apply w-8 h-8 rounded-xl flex items-center justify-center text-white;
  background: linear-gradient(135deg, rgba(168,85,247,0.35), rgba(34,211,238,0.35));
  border: 1px solid rgba(255,255,255,0.08);
}
.cp-title { @apply text-base font-bold text-white; }
.cp-count {
  @apply text-xs font-semibold px-2 py-0.5 rounded-full text-cyan-300;
  background: rgba(34,211,238,0.12); border: 1px solid rgba(34,211,238,0.2);
}
.cp-subtitle { @apply text-xs text-gray-500 mt-2; }

/* ---------- Editor ---------- */
.cp-editor {
  @apply rounded-xl border border-white/[0.08] bg-white/[0.015] p-4 mb-6 transition-colors;
}
.cp-editor:focus-within { @apply border-purple-500/30; }
.cp-guest-row { @apply flex flex-wrap gap-3 mb-3; }
.cp-input {
  @apply flex-1 min-w-[160px] px-3 py-2 rounded-lg text-sm bg-[#141432] border border-white/[0.10]
    text-gray-200 placeholder:text-gray-500 focus:outline-none focus:border-purple-500/50 focus:bg-[#1a1a44] transition-colors;
}
.cp-user-row { @apply flex items-center gap-2 mb-3; }
.cp-username { @apply text-xs text-gray-300; }
.cp-badge {
  @apply text-[10px] px-1.5 py-0.5 rounded text-emerald-300;
  background: rgba(16,185,129,0.12); border: 1px solid rgba(16,185,129,0.2);
}
.cp-textarea {
  @apply w-full px-4 py-3 rounded-lg text-sm bg-[#10102e] border border-white/[0.10] text-gray-200
    placeholder:text-gray-600 focus:outline-none focus:border-purple-500/40 focus:bg-[#15153a]
    transition-colors resize-y leading-relaxed;
}
.cp-editor-foot { @apply flex items-center justify-between mt-3; }
.cp-hint { @apply text-[11px] text-gray-600; }
.cp-submit {
  @apply inline-flex items-center px-5 py-2 rounded-lg text-xs font-medium text-white
    disabled:opacity-40 disabled:cursor-not-allowed transition-all hover:shadow-[0_0_20px_rgba(168,85,247,0.35)];
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
}
.cp-submit-sm { @apply px-3.5 py-1.5; }

/* ---------- List ---------- */
.cp-list { @apply space-y-1; }
.cp-loading { @apply py-10 flex items-center justify-center gap-2 text-xs text-gray-500; }
.cp-spinner {
  @apply inline-block w-4 h-4 rounded-full border-2 border-white/15 border-t-cyan-400;
  animation: cpspin 0.7s linear infinite;
}
@keyframes cpspin { to { transform: rotate(360deg); } }

.cp-item { @apply flex gap-3 py-4 border-b border-white/[0.05] last:border-0; }
.cp-item-body { @apply min-w-0 flex-1; }
.cp-item-head { @apply flex items-center gap-2 text-xs flex-wrap; }
.cp-name { @apply font-medium text-gray-200; }
.cp-time { @apply text-gray-600; }
.cp-actions { @apply ml-auto flex items-center gap-1 opacity-0 transition-opacity; }
.cp-item:hover .cp-actions { @apply opacity-100; }
.cp-act {
  @apply inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] text-gray-500
    hover:text-purple-300 hover:bg-white/[0.05] transition-colors;
}
.cp-act-danger { @apply hover:text-red-400; }
.cp-reply-to { @apply inline-flex items-center gap-0.5 text-gray-500; }
.cp-content { @apply text-sm text-gray-300 mt-1.5 break-words leading-[1.7]; }

/* ---------- Nested replies ---------- */
.cp-replies {
  @apply mt-3 ml-1 pl-4 space-y-2.5;
  border-left: 2px solid;
  border-image: linear-gradient(to bottom, rgba(168,85,247,0.5), rgba(34,211,238,0.15)) 1;
}
.cp-reply { @apply flex gap-2.5; }
.cp-reply-input {
  @apply mt-3 rounded-lg border border-white/[0.08] bg-[#10102e] p-3;
}
.cp-reply-foot { @apply flex items-center justify-end gap-2 mt-2; }
.cp-ghost-btn {
  @apply px-3 py-1.5 rounded-lg text-[11px] text-gray-400 hover:text-gray-200 hover:bg-white/[0.05] transition-colors;
}

/* ---------- Avatars ---------- */
.cp-avatar {
  @apply w-9 h-9 shrink-0 rounded-full flex items-center justify-center text-sm font-bold text-white select-none;
  background: linear-gradient(135deg, rgba(168,85,247,0.55), rgba(34,211,238,0.55));
}
.cp-avatar-sm { @apply w-7 h-7 text-xs; }

/* ---------- Empty / Load more ---------- */
.cp-empty { @apply py-12 flex flex-col items-center justify-center text-center text-xs text-gray-600; }
.cp-loadmore { @apply mt-5 text-center; }
.cp-loadmore-btn {
  @apply inline-flex items-center px-4 py-2 rounded-lg text-xs text-gray-400
    border border-white/[0.08] hover:text-white hover:border-white/[0.18] hover:bg-white/[0.03] transition-all;
}

/* ---------- Markdown inside comments ---------- */
.comment-md :deep(p) { margin: 0.25em 0; }
.comment-md :deep(p:first-child) { margin-top: 0; }
.comment-md :deep(p:last-child) { margin-bottom: 0; }
.comment-md :deep(a) { color: var(--color-primary); text-decoration: underline; text-underline-offset: 3px; }
.comment-md :deep(code) {
  padding: 0.1em 0.35em; border-radius: 4px; font-size: 0.9em;
  background: rgba(168, 85, 247, 0.12); color: #c4b5fd;
}
.comment-md :deep(pre) { padding: 0.6em 0.8em; border-radius: 8px; overflow-x: auto; background: #10102e; margin: 0.5em 0; }
.comment-md :deep(pre code) { background: transparent; color: inherit; }
.comment-md :deep(blockquote) { border-left: 2px solid var(--color-primary); padding-left: 0.75em; color: #9ca3af; margin: 0.5em 0; }
.comment-md :deep(img) { @apply rounded-lg max-w-full; }
</style>

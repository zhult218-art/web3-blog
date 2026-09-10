<template>
  <section class="panel p-6">
    <h3 v-if="showTitle" class="text-sm font-bold text-white mb-4">
      评论 <span v-if="total !== null" class="text-gray-500 font-normal text-xs">({{ total }})</span>
    </h3>

    <!-- 输入框 -->
    <div class="space-y-3">
      <!-- 游客才显示昵称/邮箱 -->
      <div v-if="!auth.isLoggedIn" class="flex flex-wrap gap-3">
        <input v-model="form.authorName" placeholder="昵称（必填）" class="input" maxlength="32" />
        <input v-model="form.email" placeholder="邮箱（可选，仅用于回复通知）" class="input" maxlength="64" />
      </div>
      <div v-else class="flex items-center gap-2 text-xs text-gray-400">
        <span class="w-6 h-6 rounded-full bg-gradient-to-br from-purple-500/60 to-cyan-500/60 flex items-center justify-center text-[10px] font-bold text-white">
          {{ (auth.user?.nickname || auth.user?.username || 'U')[0].toUpperCase() }}
        </span>
        <span>{{ auth.user?.nickname || auth.user?.username }}</span>
      </div>
      <textarea v-model="form.content" placeholder="说点什么吧…（支持 Markdown）" rows="3" class="input resize-y" maxlength="1000"></textarea>
      <div class="flex items-center justify-between">
        <span class="text-[10px] text-gray-600">{{ form.content.length }}/1000</span>
        <button class="submit-btn" :disabled="submitting || !form.content.trim()" @click="submit">
          {{ submitting ? '提交中…' : '发表评论' }}
        </button>
      </div>
    </div>

    <!-- 评论列表 -->
    <div class="mt-6 space-y-4">
      <div v-if="loading" class="py-8 text-center text-xs text-gray-500">评论加载中...</div>
      <template v-else>
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <!-- 顶级评论头 -->
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 shrink-0 rounded-full bg-gradient-to-br from-purple-500/60 to-cyan-500/60 flex items-center justify-center text-xs font-bold text-white">
              {{ (c.authorName || '匿')[0].toUpperCase() }}
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2 text-xs">
                <span class="font-medium text-gray-200">{{ c.authorName || '匿名' }}</span>
                <time class="text-gray-600">{{ formatDate(c.createdAt) }}</time>
                <div class="ml-auto flex items-center gap-2">
                  <button class="text-gray-600 hover:text-purple-400 transition-colors" @click="openReply(c)">回复</button>
                  <button v-if="canDelete(c)" class="text-gray-600 hover:text-red-400 transition-colors" @click="remove(c.id)">删除</button>
                </div>
              </div>
              <div class="text-sm text-gray-300 mt-1 break-words comment-md" v-html="renderMarkdown(c.content || '')"></div>
            </div>
          </div>

          <!-- 该评论下的楼中楼回复列表（递归渲染） -->
          <div v-if="c.children && c.children.length" class="ml-4 mt-3 border-l border-white/[0.08] pl-4 space-y-3">
            <div v-for="r in c.children" :key="r.id" class="reply-item">
              <div class="flex items-start gap-2.5">
                <div class="w-7 h-7 shrink-0 rounded-full bg-gradient-to-br from-cyan-500/50 to-purple-500/50 flex items-center justify-center text-[10px] font-bold text-white">
                  {{ (r.authorName || '匿')[0].toUpperCase() }}
                </div>
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 text-xs">
                    <span class="font-medium text-gray-300">{{ r.authorName || '匿名' }}</span>
                    <span v-if="r.replyToName" class="text-gray-500">回复 <span class="text-purple-400">{{ r.replyToName }}</span></span>
                    <time class="text-gray-600">{{ formatDate(r.createdAt) }}</time>
                    <div class="ml-auto flex items-center gap-2">
                      <button class="text-gray-600 hover:text-purple-400 transition-colors" @click="openReply(r, c)">回复</button>
                      <button v-if="canDelete(r)" class="text-gray-600 hover:text-red-400 transition-colors" @click="remove(r.id)">删除</button>
                    </div>
                  </div>
                  <div class="text-sm text-gray-300 mt-1 break-words comment-md" v-html="renderMarkdown(r.content || '')"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- 楼内回复输入框 -->
          <div v-if="replyingTo === c.id" class="mt-3 ml-4 flex items-start gap-2">
            <textarea v-model="replyText" :placeholder="`回复 ${replyToName || '该评论'}…`" rows="2"
              class="input resize-y flex-1" maxlength="1000"></textarea>
            <div class="flex flex-col gap-1.5">
              <button class="submit-btn text-xs" :disabled="submittingReply || !replyText.trim()" @click="submitReply(c.id)">{{ submittingReply ? '…' : '回复' }}</button>
              <button class="text-[11px] text-gray-500 hover:text-gray-300" @click="cancelReply">取消</button>
            </div>
          </div>
        </div>
        <div v-if="!comments.length" class="py-8 text-center text-xs text-gray-600">还没有评论，快来抢沙发～</div>
      </template>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore" class="mt-4 text-center">
      <button class="load-more-btn" @click="loadMore">加载更多评论</button>
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

// 楼中楼回复状态（始终挂在顶级评论 id 上，replyToName 用于展示“回复 xxx”）
const replyingTo = ref(null)
const replyText = ref('')
const replyToName = ref('')
const submittingReply = ref(false)

const hasMore = computed(() => comments.value.length < (total.value || 0))

function formatDate(d) { return d ? String(d).slice(0, 16).replace('T', ' ') : '' }

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
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }
.input {
  @apply flex-1 min-w-[160px] px-3 py-2 rounded-lg text-sm bg-[#141432] border border-white/[0.10]
    text-gray-200 placeholder:text-gray-500 focus:outline-none focus:border-purple-500/50 focus:bg-[#1a1a44] transition-colors;
}
.submit-btn {
  @apply px-5 py-2 rounded-lg text-xs font-medium text-white disabled:opacity-40 disabled:cursor-not-allowed transition-all;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
}
.load-more-btn { @apply px-4 py-1.5 rounded-lg text-xs text-gray-400 border border-white/[0.08] hover:text-white hover:border-white/[0.15] transition-colors; }
.comment-item { @apply p-4 rounded-xl border border-white/[0.08] hover:border-white/[0.14] transition-colors; }
.reply-item { @apply p-3 rounded-lg bg-white/[0.02] hover:bg-white/[0.04] transition-colors; }
.comment-md :deep(p) { margin: 0.25em 0; }
.comment-md :deep(p:first-child) { margin-top: 0; }
.comment-md :deep(p:last-child) { margin-bottom: 0; }
.comment-md :deep(a) { color: var(--color-primary); text-decoration: underline; }
.comment-md :deep(code) {
  padding: 0.1em 0.35em; border-radius: 4px; font-size: 0.9em;
  background: rgba(168, 85, 247, 0.12); color: #c4b5fd;
}
.comment-md :deep(pre) { padding: 0.6em 0.8em; border-radius: 8px; overflow-x: auto; background: #10102e; margin: 0.5em 0; }
.comment-md :deep(pre code) { background: transparent; color: inherit; }
.comment-md :deep(blockquote) { border-left: 2px solid var(--color-primary); padding-left: 0.75em; color: #9ca3af; margin: 0.5em 0; }
</style>

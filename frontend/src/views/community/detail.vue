<template>
  <div class="min-h-screen px-4 md:px-6 py-6">
    <div class="mx-auto max-w-[1400px]">
      <!-- Reading Progress Bar -->
      <div class="fixed top-0 left-0 right-0 z-40 h-[2px] bg-transparent">
        <div class="h-full bg-gradient-to-r from-purple-500 to-cyan-500 transition-all duration-150" :style="{ width: readingProgress + '%' }"></div>
      </div>

      <!-- Back -->
      <button class="text-gray-400 hover:text-white mb-6 flex items-center gap-1.5 text-xs transition-colors group" @click="$router.back()">
        <svg class="w-4 h-4 group-hover:-translate-x-0.5 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
        返回社区
      </button>

      <!-- Article Card -->
      <article v-if="detail" class="glass-panel overflow-hidden">
        <!-- Media header -->
        <div v-if="detail.coverImage" class="relative overflow-hidden">
          <img :src="detail.coverImage" class="w-full h-64 md:h-80 object-cover" />
          <div class="absolute inset-0 bg-gradient-to-t from-[#0c0c28]/90 via-[#0c0c28]/30 to-transparent"></div>
          <div class="absolute bottom-4 left-6 right-6">
            <div class="flex items-center gap-2 mb-2">
              <span :class="['text-[10px] px-2.5 py-1 rounded-lg font-medium',
                detail.type === 'article'
                  ? 'bg-purple-500/20 text-purple-300 border border-purple-400/20'
                  : 'bg-cyan-500/20 text-cyan-300 border border-cyan-400/20']">
                {{ detail.type === 'article' ? '文章' : '讨论' }}
              </span>
              <span v-if="detail.mediaType === 'video'" class="text-[10px] px-2.5 py-1 rounded-lg bg-pink-500/20 text-pink-300 border border-pink-400/20">视频</span>
            </div>
          </div>
        </div>

        <!-- Video player -->
        <div v-if="detail.mediaUrl && detail.mediaType === 'video'" class="relative bg-black/60">
          <video :src="detail.mediaUrl" controls class="w-full max-h-[480px]" preload="metadata"></video>
        </div>

        <div class="p-6 md:p-8 lg:p-10">
          <!-- Meta -->
          <div class="flex items-center gap-4 mb-6">
            <div class="w-12 h-12 rounded-full bg-gradient-to-br from-purple-500 to-cyan-500 flex items-center justify-center text-sm font-bold text-white shadow-lg shadow-purple-500/20">
              {{ (detail.authorName || 'U')[0] }}
            </div>
            <div class="flex-1">
              <p class="text-sm text-white font-medium">{{ detail.authorName || '匿名用户' }}</p>
              <div class="flex items-center gap-3 text-[11px] text-gray-500 mt-1">
                <span class="flex items-center gap-1">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                  {{ formatDate(detail.createdAt) }}
                </span>
                <span class="text-white/10">|</span>
                <span class="flex items-center gap-1">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/></svg>
                  {{ detail.category || '综合' }}
                </span>
                <span class="text-white/10">|</span>
                <span class="flex items-center gap-1">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>
                  {{ detail.viewCount || 0 }} 次阅读
                </span>
                <span class="text-white/10">|</span>
                <span class="flex items-center gap-1">
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                  约 {{ readingTime }} 分钟阅读
                </span>
              </div>
            </div>
          </div>

          <!-- Title -->
          <h1 class="text-2xl md:text-3xl lg:text-4xl font-black text-white leading-tight mb-4">{{ detail.title }}</h1>
          <div class="holo-bar mb-8"></div>

          <!-- Content -->
          <div class="article-body text-gray-300 leading-[1.85] text-[15px] mb-8" v-html="contentHtml"></div>

          <!-- Tags -->
          <div v-if="detail.tags" class="flex flex-wrap gap-2 mb-8">
            <span v-for="tag in detail.tags.split(',')" :key="tag"
              class="text-xs px-3 py-1.5 rounded-lg bg-purple-500/10 text-purple-300 border border-purple-400/15 hover:bg-purple-500/20 transition cursor-pointer">
              # {{ tag.trim() }}
            </span>
          </div>

          <!-- Action bar -->
          <div class="flex items-center gap-3 pt-6 border-t border-white/[0.06]">
            <button class="flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm transition-all duration-200"
              :class="liked ? 'bg-pink-500/15 text-pink-400 border border-pink-400/20' : 'border border-white/[0.08] text-gray-400 hover:text-pink-400 hover:border-pink-400/20'"
              @click="doLike">
              <svg class="w-5 h-5" :class="liked ? 'fill-pink-400/20' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/></svg>
              {{ liked ? '已点赞' : '点赞' }} {{ detail.likeCount || 0 }}
            </button>
            <button class="flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm border border-white/[0.08] text-gray-400 hover:text-cyan-400 hover:border-cyan-400/20 transition-all">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
              {{ commentCount }} 评论
            </button>
            <div class="ml-auto flex items-center gap-2">
              <button class="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm border border-white/[0.08] text-gray-400 hover:text-purple-400 hover:border-purple-400/20 transition-all"
                :class="bookmarked ? 'text-purple-400 border-purple-400/20 bg-purple-500/10' : ''"
                @click="doBookmark">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/></svg>
                {{ bookmarked ? '已收藏' : '收藏' }}
              </button>
              <button class="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm border border-white/[0.08] text-gray-400 hover:text-cyan-400 hover:border-cyan-400/20 transition-all" @click="sharePost">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z"/></svg>
                分享
              </button>
            </div>
          </div>
        </div>
      </article>

      <div v-else class="py-16"><Loading /></div>

      <!-- Comments Section -->
      <div class="mt-8">
        <div class="glass-panel p-6 md:p-8">
          <h3 class="text-lg font-bold text-white mb-6 flex items-center gap-2">
            <svg class="w-5 h-5 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
            评论区 ({{ comments.length }})
          </h3>

          <!-- Comment input -->
          <div class="mb-8">
            <div class="flex gap-4">
              <div class="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500/30 to-cyan-500/30 flex items-center justify-center text-sm font-bold text-white/80 flex-shrink-0">
                {{ currentUserInitial }}
              </div>
              <div class="flex-1">
                <textarea v-model="commentText" class="web3-input !min-h-[80px] resize-none" placeholder="写下你的评论... 支持 Markdown 语法" rows="3"></textarea>
                <div class="flex items-center justify-between mt-3">
                  <span class="text-[10px] text-gray-600">{{ commentText.length }}/500</span>
                  <button class="web3-btn text-xs !px-5 !py-2" :disabled="!commentText.trim()" @click="submitComment">
                    发布评论
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Comment list -->
          <div v-if="comments.length" class="space-y-1">
            <div v-for="comment in comments" :key="comment.id" class="group py-5 border-b border-white/[0.04] last:border-0">
              <div class="flex gap-4">
                <div class="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500/20 to-cyan-500/20 flex items-center justify-center text-xs font-bold text-white/70 flex-shrink-0">
                  {{ (comment.authorName || 'U')[0] }}
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1.5">
                    <span class="text-sm font-medium text-white">{{ comment.authorName || '匿名用户' }}</span>
                    <span v-if="comment.isAuthor" class="text-[10px] px-1.5 py-0.5 rounded bg-cyan-500/15 text-cyan-400 border border-cyan-400/15">作者</span>
                    <span class="text-[11px] text-gray-600">{{ formatDate(comment.createdAt) }}</span>
                  </div>
                  <p class="text-sm text-gray-300 leading-relaxed">{{ comment.content }}</p>
                  <div class="flex items-center gap-4 mt-3">
                    <button class="text-[11px] text-gray-600 hover:text-pink-400 transition-colors flex items-center gap-1">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/></svg>
                      {{ comment.likeCount || 0 }}
                    </button>
                    <button class="text-[11px] text-gray-600 hover:text-cyan-400 transition-colors">回复</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="py-12 text-center">
            <div class="text-3xl mb-3 opacity-15">💬</div>
            <p class="text-xs text-gray-600">还没有评论，来说两句吧</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBlogDetail, likeBlog, favoriteBlog, checkBlogFavorite, checkBlogLike } from '@/api/blog'
import { getForumDetail } from '@/api/forum'
import { likePost, getCommentList, createComment } from '@/api/forum'
import { renderMarkdown } from '@/utils/markdown'
import { useToastStore } from '@/stores/modules/toast'
import { useAuthStore } from '@/stores/modules/auth'
import Loading from '@/components/common/Loading.vue'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const authStore = useAuthStore()
const detail = ref(null)
const comments = ref([])
const liked = ref(false)
const bookmarked = ref(false)
const commentText = ref('')
const readingProgress = ref(0)
const type = route.params.type
const id = route.params.id
const isArticle = type === 'article' || type === 'blog'

const currentUserInitial = computed(() => {
  const n = authStore.user?.nickname || authStore.user?.username || '访'
  return n[0]?.toUpperCase()
})

const commentCount = computed(() => comments.value.length)

// 正文按 Markdown 渲染（社区帖子正文存的是 markdown 源码）
const contentHtml = computed(() => detail.value?.content ? renderMarkdown(detail.value.content) : '')

const readingTime = computed(() => {
  if (!detail.value?.content) return 1
  const words = detail.value.content.length
  return Math.max(1, Math.ceil(words / 500))
})

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function handleScroll() {
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  const scrollHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight
  readingProgress.value = scrollHeight > 0 ? (scrollTop / scrollHeight) * 100 : 0
}

async function loadComments() {
  // 博客文章不在社区页加载评论（已跳转博客详情页）
  if (isArticle) return
  try {
    const res = await getCommentList(id, 'post', { page: 1, size: 100 })
    comments.value = res.data?.records || res.records || []
  } catch (e) {
    console.warn('Failed to load comments:', e)
  }
}

async function loadDetail() {
  // 博客文章（web3_blog）走博客详情页渲染，社区页只读论坛库（web3_forum），两者分库不混用
  if (isArticle) {
    router.replace(`/blog/post/${id}`)
    return
  }
  try {
    const res = await getForumDetail(id)
    const data = res.data || res
    detail.value = data.article || data
    if (!detail.value || !detail.value.title) {
      toast.error('内容不存在或已被删除')
    }
  } catch (e) {
    console.warn('Failed to load detail:', e)
    toast.error('加载失败，请稍后重试')
  }
}

async function doLike() {
  try {
    if (isArticle) {
      await likeBlog(id)
    } else {
      await likePost(id)
    }
    detail.value.likeCount = (detail.value.likeCount || 0) + (liked.value ? -1 : 1)
    liked.value = !liked.value
    toast.success(liked.value ? '点赞成功' : '已取消点赞')
  } catch { toast.error('请先登录') }
}

async function doBookmark() {
  if (!isArticle) {
    bookmarked.value = !bookmarked.value
    toast.success(bookmarked.value ? '已加入收藏' : '已取消收藏')
    return
  }
  try {
    await favoriteBlog(id)
    bookmarked.value = !bookmarked.value
    toast.success(bookmarked.value ? '已加入收藏' : '已取消收藏')
  } catch { toast.error('请先登录') }
}

function sharePost() {
  const url = window.location.href
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url)
    toast.success('链接已复制到剪贴板')
  } else {
    toast.info('请手动复制地址栏链接分享')
  }
}

async function submitComment() {
  if (!commentText.value.trim()) return
  try {
    const user = authStore.user
    const authorName = user?.nickname || user?.username || '访客'
    const res = await createComment({
      targetId: id,
      targetType: isArticle ? 'article' : 'post',
      content: commentText.value,
      authorName
    })
    const saved = res.data || {}
    comments.value.push({
      id: saved.id || Date.now(),
      content: commentText.value,
      authorName,
      createdAt: saved.createdAt || new Date().toISOString(),
      likeCount: 0
    })
    commentText.value = ''
    toast.success('评论发布成功')
  } catch (e) {
    toast.error(e.message || '评论失败')
  }
}

onMounted(() => {
  loadDetail()
  loadComments()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.prose-content {
  line-height: 1.85;
  word-break: break-word;
}
.prose-content p {
  margin-bottom: 1em;
}
.prose-content h1, .prose-content h2, .prose-content h3 {
  color: #fff;
  font-weight: 700;
  margin: 1.5em 0 0.75em;
}
.prose-content h1 { font-size: 1.5em; }
.prose-content h2 { font-size: 1.3em; }
.prose-content h3 { font-size: 1.15em; }
.prose-content code {
  background: rgba(255,255,255,0.06);
  padding: 0.15em 0.4em;
  border-radius: 4px;
  font-size: 0.9em;
}
.prose-content pre {
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 8px;
  padding: 1em;
  overflow-x: auto;
  margin: 1em 0;
}
.prose-content pre code {
  background: none;
  padding: 0;
}
.prose-content blockquote {
  border-left: 3px solid rgba(168,85,247,0.4);
  padding-left: 1em;
  color: #9ca3af;
  margin: 1em 0;
}
.prose-content a {
  color: #22d3ee;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.prose-content ul, .prose-content ol {
  padding-left: 1.5em;
  margin: 1em 0;
}
.prose-content li {
  margin-bottom: 0.4em;
}
.prose-content img {
  border-radius: 8px;
  max-width: 100%;
  margin: 1em 0;
}
</style>

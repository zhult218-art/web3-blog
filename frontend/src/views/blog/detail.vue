<template>
  <div v-if="article" class="space-y-6">
    <!-- Reading Progress Bar -->
    <div class="fixed top-0 left-0 right-0 z-50 h-[3px] bg-transparent">
      <div class="h-full bg-gradient-to-r from-purple-500 via-pink-500 to-cyan-500 transition-all duration-150" :style="{ width: readingProgress + '%' }"></div>
    </div>

    <!-- 面包屑导航 -->
    <nav class="flex items-center gap-1.5 text-sm text-white/50 mb-4">
      <router-link to="/" class="hover:text-white/80 transition-colors">首页</router-link>
      <span class="text-white/30">/</span>
      <router-link to="/blog" class="hover:text-white/80 transition-colors">博客</router-link>
      <span v-if="article.category" class="text-white/30">/</span>
      <router-link v-if="article.category" :to="`/blog?category=${article.category}`" class="hover:text-white/80 transition-colors">{{ article.category }}</router-link>
    </nav>

    <!-- 文章头 -->
    <div class="panel p-6 md:p-8">
      <div class="flex flex-wrap items-center gap-2 text-xs text-gray-500 mb-3">
        <span class="px-2 py-0.5 rounded-md bg-purple-500/15 text-purple-300 border border-purple-500/20">{{ article.category || '未分类' }}</span>
        <span class="opacity-50">·</span>
        <time>{{ formatDate(article.createdAt) }}</time>
        <template v-if="formatDate(article.updatedAt) !== formatDate(article.createdAt)">
          <span class="opacity-50">·</span>
          <span>更新于 {{ formatDate(article.updatedAt) }}</span>
        </template>
      </div>
      <h1 class="text-2xl md:text-3xl font-black text-white leading-snug mb-3">{{ article.title }}</h1>
      <!-- 文章标签 -->
      <div v-if="article.tags" class="flex flex-wrap gap-2 mb-4">
        <router-link v-for="tag in article.tags.split(',')" :key="tag"
          :to="`/blog?tag=${tag.trim()}`"
          class="px-2.5 py-0.5 rounded-full text-xs bg-white/5 text-white/60 hover:bg-white/10 hover:text-white/80 transition-colors">
          #{{ tag.trim() }}
        </router-link>
      </div>
      <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-gray-500">
        <span>字数 {{ wordCount }}</span>
        <span>阅读 {{ readingMin }} 分钟</span>
        <span v-if="commentTotal !== null">评论 {{ commentTotal }}</span>
        <span>浏览 {{ article.viewCount ?? 0 }}</span>
        <span class="flex items-center gap-1 cursor-pointer hover:text-pink-300 transition-colors" @click="toggleLike">
          <svg class="w-3.5 h-3.5" :class="liked ? 'text-pink-500 fill-pink-500' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/></svg>
          {{ article.likeCount ?? 0 }}
        </span>
        <span class="flex items-center gap-1 cursor-pointer hover:text-amber-300 transition-colors" @click="toggleFavorite" :title="favorited ? '取消收藏' : '收藏'">
          <svg class="w-3.5 h-3.5" :class="favorited ? 'text-amber-400 fill-amber-400' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11.48 3.5a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.5.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.562.562 0 01.321-.988l5.518-.441a.563.563 0 00.475-.345L11.48 3.5z"/></svg>
          收藏
        </span>
        <span class="opacity-30">|</span>
        <span class="flex items-center gap-1 cursor-pointer hover:text-cyan-300 transition-colors" @click="shareArticle" title="分享">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z"/></svg>
          分享
        </span>
      </div>
    </div>

    <!-- 作者信息 -->
    <div v-if="article.authorName || article.author" class="panel p-4 flex items-center gap-4">
      <div class="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-cyan-500 flex items-center justify-center text-sm font-bold text-white shadow-lg shadow-purple-500/20 shrink-0">
        {{ ((article.authorName || article.author || 'U')[0]) }}
      </div>
      <div class="flex-1 min-w-0">
        <p class="text-sm text-white font-medium">{{ article.authorName || article.author }}</p>
        <p class="text-[11px] text-gray-500 mt-0.5">发布于 {{ formatDate(article.createdAt) }}</p>
      </div>
    </div>

    <!-- 封面（外链加载失败时显示渐变占位，避免白框） -->
    <div v-if="article.cover" class="w-full rounded-2xl border border-white/[0.06] overflow-hidden">
      <div v-if="coverFailed" class="w-full aspect-[2/1] flex items-center justify-center bg-gradient-to-br from-purple-500/25 via-[rgba(10,10,40,0.6)] to-cyan-500/20 text-white/50 text-3xl">✦</div>
      <img
        v-else
        :src="article.cover"
        :alt="article.title"
        referrerpolicy="no-referrer"
        @error="coverFailed = true"
        class="w-full object-cover"
      />
    </div>

    <!-- 正文 + 目录 -->
    <div class="grid grid-cols-1 xl:grid-cols-[1fr_240px] gap-6 items-start">
      <div class="panel p-6 md:p-8 article-body" v-html="contentHtml"></div>
      <PostToc v-if="toc.length" :toc="toc" class="hidden xl:block sticky top-24" />
    </div>

    <!-- 上/下一篇 -->
    <div class="grid grid-cols-2 gap-4">
      <router-link v-if="prev" :to="`/blog/post/${prev.id}`" class="panel p-4 group">
        <p class="text-[10px] text-gray-500 mb-1">上一篇</p>
        <p class="text-sm text-gray-300 group-hover:text-[color:var(--color-primary)] transition-colors line-clamp-2">{{ prev.title }}</p>
      </router-link>
      <div v-else class="panel p-4 flex items-center text-xs text-gray-600">已是第一篇</div>
      <router-link v-if="next" :to="`/blog/post/${next.id}`" class="panel p-4 group text-right">
        <p class="text-[10px] text-gray-500 mb-1">下一篇</p>
        <p class="text-sm text-gray-300 group-hover:text-[color:var(--color-primary)] transition-colors line-clamp-2">{{ next.title }}</p>
      </router-link>
      <div v-else class="panel p-4 flex items-center justify-end text-xs text-gray-600">已是最后一篇</div>
    </div>

    <!-- 相关推荐 -->
    <div v-if="related.length" class="panel p-6">
      <h3 class="text-sm font-bold text-white mb-4">喜欢这篇文章的人也看了</h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
        <router-link v-for="r in related" :key="r.id" :to="`/blog/post/${r.id}`" class="related-card group">
          <img v-if="r.cover" :src="r.cover" alt="" loading="lazy" class="h-20 w-full object-cover" />
          <div v-else class="h-20 w-full bg-gradient-to-br from-purple-500/15 to-cyan-500/15 flex items-center justify-center text-white/30 text-xl">✦</div>
          <p class="text-xs text-gray-300 mt-2 group-hover:text-[color:var(--color-primary)] transition-colors line-clamp-2">{{ r.title }}</p>
          <p class="text-[10px] text-gray-600 mt-1">{{ formatDate(r.createdAt) }}</p>
        </router-link>
      </div>
    </div>

    <!-- 版权声明 -->
    <div class="panel p-5 flex items-center gap-3 text-xs text-gray-500">
      <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
      <p>本博客所有文章除特别声明外，均采用 <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/" target="_blank" rel="noopener" class="text-[color:var(--color-primary)]">CC BY-NC-SA 4.0</a> 许可协议。转载请注明出处。</p>
    </div>

    <!-- 评论区 -->
    <CommentPanel :target-id="article.id" target-type="ARTICLE" :show-title="true" @count="commentTotal = $event" />
  </div>
  <div v-else class="py-24 text-center text-gray-500 text-sm">文章不存在或已删除</div>
</template>

<script setup>
// ============================================================
// 文章详情页：markdown 渲染 + TOC + 上/下一篇 + 相关推荐 + 评论
// ============================================================
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getBlogDetail, getBlogRelated, likeBlog, favoriteBlog, checkBlogLike, checkBlogFavorite } from '@/api/blog'
import { renderMarkdown, extractToc } from '@/utils/markdown'
import { formatDay } from '@/utils/date'
import { useReadMark } from '@/composables/useReadMark'
import PostToc from '@/components/blog/PostToc.vue'
import CommentPanel from '@/components/blog/CommentPanel.vue'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'

const route = useRoute()
const auth = useAuthStore()
const toast = useToastStore()
const { markRead } = useReadMark()

const article = ref(null)
const coverFailed = ref(false)
const prev = ref(null)
const next = ref(null)
const related = ref([])
const commentTotal = ref(null)
const liked = ref(false)
const favorited = ref(false)
const readingProgress = ref(0)
let rafId = null

const contentHtml = computed(() => article.value ? renderMarkdown(article.value.content) : '')
const toc = computed(() => extractToc(contentHtml.value))
const wordCount = computed(() => article.value?.content?.replace(/[#>*`_\-\[\]()!|]/g, '').length || 0)
const readingMin = computed(() => Math.max(1, Math.round(wordCount.value / 300)))

// 阅读进度条（requestAnimationFrame 节流）
function handleScroll() {
  if (rafId) return
  rafId = requestAnimationFrame(() => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
    const scrollHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight
    readingProgress.value = scrollHeight > 0 ? (scrollTop / scrollHeight) * 100 : 0
    rafId = null
  })
}

// 分享文章
function shareArticle() {
  const url = window.location.href
  const title = article.value?.title || ''
  if (navigator.share) {
    navigator.share({ title, url }).catch(() => {})
  } else if (navigator.clipboard) {
    navigator.clipboard.writeText(url)
    toast.success('链接已复制到剪贴板')
  } else {
    toast.info('请手动复制地址栏链接分享')
  }
}

// 日期格式化（统一走 utils/date）
function formatDate(d) { return formatDay(d, '') }

async function fetchDetail(id) {
  try {
    const d = await getBlogDetail(id)
    const payload = d?.data ?? d
    article.value = payload?.article || payload
    prev.value = payload?.prevArticle || null
    next.value = payload?.nextArticle || null
    markRead(id)
    if (article.value?.id) {
      try {
        const r = await getBlogRelated(article.value.id)
        related.value = r?.data ?? r ?? []
      } catch {}
      await initLikedState()
    }
  } catch {
    article.value = null
  }
}

async function initLikedState() {
  if (!auth.isLoggedIn || !article.value?.id) return
  try {
    const [likeRes, favRes] = await Promise.all([
      checkBlogLike(article.value.id),
      checkBlogFavorite(article.value.id)
    ])
    const likeData = likeRes?.data ?? likeRes
    const favData = favRes?.data ?? favRes
    if (likeData) liked.value = !!likeData.liked
    if (favData) favorited.value = !!favData.favorited
  } catch {}
}

async function toggleLike() {
  if (!auth.isLoggedIn) { toast.error('请先登录'); return }
  try {
    await likeBlog(article.value.id)
    liked.value = !liked.value
    article.value.likeCount = (article.value.likeCount || 0) + (liked.value ? 1 : -1)
    toast.success(liked.value ? '已点赞' : '已取消点赞')
  } catch (e) {
    toast.error(e?.response?.data?.message || '点赞失败，请稍后重试')
  }
}

async function toggleFavorite() {
  if (!auth.isLoggedIn) { toast.error('请先登录'); return }
  try {
    await favoriteBlog(article.value.id)
    favorited.value = !favorited.value
    toast.success(favorited.value ? '已收藏' : '已取消收藏')
  } catch (e) {
    toast.error(e?.response?.data?.message || '收藏失败，请稍后重试')
  }
}

watch(() => route.params.id, (id) => { if (id) fetchDetail(id) })
onMounted(() => {
  if (route.params.id) fetchDetail(route.params.id)
  window.addEventListener('scroll', handleScroll, { passive: true })
})
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (rafId) cancelAnimationFrame(rafId)
})
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }
.related-card { @apply block rounded-xl border border-white/[0.06] p-3 hover:border-white/[0.14] hover:bg-[#0e0e26] transition-all duration-300; }
</style>


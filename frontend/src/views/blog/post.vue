<template>
  <div class="relative min-h-screen">
    <ThreeBackground />
    <ParticleBackground />
    <Web3Nav />
    <ReadingProgress />

    <main class="relative z-10 max-w-[860px] mx-auto px-4 py-8 space-y-6">
      <!-- 返回按钮 -->
      <div class="post-back" ref="backRef">
        <router-link to="/blog" class="inline-flex items-center gap-1.5 text-xs text-gray-500 hover:text-white transition-colors">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
          返回博客
        </router-link>
      </div>

      <template v-if="article">
        <!-- 文章头 -->
        <header class="post-header panel p-6 md:p-8" ref="headerRef">
          <div class="flex flex-wrap items-center gap-2 text-xs text-gray-500 mb-3">
            <span class="px-2 py-0.5 rounded-md bg-purple-500/15 text-purple-300 border border-purple-500/20">{{ article.category || '未分类' }}</span>
            <span class="opacity-50">·</span>
            <time>{{ formatDate(article.createdAt) }}</time>
            <template v-if="formatDate(article.updatedAt) !== formatDate(article.createdAt)">
              <span class="opacity-50">·</span>
              <span>更新于 {{ formatDate(article.updatedAt) }}</span>
            </template>
          </div>
          <h1 class="post-title text-2xl md:text-3xl font-black text-white leading-snug mb-3">{{ article.title }}</h1>
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
          </div>
          <!-- 标签行 -->
          <div v-if="articleTags.length" class="flex flex-wrap items-center gap-1.5 mt-3 pt-3 border-t border-white/[0.06]">
            <span class="text-[11px] text-gray-500"># 标签</span>
            <span v-for="t in articleTags" :key="t" @click="goTag(t)"
              class="cursor-pointer px-2 py-0.5 rounded-md text-[11px] text-gray-400 hover:text-purple-300 hover:bg-purple-500/15 transition-colors">{{ t }}</span>
          </div>
        </header>

        <!-- 封面 -->
        <div v-if="article.cover" class="post-cover rounded-2xl border border-white/[0.06] overflow-hidden" ref="coverRef">
          <div v-if="coverFailed" class="w-full aspect-[2/1] flex items-center justify-center bg-gradient-to-br from-purple-500/25 via-[rgba(10,10,40,0.6)] to-cyan-500/20 text-white/50 text-3xl">✦</div>
          <img v-else :src="article.cover" :alt="article.title" referrerpolicy="no-referrer" @error="coverFailed = true" class="w-full object-cover" />
        </div>

        <!-- 正文 + 目录 -->
        <div class="grid grid-cols-1 xl:grid-cols-[1fr_220px] gap-6 items-start">
          <div class="post-body panel p-6 md:p-8 article-body" ref="bodyRef" v-html="contentHtml"></div>
          <PostToc v-if="toc.length" :toc="toc" class="hidden xl:block sticky top-24" />
        </div>

        <!-- 上/下一篇 -->
        <div class="post-nav grid grid-cols-2 gap-4" ref="navRef">
          <router-link v-if="prev" :to="`/blog/post/${prev.id}`" class="panel p-4 group">
            <p class="text-[10px] text-gray-500 mb-1">上一篇</p>
            <p class="text-sm text-gray-300 group-hover:text-purple-400 transition-colors line-clamp-2">{{ prev.title }}</p>
          </router-link>
          <div v-else class="panel p-4 flex items-center text-xs text-gray-600">已是第一篇</div>
          <router-link v-if="next" :to="`/blog/post/${next.id}`" class="panel p-4 group text-right">
            <p class="text-[10px] text-gray-500 mb-1">下一篇</p>
            <p class="text-sm text-gray-300 group-hover:text-purple-400 transition-colors line-clamp-2">{{ next.title }}</p>
          </router-link>
          <div v-else class="panel p-4 flex items-center justify-end text-xs text-gray-600">已是最后一篇</div>
        </div>

        <!-- 相关推荐 -->
        <div v-if="related.length" class="post-related panel p-6" ref="relatedRef">
          <h3 class="text-sm font-bold text-white mb-4">喜欢这篇文章的人也看了</h3>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
            <router-link v-for="r in related" :key="r.id" :to="`/blog/post/${r.id}`" class="related-card group">
              <img v-if="r.cover" :src="r.cover" alt="" loading="lazy" class="h-20 w-full object-cover" />
              <div v-else class="h-20 w-full bg-gradient-to-br from-purple-500/15 to-cyan-500/15 flex items-center justify-center text-white/30 text-xl">✦</div>
              <p class="text-xs text-gray-300 mt-2 group-hover:text-purple-400 transition-colors line-clamp-2">{{ r.title }}</p>
              <p class="text-[10px] text-gray-600 mt-1">{{ formatDate(r.createdAt) }}</p>
            </router-link>
          </div>
        </div>

        <!-- 版权声明 + 分享 -->
        <div class="post-copyright panel p-5" ref="copyrightRef">
          <div class="flex items-center gap-3 text-xs text-gray-500">
            <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
            <p>本博客所有文章除特别声明外，均采用 <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/" target="_blank" rel="noopener" class="text-purple-400">CC BY-NC-SA 4.0</a> 许可协议。转载请注明出处。</p>
          </div>
          <div class="flex flex-wrap items-center gap-2 mt-4 pt-3 border-t border-white/[0.06]">
            <span class="text-[11px] text-gray-500">分享到</span>
            <button class="share-btn" @click="share('wechat')">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M9.5 4C5.36 4 2 6.91 2 10.5c0 2.06 1.11 3.88 2.87 5.07L4 17l1.86-1.05c.5.13 1.03.2 1.64.2h.31C7.29 15.45 7.5 14.5 7.5 13.5c0-3.03 3.13-5.5 7-5.5.42 0 .83.02 1.22.08C15.06 5.25 12.53 4 9.5 4zM8 7.25a.85.85 0 110 1.7.85.85 0 010-1.7zm4.5 0a.85.85 0 110 1.7.85.85 0 010-1.7zM9.5 11c-.16.29.06.75.4.75h3.4c.31 0 .5-.44.34-.75-.9-1.71-2.58-1.83-4.14 0zM14.5 11.5c0 2.48 2.51 4.5 5.5 4.5.55 0 1.08-.07 1.58-.2L22 17l-.94-1.7c1.5-1.06 2.44-2.68 2.44-4.3 0-3.04-3.13-5.5-7-5.5s-7 2.46-7 5.5zM18 9.75a.6.6 0 110 1.2.6.6 0 010-1.2zM14.3 12.75a.6.6 0 110 1.2.6.6 0 010-1.2zm5.8.25a.4.4 0 100 .8.4.4 0 000-.8z"/></svg>
              微信
            </button>
            <button class="share-btn" @click="share('weibo')">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M10.5 20.8a6.3 6.3 0 01-6.3-6.3c0-2.4 1.35-4.6 3.4-5.6.35-.17.77.02.88.4.1.37-.14.73-.5.87a4.5 4.5 0 00-2.53 4.05c0 2.4 1.9 4.3 4.3 4.3 2.1 0 3.9-1.5 4.2-3.5h-2.3l2.9-3.9 2.9 3.9h-2.05a4.3 4.3 0 01-4.4 3.68zM13.5 7.5a1.05 1.05 0 110-2.1 1.05 1.05 0 010 2.1zm-2-4.6a.4.4 0 01.4.4v1.4a.4.4 0 01-.8 0V3.3a.4.4 0 01.4-.4zm2.6 1a.4.4 0 01.6.3l.05.05v1.4a.4.4 0 01-.8 0V4.3a.4.4 0 01.15-.4zM18.5 7.8c.34 0 .62.27.62.62v4.15a9.5 9.5 0 01-3.6 7.5c-1.03.77-2.3 1.18-3.62 1.18A8 8 0 017 16a5.7 5.7 0 01-1.27-3.6c0-3.16 2.6-4.72 4.77-4.72 1.3 0 2.47.55 3.32 1.44a1.4 1.4 0 012.7 0 1.4 1.4 0 01-1.2 1.9h-5.8c.6 1.5 2 2.3 3.9 2.3 1.5 0 2.9-.8 3.6-2.02V8.43c0-.34.28-.62.61-.62z"/></svg>
              微博
            </button>
            <button class="share-btn" @click="share('qzone')">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2.5l2 1.3 2.5-.7-.4 2.6 1.8 1.9-2.5.9v2.9l2.5.9-1.8 1.9.4 2.6-2.5-.7-2 1.3-2-1.3-2.5.7.4-2.6L6 11.3l2.5-.9V7.5L6 6.6l1.8-1.9L7.4 2.1 10 2.9z"/></svg>
              空间
            </button>
            <button class="share-btn" @click="share('copy')">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-1V3a2 2 0 00-2-2h-3a2 2 0 00-2 2v2H8zm5-2v2h-3V3h3z"/></svg>
              复制链接
            </button>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="post-comment" ref="commentRef">
          <CommentPanel :target-id="article.id" target-type="ARTICLE" :show-title="true" @count="commentTotal = $event" />
        </div>
      </template>
      <div v-else class="py-24 text-center text-gray-500 text-sm">文章不存在或已删除</div>
    </main>
  </div>
</template>

<script setup>
// ============================================================
// 博客详情页（独立全宽布局 + anime.js 入场特效 + 评论）
// ============================================================
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBlogDetail, getBlogRelated, likeBlog, favoriteBlog, checkBlogLike, checkBlogFavorite } from '@/api/blog'
import { renderMarkdown, extractToc } from '@/utils/markdown'
import { useReadMark } from '@/composables/useReadMark'
import PostToc from '@/components/blog/PostToc.vue'
import CommentPanel from '@/components/blog/CommentPanel.vue'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'
import ParticleBackground from '@/components/layout/ParticleBackground.vue'
import ThreeBackground from '@/components/layout/ThreeBackground.vue'
import Web3Nav from '@/components/layout/Web3Nav.vue'
import ReadingProgress from '@/components/blog/ReadingProgress.vue'
import { animate, stagger } from 'animejs'

const route = useRoute()
const router = useRouter()
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

const backRef = ref(null)
const headerRef = ref(null)
const coverRef = ref(null)
const bodyRef = ref(null)
const navRef = ref(null)
const relatedRef = ref(null)
const copyrightRef = ref(null)
const commentRef = ref(null)

const contentHtml = computed(() => article.value ? renderMarkdown(article.value.content) : '')
const toc = computed(() => extractToc(contentHtml.value))
const wordCount = computed(() => article.value?.content?.replace(/[#>*`_\-\[\]()!|]/g, '').length || 0)
const readingMin = computed(() => Math.max(1, Math.round(wordCount.value / 300)))
const articleTags = computed(() => {
  const raw = article.value?.tags ?? article.value?.tagsArr ?? (article.value?.tagsArr)
  if (Array.isArray(raw)) return raw.filter(Boolean)
  return String(raw || '').split(',').map(s => s.trim()).filter(Boolean)
})

function formatDate(d) { return d ? String(d).slice(0, 10) : '' }
function goTag(tag) { router.push({ path: '/blog', query: { tag } }) }

function runEntrance() {
  nextTick(() => {
    const targets = [backRef, headerRef, coverRef, bodyRef, navRef, relatedRef, copyrightRef, commentRef]
      .filter(r => r.value).map(r => r.value)

    if (!targets.length) return

    animate(targets, {
      opacity: [0, 1],
      translateY: [40, 0],
      duration: 800,
      delay: stagger(120, { start: 100 }),
      ease: 'outExpo'
    })

    // 标题逐字显现
    const titleEl = headerRef.value?.querySelector('.post-title')
    if (titleEl) {
      const text = titleEl.textContent
      titleEl.textContent = ''
      const chars = text.split('').map(ch => {
        const span = document.createElement('span')
        span.textContent = ch
        span.style.display = 'inline-block'
        span.style.opacity = '0'
        titleEl.appendChild(span)
        return span
      })
      animate(chars, {
        opacity: [0, 1],
        translateY: [12, 0],
        duration: 600,
        delay: stagger(30, { start: 400 }),
        ease: 'outQuad'
      })
    }

    // 正文图片渐入
    const images = targets.flatMap(t => [...t.querySelectorAll('.article-body img')])
    if (images.length) {
      animate(images, {
        opacity: [0, 1],
        scale: [0.95, 1],
        duration: 600,
        delay: stagger(80, { start: 800 }),
        ease: 'outCubic'
      })
    }
  })
}

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
    runEntrance()
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

function share(type) {
  const url = encodeURIComponent(window.location.href)
  const title = encodeURIComponent(article.value?.title || document.title)
  let target = ''
  if (type === 'weibo') target = `https://service.weibo.com/share/share.php?url=${url}&title=${title}`
  else if (type === 'qzone') target = `https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?url=${url}&title=${title}`
  else if (type === 'wechat') { toast.info('请截图二维码或使用右上角分享'); return }
  else if (type === 'copy') {
    navigator.clipboard?.writeText(window.location.href)
      .then(() => toast.success('链接已复制'))
      .catch(() => toast.error('复制失败'))
    return
  }
  if (target) window.open(target, '_blank', 'noopener,width=640,height=480')
}

watch(() => route.params.id, (id) => { if (id) fetchDetail(id) })
onMounted(() => { if (route.params.id) fetchDetail(route.params.id) })
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }
.related-card { @apply block rounded-xl border border-white/[0.08] p-3 hover:border-white/[0.18] hover:bg-[#121230] transition-all duration-300; }
.share-btn {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] text-gray-400
    border border-white/[0.08] hover:text-white hover:border-white/[0.18] hover:bg-white/[0.04] transition-colors;
}
</style>

<style>
.article-body { line-height: 1.9; color: #e2e8f0; font-size: 0.95rem; }
.article-body h1, .article-body h2, .article-body h3, .article-body h4 {
  color: #fff; font-weight: 800; margin: 1.6em 0 0.8em; line-height: 1.4; scroll-margin-top: 90px;
}
.article-body h1 { font-size: 1.5rem; }
.article-body h2 { font-size: 1.3rem; padding-bottom: 0.4em; border-bottom: 1px solid rgba(255,255,255,0.08); }
.article-body h3 { font-size: 1.1rem; }
.article-body p { margin: 0.8em 0; }
.article-body a { color: var(--color-primary); text-decoration: underline; text-underline-offset: 3px; }
.article-body strong { color: #fff; }
.article-body code { background: rgba(168,85,247,0.12); color: #e9d5ff; padding: 0.15em 0.4em; border-radius: 4px; font-size: 0.85em; font-family: var(--font-mono); }
.article-body pre.code-block {
  position: relative; background: rgba(2, 2, 12, 0.85); border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px; padding: 1rem 1.2rem; overflow-x: auto; margin: 1.2em 0;
}
.article-body pre.code-block code { background: transparent; color: #cbd5e1; padding: 0; font-size: 0.85rem; }
.article-body pre.code-block .code-copy {
  position: absolute; top: 8px; right: 8px; font-size: 10px; color: #6b7280;
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 6px; padding: 2px 8px; cursor: pointer; opacity: 0; transition: opacity 0.2s;
}
.article-body pre.code-block:hover .code-copy { opacity: 1; }
.article-body blockquote { border-left: 3px solid var(--color-primary); background: rgba(168,85,247,0.06); padding: 0.6em 1em; border-radius: 0 8px 8px 0; margin: 1em 0; color: #9ca3af; }
.article-body ul, .article-body ol { margin: 0.8em 0; padding-left: 1.5em; }
.article-body ul { list-style: disc; }
.article-body ol { list-style: decimal; }
.article-body li { margin: 0.3em 0; }
.article-body img { max-width: 100%; border-radius: 12px; margin: 1em 0; border: 1px solid rgba(255,255,255,0.08); cursor: zoom-in; }
.article-body hr { border: none; border-top: 1px solid rgba(255,255,255,0.08); margin: 2em 0; }
.article-body .table-wrap { overflow-x: auto; margin: 1em 0; }
.article-body table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
.article-body th, .article-body td { border: 1px solid rgba(255,255,255,0.1); padding: 0.5em 0.8em; text-align: left; }
.article-body th { background: rgba(168,85,247,0.1); color: #fff; }
.article-body tr:nth-child(even) td { background: rgba(255,255,255,0.02); }
</style>

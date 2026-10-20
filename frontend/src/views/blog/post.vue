<template>
  <div class="relative min-h-screen">
    <!-- 本路由为顶层独立全宽布局（router 中 /blog/post/:id 不嵌套 BlogLayout），
         背景需由本页自行挂载；Web3Layout / BlogLayout 各自的背景不作用于本页 -->
    <ThreeBackground />
    <ParticleBackground />
    <Web3Nav />
    <ReadingProgress />

    <main class="relative z-10 mx-auto w-full max-w-[1400px] px-4 md:px-6 py-8">
      <!-- 加载 -->
      <div v-if="loading" class="py-24 flex flex-col items-center gap-3 text-gray-500">
        <div class="h-8 w-8 rounded-full border-2 border-white/15 border-t-cyan-400 animate-spin"></div>
        <span class="text-xs">正在加载文章…</span>
      </div>

      <div v-else-if="article" class="space-y-6">
        <router-link to="/blog" class="inline-flex items-center gap-1.5 text-xs text-gray-500 hover:text-cyan-300 transition-colors">
          ← 返回博客
        </router-link>

        <!-- 文章主体：与知识库「知识详情」同构（单列 panel + 徽章行 + 标题 + 摘要 + 元信息行 + 正文 + 标签行） -->
        <article class="panel p-6 md:p-9">
          <div class="flex flex-wrap items-center gap-2 text-xs mb-4">
            <span class="kb-badge cat">{{ article.category || '未分类' }}</span>
            <span v-if="article.isTop" class="kb-badge">置顶</span>
            <span v-if="formatDate(article.updatedAt) !== formatDate(article.createdAt)" class="kb-badge">已更新</span>
          </div>

          <h1 class="text-2xl md:text-3xl font-bold text-white leading-snug">{{ article.title }}</h1>
          <p v-if="article.summary" class="text-sm text-gray-400 mt-3 leading-relaxed">{{ article.summary }}</p>

          <div class="flex flex-wrap items-center gap-3 mt-4 text-xs text-gray-500">
            <span>更新于 {{ formatDate(article.updatedAt || article.createdAt) }}</span>
            <span>浏览 {{ article.viewCount ?? 0 }}</span>
            <span>字数 {{ wordCount }}</span>
            <span>约 {{ readingMin }} 分钟</span>
            <span v-if="commentTotal !== null">评论 {{ commentTotal }}</span>
            <span class="ml-auto flex flex-wrap items-center gap-2">
              <button class="kb-act-btn" :class="{ 'is-on': liked }" @click="toggleLike" title="点赞">
                {{ liked ? '♥' : '♡' }} {{ article.likeCount ?? 0 }}
              </button>
              <button class="kb-act-btn" :class="{ 'is-on': favorited }" @click="toggleFavorite" :title="favorited ? '取消收藏' : '收藏'">
                {{ favorited ? '★' : '☆' }} {{ favorited ? '已收藏' : '收藏' }}
              </button>
              <button class="kb-act-btn" :class="{ 'is-on': showAi }" @click="toggleAi" title="AI 摘要与文章问答">✦ AI</button>
              <button class="kb-act-btn" @click="exportMd" title="导出 Markdown">⬇ MD</button>
            </span>
          </div>

          <!-- 正文（内部链接交给 Vue Router，代码块复制走事件委托） -->
          <div class="kb-body article-body mt-8" ref="bodyRef" v-html="contentHtml" @click="onBodyClick"></div>

          <div v-if="articleTags.length" class="flex flex-wrap gap-2 mt-8 pt-6 border-t border-white/10">
            <span v-for="t in articleTags" :key="t" @click="goTag(t)"
              class="kb-badge hover:!text-cyan-300 cursor-pointer transition-colors">#{{ t }}</span>
          </div>
        </article>

        <!-- AI 助手面板：摘要 + 文章问答（AI 通道，流式输出） -->
        <section v-if="showAi" class="ai-assistant panel p-5 md:p-6">
          <div class="ai-head">
            <span class="ai-badge">✦ AI 助手</span>
            <span class="text-[10px] text-gray-600">由 AI 提供 · 内容仅供参考</span>
            <button v-if="aiSummary" class="ai-regen" :disabled="aiSumLoading" @click="genSummary">重新生成摘要</button>
            <button class="ai-close" title="收起" @click="showAi = false">✕</button>
          </div>

          <div class="ai-summary-box">
            <div class="ai-label">📌 文章摘要</div>
            <div v-if="aiSumLoading && !aiSummary" class="ai-typing">正在生成摘要…</div>
            <div v-else-if="aiSummary" class="ai-summary-text">{{ aiSummary }}</div>
            <div v-else class="ai-empty">
              <span>一键生成要点摘要，帮你快速把握文章核心</span>
              <button class="ai-btn" @click="genSummary">✦ 生成摘要</button>
            </div>
          </div>

          <div class="ai-qa">
            <div class="ai-label">💬 向本文提问</div>
            <div v-for="(item, i) in aiQa" :key="i" class="ai-qa-item">
              <div class="ai-qa-q">👤 {{ item.q }}</div>
              <div class="ai-qa-a">{{ item.loading && !item.a ? '思考中…' : item.a }}</div>
            </div>
            <div class="ai-qa-input">
              <input v-model="aiQ" class="ai-input" :disabled="aiQaBusy" placeholder="例如：这篇文章的核心结论是什么？" @keyup.enter="askAi" />
              <button class="ai-btn" :disabled="aiQaBusy || !aiQ.trim()" @click="askAi">{{ aiQaBusy ? '回答中…' : '发送' }}</button>
            </div>
          </div>
        </section>

        <!-- 文章目录（单列内嵌 panel，与知识库详情保持同一列结构） -->
        <PostToc v-if="toc.length" :toc="toc" />

        <!-- 上/下一篇 -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <router-link v-if="prev" :to="`/blog/post/${prev.id}`" class="panel p-4 group">
            <p class="text-[10px] text-gray-500 mb-1">上一篇</p>
            <p class="text-sm text-gray-300 group-hover:text-cyan-300 transition-colors line-clamp-2">{{ prev.title }}</p>
          </router-link>
          <div v-else class="panel p-4 flex items-center text-xs text-gray-600">已是第一篇</div>
          <router-link v-if="next" :to="`/blog/post/${next.id}`" class="panel p-4 group text-right">
            <p class="text-[10px] text-gray-500 mb-1">下一篇</p>
            <p class="text-sm text-gray-300 group-hover:text-cyan-300 transition-colors line-clamp-2">{{ next.title }}</p>
          </router-link>
          <div v-else class="panel p-4 flex items-center justify-end text-xs text-gray-600">已是最后一篇</div>
        </div>

        <!-- 同类文章（与知识库「同分类的其他知识」同构） -->
        <div v-if="related.length" class="panel p-6">
          <h3 class="text-sm font-semibold text-white/90 mb-4">🧭 同分类的其他文章</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <router-link v-for="r in related" :key="r.id" :to="`/blog/post/${r.id}`" class="related-item">
              <span class="text-sm">📄</span>
              <span class="text-sm text-white/80 truncate flex-1">{{ r.title }}</span>
              <span class="text-[11px] text-gray-500 shrink-0">{{ formatDate(r.createdAt) }}</span>
            </router-link>
          </div>
        </div>

        <!-- 版权声明 + 分享 -->
        <div class="panel p-5">
          <div class="flex items-center gap-3 text-xs text-gray-500">
            <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
            <p>本博客所有文章除特别声明外，均采用 <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/" target="_blank" rel="noopener" class="text-cyan-400">CC BY-NC-SA 4.0</a> 许可协议。转载请注明出处。</p>
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

        <!-- 评论区：区块按需挂载 —— 评论面板需拉取评论列表并建 DOM，
             位于文章最底部，用 IntersectionObserver（rootMargin 800px）在滚动到附近时才挂载，
             减少长文首屏的渲染与请求开销（借鉴 Astro 博客对正文重型区块的懒加载思路） -->
        <div class="post-comment" ref="commentRef">
          <CommentPanel v-if="commentShown" :target-id="article.id" target-type="ARTICLE" :show-title="true" @count="commentTotal = $event" />
          <div v-else class="post-comment-skeleton">评论区将在滚动到此处时加载…</div>
        </div>
      </div>
      <div v-else class="py-24 text-center text-gray-500 text-sm">文章不存在或已删除</div>
    </main>
  </div>
</template>

<script setup>
// ============================================================
// 博客详情页：版面与「知识库 · 知识详情」保持一致
// （单列 panel + 分类徽章 + 标题 + 摘要 + 元信息行 + article-body 正文 + 标签 + 同类推荐），
// 保留评论区、AI 助手、版权分享与上下篇导航
// ============================================================
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBlogDetail, getBlogRelated, likeBlog, favoriteBlog, checkBlogLike, checkBlogFavorite } from '@/api/blog'
import { renderMarkdown, extractToc } from '@/utils/markdown'
import { formatDay } from '@/utils/date'
import { useReadMark } from '@/composables/useReadMark'
import { useLazyMount } from '@/composables/useLazyMount'
import PostToc from '@/components/blog/PostToc.vue'
import CommentPanel from '@/components/blog/CommentPanel.vue'
import { aiChatStream } from '@/api/freeAi'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'
import ParticleBackground from '@/components/layout/ParticleBackground.vue'
import ThreeBackground from '@/components/layout/ThreeBackground.vue'
import Web3Nav from '@/components/layout/Web3Nav.vue'
import ReadingProgress from '@/components/blog/ReadingProgress.vue'
import 'highlight.js/styles/atom-one-dark.css'
import { formatDay as formatDate } from '@/utils/date.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const { markRead } = useReadMark()

const article = ref(null)
const loading = ref(true)
const prev = ref(null)
const next = ref(null)
const related = ref([])
const commentTotal = ref(null)
const liked = ref(false)
const favorited = ref(false)

// ── AI 助手（摘要 + 文章问答，走免费 AI 通道；摘要按文章 ID 本地缓存）──
const showAi = ref(false)
const aiSummary = ref('')
const aiSumLoading = ref(false)
const aiQa = ref([])
const aiQ = ref('')
const aiQaBusy = ref(false)

const bodyRef = ref(null)
// 评论区锚点：滚动到附近时才挂载真实评论面板
const { target: commentRef, shown: commentShown } = useLazyMount({ rootMargin: '800px' })

const contentHtml = computed(() => article.value ? renderMarkdown(article.value.content) : '')
const toc = computed(() => extractToc(contentHtml.value))
const wordCount = computed(() => article.value?.content?.replace(/[#>*`_\-\[\]()!|]/g, '').length || 0)
const readingMin = computed(() => Math.max(1, Math.round(wordCount.value / 300)))
const articleTags = computed(() => {
  const raw = article.value?.tags ?? article.value?.tagsArr
  if (Array.isArray(raw)) return raw.filter(Boolean)
  return String(raw || '').split(',').map(s => s.trim()).filter(Boolean)
})

// 日期格式化（统一走 utils/date）
function goTag(tag) { router.push({ path: '/blog', query: { tag } }) }

// 导出 Markdown：补一段 front matter，便于迁移到其它静态博客
function exportMd() {
  const a = article.value
  if (!a) return
  const text = `---\ntitle: ${a.title || ''}\ncategory: ${a.category || ''}\ntags: ${articleTags.value.join(', ')}\ndate: ${formatDate(a.createdAt)}\n---\n\n${a.content || ''}`
  const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const el = document.createElement('a')
  el.href = url; el.download = `${a.title || 'article'}.md`
  document.body.appendChild(el); el.click(); document.body.removeChild(el)
  URL.revokeObjectURL(url)
  toast.success('已导出 Markdown')
}

// ── AI 助手：文章摘要 + 问答（免费通道，流式输出）──
const sumCacheKey = id => `ai_blog_sum_${id}`

function aiArticleCtx() {
  const a = article.value || {}
  return `文章标题：《${a.title || ''}》\n分类：${a.category || '未分类'}\n正文：\n${String(a.content || a.summary || '').slice(0, 6000)}`
}

function toggleAi() {
  showAi.value = !showAi.value
  if (!showAi.value || !article.value) return
  // 优先读本地缓存，无缓存则自动生成
  const cached = localStorage.getItem(sumCacheKey(article.value.id))
  if (cached) { aiSummary.value = cached; return }
  if (!aiSummary.value) genSummary()
}

async function genSummary() {
  if (aiSumLoading.value || !article.value) return
  aiSumLoading.value = true
  aiSummary.value = ''
  try {
    await aiChatStream(
      [{ role: 'user', content: '请为这篇文章生成摘要' }],
      d => { aiSummary.value += d },
      {
        system: '你是博客阅读助手，请用中文输出 3-5 条要点摘要（每条一行，以「·」开头，每条不超过 24 字），最后一行用一句话总结全文。不要复述长句，不要客套。\n\n' + aiArticleCtx(),
        max_tokens: 420,
      }
    )
    if (aiSummary.value && article.value) {
      localStorage.setItem(sumCacheKey(article.value.id), aiSummary.value)
    }
  } catch (e) {
    aiSummary.value = '（摘要生成失败：' + (e?.message || '服务暂时不可用') + '）'
  } finally {
    aiSumLoading.value = false
  }
}

async function askAi() {
  const q = aiQ.value.trim()
  if (!q || aiQaBusy.value || !article.value) return
  aiQ.value = ''
  aiQa.value.push({ q, a: '', loading: true })
  const item = aiQa.value[aiQa.value.length - 1]
  aiQaBusy.value = true
  try {
    const history = aiQa.value.slice(-6, -1).flatMap(it => ([
      { role: 'user', content: it.q },
      { role: 'assistant', content: it.a || '（无内容）' },
    ]))
    await aiChatStream([...history, { role: 'user', content: q }], d => { item.a += d }, {
      system: '你是文章问答助手：只依据下面这篇文章的内容回答用户的提问；文章未提及时请明确说明"文章未提及"。回答用中文、简洁口语化，不超过 120 字。\n\n' + aiArticleCtx(),
      max_tokens: 500,
    })
    item.a = item.a || '（未返回内容）'
  } catch (e) {
    item.a = '（回答失败：' + (e?.message || '服务暂时不可用') + '）'
  } finally {
    item.loading = false
    aiQaBusy.value = false
  }
}

// 正文 Markdown 中的站内链接（如 /tools/hash、/knowledge/3）走前端路由，不整页刷新；
// 文章里提到工具时直接 [Hash 生成](/tools/hash) 即可跳到对应工具站
function onBodyClick(e) {
  // 代码块复制按钮（v-html 渲染，无 Vue 事件绑定，用事件委托处理）
  const copyBtn = e.target.closest('.code-copy')
  if (copyBtn && bodyRef.value?.contains(copyBtn)) {
    e.preventDefault()
    const code = decodeURIComponent(copyBtn.getAttribute('data-code') || '')
    navigator.clipboard?.writeText(code).then(() => {
      const orig = copyBtn.textContent
      copyBtn.textContent = '已复制 ✓'
      setTimeout(() => { copyBtn.textContent = orig }, 1500)
    }).catch(() => toast.error('复制失败'))
    return
  }
  const a = e.target.closest('a')
  if (!a || !bodyRef.value?.contains(a)) return
  const href = a.getAttribute('href') || ''
  if (href.startsWith('/') && !href.startsWith('//')) {
    e.preventDefault()
    if (href !== route.fullPath) router.push(href)
  }
}

async function fetchDetail(id) {
  loading.value = true
  article.value = null
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
  } finally {
    loading.value = false
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

watch(() => route.params.id, (id) => { if (id) { loading.value = true; fetchDetail(id) } })
// 切换文章时重置 AI 助手状态（摘要/问答与当前文章强绑定）
watch(() => route.params.id, () => { showAi.value = false; aiSummary.value = ''; aiQa.value = []; aiQ.value = '' })
onMounted(() => { if (route.params.id) fetchDetail(route.params.id) })
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }
/* 评论区懒加载占位：与 panel 同高观感，滚动到附近后原地替换为真实评论面板 */
.post-comment-skeleton {
  @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] py-8 text-center text-xs text-gray-600;
}
.share-btn {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] text-gray-400
    border border-white/[0.08] hover:text-white hover:border-white/[0.18] hover:bg-white/[0.04] transition-colors;
}

/* 与知识库详情同款徽章 / 同类项 / 元信息操作按钮 */
.kb-badge {
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 7px;
  background: rgba(255,255,255,.06);
  color: rgba(255,255,255,.65);
  border: 1px solid rgba(255,255,255,.08);
}
.kb-badge.cat { background: rgba(34,211,238,.08); color: #67e8f9; border-color: rgba(34,211,238,.2); }
.related-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(255,255,255,.03);
  border: 1px solid rgba(255,255,255,.07);
  transition: all .2s;
}
.related-item:hover { border-color: rgba(103,232,249,.3); transform: translateY(-1px); }
.kb-act-btn {
  padding: 4px 10px; border-radius: 7px; font-size: 11.5px;
  background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.1);
  color: rgba(255,255,255,.7); transition: all .2s;
}
.kb-act-btn:hover { border-color: rgba(103,232,249,.35); color: #67e8f9; }
.kb-act-btn.is-on { border-color: rgba(103,232,249,.45); color: #67e8f9; background: rgba(34,211,238,.10); }

/* ── AI 助手：摘要 + 文章问答 ── */
.ai-assistant { @apply flex flex-col gap-4; }
.ai-head { @apply flex flex-wrap items-center gap-2; }
.ai-badge {
  @apply inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-medium;
  background: rgba(168, 85, 247, 0.16);
  color: #d8b4fe;
  border: 1px solid rgba(168, 85, 247, 0.3);
}
.ai-regen { @apply ml-auto text-[11px] text-purple-300/70 hover:text-purple-200 transition-colors disabled:opacity-40; }
.ai-close { @apply text-[11px] text-gray-500 hover:text-white transition-colors px-1; }
.ai-label { @apply text-[11px] text-gray-500 mb-2; }
.ai-summary-box { @apply rounded-xl border border-white/[0.06] bg-white/[0.02] p-3.5; }
.ai-summary-text { @apply text-[13px] leading-relaxed text-gray-200 whitespace-pre-wrap; }
.ai-typing { @apply text-[12px] text-purple-300/80; }
.ai-typing::after { content: '▍'; animation: ai-blink 1s steps(1) infinite; }
@keyframes ai-blink { 50% { opacity: 0; } }
.ai-empty { @apply flex flex-wrap items-center gap-3 text-[12px] text-gray-500; }
.ai-btn {
  @apply inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-[11px] text-white transition-all
    bg-purple-600/80 hover:bg-purple-500 disabled:opacity-40 disabled:cursor-not-allowed;
}
.ai-qa { @apply rounded-xl border border-white/[0.06] bg-white/[0.02] p-3.5; }
.ai-qa-item + .ai-qa-item { @apply mt-3 pt-3 border-t border-white/[0.05]; }
.ai-qa-q { @apply text-[12px] text-gray-300 mb-1; }
.ai-qa-a { @apply text-[13px] leading-relaxed text-gray-200 whitespace-pre-wrap; }
.ai-qa-input { @apply flex items-center gap-2 mt-3; }
.ai-input {
  @apply flex-1 bg-white/[0.04] border border-white/[0.10] rounded-lg px-3 py-2 text-[12px] text-white
    placeholder:text-gray-600 focus:outline-none focus:border-purple-500/50 transition-colors;
}
</style>

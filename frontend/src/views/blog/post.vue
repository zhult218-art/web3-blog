<template>
  <div class="relative min-h-screen">
    <ThreeBackground />
    <ParticleBackground />
    <Web3Nav />
    <ReadingProgress />

    <main class="relative z-10 max-w-[1400px] mx-auto px-4 md:px-6 py-8 space-y-6">
      <!-- 返回按钮 -->
      <div class="post-back" ref="backRef">
        <router-link to="/blog" class="inline-flex items-center gap-1.5 text-xs text-gray-500 hover:text-white transition-colors">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
          返回博客
        </router-link>
      </div>

      <!-- 加载骨架 -->
      <template v-if="loading">
        <div class="panel p-6 md:p-8 space-y-4">
          <div class="h-3 w-24 rounded bg-white/8 animate-pulse"></div>
          <div class="h-7 w-2/3 rounded bg-white/10 animate-pulse"></div>
          <div class="h-3 w-1/3 rounded bg-white/8 animate-pulse"></div>
        </div>
        <div class="panel p-6 md:p-8">
          <div class="space-y-3">
            <div class="h-3 w-full rounded bg-white/8 animate-pulse"></div>
            <div class="h-3 w-5/6 rounded bg-white/8 animate-pulse"></div>
            <div class="h-3 w-4/6 rounded bg-white/8 animate-pulse"></div>
            <div class="h-3 w-full rounded bg-white/8 animate-pulse"></div>
            <div class="h-3 w-3/4 rounded bg-white/8 animate-pulse"></div>
          </div>
        </div>
      </template>

      <template v-else-if="article">
        <!-- 文章头 -->
        <header class="post-header panel p-6 md:p-8" ref="headerRef">
          <div class="flex flex-wrap items-center gap-2 text-xs text-gray-500 mb-3">
            <span class="post-category">{{ article.category || '未分类' }}</span>
            <span class="opacity-50">·</span>
            <time class="post-meta-time">{{ formatDate(article.createdAt) }}</time>
            <template v-if="formatDate(article.updatedAt) !== formatDate(article.createdAt)">
              <span class="opacity-50">·</span>
              <span>更新于 {{ formatDate(article.updatedAt) }}</span>
            </template>
          </div>
          <h1 class="post-title text-2xl md:text-[2rem] font-black text-white leading-snug mb-4">{{ article.title }}</h1>
          <!-- 元数据分组：信息区 + 操作区 -->
          <div class="post-meta-bar">
            <div class="post-meta-info">
              <span class="post-meta-chip">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 0L12 9.586V12h2.414l5.586-5.586a2 2 0 000-2.828z"/></svg>
                字数 {{ wordCount }}
              </span>
              <span class="post-meta-chip">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                阅读 {{ readingMin }} 分钟
              </span>
              <span v-if="commentTotal !== null" class="post-meta-chip">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.86 9.86 0 01-4-.8L3 21l1.8-3.2A7.96 7.96 0 013 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
                评论 {{ commentTotal }}
              </span>
              <span class="post-meta-chip">
                <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>
                浏览 {{ article.viewCount ?? 0 }}
              </span>
            </div>
            <div class="post-meta-actions">
              <button class="post-action" :class="{ 'is-active': liked }" @click="toggleLike">
                <svg class="w-3.5 h-3.5" :class="liked ? 'fill-pink-500' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/></svg>
                {{ article.likeCount ?? 0 }}
              </button>
              <button class="post-action" :class="{ 'is-active': favorited }" @click="toggleFavorite" :title="favorited ? '取消收藏' : '收藏'">
                <svg class="w-3.5 h-3.5" :class="favorited ? 'fill-amber-400' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11.48 3.5a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.5.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.562.562 0 01.321-.988l5.518-.441a.563.563 0 00.475-.345L11.48 3.5z"/></svg>
                收藏
              </button>
              <button class="post-action ai-action" :class="{ 'is-active': showAi }" @click="toggleAi" :title="showAi ? '收起 AI 助手' : 'AI 摘要与问答'">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z"/></svg>
                AI 助手
              </button>
            </div>
          </div>
          <!-- 标签行 -->
          <div v-if="articleTags.length" class="post-tags-row">
            <span class="text-[11px] text-gray-500"># 标签</span>
            <span v-for="t in articleTags" :key="t" @click="goTag(t)" class="post-tag">{{ t }}</span>
          </div>
        </header>

        <!-- AI 助手面板：摘要 + 文章问答（免费 AI 通道，流式输出） -->
        <section v-if="showAi" class="ai-assistant panel mt-6 p-5 md:p-6">
          <div class="ai-head">
            <span class="ai-badge">✦ AI 助手</span>
            <span class="text-[10px] text-gray-600">由免费 AI 提供 · 内容仅供参考</span>
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

        <!-- 目录 + 正文 -->
        <div class="grid grid-cols-1 xl:grid-cols-[220px_1fr] gap-6 items-start">
          <PostToc v-if="toc.length" :toc="toc" class="hidden xl:block sticky top-24 order-first" />
          <article class="post-body panel p-6 md:p-8 article-body" ref="bodyRef" v-html="contentHtml" @click="onBodyClick"></article>
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
import { formatDay } from '@/utils/date'
import { useReadMark } from '@/composables/useReadMark'
import PostToc from '@/components/blog/PostToc.vue'
import CommentPanel from '@/components/blog/CommentPanel.vue'
import { aiChatStream } from '@/api/freeAi'
import { useAuthStore } from '@/stores/modules/auth'
import { useToastStore } from '@/stores/modules/toast'
import ParticleBackground from '@/components/layout/ParticleBackground.vue'
import ThreeBackground from '@/components/layout/ThreeBackground.vue'
import Web3Nav from '@/components/layout/Web3Nav.vue'
import ReadingProgress from '@/components/blog/ReadingProgress.vue'
import { animate, stagger } from 'animejs'
import 'highlight.js/styles/atom-one-dark.css'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const { markRead } = useReadMark()

const article = ref(null)
const loading = ref(true)
const coverFailed = ref(false)
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

// 日期格式化（统一走 utils/date）
function formatDate(d) { return formatDay(d, '') }
function goTag(tag) { router.push({ path: '/blog', query: { tag } }) }

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
  loading.value = true
  article.value = null
  coverFailed.value = false
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
.related-card { @apply block rounded-xl border border-white/[0.08] p-3 hover:border-white/[0.18] hover:bg-[#121230] transition-all duration-300; }
.share-btn {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] text-gray-400
    border border-white/[0.08] hover:text-white hover:border-white/[0.18] hover:bg-white/[0.04] transition-colors;
}

/* 文章头排版优化 */
.post-header { position: relative; }
.post-category {
  @apply px-2 py-0.5 rounded-md text-[11px] font-medium border;
  background: rgba(168, 85, 247, 0.12);
  border-color: rgba(168, 85, 247, 0.22);
  color: #d8b4fe;
}
.post-meta-time { @apply text-gray-400 tabular-nums; }

.post-meta-bar {
  @apply flex flex-wrap items-center justify-between gap-3 pt-3 mt-3 border-t border-white/[0.06];
}
.post-meta-info { @apply flex flex-wrap items-center gap-2; }
.post-meta-chip {
  @apply inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] text-gray-400
    bg-white/[0.03] border border-white/[0.06] tabular-nums;
}
.post-meta-chip svg { @apply opacity-60; }
.post-meta-actions { @apply flex items-center gap-2; }
.post-action {
  @apply inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-[11px] text-gray-400
    border border-white/[0.08] hover:text-white hover:border-white/[0.18] hover:bg-white/[0.04] transition-all;
}
.post-action.is-active {
  color: #fff;
  border-color: transparent;
}
.post-action.is-active svg { @apply text-pink-500 fill-pink-500/30; }
.post-action.is-active:nth-of-type(2) svg { color: #fbbf24; }
.post-tags-row {
  @apply flex flex-wrap items-center gap-1.5 mt-4 pt-3 border-t border-white/[0.06];
}
.post-tag {
  @apply cursor-pointer px-2 py-0.5 rounded-md text-[11px] text-gray-400
    hover:text-purple-300 hover:bg-purple-500/15 transition-colors;
}

/* ── AI 助手：摘要 + 文章问答 ── */
.ai-action.is-active svg { color: #c084fc; fill: rgba(192, 132, 252, 0.3); }
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


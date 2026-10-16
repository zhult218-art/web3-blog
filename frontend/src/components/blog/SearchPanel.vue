<template>
  <Teleport to="body">
    <Transition name="overlay">
      <div v-if="ui.searchOpen" class="fixed inset-0 z-[130] flex items-start justify-center p-4 pt-[12vh]"
        @click.self="close">
        <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
        <div class="relative w-full max-w-xl rounded-2xl border border-white/10 bg-[#0a0a18]/95 backdrop-blur-xl shadow-2xl overflow-hidden">
          <!-- 搜索框 -->
          <div class="flex items-center gap-3 px-5 py-4 border-b border-white/[0.06]">
            <span class="text-purple-300">🔎</span>
            <input ref="inputEl" v-model="keyword" placeholder="全站搜索：文章 / 商品 / 社区 / 资源 / 音乐…" class="flex-1 bg-transparent text-sm text-white placeholder:text-gray-600 focus:outline-none" />
            <span class="text-[10px] text-gray-600 font-mono border border-white/10 rounded px-1.5 py-0.5">Esc</span>
          </div>

<!-- 结果 -->
        <div class="max-h-[50vh] overflow-y-auto">
          <!-- AI 智能解读：自然语言理解 + 候选结果语义化推荐 -->
          <div v-if="keyword.trim()" class="px-5 pt-4">
            <div class="flex items-center gap-3 flex-wrap">
              <button class="ai-search-btn" :disabled="aiBusy" @click="runAiSearch">
                {{ aiBusy ? '✦ AI 思考中…' : '✦ AI 智能解读' }}
              </button>
              <span class="text-[10px] text-gray-600">支持自然语言提问，如"怎么学习 Solidity"</span>
              <button v-if="aiText && !aiBusy" class="text-[10px] text-gray-500 hover:text-cyan-300 transition" @click="runAiSearch">重新解读</button>
            </div>
            <div v-if="aiText || aiBusy" class="ai-search-answer mt-3">
              <span class="whitespace-pre-wrap">{{ aiText }}</span><span v-if="aiBusy" class="animate-pulse">▍</span>
            </div>
          </div>
          <div v-if="searching" class="py-10 text-center text-xs text-gray-500">搜索中...</div>
          <template v-else-if="keyword.trim()">
            <div v-if="!searching && !groups.length" class="py-10 text-center text-xs text-gray-600">没有找到相关内容，换个关键词试试</div>
            <div v-for="g in groups" :key="g.key" class="group-section">
              <div class="flex items-center justify-between px-5 pt-4 pb-1.5">
                <span class="group-title">{{ g.label }} <span class="text-gray-600">· {{ g.items.length }}</span></span>
                <button class="text-[11px] text-gray-500 hover:text-cyan-300 transition-colors" @click="goAll(g)">查看全部 →</button>
              </div>
              <button v-for="r in g.items" :key="r.type + r.id" class="result-row group" @click="go(r)">
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2">
                    <span class="text-sm text-white group-hover:text-cyan-300 transition-colors truncate">{{ r.title }}</span>
                    <span v-if="r.badge" class="badge">{{ r.badge }}</span>
                  </div>
                  <p v-if="r.sub" class="text-xs text-gray-500 mt-1 line-clamp-1">{{ r.sub }}</p>
                </div>
                <span v-if="r.meta" class="text-[10px] text-gray-600 font-mono shrink-0">{{ r.meta }}</span>
              </button>
            </div>
          </template>
          <div v-else class="py-10 text-center text-xs text-gray-600">输入关键字开始搜索（文章 / 商品 / 社区 / 资源…）</div>
        </div>
      </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
// ============================================================
// 站内搜索面板（Shift+S 打开）
// 调用 /article/list?keyword= 实时搜索文章，回车确认跳转
// ============================================================
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUiStore } from '@/stores/modules/ui'
import { getBlogList } from '@/api/blog'
import { getForumList } from '@/api/forum'
import { getProductList } from '@/api/shop'
import { getBookList, getVideoList, getMusicList } from '@/api/media'
import { getToolList } from '@/api/tools'
import { getResourceList } from '@/api/resources'
import { getSoftwareList } from '@/api/software'
import { getSiteList } from '@/api/sites'
import { aiChatStream } from '@/api/freeAi'

const ui = useUiStore()
const router = useRouter()
const keyword = ref('')
const resultsByType = ref({})
const searching = ref(false)
const inputEl = ref(null)

// ── AI 智能解读（免费通道，流式）：结合关键词与站内候选结果给出语义化回答 ──
const aiBusy = ref(false)
const aiText = ref('')
let aiSeq = 0

let timer = null
let searchSeq = 0

// 各模块搜索归一化为统一条目 {type, title, sub, badge, meta, route, searchRoute}
const SECTIONS = [
  {
    key: 'article', label: '博客文章',
    fetch: kw => getBlogList({ page: 1, size: 8, keyword: kw }),
    map: r => ({ type: 'article', id: r.id, title: r.title, sub: r.summary || '', badge: r.category || '', meta: (r.createdAt || '').slice(0, 10), route: `/blog/post/${r.id}`, searchRoute: '/blog' })
  },
  {
    key: 'post', label: '社区帖子',
    fetch: kw => getForumList({ page: 1, size: 6, keyword: kw }),
    map: r => ({ type: 'post', id: r.id, title: r.title, sub: (r.content || '').replace(/[#>*`]/g, '').slice(0, 60), badge: r.category || '', meta: `${r.replyCount ?? 0} 回复`, route: `/community/post/${r.id}`, searchRoute: '/community' })
  },
  {
    key: 'product', label: '商城商品',
    fetch: kw => getProductList({ page: 1, size: 6, keyword: kw }),
    map: r => ({ type: 'product', id: r.id, title: r.name, sub: r.description || '', badge: r.category || '', meta: `¥${r.price}`, route: `/shop/${r.id}`, searchRoute: '/shop' })
  },
  {
    key: 'book', label: '书籍',
    fetch: kw => getBookList({ page: 1, size: 5, keyword: kw }),
    map: r => ({ type: 'book', id: r.id, title: r.title, sub: r.author ? `作者：${r.author}` : '', badge: r.category || '', meta: '', route: `/media/book/${r.id}`, searchRoute: '/media' })
  },
  {
    key: 'video', label: '视频',
    fetch: kw => getVideoList({ page: 1, size: 4, keyword: kw }),
    map: r => ({ type: 'video', id: r.id, title: r.title, sub: r.description || '', badge: '', meta: '', route: '/media', searchRoute: '/media' })
  },
  {
    key: 'music', label: '音乐',
    fetch: kw => getMusicList({ page: 1, size: 4, keyword: kw }),
    map: r => ({ type: 'music', id: r.id, title: r.title || '', sub: r.artist ? `歌手：${r.artist}` : '', badge: '', meta: '', route: '/media', searchRoute: '/media' })
  },
  {
    key: 'tool', label: '工具脚本',
    fetch: kw => getToolList({ page: 1, size: 5, keyword: kw }),
    map: r => ({ type: 'tool', id: r.id, title: r.name, sub: r.description || '', badge: r.category || '', meta: '', route: `/tools/${r.id}`, searchRoute: '/tools' })
  },
  {
    key: 'resource', label: '资源下载',
    fetch: kw => getResourceList({ page: 1, size: 5, keyword: kw }),
    map: r => ({ type: 'resource', id: r.id, title: r.title, sub: r.description || '', badge: r.category || '', meta: '', route: '/resources', searchRoute: '/resources' })
  },
  {
    key: 'software', label: '软件应用',
    fetch: kw => getSoftwareList({ page: 1, size: 5, keyword: kw }),
    map: r => ({ type: 'software', id: r.id, title: r.name, sub: r.description || '', badge: r.category || '', meta: '', route: `/software/${r.id}`, searchRoute: '/software' })
  },
  {
    key: 'site', label: '网址导航',
    fetch: kw => getSiteList({ category: '', keyword: kw, onlyVisible: true }),
    map: r => ({ type: 'site', id: r.id, title: r.name, sub: r.description || '', badge: '外链', meta: '', route: r.url, external: true, searchRoute: '/tools/sites' })
  }
]

const groups = computed(() =>
  SECTIONS.map(s => ({ ...s, items: resultsByType.value[s.key] || [] })).filter(g => g.items.length)
)

async function doSearch(kw) {
  if (!kw.trim()) { resultsByType.value = {}; return }
  searching.value = true
  const seq = ++searchSeq
  try {
    const entries = await Promise.allSettled(SECTIONS.map(async s => {
      const res = await s.fetch(kw.trim())
      const records = res?.data?.records ?? res?.records ?? res ?? []
      return [s.key, records.slice(0, 8).map(s.map)]
    }))
    if (seq !== searchSeq) return
    const acc = {}
    for (const e of entries) { if (e.status === 'fulfilled') acc[e.value[0]] = e.value[1] }
    resultsByType.value = acc
  } finally {
    if (seq === searchSeq) searching.value = false
  }
}

// 站内候选结果摘要（供 AI 解读引用，保留路径格式）
function resultsDigest() {
  const lines = []
  for (const g of groups.value) {
    for (const it of g.items.slice(0, 3)) {
      lines.push(`- [${g.label}] ${it.title}${it.sub ? '：' + String(it.sub).slice(0, 50) : ''} → ${it.route}`)
    }
  }
  return lines.length ? lines.join('\n') : '（站内没有匹配结果）'
}

async function runAiSearch() {
  const kw = keyword.value.trim()
  if (!kw || aiBusy.value) return
  const seq = ++aiSeq
  aiBusy.value = true
  aiText.value = ''
  const sys = [
    '你是极光数字空间的站内智能搜索助手。用户会输入关键词或自然语言问题。',
    '请用中文简洁作答（总字数 ≤150 字）：',
    '1) 先用一句话直接回答用户想找的东西或问题本身；',
    '2) 然后从下面「站内候选结果」中挑最相关的 1-3 项，逐条列出标题与路径（保留原路径格式，如 /blog/post/34）；',
    '3) 若候选为空，基于常识给简短建议，并提示可尝试的站内入口。',
    '不要编造不存在的站内数据。',
    '',
    '站内候选结果：',
    resultsDigest(),
    '',
    '站点主要入口：首页/ 博客/blog 社区/community 音乐馆/music 量化终端/quant 相册/album 资源/resources 工具/tools',
  ].join('\n')
  try {
    await aiChatStream([{ role: 'user', content: kw }], d => { if (seq === aiSeq) aiText.value += d }, { system: sys, max_tokens: 450 })
    if (seq === aiSeq) aiText.value = aiText.value || '（未返回内容）'
  } catch (e) {
    if (seq === aiSeq) aiText.value = '（AI 解读失败：' + (e?.message || '服务暂时不可用') + '）'
  } finally {
    if (seq === aiSeq) aiBusy.value = false
  }
}

watch(keyword, (kw) => {
  clearTimeout(timer)
  aiSeq++          // 使在途 AI 解读失效，避免旧答案串入新关键词
  aiText.value = ''
  timer = setTimeout(() => doSearch(kw), 300)
})

watch(() => ui.searchOpen, (open) => {
  if (open) {
    keyword.value = ui.pendingSearch || ''
    ui.pendingSearch = ''
    resultsByType.value = {}
    aiSeq++
    aiText.value = ''
    aiBusy.value = false
    if (keyword.value.trim()) doSearch(keyword.value)
    nextTick(() => inputEl.value?.focus())
  }
})

function go(r) {
  ui.setSearch(false)
  if (r.external) { window.open(r.route, '_blank', 'noopener'); return }
  router.push(r.route)
}

function goAll(g) {
  ui.setSearch(false)
  if (!g.items[0]) return
  const path = g.items[0].searchRoute || '/blog'
  const q = {}
  q[path === '/blog' ? 'kw' : 'keyword'] = keyword.value.trim()
  router.push({ path, query: q })
}

function close() { ui.setSearch(false) }

onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
.badge { @apply shrink-0 px-1.5 py-0.5 rounded-md text-[10px] text-purple-300 bg-purple-500/10 border border-purple-400/20; }
.group-title { @apply text-[11px] font-semibold text-cyan-300/90; }
.ai-search-btn {
  @apply inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-[11px] text-purple-200 transition-all
    bg-purple-500/15 border border-purple-400/30 hover:bg-purple-500/25 disabled:opacity-50 disabled:cursor-not-allowed;
}
.ai-search-answer {
  @apply rounded-xl border border-purple-400/20 bg-purple-500/[0.06] px-3.5 py-3 text-xs leading-relaxed text-gray-200;
}
.group-section { @apply border-b border-white/[0.04]; }
.group-section:last-child { @apply border-b-0; }
.result-row {
  @apply flex items-center gap-3 w-full text-left px-5 py-3.5 transition-colors
    hover:bg-[#121230] border-b border-white/[0.04] last:border-0;
}
.overlay-enter-active, .overlay-leave-active { transition: opacity 0.2s ease; }
.overlay-enter-from, .overlay-leave-to { opacity: 0; }
</style>
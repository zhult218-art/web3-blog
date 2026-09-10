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

const ui = useUiStore()
const router = useRouter()
const keyword = ref('')
const resultsByType = ref({})
const searching = ref(false)
const inputEl = ref(null)

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

watch(keyword, (kw) => {
  clearTimeout(timer)
  timer = setTimeout(() => doSearch(kw), 300)
})

watch(() => ui.searchOpen, (open) => {
  if (open) {
    keyword.value = ui.pendingSearch || ''
    ui.pendingSearch = ''
    resultsByType.value = {}
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
.group-section { @apply border-b border-white/[0.04]; }
.group-section:last-child { @apply border-b-0; }
.result-row {
  @apply flex items-center gap-3 w-full text-left px-5 py-3.5 transition-colors
    hover:bg-[#121230] border-b border-white/[0.04] last:border-0;
}
.overlay-enter-active, .overlay-leave-active { transition: opacity 0.2s ease; }
.overlay-enter-from, .overlay-leave-to { opacity: 0; }
</style>
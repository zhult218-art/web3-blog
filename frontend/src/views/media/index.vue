<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl space-y-8">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <h1 class="text-3xl font-bold text-gradient-cyber">在线媒体</h1>
        <!-- 顶部标签页：音乐馆 / 图书 / 视频 -->
        <div class="flex gap-1 p-1 rounded-xl glass-panel-sm">
          <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
            class="px-4 py-1.5 rounded-lg text-sm font-medium transition"
            :class="activeTab === tab.key ? 'bg-purple-500/25 text-white border border-purple-400/30' : 'text-gray-500 hover:text-white border border-transparent'">
            <span class="mr-1">{{ tab.icon }}</span>{{ tab.label }}
          </button>
        </div>
      </div>

      <!-- 音乐馆模块 -->
      <template v-if="activeTab === 'music'">
        <MusicHall />
      </template>

      <!-- 图书模块 -->
      <template v-else-if="activeTab === 'book'">
        <section class="glass-panel p-6">
          <div class="flex items-center justify-between gap-3 mb-4">
            <h2 class="text-xl font-bold text-white flex items-center gap-2"><span>📜</span>书籍</h2>
            <span v-if="bookList.length" class="text-[11px] text-gray-500">{{ bookList.length }} 部古籍</span>
          </div>

          <!-- 分类过滤 -->
          <div v-if="bookCategories.length" class="flex flex-wrap items-center gap-2 mb-5">
            <button v-for="cat in bookCategories" :key="cat"
              @click="bookCategory = cat"
              class="px-3 py-1 rounded-full text-xs font-medium transition border"
              :class="bookCategory === cat
                ? 'bg-amber-500/25 text-amber-200 border-amber-400/40'
                : 'text-gray-500 hover:text-white border-white/10 hover:border-amber-400/20'">
              {{ cat }}
            </button>
          </div>

          <div v-if="filteredBooks.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <div v-for="book in filteredBooks" :key="book.id" @click="openBook(book)"
              class="group glass-panel-sm p-4 cursor-pointer transition-all duration-300 hover:-translate-y-0.5 hover:border-amber-400/30">
              <div class="flex items-start gap-3">
                <div class="w-12 h-16 flex-shrink-0 rounded-sm bg-gradient-to-br from-amber-500/40 to-red-900/40 border border-amber-300/20 flex flex-col items-center justify-center shadow-lg">
                  <span class="text-lg font-bold text-amber-100 font-serif" style="writing-mode: vertical-rl; letter-spacing: 0.15em;">{{ book.title.slice(0, 4) }}</span>
                </div>
                <div class="min-w-0 flex-1">
                  <h4 class="font-semibold text-white text-sm group-hover:text-amber-300 transition line-clamp-1 font-serif">{{ book.title }}</h4>
                  <p class="text-[11px] text-gray-500 mt-1">
                    <span v-if="book.author">{{ book.author }}</span><span v-if="book.dynasty"> · {{ book.dynasty }}</span>
                  </p>
                  <p v-if="book.description" class="text-[11px] text-gray-600 mt-1.5 line-clamp-2">{{ book.description }}</p>
                  <div class="flex items-center gap-3 mt-2 text-[10px] text-gray-600">
                    <span class="flex items-center gap-1"><span class="text-amber-400/70">共</span>{{ book.chapterCount }} 章</span>
                    <span v-if="book.category" class="px-1.5 py-0.5 rounded-full border border-white/10">{{ book.category }}</span>
                    <span v-if="book.categorySub" class="px-1.5 py-0.5 rounded-full border border-white/10">{{ book.categorySub }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="py-16 text-center">
            <div class="text-4xl mb-3 opacity-15">📜</div>
            <p class="text-xs text-gray-600">暂无书籍数据</p>
          </div>
        </section>
      </template>

      <!-- 视频模块 -->
      <template v-else>
        <section class="glass-panel p-6">
          <div class="flex flex-wrap items-center justify-between gap-3 mb-5">
            <h2 class="text-xl font-bold text-white flex items-center gap-2"><span>🎬</span>视频</h2>
            <div class="flex gap-1 p-1 rounded-xl glass-panel-sm">
              <button v-for="tab in videoTabs" :key="tab.key" @click="videoTab = tab.key"
                class="px-3 py-1.5 rounded-lg text-sm font-medium transition"
                :class="videoTab === tab.key ? 'bg-cyan-500/25 text-white border border-cyan-400/30' : 'text-gray-500 hover:text-white border border-transparent'">
                <span class="mr-1">{{ tab.icon }}</span>{{ tab.label }}
              </button>
            </div>
          </div>

          <!-- 在线视频（后端上传） -->
          <template v-if="videoTab === 'online'">
            <div v-if="videoList.length" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="item in videoList" :key="item.id" class="glass-panel-sm overflow-hidden group cursor-pointer">
                <div class="relative">
                  <video class="w-full aspect-video bg-black/60" controls :src="item.url" :poster="item.cover"
                    preload="metadata" @play="onVideoPlay" @pause="onVideoPause"></video>
                </div>
                <div class="p-4">
                  <h4 class="font-semibold text-white text-sm group-hover:text-cyan-300 transition">{{ item.title }}</h4>
                  <p class="text-xs text-gray-500 mt-1.5 line-clamp-2">{{ item.description }}</p>
                  <div class="flex flex-wrap gap-3 mt-2 text-[10px] text-gray-600">
                    <span v-if="item.duration">{{ formatDuration(item.duration) }}</span>
                    <span v-if="item.tags" class="flex flex-wrap gap-1">
                      <span v-for="tag in String(item.tags).split(',').map(s => s.trim()).filter(Boolean)" :key="tag"
                        class="px-1.5 py-0.5 rounded-full border border-cyan-400/20 text-cyan-300/80">{{ tag }}</span>
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="py-16 text-center">
              <div class="text-4xl mb-3 opacity-15">🎬</div>
              <p class="text-xs text-gray-600">暂无在线视频</p>
            </div>
          </template>

          <!-- 动漫推荐 -->
          <template v-else-if="videoTab === 'anime'">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="item in ANIME_RECOMMENDS" :key="item.title"
                class="glass-panel-sm p-5 group cursor-pointer transition-all duration-300 hover:-translate-y-0.5 hover:border-pink-400/30">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex items-center gap-2.5">
                    <span class="text-2xl">🍙</span>
                    <div>
                      <h4 class="font-semibold text-white text-sm group-hover:text-pink-300 transition">{{ item.title }}</h4>
                      <p class="text-[11px] text-gray-500 mt-0.5">{{ item.year }} · {{ item.tags.join(' / ') }}</p>
                    </div>
                  </div>
                  <span class="text-[11px] font-bold text-pink-300/90 bg-pink-500/10 border border-pink-400/20 rounded-full px-2 py-0.5">{{ item.rating }}</span>
                </div>
                <p class="text-[11px] text-gray-500 mt-3 line-clamp-3 leading-relaxed">{{ item.desc }}</p>
                <div class="flex flex-wrap items-center gap-2 mt-4">
                  <a :href="item.link" target="_blank" rel="noopener"
                    class="text-[11px] px-2.5 py-1 rounded-lg bg-pink-500/15 text-pink-300 border border-pink-400/20 hover:bg-pink-500/25 transition">▶ 去观看</a>
                  <a v-for="ln in item.links" :key="ln.label" :href="ln.url" target="_blank" rel="noopener"
                    class="text-[11px] px-2.5 py-1 rounded-lg border border-white/10 text-gray-400 hover:text-white hover:border-pink-400/20 transition">{{ ln.label }} ↗</a>
                </div>
              </div>
            </div>
          </template>

          <!-- 影视剧集 -->
          <template v-else-if="videoTab === 'movie'">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="item in MOVIE_LINKS" :key="item.title"
                class="glass-panel-sm p-5 group cursor-pointer transition-all duration-300 hover:-translate-y-0.5 hover:border-cyan-400/30">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex items-center gap-2.5">
                    <span class="text-2xl">🎬</span>
                    <div>
                      <h4 class="font-semibold text-white text-sm group-hover:text-cyan-300 transition">{{ item.title }}</h4>
                      <p class="text-[11px] text-gray-500 mt-0.5">{{ item.year }} · {{ item.tags.join(' / ') }}</p>
                    </div>
                  </div>
                  <span class="text-[11px] font-bold text-cyan-300/90 bg-cyan-500/10 border border-cyan-400/20 rounded-full px-2 py-0.5">{{ item.rating }}</span>
                </div>
                <p class="text-[11px] text-gray-500 mt-3 line-clamp-3 leading-relaxed">{{ item.desc }}</p>
                <div class="flex flex-wrap items-center gap-2 mt-4">
                  <a v-for="ln in item.links" :key="ln.label" :href="ln.url" target="_blank" rel="noopener"
                    class="text-[11px] px-2.5 py-1 rounded-lg bg-cyan-500/15 text-cyan-300 border border-cyan-400/20 hover:bg-cyan-500/25 transition">{{ ln.label }} ▶</a>
                </div>
              </div>
            </div>
          </template>

          <!-- 磁力资源 -->
          <template v-else>
            <p class="text-[11px] text-gray-500 mb-3">点击「复制磁力」即可一键复制到剪贴板，粘贴到下载器（如 qBittorrent / 迅雷）解析下载。</p>
            <div class="space-y-2.5">
              <div v-for="m in MAGNET_RESOURCES" :key="m.name"
                class="glass-panel-sm p-4 group flex flex-wrap items-center gap-3">
                <span class="text-xl">🧲</span>
                <div class="flex-1 min-w-0">
                  <h4 class="font-semibold text-white text-sm truncate group-hover:text-cyan-300 transition">{{ m.name }}</h4>
                  <p class="text-[11px] text-gray-500 mt-0.5">{{ m.count }} 个文件 · {{ m.size }} · {{ m.source }}</p>
                  <p class="text-[10px] text-gray-600 font-mono mt-1 truncate">{{ m.hash }}</p>
                </div>
                <button @click="copyHash(m.hash, m.name)"
                  class="text-[11px] px-3 py-1.5 rounded-lg bg-cyan-500/15 text-cyan-300 border border-cyan-400/25 hover:bg-cyan-500/25 transition whitespace-nowrap">
                  {{ copiedHash === m.hash ? '✓ 已复制' : '复制磁力' }}
                </button>
              </div>
            </div>
          </template>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 在线媒体页：音乐馆 / 图书 / 视频 三大模块标签页
// 音乐馆为完整音乐馆组件；图书与视频在此内联
// 播放条由 App.vue 级 GlobalPlayer 承载
// ============================================================
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import MusicHall from './MusicHall.vue'
import { getVideoList, getBookList } from '@/api/media'
import { usePlayerStore } from '@/stores/modules/player'
import { VIDEO_TABS, ANIME_RECOMMENDS, MOVIE_LINKS, MAGNET_RESOURCES } from '@/data/mediaResources'

const router = useRouter()
const player = usePlayerStore()
const videoList = ref([])
const bookList = ref([])
const videoTab = ref('online')
const bookCategory = ref('全部')
const copiedHash = ref('')

const tabs = [
  { key: 'music', label: '音乐馆', icon: '🎵' },
  { key: 'book', label: '图书', icon: '📜' },
  { key: 'video', label: '视频', icon: '🎬' },
]
const activeTab = ref('music')

// 书籍分类列表（自动取 book.category 去重；无分类时默认「全部」）
const bookCategories = computed(() => {
  const set = new Set(['全部'])
  bookList.value.forEach(b => { if (b.category) set.add(b.category) })
  return [...set]
})

// 当前分类下的书籍
const filteredBooks = computed(() => {
  if (bookCategory.value === '全部') return bookList.value
  return bookList.value.filter(b => b.category === bookCategory.value)
})

// 秒数格式化为「m:ss」
function formatDuration(secs) {
  if (!secs) return '--:--'
  const n = Number(secs)
  if (!isFinite(n)) return String(secs)
  const m = Math.floor(n / 60)
  const s = Math.floor(n % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

// 复制磁力链接到剪贴板
async function copyHash(hash, name) {
  try {
    await navigator.clipboard.writeText(hash)
    copiedHash.value = hash
    setTimeout(() => { copiedHash.value = '' }, 2000)
  } catch {
    const el = document.createElement('textarea')
    el.value = hash
    el.style.position = 'fixed'
    el.style.opacity = '0'
    document.body.appendChild(el)
    el.select()
    try { document.execCommand('copy') } catch {}
    document.body.removeChild(el)
    copiedHash.value = hash
    setTimeout(() => { copiedHash.value = '' }, 2000)
  }
}

// 跳转到电子书阅读页
function openBook(book) {
  router.push(`/media/book/${book.id}`)
}

// 播放视频时自动暂停全局音乐
function onVideoPlay() { if (player.isPlaying) player.pause() }

// 挂载时并行加载视频/图书两类列表数据（音乐列表由 MusicHall 自行加载）
onMounted(() => {
  getVideoList({ page: 1, size: 50 }).then(res => {
    const data = res.data?.records || res.data || []
    videoList.value = data
    if (!data.length) videoList.value = []
  }).catch(() => { videoList.value = [] })

  getBookList({ page: 1, size: 50 }).then(res => {
    const data = res.data?.records || res.data || []
    bookList.value = data
  }).catch(() => { bookList.value = [] })
})
</script>

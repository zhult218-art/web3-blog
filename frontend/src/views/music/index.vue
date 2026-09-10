<template>
  <div class="min-h-screen px-4 md:px-6 py-10 sylva-music">
    <div class="mx-auto max-w-6xl space-y-6">
      <!-- 馆头 -->
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="s-yebrow">Into the Living Sounds</p>
          <h1 class="s-title">
            音乐馆<span class="s-dot">.</span>
            <span class="s-vinyl"><span></span></span>
          </h1>
          <div class="s-rule"></div>
          <p class="text-[13px] tracking-[0.08em] text-gray-400 mt-3">搜索、聆听、驻足 · 点击任意曲目即可全局播放</p>
        </div>
        <div v-if="player.hasTrack" class="flex items-center gap-3 glass-panel px-4 py-2">
          <span class="text-[11px] text-cyan-300 animate-pulse max-w-40 truncate">♪ {{ player.currentTrack?.title }}</span>
          <button @click="player.toggle" class="text-white text-sm hover:text-cyan-300 transition" :title="player.isPlaying ? '暂停' : '播放'">
            {{ player.isPlaying ? '⏸' : '▶' }}
          </button>
          <button v-if="player.playerBarVisible" @click="player.hideBar" class="text-gray-500 hover:text-white transition text-xs" title="收起播放器">✕</button>
          <button v-else @click="player.showBar" class="text-[11px] text-cyan-300 hover:text-cyan-200 transition border border-cyan-400/30 rounded-lg px-2.5 py-1" title="显示播放器">▶ 显示播放器</button>
        </div>
      </div>

      <!-- 搜索栏 -->
      <div class="glass-panel p-4">
        <div class="flex gap-3">
          <div class="flex-1 relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-600 text-sm">🔍</span>
            <input v-model="keyword" @keyup.enter="doSearch" type="text"
              placeholder="搜索歌曲 / 歌手 / 专辑，如：周杰伦"
              class="w-full bg-[#10102a] border border-white/10 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder-gray-600 outline-none focus:border-purple-400/40 transition" />
          </div>
          <button @click="doSearch" :disabled="searching"
            class="px-6 rounded-xl text-sm font-medium s-search-btn transition disabled:opacity-50">
            {{ searching ? '搜索中...' : '搜索' }}
          </button>
        </div>

        <!-- 搜索结果 -->
        <div v-if="searchDone" class="mt-4">
          <p class="text-[11px] text-gray-500 mb-2">
            {{ searchResults.length ? `找到 ${searchResults.length} 首，点击播放` : '没有找到相关音乐，换个关键词试试' }}
          </p>
          <div v-if="searchResults.length" class="max-h-72 overflow-y-auto space-y-1">
            <div v-for="(t, i) in searchResults" :key="t.id"
              class="group flex items-center gap-3 p-2.5 rounded-xl cursor-pointer transition-all border border-transparent hover:bg-[#0e0e26] hover:border-cyan-400/15"
              @click="playSearchResult(t)">
              <span class="w-6 text-center text-xs text-gray-600 group-hover:hidden font-mono">{{ i + 1 }}</span>
              <span class="w-6 text-center hidden group-hover:block text-cyan-300 text-xs">▶</span>
              <div class="w-9 h-9 rounded-md bg-gradient-to-br from-cyan-500/30 to-purple-500/30 flex-shrink-0 overflow-hidden">
                <img v-if="t.cover" :src="t.cover" class="w-full h-full object-cover" alt="" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm text-white truncate" :class="{ 'text-cyan-300': player.currentTrack?.id === t.id }">{{ t.title }}</p>
                <p class="text-[11px] text-gray-500 truncate">{{ t.artist }}</p>
              </div>
              <span class="text-[11px] text-gray-600 font-mono hidden sm:inline">{{ formatDuration(t.duration) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 我的音乐 + 精选歌单 并排 -->
      <section class="grid gap-6 lg:grid-cols-2">
        <!-- 我的音乐 -->
        <div class="glass-panel p-6 min-w-0">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-sm font-bold text-white flex items-center gap-2">
              <span class="text-[color:var(--color-primary)]">💿</span> 我的音乐
              <span class="text-[11px] text-gray-600 font-normal">共 {{ myMusic.length }} 首</span>
            </h2>
          </div>
          <div v-if="listLoading" class="py-12 text-center text-xs text-gray-600">加载中...</div>
          <div v-else-if="myMusic.length" class="space-y-1 max-h-[500px] overflow-y-auto pr-1 music-scroll">
            <div v-for="(item, i) in myMusic" :key="item.id"
              class="group flex items-center gap-4 p-3 rounded-2xl cursor-pointer transition-all duration-200 border border-transparent hover:bg-[#0e0e26] hover:border-purple-400/15"
              @click="openPlayer(item)">
              <span class="w-7 text-center text-sm text-gray-600 group-hover:hidden font-mono">{{ String(i + 1).padStart(2, '0') }}</span>
              <span class="w-7 text-center hidden group-hover:block text-purple-300">
                <svg class="w-4 h-4 mx-auto" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
              </span>
              <div class="w-12 h-12 rounded-full bg-gradient-to-br from-purple-500/30 to-cyan-500/30 flex-shrink-0 overflow-hidden border border-white/10 relative">
                <img v-if="item.cover" :src="item.cover" class="w-full h-full object-cover" alt="" />
                <span v-else class="w-full h-full flex items-center justify-center text-lg text-white/70">{{ (item.title || '♪')[0] }}</span>
                <span v-if="isCurrent(item)" class="absolute inset-0 flex items-center justify-center bg-black/50 text-cyan-300 text-lg">♪</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm text-white truncate" :class="{ 'text-cyan-300': isCurrent(item) }">{{ item.title }}</p>
                <p class="text-[11px] text-gray-500 mt-0.5 truncate">
                  {{ item.artist || '未知艺术家' }}
                  <span v-if="item.album"> · {{ item.album }}</span>
                  <span v-if="item.category" class="text-gray-600"> · {{ item.category }}</span>
                </p>
              </div>
              <span class="text-[11px] text-gray-600 font-mono hidden sm:inline">{{ formatDuration(item.duration) }}</span>
              <span class="text-purple-300/70 text-xs transition-transform duration-200 group-hover:translate-x-1">→</span>
            </div>
          </div>
          <div v-else class="py-12 text-center">
            <div class="text-4xl mb-3 opacity-15">💿</div>
            <p class="text-xs text-gray-600">暂无音乐数据</p>
          </div>
        </div>

        <!-- 精选歌单（真实歌单代理） -->
        <div class="glass-panel p-6 min-w-0">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-sm font-bold text-white flex items-center gap-2">
              <span class="text-[color:var(--color-primary)]">🎧</span> 精选歌单
              <span class="text-[11px] text-gray-600 font-normal">「{{ neteasePlaylist?.name || '歌单' }}」共 {{ displayTracks.length }} 首</span>
            </h2>
            <span v-if="player.isPlaying && isCurrent(neteaseTracks[0])" class="text-[11px] text-cyan-300 animate-pulse">正在播放</span>
          </div>
          <div v-if="neteaseLoading" class="py-12 text-center text-xs text-gray-600">歌单加载中...</div>
          <div v-else-if="displayTracks.length" class="space-y-1 max-h-[500px] overflow-y-auto pr-1 music-scroll">
            <div v-for="(t, i) in displayTracks" :key="t.id"
              class="group flex items-center gap-3 p-3 rounded-2xl cursor-pointer transition-all duration-200 border border-transparent hover:bg-[#0e0e26] hover:border-cyan-400/15"
              @click="playNetease(t)">
              <span class="w-7 text-center text-sm text-gray-600 group-hover:hidden font-mono">{{ String(i + 1).padStart(2, '0') }}</span>
              <span class="w-7 text-center hidden group-hover:block text-cyan-300">
                <svg class="w-4 h-4 mx-auto" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
              </span>
              <div class="w-12 h-12 rounded-lg bg-gradient-to-br from-cyan-500/30 to-purple-500/30 flex-shrink-0 overflow-hidden relative">
                <img v-if="t.cover" :src="t.cover" class="w-full h-full object-cover" alt="" />
                <span v-if="isCurrent(t)" class="absolute inset-0 flex items-center justify-center bg-black/50 text-cyan-300 text-lg">♪</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm text-white truncate" :class="{ 'text-cyan-300': isCurrent(t) }">{{ t.title }}</p>
                <p class="text-[11px] text-gray-500 mt-0.5 truncate">{{ t.artist }}</p>
              </div>
              <span class="text-[11px] text-gray-600 font-mono hidden sm:inline">{{ formatDuration(t.duration) }}</span>
            </div>
          </div>
          <div v-else class="py-12 text-center">
            <div class="text-4xl mb-3 opacity-15">🎵</div>
            <p class="text-xs text-gray-600">歌单加载失败，请检查服务</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 音乐馆首页
// 顶部搜索栏：网易云搜索 → 结果一键全局播放
// 下方双栏并排：我的音乐 / 精选歌单（真实歌单代理）
// 正在播放的歌曲自动置顶；全局播放条由 App.vue 级 GlobalPlayer 承载
// ============================================================
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMusicList } from '@/api/media'
import { getNeteasePlaylist, getNeteaseSongUrl, searchNetease } from '@/api/music'
import { usePlayerStore } from '@/stores/modules/player'

const router = useRouter()
const musicList = ref([])
const listLoading = ref(true)

const player = usePlayerStore()
const neteaseReady = ref(false)
const neteaseLoading = ref(false)
const neteasePlaylist = ref(null)
const neteaseTracks = ref([])
const neteasePlaying = ref(false)

// 搜索
const keyword = ref('')
const searching = ref(false)
const searchDone = ref(false)
const searchResults = ref([])

const NETEASE_PLAYLIST_ID = 652135520

// 当前播放曲目置顶排序
function sortPlayingFirst(list) {
  const curId = player.currentTrack?.id
  if (!curId) return list
  return [...list].sort((a, b) => {
    const pa = a.id === curId ? -1 : 0
    const pb = b.id === curId ? -1 : 0
    return pa - pb
  })
}

const isCurrent = t => player.currentTrack?.id === t.id

const myMusic = computed(() => sortPlayingFirst(musicList.value))
const displayTracks = computed(() => sortPlayingFirst(neteaseTracks.value))

// ---- 搜索 ----
async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw || searching.value) return
  searching.value = true
  searchDone.value = true
  try {
    const res = await searchNetease(kw, 20)
    const list = (res?.result?.songs || []).map(s => ({
      id: s.id,
      title: s.name,
      artist: (s.artists || []).map(a => a.name).join(' / ') || '未知歌手',
      cover: s.album?.picUrl || '',
      duration: Math.round((s.duration || 0) / 1000),
      url: '',
    }))
    searchResults.value = list
    void preloadUrls(list)
  } catch {
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

// 点击搜索结果：替换播放列表并播放该曲
async function playSearchResult(track) {
  if (!track.url) {
    try {
      const r = await getNeteaseSongUrl(track.id)
      track.url = r?.data?.[0]?.url || ''
    } catch { /* 静默 */ }
  }
  if (!track.url) return
  player.setPlaylist(searchResults.value.filter(t => t.url))
  player.play({ ...track })
}

// 后台预取播放地址（并行，失败不影响列表）
async function preloadUrls(tracks) {
  const list = await Promise.all(tracks.map(async t => {
    try {
      const r = await getNeteaseSongUrl(t.id)
      return { ...t, url: r?.data?.[0]?.url || '' }
    } catch { return t }
  }))
  neteaseTracks.value = list
}

// ---- 网易云歌单 ----
async function loadNetease() {
  neteaseLoading.value = true
  try {
    const res = await getNeteasePlaylist(NETEASE_PLAYLIST_ID, 60)
    const pl = res?.playlist
    if (!pl?.tracks?.length) { neteaseReady.value = false; return }
    neteasePlaylist.value = { name: pl.name, cover: pl.coverImgUrl }
    const tracks = pl.tracks.slice(0, 60).map(t => ({
      id: t.id,
      title: t.name,
      artist: (t.ar || []).map(a => a.name).join(' / ') || '未知歌手',
      cover: t.al?.picUrl || '',
      duration: Math.round((t.dt || 0) / 1000),
      url: '',
    }))
    neteaseTracks.value = tracks
    neteaseReady.value = true
    void preloadUrls(tracks)
  } catch { neteaseReady.value = false }
  finally { neteaseLoading.value = false }
}

// 点击歌单歌曲：整体替换播放列表并播放该曲
async function playNetease(track) {
  if (!track.url) {
    try {
      const r = await getNeteaseSongUrl(track.id)
      track.url = r?.data?.[0]?.url || ''
    } catch { /* 静默 */ }
  }
  if (!track.url) return
  player.setPlaylist(neteaseTracks.value.filter(t => t.url))
  player.play({ ...track })
  neteasePlaying.value = true
}

// ---- 本地音乐 ----
const DEFAULT_CATS = ['电子', '流行', '古典', '嘻哈', '摇滚', '轻音乐', '纯音乐']

function formatDuration(secs) {
  if (!secs) return '--:--'
  const n = Number(secs)
  if (!isFinite(n)) return String(secs)
  const m = Math.floor(n / 60)
  const s = Math.floor(n % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

function parseCategory(t, i) {
  if (t.category) return t.category
  const tags = (t.tags || '').split(',').map(s => s.trim()).filter(Boolean)
  const hit = tags.find(tag => DEFAULT_CATS.includes(tag))
  if (hit) return hit
  if (tags.length) return tags[0]
  return DEFAULT_CATS[i % DEFAULT_CATS.length]
}

function openPlayer(item) {
  router.push(`/music/player/${item.id}`)
}

onMounted(() => {
  getMusicList({ page: 1, size: 100 }).then(res => {
    const data = res.data?.records || res.data || []
    musicList.value = data.map((t, i) => ({
      ...t,
      id: t.id || t.musicId || i + 1,
      duration: t.duration || 0,
      category: parseCategory(t, i),
      url: t.url || '',
    }))
  }).catch(() => { musicList.value = [] }).finally(() => { listLoading.value = false })

  loadNetease()
})
</script>

<style scoped>
.vinyl-mini {
  display: inline-block;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 50%, #1c1c1c 0%, #0a0a0a 60%, #000 100%);
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.05), 0 2px 8px rgba(0, 0, 0, 0.6);
  position: relative;
}

.vm-label {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a855f7, #06b6d4);
}

.music-scroll{
  scrollbar-width: thin;
  scrollbar-color: rgba(159, 176, 140, 0.35) transparent;
}
.music-scroll::-webkit-scrollbar {
  width: 5px;
}
.music-scroll::-webkit-scrollbar-thumb {
  background: rgba(159, 176, 140, 0.35);
  border-radius: 999px;
}
.music-scroll::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 999px;
}

/* ── Sylva editorial 编辑风 ─────────────────────────────────────── */
.sylva-music {
  --s-moss: #a8b593;
  --s-moss-bright: #c9d6b8;
  --s-rule: rgba(255, 255, 255, 0.07);
}
.s-yebrow {
  font-size: 11px;
  letter-spacing: 0.4em;
  text-transform: uppercase;
  color: var(--s-moss);
  margin-bottom: 10px;
}
.s-title {
  font-family: 'Lexend', 'Segoe UI', system-ui, sans-serif;
  font-size: clamp(2.75rem, 6vw, 4rem);
  font-weight: 300;
  letter-spacing: -0.015em;
  line-height: 1;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 14px;
}
.s-dot { color: var(--s-moss); font-weight: 600; }
.s-rule {
  width: 56px;
  height: 1px;
  background: var(--s-moss);
  opacity: 0.55;
  margin-top: 14px;
}
.s-vinyl {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 50%, #23261f 0%, #15170f 70%, #0c0d09 100%);
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.06), 0 4px 14px rgba(0, 0, 0, 0.55);
  position: relative;
  display: inline-flex;
}
.s-vinyl span {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: radial-gradient(circle, #f2f3ef 0%, #a8b593 55%, #6f7d5c 100%);
  box-shadow: 0 0 10px rgba(168, 181, 147, 0.45);
}
.s-search-btn {
  background: var(--s-moss);
  color: #14160f;
  box-shadow: 0 6px 18px rgba(168, 181, 147, 0.22);
  transition: all 0.2s;
}
.s-search-btn:hover:not(:disabled) {
  background: var(--s-moss-bright);
  box-shadow: 0 8px 22px rgba(201, 214, 184, 0.3);
  transform: translateY(-1px);
}
.sylva-music :deep(.glass-panel) {
  background: rgba(242, 243, 239, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(10px);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.03) inset;
}
.sylva-music :deep(.group:hover) {
  border-color: rgba(168, 181, 147, 0.25) !important;
  background: rgba(168, 181, 147, 0.04) !important;
}
.sylva-music :deep(.text-cyan-300) { color: var(--s-moss-bright) !important; }
.sylva-music :deep(input:focus) { border-color: rgba(168, 181, 147, 0.4) !important; }
</style>

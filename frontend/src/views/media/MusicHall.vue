<template>
  <div class="sylva-music">
    <div class="space-y-6">
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
          <button @click="cycleMode" class="text-sm transition" :class="player.isPlaying ? 'text-white hover:text-cyan-300' : 'text-gray-400 hover:text-white'" :title="modeTitle">{{ modeIcon }}</button>
          <button @click="player.toggle" class="text-white text-sm hover:text-cyan-300 transition" :title="player.isPlaying ? '暂停' : '播放'">
            {{ player.isPlaying ? '⏸' : '▶' }}
          </button>
          <button v-if="player.playerBarVisible" @click="player.hideBar" class="text-gray-500 hover:text-white transition text-xs" title="收起播放器">✕</button>
          <button v-else @click="player.showBar" class="text-[11px] text-cyan-300 hover:text-cyan-200 transition border border-cyan-400/30 rounded-lg px-2.5 py-1" title="显示播放器">▶ 显示播放器</button>
        </div>
      </div>

      <!-- 曲库来源切换：默认走免费全曲库（Audius），开箱可用、无需任何后端服务 -->
      <div class="glass-panel px-3 py-2 flex flex-wrap items-center gap-1.5">
        <button v-for="s in SOURCES" :key="s.key" @click="switchSource(s.key)"
          class="px-3.5 py-1.5 rounded-xl text-[12px] transition border"
          :class="source === s.key
            ? 'bg-[rgba(168,181,147,0.16)] border-[rgba(168,181,147,0.45)] text-white'
            : 'border-transparent text-gray-400 hover:text-white hover:bg-white/5'">
          {{ s.icon }} {{ s.label }}
        </button>
        <span class="text-[11px] text-gray-600 ml-auto pr-2 hidden sm:inline">{{ SOURCE_HINT[source] }}</span>
      </div>

      <!-- 搜索栏 -->
      <div class="glass-panel p-4">
        <div class="flex gap-3">
          <div class="flex-1 relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-600 text-sm">🔍</span>
            <input v-model="keyword" @keyup.enter="doSearch" type="text"
              :placeholder="searchPlaceholder"
              class="w-full bg-[#10102a] border border-white/10 rounded-xl pl-9 pr-4 py-2.5 text-sm text-white placeholder-gray-600 outline-none focus:border-purple-400/40 transition" />
          </div>
          <button @click="doSearch" :disabled="searching"
            class="px-6 rounded-xl text-sm font-medium s-search-btn transition disabled:opacity-50">
            {{ searching ? '搜索中...' : '搜索' }}
          </button>
        </div>

        <!-- 搜索历史（localStorage 持久化，最多 10 条） -->
        <div v-if="!searchDone && searchHistory.length" class="mt-3 flex flex-wrap items-center gap-2">
          <span class="text-[11px] text-gray-600">最近搜索</span>
          <button v-for="kw in searchHistory" :key="kw" @click="searchFromHistory(kw)"
            class="text-[11px] px-2.5 py-1 rounded-full border border-white/10 text-gray-400 hover:text-white hover:border-[rgba(168,181,147,0.4)] transition">
            {{ kw }}
          </button>
          <button @click="clearHistory" class="text-[11px] text-gray-600 hover:text-red-300 transition ml-1" title="清空搜索历史">清空</button>
        </div>

        <!-- 搜索结果 -->
        <div v-if="searchDone" class="mt-4">
          <p class="text-[11px] text-gray-500 mb-2">
            {{ searchError
              ? (searchServiceDown ? '搜索失败：网易云服务未启动（backend/netease-music-api，端口3000）' : '搜索失败，请稍后重试')
              : (searchResults.length ? `在「${activeSource.label}」找到 ${searchResults.length} 首，点击播放` : '没有找到相关音乐，换个关键词试试') }}
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
              <span v-if="musicSource === 'supabase'" class="text-[9px] px-1.5 py-0.5 rounded-full border border-emerald-400/30 text-emerald-300">Supabase</span>
            </h2>
            <button @click="playAllMyMusic" :disabled="!playableMyMusic.length" class="s-mini-btn" title="按列表顺序播放全部">▶ 播放全部</button>
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
                <p class="text-sm text-white truncate flex items-center" :class="{ 'text-cyan-300': isCurrent(item) }">
                  <span class="truncate">{{ item.title }}</span>
                  <span v-if="isCurrent(item)" class="s-eq" :class="{ 's-eq-paused': !player.isPlaying }" aria-hidden="true"><i></i><i></i><i></i></span>
                </p>
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
            <p class="text-xs text-gray-600">{{ localError ? '列表加载失败' : '暂无音乐数据' }}</p>
            <button v-if="localError" @click="loadLocalMusic" class="s-mini-btn mt-3">↻ 重试</button>
          </div>
        </div>

        <!-- 云上曲库：Audius 全曲 / iTunes 试听 / 网易云（可选） -->
        <div class="glass-panel p-6 min-w-0">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-sm font-bold text-white flex items-center gap-2">
              <span class="text-[color:var(--color-primary)]">{{ activeSource.icon }}</span> {{ activeSource.panel }}
              <span class="text-[11px] text-gray-600 font-normal">{{ cloudTitle || `共 ${cloudTracks.length} 首` }}</span>
            </h2>
            <div class="flex items-center gap-2">
              <span v-if="player.isPlaying && displayTracks.length && isCurrent(displayTracks[0])" class="text-[11px] text-cyan-300 animate-pulse">正在播放</span>
              <button @click="playAllCloud" :disabled="!cloudTracks.length" class="s-mini-btn" title="按列表顺序播放全部">▶ 播放全部</button>
            </div>
          </div>
          <!-- 分区 chips：Audius 流派 / iTunes 精选歌单 -->
          <div v-if="chips.length" class="flex flex-wrap gap-2 mb-3">
            <button v-for="c in chips" :key="c.key" @click="loadChip(c)" :disabled="cloudLoading"
              class="text-[11px] px-2.5 py-1 rounded-full border transition disabled:opacity-50"
              :class="activeChipKey === c.key
                ? 'border-[rgba(168,181,147,0.5)] text-white bg-[rgba(168,181,147,0.12)]'
                : 'border-white/10 text-gray-400 hover:text-white hover:border-[rgba(168,181,147,0.4)]'">
              {{ c.label }}
            </button>
          </div>
          <div v-if="cloudLoading" class="py-12 text-center text-xs text-gray-600">加载中...</div>
          <div v-else-if="cloudTracks.length" class="space-y-1 max-h-[460px] overflow-y-auto pr-1 music-scroll">
            <div v-for="(t, i) in displayTracks" :key="t.id"
              class="group flex items-center gap-3 p-3 rounded-2xl cursor-pointer transition-all duration-200 border border-transparent hover:bg-[#0e0e26] hover:border-cyan-400/15"
              @click="playCloud(t)">
              <span class="w-7 text-center text-sm text-gray-600 group-hover:hidden font-mono">{{ String(i + 1).padStart(2, '0') }}</span>
              <span class="w-7 text-center hidden group-hover:block text-cyan-300">
                <svg class="w-4 h-4 mx-auto" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
              </span>
              <div class="w-12 h-12 rounded-lg bg-gradient-to-br from-cyan-500/30 to-purple-500/30 flex-shrink-0 overflow-hidden relative">
                <img v-if="t.cover" :src="t.cover" class="w-full h-full object-cover" alt="" />
                <span v-if="isCurrent(t)" class="absolute inset-0 flex items-center justify-center bg-black/50 text-cyan-300 text-lg">♪</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm text-white truncate flex items-center" :class="{ 'text-cyan-300': isCurrent(t) }">
                  <span class="truncate">{{ t.title }}</span>
                  <span v-if="isCurrent(t)" class="s-eq" :class="{ 's-eq-paused': !player.isPlaying }" aria-hidden="true"><i></i><i></i><i></i></span>
                </p>
                <p class="text-[11px] text-gray-500 mt-0.5 truncate">
                  {{ t.artist }}
                  <span v-if="t.category" class="text-gray-600"> · {{ t.category }}</span>
                </p>
              </div>
              <span class="text-[11px] text-gray-600 font-mono hidden sm:inline">{{ formatDuration(t.duration) }}</span>
            </div>
          </div>
          <div v-else class="py-12 text-center">
            <div class="text-4xl mb-3 opacity-15">🎵</div>
            <p class="text-xs text-gray-600">{{ cloudError ? '曲库加载失败' : '暂无歌曲' }}</p>
            <p v-if="source === 'netease' && serviceDown" class="text-[11px] text-amber-300/80 mt-1">
              网易云服务未启动（backend/netease-music-api，端口3000），可改用「全曲库 / 试听精选」免费源
            </p>
            <button v-if="cloudError" @click="loadCloud" class="s-mini-btn mt-3" :disabled="cloudLoading">↻ 重试</button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 音乐馆（媒体页内的音乐模块）
// 曲库来源可切换：全曲库(Audius 免费免 Key) / 试听精选(iTunes) / 网易云(需自建服务)
// 顶部搜索栏按当前来源搜索 → 结果一键全局播放；搜索历史本地持久化（最多 10 条）
// 下方双栏并排：我的音乐 / 云上曲库，均支持播放全部；卡片按流派/主题分区切换
// 正在播放的歌曲自动置顶并带 EQ 高亮；全局播放条由 App.vue 级 GlobalPlayer 承载
// ============================================================
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMusicList } from '@/api/media'
import { getNeteasePlaylist, getNeteaseSongUrl, searchNetease, isMusicServiceDown } from '@/api/music'
import { searchAudius, getAudiusTrending, AUDIUS_PRESET } from '@/api/audius'
import { ITUNES_PRESET, getItunesPlaylist, searchItunes } from '@/api/itunes'
import { usePlayerStore } from '@/stores/modules/player'
import { useSupabase } from '@/composables/useSupabase'
import { fixNeteaseId } from '@/config/musicFixes'

const router = useRouter()
const musicList = ref([])
const listLoading = ref(true)
const localError = ref(false)
const musicSource = ref('backend')

const player = usePlayerStore()

// ---- 曲库来源（默认 Audius：免费完整曲目，开箱即用、无需任何后端服务）----
const SOURCES = [
  { key: 'audius', icon: '🌐', label: '全曲库', panel: '云上全曲' },
  { key: 'itunes', icon: '✧', label: '试听精选', panel: '试听精选' },
  { key: 'netease', icon: '🎧', label: '网易云', panel: '网易云歌单' },
]
const SOURCE_HINT = {
  audius: '免费完整曲目 · 无需登录 · 点开即播',
  itunes: '官方 30 秒试听 · 免 Key 全球曲库',
  netease: '需本地启动 backend/netease-music-api（端口 3000）',
}
const source = ref('audius')
const activeSource = computed(() => SOURCES.find(s => s.key === source.value) || SOURCES[0])
const searchPlaceholder = computed(() => ({
  audius: '搜索完整曲目 / 音乐人，如：lofi / electronic',
  itunes: '搜索试听曲目，如：夜曲 / piano',
  netease: '搜索网易云歌曲 / 歌手 / 专辑，如：周杰伦',
}[source.value]))

// 云上曲库列表（三个来源共用一个列表 + 加载态）
const cloudTracks = ref([])
const cloudLoading = ref(false)
const cloudError = ref(false)
const cloudTitle = ref('')
const activeChipKey = ref('')
// 网易云服务可用性：后端 netease-music-api（端口 3000）未启动时置 true
const serviceDown = ref(false)

// 搜索
const keyword = ref('')
const searching = ref(false)
const searchDone = ref(false)
const searchResults = ref([])
const searchError = ref(false)
const searchServiceDown = ref(false)

// 搜索历史（localStorage 持久化，最多 10 条）
const HISTORY_KEY = 'music-search-history'
const searchHistory = ref(readHistory())

function readHistory() {
  try {
    const raw = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    return Array.isArray(raw) ? raw.filter(k => typeof k === 'string' && k.trim()).slice(0, 10) : []
  } catch { return [] }
}

// 记录搜索关键词（去重、最新在前、最多 10 条）
function saveHistory(kw) {
  const list = [kw, ...searchHistory.value.filter(k => k !== kw)].slice(0, 10)
  searchHistory.value = list
  try { localStorage.setItem(HISTORY_KEY, JSON.stringify(list)) } catch { /* 存储不可用时静默 */ }
}

// 清空搜索历史
function clearHistory() {
  searchHistory.value = []
  try { localStorage.removeItem(HISTORY_KEY) } catch { /* 静默 */ }
}

// 点击历史词直接搜索
function searchFromHistory(kw) {
  keyword.value = kw
  doSearch()
}

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
const displayTracks = computed(() => sortPlayingFirst(cloudTracks.value))

// 分区 chips：Audius 流派榜单 / iTunes 精选主题
const chips = computed(() => {
  if (source.value === 'audius') return AUDIUS_PRESET.map(p => ({ key: p.genre || 'hot', label: p.label, genre: p.genre }))
  if (source.value === 'itunes') return ITUNES_PRESET.map(p => ({ key: p.term, label: p.label, term: p.term }))
  return []
})

// 可播放的本地曲目（有音频地址，或可凭 neteaseId 在线取流）
const playableMyMusic = computed(() =>
  musicList.value.filter(t => t.url || /^\d+$/.test(String(t.neteaseId || ''))))

// ---- 播放模式（顺序 / 随机 / 单曲循环，与 store、GlobalPlayer 保持一致）----
const MODES = ['order', 'shuffle', 'loop-one']
const MODE_META = {
  order: { icon: '🔁', label: '列表循环' },
  shuffle: { icon: '🔀', label: '随机播放' },
  'loop-one': { icon: '🔂', label: '单曲循环' },
}
const modeIcon = computed(() => MODE_META[player.playMode]?.icon || '🔁')
const modeTitle = computed(() => MODE_META[player.playMode]?.label || '列表循环')

// 循环切换播放模式
function cycleMode() {
  const idx = MODES.indexOf(player.playMode)
  player.setMode(MODES[(idx + 1) % MODES.length])
}

// ---- 播放全部 ----
// 我的音乐：整体替换播放列表并从第一首开始
function playAllMyMusic() {
  const list = playableMyMusic.value
  if (!list.length) return
  player.setPlaylist(list)
  player.play({ ...list[0] })
}

// 云上曲库：只把有播放地址的曲目进列表（Audius / iTunes 已带地址）
function playAllCloud() {
  const list = cloudTracks.value.filter(t => t.url)
  if (!list.length) return
  player.setPlaylist(list)
  player.play(withNeteaseId(list[0]))
}

// 网易云曲目需把数字 id 作为 neteaseId 传给播放器，才能按需取流并加载歌词
function withNeteaseId(track) {
  return /^\d+$/.test(String(track.id)) ? { ...track, neteaseId: track.id } : { ...track }
}

// 切换曲库来源并重新加载列表
function switchSource(key) {
  if (source.value === key) return
  source.value = key
  searchDone.value = false
  searchResults.value = []
  activeChipKey.value = ''
  loadCloud()
}

// ---- 搜索 ----
async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw || searching.value) return
  searching.value = true
  searchDone.value = true
  searchError.value = false
  searchServiceDown.value = false
  try {
    let list = []
    if (source.value === 'audius') {
      list = await searchAudius(kw, 20)
    } else if (source.value === 'itunes') {
      list = await searchItunes(kw, 20)
    } else {
      const res = await searchNetease(kw, 20)
      list = (res?.result?.songs || []).map(s => ({
        id: s.id,
        title: s.name,
        artist: (s.artists || []).map(a => a.name).join(' / ') || '未知歌手',
        cover: s.album?.picUrl || '',
        duration: Math.round((s.duration || 0) / 1000),
        url: '',
      }))
      void preloadUrls(list)
    }
    searchResults.value = list
    if (list.length) saveHistory(kw)
    serviceDown.value = false
  } catch (e) {
    searchResults.value = []
    searchError.value = true
    if (source.value === 'netease' && isMusicServiceDown(e)) {
      searchServiceDown.value = true
      serviceDown.value = true
    }
  } finally {
    searching.value = false
  }
}

// 点击搜索结果：替换播放列表并播放该曲（网易云缺地址时按需取流）
async function playSearchResult(track) {
  if (!track.url) {
    try {
      const r = await getNeteaseSongUrl(track.id)
      track.url = r?.data?.[0]?.url || ''
    } catch { /* 静默 */ }
  }
  if (!track.url) return
  player.setPlaylist(searchResults.value.filter(t => t.url).map(withNeteaseId))
  player.play(withNeteaseId(track))
}

// 后台预取播放地址（并行，失败不影响列表）；同时把 neteaseId 落进曲目对象，
// 保证此后 GlobalPlayer 切上一首/下一首时也能正确加载歌词。
// 注意：只就地更新传入的列表，绝不能回写 cloudTracks 之外的来源（否则一次搜索就会清空曲库）
async function preloadUrls(tracks) {
  await Promise.all(tracks.map(async t => {
    try {
      const r = await getNeteaseSongUrl(t.id)
      t.url = r?.data?.[0]?.url || ''
    } catch { /* 静默 */ }
    t.neteaseId = t.neteaseId || t.id
  }))
}

// ---- 云上曲库：按当前来源加载默认分区 ----
async function loadCloud() {
  cloudLoading.value = true
  cloudError.value = false
  try {
    if (source.value === 'audius') {
      const p = AUDIUS_PRESET[0]
      activeChipKey.value = p.genre || 'hot'
      const list = await getAudiusTrending(30, p.genre)
      cloudTracks.value = list
      cloudTitle.value = `「${p.label}」共 ${list.length} 首`
    } else if (source.value === 'itunes') {
      const p = ITUNES_PRESET[0]
      activeChipKey.value = p.term
      const list = await getItunesPlaylist(p, 24)
      cloudTracks.value = list
      cloudTitle.value = `「${p.label}」共 ${list.length} 首`
    } else {
      await loadNetease()
    }
  } catch (e) {
    cloudTracks.value = []
    cloudTitle.value = ''
    cloudError.value = true
    if (source.value === 'netease' && isMusicServiceDown(e)) serviceDown.value = true
  } finally {
    cloudLoading.value = false
  }
}

// 切换分区 chips（Audius 流派榜单 / iTunes 精选主题）
async function loadChip(c) {
  if (cloudLoading.value) return
  activeChipKey.value = c.key
  cloudLoading.value = true
  cloudError.value = false
  try {
    const list = source.value === 'audius'
      ? await getAudiusTrending(30, c.genre)
      : await getItunesPlaylist({ term: c.term }, 24)
    cloudTracks.value = list
    cloudTitle.value = `「${c.label}」共 ${list.length} 首`
  } catch {
    cloudTracks.value = []
    cloudError.value = true
  } finally {
    cloudLoading.value = false
  }
}

// ---- 网易云歌单（需自建 backend/netease-music-api）----
async function loadNetease() {
  const res = await getNeteasePlaylist(NETEASE_PLAYLIST_ID, 60)
  const pl = res?.playlist
  if (!pl?.tracks?.length) { cloudTracks.value = []; cloudTitle.value = ''; cloudError.value = true; return }
  const tracks = pl.tracks.slice(0, 60).map(t => ({
    id: t.id,
    title: t.name,
    artist: (t.ar || []).map(a => a.name).join(' / ') || '未知歌手',
    cover: t.al?.picUrl || '',
    duration: Math.round((t.dt || 0) / 1000),
    url: '',
  }))
  cloudTracks.value = tracks
  cloudTitle.value = `「${pl.name}」共 ${tracks.length} 首`
  serviceDown.value = false
  void preloadUrls(tracks)
}

// 点击曲库歌曲：整体替换播放列表并播放该曲（网易云缺地址时按需取流）
async function playCloud(track) {
  if (!track.url) {
    try {
      const r = await getNeteaseSongUrl(track.id)
      track.url = r?.data?.[0]?.url || ''
    } catch { /* 静默 */ }
  }
  if (!track.url) return
  player.setPlaylist(cloudTracks.value.filter(t => t.url).map(withNeteaseId))
  player.play(withNeteaseId(track))
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
  // 有音频或可凭 neteaseId 在线解析的都进全局播放条（store 会按需取流），再进详情页同步展示
  if (item.url || /^\d+$/.test(String(item.neteaseId || ''))) {
    player.setPlaylist(musicList.value.filter(t => t.url || /^\d+$/.test(String(t.neteaseId || ''))))
    player.play(item)
  }
  router.push(`/music/player/${item.id}`)
}

const { fetchMusic, supabase } = useSupabase()

// 优先读取 Supabase 本地曲库（music_tracks 表），失败/为空再退回后端媒体接口
async function loadLocalMusic() {
  listLoading.value = true
  localError.value = false
  try {
    const { data, error } = await fetchMusic(100)
    if (!error && data?.length) {
      musicList.value = data.map((t, i) => ({
        ...t,
        id: t.id ?? i + 1,
        neteaseId: fixNeteaseId(t),
        duration: t.duration_seconds || t.duration || 0,
        category: parseCategory(t, i),
        url: t.audio_url || t.url || '',
        cover: t.cover_url || t.cover || '',
        artist: t.artist || '未知艺术家',
        album: t.album || '',
        source: 'supabase',
      }))
      musicSource.value = 'supabase'
      listLoading.value = false
      return
    }
  } catch { /* 走后端兜底 */ }

  getMusicList({ page: 1, size: 100 }).then(res => {
    const data = res.data?.records || res.data || []
    musicList.value = data.map((t, i) => ({
      ...t,
      id: t.id || t.musicId || i + 1,
      neteaseId: t.netease_id || t.neteaseId || '',
      duration: t.duration || 0,
      category: parseCategory(t, i),
      url: t.url || '',
    }))
  }).catch(() => { musicList.value = []; localError.value = true }).finally(() => { listLoading.value = false })
}

onMounted(() => {
  loadLocalMusic()
  // 默认加载免费全曲库（Audius），无需后端服务即可播放；
  // 网易云为可选来源，仅在用户切到该来源时才探测，避免首屏出现服务未启动告警
  loadCloud()
})
</script>

<style scoped>
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
  font-size: clamp(2.2rem, 5vw, 3rem);
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
/* 小型操作按钮：播放全部 / 重试 */
.s-mini-btn {
  flex-shrink: 0;
  border: 1px solid rgba(168, 181, 147, 0.4);
  color: var(--s-moss-bright);
  border-radius: 999px;
  font-size: 11px;
  padding: 4px 12px;
  transition: all 0.2s;
  cursor: pointer;
  background: transparent;
}
.s-mini-btn:hover:not(:disabled) {
  background: rgba(168, 181, 147, 0.15);
  border-color: rgba(201, 214, 184, 0.6);
  color: #fff;
}
.s-mini-btn:disabled { opacity: 0.4; cursor: not-allowed; }
/* 当前播放行的 EQ 律动条 */
.s-eq {
  display: inline-flex;
  align-items: flex-end;
  gap: 2px;
  height: 12px;
  margin-left: 7px;
  flex-shrink: 0;
}
.s-eq i {
  width: 2px;
  height: 4px;
  border-radius: 1px;
  background: var(--s-moss-bright);
  animation: s-eq-bounce 0.9s ease-in-out infinite;
}
.s-eq i:nth-child(2) { animation-delay: 0.25s; }
.s-eq i:nth-child(3) { animation-delay: 0.5s; }
.s-eq-paused i { animation-play-state: paused; height: 4px; opacity: 0.6; }
@keyframes s-eq-bounce {
  0%, 100% { height: 4px; }
  50% { height: 12px; }
}
.sylva-music :deep(.glass-panel) {
  background: rgba(16, 18, 30, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(12px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}
.sylva-music :deep(.group:hover) {
  border-color: rgba(168, 181, 147, 0.35) !important;
  background: rgba(168, 181, 147, 0.08) !important;
}
.sylva-music :deep(.text-cyan-300) { color: var(--s-moss-bright) !important; }
.sylva-music :deep(button) { color: inherit; }
.sylva-music :deep(input:focus) { border-color: rgba(168, 181, 147, 0.4) !important; }
</style>

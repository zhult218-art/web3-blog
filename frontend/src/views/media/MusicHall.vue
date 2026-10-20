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
          <p class="text-[13px] tracking-[0.08em] text-gray-300 mt-3" style="text-shadow: 0 1px 8px rgba(4,4,14,0.8)">搜索、聆听、驻足 · 点击任意曲目即可全局播放</p>
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

      <!-- 曲库来源切换：card-style tabs with icons, descriptions & glow -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <button v-for="s in SOURCES" :key="s.key" @click="switchSource(s.key)"
          class="source-card group relative text-left rounded-2xl p-4 transition-all duration-300"
          :class="source === s.key ? 'source-card--active' : 'source-card--idle'">
          <div class="flex items-start gap-3">
            <span class="text-2xl flex-shrink-0 mt-0.5 source-card__icon"
              :class="{ 'source-card__icon--active': source === s.key }">{{ s.icon }}</span>
            <div class="min-w-0">
              <p class="text-sm font-medium transition-colors"
                :class="source === s.key ? 'text-white' : 'text-gray-300 group-hover:text-white'">
                {{ s.label }}
              </p>
              <p class="text-[11px] mt-1 leading-relaxed transition-colors"
                :class="source === s.key ? 'text-gray-400' : 'text-gray-600 group-hover:text-gray-500'">
                {{ SOURCE_DESC[s.key] }}
              </p>
            </div>
          </div>
          <!-- Active indicator bar -->
          <span v-if="source === s.key" class="absolute bottom-0 left-4 right-4 h-[2px] rounded-full bg-gradient-to-r from-transparent via-[var(--s-moss)] to-transparent"></span>
        </button>
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

        <!-- 搜索历史 with gradient-border chips -->
        <div v-if="!searchDone && searchHistory.length" class="mt-3 flex flex-wrap items-center gap-2">
          <span class="text-[11px] text-gray-600">最近搜索</span>
          <button v-for="kw in searchHistory" :key="kw" @click="searchFromHistory(kw)"
            class="history-chip text-[11px] px-3 py-1 rounded-full text-gray-400 transition">
            {{ kw }}
          </button>
          <button @click="clearHistory" class="text-[11px] text-gray-600 hover:text-red-300 transition ml-1" title="清空搜索历史">清空</button>
        </div>

        <!-- Loading skeleton while searching -->
        <div v-if="searching" class="mt-4 space-y-2">
          <div v-for="n in 5" :key="n" class="flex items-center gap-3 p-2.5 rounded-xl">
            <div class="w-6 h-4 rounded skeleton-shimmer"></div>
            <div class="w-10 h-10 rounded-lg skeleton-shimmer flex-shrink-0"></div>
            <div class="flex-1 space-y-1.5">
              <div class="h-3.5 w-3/5 rounded skeleton-shimmer"></div>
              <div class="h-2.5 w-2/5 rounded skeleton-shimmer"></div>
            </div>
            <div class="h-3 w-10 rounded skeleton-shimmer hidden sm:block"></div>
          </div>
        </div>

        <!-- 搜索结果 with fade transition -->
        <Transition name="s-fade" mode="out-in">
          <div v-if="searchDone && !searching" key="results" class="mt-4">
            <p class="text-[11px] text-gray-500 mb-2">
              {{ searchError
                ? (searchServiceDown ? '搜索失败：音乐搜索服务未启动，请稍后重试' : '搜索失败，请稍后重试')
                : (searchResults.length ? `在「${activeSource.label}」找到 ${searchResults.length} 首，点击播放` : '没有找到相关音乐，换个关键词试试') }}
            </p>
            <div v-if="searchResults.length" class="max-h-72 overflow-y-auto space-y-1 music-scroll">
              <div v-for="(t, i) in searchResults" :key="t.id"
                class="group flex items-center gap-3 p-2.5 rounded-xl cursor-pointer transition-all duration-200 border border-transparent hover:bg-[#0e0e26] hover:border-cyan-400/15"
                @click="playSearchResult(t)">
                <span class="w-6 text-center text-xs text-gray-600 group-hover:hidden font-mono">{{ i + 1 }}</span>
                <span class="w-6 text-center hidden group-hover:block text-cyan-300 text-xs">▶</span>
                <!-- Cover with hover play overlay -->
                <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-cyan-500/30 to-purple-500/30 flex-shrink-0 overflow-hidden relative group/cover">
                  <img v-if="t.cover" :src="t.cover" class="w-full h-full object-cover transition-transform duration-300 group-hover/cover:scale-110" alt="" />
                  <div class="play-overlay">
                    <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                  </div>
                  <!-- Source badge -->
                  <span class="source-badge source-badge--corner">{{ activeSource.icon }}</span>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm text-white truncate flex items-center gap-2" :class="{ 'text-cyan-300': player.currentTrack?.id === t.id }">
                    <span class="truncate">{{ t.title }}</span>
                    <span class="source-badge source-badge--inline hidden sm:inline-flex">{{ activeSource.icon }} {{ activeSource.label }}</span>
                  </p>
                  <p class="text-[11px] text-gray-500 truncate">{{ t.artist }}</p>
                </div>
                <span class="text-[11px] text-gray-600 font-mono hidden sm:inline">{{ formatDuration(t.duration) }}</span>
              </div>
            </div>
          </div>
        </Transition>
      </div>

      <!-- 云上曲库（全宽，默认八九十年代金曲） -->
      <section>
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
          <!-- 分区 chips -->
          <div v-if="chips.length" class="flex flex-wrap gap-2 mb-3">
            <button v-for="c in chips" :key="c.key" @click="loadChip(c)" :disabled="cloudLoading"
              class="text-[11px] px-2.5 py-1 rounded-full border transition disabled:opacity-50"
              :class="activeChipKey === c.key
                ? 'border-[rgba(168,181,147,0.5)] text-white bg-[rgba(168,181,147,0.12)]'
                : 'border-white/10 text-gray-400 hover:text-white hover:border-[rgba(168,181,147,0.4)]'">
              {{ c.label }}
            </button>
          </div>

          <!-- Loading skeleton for cloud tracks -->
          <div v-if="cloudLoading" class="space-y-1">
            <div v-for="n in 8" :key="n"
              class="flex items-center gap-3 p-3 rounded-2xl"
              :class="n % 2 === 0 ? 'bg-white/[0.015]' : ''">
              <div class="w-7 h-4 rounded skeleton-shimmer"></div>
              <div class="w-12 h-12 rounded-lg skeleton-shimmer flex-shrink-0"></div>
              <div class="flex-1 space-y-2">
                <div class="h-4 w-2/5 rounded skeleton-shimmer"></div>
                <div class="h-3 w-1/3 rounded skeleton-shimmer"></div>
              </div>
              <div class="h-3 w-10 rounded skeleton-shimmer hidden sm:block"></div>
            </div>
          </div>

          <!-- Cloud tracks grid with alternating stripes & transitions -->
          <Transition name="s-fade" mode="out-in">
            <div v-if="!cloudLoading && cloudTracks.length" key="tracks"
              class="grid grid-cols-1 md:grid-cols-2 gap-1 max-h-[640px] overflow-y-auto pr-1 music-scroll">
              <div v-for="(t, i) in displayTracks" :key="t.id"
                class="track-row group flex items-center gap-3 p-3 rounded-2xl cursor-pointer transition-all duration-200 border border-transparent hover:bg-[#0e0e26] hover:border-cyan-400/15"
                :class="{ 'track-row--alt': i % 2 === 1, 'track-row--playing': isCurrent(t) }"
                @click="playCloud(t)">
                <span class="w-7 text-center text-sm text-gray-600 group-hover:hidden font-mono">{{ String(i + 1).padStart(2, '0') }}</span>
                <span class="w-7 text-center hidden group-hover:block text-cyan-300">
                  <svg class="w-4 h-4 mx-auto" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                </span>
                <!-- Cover with hover play overlay -->
                <div class="w-12 h-12 rounded-lg bg-gradient-to-br from-cyan-500/30 to-purple-500/30 flex-shrink-0 overflow-hidden relative group/cover">
                  <img v-if="t.cover" :src="t.cover" class="w-full h-full object-cover transition-transform duration-300 group-hover/cover:scale-110" alt="" />
                  <div class="play-overlay play-overlay--lg">
                    <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                  </div>
                  <span v-if="isCurrent(t)" class="absolute inset-0 flex items-center justify-center bg-black/50 rounded-lg">
                    <span class="s-eq" :class="{ 's-eq-paused': !player.isPlaying }" aria-hidden="true"><i></i><i></i><i></i></span>
                  </span>
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
                <!-- More action on hover -->
                <button class="track-more opacity-0 group-hover:opacity-100 transition-all duration-200 text-gray-500 hover:text-[var(--s-moss-bright)] flex-shrink-0"
                  @click.stop
                  :title="'加入播放'">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" d="M12 5v14M5 12h14"/>
                  </svg>
                </button>
              </div>
            </div>
          </Transition>

          <!-- Improved empty state -->
          <div v-if="!cloudLoading && !cloudTracks.length" class="py-16 text-center s-empty-state">
            <div class="empty-art">
              <span class="empty-art__ring"></span>
              <span class="empty-art__note empty-art__note--1">🎵</span>
              <span class="empty-art__note empty-art__note--2">🎶</span>
              <span class="empty-art__note empty-art__note--3">♪</span>
              <span class="empty-art__disc">🎧</span>
            </div>
            <p class="text-sm text-gray-500 mt-5 font-medium">
              {{ cloudError ? '曲库加载失败' : '暂无歌曲' }}
            </p>
            <p v-if="!cloudError" class="text-[11px] text-gray-600 mt-1.5 max-w-xs mx-auto leading-relaxed">
              试试切换上方的曲库来源，或点击分区标签探索不同风格
            </p>
            <p v-if="source === 'netease' && serviceDown" class="text-[11px] text-amber-300/80 mt-2">
              音乐搜索服务未启动，可改用「试听精选 / 全曲库」等可用源
            </p>
            <div v-if="cloudError" class="flex items-center justify-center gap-2 mt-4">
              <button @click="loadCloud" class="s-mini-btn" :disabled="cloudLoading">↻ 重试</button>
              <button v-if="source === 'netease'" @click="switchSource('itunes')" class="s-mini-btn">切换到试听精选</button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 音乐馆（媒体页内的音乐模块）
// 曲库来源可切换：试听精选(iTunes 八九十年代金曲 默认) / 全曲库(Audius) / 网易云(需自建服务)
// 顶部搜索栏按当前来源搜索 → 结果一键全局播放；搜索历史本地持久化（最多 10 条）
// 下方全宽云上曲库，支持播放全部；卡片按流派/主题分区切换
// 正在播放的歌曲自动置顶并带 EQ 高亮；全局播放条由 App.vue 级 GlobalPlayer 承载
// ============================================================
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNeteaseSongUrl, searchNetease, isMusicServiceDown } from '@/api/music'
import { searchAudius, getAudiusTrending, AUDIUS_PRESET } from '@/api/audius'
import { ITUNES_PRESET, getItunesPlaylist, searchItunes } from '@/api/itunes'
import { usePlayerStore } from '@/stores/modules/player'

const router = useRouter()
const player = usePlayerStore()

// ---- 曲库来源（默认网易云：完整播放+歌词，八九十年代金曲搜索预设）----
const SOURCES = [
  { key: 'netease', icon: '🎧', label: '八九十年代', panel: '八九十年代金曲' },
  { key: 'itunes', icon: '✧', label: '试听精选', panel: '试听精选' },
  { key: 'audius', icon: '🌐', label: '全曲库', panel: '云上全曲' },
]
const SOURCE_HINT = {
  netease: '八九十年代金曲 · 完整播放 · 含歌词',
  itunes: '官方 30 秒试听 · 免 Key 全球曲库',
  audius: '免费完整曲目 · 无需登录 · 点开即播',
}
const SOURCE_DESC = {
  netease: '八九十年代金曲，完整播放含歌词',
  itunes: '官方 30 秒试听，全球曲库免 Key',
  audius: '免费完整曲目，无需登录点开即播',
}
const source = ref('netease')
const activeSource = computed(() => SOURCES.find(s => s.key === source.value) || SOURCES[0])
const searchPlaceholder = computed(() => ({
  netease: '搜索八九十年代金曲 / 歌手，如：邓丽君 / beyond / 80年代',
  itunes: '搜索试听曲目，如：夜曲 / piano',
  audius: '搜索完整曲目 / 音乐人，如：lofi / electronic',
}[source.value]))

// 网易云八九十年代搜索预设歌单（按关键词搜索，完整播放+歌词）
const NETEASE_PRESET = [
  { label: '80年代经典', term: '80年代经典' },
  { label: '90年代金曲', term: '90年代金曲' },
  { label: '邓丽君', term: '邓丽君' },
  { label: '张国荣', term: '张国荣' },
  { label: 'Beyond', term: 'beyond' },
  { label: '谭咏麟', term: '谭咏麟' },
  { label: '罗大佑', term: '罗大佑' },
  { label: '李宗盛', term: '李宗盛' },
  { label: '凤飞飞', term: '凤飞飞' },
  { label: '蔡琴', term: '蔡琴' },
  { label: '刘文正', term: '刘文正' },
  { label: '80英文金曲', term: '80s english pop' },
]

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

const displayTracks = computed(() => sortPlayingFirst(cloudTracks.value))

// 分区 chips：网易云八九十年代搜索 / iTunes 精选 / Audius 流派
const chips = computed(() => {
  if (source.value === 'netease') return NETEASE_PRESET.map(p => ({ key: p.term, label: p.label, term: p.term }))
  if (source.value === 'itunes') return ITUNES_PRESET.map(p => ({ key: p.term, label: p.label, term: p.term }))
  if (source.value === 'audius') return AUDIUS_PRESET.map(p => ({ key: p.genre || 'hot', label: p.label, genre: p.genre }))
  return []
})

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

// 云上曲库：只把有播放地址的曲目进列表（iTunes / Audius 已带地址）
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
      // 网易云：默认加载第一个八九十年代预设搜索
      const p = NETEASE_PRESET[0]
      activeChipKey.value = p.term
      await loadNeteaseSearch(p.term, p.label)
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

// 切换分区 chips
async function loadChip(c) {
  if (cloudLoading.value) return
  activeChipKey.value = c.key
  cloudLoading.value = true
  cloudError.value = false
  try {
    let list = []
    if (source.value === 'audius') {
      list = await getAudiusTrending(30, c.genre)
    } else if (source.value === 'itunes') {
      list = await getItunesPlaylist({ term: c.term }, 24)
    } else {
      // 网易云按关键词搜索
      list = await loadNeteaseSearch(c.term, c.label)
      return
    }
    cloudTracks.value = list
    cloudTitle.value = `「${c.label}」共 ${list.length} 首`
  } catch {
    cloudTracks.value = []
    cloudError.value = true
  } finally {
    cloudLoading.value = false
  }
}

// 网易云按关键词搜索（完整播放+歌词）
async function loadNeteaseSearch(term, label) {
  const res = await searchNetease(term, 30)
  const songs = res?.result?.songs || []
  const tracks = songs.map(s => ({
    id: s.id,
    title: s.name,
    artist: (s.artists || []).map(a => a.name).join(' / ') || '未知歌手',
    cover: s.album?.picUrl || '',
    duration: Math.round((s.duration || 0) / 1000),
    url: '',
  }))
  cloudTracks.value = tracks
  cloudTitle.value = `「${label}」共 ${tracks.length} 首`
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

function formatDuration(secs) {
  if (!secs) return '--:--'
  const n = Number(secs)
  if (!isFinite(n)) return String(secs)
  const m = Math.floor(n / 60)
  const s = Math.floor(n % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

onMounted(() => {
  // 默认加载八九十年代金曲（iTunes 免费源），无需后端服务即可播放
  loadCloud()
})
</script>

<style scoped>
/* ================================================================
   Sylva Music Hall — editorial design system
   ================================================================ */

.sylva-music {
  --s-moss: #a8b593;
  --s-moss-bright: #c9d6b8;
  --s-moss-glow: rgba(168, 181, 147, 0.25);
  --s-rule: rgba(255, 255, 255, 0.07);
}

/* ── Scrollbar ─────────────────────────────────────────────────── */
.music-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgba(159, 176, 140, 0.35) transparent;
}
.music-scroll::-webkit-scrollbar { width: 5px; }
.music-scroll::-webkit-scrollbar-thumb {
  background: rgba(159, 176, 140, 0.35);
  border-radius: 999px;
}
.music-scroll::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 999px;
}

/* ── Editorial typography ──────────────────────────────────────── */
.s-yebrow {
  font-size: 11px;
  letter-spacing: 0.4em;
  text-transform: uppercase;
  color: var(--s-moss);
  margin-bottom: 10px;
  /* 页头直接铺在壁纸上，细字重的小字容易被画面吃掉 */
  text-shadow: 0 1px 8px rgba(4, 4, 14, 0.8);
}
.s-title {
  font-family: 'Lexend', 'Segoe UI', system-ui, sans-serif;
  font-size: clamp(2.2rem, 5vw, 3rem);
  font-weight: 300;
  letter-spacing: -0.015em;
  line-height: 1;
  color: #fff;
  text-shadow: 0 2px 16px rgba(4, 4, 14, 0.8);
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

/* ── Source card tabs ──────────────────────────────────────────── */
.source-card {
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(16, 18, 30, 0.6);
  backdrop-filter: blur(8px);
}
.source-card--idle:hover {
  border-color: rgba(168, 181, 147, 0.3);
  background: rgba(168, 181, 147, 0.06);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}
.source-card--active {
  border-color: rgba(168, 181, 147, 0.5) !important;
  background: rgba(168, 181, 147, 0.1) !important;
  box-shadow:
    0 0 24px rgba(168, 181, 147, 0.12),
    0 4px 20px rgba(0, 0, 0, 0.25),
    inset 0 1px 0 rgba(168, 181, 147, 0.15);
}
.source-card__icon {
  transition: transform 0.3s, filter 0.3s;
}
.source-card__icon--active {
  filter: drop-shadow(0 0 8px rgba(168, 181, 147, 0.5));
  transform: scale(1.1);
}
.source-card--idle:hover .source-card__icon {
  transform: scale(1.05);
}

/* ── Search button ─────────────────────────────────────────────── */
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

/* ── Mini action buttons ───────────────────────────────────────── */
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

/* ── History chips with gradient borders ───────────────────────── */
.history-chip {
  position: relative;
  background: rgba(16, 18, 30, 0.9);
  border: none;
  z-index: 1;
}
.history-chip::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 999px;
  padding: 1px;
  background: linear-gradient(
    135deg,
    rgba(168, 181, 147, 0.45),
    rgba(120, 140, 160, 0.2),
    rgba(168, 181, 147, 0.3)
  );
  -webkit-mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  transition: opacity 0.25s;
  opacity: 0.6;
}
.history-chip:hover::before {
  opacity: 1;
  background: linear-gradient(
    135deg,
    rgba(201, 214, 184, 0.7),
    rgba(168, 181, 147, 0.4),
    rgba(201, 214, 184, 0.6)
  );
}
.history-chip:hover {
  color: #fff !important;
}

/* ── Skeleton loading shimmer ──────────────────────────────────── */
.skeleton-shimmer {
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.04) 0%,
    rgba(255, 255, 255, 0.08) 40%,
    rgba(255, 255, 255, 0.04) 80%
  );
  background-size: 200% 100%;
  animation: skeleton-slide 1.6s ease-in-out infinite;
}
@keyframes skeleton-slide {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── Play overlay on covers ────────────────────────────────────── */
.play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
  color: var(--s-moss-bright);
  opacity: 0;
  transition: opacity 0.25s ease;
  border-radius: inherit;
}
.play-overlay--lg {
  background: rgba(0, 0, 0, 0.5);
}
.group\/cover:hover .play-overlay {
  opacity: 1;
}

/* ── Source badges ─────────────────────────────────────────────── */
.source-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 9px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(168, 181, 147, 0.12);
  color: var(--s-moss);
  border: 1px solid rgba(168, 181, 147, 0.2);
  white-space: nowrap;
  line-height: 1.4;
}
.source-badge--corner {
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 8px;
  padding: 0px 4px;
  background: rgba(0, 0, 0, 0.6);
  border: none;
  backdrop-filter: blur(4px);
}
.source-badge--inline {
  flex-shrink: 0;
}

/* ── Track rows with alternating stripes ───────────────────────── */
.track-row {
  position: relative;
}
.track-row--alt {
  background: rgba(255, 255, 255, 0.015);
}
.track-row--playing {
  background: rgba(168, 181, 147, 0.06) !important;
  border-color: rgba(168, 181, 147, 0.2) !important;
}
.track-row--playing.track-row--alt {
  background: rgba(168, 181, 147, 0.08) !important;
}

/* ── Track "more" action ───────────────────────────────────────── */
.track-more {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s;
}
.track-more:hover {
  background: rgba(168, 181, 147, 0.12);
}

/* ── EQ visualiser bars ────────────────────────────────────────── */
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

/* ── Empty state illustration ──────────────────────────────────── */
.s-empty-state {
  animation: s-fade-in 0.5s ease;
}
.empty-art {
  position: relative;
  width: 100px;
  height: 100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-art__ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px dashed rgba(168, 181, 147, 0.2);
  animation: empty-spin 20s linear infinite;
}
.empty-art__disc {
  font-size: 40px;
  opacity: 0.35;
  filter: grayscale(0.3);
}
.empty-art__note {
  position: absolute;
  font-size: 18px;
  opacity: 0;
  animation: empty-float 3.5s ease-in-out infinite;
}
.empty-art__note--1 {
  top: 2px;
  right: 8px;
  animation-delay: 0s;
}
.empty-art__note--2 {
  bottom: 6px;
  left: 4px;
  animation-delay: 1.2s;
  font-size: 14px;
}
.empty-art__note--3 {
  top: 18px;
  left: 0px;
  animation-delay: 2.4s;
  font-size: 12px;
}
@keyframes empty-spin {
  to { transform: rotate(360deg); }
}
@keyframes empty-float {
  0%, 100% {
    opacity: 0;
    transform: translateY(6px) scale(0.8);
  }
  25% {
    opacity: 0.55;
    transform: translateY(-6px) scale(1);
  }
  75% {
    opacity: 0.35;
    transform: translateY(-14px) scale(0.9);
  }
}

/* ── Transitions ───────────────────────────────────────────────── */
.s-fade-enter-active,
.s-fade-leave-active {
  transition: opacity 0.3s ease;
}
.s-fade-enter-from,
.s-fade-leave-to {
  opacity: 0;
}
@keyframes s-fade-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ── Glass panel & deep overrides ──────────────────────────────── */
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

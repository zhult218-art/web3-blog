// ============================================================
// 全局音乐播放器 Store（Pinia）
// 单例 Audio 播放核心：播放 / 暂停 / 切歌 / 音量 / 进度 / 播放模式
// 被音乐页、GlobalPlayer 悬浮播放器与歌词组件共用
// ============================================================
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getNeteaseSongUrl, getNeteaseLyric, recordPlay } from '@/api/music'
import { formatDuration as formatTime } from '@/utils/date.js'

// localStorage 兜底历史 key（未登录/弱网仍可访问最近播放）
const LOCAL_HISTORY_KEY = 'music_history'
const LOCAL_HISTORY_MAX = 100

// 读取本地历史（[{ id, title, artist, cover, neteaseId, url, duration, playedAt }]）
function readLocalHistory() {
  try {
    const raw = localStorage.getItem(LOCAL_HISTORY_KEY)
    return raw ? JSON.parse(raw) : []
  } catch { return [] }
}

// 写入本地历史（新歌 unshift 到顶部，按 id 去重，最多 100 条）
function writeLocalHistory(track) {
  if (!track?.id) return
  try {
    const list = readLocalHistory().filter(t => String(t.id) !== String(track.id))
    list.unshift({
      id: track.id,
      title: track.title,
      artist: track.artist,
      cover: track.cover,
      neteaseId: track.neteaseId,
      url: track.url,
      duration: track.duration,
      playedAt: Date.now(),
    })
    localStorage.setItem(LOCAL_HISTORY_KEY, JSON.stringify(list.slice(0, LOCAL_HISTORY_MAX)))
  } catch { /* localStorage 不可用时静默 */ }
}

// 公开：读本地历史（前端历史面板兜底展示）
export function getLocalHistory() { return readLocalHistory() }

// 公开：清空本地历史
export function clearLocalHistory() {
  try { localStorage.removeItem(LOCAL_HISTORY_KEY) } catch { /* ignore */ }
}

// 同步 navigator.mediaSession.metadata（锁屏/通知栏控制）
function updateMediaSession(track) {
  if (typeof navigator === 'undefined' || !('mediaSession' in navigator)) return
  try {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: track?.title || '未知曲目',
      artist: track?.artist || '未知艺术家',
      album: track?.album || '星屿电台',
      artwork: track?.cover
        ? [{ src: track.cover, sizes: '512x512', type: 'image/jpeg' }]
        : [],
    })
  } catch { /* MediaMetadata 不可用时静默 */ }
}

// 同步 mediaSession.playbackState（'playing' / 'paused'）：通知栏控制图标显隐
function updatePlaybackState(state) {
  if (typeof navigator === 'undefined' || !('mediaSession' in navigator)) return
  try { navigator.mediaSession.playbackState = state } catch { /* ignore */ }
}

// 注册 mediaSession 控制按钮（play/pause/previoustrack/nexttrack/seekto）
// 传入回调函数（store 实例方法）— 仅注册一次
let mediaSessionBound = false
function bindMediaSessionHandlers({ onPlay, onPause, onPrev, onNext, onSeekTo }) {
  if (mediaSessionBound) return
  if (typeof navigator === 'undefined' || !('mediaSession' in navigator)) return
  const ms = navigator.mediaSession
  try { ms.setActionHandler('play', () => onPlay?.()) } catch { /* not supported */ }
  try { ms.setActionHandler('pause', () => onPause?.()) } catch { /* not supported */ }
  try { ms.setActionHandler('previoustrack', () => onPrev?.()) } catch { /* not supported */ }
  try { ms.setActionHandler('nexttrack', () => onNext?.()) } catch { /* not supported */ }
  try {
    ms.setActionHandler('seekto', (details) => {
      if (details?.seekTime != null) onSeekTo?.(details.seekTime)
    })
  } catch { /* not supported */ }
  mediaSessionBound = true
}

// 将网易云 LRC 文本解析为 [{ time, text }]
export function parseLrc(lrcText) {
  const lines = []
  if (!lrcText) return lines
  const re = /\[(\d+):(\d+)(?:[.:](\d+))?\]/g
  lrcText.split(/\r?\n/).forEach(line => {
    re.lastIndex = 0
    let match
    const hits = []
    while ((match = re.exec(line)) !== null) {
      const m = parseInt(match[1], 10)
      const s = parseInt(match[2], 10)
      const ms = match[3] ? parseInt(String(match[3]).slice(0, 2).padEnd(2, '0'), 10) : 0
      hits.push(m * 60 + s + ms / 100)
    }
    const text = line.replace(re, '').trim()
    if (!text) return
    hits.forEach(t => lines.push({ time: t, text }))
  })
  lines.sort((a, b) => a.time - b.time)
  return lines
}

// 根据当前时间定位歌词行下标
export function findLyricIndex(lyrics, time) {
  if (!Array.isArray(lyrics) || lyrics.length === 0) return -1
  let idx = -1
  for (let i = 0; i < lyrics.length; i++) {
    if ((lyrics[i].time || 0) <= time) idx = i
    else break
  }
  return idx
}

export const usePlayerStore = defineStore('player', () => {
  const currentTrack = ref(null)
  const isPlaying = ref(false)
  const volume = ref(0.7)
  const playlist = ref([])
  const playlistVisible = ref(false)
  const playerBarVisible = ref(true) // 底部浮窗显隐（可手动/语音开关）
  const currentTime = ref(0)
  const duration = ref(0)
  const playMode = ref('order') // 播放模式: order 顺序 | shuffle 随机 | loop-one 单曲循环
  const lyrics = ref([])           // [{ time, text }]
  const lyricsLoading = ref(false)

  let audio = null
  let trackDuration = 0   // known duration from track object (fallback for Infinity)
  let playSeq = 0         // race-condition guard for play() URL fetches
  let playRetryCount = 0  // retry counter for current track playback
  let autoSkipUsed = false // prevent infinite auto-skip loop on repeated errors

  // 懒创建全局唯一的 Audio 元素并绑定进度 / 结束 / 错误事件
  function ensureAudio() {
    if (audio) return audio
    audio = new Audio()
    audio.preload = 'metadata'
    audio.volume = volume.value
    audio.addEventListener('timeupdate', () => {
      currentTime.value = audio.currentTime
    })
    audio.addEventListener('loadedmetadata', () => {
      // Netease proxy streams can return Infinity for duration — fall back to track metadata
      duration.value = (audio.duration && isFinite(audio.duration)) ? audio.duration : (trackDuration || 0)
    })
    audio.addEventListener('durationchange', () => {
      duration.value = (audio.duration && isFinite(audio.duration)) ? audio.duration : (trackDuration || 0)
    })
    audio.addEventListener('ended', handleEnded)
    audio.addEventListener('error', () => {
      const failedTrack = currentTrack.value
      isPlaying.value = false
      updatePlaybackState('paused')

      // Retry 1: re-fetch URL for Netease tracks (stale/expired URL)
      if (playRetryCount < 1 && failedTrack && /^\d+$/.test(String(failedTrack.neteaseId || ''))) {
        playRetryCount++
        getNeteaseSongUrl(failedTrack.neteaseId).then(r => {
          const newUrl = r?.data?.[0]?.url
          if (newUrl && currentTrack.value?.id === failedTrack.id) {
            audio.crossOrigin = 'anonymous'
            audio.src = newUrl
            currentTrack.value = { ...failedTrack, url: newUrl }
            audio.play().catch(() => {
              isPlaying.value = false
              updatePlaybackState('paused')
            })
          }
        }).catch(() => { /* re-fetch failed, fall through */ })
        return
      }

      // Retry 2: try without crossOrigin (CORS misconfiguration on backend proxy)
      if (playRetryCount < 2 && audio.crossOrigin === 'anonymous') {
        playRetryCount++
        audio.crossOrigin = null
        audio.play().catch(() => {
          isPlaying.value = false
          updatePlaybackState('paused')
        })
        return
      }

      // All retries exhausted — auto-skip to next track
      if (!autoSkipUsed) {
        autoSkipUsed = true
        errorOnce('播放失败，正在切换...')
        setTimeout(() => {
          const list = playlist.value
          if (list.length > 1 && currentTrack.value) {
            const idx = list.findIndex(t => t.id === currentTrack.value?.id)
            const nextTrack = list[idx + 1] || list[0]
            if (nextTrack && nextTrack.id !== currentTrack.value?.id) {
              play(nextTrack)
            }
          }
        }, 1000)
      }
    })
    return audio
  }

  // 是否已有播放中的曲目
  const hasTrack = computed(() => !!currentTrack.value)

  // 随机取下一首（避免与当前曲目重复）
  function randomNext(list, currentId) {
    if (list.length <= 1) return list[0]
    let idx = Math.floor(Math.random() * list.length)
    const cur = list.findIndex(t => t.id === currentId)
    while (idx === cur) idx = Math.floor(Math.random() * list.length)
    return list[idx]
  }

  // 播放结束回调：按播放模式决定下一首
  function handleEnded() {
    const list = playlist.value
    if (!list.length || !currentTrack.value) return
    if (playMode.value === 'loop-one') {
      play(currentTrack.value)
      return
    }
    const cur = currentTrack.value
    if (playMode.value === 'shuffle') {
      const next = randomNext(list, cur.id)
      if (next && next.id !== cur.id) play(next)
      else play(cur)
      return
    }
    // order: 顺序播放，播完自动回到第一首
    const idx = list.findIndex(t => t.id === cur.id)
    const next = list[idx + 1] || list[0]
    if (next && next.id !== cur.id) play(next)
  }

  // 播放指定曲目（本地曲库仅有 neteaseId、缺播放地址时按需取网易云流）
  async function play(track) {
    if (!track) return
    const seq = ++playSeq
    let target = track
    if (!target.url && /^\d+$/.test(String(target.neteaseId || ''))) {
      try {
        const r = await getNeteaseSongUrl(target.neteaseId)
        if (seq !== playSeq) return // stale — a newer play() already took over
        if (r?.data?.[0]?.url) target = { ...target, url: r.data[0].url }
      } catch { /* 取流失败则按原样处理 */ }
    }
    const el = ensureAudio()
    if (!target.url) {
      errorOnce('该曲目缺少播放地址')
      return
    }
    // 始终声明 crossOrigin = anonymous，保证 Web Audio 分析器不会静音；
    // 若实际播放因 CORS 失败，error handler 会去掉 crossOrigin 重试一次
    el.crossOrigin = 'anonymous'
    // Reset per-track state
    playRetryCount = 0
    autoSkipUsed = false
    trackDuration = target.duration || 0
    currentTrack.value = target
    el.src = target.url
    currentTime.value = 0
    duration.value = target.duration || 0
    lyrics.value = []
    loadLyrics(target)
    updateMediaSession(target)
    el.play().then(() => {
      isPlaying.value = true
      updatePlaybackState('playing')
      // 埋点：本地历史（兜底，未登录也能用）+ 后端 play_count +1（失败静默，不影响播放）
      writeLocalHistory(target)
      const idStr = String(target.id || '')
      if (/^\d+$/.test(idStr)) {
        recordPlay(target.id).catch(() => { /* 后端不可用时不影响播放 */ })
      }
    }).catch(() => {
      isPlaying.value = false
      updatePlaybackState('paused')
      errorOnce('播放失败，正在切换...')
      // Auto-skip to next track after short delay
      if (!autoSkipUsed) {
        autoSkipUsed = true
        setTimeout(() => {
          const list = playlist.value
          if (list.length > 1 && currentTrack.value) {
            const idx = list.findIndex(t => t.id === currentTrack.value?.id)
            const nextTrack = list[idx + 1] || list[0]
            if (nextTrack && nextTrack.id !== currentTrack.value?.id) {
              play(nextTrack)
            }
          }
        }, 1000)
      }
    })
  }

  // 加载当前曲目的歌词：优先网易云 lyric 接口，失败/无 id 时清空
  let lyricSeq = 0
  async function loadLyrics(track) {
    const id = track?.neteaseId || track?.id
    const realNeteaseId = track?.neteaseId ? String(track.neteaseId) : null
    if (!realNeteaseId || !/^\d+$/.test(realNeteaseId)) {
      lyrics.value = []
      return
    }
    const seq = ++lyricSeq
    lyricsLoading.value = true
    try {
      const res = await getNeteaseLyric(realNeteaseId)
      // 网关直出网易原始结构 {lrc:{lyric}}；也兼容被信封包裹的 {data:{lrc:{lyric}}}
      const body = (res?.lrc || res?.lyric !== undefined) ? res : (res?.data || {})
      const lrc = body?.lrc?.lyric || body?.lyric || ''
      if (seq !== lyricSeq) return
      lyrics.value = lrc ? parseLrc(lrc) : []
    } catch {
      if (seq === lyricSeq) lyrics.value = []
    } finally {
      if (seq === lyricSeq) lyricsLoading.value = false
    }
  }

  // 当前高亮歌词行下标
  const currentLyricIndex = computed(() => findLyricIndex(lyrics.value, currentTime.value))

  // 当前歌词行（便于组件直接读取）
  const currentLyricLine = computed(() => {
    const i = currentLyricIndex.value
    return i >= 0 ? lyrics.value[i] : null
  })

  let lastErrorToast = ''
  // 同一错误提示 3 秒内只弹一次（避免循环触发刷屏）；经全局 Toast 展示
  function errorOnce(msg) {
    if (lastErrorToast === msg) return
    lastErrorToast = msg
    setTimeout(() => { lastErrorToast = '' }, 3000)
    import('@/stores/modules/toast').then(({ useToastStore }) => {
      useToastStore().error(msg)
    }).catch(() => { /* Toast 不可用时静默 */ })
  }

  // 暂停播放
  function pause() {
    isPlaying.value = false
    updatePlaybackState('paused')
    ensureAudio().pause()
  }

  // 继续播放
  function resume() {
    const el = ensureAudio()
    if (!currentTrack.value) return
    el.play().then(() => { isPlaying.value = true; updatePlaybackState('playing') }).catch(() => { isPlaying.value = false; updatePlaybackState('paused') })
  }

  // 播放/暂停切换
  function toggle() {
    if (isPlaying.value) pause()
    else resume()
  }

  // 设置音量（0~1 区间限制）
  function setVolume(v) {
    volume.value = Math.max(0, Math.min(1, v))
    if (audio) audio.volume = volume.value
  }

  // 跳转到指定时间（秒）
  function seek(time) {
    const el = ensureAudio()
    if (!currentTrack.value) return
    const t = Math.max(0, Math.min(time, duration.value || el.duration || 0))
    el.currentTime = t
    currentTime.value = t
  }

  // 按百分比跳转进度
  function seekByPercent(pct) {
    const total = duration.value || (audio ? audio.duration : 0) || 0
    if (total > 0) seek((pct / 100) * total)
  }

  // 切换播放模式（单曲循环模式下启用 audio.loop）
  function setMode(mode) {
    playMode.value = mode
    if (mode === 'loop-one' && audio) {
      audio.loop = true
    } else if (audio) {
      audio.loop = false
    }
  }

  // 追加曲目到播放列表（去重）
  function addToPlaylist(track) {
    if (!playlist.value.find(t => t.id === track.id)) {
      playlist.value.push(track)
    }
  }

  // 整体替换播放列表
  function setPlaylist(tracks) { playlist.value = tracks || [] }

  // 切换播放列表抽屉显隐
  function togglePlaylist() { playlistVisible.value = !playlistVisible.value }

  // 底部浮窗开关（仅显隐，不停止播放）
  function showBar() { playerBarVisible.value = true }
  function hideBar() { playerBarVisible.value = false }
  function toggleBar() { playerBarVisible.value = !playerBarVisible.value }

  // 上一首（随机模式下随机选曲）
  function prev() {
    const list = playlist.value
    if (!list.length) return
    if (playMode.value === 'shuffle') {
      play(randomNext(list, currentTrack.value?.id))
      return
    }
    const idx = list.findIndex(t => t.id === currentTrack.value?.id)
    const prevTrack = list[idx > 0 ? idx - 1 : list.length - 1]
    if (prevTrack) play(prevTrack)
  }

  // 下一首（随机模式下随机选曲）
  function next() {
    const list = playlist.value
    if (!list.length) return
    if (playMode.value === 'shuffle') {
      play(randomNext(list, currentTrack.value?.id))
      return
    }
    const idx = list.findIndex(t => t.id === currentTrack.value?.id)
    const nextTrack = list[idx < list.length - 1 ? idx + 1 : 0]
    if (nextTrack) play(nextTrack)
  }

  // 停止并清空当前曲目
  function stop() {
    pause()
    currentTrack.value = null
    currentTime.value = 0
    duration.value = 0
    if (audio) { audio.removeAttribute('src'); audio.load() }
  }

  // 获取全局唯一 Audio 元素（可视化组件用它创建 MediaElementSource）
  function getAudioElement() {
    return ensureAudio()
  }

  // 秒数格式化为 mm:ss

  // 注册锁屏/通知栏控制按钮（store 实例方法已就绪时调用一次）
  // 调用时机：音乐页面 onMounted 时（避免空 store 时注册无意义 handler）
  function bindMediaSession() {
    bindMediaSessionHandlers({
      onPlay: () => resume(),
      onPause: () => pause(),
      onPrev: () => prev(),
      onNext: () => next(),
      onSeekTo: (t) => seek(t),
    })
  }

  return {
    currentTrack, isPlaying, volume, playlist, playlistVisible,
    playerBarVisible, currentTime, duration, playMode, hasTrack,
    lyrics, lyricsLoading, currentLyricIndex, currentLyricLine,
    play, pause, resume, toggle, setVolume, seek, seekByPercent,
    setMode, addToPlaylist, setPlaylist, togglePlaylist,
    showBar, hideBar, toggleBar,
    prev, next, stop, formatTime, getAudioElement,
    bindMediaSession,
  }
})
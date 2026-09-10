// ============================================================
// 全局音乐播放器 Store（Pinia）
// 单例 Audio 播放核心：播放 / 暂停 / 切歌 / 音量 / 进度 / 播放模式
// 被音乐页、GlobalPlayer 悬浮播放器与歌词组件共用
// ============================================================
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getNeteaseSongUrl, getNeteaseLyric } from '@/api/music'

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
      duration.value = audio.duration || 0
    })
    audio.addEventListener('durationchange', () => {
      duration.value = audio.duration || 0
    })
    audio.addEventListener('ended', handleEnded)
    audio.addEventListener('error', () => {
      isPlaying.value = false
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
    let target = track
    if (!target.url && /^\d+$/.test(String(target.neteaseId || ''))) {
      try {
        const r = await getNeteaseSongUrl(target.neteaseId)
        if (r?.data?.[0]?.url) target = { ...target, url: r.data[0].url }
      } catch { /* 取流失败则按原样处理 */ }
    }
    const el = ensureAudio()
    if (!target.url) {
      errorOnce('该曲目缺少播放地址')
      return
    }
    currentTrack.value = target
    el.src = target.url
    currentTime.value = 0
    duration.value = target.duration || 0
    lyrics.value = []
    loadLyrics(target)
    el.play().then(() => {
      isPlaying.value = true
    }).catch(() => {
      isPlaying.value = false
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
      const lrc = res?.data?.lrc?.lyric || res?.data?.lyric || ''
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
  // 同一错误提示 3 秒内只弹一次（避免循环触发刷屏）
  function errorOnce(msg) {
    if (lastErrorToast === msg) return
    lastErrorToast = msg
    setTimeout(() => { lastErrorToast = '' }, 3000)
  }

  // 暂停播放
  function pause() {
    isPlaying.value = false
    ensureAudio().pause()
  }

  // 继续播放
  function resume() {
    const el = ensureAudio()
    if (!currentTrack.value) return
    el.play().then(() => { isPlaying.value = true }).catch(() => { isPlaying.value = false })
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

  // 秒数格式化为 mm:ss
  function formatTime(secs) {
    if (!secs || !isFinite(secs) || secs < 0) return '00:00'
    secs = Math.floor(secs)
    const m = Math.floor(secs / 60)
    const s = secs % 60
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }

  return {
    currentTrack, isPlaying, volume, playlist, playlistVisible,
    playerBarVisible, currentTime, duration, playMode, hasTrack,
    lyrics, lyricsLoading, currentLyricIndex, currentLyricLine,
    play, pause, resume, toggle, setVolume, seek, seekByPercent,
    setMode, addToPlaylist, setPlaylist, togglePlaylist,
    showBar, hideBar, toggleBar,
    prev, next, stop, formatTime,
  }
})
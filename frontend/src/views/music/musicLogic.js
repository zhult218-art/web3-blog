/**
 * 模拟播放核心逻辑（纯 JS，不依赖 Vue / DOM）
 * 后续对接后端音乐接口时：将 tick 推进替换为真实 Audio 播放并保持同一状态结构即可。
 */

// 播放模式：order 顺序 | loop 循环 | shuffle 随机 | one 单曲
export const PLAY_MODES = ['order', 'loop', 'shuffle', 'one']

const TICK_MS = 250
const TICK_STEP = 0.25

// 根据当前播放时间定位歌词行下标（无匹配返回 -1）
export function findLyricIndex(lyrics, time) {
  if (!Array.isArray(lyrics) || lyrics.length === 0) return -1
  let idx = -1
  for (let i = 0; i < lyrics.length; i++) {
    if ((lyrics[i].time || 0) <= time) idx = i
    else break
  }
  return idx
}

// 秒数格式化为 mm:ss
export function formatTime(secs) {
  if (!secs || !isFinite(secs) || secs < 0) secs = 0
  secs = Math.floor(secs)
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// 创建模拟播放器实例：返回 state 与播放控制方法集合
export function createMusicPlayer({ playlist = [], onTick } = {}) {
  const state = {
    playlist: [...playlist],
    track: null,
    index: -1,
    playing: false,
    time: 0,
    duration: 0,
    modeIndex: 0, // 对应 PLAY_MODES
    liked: false,
    volume: 0.7,
  }

  let timer = null
  let endedFired = false

  function stopTimer() {
    if (timer) { clearInterval(timer); timer = null }
  }

  function emitTick() {
    if (typeof onTick === 'function') onTick(state)
  }

  function startTimer() {
    stopTimer()
    endedFired = false
    timer = setInterval(() => {
      state.time += TICK_STEP
      if (state.time >= state.duration) {
        state.time = state.duration
        emitTick()
        if (!endedFired) { endedFired = true; handleEnded() }
        return
      }
      emitTick()
    }, TICK_MS)
  }

  function mode() { return PLAY_MODES[state.modeIndex] }

  function playAt(idx) {
    const list = state.playlist
    if (!list.length) return
    idx = ((idx % list.length) + list.length) % list.length
    state.index = idx
    state.track = list[idx]
    state.time = 0
    state.duration = Number(state.track.duration) || 0
    if (!state.duration || state.duration <= 0) state.duration = 240
    state.liked = !!state.track.liked
    state.playing = true
    emitTick()
    startTimer()
  }

  function handleEnded() {
    stopTimer()
    const list = state.playlist
    if (!list.length) return
    const m = mode()
    if (m === 'one') { playAt(state.index); return }
    if (m === 'shuffle') {
      const r = Math.floor(Math.random() * list.length)
      playAt(r === state.index ? (r + 1) % list.length : r)
      return
    }
    const next = state.index + 1
    if (m === 'order' && next >= list.length) {
      state.playing = false
      emitTick()
      return
    }
    playAt(next)
  }

  return {
    state,

    play(track) {
      const list = state.playlist
      const idx = list.findIndex(t => (t.id || t.musicId) === (track.id || track.musicId))
      if (idx >= 0) { playAt(idx); return }
      list.push(track)
      playAt(list.length - 1)
    },

    toggle() {
      if (state.playing) this.pause()
      else this.resume()
    },

    pause() {
      state.playing = false
      stopTimer()
      emitTick()
    },

    resume() {
      if (!state.track) return
      if (state.time >= state.duration) { state.time = 0; emitTick() }
      state.playing = true
      emitTick()
      startTimer()
    },

    next() {
      if (!state.playlist.length) return
      const m = mode()
      const cur = state.track ? state.index : -1
      if (m === 'shuffle') {
        const r = Math.floor(Math.random() * state.playlist.length)
        playAt(r === cur ? (r + 1) % state.playlist.length : r)
        return
      }
      playAt((cur + 1) % state.playlist.length)
    },

    prev() {
      if (!state.playlist.length) return
      if (state.time > 3) { state.time = 0; emitTick(); return }
      const m = mode()
      const cur = state.track ? state.index : 0
      if (m === 'shuffle') {
        const r = Math.floor(Math.random() * state.playlist.length)
        playAt(r === cur ? (r + 1) % state.playlist.length : r)
        return
      }
      playAt(((cur - 1) % state.playlist.length + state.playlist.length) % state.playlist.length)
    },

    seekByPercent(pct) {
      if (!state.track) return
      state.time = Math.max(0, Math.min(state.duration, (pct / 100) * state.duration))
      emitTick()
    },

    cycleMode() {
      state.modeIndex = (state.modeIndex + 1) % PLAY_MODES.length
      emitTick()
    },

    toggleLike() {
      state.liked = !state.liked
      emitTick()
    },

    setPlaylist(list) {
      stopTimer()
      state.playlist = [...(list || [])]
      state.track = null
      state.index = -1
      state.time = 0
      state.duration = 0
      state.playing = false
      emitTick()
    },

    destroy() {
      stopTimer()
    },
  }
}
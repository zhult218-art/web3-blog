<template>
  <div class="music-player">
    <!-- 主区：黑胶 + 歌词 -->
    <div class="player-main" :class="{ 'player-main-single': !showVinyl }">
      <!-- 黑胶唱片 -->
      <div v-if="showVinyl" class="vinyl-side">
        <div class="tonearm" :class="{ 'tonearm-on': ui.playing }">
          <div class="tonearm-head"></div>
        </div>
        <div class="vinyl" :class="{ 'vinyl-spin': ui.playing }">
          <div class="vinyl-grooves"></div>
          <div class="vinyl-shine"></div>
          <div class="vinyl-label" :style="labelStyle">
            <img v-if="currentCover" :src="currentCover" class="label-img" alt="" />
            <div v-else class="label-fallback">{{ currentInitials }}</div>
          </div>
        </div>
        <div class="vinyl-tip">
          <span class="dot" :class="{ 'dot-on': ui.playing }"></span>
          {{ ui.playing ? 'PLAYING · 45RPM' : 'PAUSED' }}
        </div>
      </div>

      <!-- 滚动歌词 -->
      <div class="lyrics-wrap" ref="lyricsWrap">
        <div class="lyrics-inner" :style="{ transform: `translateY(${lyricsOffset}px)` }">
          <div
            v-for="(line, i) in lyricLines"
            :key="i"
            class="lyric-line"
            :class="{ 'lyric-active': i === lyricIndex }"
          >
            {{ line.text }}
          </div>
        </div>
        <div v-if="!lyricLines.length" class="lyrics-empty">
          <span class="empty-note">NO LYRICS</span>
          暂无歌词，静心聆听
        </div>
      </div>
    </div>

    <!-- 底部播放器栏 -->
    <div class="player-bar" data-glow>
      <!-- 进度区（支持点击与拖拽定位） -->
      <div class="bar-progress">
        <span class="bar-time">{{ format(ui.time) }}</span>
        <div class="progress-track" ref="progressTrack" @pointerdown="onSeekPointerDown">
          <div class="progress-fill" :style="{ width: (dragPct ?? progressPct) + '%' }">
            <span class="progress-knob" :class="{ 'knob-active': dragging }"></span>
          </div>
        </div>
        <span class="bar-time">{{ format(ui.duration) }}</span>
      </div>

      <!-- 控制按钮区 -->
      <div class="bar-controls">
        <span class="quality-chip">LOSSLESS · FLAC</span>

        <button class="ctl" :title="modeLabel" @click="cycleMode">
          <svg v-if="ui.modeIndex === 1" class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M7 7h10v3l4-4-4-4v3H5v7h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-7h-2v5z"/></svg>
          <svg v-else-if="ui.modeIndex === 2" class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/></svg>
          <svg v-else-if="ui.modeIndex === 3" class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M7 7h10v3l4-4-4-4v3H5v7h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-7h-2v5zm-4-2v-4h-1.5v4H13z"/></svg>
          <svg v-else class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M7 7h10v3l4-4-4-4v3H5v7h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-7h-2v5z"/></svg>
        </button>

        <button class="ctl" title="上一曲" @click="player.prev()">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M6 6h2v12H6V6zm3.5 6l8.5 6V6l-8.5 6z"/></svg>
        </button>

        <button class="play-btn" title="播放 / 暂停" @click="player.toggle()">
          <svg v-if="!ui.playing" class="icon icon-lg" viewBox="0 0 24 24"><path fill="currentColor" d="M8 5v14l11-7z"/></svg>
          <svg v-else class="icon icon-lg" viewBox="0 0 24 24"><path fill="currentColor" d="M6 4h4v16H6V4zm8 0h4v16h-4V4z"/></svg>
        </button>

        <button class="ctl" title="下一曲" @click="player.next()">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
        </button>

        <span class="ctl-divider"></span>

        <button class="ctl" title="音量" @click="toggleMute">
          <svg v-if="muted || volumePct === 0" class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M16.5 12A4.5 4.5 0 0014 7.97v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/></svg>
          <svg v-else class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0014 7.97v8.05A4.5 4.5 0 0016.5 12zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/></svg>
        </button>
        <input class="vol-slider" type="range" min="0" max="100" :value="volumePct"
          @input="onVolumeInput" :title="`音量 ${volumePct}%`" />

        <button class="ctl" data-glow :title="liked ? '取消喜欢' : '喜欢'" @click="toggleLike">
          <svg class="icon" :class="{ 'like-on': liked }" viewBox="0 0 24 24">
            <path fill="currentColor" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
          </svg>
        </button>

        <button class="ctl" :class="{ 'ctl-on': listOpen }" title="歌单" @click="listOpen = !listOpen">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M6 6h2v12H6V6zm3.5 6l8.5 6V6l-8.5 6zM15 4h4v2h-4V4z"/></svg>
        </button>

        <button class="ctl" title="评论">
          <svg class="icon" viewBox="0 0 24 24"><path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/></svg>
          <span class="ctl-num">{{ commentCount }}</span>
        </button>
      </div>

      <!-- 歌单面板 -->
      <transition name="list-fade">
        <div v-if="listOpen" class="playlist-panel" data-glow>
          <div class="pl-head">
            <span>NOW PLAYING · 播放列表</span>
            <span class="pl-count">{{ ui.playlist.length }} TRACKS</span>
          </div>
          <div class="pl-list">
            <div
              v-for="(t, i) in ui.playlist"
              :key="t.id || i"
              class="pl-item"
              :class="{ 'pl-item-on': ui.track && (ui.track.id || ui.track.musicId) === (t.id || t.musicId) }"
              @click="player.play(t)"
            >
              <span class="pl-idx">{{ isCurrent(t) ? '♪' : String(i + 1).padStart(2, '0') }}</span>
              <span class="pl-title">{{ t.title }}</span>
              <span class="pl-artist">{{ t.artist }}</span>
              <span class="pl-dur">{{ format(t.duration) }}</span>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 音乐播放器组件：真实播放（全局 player store 驱动）
// 播放控制、歌词滚动高亮（真实网易云歌词）、进度拖动、播放模式切换
// ====================================================
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { usePlayerStore } from '@/stores/modules/player'

const props = defineProps({
  tracks: { type: Array, default: () => [] },
  lyricsMap: { type: Object, default: () => ({}) },
  showVinyl: { type: Boolean, default: true },
  initialId: { type: [String, Number], default: null },
})

const emit = defineEmits(['track-change'])

const player = usePlayerStore()
const listOpen = ref(false)
const commentCount = ref(128)
const lyricsWrap = ref(null)
const muted = ref(false)
const liked = ref(false)
function toggleLike() { liked.value = !liked.value }

// ---- 进度条拖拽（pointer 事件：按住拖动实时预览，松手真正 seek）----
const progressTrack = ref(null)
const dragging = ref(false)
const dragPct = ref(null)

// 根据指针事件换算相对进度条的百分比（0~100）
function pctFromEvent(e) {
  const el = progressTrack.value
  if (!el) return 0
  const rect = el.getBoundingClientRect()
  return Math.min(100, Math.max(0, ((e.clientX - rect.left) / rect.width) * 100))
}

function onSeekPointerDown(e) {
  dragging.value = true
  dragPct.value = pctFromEvent(e)
  window.addEventListener('pointermove', onSeekPointerMove)
  window.addEventListener('pointerup', onSeekPointerUp)
}

function onSeekPointerMove(e) {
  if (!dragging.value) return
  dragPct.value = pctFromEvent(e)
}

function onSeekPointerUp(e) {
  if (!dragging.value) return
  const pct = pctFromEvent(e)
  dragging.value = false
  dragPct.value = null
  window.removeEventListener('pointermove', onSeekPointerMove)
  window.removeEventListener('pointerup', onSeekPointerUp)
  player.seekByPercent(pct)
}

// ---- 音量：滑块调节 + 静音切换（记住静音前的音量）----
let savedVolume = player.volume > 0 ? player.volume : 0.7
const volumePct = computed(() => Math.round((player.volume ?? 0.7) * 100))

function onVolumeInput(e) {
  const v = Math.min(1, Math.max(0, Number(e.target.value) / 100))
  muted.value = v === 0
  if (v > 0) savedVolume = v
  player.setVolume(v)
}

function toggleMute() {
  if (!muted.value) {
    if (player.volume > 0) savedVolume = player.volume
    player.setVolume(0)
    muted.value = true
  } else {
    player.setVolume(savedVolume > 0 ? savedVolume : 0.7)
    muted.value = false
  }
}

// 统一暴露给模板的响应式状态：直接映射全局 store
const ui = reactive({
  get track() { return player.currentTrack },
  get playing() { return player.isPlaying },
  get time() { return player.currentTime },
  get duration() { return player.duration || (player.currentTrack?.duration || 0) },
  get playlist() { return player.playlist },
  get modeIndex() { return ({ order: 0, shuffle: 2, 'loop-one': 3 })[player.playMode] ?? 0 },
  get liked() { return liked.value },
  get volume() { return player.volume },
})

// tracks props 变化时同步到全局列表，并自动播放 initialId 曲目
// 说明：neteaseId 仅在曲目确实带有网易云 id 时才写入（绝不拿本地库 id 冒充），
//       否则歌词接口会拉到无关歌曲
watch(() => props.tracks, (list) => {
  const normalized = (list || []).map((t, i) => ({
    ...t,
    id: t.id || t.musicId || i + 1,
    neteaseId: t.neteaseId || t.netease_id || '',
    duration: Number(t.duration) || 240,
    url: t.url || t.audio_url || '',
  }))
  // 详情页拥有自己的曲目上下文：始终用本页列表作为全局播放列表，避免与其它页面残留列表串歌
  if (normalized.length) player.setPlaylist(normalized)
  if (props.initialId) {
    const target = normalized.find(t => String(t.id || t.musicId) === String(props.initialId)) || normalized[0]
    if (target && player.currentTrack?.id !== target.id) player.play({ ...target, neteaseId: target.neteaseId })
  }
}, { immediate: true })

watch(() => ui.track, (t) => emit('track-change', t))

const currentCover = computed(() => ui.track?.cover || '')
const currentInitials = computed(() => {
  const t = ui.track
  if (!t) return '♪'
  return (t.title || 'M')?.slice(0, 1).toUpperCase()
})

const labelStyle = computed(() => {
  if (currentCover.value) return {}
  const seed = (ui.track?.id || 'x').toString().split('').reduce((a, c) => a + c.charCodeAt(0), 0)
  const hues = [26, 340, 200, 160, 45]
  const h = hues[seed % hues.length]
  return { background: `linear-gradient(135deg, hsl(${h},70%,55%), hsl(${(h + 40) % 360},65%,40%))` }
})

const lyricLines = computed(() => player.lyrics && player.lyrics.length ? player.lyrics : [])

const lyricIndex = computed(() => player.currentLyricIndex)

const lyricsOffset = computed(() => {
  const wrapH = lyricsWrap.value?.clientHeight || 320
  const lineH = 46
  const idx = lyricIndex.value
  if (idx < 0) return lineH
  return wrapH / 2 - lineH / 2 - idx * lineH
})

const progressPct = computed(() => {
  if (!ui.duration) return 0
  return Math.min(100, Math.max(0, (ui.time / ui.duration) * 100))
})

const MODE_LABELS = ['顺序播放', '循环列表', '随机播放', '单曲循环']
const modeLabel = computed(() => MODE_LABELS[ui.modeIndex] || '顺序播放')

// 时间格式化（全局 store）
function format(s) { return player.formatTime(s) }

// 判断指定歌曲是否为当前播放曲目
function isCurrent(t) {
  const cur = ui.track
  if (!cur) return false
  return String(cur.id || cur.musicId) === String(t.id || t.musicId)
}

// 切换播放模式（顺序/循环列表/随机/单曲循环）
function cycleMode() {
  const map = ['order', 'shuffle', 'loop-one']
  const idx = map.indexOf(player.playMode)
  player.setMode(map[(idx + 1) % map.length])
}

// 点击歌词行时跳到对应时间点定位播放
function onKey(e) {
  if (e.code === 'Space' && e.target.tagName !== 'BUTTON') {
    e.preventDefault()
    player.toggle()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKey)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  window.removeEventListener('pointermove', onSeekPointerMove)
  window.removeEventListener('pointerup', onSeekPointerUp)
})
</script>

<style scoped>
.music-player {
  position: relative;
  width: 100%;
  min-height: 520px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

/* ============ 主区 ============ */
.player-main {
  display: grid;
  grid-template-columns: minmax(320px, 1.05fr) minmax(300px, 1fr);
  gap: 3rem;
  align-items: center;
  flex: 1;
  padding-bottom: 2.4rem;
}

.player-main-single {
  grid-template-columns: 1fr;
}

/* ---- 黑胶 ---- */
.vinyl-side {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 0 0;
}

.vinyl {
  position: relative;
  width: 330px;
  height: 330px;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 50%, #161616 0%, #0b0b0b 58%, #000 100%);
  box-shadow:
    inset 0 0 0 4px rgba(255, 255, 255, 0.04),
    inset 0 0 24px rgba(0, 0, 0, 0.9),
    0 26px 60px rgba(0, 0, 0, 0.35);
  overflow: hidden;
}

.vinyl-grooves {
  position: absolute;
  inset: 6%;
  border-radius: 50%;
  background: repeating-radial-gradient(circle at 50% 50%, transparent 0 2px, rgba(255, 255, 255, 0.035) 2px 3px);
  mask-image: radial-gradient(circle at 50% 50%, #000 10%, transparent 95%);
}

.vinyl-shine {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.09) 0%, transparent 30%, transparent 62%, rgba(255, 255, 255, 0.05) 82%);
}

.vinyl-label {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 128px;
  height: 128px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.25);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 0 6px rgba(255, 255, 255, 0.08), 0 6px 18px rgba(0, 0, 0, 0.5);
}

.label-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.label-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

.vinyl-spin {
  animation: vinylRotate 1.8s linear infinite;
}

@keyframes vinylRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 唱臂 */
.tonearm {
  position: absolute;
  top: 12px;
  right: calc(50% - 118px);
  width: 150px;
  height: 26px;
  transform-origin: 6px 12px;
  transform: rotate(14deg);
  transition: transform 0.6s cubic-bezier(0.23, 1, 0.32, 1);
  z-index: 3;
  pointer-events: none;
}

.tonearm::before {
  content: '';
  position: absolute;
  top: 8px;
  left: 0;
  width: 146px;
  height: 5px;
  border-radius: 4px;
  background: linear-gradient(90deg, #3b3b45, #23232b);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
}

.tonearm::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #6b6b78, #23232b);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.5);
}

.tonearm-head {
  position: absolute;
  top: 2px;
  right: -10px;
  width: 16px;
  height: 20px;
  border-radius: 0 40% 40% 0;
  background: linear-gradient(90deg, #4a4a56, #1c1c22);
}

.tonearm-on { transform: rotate(0deg); }

.vinyl-tip {
  margin-top: 1.6rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.68rem;
  letter-spacing: 0.3em;
  font-family: 'Courier New', monospace;
  color: rgba(148, 163, 184, 0.65);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.6);
}

.dot-on {
  background: #6ee7b7;
  box-shadow: 0 0 10px rgba(110, 231, 183, 0.8);
  animation: dotPulse 1.2s ease-in-out infinite;
}

@keyframes dotPulse { 50% { opacity: 0.4; } }

/* ---- 歌词 ---- */
.lyrics-wrap {
  position: relative;
  height: 320px;
  overflow: hidden;
  padding: 0 0.4rem 0 1rem;
  border-left: 1px solid rgba(148, 163, 184, 0.14);
}

.lyrics-inner {
  transition: transform 0.55s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.lyric-line {
  display: flex;
  align-items: center;
  height: 46px;
  font-size: 1.02rem;
  color: rgba(203, 213, 225, 0.32);
  letter-spacing: 0.08em;
  transition: color 0.45s ease, transform 0.45s ease, opacity 0.45s ease;
  transform: scale(1);
  opacity: 0.55;
}

.lyric-active {
  color: #f1f5ff;
  text-shadow: 0 0 18px rgba(165, 243, 252, 0.35);
  transform: scale(1.06);
  opacity: 1;
  font-weight: 700;
}

.lyrics-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.7rem;
  font-size: 0.85rem;
  color: rgba(148, 163, 184, 0.5);
  letter-spacing: 0.1em;
}

.empty-note {
  font-size: 0.66rem;
  letter-spacing: 0.35em;
  color: rgba(148, 163, 184, 0.35);
  font-family: 'Courier New', monospace;
}

/* ============ 底部播放器栏 ============ */
.player-bar {
  position: relative;
  padding: 1.1rem 1.6rem 1rem;
  border-radius: 20px;
  background: rgba(10, 12, 32, 0.72);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(148, 163, 184, 0.12);
}

.bar-progress {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.9rem;
}

.bar-time {
  font-size: 0.7rem;
  font-family: 'Courier New', monospace;
  color: rgba(148, 163, 184, 0.7);
  min-width: 44px;
  text-align: center;
}

.progress-track {
  flex: 1;
  height: 18px;
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
}

.progress-track::before {
  content: '';
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.18);
}

.progress-fill {
  position: absolute;
  left: 0;
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(90deg, #22d3ee, #a855f7);
  box-shadow: 0 0 10px rgba(168, 85, 247, 0.4);
  transition: width 0.12s linear;
}

.progress-knob {
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e0e7ff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.25s;
}

.progress-track:hover .progress-knob { opacity: 1; }
.progress-knob.knob-active { opacity: 1; box-shadow: 0 0 0 4px rgba(168, 85, 247, 0.3); }

/* 音量滑块 */
.vol-slider {
  width: 64px;
  height: 3px;
  accent-color: #22d3ee;
  cursor: pointer;
}

.bar-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.9rem;
}

.quality-chip {
  margin-right: auto;
  font-size: 0.6rem;
  letter-spacing: 0.22em;
  color: rgba(148, 163, 184, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 999px;
  padding: 0.32rem 0.8rem;
  font-family: 'Courier New', monospace;
}

.ctl {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: rgba(203, 213, 225, 0.65);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.35rem;
  border-radius: 10px;
  transition: all 0.25s;
  position: relative;
}

.ctl:hover { color: #f1f5ff; background: rgba(148, 163, 184, 0.1); transform: translateY(-1px); }
.ctl-on { background: rgba(103, 232, 249, 0.12); color: #a5f3fc; }

.icon {
  width: 19px;
  height: 19px;
  display: block;
}

.icon-lg { width: 22px; height: 22px; }

.like-on { color: #fb7185; filter: drop-shadow(0 0 6px rgba(251, 113, 133, 0.55)); }

.ctl-num {
  font-size: 0.62rem;
  font-family: 'Courier New', monospace;
  color: inherit;
}

.ctl-divider {
  width: 1px;
  height: 22px;
  background: rgba(148, 163, 184, 0.2);
  margin: 0 0.3rem;
}

.play-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 1px solid rgba(165, 243, 252, 0.4);
  background: radial-gradient(circle at 32% 28%, #312e81, #0f0f2e 68%);
  color: #eef2ff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 8px 26px rgba(124, 58, 237, 0.4), 0 0 18px rgba(34, 211, 238, 0.2);
  transition: transform 0.25s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.3s;
}

.play-btn:hover { transform: scale(1.07); box-shadow: 0 12px 34px rgba(124, 58, 237, 0.55), 0 0 26px rgba(34, 211, 238, 0.3); }

/* ---- 歌单面板 ---- */
.playlist-panel {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(100% + 12px);
  border-radius: 16px;
  background: rgba(8, 10, 28, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.55), 0 0 24px rgba(124, 58, 237, 0.15);
  padding: 1rem;
  max-height: 300px;
  overflow: hidden;
}

.pl-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.66rem;
  letter-spacing: 0.22em;
  font-family: 'Courier New', monospace;
  color: rgba(148, 163, 184, 0.65);
  padding: 0.2rem 0.4rem 0.8rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  margin-bottom: 0.5rem;
}

.pl-list {
  max-height: 224px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.3) transparent;
}

.pl-list::-webkit-scrollbar { width: 5px; }
.pl-list::-webkit-scrollbar-thumb { background: rgba(148, 163, 184, 0.25); border-radius: 999px; }

.pl-item {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.55rem 0.6rem;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.pl-item:hover { background: rgba(148, 163, 184, 0.08); }

.pl-item-on { background: rgba(103, 232, 249, 0.1); }

.pl-idx {
  width: 26px;
  font-size: 0.68rem;
  font-family: 'Courier New', monospace;
  color: rgba(148, 163, 184, 0.5);
  text-align: center;
}

.pl-item-on .pl-idx { color: #67e8f9; }

.pl-title { flex: 1; font-size: 0.86rem; font-weight: 600; color: #eef2ff; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.pl-artist { font-size: 0.72rem; color: rgba(148, 163, 184, 0.7); }
.pl-dur { font-size: 0.68rem; font-family: 'Courier New', monospace; color: rgba(148, 163, 184, 0.55); }

.list-fade-enter-active, .list-fade-leave-active { transition: opacity 0.25s, transform 0.25s; }
.list-fade-enter-from, .list-fade-leave-to { opacity: 0; transform: translateY(8px); }

/* ---- 响应式（优先大屏）---- */
@media (max-width: 1100px) {
  .vinyl { width: 260px; height: 260px; }
  .vinyl-label { width: 100px; height: 100px; }
  .tonearm { right: calc(50% - 92px); }
}

@media (max-width: 900px) {
  .player-main { grid-template-columns: 1fr; gap: 1.6rem; }
  .lyrics-wrap { border-left: none; border-top: 1px solid rgba(148, 163, 184, 0.14); padding: 1rem 0 0; height: 240px; }
  .quality-chip { display: none; }
  .vol-slider { display: none; }
}
</style>
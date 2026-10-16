<template>
  <!-- Global floating player bar at bottom — YesPlayMusic inspired -->
  <Transition name="global-player">
    <div
      v-if="player.hasTrack && player.playerBarVisible"
      class="gp-root fixed bottom-0 left-0 right-0 z-50"
    >
      <!-- Dynamic album-art gradient background -->
      <div
        class="gp-bg absolute inset-0 pointer-events-none"
        :style="bgStyle"
      />

      <!-- Frosted glass base -->
      <div class="gp-body relative border-t border-white/[0.06] bg-[#06060e]/80 backdrop-blur-2xl">
        <div class="mx-auto max-w-6xl px-3 sm:px-5">

          <!-- ─── Progress bar (top edge) ─── -->
          <div
            ref="progressBar"
            class="gp-progress-wrap group/prog relative -mt-[3px] h-[6px] cursor-pointer hover:h-[10px] transition-all duration-200 ease-out z-10"
            @click="onProgressClick"
            @mousemove="onProgressHover"
            @mouseleave="hoverTime = null"
          >
            <div class="absolute inset-0 bg-white/[0.06] rounded-full" />
            <div
              class="gp-progress-fill absolute inset-y-0 left-0 rounded-full"
              :style="{ width: progressPercent + '%' }"
            />
            <!-- Thumb dot -->
            <div
              class="gp-thumb absolute top-1/2 -translate-y-1/2 w-3 h-3 rounded-full bg-white shadow-[0_0_10px_rgba(255,255,255,0.5)] opacity-0 group-hover/prog:opacity-100 transition-opacity duration-150"
              :style="{ left: 'calc(' + progressPercent + '% - 6px)' }"
            />
            <!-- Hover time tooltip -->
            <div
              v-if="hoverTime !== null"
              class="gp-tooltip absolute -top-8 px-1.5 py-0.5 rounded bg-black/80 text-[10px] text-white/90 font-mono pointer-events-none -translate-x-1/2 whitespace-nowrap"
              :style="{ left: hoverX + 'px' }"
            >{{ player.formatTime(hoverTime) }}</div>
          </div>

          <!-- ─── Main row ─── -->
          <div class="gp-row flex items-center gap-2 sm:gap-4 pt-1.5 pb-1">

            <!-- LEFT: Album art + track info -->
            <div class="gp-left flex items-center gap-2.5 flex-1 min-w-0 cursor-pointer" @click="showPlaylist = !showPlaylist">
              <div class="gp-cover-wrap relative w-10 h-10 rounded-lg overflow-hidden flex-shrink-0 shadow-lg">
                <img
                  v-if="player.currentTrack?.cover"
                  :src="player.currentTrack.cover"
                  crossorigin="anonymous"
                  class="gp-cover-img w-full h-full object-cover"
                  :class="{ 'gp-spin': player.isPlaying }"
                  @load="onCoverLoad"
                  @error="coverLoadError = true"
                  alt=""
                />
                <div
                  v-else
                  class="w-full h-full bg-gradient-to-br from-purple-500/30 to-cyan-500/30 flex items-center justify-center"
                  :class="{ 'animate-pulse-slow': player.isPlaying }"
                >
                  <span class="text-base">{{ player.isPlaying ? '🎵' : '⏸' }}</span>
                </div>
                <!-- Subtle ring overlay -->
                <div class="absolute inset-0 rounded-lg ring-1 ring-white/[0.08]" />
              </div>
              <div class="min-w-0 hidden sm:block">
                <p class="gp-title text-sm font-semibold text-white/95 truncate leading-tight">{{ player.currentTrack?.title }}</p>
                <p class="gp-artist text-[11px] text-white/40 truncate leading-tight mt-0.5">{{ player.currentTrack?.artist || '未知艺术家' }}</p>
              </div>
            </div>

            <!-- CENTER: Playback controls + time -->
            <div class="gp-center flex flex-col items-center gap-0.5 flex-shrink-0">
              <div class="flex items-center gap-1.5 sm:gap-2.5">
                <button class="gp-btn text-white/50 hover:text-white transition-colors" @click="player.prev" title="上一首">
                  <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M6 6h2v12H6zm3.5 6 8.5 6V6z"/></svg>
                </button>
                <button
                  class="gp-play-btn w-9 h-9 rounded-full flex items-center justify-center text-white transition-all hover:scale-105 active:scale-95"
                  :style="playBtnStyle"
                  @click="player.toggle"
                >
                  <svg v-if="!player.isPlaying" class="w-4 h-4 ml-0.5" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                  <svg v-else class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z"/></svg>
                </button>
                <button class="gp-btn text-white/50 hover:text-white transition-colors" @click="player.next" title="下一首">
                  <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
                </button>
              </div>
              <div class="flex items-center gap-1.5 text-[10px] font-mono text-white/30">
                <span class="w-9 text-right tabular-nums">{{ player.formatTime(player.currentTime) }}</span>
                <span class="text-white/15">/</span>
                <span class="w-9 tabular-nums">{{ player.formatTime(player.duration || player.currentTrack?.duration) }}</span>
              </div>
            </div>

            <!-- RIGHT: Volume + toggles + close -->
            <div class="gp-right flex items-center gap-1 sm:gap-2 flex-1 justify-end flex-shrink-0">
              <!-- Play mode -->
              <button class="gp-toggle-btn" :title="modeTitle" @click="cycleMode">
                <svg v-if="player.playMode === 'order'" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z"/></svg>
                <svg v-else-if="player.playMode === 'shuffle'" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M10.59 9.17 5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/></svg>
                <svg v-else class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z"/><text x="12" y="15" font-size="7" text-anchor="middle" fill="currentColor">1</text></svg>
              </button>

              <!-- Lyrics toggle -->
              <button
                class="gp-toggle-btn hidden sm:flex"
                :class="{ 'gp-toggle-active': showLyrics }"
                @click="showLyrics = !showLyrics"
                :title="showLyrics ? '收起歌词' : '显示歌词'"
              >
                <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/><path d="M7 9h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z"/></svg>
              </button>

              <!-- Volume (desktop only) -->
              <div class="gp-volume hidden sm:flex items-center gap-1.5 group/vol">
                <button class="gp-toggle-btn" @click="toggleMute" :title="player.volume === 0 ? '取消静音' : '静音'">
                  <svg v-if="player.volume > 0.5" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/></svg>
                  <svg v-else-if="player.volume > 0" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M7 9v6h4l5 5V4l-5 5H7z"/><path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/></svg>
                  <svg v-else class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3 3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4 9.91 6.09 12 8.18V4z"/></svg>
                </button>
                <div class="gp-vol-slider-wrap w-0 overflow-hidden group-hover/vol:w-20 transition-all duration-300 ease-out">
                  <input
                    type="range" min="0" max="100"
                    :value="Math.round(player.volume * 100)"
                    @input="player.setVolume($event.target.value / 100)"
                    class="gp-vol-slider"
                  />
                </div>
              </div>

              <!-- Playlist toggle -->
              <button
                class="gp-toggle-btn relative"
                :class="{ 'gp-toggle-active': showPlaylist }"
                @click="showPlaylist = !showPlaylist"
                :title="'播放列表 (' + player.playlist.length + ')'"
              >
                <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z"/></svg>
                <span v-if="player.playlist.length" class="gp-badge absolute -top-1 -right-1 min-w-[14px] h-3.5 rounded-full text-[9px] font-bold flex items-center justify-center px-0.5">
                  {{ player.playlist.length > 99 ? '99+' : player.playlist.length }}
                </span>
              </button>

              <!-- Close -->
              <button class="gp-toggle-btn text-white/30 hover:text-white/70 transition-colors ml-0.5" @click="player.hideBar" title="收起播放器">
                <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
              </button>
            </div>
          </div>
        </div>

        <!-- ─── Playlist dropdown ─── -->
        <Transition name="slide-up">
          <div
            v-if="showPlaylist && player.playlist.length"
            class="gp-panel absolute bottom-full left-0 right-0 mb-2 mx-auto max-w-6xl bg-[#0a0a18]/90 backdrop-blur-2xl border border-white/[0.08] rounded-xl shadow-2xl shadow-black/50 overflow-hidden"
          >
            <div class="flex items-center justify-between px-4 pt-3 pb-2 border-b border-white/[0.06]">
              <p class="text-[11px] text-white/40 uppercase tracking-wider font-medium">播放列表</p>
              <p class="text-[11px] text-white/25 font-mono">{{ player.playlist.length }} 首</p>
            </div>
            <div class="gp-playlist-scroll max-h-64 overflow-y-auto px-1.5 py-1.5">
              <div
                v-for="(t, i) in player.playlist"
                :key="t.id"
                class="gp-pl-item flex items-center gap-3 px-3 py-2 rounded-lg cursor-pointer transition-all duration-150"
                :class="player.currentTrack?.id === t.id ? 'gp-pl-item-active' : 'hover:bg-white/[0.04]'"
                @click="player.play(t)"
              >
                <!-- Index or EQ bars -->
                <div class="w-5 flex items-center justify-center flex-shrink-0">
                  <template v-if="player.currentTrack?.id === t.id">
                    <div class="gp-eq-bars flex items-end gap-[2px] h-3.5">
                      <span /><span /><span /><span />
                    </div>
                  </template>
                  <span v-else class="text-[10px] text-white/20 font-mono">{{ i + 1 }}</span>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-xs truncate" :class="player.currentTrack?.id === t.id ? 'text-white/90 font-medium' : 'text-white/60'">{{ t.title }}</p>
                </div>
                <span class="text-[10px] text-white/20 font-mono flex-shrink-0">{{ player.formatTime(t.duration) }}</span>
              </div>
            </div>
          </div>
        </Transition>

        <!-- ─── Lyrics panel (slide-up from bottom) ─── -->
        <Transition name="slide-up">
          <div
            v-if="showLyrics"
            class="gp-panel absolute bottom-full left-0 right-0 mb-2 mx-auto max-w-6xl bg-[#0a0a18]/85 backdrop-blur-2xl border border-white/[0.08] rounded-xl shadow-2xl shadow-black/50 overflow-hidden"
          >
            <div class="flex items-center justify-between px-4 pt-3 pb-2 border-b border-white/[0.06]">
              <p class="text-[11px] text-white/40 uppercase tracking-wider font-medium truncate mr-2">
                歌词 · {{ player.currentTrack?.title }}
              </p>
              <p class="text-[11px] text-white/25 font-mono flex-shrink-0">
                {{ player.lyricsLoading ? '加载中...' : (player.lyrics.length ? player.lyrics.length + ' 行' : '暂无歌词') }}
              </p>
            </div>
            <div class="gp-lyrics-scroll max-h-72 overflow-y-auto px-4 py-3" ref="lyricsBox">
              <template v-if="player.lyrics.length">
                <p
                  v-for="(line, i) in player.lyrics"
                  :key="i"
                  class="gp-lyric-line px-2 py-1.5 cursor-pointer transition-all duration-300 leading-relaxed"
                  :class="i === player.currentLyricIndex ? 'gp-lyric-active' : 'gp-lyric-inactive'"
                  @click="player.seek(line.time)"
                >{{ line.text }}</p>
              </template>
              <p v-else class="py-10 text-center text-xs text-white/20">暂无歌词，静心聆听</p>
            </div>
          </div>
        </Transition>
      </div>

      <!-- ─── Bottom visualizer bars ─── -->
      <div class="gp-bars" aria-hidden="true">
        <AudioBars :height="24" :bars="72" :accent="dominantColor" />
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ============================================================
// 全局悬浮播放器（GlobalPlayer）— YesPlayMusic 风格重设计
// 固定在页面底部，由 player store 驱动：
// 进度条、上/下一首、播放/暂停、播放模式、音量、曲目列表、歌词
// 由 App.vue 全局挂载，任何页面播放音乐都会出现
// ============================================================
import { computed, ref, watch, nextTick } from 'vue'
import { usePlayerStore } from '@/stores/modules/player'
import AudioBars from './AudioBars.vue'

const player = usePlayerStore()
const progressBar = ref(null)
const showPlaylist = ref(false)
const showLyrics = ref(false)
const lyricsBox = ref(null)

// ── Album art color extraction ──
const dominantColor = ref({ r: 88, g: 28, b: 135 }) // default purple-ish
const coverLoadError = ref(false)

function onCoverLoad(e) {
  extractColor(e.target)
}

function extractColor(imgEl) {
  try {
    const canvas = document.createElement('canvas')
    const size = 32 // small for performance
    canvas.width = size
    canvas.height = size
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    ctx.drawImage(imgEl, 0, 0, size, size)
    const data = ctx.getImageData(0, 0, size, size).data
    let r = 0, g = 0, b = 0, count = 0
    // Sample every 4th pixel for speed
    for (let i = 0; i < data.length; i += 16) {
      r += data[i]
      g += data[i + 1]
      b += data[i + 2]
      count++
    }
    if (count > 0) {
      dominantColor.value = {
        r: Math.round(r / count),
        g: Math.round(g / count),
        b: Math.round(b / count),
      }
    }
  } catch {
    // CORS or other error — keep previous color, silently ignore
  }
}

// Background gradient style from extracted color
const bgStyle = computed(() => {
  const { r, g, b } = dominantColor.value
  return {
    background: `
      radial-gradient(ellipse 120% 200% at 0% 100%, rgba(${r},${g},${b},0.18) 0%, transparent 60%),
      radial-gradient(ellipse 80% 150% at 100% 100%, rgba(${Math.min(255, r + 40)},${Math.min(255, g + 20)},${b},0.10) 0%, transparent 50%)
    `,
  }
})

// Play button gradient using album color
const playBtnStyle = computed(() => {
  const { r, g, b } = dominantColor.value
  const lighter = `rgb(${Math.min(255, r + 60)},${Math.min(255, g + 40)},${Math.min(255, b + 60)})`
  const base = `rgb(${r},${g},${b})`
  return {
    background: `linear-gradient(135deg, ${base}, ${lighter})`,
    boxShadow: `0 4px 20px rgba(${r},${g},${b},0.4)`,
  }
})

// ── Progress bar ──
const progressPercent = computed(() => {
  const total = player.duration || player.currentTrack?.duration || 0
  if (!total) return 0
  return Math.min(100, Math.max(0, (player.currentTime / total) * 100))
})

// Hover tooltip for progress bar
const hoverTime = ref(null)
const hoverX = ref(0)

function onProgressHover(e) {
  const el = progressBar.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  const total = player.duration || player.currentTrack?.duration || 0
  hoverTime.value = pct * total
  hoverX.value = e.clientX - rect.left
}

// ── Play mode ──
const modeTitle = computed(() => {
  const map = { order: '顺序播放', shuffle: '随机播放', 'loop-one': '单曲循环' }
  return map[player.playMode]
})

const MODES = ['order', 'shuffle', 'loop-one']

function cycleMode() {
  const idx = MODES.indexOf(player.playMode)
  player.setMode(MODES[(idx + 1) % MODES.length])
}

// ── Progress click ──
function onProgressClick(e) {
  const el = progressBar.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const pct = ((e.clientX - rect.left) / rect.width) * 100
  player.seekByPercent(pct)
}

// ── Volume mute toggle ──
let lastVolume = 0.7
function toggleMute() {
  if (player.volume > 0) {
    lastVolume = player.volume
    player.setVolume(0)
  } else {
    player.setVolume(lastVolume || 0.7)
  }
}

// ── Watchers ──
watch(() => player.currentTrack, () => {
  showPlaylist.value = false
  coverLoadError.value = false
})

watch(() => player.currentLyricIndex, (idx) => {
  const box = lyricsBox.value
  if (!box || idx < 0) return
  nextTick(() => {
    const els = box.querySelectorAll('p')
    const active = els[idx]
    if (active) {
      const top = active.offsetTop - box.clientHeight / 2 + active.clientHeight / 2
      box.scrollTo({ top, behavior: 'smooth' })
    }
  })
})
</script>

<style scoped>
/* ── Transition ── */
.global-player-enter-active,
.global-player-leave-active { transition: all 0.4s cubic-bezier(0.23, 1, 0.32, 1); }
.global-player-enter-from,
.global-player-leave-to { opacity: 0; transform: translateY(100%); }

/* ── Slide-up panel transition ── */
.slide-up-enter-active,
.slide-up-leave-active { transition: all 0.3s cubic-bezier(0.23, 1, 0.32, 1); }
.slide-up-enter-from,
.slide-up-leave-to { opacity: 0; transform: translateY(12px); }

/* ── Cover spin ── */
@keyframes gp-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.gp-spin {
  animation: gp-spin 20s linear infinite;
}

@keyframes pulse-slow {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.85; transform: scale(1.03); }
}
.animate-pulse-slow { animation: pulse-slow 3s ease-in-out infinite; }

/* ── Progress bar ── */
.gp-progress-wrap {
  position: relative;
}
.gp-progress-fill {
  background: linear-gradient(90deg, rgba(168,85,247,0.7), rgba(34,211,238,0.7));
  transition: width 0.15s linear;
  border-radius: 9999px;
}
.gp-progress-wrap:hover .gp-progress-fill {
  background: linear-gradient(90deg, rgb(168,85,247), rgb(34,211,238));
}
.gp-thumb {
  transition: opacity 0.15s;
  pointer-events: none;
}

/* ── Button styles ── */
.gp-btn {
  padding: 4px;
  border-radius: 6px;
  transition: color 0.15s, background 0.15s;
}
.gp-btn:hover {
  background: rgba(255,255,255,0.06);
}

.gp-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  color: rgba(255,255,255,0.4);
  transition: all 0.15s;
}
.gp-toggle-btn:hover {
  color: rgba(255,255,255,0.8);
  background: rgba(255,255,255,0.06);
}
.gp-toggle-active {
  color: rgb(196,181,253) !important;
  background: rgba(168,85,247,0.12);
}

/* ── Volume slider ── */
.gp-vol-slider {
  -webkit-appearance: none;
  appearance: none;
  width: 80px;
  height: 3px;
  border-radius: 2px;
  background: rgba(255,255,255,0.12);
  outline: none;
  cursor: pointer;
}
.gp-vol-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 0 6px rgba(255,255,255,0.3);
  cursor: pointer;
}
.gp-vol-slider::-moz-range-thumb {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: white;
  border: none;
  box-shadow: 0 0 6px rgba(255,255,255,0.3);
  cursor: pointer;
}

/* ── Playlist ── */
.gp-pl-item-active {
  background: rgba(168,85,247,0.08);
}

/* EQ bars animation for currently playing track */
.gp-eq-bars span {
  display: block;
  width: 2.5px;
  border-radius: 1px;
  background: rgb(168,85,247);
  animation: gp-eq 0.8s ease-in-out infinite alternate;
}
.gp-eq-bars span:nth-child(1) { height: 40%; animation-delay: 0s; }
.gp-eq-bars span:nth-child(2) { height: 70%; animation-delay: 0.15s; }
.gp-eq-bars span:nth-child(3) { height: 50%; animation-delay: 0.3s; }
.gp-eq-bars span:nth-child(4) { height: 80%; animation-delay: 0.45s; }

@keyframes gp-eq {
  0% { height: 20%; }
  100% { height: 100%; }
}

/* Playlist scrollbar */
.gp-playlist-scroll::-webkit-scrollbar { width: 4px; }
.gp-playlist-scroll::-webkit-scrollbar-track { background: transparent; }
.gp-playlist-scroll::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.08); border-radius: 2px; }
.gp-playlist-scroll::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.15); }

/* ── Lyrics ── */
.gp-lyric-active {
  color: rgba(196,181,253,1);
  font-size: 15px;
  font-weight: 600;
  text-shadow: 0 0 20px rgba(168,85,247,0.5), 0 0 40px rgba(168,85,247,0.2);
  transform: scale(1.02);
  transform-origin: left center;
}
.gp-lyric-inactive {
  color: rgba(255,255,255,0.25);
  font-size: 13px;
}
.gp-lyric-inactive:hover {
  color: rgba(255,255,255,0.5);
}

.gp-lyrics-scroll::-webkit-scrollbar { width: 4px; }
.gp-lyrics-scroll::-webkit-scrollbar-track { background: transparent; }
.gp-lyrics-scroll::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.08); border-radius: 2px; }

/* ── Badge ── */
.gp-badge {
  background: rgba(168,85,247,0.7);
  color: white;
  font-size: 9px;
  line-height: 1;
}

/* ── Bottom bars ── */
.gp-bars {
  pointer-events: none;
  margin: 3px -16px -8px;
  padding: 0 16px;
  opacity: 0.95;
  /* 叠加发光：在近黑播放条上呈"光柱"质感 */
  mix-blend-mode: screen;
  filter: drop-shadow(0 -1px 10px rgba(168, 85, 247, 0.28));
  -webkit-mask-image: linear-gradient(90deg, transparent, #000 7%, #000 93%, transparent);
  mask-image: linear-gradient(90deg, transparent, #000 7%, #000 93%, transparent);
}

/* ── Background layer ── */
.gp-bg {
  z-index: 0;
  transition: background 0.8s ease;
}
.gp-body {
  z-index: 1;
}

/* ── Mobile adjustments ── */
@media (max-width: 639px) {
  .gp-row {
    flex-wrap: wrap;
    gap: 0.5rem;
  }
  .gp-left {
    flex: 1 1 auto;
    min-width: 0;
  }
  .gp-center {
    flex: 0 0 auto;
  }
  .gp-right {
    flex: 1 1 auto;
    justify-content: flex-end;
  }
}
</style>

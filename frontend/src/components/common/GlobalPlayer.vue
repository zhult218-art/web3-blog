<template>
  <!-- Global floating player bar at bottom -->
  <Transition name="global-player">
    <div v-if="player.hasTrack && player.playerBarVisible" class="fixed bottom-0 left-0 right-0 z-50 border-t border-white/10 bg-[#06060e]/95 backdrop-blur-2xl px-4 py-2.5">
      <div class="mx-auto max-w-5xl">
        <!-- Progress bar -->
        <div class="flex items-center gap-3 mb-1.5">
          <span class="text-[10px] text-gray-500 font-mono w-10 text-right">{{ player.formatTime(player.currentTime) }}</span>
          <div class="flex-1 h-1.5 rounded-full bg-white/10 cursor-pointer group relative" ref="progressBar"
            @click="onProgressClick">
            <div class="h-full rounded-full bg-gradient-to-r from-purple-500 to-cyan-400 transition-all duration-150"
              :style="{ width: progressPercent + '%' }"></div>
            <div class="absolute top-1/2 -translate-y-1/2 w-3 h-3 rounded-full bg-white shadow-[0_0_8px_rgba(34,211,238,0.8)] opacity-0 group-hover:opacity-100 transition-opacity"
              :style="{ left: 'calc(' + progressPercent + '% - 6px)' }"></div>
          </div>
          <span class="text-[10px] text-gray-500 font-mono w-10">{{ player.formatTime(player.duration || player.currentTrack?.duration) }}</span>
        </div>

        <div class="flex items-center gap-3">
          <!-- Track info -->
          <div class="flex items-center gap-3 flex-1 min-w-0 cursor-pointer" @click="showPlaylist = !showPlaylist">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-purple-500/30 to-cyan-500/30 flex items-center justify-center text-lg flex-shrink-0"
              :class="{ 'animate-pulse-slow': player.isPlaying }">
              {{ player.isPlaying ? '🎵' : '⏸' }}
            </div>
            <div class="min-w-0">
              <p class="text-sm font-semibold text-white truncate">{{ player.currentTrack?.title }}</p>
              <p class="text-[11px] text-gray-500 truncate">{{ player.currentTrack?.artist || '未知艺术家' }}</p>
            </div>
          </div>

          <!-- Controls -->
          <div class="flex items-center gap-2 sm:gap-3 flex-shrink-0">
            <button class="text-gray-400 hover:text-white transition text-sm hidden sm:block" @click="player.prev" title="上一首">⏮</button>
            <button class="w-9 h-9 rounded-full bg-gradient-to-r from-purple-500 to-cyan-500 flex items-center justify-center text-white shadow-lg shadow-purple-500/25 hover:shadow-purple-500/40 transition-all hover:scale-105"
              @click="player.toggle">
              <svg v-if="!player.isPlaying" class="w-4 h-4 ml-0.5" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
              <svg v-else class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z"/></svg>
            </button>
            <button class="text-gray-400 hover:text-white transition text-sm hidden sm:block" @click="player.next" title="下一首">⏭</button>

            <!-- Lyrics toggle -->
            <button class="text-gray-400 hover:text-white transition text-xs flex-shrink-0 px-1.5" @click="showLyrics = !showLyrics"
              :class="{ 'text-purple-300': showLyrics }" :title="showLyrics ? '收起歌词' : '显示歌词'">歌词</button>

            <!-- Play mode -->
            <button class="text-gray-400 hover:text-white transition text-xs flex-shrink-0" @click="cycleMode" :title="modeTitle">
              <span v-if="player.playMode === 'order'" class="text-base">🔁</span>
              <span v-else-if="player.playMode === 'shuffle'" class="text-base">🔀</span>
              <span v-else class="text-base">🔂</span>
            </button>
          </div>

          <!-- Volume + close -->
          <div class="hidden sm:flex items-center gap-2 flex-shrink-0">
            <span class="text-[10px] text-gray-600">VOL</span>
            <input type="range" min="0" max="100" :value="Math.round(player.volume * 100)"
              @input="player.setVolume($event.target.value / 100)"
              class="w-16 h-1 accent-purple-500 cursor-pointer" />
          </div>
          <button class="text-gray-500 hover:text-white transition text-xs flex-shrink-0 ml-1" @click="player.hideBar" title="收起播放器">✕</button>
        </div>

        <!-- Playlist dropdown -->
        <div v-if="showPlaylist && player.playlist.length" class="absolute bottom-full left-0 right-0 mb-2 mx-auto max-w-5xl bg-[#0a0a18]/95 backdrop-blur-xl border border-white/10 rounded-xl p-3 max-h-64 overflow-y-auto shadow-2xl">
          <p class="text-[10px] text-gray-500 uppercase tracking-wider mb-2 px-2">播放列表 ({{ player.playlist.length }})</p>
          <div v-for="(t, i) in player.playlist" :key="t.id"
            class="flex items-center gap-3 px-2 py-1.5 rounded-lg cursor-pointer transition"
            :class="player.currentTrack?.id === t.id ? 'bg-purple-500/10 text-cyan-300' : 'hover:bg-[#121230] text-white/80'"
            @click="player.play(t)">
            <span class="text-[10px] text-gray-600 font-mono w-4">{{ i + 1 }}</span>
            <span class="flex-1 text-xs truncate">{{ t.title }}</span>
            <span class="text-[10px] text-gray-600 font-mono">{{ player.formatTime(t.duration) }}</span>
          </div>
        </div>
      <!-- Lyrics popup -->
        <div v-if="showLyrics" class="absolute bottom-full left-0 right-0 mb-2 mx-auto max-w-5xl bg-[#0a0a18]/95 backdrop-blur-xl border border-white/10 rounded-xl p-4 shadow-2xl">
          <p class="text-[10px] text-gray-500 uppercase tracking-wider mb-2 px-2 flex items-center justify-between">
            <span>歌词 · {{ player.currentTrack?.title }}</span>
            <span>{{ player.lyricsLoading ? '加载中...' : (player.lyrics.length ? player.lyrics.length + ' 行' : '暂无歌词') }}</span>
          </p>
          <div class="max-h-56 overflow-y-auto lyrics-scroll pr-1" ref="lyricsBox">
            <template v-if="player.lyrics.length">
              <p v-for="(line, i) in player.lyrics" :key="i"
                class="px-2 py-1 text-sm transition-all duration-300 leading-relaxed cursor-pointer"
                :class="i === player.currentLyricIndex
                  ? 'text-purple-300 font-semibold scale-105 bg-purple-500/10 rounded-lg'
                  : 'text-gray-500 hover:text-gray-300'"
                @click="player.seek(line.time)">
                {{ line.text }}
              </p>
            </template>
            <p v-else class="px-2 py-8 text-center text-xs text-gray-600">暂无歌词，静心聆听</p>
          </div>
        </div>
      </div>

      <!-- 底部柱状流动条：全站任何页面都可见，不拦截点击 -->
      <div class="global-bars" aria-hidden="true">
        <AudioBars :height="24" :bars="72" />
      </div>
    </div>
  </Transition>
</template>

<script setup>
// ============================================================
// 全局悬浮播放器（GlobalPlayer）
// 固定在页面底部，由 player store 驱动：
// 进度条、上/下一首、播放/暂停、播放模式、音量、曲目列表
// 由 App.vue 全局挂载，任何页面播放音乐都会出现
// ============================================================
import { computed, ref, watch } from 'vue'
import { usePlayerStore } from '@/stores/modules/player'
import AudioBars from './AudioBars.vue'

const player = usePlayerStore()
const progressBar = ref(null)
const showPlaylist = ref(false)
const showLyrics = ref(false)
const lyricsBox = ref(null)

// 播放进度百分比（0~100）
const progressPercent = computed(() => {
  const total = player.duration || player.currentTrack?.duration || 0
  if (!total) return 0
  return Math.min(100, Math.max(0, (player.currentTime / total) * 100))
})

// 播放模式的中文提示（按钮 title）
const modeTitle = computed(() => {
  const map = { order: '顺序播放', shuffle: '随机播放', 'loop-one': '单曲循环' }
  return map[player.playMode]
})

// 播放模式循环顺序
const MODES = ['order', 'shuffle', 'loop-one']

// 循环切换播放模式
function cycleMode() {
  const idx = MODES.indexOf(player.playMode)
  player.setMode(MODES[(idx + 1) % MODES.length])
}

// 点击进度条跳转播放进度
function onProgressClick(e) {
  const el = progressBar.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const pct = ((e.clientX - rect.left) / rect.width) * 100
  player.seekByPercent(pct)
}

// 切换曲目时自动收起播放列表
watch(() => player.currentTrack, () => {
  showPlaylist.value = false
})

// 歌词高亮行自动滚动到可视区
watch(() => player.currentLyricIndex, (idx) => {
  const box = lyricsBox.value
  if (!box || idx < 0) return
  const els = box.querySelectorAll('p')
  const active = els[idx]
  if (active) {
    const top = active.offsetTop - box.clientHeight / 2 + active.clientHeight / 2
    box.scrollTo({ top, behavior: 'smooth' })
  }
})
</script>

<style scoped>
.global-player-enter-active,
.global-player-leave-active { transition: all 0.35s cubic-bezier(0.23, 1, 0.32, 1); }
.global-player-enter-from,
.global-player-leave-to { opacity: 0; transform: translateY(100%); }
@keyframes pulse-slow {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.85; transform: scale(1.03); }
}
.animate-pulse-slow { animation: pulse-slow 3s ease-in-out infinite; }

/* 底部柱状流动条：贴齐底边、两侧渐隐，不拦截点击 */
.global-bars {
  pointer-events: none;
  margin: 2px -16px -10px;
  padding: 0 16px;
  opacity: 0.9;
  -webkit-mask-image: linear-gradient(90deg, transparent, #000 8%, #000 92%, transparent);
  mask-image: linear-gradient(90deg, transparent, #000 8%, #000 92%, transparent);
}
</style>
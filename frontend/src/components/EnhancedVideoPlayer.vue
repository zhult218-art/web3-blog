<template>
  <div class="evp-container" :class="{ 'evp-fullscreen': isFs }">
    <div class="evp-video-wrap" ref="wrapRef">
      <video
        ref="videoRef"
        class="evp-video"
        :src="src"
        :poster="poster"
        preload="metadata"
        @timeupdate="onTimeUpdate"
        @loadedmetadata="onLoaded"
        @play="onPlay"
        @pause="onPause"
        @ended="onEnded"
        @click="togglePlay"
      ></video>

      <!-- 字幕层 -->
      <div v-if="subtitlesVisible && currentSubtitle" class="evp-subtitles" @click.stop>
        <span
          v-for="(word, idx) in currentSubtitleWords"
          :key="idx"
          class="evp-sub-word"
          @click="onWordClick(word)"
        >{{ word }} </span>
      </div>

      <!-- 中央播放按钮 -->
      <div v-if="!isPlaying && !showControls" class="evp-center-play" @click="togglePlay">
        <div class="evp-play-icon">▶</div>
      </div>

      <!-- 控制栏 -->
      <div class="evp-controls" :class="{ 'evp-show': showControls }" @click.stop>
        <div class="evp-progress" @click="seekByClick">
          <div class="evp-progress-buffered" :style="{ width: bufferedPct + '%' }"></div>
          <div class="evp-progress-played" :style="{ width: playedPct + '%' }"></div>
          <div class="evp-progress-thumb" :style="{ left: playedPct + '%' }"></div>
          <div v-if="abLoop.a !== null" class="evp-ab-marker a" :style="{ left: abPct(abLoop.a) + '%' }">A</div>
          <div v-if="abLoop.b !== null" class="evp-ab-marker b" :style="{ left: abPct(abLoop.b) + '%' }">B</div>
        </div>

        <div class="evp-controls-row">
          <button class="evp-btn" @click="prevSubtitle" title="上一句">⏮</button>
          <button class="evp-btn" @click="togglePlay">{{ isPlaying ? '⏸' : '▶' }}</button>
          <button class="evp-btn" @click="nextSubtitle" title="下一句">⏭</button>

          <span class="evp-time">{{ fmtTime(currentTime) }} / {{ fmtTime(duration) }}</span>

          <div class="evp-speed">
            <button class="evp-btn" @click="cycleSpeed">{{ playbackRate }}x</button>
          </div>

          <button class="evp-btn" :class="{ active: abLoop.a !== null }" @click="setA" title="设置 A 点">A</button>
          <button class="evp-btn" :class="{ active: abLoop.b !== null }" @click="setB" title="设置 B 点">B</button>
          <button v-if="abLoop.a !== null || abLoop.b !== null" class="evp-btn" @click="clearAB" title="清除循环">⟲</button>

          <button class="evp-btn" @click="toggleSubtitles" :class="{ active: subtitlesVisible }" title="字幕">CC</button>

          <button class="evp-btn" @click="toggleFullscreen" title="全屏">⛶</button>
        </div>
      </div>
    </div>

    <!-- 查词弹窗 -->
    <div v-if="wordPopup.visible" class="evp-word-popup" :style="{ top: wordPopup.y + 'px', left: wordPopup.x + 'px' }">
      <div class="evp-word-header">
        <span class="evp-word-text">{{ wordPopup.word }}</span>
        <button class="evp-word-close" @click="wordPopup.visible = false">✕</button>
      </div>
      <div v-if="wordPopup.loading" class="evp-word-loading">查询中...</div>
      <div v-else-if="wordPopup.result" class="evp-word-result">
        <div v-if="wordPopup.result.phonetic" class="evp-phonetic">/{{ wordPopup.result.phonetic }}/</div>
        <div v-for="(d, i) in wordPopup.result.definitions" :key="i" class="evp-def">
          <span class="evp-pos">{{ d.partOfSpeech }}</span>
          <span>{{ d.definition }}</span>
        </div>
        <div v-if="wordPopup.result.definitions?.length === 0" class="evp-no-result">未找到释义</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  src: { type: String, required: true },
  poster: { type: String, default: '' },
  videoId: { type: [String, Number], default: '' },
  subtitleUrl: { type: String, default: '' },
  subtitleLang: { type: String, default: 'zh' }, // zh / en / both
})

const emit = defineEmits(['play', 'pause', 'ended'])

const videoRef = ref(null)
const wrapRef = ref(null)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const bufferedPct = ref(0)
const playbackRate = ref(1)
const isFs = ref(false)
const showControls = ref(true)
const subtitlesVisible = ref(true)
const subtitles = ref([]) // [{ start, end, text }]
const currentSubtitle = ref(null)
const wordPopup = ref({ visible: false, word: '', x: 0, y: 0, loading: false, result: null })
const abLoop = ref({ a: null, b: null })

let hideTimer = null
let abLoopTimer = null

const playedPct = computed(() => duration.value ? (currentTime.value / duration.value) * 100 : 0)
const currentSubtitleWords = computed(() => {
  if (!currentSubtitle.value) return []
  return currentSubtitle.value.text.split(/\s+/)
})

// ===== 播放控制 =====
function togglePlay() {
  const v = videoRef.value
  if (!v) return
  if (v.paused) v.play()
  else v.pause()
}
function onPlay() { isPlaying.value = true; emit('play'); scheduleHide() }
function onPause() { isPlaying.value = false; showControls.value = true }
function onEnded() { isPlaying.value = false; emit('ended'); saveProgress() }
function onTimeUpdate() {
  currentTime.value = videoRef.value.currentTime
  // 更新当前字幕
  updateCurrentSubtitle()
  // AB 循环
  if (abLoop.value.a !== null && abLoop.value.b !== null) {
    if (currentTime.value >= abLoop.value.b) {
      videoRef.value.currentTime = abLoop.value.a
    }
  }
  // 缓冲
  if (videoRef.value.buffered.length) {
    bufferedPct.value = (videoRef.value.buffered.end(videoRef.value.buffered.length - 1) / duration.value) * 100
  }
}
function onLoaded() {
  duration.value = videoRef.value.duration
  loadProgress()
}
function seekByClick(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  const pct = (e.clientX - rect.left) / rect.width
  videoRef.value.currentTime = pct * duration.value
}

// ===== 倍速 =====
const speeds = [0.5, 0.75, 1, 1.25, 1.5, 2]
function cycleSpeed() {
  const i = speeds.indexOf(playbackRate.value)
  playbackRate.value = speeds[(i + 1) % speeds.length]
  videoRef.value.playbackRate = playbackRate.value
}

// ===== 字幕（解析 srt/vtt）=====
function parseSRT(text) {
  const blocks = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split(/\n\n+/)
  const subs = []
  for (const block of blocks) {
    const lines = block.trim().split('\n')
    if (lines.length < 2) continue
    const timeLine = lines.find(l => l.includes('-->'))
    if (!timeLine) continue
    const [startStr, endStr] = timeLine.split('-->').map(s => s.trim())
    const text = lines.slice(lines.indexOf(timeLine) + 1).join(' ')
    subs.push({ start: parseTime(startStr), end: parseTime(endStr), text })
  }
  return subs
}
function parseTime(s) {
  // 00:00:00,000 或 00:00:00.000
  const m = s.match(/(\d+):(\d+):(\d+)[,.](\d+)/)
  if (!m) return 0
  return (+m[1]) * 3600 + (+m[2]) * 60 + (+m[3]) + (+m[4]) / 1000
}
function updateCurrentSubtitle() {
  const t = currentTime.value
  const sub = subtitles.value.find(s => t >= s.start && t <= s.end)
  currentSubtitle.value = sub || null
}
async function loadSubtitles() {
  if (!props.subtitleUrl) return
  try {
    const res = await fetch(props.subtitleUrl)
    const text = await res.text()
    subtitles.value = parseSRT(text)
  } catch { subtitles.value = [] }
}
function prevSubtitle() {
  const idx = subtitles.value.findIndex(s => s.start >= currentTime.value)
  if (idx > 0) videoRef.value.currentTime = subtitles.value[idx - 1].start
  else if (idx === 0) videoRef.value.currentTime = subtitles.value[0].start
}
function nextSubtitle() {
  const sub = subtitles.value.find(s => s.start > currentTime.value)
  if (sub) videoRef.value.currentTime = sub.start
}
function toggleSubtitles() { subtitlesVisible.value = !subtitlesVisible.value }

// ===== AB 循环 =====
function setA() {
  abLoop.value.a = currentTime.value
  if (abLoop.value.b !== null && abLoop.value.b <= abLoop.value.a) abLoop.value.b = null
}
function setB() {
  if (abLoop.value.a === null) { setA(); return }
  if (currentTime.value <= abLoop.value.a) return
  abLoop.value.b = currentTime.value
}
function clearAB() { abLoop.value = { a: null, b: null } }
function abPct(t) { return duration.value ? (t / duration.value) * 100 : 0 }

// ===== 全屏 =====
function toggleFullscreen() {
  const el = wrapRef.value
  if (!document.fullscreenElement) el.requestFullscreen?.()
  else document.exitFullscreen?.()
}
function onFsChange() { isFs.value = !!document.fullscreenElement }

// ===== 播放进度记忆 =====
const storageKey = computed(() => `evp_progress_${props.videoId || props.src}`)
function saveProgress() {
  if (!props.videoId && !props.src) return
  try { localStorage.setItem(storageKey.value, String(currentTime.value)) } catch {}
}
function loadProgress() {
  try {
    const t = localStorage.getItem(storageKey.value)
    if (t && +t > 5 && +t < duration.value - 5) {
      videoRef.value.currentTime = +t
    }
  } catch {}
}

// ===== 查词（DashPlayer 风格：点击字幕单词查询）=====
async function onWordClick(word) {
  word = word.replace(/[^a-zA-Z'-]/g, '')
  if (word.length < 2) return
  const rect = wrapRef.value.getBoundingClientRect()
  wordPopup.value = {
    visible: true, word, x: rect.left + 20, y: rect.top + 20,
    loading: true, result: null,
  }
  try {
    const res = await fetch(`https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(word)}`)
    const data = await res.json()
    if (Array.isArray(data) && data[0]) {
      const entry = data[0]
      const definitions = []
      for (const m of entry.meanings || []) {
        for (const d of m.definitions || []) {
          definitions.push({ partOfSpeech: m.partOfSpeech, definition: d.definition })
        }
      }
      wordPopup.value.result = { phonetic: entry.phonetic, definitions }
    } else {
      wordPopup.value.result = { definitions: [] }
    }
  } catch {
    wordPopup.value.result = { definitions: [] }
  }
  wordPopup.value.loading = false
}

// ===== 控制栏自动隐藏 =====
function scheduleHide() {
  if (hideTimer) clearTimeout(hideTimer)
  hideTimer = setTimeout(() => { if (isPlaying.value) showControls.value = false }, 3000)
}
function onMouseMove() {
  showControls.value = true
  scheduleHide()
}

function fmtTime(s) {
  if (!s || isNaN(s)) return '00:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

watch(() => props.subtitleUrl, loadSubtitles)

onMounted(() => {
  loadSubtitles()
  document.addEventListener('fullscreenchange', onFsChange)
  wrapRef.value?.addEventListener('mousemove', onMouseMove)
})
onBeforeUnmount(() => {
  saveProgress()
  document.removeEventListener('fullscreenchange', onFsChange)
  wrapRef.value?.removeEventListener('mousemove', onMouseMove)
  if (hideTimer) clearTimeout(hideTimer)
})
</script>

<style scoped>
.evp-container { width: 100%; background: #000; border-radius: 12px; overflow: hidden; position: relative; }
.evp-video-wrap { position: relative; width: 100%; aspect-ratio: 16/9; background: #000; }
.evp-video { width: 100%; height: 100%; display: block; cursor: pointer; }

/* 字幕 */
.evp-subtitles {
  position: absolute; bottom: 60px; left: 50%; transform: translateX(-50%);
  max-width: 80%; text-align: center; pointer-events: auto;
}
.evp-sub-word {
  font-size: 18px; color: #fff; text-shadow: 0 1px 4px rgba(0,0,0,0.9);
  cursor: pointer; padding: 0 1px; border-radius: 3px; transition: background 0.15s;
}
.evp-sub-word:hover { background: rgba(34, 211, 238, 0.4); }

/* 中央播放 */
.evp-center-play {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  cursor: pointer;
}
.evp-play-icon {
  width: 64px; height: 64px; border-radius: 50%;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: #fff; backdrop-filter: blur(8px);
}

/* 控制栏 */
.evp-controls {
  position: absolute; bottom: 0; left: 0; right: 0; padding: 8px 12px;
  background: linear-gradient(transparent, rgba(0,0,0,0.8));
  opacity: 0; transition: opacity 0.3s; pointer-events: none;
}
.evp-controls.evp-show { opacity: 1; pointer-events: auto; }
.evp-progress {
  position: relative; height: 4px; background: rgba(255,255,255,0.2); border-radius: 2px;
  cursor: pointer; margin-bottom: 8px;
}
.evp-progress-buffered { position: absolute; height: 100%; background: rgba(255,255,255,0.3); border-radius: 2px; }
.evp-progress-played { position: absolute; height: 100%; background: #22d3ee; border-radius: 2px; }
.evp-progress-thumb {
  position: absolute; top: 50%; transform: translate(-50%, -50%);
  width: 12px; height: 12px; border-radius: 50%; background: #22d3ee; opacity: 0; transition: opacity 0.2s;
}
.evp-progress:hover .evp-progress-thumb { opacity: 1; }
.evp-ab-marker {
  position: absolute; top: -4px; transform: translateX(-50%);
  width: 14px; height: 12px; font-size: 9px; color: #fff;
  display: flex; align-items: center; justify-content: center; border-radius: 3px;
}
.evp-ab-marker.a { background: #f59e0b; }
.evp-ab-marker.b { background: #ef4444; }

.evp-controls-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.evp-btn {
  background: none; border: none; color: #fff; cursor: pointer; padding: 4px 8px;
  border-radius: 6px; font-size: 14px; transition: background 0.15s;
}
.evp-btn:hover { background: rgba(255,255,255,0.15); }
.evp-btn.active { background: rgba(34, 211, 238, 0.3); color: #22d3ee; }
.evp-time { color: rgba(255,255,255,0.8); font-size: 12px; font-family: monospace; margin: 0 4px; }
.evp-speed select { background: rgba(255,255,255,0.1); color: #fff; border: none; border-radius: 4px; padding: 2px 4px; }

/* 查词弹窗 */
.evp-word-popup {
  position: fixed; z-index: 10000; min-width: 220px; max-width: 320px;
  background: rgba(15, 20, 40, 0.95); backdrop-filter: blur(12px);
  border: 1px solid rgba(34, 211, 238, 0.3); border-radius: 12px;
  padding: 12px; box-shadow: 0 8px 32px rgba(0,0,0,0.5);
}
.evp-word-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.evp-word-text { font-size: 16px; font-weight: 700; color: #22d3ee; }
.evp-word-close { background: none; border: none; color: rgba(255,255,255,0.5); cursor: pointer; }
.evp-word-loading { color: rgba(255,255,255,0.6); font-size: 13px; }
.evp-phonetic { color: rgba(255,255,255,0.7); font-size: 12px; margin-bottom: 6px; }
.evp-def { font-size: 13px; color: rgba(255,255,255,0.9); margin-bottom: 4px; }
.evp-pos { color: #a855f7; margin-right: 6px; font-style: italic; }
.evp-no-result { color: rgba(255,255,255,0.5); font-size: 13px; }
</style>

<template>
  <div class="music-page">
    <!-- 极光夜景背景 -->
    <div class="aurora-bg">
      <div class="aurora a1"></div>
      <div class="aurora a2"></div>
      <div class="aurora a3"></div>
      <div class="stars"><span v-for="i in 60" :key="i" class="star" :style="starStyle(i)"></span></div>
    </div>

    <!-- 返回音乐馆 -->
    <button class="back-btn" ref="backBtn" @click="router.push('/media')">
      <span class="bb-arrow">←</span> 幻镜水晶
    </button>

    <!-- 左上标题 -->
    <header class="main-head" ref="mainHead">
      <h1 class="t-fashion" ref="tFashion">AURORA</h1>
      <p class="t-self" ref="tSelf">星屿电台 · 唯美夜航</p>
      <p class="t-year" ref="tYear">— AURORA-朱 · NIGHT RADIO —</p>
    </header>

    <!-- 主体：黑胶 + 可视化 + 播放器 -->
    <main class="main-body">
      <div class="hero-zone" ref="heroZone">
        <div class="hero-marks">
          <span class="hm" style="top: 4%; left: 12%">✦</span>
          <span class="hm" style="top: 14%; right: 8%">✧</span>
          <span class="hm" style="bottom: 12%; left: 6%">✦</span>
          <span class="hm" style="bottom: 6%; right: 18%">✧</span>
        </div>
        <!-- 黑胶唱片：label 内为当前歌曲封面/首字（外层 wrap 承载 GSAP 入场，内层负责旋转） -->
        <div class="hero-vinyl-wrap" ref="heroVinyl">
          <div class="hero-vinyl">
            <div class="vd-grooves"></div>
            <div class="vd-shine"></div>
            <div class="vd-label">
              <img v-if="coverImg" :src="coverImg" class="lp-cover" alt="" />
              <div v-else class="lp-portrait"></div>
              <span class="lp-name">{{ currentInitials }}<br/>VINYL</span>
            </div>
          </div>
        </div>
      </div>

      <div class="wave-wrap" ref="waveWrap">
        <!-- 柱状流动可视化：Web Audio API 实时频谱 -->
        <AudioBars :height="120" :bars="56" />
      </div>

      <!-- 云端曲库条：精选歌单 + 搜索（iTunes 免费接口） -->
      <div class="cloud-bar" ref="cloudBar">
        <button
          v-for="p in presets" :key="p.term"
          class="cloud-chip" :class="{ on: activePreset === p.term }"
          @click="loadPreset(p)"
        >✧ {{ p.label }}</button>
        <span class="cloud-sep"></span>
        <div class="cloud-search">
          <input
            v-model="searchTerm" class="cloud-input" placeholder="搜索云端曲库，如：夜曲 / lofi / 钢琴…"
            @keyup.enter="doSearch"
          />
          <button class="cloud-go" @click="doSearch">搜索</button>
        </div>
      </div>

      <div class="player-wrap" ref="playerWrap">
        <div class="player-caption" ref="playerCaption">
          <span class="pc-dot" :class="{ 'pc-on': playing }"></span>
          {{ playing ? 'NOW PLAYING' : 'PAUSED' }} · {{ sourceLabel }}
        </div>
        <MusicPlayer
          :tracks="tracks"
          :lyrics-map="lyricsMap"
          :show-vinyl="false"
          :initial-id="route.params.id"
          @track-change="onTrackChange"
        />
      </div>
    </main>

    <!-- 花瓣星屑飘落 -->
    <div class="petals" aria-hidden="true">
      <span v-for="i in 24" :key="i" class="petal" :style="petalStyle(i)"></span>
    </div>

    <footer class="main-foot" ref="mainFoot">
      <span>AURORA-朱 · NIGHT RADIO</span>
      <span>45RPM · STEREO · WEB AUDIO</span>
    </footer>
  </div>
</template>

<script setup>
// ====================================================
// 音乐播放页（极光夜景 · 唯美主题）：
// GSAP 开场动画 + 星屑花瓣特效 + AudioWave 波形可视化
// 曲库来源：站点曲库（media-service）+ iTunes 免费云端歌单
// ====================================================
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import gsap from 'gsap'
import MusicPlayer from './components/MusicPlayer.vue'
import AudioBars from '@/components/common/AudioBars.vue'
import { getMusicList } from '@/api/media'
import { ITUNES_PRESET, searchItunes } from '@/api/itunes'
import { useSupabase } from '@/composables/useSupabase'
import { usePlayerStore } from '@/stores/modules/player'
import { fixNeteaseId } from '@/config/musicFixes'
import { DEMO_TRACKS, DEMO_LYRICS } from './lyricsData'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()

const presets = ITUNES_PRESET
const tracks = ref([])
const lyricsMap = ref({ ...DEMO_LYRICS })
const playing = ref(false)
const activePreset = ref('')
const searchTerm = ref('')
const sourceLabel = ref('NIGHT RADIO')

const mainHead = ref(null)
const tFashion = ref(null)
const tSelf = ref(null)
const tYear = ref(null)
const heroZone = ref(null)
const heroVinyl = ref(null)
const waveWrap = ref(null)
const cloudBar = ref(null)
const playerWrap = ref(null)
const playerCaption = ref(null)
const mainFoot = ref(null)
const backBtn = ref(null)

// 根据路由 id 匹配当前播放歌曲
const currentTrack = computed(() => tracks.value.find(t => String(t.id) === String(route.params.id)) || null)
// 当前歌曲封面图
const coverImg = computed(() => currentTrack.value?.cover || '')
// 封面占位首字母（无封面时显示歌曲名首字母）
const currentInitials = computed(() => (currentTrack.value?.title || 'M').slice(0, 1).toUpperCase())

// 歌曲切换时同步页面状态
function onTrackChange(track) {
  playing.value = !!track
}

// 把云端/曲库曲目并入播放列表（按 id 去重）
function mergeTracks(list, source) {
  const seen = new Set(tracks.value.map(t => t.id))
  const fresh = list.filter(t => !seen.has(t.id))
  tracks.value = [...tracks.value, ...fresh]
  if (source) sourceLabel.value = source
}

// 加载精选歌单
async function loadPreset(p) {
  activePreset.value = p.term
  try {
    const list = await searchItunes(p.term, 12)
    if (list.length) mergeTracks(list, `CLOUD · ${p.label}`)
  } catch { /* 云端不可用时保持曲库 */ }
}

// 云端搜索
async function doSearch() {
  const kw = searchTerm.value.trim()
  if (!kw) return
  activePreset.value = ''
  try {
    const list = await searchItunes(kw, 16)
    if (list.length) mergeTracks(list, `CLOUD · “${kw}”`)
  } catch { /* 忽略 */ }
}

// 星星随机样式
function starStyle(i) {
  const seed = i * 131
  return {
    left: (seed % 100) + '%',
    top: (seed % 62) + '%',
    '--size': (1 + (seed % 2)) + 'px',
    '--tw-dur': (2 + seed % 4) + 's',
    '--tw-delay': (seed % 5) + 's',
  }
}

// 花瓣星屑飘落的随机样式（位置/摇摆/时长）
function petalStyle(i) {
  const seed = i * 67
  const sway = 10 + (seed % 26)
  return {
    left: (seed % 96 + 2) + '%',
    top: -(30 + (seed % 70)) + 'px',
    '--size': (10 + (seed % 14)) + 'px',
    '--dur': (9 + (seed % 9)) + 's',
    '--delay': (seed % 10) + 's',
    '--sway': sway + 'px',
    '--rot': (seed % 360) + 'deg',
    '--hue1': 280 + (seed % 3) * 26, // 粉紫→蓝紫系
    '--hue2': 200 + (seed % 4) * 22,
    filter: 'blur(' + (seed % 2) + 'px)',
    zIndex: (seed % 3) * 2,
  }
}

/* 入场动画：黑胶浮现 → 标题 → 可视化 → 曲库条 → 播放器 */
function runIntro() {
  const tl = gsap.timeline()
  tl.fromTo(backBtn.value, { autoAlpha: 0, y: -10 }, { autoAlpha: 1, y: 0, duration: 0.5 }, 0)
    .fromTo(heroVinyl.value, { scale: 0.3, autoAlpha: 0, rotate: -120 }, { scale: 1, autoAlpha: 1, rotate: 0, duration: 1.15, ease: 'power3.out' }, 0.1)
    .fromTo(heroZone.value, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.6 }, 0.1)
    .fromTo(tFashion.value, { y: 34, autoAlpha: 0, skewX: 6 }, { y: 0, autoAlpha: 1, skewX: 0, duration: 0.8, ease: 'power3.out' }, 0.45)
    .fromTo(tSelf.value, { y: 18, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.6, ease: 'power2.out' }, 0.58)
    .fromTo(tYear.value, { y: 14, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.5, ease: 'power2.out' }, 0.68)
    .fromTo(waveWrap.value, { y: 24, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.7, ease: 'power2.out' }, 0.72)
    .fromTo(cloudBar.value, { y: 20, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.6, ease: 'power2.out' }, 0.82)
    .fromTo(playerWrap.value, { y: 44, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.85, ease: 'power3.out' }, 0.9)
    .fromTo(playerCaption.value, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.5 }, 1.02)
    .fromTo(mainFoot.value, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.6 }, 1.08)
}

// 挂载时播放开场动画并加载曲目：
// 优先复用全局播放上下文 → Supabase 本地曲库（与音乐馆同源）→ 站点曲库 → 云端精选
onMounted(async () => {
  nextTick(runIntro)

  // 0) 从音乐馆点进来时：直接复用全局播放列表，保证听到的歌与显示的歌是同一首
  if (player.currentTrack && String(player.currentTrack.id) === String(route.params.id)) {
    mergeTracks(player.playlist.map(t => ({ ...t })), 'LIBRARY')
  }

  // 1) Supabase 本地曲库（与音乐馆 loadLocalMusic 完全同源，id 才能对得上）
  try {
    const { fetchMusic } = useSupabase()
    const { data, error } = await fetchMusic(100)
    if (!error && data?.length) {
      mergeTracks(data.map(t => ({
        ...t,
        id: t.id,
        neteaseId: fixNeteaseId(t),
        duration: t.duration_seconds || 0,
        url: t.audio_url || '',
        cover: t.cover_url || '',
        artist: t.artist || '未知艺术家',
        album: t.album || '',
        source: 'supabase',
      })), 'LIBRARY')
    }
  } catch { /* Supabase 不可用时继续 */ }

  // 2) 站点曲库（media-service）
  try {
    const res = await getMusicList({ page: 1, size: 100 })
    const data = res.data?.records || res.data || []
    if (data.length) {
      mergeTracks(data.map((t, i) => ({
        ...t,
        id: t.id || t.musicId || i + 1,
        neteaseId: t.netease_id || t.neteaseId || '',
        duration: Number(t.duration) || 240,
        lyricsKey: t.title || '',
      })), 'LIBRARY')
    }
  } catch { /* 曲库服务未启动时走云端 */ }

  // 3) 默认并入第一个精选云端歌单（iTunes 免费，直接可播）
  await loadPreset(presets[0])

  // 4) 仍然为空（曲库 + 云端都失败）→ 演示曲目兜底
  if (!tracks.value.length) {
    tracks.value = DEMO_TRACKS.map(t => ({ ...t }))
    sourceLabel.value = 'DEMO'
  }

  // 当前曲目 id 不存在时回退第一批
  if (!tracks.value.find(t => String(t.id) === String(route.params.id))) {
    if (route.params.id) {
      tracks.value.unshift({ id: route.params.id, title: '未收录曲目', artist: '未知', duration: 240, url: '' })
    }
  }
})

onBeforeUnmount(() => {})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&display=swap');

.music-page {
  position: relative;
  min-height: 100vh;
  margin: -1px;
  overflow-y: auto;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #05060f;
  color: rgba(226, 232, 248, 0.92);
}

/* ============ 极光夜景背景 ============ */
.aurora-bg {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(ellipse 120% 60% at 50% -10%, rgba(59, 130, 246, 0.12), transparent 60%),
    linear-gradient(180deg, #060714 0%, #0a0b1e 55%, #05060f 100%);
  overflow: hidden;
  pointer-events: none;
}

/* 三条极光彩带：青 / 紫粉 / 蓝，大半径模糊 + 缓慢摆动 */
.aurora {
  position: absolute;
  width: 70vw;
  height: 34vh;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.35;
  mix-blend-mode: screen;
  animation: auroraSway 16s ease-in-out infinite alternate;
}
.aurora.a1 { top: -6%; left: -12%; background: radial-gradient(ellipse, rgba(52, 211, 153, 0.5), transparent 65%); }
.aurora.a2 { top: -2%; right: -14%; background: radial-gradient(ellipse, rgba(217, 70, 239, 0.42), transparent 65%); animation-delay: -6s; }
.aurora.a3 { top: 14%; left: 26%; background: radial-gradient(ellipse, rgba(56, 130, 246, 0.4), transparent 68%); animation-delay: -11s; }

@keyframes auroraSway {
  from { transform: translateX(-4%) rotate(-4deg) scaleY(0.9); }
  to { transform: translateX(5%) rotate(5deg) scaleY(1.12); }
}

/* 星星闪烁 */
.stars { position: absolute; inset: 0; }
.star {
  position: absolute;
  width: var(--size);
  height: var(--size);
  border-radius: 50%;
  background: #dbeafe;
  opacity: 0.7;
  animation: twinkle var(--tw-dur) ease-in-out var(--tw-delay) infinite;
}
@keyframes twinkle {
  0%, 100% { opacity: 0.15; transform: scale(0.8); }
  50% { opacity: 0.85; transform: scale(1.15); }
}

/* ============ 返回 ============ */
.back-btn {
  position: absolute;
  top: 1.6rem;
  right: 2.2rem;
  z-index: 6;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.5rem 1.1rem;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(10, 12, 32, 0.55);
  backdrop-filter: blur(10px);
  color: rgba(203, 213, 225, 0.9);
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  cursor: pointer;
  transition: all 0.25s;
}

.back-btn:hover {
  background: rgba(30, 34, 68, 0.8);
  border-color: rgba(103, 232, 249, 0.5);
  color: #a5f3fc;
  transform: translateY(-1px);
}

.bb-arrow {
  font-size: 0.95rem;
  line-height: 1;
}

/* ============ 标题 ============ */
.main-head {
  position: relative;
  z-index: 3;
  padding: 2.6rem 3.4rem 0;
}

.t-fashion {
  margin: 0;
  font-size: clamp(2.6rem, 5.6vw, 4.4rem);
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.02;
  background: linear-gradient(100deg, #a5f3fc 0%, #c4b5fd 40%, #f0abfc 75%, #6ee7b7 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 0 24px rgba(168, 85, 247, 0.35));
}

.t-self {
  margin: 0.5rem 0 0;
  font-size: clamp(0.68rem, 1.3vw, 0.9rem);
  font-weight: 600;
  letter-spacing: 0.5em;
  color: rgba(196, 181, 253, 0.75);
}

.t-year {
  margin: 0.5rem 0 0;
  font-family: 'Courier New', monospace;
  font-size: 0.72rem;
  letter-spacing: 0.42em;
  color: rgba(148, 163, 184, 0.5);
}

/* ============ 主体 ============ */
.main-body {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.6rem;
  padding: 1.4rem 3.4rem 1.6rem;
  min-height: calc(100vh - 190px);
}

.hero-zone {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: clamp(220px, 32vh, 300px);
}

/* 黑胶（自转） */
.hero-vinyl-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-vinyl {
  position: relative;
  width: clamp(210px, 28vh, 300px);
  height: clamp(210px, 28vh, 300px);
  border-radius: 50%;
  background: radial-gradient(circle at 50% 50%, #1c1c2c 0%, #0d0d1c 55%, #05050e 100%);
  box-shadow:
    inset 0 0 0 4px rgba(148, 163, 184, 0.08),
    inset 0 0 30px rgba(0, 0, 0, 0.95),
    0 30px 90px rgba(124, 58, 237, 0.28),
    0 10px 40px rgba(34, 211, 238, 0.14);
  animation: vinylSpin 2s linear infinite;
}

@keyframes vinylSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.vd-grooves {
  position: absolute;
  inset: 6%;
  border-radius: 50%;
  background: repeating-radial-gradient(circle at 50% 50%, transparent 0 2px, rgba(165, 180, 252, 0.05) 2px 3px);
  mask-image: radial-gradient(circle at 50% 50%, #000 10%, transparent 95%);
}

.vd-shine {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(165, 243, 252, 0.12) 0%, transparent 32%, transparent 62%, rgba(240, 171, 252, 0.07) 84%);
}

.vd-label {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 44%;
  height: 44%;
  border-radius: 50%;
  border: 1px solid rgba(165, 243, 252, 0.35);
  background: linear-gradient(140deg, #1e1b4b, #312e81);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 0 5px rgba(148, 163, 184, 0.1), inset 0 0 14px rgba(124, 58, 237, 0.4);
  overflow: hidden;
}

.lp-cover {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.lp-portrait {
  width: 58%;
  height: 58%;
  border-radius: 50% 50% 46% 46%;
  background:
    radial-gradient(ellipse 50% 28% at 50% 26%, rgba(196, 181, 253, 0.85) 0%, transparent 70%),
    radial-gradient(ellipse 100% 90% at 50% 82%, rgba(103, 232, 249, 0.55) 0%, transparent 75%);
  opacity: 0.85;
}

.lp-name {
  position: relative;
  margin-top: 0.35rem;
  font-size: 0.55rem;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-align: center;
  color: rgba(199, 210, 254, 0.85);
  font-family: 'Courier New', monospace;
  line-height: 1.4;
}

.hero-marks {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hm {
  position: absolute;
  font-size: 1.05rem;
  color: rgba(165, 243, 252, 0.45);
  text-shadow: 0 0 10px rgba(103, 232, 249, 0.6);
  animation: hmSpin 14s linear infinite;
}

@keyframes hmSpin {
  from { transform: rotate(0deg) scale(1); }
  50% { transform: rotate(180deg) scale(0.7); }
  to { transform: rotate(360deg) scale(1); }
}

/* 可视化区 */
.wave-wrap {
  width: min(880px, 100%);
  border-radius: 14px;
  box-shadow: 0 10px 44px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(148, 163, 184, 0.12);
}

/* ============ 云端曲库条 ============ */
.cloud-bar {
  width: min(880px, 100%);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.cloud-chip {
  padding: 0.36rem 0.85rem;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(12, 14, 34, 0.6);
  color: rgba(203, 213, 225, 0.8);
  font-size: 0.72rem;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: all 0.25s;
}
.cloud-chip:hover { border-color: rgba(103, 232, 249, 0.5); color: #a5f3fc; transform: translateY(-1px); }
.cloud-chip.on {
  border-color: rgba(168, 85, 247, 0.65);
  color: #e9d5ff;
  background: rgba(88, 28, 135, 0.35);
  box-shadow: 0 0 14px rgba(168, 85, 247, 0.3);
}

.cloud-sep { width: 1px; height: 18px; background: rgba(148, 163, 184, 0.2); }

.cloud-search { display: flex; align-items: center; gap: 0.45rem; margin-left: auto; }
.cloud-input {
  width: 230px;
  padding: 0.4rem 0.8rem;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(12, 14, 34, 0.6);
  color: rgba(226, 232, 248, 0.92);
  font-size: 0.72rem;
  outline: none;
  transition: border-color 0.25s;
}
.cloud-input::placeholder { color: rgba(148, 163, 184, 0.5); }
.cloud-input:focus { border-color: rgba(103, 232, 249, 0.55); }
.cloud-go {
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  border: 1px solid rgba(103, 232, 249, 0.4);
  background: rgba(8, 51, 68, 0.45);
  color: #a5f3fc;
  font-size: 0.72rem;
  cursor: pointer;
  transition: all 0.25s;
}
.cloud-go:hover { background: rgba(14, 83, 108, 0.6); box-shadow: 0 0 12px rgba(34, 211, 238, 0.3); }

.player-wrap {
  width: min(720px, 100%);
  min-width: 0;
}

.player-caption {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  font-size: 0.62rem;
  letter-spacing: 0.34em;
  font-family: 'Courier New', monospace;
  color: rgba(165, 180, 252, 0.65);
  margin-bottom: 1.1rem;
}

.pc-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.35);
}

.pc-on {
  background: #6ee7b7;
  box-shadow: 0 0 10px rgba(110, 231, 183, 0.8);
  animation: pcBlink 1.2s ease-in-out infinite;
}

@keyframes pcBlink { 50% { opacity: 0.35; } }

/* ============ 花瓣星屑 ============ */
.petals {
  position: fixed;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
  opacity: 0;
  animation: petalsIn 1.2s ease-out 0.9s forwards;
}

@keyframes petalsIn {
  to { opacity: 1; }
}

.petal {
  position: absolute;
  width: var(--size);
  height: calc(var(--size) * 0.9);
  border-radius: 90% 10% 90% 10%;
  background: linear-gradient(135deg, hsl(var(--hue1), 85%, 72%), hsl(var(--hue2), 80%, 60%));
  box-shadow: 0 0 8px rgba(192, 132, 252, 0.5);
  animation: petalFall var(--dur) linear var(--delay) infinite;
}

@keyframes petalFall {
  0% {
    transform: translate(0, 0) rotate(var(--rot));
    opacity: 0;
  }
  8% { opacity: 0.8; }
  25% {
    transform: translate(calc(var(--sway) * 0.7), 26vh) rotate(calc(var(--rot) + 55deg));
  }
  50% {
    transform: translate(calc(var(--sway) * -0.8), 54vh) rotate(calc(var(--rot) + 130deg));
  }
  75% {
    transform: translate(calc(var(--sway) * 0.5), 82vh) rotate(calc(var(--rot) + 200deg));
  }
  100% {
    transform: translate(calc(var(--sway) * -0.6), 112vh) rotate(calc(var(--rot) + 260deg));
    opacity: 0.85;
  }
}

/* ============ 页脚 ============ */
.main-foot {
  position: relative;
  z-index: 3;
  display: flex;
  justify-content: space-between;
  padding: 1rem 3.4rem 1.8rem;
  font-family: 'Courier New', monospace;
  font-size: 0.6rem;
  letter-spacing: 0.28em;
  color: rgba(148, 163, 184, 0.45);
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}

/* ============ 响应式 ============ */
@media (max-width: 1024px) {
  .main-body {
    gap: 1.2rem;
    padding: 1.2rem 1.6rem;
  }

  .hero-zone { min-height: clamp(180px, 26vh, 230px); }

  .hero-vinyl { width: clamp(170px, 24vh, 220px); height: clamp(170px, 24vh, 220px); }

  .main-head { padding: 1.8rem 1.6rem 0; }

  .main-foot { padding: 0.9rem 1.6rem 1.4rem; }

  .back-btn { top: 1.1rem; right: 1.3rem; }

  .cloud-search { margin-left: 0; width: 100%; }
  .cloud-input { flex: 1; width: auto; }
}
</style>

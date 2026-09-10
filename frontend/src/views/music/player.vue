<template>
  <div class="music-page">
    <!-- 秋日背景 -->
    <div class="autumn-bg">
      <div class="autumn-sun"></div>
      <div class="autumn-street"></div>
      <div class="autumn-grain"></div>
    </div>

    <!-- 返回音乐馆 -->
    <button class="back-btn" ref="backBtn" @click="router.push('/music')">
      <span class="bb-arrow">←</span> 音乐馆
    </button>

    <!-- 左上标题 -->
    <header class="main-head" ref="mainHead">
      <h1 class="t-fashion" ref="tFashion">FASHION</h1>
      <p class="t-self" ref="tSelf">SELF&nbsp;-&nbsp;PORTRAIT</p>
      <p class="t-year" ref="tYear">—&nbsp;1901&nbsp;—</p>
    </header>

    <!-- 主体：黑胶 + 唱片下方播放器 -->
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

      <div class="player-wrap" ref="playerWrap">
        <div class="player-caption" ref="playerCaption">
          <span class="pc-dot" :class="{ 'pc-on': playing }"></span>
          {{ playing ? 'NOW PLAYING' : 'PAUSED' }} · VINYL SESSIONS
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

    <!-- 黄叶飘落 -->
    <div class="leaves" aria-hidden="true">
      <span v-for="i in 26" :key="i" class="leaf" :style="leafStyle(i)"></span>
    </div>

    <footer class="main-foot" ref="mainFoot">
      <span>WEB3 BLOG · AUDIO LAB</span>
      <span>45RPM · STEREO</span>
    </footer>
  </div>
</template>

<script setup>
// ====================================================
// 音乐播放页（秋日主题）：GSAP 开场动画 + 落叶特效，
// 集成 MusicPlayer 组件播放歌曲与歌词
// ====================================================
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import gsap from 'gsap'
import MusicPlayer from './components/MusicPlayer.vue'
import { getMusicList } from '@/api/media'
import { DEMO_TRACKS, DEMO_LYRICS } from './lyricsData'

const route = useRoute()
const router = useRouter()

const tracks = ref([])
const lyricsMap = ref({ ...DEMO_LYRICS })
const playing = ref(false)

const mainHead = ref(null)
const tFashion = ref(null)
const tSelf = ref(null)
const tYear = ref(null)
const heroZone = ref(null)
const heroVinyl = ref(null)
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

// 歌曲切换时同步页面状态（标题/封面等）
function onTrackChange(track) {
  playing.value = !!track
}

// 按索引生成落叶动画的随机样式（位置/摇摆/时长）
function leafStyle(i) {
  const seed = i * 67
  const sway = 10 + (seed % 26)
  return {
    left: (seed % 96 + 2) + '%',
    top: -(30 + (seed % 70)) + 'px',
    '--size': (12 + (seed % 18)) + 'px',
    '--dur': (8 + (seed % 9)) + 's',
    '--delay': (seed % 10) + 's',
    '--sway': sway + 'px',
    '--rot': (seed % 360) + 'deg',
    '--hue1': 38 + (seed % 3) * 8,
    '--hue2': 22 + (seed % 4) * 7,
    filter: 'blur(' + (seed % 2) + 'px)',
    zIndex: (seed % 3) * 2,
  }
}

/* 入场动画：黑胶放大浮现 → 标题 → 播放器 */
// GSAP 开场动画：头部/封面/播放器渐次入场
function runIntro() {
  const tl = gsap.timeline()
  tl.fromTo(backBtn.value, { autoAlpha: 0, y: -10 }, { autoAlpha: 1, y: 0, duration: 0.5 }, 0)
    .fromTo(heroVinyl.value, { scale: 0.3, autoAlpha: 0, rotate: -120 }, { scale: 1, autoAlpha: 1, rotate: 0, duration: 1.15, ease: 'power3.out' }, 0.1)
    .fromTo(heroZone.value, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.6 }, 0.1)
    .fromTo(tFashion.value, { y: 34, autoAlpha: 0, skewX: 6 }, { y: 0, autoAlpha: 1, skewX: 0, duration: 0.8, ease: 'power3.out' }, 0.45)
    .fromTo(tSelf.value, { y: 18, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.6, ease: 'power2.out' }, 0.58)
    .fromTo(tYear.value, { y: 14, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.5, ease: 'power2.out' }, 0.68)
    .fromTo(playerWrap.value, { y: 44, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.85, ease: 'power3.out' }, 0.75)
    .fromTo(playerCaption.value, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.5 }, 0.9)
    .fromTo(mainFoot.value, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.6 }, 0.95)
}

// 挂载时播放开场动画并加载歌曲列表
onMounted(async () => {
  nextTick(runIntro)

  try {
    const res = await getMusicList({ page: 1, size: 100 })
    const data = res.data?.records || res.data || []
    if (data.length) {
      tracks.value = data.map((t, i) => ({
        ...t,
        id: t.id || t.musicId || i + 1,
        duration: Number(t.duration) || 240,
        lyricsKey: t.title || '',
      }))
    } else {
      tracks.value = DEMO_TRACKS.map(t => ({ ...t }))
    }
  } catch {
    tracks.value = DEMO_TRACKS.map(t => ({ ...t }))
  }

  // 当前曲目 id 不存在时回退第一批
  if (!tracks.value.find(t => String(t.id) === String(route.params.id))) {
    tracks.value.unshift({ id: route.params.id || 1, title: '未收录曲目', artist: '未知', duration: 240, url: '' })
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
  overflow: hidden;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #f3e7cf;
  color: rgba(58, 40, 22, 0.92);
}

/* ============ 背景 ============ */
.autumn-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #f6e3c0 0%, #edc995 42%, #d9a86a 100%);
  overflow: hidden;
}

.autumn-sun {
  position: absolute;
  right: -8%;
  top: -10%;
  width: 46vw;
  height: 46vw;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 236, 190, 0.85), rgba(250, 210, 130, 0.35) 55%, transparent 72%);
  filter: blur(6px);
}

.autumn-street {
  position: absolute;
  left: -6%;
  right: -6%;
  bottom: -12%;
  height: 46%;
  background:
    radial-gradient(ellipse 24% 12% at 12% 30%, rgba(96, 62, 32, 0.16), transparent 70%),
    radial-gradient(ellipse 30% 14% at 64% 18%, rgba(96, 62, 32, 0.2), transparent 70%),
    radial-gradient(ellipse 22% 11% at 90% 42%, rgba(96, 62, 32, 0.14), transparent 70%),
    linear-gradient(180deg, rgba(150, 100, 50, 0.18), rgba(110, 68, 30, 0.5));
  border-radius: 50% 50% 0 0;
  filter: blur(2px);
}

.autumn-grain {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(120, 80, 40, 0.07) 1px, transparent 1px);
  background-size: 3px 3px;
  pointer-events: none;
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
  border: 1px solid rgba(70, 48, 26, 0.25);
  background: rgba(250, 240, 220, 0.45);
  backdrop-filter: blur(10px);
  color: rgba(70, 48, 26, 0.85);
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  cursor: pointer;
  transition: all 0.25s;
}

.back-btn:hover {
  background: rgba(250, 240, 220, 0.8);
  border-color: rgba(70, 48, 26, 0.5);
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
  color: rgba(58, 40, 22, 0.92);
}

.t-self {
  margin: 0.5rem 0 0;
  font-size: clamp(0.68rem, 1.3vw, 0.9rem);
  font-weight: 600;
  letter-spacing: 0.5em;
  color: rgba(76, 54, 32, 0.62);
}

.t-year {
  margin: 0.5rem 0 0;
  font-family: 'Courier New', monospace;
  font-size: 0.72rem;
  letter-spacing: 0.42em;
  color: rgba(76, 54, 32, 0.45);
}

/* ============ 主体 ============ */
.main-body {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  padding: 1.4rem 3.4rem 1.6rem;
  min-height: calc(100vh - 190px);
}

.hero-zone {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: clamp(240px, 36vh, 340px);
}

/* 黑胶（自转） */
.hero-vinyl-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-vinyl {
  position: relative;
  width: clamp(230px, 30vh, 330px);
  height: clamp(230px, 30vh, 330px);
  border-radius: 50%;
  background: radial-gradient(circle at 50% 50%, #202020 0%, #101010 55%, #000 100%);
  box-shadow:
    inset 0 0 0 4px rgba(255, 255, 255, 0.05),
    inset 0 0 30px rgba(0, 0, 0, 0.95),
    0 30px 80px rgba(70, 45, 22, 0.4);
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
  background: repeating-radial-gradient(circle at 50% 50%, transparent 0 2px, rgba(255, 255, 255, 0.04) 2px 3px);
  mask-image: radial-gradient(circle at 50% 50%, #000 10%, transparent 95%);
}

.vd-shine {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, transparent 32%, transparent 62%, rgba(255, 255, 255, 0.05) 84%);
}

.vd-label {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 44%;
  height: 44%;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: #e8d9bd;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 0 5px rgba(255, 255, 255, 0.1), inset 0 0 14px rgba(120, 90, 50, 0.35);
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
    radial-gradient(ellipse 50% 28% at 50% 26%, rgba(96, 70, 46, 0.85) 0%, transparent 70%),
    radial-gradient(ellipse 100% 90% at 50% 82%, rgba(120, 88, 56, 0.75) 0%, transparent 75%);
  opacity: 0.85;
}

.lp-name {
  position: relative;
  margin-top: 0.35rem;
  font-size: 0.55rem;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-align: center;
  color: rgba(70, 48, 26, 0.75);
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
  color: rgba(96, 62, 32, 0.4);
  animation: hmSpin 14s linear infinite;
}

@keyframes hmSpin {
  from { transform: rotate(0deg) scale(1); }
  50% { transform: rotate(180deg) scale(0.7); }
  to { transform: rotate(360deg) scale(1); }
}

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
  color: rgba(70, 48, 26, 0.6);
  margin-bottom: 1.1rem;
}

.pc-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(70, 48, 26, 0.35);
}

.pc-on {
  background: #2f9e6e;
  box-shadow: 0 0 10px rgba(47, 158, 110, 0.8);
  animation: pcBlink 1.2s ease-in-out infinite;
}

@keyframes pcBlink { 50% { opacity: 0.35; } }

/* ============ 黄叶 ============ */
.leaves {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
  opacity: 0;
  animation: leavesIn 1.2s ease-out 0.9s forwards;
}

@keyframes leavesIn {
  to { opacity: 1; }
}

.leaf {
  position: absolute;
  width: var(--size);
  height: calc(var(--size) * 0.9);
  border-radius: 90% 10% 90% 10%;
  background: linear-gradient(135deg, hsl(var(--hue1), 80%, 62%), hsl(var(--hue2), 75%, 45%));
  box-shadow: inset -2px -2px 4px rgba(90, 50, 10, 0.25);
  animation: leafFall var(--dur) linear var(--delay) infinite;
}

@keyframes leafFall {
  0% {
    transform: translate(0, 0) rotate(var(--rot));
    opacity: 0;
  }
  8% { opacity: 0.85; }
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
    opacity: 0.9;
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
  color: rgba(70, 48, 26, 0.5);
  border-top: 1px solid rgba(70, 48, 26, 0.14);
}

/* ============ 响应式 ============ */
@media (max-width: 1024px) {
  .main-body {
    gap: 1.2rem;
    padding: 1.2rem 1.6rem;
    min-height: calc(100vh - 150px);
  }

  .hero-zone { min-height: clamp(200px, 30vh, 260px); }

  .hero-vinyl { width: clamp(180px, 26vh, 240px); height: clamp(180px, 26vh, 240px); }

  .main-head { padding: 1.8rem 1.6rem 0; }

  .main-foot { padding: 0.9rem 1.6rem 1.4rem; }

  .back-btn { top: 1.1rem; right: 1.3rem; }

  .music-page { overflow-y: auto; }
}

@media (max-width: 640px) {
  .leaves { display: none; }
}
</style>
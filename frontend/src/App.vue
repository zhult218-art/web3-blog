<template>
  <div id="bg-layer" aria-hidden="true">
    <!-- 视频壁纸：poster 首帧兜底（黑屏防护）；低端机/减少动效偏好 → 静态海报图 -->
    <video
      v-if="bgVideo && canPlayVideo && !videoFailed"
      ref="bgVideoEl"
      :src="bgVideo.video"
      :poster="bgVideo.poster"
      muted loop playsinline autoplay disablepictureinpicture preload="metadata"
      @error="videoFailed = true"
      :style="{ width: '100%', height: '100%', objectFit: 'cover', position: 'absolute', inset: 0 }"
    ></video>
    <img v-else-if="bgVideo" :src="bgVideo.poster" :style="{ width: '100%', height: '100%', objectFit: 'cover', position: 'absolute', inset: 0 }" />
  </div>
  <div class="scroll-progress" :style="{ width: scrollProgress + '%' }"></div>
  <router-view v-slot="{ Component }">
    <transition name="page" mode="out-in"
      @before-leave="dimBg(true)" @after-enter="dimBg(false)" @after-leave="dimBg(false)">
      <component :is="Component" />
    </transition>
  </router-view>
  <Toast />
  <ConfirmDialog />
  <ExternalLinkGuard />
  <XingTuAssistant />
  <GlobalPlayer />
  <NoticePanel />
  <SearchPanel />
  <Shortcuts />
  <CustomContextMenu />
  <ConsolePanel />
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import Toast from '@/components/common/Toast.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import ExternalLinkGuard from '@/components/common/ExternalLinkGuard.vue'
import XingTuAssistant from '@/components/common/XingTuAssistant.vue'
import GlobalPlayer from '@/components/common/GlobalPlayer.vue'
import NoticePanel from '@/components/common/NoticePanel.vue'
import SearchPanel from '@/components/blog/SearchPanel.vue'
import Shortcuts from '@/components/blog/Shortcuts.vue'
import CustomContextMenu from '@/components/blog/CustomContextMenu.vue'
import ConsolePanel from '@/components/blog/ConsolePanel.vue'
import { useShortcuts } from '@/composables/useShortcuts'
import { initApplied, currentWallpaper } from '@/composables/useTheme'
import { WALLPAPERS } from '@/config/wallpapers'

useShortcuts()
initApplied()

const auth = useAuthStore()
const route = useRoute()
const scrollProgress = ref(0)

// ── 动态壁纸（流畅播放算法）──────────────────────────────
// 1) 低端设备（核数 ≤4）或系统开启"减少动效" → 不播视频，仅静态海报（poster）
// 2) poster 首帧兜底：视频未就绪时先亮海报，杜绝黑屏/白屏
// 3) 页面隐藏（切 tab）→ 立即暂停解码，释放 GPU/CPU，避免后台卡顿
// 4) canplay 后才 play()，且 play() 失败静默降级（自动播放策略拒绝时仍显示海报）
// 5) 单实例 video：切换壁纸仅换 src，绝不多路解码
const canPlayVideo = !window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches &&
  (navigator.hardwareConcurrency == null || navigator.hardwareConcurrency > 4)

const bgWallpaper = computed(() =>
  WALLPAPERS.find(w => w.id === currentWallpaper.value) || null
)
const bgVideo = computed(() => (bgWallpaper.value?.video ? bgWallpaper.value : null))
const bgVideoEl = ref(null)
// 视频加载/播放出错时降级为静态海报，避免黑屏与反复报错
const videoFailed = ref(false)

function tryPlayVideo() {
  const v = bgVideoEl.value
  if (!v || document.hidden || v.readyState < 2) return
  v.play().catch(() => { /* 自动播放被拒 → 静默停留海报 */ })
}

function onVisibilityChange() {
  const v = bgVideoEl.value
  if (!v) return
  if (document.hidden) v.pause()
  else tryPlayVideo()
}

// 路由过渡期间压暗背景：页面淡出/淡入在暗底上进行，
// 避免跳转瞬间露出中央偏亮的壁纸画面造成“白框/亮框”错觉
function dimBg(dim) {
  document.body.classList.toggle('route-transitioning', dim)
}

watch(bgVideoEl, el => {
  if (!el) return
  el.addEventListener('canplay', tryPlayVideo, { once: true })
  const t = setTimeout(tryPlayVideo, 1200) // 就绪超时也尝试（readyState≥2 才真正播放）
  el.addEventListener('play', () => clearTimeout(t), { once: true })
})

watch(bgWallpaper, () => { videoFailed.value = false; tryPlayVideo() })

onMounted(() => document.addEventListener('visibilitychange', onVisibilityChange))
onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('scroll', updateScrollProgress)
  window.removeEventListener('resize', updateScrollProgress)
  if (revealMutationObserver) revealMutationObserver.disconnect()
})

let revealMutationObserver = null
let lastVisitReport = 0

// 访问上报：IP / 经纬度 / 位置 → admin-service（每次路由切换都上报一次，供真实在线人数统计）
async function reportVisit() {
  try {
    const geo = await fetch('https://ipwho.is/').then(r => r.json()).catch(() => null)
    const body = {
      ip: geo?.ip || '',
      country: geo?.country || '',
      region: geo?.region || '',
      city: geo?.city || '',
      isp: geo?.connection?.isp || '',
      latitude: geo?.latitude || null,
      longitude: geo?.longitude || null,
      userAgent: navigator.userAgent.slice(0, 400),
      pagePath: location.pathname + location.search
    }
    // keepalive: 请求不随页面刷新/关闭被 abort（消除控制台 net::ERR_ABORTED /api/admin/visit）
    await fetch('/api/admin/visit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      keepalive: true
    })
  } catch {}
}

function updateScrollProgress() {
  const doc = document.documentElement
  const max = doc.scrollHeight - window.innerHeight
  scrollProgress.value = max > 0 ? Math.min(100, (window.scrollY / max) * 100) : 0
}

onMounted(async () => {
  updateScrollProgress()
  window.addEventListener('scroll', updateScrollProgress, { passive: true })
  window.addEventListener('resize', updateScrollProgress)

  reportVisit()

  // 每次路由切换都上报一次访问（30s 节流），保证近 10 分钟窗口有鲜活数据
  watch(() => route.fullPath, () => {
    const now = Date.now()
    if (now - lastVisitReport < 30000) return
    lastVisitReport = now
    reportVisit()
  })

  // Restore auth state via httpOnly Cookie session refresh
  if (!auth.token) {
    await auth.tryRefresh()
  }

  // IntersectionObserver for reveal animations (covers async route components)
  const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible')
        revealObserver.unobserve(entry.target)
      }
    })
  }, { threshold: 0.1 })

  const registerReveals = (root = document) => {
    root.querySelectorAll('.motions-reveal:not(.visible)').forEach(el => revealObserver.observe(el))
  }

  registerReveals()
  const mutationObserver = new MutationObserver(mutations => {
    for (const m of mutations) {
      for (const node of m.addedNodes) {
        if (node.nodeType === 1) registerReveals(node)
      }
    }
  })
  revealMutationObserver = mutationObserver
  mutationObserver.observe(document.body, { childList: true, subtree: true })
})
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  min-height: 100vh;
}

.scroll-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 2px;
  z-index: 9999;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent), var(--color-pink));
  box-shadow: 0 0 10px var(--color-glow);
  transition: width 0.1s linear;
}

.page-enter-active,
.page-leave-active {
  transition: opacity 0.22s ease;
}
.page-enter-from {
  opacity: 0;
}
.page-leave-to {
  opacity: 0;
}
/* WebGL canvas 路由过渡"白框"终极防护（Chromium 合成层问题）：
   祖先 opacity 过渡时浏览器会临时创建/销毁 WebGL 合成层，层建立后的
   头几帧纹理未就绪 → 画布区域闪现白色矩形框。
   三重防护：
   ① translate3d 让画布常驻独立合成层，层不随过渡重建，纹理始终有效；
   ② 离开阶段第一时间移出渲染树，不参与 0.22s 的 opacity 渐变；
   ③ 进入阶段等页面过渡结束后自身再 0.45s 淡入。 */
canvas.hero-canvas,
canvas.bg-gl-canvas {
  transform: translate3d(0, 0, 0);
  backface-visibility: hidden;
  transition: opacity 0.45s ease;
}
.page-leave-active canvas.hero-canvas,
.page-leave-active canvas.bg-gl-canvas {
  display: none !important;
}
.page-enter-active canvas.hero-canvas,
.page-enter-active canvas.bg-gl-canvas {
  opacity: 0;
}

/* 路由过渡压暗罩：覆盖在 #bg-layer（含视频壁纸）之上，过渡在暗底上进行 */
body.route-transitioning #bg-layer::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 2;
  background: rgba(4, 4, 14, 0.72);
}
</style>

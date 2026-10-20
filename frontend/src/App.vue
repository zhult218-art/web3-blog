<template>
  <div id="bg-layer" aria-hidden="true">
    <!-- 视频壁纸：poster 首帧兜底（黑屏防护）；低端机/减少动效偏好 → 静态海报图。
         元素常驻（v-show 而非 v-if）：切换壁纸不销毁 video，避免进行中的媒体流
         被强行中止而在控制台刷 net::ERR_ABORTED -->
    <video
      v-if="canPlayVideo"
      v-show="bgVideo && !videoFailed"
      ref="bgVideoEl"
      :src="bgVideo && !videoFailed ? bgVideo.video : undefined"
      :poster="bgVideo ? bgVideo.poster : undefined"
      muted loop playsinline disablepictureinpicture preload="auto"
      @error="onBgVideoError"
      :style="{ width: '100%', height: '100%', objectFit: 'cover', position: 'absolute', inset: 0 }"
    ></video>
    <img v-if="bgVideo && (videoFailed || !canPlayVideo)" :src="bgVideo.poster" :style="{ width: '100%', height: '100%', objectFit: 'cover', position: 'absolute', inset: 0 }" />
    <!-- 可读性暗纱：壁纸是 <video>/<img> 子元素，会整块盖住 #bg-layer 自身的渐变遮罩，
         导致「壁纸模式压暗」实际从未生效 —— 正文一直直接压在动漫人物上。
         这层常驻在壁纸之上、内容之下：上下两端压得更深（导航 / 标题 / 页脚所在），
         中间留一档，画面仍可辨认。 -->
    <div class="bg-veil"></div>
  </div>
  <div class="scroll-progress" :style="{ width: scrollProgress + '%' }"></div>
  <!-- 维护模式横幅：系统设置中开启后全站顶部提示（普通流程文档流，随滚动离开） -->
  <div v-if="maintenanceOn" class="maint-bar">
    <span class="maint-icon">🚧</span>
    <span>站点正在维护升级中，部分功能可能暂时不可用，给您带来不便敬请谅解</span>
    <router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/settings" class="maint-admin">管理员：前往关闭</router-link>
  </div>
  <router-view v-slot="{ Component }">
    <transition name="page" mode="out-in"
      @before-leave="dimBg(true)" @after-enter="dimBg(false)" @after-leave="dimBg(false)">
      <component :is="Component" />
    </transition>
  </router-view>
  <Toast />
  <ConfirmDialog />
  <ExternalLinkGuard />
  <!-- 全局加载界面：由真实加载（在途请求 / 路由切换）驱动，无加载不显示 -->
  <LoadingScreen />
  <XingTuAssistant v-if="jarvisOn" />
  <GlobalPlayer />
  <!-- 极光广场聊天 Dock：登录后全局常驻左侧栏（可展开/缩窄/关闭），路由切换不断线；管理员关闭群聊时隐藏 -->
  <ChatDock v-if="auth.isLoggedIn && !chat.chatClosed" />
  <NoticePanel />
  <SearchPanel />
  <Shortcuts />
  <CustomContextMenu />
  <ConsolePanel />
  <QzoneJobBadge />
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, onErrorCaptured } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useSiteSettingsStore } from '@/stores/modules/siteSettings'
import Toast from '@/components/common/Toast.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import ExternalLinkGuard from '@/components/common/ExternalLinkGuard.vue'
import LoadingScreen from '@/components/common/LoadingScreen.vue'
import XingTuAssistant from '@/components/common/XingTuAssistant.vue'
import GlobalPlayer from '@/components/common/GlobalPlayer.vue'
import ChatDock from '@/components/chat/ChatDock.vue'
import { useChatStore } from '@/stores/modules/chat'
import NoticePanel from '@/components/common/NoticePanel.vue'
import SearchPanel from '@/components/blog/SearchPanel.vue'
import Shortcuts from '@/components/blog/Shortcuts.vue'
import CustomContextMenu from '@/components/blog/CustomContextMenu.vue'
import ConsolePanel from '@/components/blog/ConsolePanel.vue'
import QzoneJobBadge from '@/components/qzone/QzoneJobBadge.vue'
import { useShortcuts } from '@/composables/useShortcuts'
import { FEED_URL } from '@/api/blog'
import { initApplied, currentWallpaper } from '@/composables/useTheme'
import { WALLPAPERS } from '@/config/wallpapers'

useShortcuts()
initApplied()

// RSS 自动发现：静态 index.html 读不到 VITE_API_BASE，故运行时注入
// <link rel="alternate" type="application/rss+xml">，让阅读器/浏览器扩展能直接识别订阅源
{
  const link = document.createElement('link')
  link.rel = 'alternate'
  link.type = 'application/rss+xml'
  link.title = 'Aurora-朱 · 极光数字空间'
  link.href = FEED_URL
  document.head.appendChild(link)
}

// ── 屏蔽 Vue Suspense/transition 挂载·切页竞态的已知良性报错 ──
// `<Suspense :timeout="0"><transition mode="out-in"><component :is>` 组合下，
// 异步路由组件尚未解析完就切页（或挂载中途被卸载）时，Vue 运行时内部会对
// 已置空的节点字段取值抛 TypeError。已观测变体：reading 'component' /
// 'flags' / 'shapeFlag' / 'parentNode'，堆栈均指向 runtime-core 内部
// （locateNonHydratedAsyncRoot / componentUpdateFn / getNextHostNode / flushJobs）。
// 精确匹配"Vue 内部空指针 + 组件更新堆栈"才吞掉，其余错误照常上抛。
const VUE_NULL_READ = /Cannot read properties of null \(reading '(flags|shapeFlag|parentNode|component|el|anchor)'\)/
onErrorCaptured((err, _instance, info) => {
  const msg = String(err?.message || '')
  const stack = String(err?.stack || '')
  const vueInternalStack = stack.includes('componentUpdateFn')
    || stack.includes('locateNonHydratedAsyncRoot')
    || stack.includes('getNextHostNode')
    || stack.includes('flushJobs')
  if (VUE_NULL_READ.test(msg) && vueInternalStack) {
    if (import.meta.env.DEV) console.warn('[vue] 已忽略挂载/切页竞态的内部良性报错（' + info + '）')
    return false
  }
  return true
})

const auth = useAuthStore()
const site = useSiteSettingsStore()
// 全局群聊：登录即建立常驻 WebSocket，登出彻底断开
const chat = useChatStore()
watch(() => auth.isLoggedIn, (loggedIn) => {
  if (loggedIn) chat.init()
  else chat.reset()
})
// 功能开关：星途助手显隐 / 维护模式横幅（配置由路由守卫首跳时加载）
const jarvisOn = computed(() => site.featureOn('jarvis'))
const maintenanceOn = computed(() => site.featureOn('maintenance'))
const route = useRoute()
const scrollProgress = ref(0)

// ── 动态壁纸（流畅播放算法）──────────────────────────────
// 1) 低端设备（核数 ≤4）或系统开启"减少动效" → 不播视频，仅静态海报（poster）
// 2) poster 首帧兜底：视频未就绪时先亮海报，杜绝黑屏/白屏
// 3) 页面隐藏（切 tab）→ 立即暂停解码，释放 GPU/CPU，避免后台卡顿
// 4) canplay 后才 play()，且 play() 失败静默降级（自动播放策略拒绝时仍显示海报）
// 5) 单实例常驻 video（v-show 切换）：换壁纸仅换 src 不销毁元素；换源前先 pause()，
//    让旧流优雅停止，最大限度减少控制台 net::ERR_ABORTED 噪音
const canPlayVideo = !window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches &&
  (navigator.hardwareConcurrency == null || navigator.hardwareConcurrency > 4)

const bgWallpaper = computed(() =>
  WALLPAPERS.find(w => w.id === currentWallpaper.value) || null
)
const bgVideo = computed(() => (bgWallpaper.value?.video ? bgWallpaper.value : null))
const bgVideoEl = ref(null)
// 视频加载/播放出错时降级为静态海报，避免黑屏与反复报错
const videoFailed = ref(false)

// 媒体元素 error 事件处理：MEDIA_ERR_ABORTED(1) 只是换源/预载被中止
// （浏览器读够 metadata 后会主动中断剩余请求，控制台因此出现 net::ERR_ABORTED），
// 并非真实故障。若把它当作失败会让壁纸被永久降级成静态海报，故此处忽略。
function onBgVideoError() {
  const v = bgVideoEl.value
  if (v && v.error && v.error.code === 1) return
  videoFailed.value = true
}

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

watch(bgWallpaper, (nw, ow) => {
  // 先暂停旧流再让 Vue 换 src：播放/缓冲中的媒体被直接换源会被浏览器强行中止
  const v = bgVideoEl.value
  if (v && ow?.video && ow.video !== nw?.video) v.pause()
  videoFailed.value = false
  tryPlayVideo()
})

onMounted(() => document.addEventListener('visibilitychange', onVisibilityChange))
onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('scroll', updateScrollProgress)
  window.removeEventListener('resize', updateScrollProgress)
  if (revealMutationObserver) revealMutationObserver.disconnect()
})

let revealMutationObserver = null
let lastVisitReport = 0
let ipwhoFailed = false // ipwho.is 本次会话内失败过则不再请求，避免控制台反复打 net::ERR

// 访问上报：IP / 经纬度 / 位置 → admin-service（每次路由切换都上报一次，供真实在线人数统计）
async function reportVisit() {
  try {
    let geo = null
    if (!ipwhoFailed) {
      const ctrl = new AbortController()
      const timer = setTimeout(() => ctrl.abort(), 5000) // 5s 超时，避免挂起
      try {
        const r = await fetch('https://ipwho.is/', { signal: ctrl.signal })
        if (!r.ok) throw new Error(`ipwho ${r.status}`)
        geo = await r.json()
        // ipwho.is 失败时返回 { success: false, error: { ... } }
        if (geo && geo.success === false) geo = null
      } catch {
        ipwhoFailed = true // 失败后本次会话不再请求 ipwho.is
      } finally {
        clearTimeout(timer)
      }
    }
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
    // sendBeacon：页面刷新/关闭时请求仍可靠送达，且失败不产生控制台 net::ERR_ABORTED 红字
    const blob = new Blob([JSON.stringify(body)], { type: 'application/json' })
    if (!navigator.sendBeacon('/api/admin/visit', blob)) {
      await fetch('/api/admin/visit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
        keepalive: true
      })
    }
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

  // 幂等确保站点配置就绪（标题/SEO/开关/页脚）
  try { await site.load() } catch {}

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

  // 登录态就绪（含刷新恢复）后立即接入全局群聊 WebSocket
  if (auth.isLoggedIn) chat.init()

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

/* 维护模式横幅 */
.maint-bar {
  position: relative;
  z-index: 9998;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 7px 16px;
  font-size: 12px;
  letter-spacing: 0.03em;
  color: #ffe9c4;
  background: linear-gradient(90deg, rgba(180, 83, 9, 0.92), rgba(217, 119, 6, 0.92), rgba(180, 83, 9, 0.92));
  border-bottom: 1px solid rgba(253, 230, 138, 0.35);
  box-shadow: 0 4px 18px -6px rgba(217, 119, 6, 0.6);
}
.maint-icon { font-size: 14px; }
.maint-admin {
  color: #fff7ed;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.maint-admin:hover { color: #fff; }

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

/* 壁纸可读性暗纱（见模板注释）。z-index 低于路由过渡压暗罩，两者可叠加。
   数值取「顶部最重 → 中段最轻 → 底部再压」的曲线：顶部是导航与页面主标题，
   底部是页脚与固定工具条，中段留给画面本身。 */
#bg-layer .bg-veil {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    linear-gradient(180deg,
      rgba(5, 5, 13, 0.86) 0%,
      rgba(5, 5, 13, 0.68) 16%,
      rgba(5, 5, 13, 0.52) 38%,
      rgba(5, 5, 13, 0.58) 70%,
      rgba(5, 5, 13, 0.78) 100%),
    radial-gradient(ellipse 118% 78% at 50% 42%, transparent 32%, rgba(4, 4, 12, 0.34) 100%);
}

/* 明亮主题下换成浅色薄纱，避免整页压成灰 */
body[data-theme='aurora'] #bg-layer .bg-veil {
  background:
    linear-gradient(180deg,
      rgba(240, 243, 250, 0.82) 0%,
      rgba(240, 243, 250, 0.58) 18%,
      rgba(240, 243, 250, 0.34) 40%,
      rgba(240, 243, 250, 0.52) 100%);
}

/* 路由过渡压暗罩：覆盖在 #bg-layer（含视频壁纸）之上，过渡在暗底上进行。
   route-transitioning：顶层路由（App.vue 的 page 过渡）期间；
   vt-active：布局内切页（View Transitions API）期间 —— 两者观感保持一致。 */
body.route-transitioning #bg-layer::before,
body.vt-active #bg-layer::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 2;
  background: rgba(4, 4, 14, 0.72);
}

/* ══ View Transitions API：布局内切页过渡（见 router/viewTransitions.js）══
   浏览器级快照过渡，不参与 Vue 组件树，与 Suspense / keep-alive / <transition>
   互不干扰；仅浏览器支持且未开启「减少动效」时生效。
   参与动画的只有内容区（main → vt-content）与常驻播放器（vt-player）；
   导航 / 壁纸 / 页脚在两份快照中一致，交叉淡入淡出后视觉上完全静止，
   整体观感即「内容滑动淡入、其余常驻不动」。 */
::view-transition-old(root),
::view-transition-new(root) {
  mix-blend-mode: normal;
}
::view-transition-old(root) {
  animation: 200ms ease-out both vt-root-out;
}
::view-transition-new(root) {
  animation: 240ms cubic-bezier(0.22, 0.61, 0.36, 1) both vt-root-in;
}
@keyframes vt-root-out { to { opacity: 0; } }
@keyframes vt-root-in { from { opacity: 0; } }

/* 内容区：离开时轻微下沉淡出，进入时上移淡入，形成"换页"层次感 */
::view-transition-old(vt-content) {
  animation: 180ms ease-out both vt-content-out;
}
::view-transition-new(vt-content) {
  animation: 260ms cubic-bezier(0.22, 0.61, 0.36, 1) both vt-content-in;
}
@keyframes vt-content-out { to { opacity: 0; transform: translateY(-8px); } }
@keyframes vt-content-in { from { opacity: 0; transform: translateY(10px); } }

/* 常驻播放器：两份快照位置完全一致，禁用动画避免多余重绘 */
::view-transition-group(vt-player) { animation: none; }
::view-transition-old(vt-player) { display: none; }
::view-transition-new(vt-player) { animation: none; }

</style>

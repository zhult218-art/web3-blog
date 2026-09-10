<template>
  <div class="ring-wrap" ref="stageEl">
    <div class="ring-stage" ref="pinEl">
      <div class="ring-perspective">
        <div class="ring" ref="ringEl">
          <div
            v-for="(c, i) in cards"
            :key="c.title"
            class="ring-card"
            :style="{ transform: `rotateY(${i * step}deg) translateZ(var(--ring-r))` }"
            @click="openSite(c)"
          >
            <div class="rc-inner" :data-i="i">
              <span class="rc-icon">{{ c.icon }}</span>
              <h4 class="rc-title">{{ c.title }}</h4>
              <p class="rc-sub">{{ c.sub }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 中央信息面板：显示当前朝前的卡片 -->
      <div class="ring-center">
        <span class="rc-no">{{ String(active + 1).padStart(2, '0') }} / {{ String(cards.length).padStart(2, '0') }}</span>
        <h3 class="rc-name">{{ current.title }}</h3>
        <p class="rc-desc">{{ current.desc }}</p>
      </div>

      <!-- 顶部 HUD -->
      <div class="ring-hud">
        <span class="fw-tag">EFFECT_04</span>
        <span>3D RING CAROUSEL · 滚动旋转 · 点击卡片访问站点</span>
      </div>
    </div>
    <p class="ring-tip">滚动驱动环形阵列旋转 · 松手吸附对齐卡片</p>
  </div>
</template>

<script setup>
// ============================================================
// EFFECT_05 滚动驱动的 3D 环形轮播
// 卡片以 rotateY(i*step) translateZ(R) 排成圆环，
// 整环 rotationY 由 scrub 驱动；onUpdate 内按朝向角计算
// 每张卡亮度/缩放（写在内层避免与定位 transform 冲突）；
// snap 吸附到每张卡片正对镜头的位置。
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
gsap.registerPlugin(ScrollTrigger)

// 精选收藏站点（避免与「技术矩阵」模块重复，内容独立）
const cards = [
  { icon: '🐙', title: 'GitHub', sub: 'github.com', url: 'https://github.com/zhult218-art', desc: '我的代码托管与开源项目主页，所有练手与生产级工程都在这里。' },
  { icon: '🟢', title: 'Vue.js', sub: 'vuejs.org', url: 'https://vuejs.org', desc: '渐进式前端框架，组合式 API 让大型应用依旧优雅可维护。' },
  { icon: '📘', title: 'MDN', sub: 'developer.mozilla.org', url: 'https://developer.mozilla.org', desc: 'Web 技术权威文档，前端与标准的终极参考资料库。' },
  { icon: '⚡', title: 'GSAP', sub: 'gsap.com', url: 'https://gsap.com', desc: '专业级滚动与时间线动效引擎，本站特效即由它驱动。' },
  { icon: '🧊', title: 'Three.js', sub: 'threejs.org', url: 'https://threejs.org', desc: 'WebGL 的抽象层，数行代码即可构建沉浸式 3D 场景。' },
  { icon: '⚡', title: 'Vite', sub: 'vitejs.dev', url: 'https://vitejs.dev', desc: '极速原生 ESM 开发服务器与构建工具，热更新毫秒级响应。' },
  { icon: '🎨', title: 'Dribbble', sub: 'dribbble.com', url: 'https://dribbble.com', desc: '设计师作品社区，交互与视觉灵感的常驻地。' },
  { icon: '📐', title: 'WebGL Fund.', sub: 'webglfundamentals.com', url: 'https://webglfundamentals.com', desc: '从矩阵到着色器的实时图形入门，硬核且易懂。' },
]

const N = cards.length
const step = 360 / N

const stageEl = ref(null)
const pinEl = ref(null)
const ringEl = ref(null)
const active = ref(0)
const current = computed(() => cards[active.value])
let ctx

// 点击卡片在新标签打开收藏站点
function openSite(c) {
  if (c.url) window.open(c.url, '_blank', 'noopener')
}

// 计算环形半径：随视口自适应
function radius() {
  const vw = innerWidth
  const cardW = Math.min(240, Math.max(150, vw * .17))
  return Math.round(Math.max(cardW * 1.9, Math.min(430, vw * .34)))
}

onMounted(() => {
  ctx = gsap.context(() => {
    const ring = ringEl.value
    const inners = ring.querySelectorAll('.rc-inner')
    gsap.set('.ring-stage', { '--ring-r': radius() + 'px' })

    const setBright = gsap.quickSetter('[data-i]', 'filter') // 备用（未直接使用）
    const setScale = gsap.quickSetter('[data-i]', 'scale')

    const tl = gsap.timeline({
      defaults: { ease: 'none' },
      scrollTrigger: {
        trigger: stageEl.value,
        start: 'top top',
        end: '+=2600',
        scrub: .5,
        pin: pinEl.value,
        anticipatePin: 1,
        invalidateOnRefresh: true,
        // 吸附到每张卡正对镜头的进度点
        snap: {
          snapTo: Array.from({ length: N }, (_, i) => i / (N - 1) * ((N - 1) / N)).concat([1]),
          duration: .45,
          ease: 'power2.inOut',
          delay: .06,
        },
        onUpdate(self) {
          const rotY = -8 - self.progress * 360
          // 找最接近正前方的卡（角度差最小）
          let best = 0, bestDiff = 999
          for (let i = 0; i < N; i++) {
            let d = Math.abs(((rotY + i * step) % 360 + 360) % 360)
            d = Math.min(d, 360 - d)
            if (d < bestDiff) { bestDiff = d; best = i }
          }
          if (best !== active.value) active.value = best
          // 依朝向更新每张卡的亮度与缩放（内层元素）
          for (let i = 0; i < N; i++) {
            const facing = Math.cos((rotY + i * step) * Math.PI / 180)
            const f = Math.max(0, facing)
            const el = inners[i]
            el.style.filter = `brightness(${.42 + .58 * f})`
            el.style.transform = `translateZ(${f * 30}px) scale(${.86 + .14 * f})`
            el.style.opacity = String(.35 + .65 * f)
          }
        },
      },
    })

    // 主旋转：从 -8deg 转过一整圈回到起点姿态
    tl.fromTo(ring,
      { rotationY: -8, rotationX: 8 },
      { rotationY: -8 - 360, duration: 1 },
      0)

    // 背景光晕呼吸（非 scrub，独立装饰）
    gsap.to('.ring-center', {
      opacity: .92,
      repeat: -1,
      yoyo: true,
      duration: 2.6,
      ease: 'sine.inOut',
    })
  }, stageEl.value)

  window.addEventListener('resize', onResize)
})

function onResize() {
  gsap.set('.ring-stage', { '--ring-r': radius() + 'px' })
  ScrollTrigger.refresh()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  ctx?.revert()
})
</script>

<style scoped>
.ring-wrap { position: relative; }

.ring-stage {
  --ring-r: 380px;
  position: relative;
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(60% 50% at 50% 46%, rgba(102,126,234,.14), transparent 70%),
    radial-gradient(40% 36% at 50% 52%, rgba(118,75,162,.12), transparent 70%);
}

.ring-hud {
  position: absolute;
  top: clamp(4.5rem, 10vh, 6rem);
  left: clamp(1.2rem, 5vw, 3.4rem);
  z-index: 5;
  display: flex;
  gap: 1rem;
  font-family: 'Courier New', monospace;
  font-size: .66rem;
  letter-spacing: .18em;
  color: rgba(255,255,255,.45);
}
.fw-tag { color: #00d4ff; }

.ring-perspective {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  perspective: 1500px;
}

.ring {
  position: relative;
  width: 0; height: 0;
  transform-style: preserve-3d;
  will-change: transform;
}

.ring-card {
  position: absolute;
  width: min(240px, 62vw);
  margin-left: calc(min(240px, 62vw) / -2);
  height: 300px;
  margin-top: -150px;
  transform-style: preserve-3d;
  cursor: pointer;
}

.rc-inner {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: .7rem;
  text-align: center;
  padding: 1.4rem;
  border-radius: 18px;
  background: linear-gradient(160deg, rgba(20,20,48,.92), rgba(12,12,32,.88));
  border: 1px solid rgba(102,126,234,.35);
  box-shadow: 0 24px 60px rgba(0,0,0,.5), inset 0 0 40px rgba(102,126,234,.08);
  will-change: transform, filter, opacity;
}

.rc-icon {
  font-size: 2.1rem;
  color: #00d4ff;
  text-shadow: 0 0 22px rgba(0,212,255,.6);
}
.rc-title { font-size: 1.15rem; font-weight: 700; color: rgba(255,255,255,.94); }
.rc-sub {
  font-family: 'Courier New', monospace;
  font-size: .62rem;
  letter-spacing: .25em;
  color: rgba(255,255,255,.38);
}

/* ---- 中央信息 ---- */
.ring-center {
  position: absolute;
  left: 50%;
  bottom: clamp(3.2rem, 9vh, 6rem);
  transform: translateX(-50%);
  z-index: 4;
  width: min(420px, 84vw);
  text-align: center;
  pointer-events: none;
}
.rc-no {
  font-family: 'Courier New', monospace;
  font-size: .68rem;
  letter-spacing: .3em;
  color: #00d4ff;
}
.rc-name {
  margin: .5rem 0 .4rem;
  font-size: clamp(1.5rem, 3.4vw, 2.3rem);
  font-weight: 800;
  background: linear-gradient(135deg, #fff 30%, rgba(255,255,255,.6));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.rc-desc {
  font-size: .82rem;
  line-height: 1.75;
  color: rgba(255,255,255,.55);
}

.ring-tip {
  padding: 1rem 0 .4rem;
  text-align: center;
  font-size: .72rem;
  letter-spacing: .1em;
  color: rgba(255,255,255,.32);
}
</style>

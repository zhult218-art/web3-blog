<template>
  <section id="motionlab" class="motion-lab">
    <div class="section-bg">
      <div class="bg-grid"></div>
      <div class="ml-glow"></div>
    </div>

    <SectionHead num="[ 04 ]" title="动效实验室" sub="MOTION LAB" />

    <p class="lab-intro">
      滚动即叙事。这一区把八种现代滚动动效范式做成可交互的实机演示：
      流体扭曲、全屏扩展、场景切换、3D 环形阵列、叠层转场、视差图层、
      打字机滚动、卡片 3D 旋转，全部由 GSAP ScrollTrigger 驱动。
    </p>

    <!-- EFFECT_01 交互式流体扭曲（非 pin，常规流） -->
    <FluidWarp />

    <!-- EFFECT_02 滚动驱动全屏扩展转场（pin + scrub + snap） -->
    <ExpandReveal />

    <!-- EFFECT_03 滚动驱动场景切换 + 吸附（pin + labels snap） -->
    <SceneSwitcher />

    <!-- EFFECT_04 滚动驱动 3D 环形轮播（pin + scrub + snap） -->
    <RingCarousel />

    <!-- EFFECT_05 滚动叠层转场（sticky 堆叠 + 覆盖压暗） -->
    <StackDeck />

    <!-- EFFECT_06 视差图层：背景 / 中景 / 前景以不同速度漂移 -->
    <div class="ext-wrap" ref="parallaxRoot">
      <div class="ext-head">
        <span class="ext-tag">EFFECT_06 · PARALLAX</span>
        <h3 class="ext-title">视差图层 · 多速度漂移</h3>
        <p class="ext-desc">滚动时背景、中景、前景以不同速率位移，制造空间纵深与立体感。</p>
      </div>
      <div class="parallax-stage">
        <div class="px-layer px-far" data-speed="0.2">
          <span class="px-word">SCROLL</span>
          <span class="px-word px-word-2">DEPTH</span>
        </div>
        <div class="px-layer px-mid" data-speed="0.5">
          <div class="px-orb a"></div>
          <div class="px-orb b"></div>
          <div class="px-orb c"></div>
        </div>
        <div class="px-layer px-near" data-speed="0.85">
          <span class="px-chip">parallax.layer</span>
          <span class="px-chip">depth = 0.85</span>
          <span class="px-chip">scrollY → translateY</span>
        </div>
      </div>
    </div>

    <!-- EFFECT_07 滚动驱动打字机：文本随滚动进度逐字呈现 -->
    <div class="ext-wrap" ref="typeRoot">
      <div class="ext-head">
        <span class="ext-tag">EFFECT_07 · TYPEWRITER SCROLL</span>
        <h3 class="ext-title">滚动打字机 · 进度映射字符</h3>
        <p class="ext-desc">把滚动进度映射到字符数，文本随你下滑而逐字写出，倒滚则回收。</p>
      </div>
      <div class="type-stage">
        <span class="type-text" ref="typeText">{{ typed }}</span><span class="type-caret" v-show="typed.length < fullText.length"></span>
      </div>
      <div class="type-meta">
        <span>{{ typed.length }} / {{ fullText.length }} chars</span>
        <span>{{ Math.round(progress * 100) }}%</span>
      </div>
    </div>

    <!-- EFFECT_08 卡片 3D 旋转：随滚动旋转一组卡片 -->
    <div class="ext-wrap" ref="rotateRoot">
      <div class="ext-head">
        <span class="ext-tag">EFFECT_08 · CARD 3D ROTATE</span>
        <h3 class="ext-title">卡片 3D 旋转 · 滚动驱动</h3>
        <p class="ext-desc">3D 透视舞台上的卡片阵列，滚动驱动 X/Y 轴旋转，制造立体翻页节奏。</p>
      </div>
      <div class="rotate-stage" ref="rotateStage">
        <div v-for="(c, i) in rotateCards" :key="c.name" class="rt-card" :style="{ '--i': i }">
          <div class="rt-inner">
            <span class="rt-idx">0{{ i + 1 }}</span>
            <span class="rt-icon">{{ c.icon }}</span>
            <h4 class="rt-name">{{ c.name }}</h4>
            <p class="rt-desc">{{ c.desc }}</p>
            <span class="rt-mono">{{ c.mono }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
// ============================================================
// [04] 动效实验室：滚动特效合集容器
// 组装五个特效子组件 + 三个内联特效（视差、打字机、3D 旋转）
// 各子组件自管 gsap.context 生命周期，卸载时自动回收。
// 内联特效共用本组件的 gsap.context，统一在 onBeforeUnmount 清理。
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import SectionHead from './SectionHead.vue'
import FluidWarp from './effects/FluidWarp.vue'
import ExpandReveal from './effects/ExpandReveal.vue'
import SceneSwitcher from './effects/SceneSwitcher.vue'
import RingCarousel from './effects/RingCarousel.vue'
import StackDeck from './effects/StackDeck.vue'

gsap.registerPlugin(ScrollTrigger)

const parallaxRoot = ref(null)
const typeRoot = ref(null)
const typeText = ref(null)
const rotateRoot = ref(null)
const rotateStage = ref(null)

// 打字机目标文本
const fullText = '滚动驱动动画的本质：把 scrollY 映射到时间轴，让叙事随用户行为而非时间流逝而展开。这里我们用 scrub 钩住滚动进度，将其转为字符数，实现「滚动即书写」。'
const typed = ref('')
const progress = ref(0)

// 3D 旋转卡片数据
const rotateCards = [
  { icon: '🌌', name: '宇宙粒子', desc: 'GPU 粒子流体演化的视觉表达', mono: 'WebGL · Shader' },
  { icon: '🧠', name: '智能体', desc: 'LLM + RAG 的多模态推理核心', mono: 'FastAPI · Python' },
  { icon: '🛰️', name: '微服务', desc: 'Spring Cloud 弹性分布式架构', mono: 'Java · Nacos' },
  { icon: '📊', name: '量化引擎', desc: '多因子选股与策略回测平台', mono: 'Pandas · Backtrader' },
  { icon: '🎨', name: '动效设计', desc: 'GSAP 滚动叙事与 3D 交互', mono: 'GSAP · Three.js' },
]

let ctx = null
let refreshTimer = 0

onMounted(() => {
  refreshTimer = setTimeout(() => ScrollTrigger.refresh(), 600)
  window.addEventListener('load', () => ScrollTrigger.refresh(), { once: true })

  ctx = gsap.context(() => {
    // ============ EFFECT_06 视差图层 ============
    const layers = gsap.utils.toArray('.parallax-stage .px-layer')
    layers.forEach(layer => {
      const speed = parseFloat(layer.dataset.speed || '0.5')
      gsap.to(layer, {
        yPercent: -60 * speed,
        ease: 'none',
        scrollTrigger: {
          trigger: '.parallax-stage',
          start: 'top bottom',
          end: 'bottom top',
          scrub: true,
        },
      })
    })

    // ============ EFFECT_07 滚动打字机 ============
    const updateTyped = () => {
      const p = progress.value
      const n = Math.floor(p * fullText.length)
      typed.value = fullText.slice(0, n)
    }
    gsap.to({ v: 0 }, {
      v: 1,
      ease: 'none',
      scrollTrigger: {
        trigger: typeRoot.value,
        start: 'top 70%',
        end: 'bottom 70%',
        scrub: true,
        onUpdate: (self) => {
          progress.value = self.progress
          updateTyped()
        },
      },
    })

    // ============ EFFECT_08 卡片 3D 旋转 ============
    if (rotateStage.value) {
      const cards = gsap.utils.toArray('.rt-card')
      gsap.from(cards, {
        y: 60,
        opacity: 0,
        duration: 0.8,
        stagger: 0.1,
        ease: 'power3.out',
        scrollTrigger: {
          trigger: rotateStage.value,
          start: 'top 80%',
          once: true,
        },
      })
      // 整体随滚动做 3D 旋转
      gsap.to(rotateStage.value, {
        rotateY: 12,
        rotateX: -6,
        ease: 'none',
        scrollTrigger: {
          trigger: rotateRoot.value,
          start: 'top bottom',
          end: 'bottom top',
          scrub: true,
        },
      })
      // 每张卡片各自轻微旋转
      cards.forEach((card, i) => {
        gsap.to(card, {
          rotateY: (i % 2 ? 1 : -1) * 8,
          y: -10 * (i % 2 ? 1 : -1),
          ease: 'none',
          scrollTrigger: {
            trigger: rotateStage.value,
            start: 'top bottom',
            end: 'bottom top',
            scrub: true,
          },
        })
      })
    }
  })
})

onBeforeUnmount(() => {
  if (refreshTimer) clearTimeout(refreshTimer)
  if (ctx) ctx.revert()
})
</script>

<style scoped>
.motion-lab {
  position: relative;
  padding: clamp(4rem, 9vh, 7rem) clamp(1.2rem, 5vw, 4rem) 0;
}

.section-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image: none;
  mask-image: linear-gradient(180deg, transparent, #000 8%, #000 92%, transparent);
  -webkit-mask-image: linear-gradient(180deg, transparent, #000 8%, #000 92%, transparent);
}

.ml-glow {
  position: absolute;
  top: -10%;
  left: 50%;
  width: 70vw;
  height: 40vh;
  transform: translateX(-50%);
  background: radial-gradient(ellipse at center, rgba(102,126,234,.1), transparent 65%);
}

.lab-intro {
  max-width: 68ch;
  margin: -1.6rem auto 3.2rem;
  text-align: center;
  font-size: .86rem;
  line-height: 2;
  color: rgba(255,255,255,.5);
}

/* ============================================================
   公共包装：内联特效外层
   ============================================================ */
.ext-wrap {
  position: relative;
  margin: 5rem auto;
  max-width: 1100px;
  padding: 0 1rem;
}

.ext-head {
  text-align: center;
  margin-bottom: 2rem;
}

.ext-tag {
  display: inline-block;
  font-size: 0.6rem;
  letter-spacing: 0.3em;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
  padding: 0.3rem 0.8rem;
  border: 1px solid rgba(0, 212, 255, 0.3);
  border-radius: 999px;
  background: rgba(0, 212, 255, 0.05);
  margin-bottom: 0.8rem;
}

.ext-title {
  font-size: clamp(1.4rem, 3vw, 2rem);
  font-weight: 800;
  color: #fff;
  margin-bottom: 0.5rem;
  background: linear-gradient(135deg, #fff, #9db2ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.ext-desc {
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.5);
  max-width: 60ch;
  margin: 0 auto;
  line-height: 1.85;
}

/* ============================================================
   EFFECT_06 视差图层
   ============================================================ */
.parallax-stage {
  position: relative;
  height: 460px;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(8, 8, 20, 0.6), rgba(14, 14, 36, 0.4));
  border: 1px solid rgba(255, 255, 255, 0.07);
}

.px-layer {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  pointer-events: none;
}

.px-far {
  flex-direction: column;
  gap: 1rem;
}

.px-word {
  font-size: clamp(3rem, 8vw, 6rem);
  font-weight: 900;
  letter-spacing: 0.1em;
  color: rgba(102, 126, 234, 0.12);
  font-family: 'Courier New', monospace;
  text-transform: uppercase;
}

.px-word-2 {
  color: rgba(0, 212, 255, 0.1);
}

.px-mid { gap: 4rem; }

.px-orb {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #667eea, #4a1d8a);
  box-shadow: 0 0 50px rgba(102, 126, 234, 0.5);
  filter: blur(2px);
}

.px-orb.b {
  width: 60px;
  height: 60px;
  background: radial-gradient(circle at 30% 30%, #00d4ff, #034964);
  box-shadow: 0 0 40px rgba(0, 212, 255, 0.5);
}

.px-orb.c {
  width: 40px;
  height: 40px;
  background: radial-gradient(circle at 30% 30%, #ffd166, #7a4f02);
  box-shadow: 0 0 30px rgba(255, 209, 102, 0.5);
}

.px-near { gap: 1.4rem; }

.px-chip {
  font-size: 0.7rem;
  font-family: 'Courier New', monospace;
  color: rgba(255, 255, 255, 0.7);
  padding: 0.4rem 0.8rem;
  background: rgba(12, 12, 28, 0.7);
  border: 1px solid rgba(0, 212, 255, 0.3);
  border-radius: 8px;
  letter-spacing: 0.04em;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
}

/* ============================================================
   EFFECT_07 滚动打字机
   ============================================================ */
.type-stage {
  position: relative;
  padding: 2.5rem 2rem;
  min-height: 200px;
  background: rgba(8, 8, 20, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 20px;
  font-family: 'Courier New', monospace;
  font-size: 0.95rem;
  line-height: 2;
  color: rgba(255, 255, 255, 0.82);
  overflow: hidden;
}

.type-stage::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.55), transparent);
}

.type-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.type-caret {
  display: inline-block;
  width: 9px;
  height: 18px;
  margin-left: 2px;
  background: #00d4ff;
  vertical-align: -3px;
  animation: typeBlink 0.8s steps(2) infinite;
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.8);
}

@keyframes typeBlink { 50% { opacity: 0; } }

.type-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 0.8rem;
  font-size: 0.62rem;
  letter-spacing: 0.18em;
  color: rgba(255, 255, 255, 0.4);
  font-family: 'Courier New', monospace;
}

/* ============================================================
   EFFECT_08 卡片 3D 旋转
   ============================================================ */
.rotate-stage {
  position: relative;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 1rem;
  perspective: 1200px;
  perspective-origin: 50% 50%;
  padding: 1rem 0;
}

.rt-card {
  position: relative;
  height: 230px;
  border-radius: 16px;
  background: linear-gradient(160deg, rgba(20, 20, 44, 0.85), rgba(8, 8, 20, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
  transform-style: preserve-3d;
  will-change: transform;
  box-shadow: 0 8px 26px rgba(0, 0, 0, 0.4);
  transition: border-color 0.35s, box-shadow 0.35s;
}

.rt-card:hover {
  border-color: rgba(102, 126, 234, 0.45);
  box-shadow: 0 0 30px rgba(102, 126, 234, 0.3), 0 14px 40px rgba(0, 0, 0, 0.5);
}

.rt-inner {
  position: absolute;
  inset: 0;
  padding: 1.2rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  transform: translateZ(20px);
}

.rt-idx {
  font-size: 0.6rem;
  letter-spacing: 0.2em;
  color: rgba(255, 255, 255, 0.25);
  font-family: 'Courier New', monospace;
}

.rt-icon {
  font-size: 1.6rem;
  filter: drop-shadow(0 0 10px rgba(102, 126, 234, 0.5));
  margin-top: 0.3rem;
}

.rt-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #fff;
}

.rt-desc {
  font-size: 0.7rem;
  line-height: 1.55;
  color: rgba(255, 255, 255, 0.5);
  flex: 1;
}

.rt-mono {
  font-size: 0.58rem;
  letter-spacing: 0.12em;
  color: #8fa3ff;
  font-family: 'Courier New', monospace;
  padding-top: 0.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

@media (max-width: 900px) {
  .rotate-stage { grid-template-columns: repeat(2, 1fr); }
  .parallax-stage { height: 360px; }
}

@media (max-width: 640px) {
  .rotate-stage { grid-template-columns: 1fr; }
  .ext-wrap { margin: 3.5rem auto; }
  .px-near { flex-wrap: wrap; }
}
</style>

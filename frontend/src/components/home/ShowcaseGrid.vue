<template>
  <section id="showcase" class="showcase">
    <div class="section-bg">
      <div class="bg-grid"></div>
      <div class="bg-glow"></div>
    </div>

    <div class="section-container">
      <SectionHead num="[ 02 ]" title="核心作品与 Demo 橱窗" sub="FEATURED SHOWCASE" />

      <div class="show-grid">
        <article
          v-for="(card, i) in cards"
          :key="card.title"
          class="show-card show-card-outer"
          data-glow
          @click="openOverlay(i)"
        >
          <div class="border-beam"></div>
          <div class="show-card" ref="cardEls" @mousemove="onTilt" @mouseleave="onTiltLeave">
            <div class="spotlight"></div>

            <div class="demo-panel" :class="`demo-${i}`">
              <template v-if="i === 0">
                <div class="chat-row q">Q: {{ card.demo.q }}</div>
                <div class="chat-row a typing" v-for="(line, li) in typedLines[i]" :key="li">{{ line }}</div>
                <span class="caret" v-if="typingIdx[i] < card.demo.a.length"></span>
              </template>
              <template v-else-if="i === 1">
                <div class="galaxy">
                  <span class="star" v-for="s in 18" :key="s" :style="starStyle(s)"></span>
                  <div class="orbit o1"><div class="planet"></div></div>
                  <div class="orbit o2"><div class="planet p2"></div></div>
                  <div class="orbit o3"><div class="planet p3"></div></div>
                </div>
              </template>
              <template v-else-if="i === 2">
                <div class="term">
                  <div class="term-bar"><span></span><span></span><span></span></div>
                  <div class="term-body">
                    <div class="tl" v-for="(line, li) in typedLines[i]" :key="li"><span class="prompt">$</span> {{ line }}</div>
                    <span class="caret" v-if="typingIdx[i] < card.demo.a.length"></span>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="gesture-field">
                  <div class="wave-line" v-for="w in 5" :key="w"></div>
                  <div class="hand-note">✋ {{ typedLines[i].join(' ') }}<span class="caret"></span></div>
                </div>
              </template>
            </div>

            <div class="show-body">
              <div class="show-top">
                <h3 class="show-title">{{ card.title }}</h3>
                <span class="show-arrow">↗</span>
              </div>
              <p class="show-desc">{{ card.desc }}</p>
              <div class="show-tags">
                <span v-for="t in card.tags" :key="t" class="tag">#{{ t }}</span>
              </div>
            </div>

            <div class="show-actions">
              <span class="action-line"></span>
              <button class="demo-btn" data-magnetic @click.stop="go(card.path)">
                <span>LIVE DEMO</span>
                <span class="caret-b">▸</span>
              </button>
            </div>
          </div>
        </article>
      </div>
    </div>

    <!-- 全屏展开层 -->
    <Teleport to="body">
      <div v-if="overlay.open" class="show-overlay" @click.self="closeOverlay">
        <div class="overlay-inner" ref="overlayInner">
          <div class="overlay-head">
            <span class="overlay-idx">0{{ overlay.index + 1 }} / FEATURED</span>
            <button class="overlay-close" data-glow @click="closeOverlay">✕</button>
          </div>
          <div class="overlay-glow" :style="overlayGlowStyle"></div>
          <h2 class="overlay-title">{{ overlayCard.title }}</h2>
          <p class="overlay-desc">{{ overlayCard.desc }}</p>
          <div class="overlay-tags">
            <span v-for="t in overlayCard.tags" :key="t" class="tag">#{{ t }}</span>
          </div>
          <div class="overlay-feats">
            <div v-for="(f, fi) in overlayCard.features" :key="fi" class="feat">
              <span class="feat-num">0{{ fi + 1 }}</span>
              <span class="feat-txt">{{ f }}</span>
            </div>
          </div>
          <button class="overlay-cta" data-magnetic @click="go(overlayCard.path)">
            <span>启动 {{ overlayCard.title }}</span>
            <span class="cta-caret">▸▸</span>
          </button>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup>
// ============================================================
// 首页"核心作品"橱窗区（ShowcaseGrid）
// 四张 Demo 卡片：AI 对话打字机 / 星系动画 / 终端 / 手势场
// 支持 3D 倾斜跟随、光圈 spotlight、全屏展开层（FLIP 动画）
// ============================================================
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import SectionHead from './SectionHead.vue'

gsap.registerPlugin(ScrollTrigger)

const router = useRouter()

// 橱窗卡片配置：标题 / 描述 / 标签 / 跳转路径 / 打字机演示文案 / 展开层特性
const cards = [
  {
    title: 'AI 知识库智能体',
    desc: '接入 RAG 检索与 LLM 推理的问答体，支持长期记忆、工具调用与多轮对话。',
    tags: ['FastAPI', 'RAG', 'LLM'],
    path: '/ai',
    demo: { q: '如何快速搭建微服务网关？', a: '引入 Spring Cloud Gateway，配置路由熔断与限流策略，两步即可上线。' },
    features: ['RAG 文档检索与引用溯源', '多轮对话 + 上下文记忆', '函数调用 / 工具编排', '流式输出与语音交互'],
  },
  {
    title: '3D 交互粒子云 · 星系模拟',
    desc: '数万粒子在 GPU 中实时演化，鼠标拖拽旋转视角，体验沉浸式星云环绕。',
    tags: ['Three.js', 'WebGL', 'GLSL'],
    path: '/three',
    demo: { q: '', a: '' },
    features: ['GPU 粒子流体模拟', '轨道力学 + 引力牵引', 'Shader 星云着色', '手势拖拽视点控制'],
  },
  {
    title: 'Spring Cloud 微服务脚手架',
    desc: '一键生成网关、注册中心、配置中心与监控链路，开箱即用的生产级工程模板。',
    tags: ['Spring Cloud', 'Gateway', 'Docker'],
    path: '/architecture',
    demo: { q: '', a: 'mvn clean package -DskipTests\ndocker compose up -d --build\n[OK] Nacos 注册中心已就绪\ngateway 启动成功 -> :8030\n[OK] 6 个微服务全部上线' },
    features: ['Nacos 注册 / 配置中心', 'Gateway 统一网关', 'OpenFeign + 熔断降级', 'Docker Compose 一键编排'],
  },
  {
    title: '手势控制 3D 虚拟展厅',
    desc: 'MediaPipe 手势捕捉驱动 WebGL 场景，挥手旋转展品、握拳拉近视角。',
    tags: ['MediaPipe', 'WebGL', 'Gesture'],
    path: '/three',
    demo: { q: '', a: '手部关键点 21 个已锁定\n握拳拉近 · 挥手旋转\n展厅视角实时跟随中' },
    features: ['21 点手部关键点追踪', '手势 → 视角映射算法', '近场交互力反馈', '多语言展厅场景'],
  },
]

const cardEls = ref([])
const overlay = reactive({ open: false, index: 0 })
const typingIdx = ref([0, 0, 0, 0])
const typedLines = ref([[], [], [], []])
let timers = new Map()
let ctx = null
let lastRect = null

const overlayCard = computed(() => cards[overlay.index])
const overlayGlowStyle = computed(() => ({
  background: `radial-gradient(60% 55% at 50% 0%, rgba(102, 126, 234, 0.22), transparent 70%)`,
}))

function starStyle(i) {
  // 用下标做种子生成伪随机星星位置/大小/动画延迟
  const seed = i * 37
  return {
    left: (seed % 90) + 5 + '%',
    top: ((seed * 7) % 85) + 7 + '%',
    width: ((seed % 3) + 1) + 'px',
    animationDelay: (seed % 30) / 10 + 's',
    opacity: 0.35 + (seed % 5) / 10,
  }
}

// 鼠标移动时卡片 3D 倾斜跟随 + 聚光斑跟随
function onTilt(e) {
  const el = e.currentTarget
  const r = el.getBoundingClientRect()
  const px = (e.clientX - r.left) / r.width - 0.5
  const py = (e.clientY - r.top) / r.height - 0.5
  el.style.transform = `perspective(1000px) rotateY(${px * 6}deg) rotateX(${-py * 6}deg) translateY(-4px)`
  el.style.transition = 'transform 0.1s ease-out'
  el.style.setProperty('--spot-x', (px + 0.5) * 100 + '%')
  el.style.setProperty('--spot-y', (py + 0.5) * 100 + '%')
}

// 鼠标移出时回正卡片
function onTiltLeave(e) {
  const el = e.currentTarget
  el.style.transform = ''
  el.style.transition = 'transform 0.5s cubic-bezier(0.23,1,0.32,1)'
}

// 跳转到 Demo 对应页面
function go(path) {
  router.push(path)
}

// 启动各卡片的打字机演示（间隔逐字显示回答文案）
function startTyping() {
  timers.forEach(clearInterval)
  timers = new Map()
  cards.forEach((card, i) => {
    if (!card.demo.a) return
    const full = card.demo.a
    typingIdx.value[i] = 0
    typedLines.value[i] = []
    const tid = setInterval(() => {
      const idx = typingIdx.value[i]
      if (idx <= full.length) {
        typedLines.value[i] = full.slice(0, idx).split('\n')
        typingIdx.value[i] = idx + 1
      } else {
        clearInterval(tid)
        timers.delete(i)
      }
    }, 55)
    timers.set(i, tid)
  })
}

// 展开全屏层：记录卡片原始位置，用 FLIP 动画从卡片放大到全屏
function openOverlay(i) {
  const el = cardEls.value[i]
  lastRect = el ? el.getBoundingClientRect() : null
  overlay.index = i
  overlay.open = true
  nextTick(() => {
    const inner = document.querySelector('.show-overlay .overlay-inner')
    if (!inner) return
    inner.style.transition = 'none'
    inner.style.opacity = '1'
    if (lastRect) {
      inner.style.transformOrigin = '0 0'
      inner.style.transform = `translate(${lastRect.left}px, ${lastRect.top}px) scale(${lastRect.width / window.innerWidth}, ${lastRect.height / window.innerHeight})`
      requestAnimationFrame(() => {
        inner.style.transition = 'transform 0.55s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.4s ease'
        inner.style.transform = 'translate(0, 0) scale(1)'
      })
    }
  })
}

// 关闭全屏层：反向 FLIP 缩小回原卡片位置后再隐藏
function closeOverlay() {
  const inner = document.querySelector('.show-overlay .overlay-inner')
  if (inner && lastRect) {
    inner.style.transition = 'transform 0.45s cubic-bezier(0.55, 0, 0.75, 0.4), opacity 0.3s ease'
    inner.style.transform = `translate(${lastRect.left}px, ${lastRect.top}px) scale(${lastRect.width / window.innerWidth}, ${lastRect.height / window.innerHeight})`
    inner.style.opacity = '0'
    setTimeout(() => { overlay.open = false }, 420)
  } else {
    overlay.open = false
  }
}

onMounted(() => {
  startTyping()
  ctx = gsap.context(() => {
    gsap.from('.show-card', {
      y: 80,
      opacity: 0,
      scale: 0.96,
      duration: 1,
      stagger: 0.12,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: '.show-grid',
        start: 'top 82%',
        once: true,
      },
    })
  })
})

onBeforeUnmount(() => {
  timers.forEach(clearInterval)
  if (ctx) ctx.revert()
})
</script>

<style scoped>
.showcase {
  position: relative;
  padding: 6.5rem 2rem;
  overflow: hidden;
  /* 上下边缘渐隐：与相邻区块自然融合 */
  background: linear-gradient(180deg,
    transparent 0%, rgba(6, 6, 14, 0.15) 16%,
    rgba(10, 10, 26, 0.32) 84%, transparent 100%);
}

.section-bg { position: absolute; inset: 0; z-index: 0; pointer-events: none; }

.bg-grid {
  position: absolute;
  inset: 0;
  background-image: none;
  mask-image: radial-gradient(ellipse 70% 75% at 50% 50%, #000 20%, transparent 78%);
}

.bg-glow {
  position: absolute;
  width: 720px;
  height: 720px;
  left: 50%;
  top: 40%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.07), rgba(102, 126, 234, 0.05) 40%, transparent 70%);
  filter: blur(90px);
}

.section-container { position: relative; z-index: 1; max-width: 1200px; margin: 0 auto; }

.show-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.75rem;
  perspective: 1300px;
}

.show-card-outer { position: relative; border-radius: 20px; }

.show-card {
  position: relative;
  z-index: 1;
  padding: 1.25rem;
  background: rgba(12, 12, 28, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.1s ease-out, border-color 0.35s, box-shadow 0.35s;
  transform-style: preserve-3d;
}

.show-card:hover {
  border-color: rgba(0, 212, 255, 0.4);
  box-shadow:
    0 0 40px rgba(0, 212, 255, 0.14),
    0 26px 60px rgba(0, 0, 0, 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.spotlight {
  position: absolute;
  inset: 0;
  background: radial-gradient(340px circle at var(--spot-x, 50%) var(--spot-y, 50%), rgba(0, 212, 255, 0.12), transparent 70%);
  opacity: 0;
  transition: opacity 0.35s;
  pointer-events: none;
  z-index: 2;
}

.show-card:hover .spotlight { opacity: 1; }

.border-beam {
  position: absolute;
  inset: 0;
  border-radius: 20px;
  padding: 1.5px;
  background: conic-gradient(from var(--beam-angle, 0deg),
    transparent 0deg, transparent 285deg,
    rgba(0, 212, 255, 0.95) 335deg, rgba(102, 126, 234, 0.85) 360deg);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask-composite: exclude;
  opacity: 0;
  transition: opacity 0.4s;
  animation: showBeamSpin 5s linear infinite;
  pointer-events: none;
  z-index: 3;
}

@property --beam-angle {
  syntax: '<angle>';
  initial-value: 0deg;
  inherits: false;
}

@keyframes showBeamSpin {
  from { --beam-angle: 0deg; }
  to { --beam-angle: 360deg; }
}

.show-card-outer:hover .border-beam { opacity: 1; }

/* Demo 预览区 */
.demo-panel {
  position: relative;
  height: 190px;
  border-radius: 14px;
  overflow: hidden;
  margin-bottom: 1.15rem;
  background:
    radial-gradient(ellipse at 50% -20%, rgba(102, 126, 234, 0.16), transparent 65%),
    rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

/* AI 对话 */
.chat-row {
  padding: 0.45rem 0.9rem;
  font-size: 0.75rem;
  line-height: 1.65;
  font-family: 'Courier New', monospace;
}

.chat-row.q {
  color: #9db2ff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.chat-row.a { color: rgba(255, 255, 255, 0.75); }

.caret {
  display: inline-block;
  width: 7px;
  height: 14px;
  margin-left: 2px;
  vertical-align: -2px;
  background: #00d4ff;
  animation: caretBlink 0.9s steps(2) infinite;
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.8);
}

@keyframes caretBlink { 50% { opacity: 0; } }

/* 星系 */
.galaxy {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.star {
  position: absolute;
  background: #dbe6ff;
  border-radius: 50%;
  animation: twinkle 2.4s ease-in-out infinite;
}

@keyframes twinkle {
  0%, 100% { opacity: 0.25; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.4); }
}

.orbit {
  position: absolute;
  left: 50%;
  top: 50%;
  border: 1px dashed rgba(122, 145, 255, 0.3);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: orbitSpin 9s linear infinite;
}

.o1 { width: 110px; height: 110px; }
.o2 { width: 150px; height: 150px; animation-duration: 14s; animation-direction: reverse; }
.o3 { width: 190px; height: 190px; animation-duration: 20s; }

@keyframes orbitSpin { from { transform: translate(-50%, -50%) rotate(0deg); } to { transform: translate(-50%, -50%) rotate(360deg); } }

.planet {
  position: absolute;
  left: 50%;
  top: 4px;
  width: 9px;
  height: 9px;
  margin-left: -4.5px;
  border-radius: 50%;
  background: #00d4ff;
  box-shadow: 0 0 14px rgba(0, 212, 255, 0.9);
}

.planet.p2 { background: #8b7cf6; box-shadow: 0 0 14px rgba(139, 124, 246, 0.9); }
.planet.p3 { background: #ffd166; box-shadow: 0 0 14px rgba(255, 209, 102, 0.9); }

/* 终端 */
.term {
  position: absolute;
  inset: 14px;
  border-radius: 10px;
  background: rgba(4, 4, 12, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.07);
  overflow: hidden;
}

.term-bar {
  display: flex;
  gap: 5px;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.term-bar span { width: 7px; height: 7px; border-radius: 50%; background: rgba(255, 255, 255, 0.15); }
.term-bar span:first-child { background: rgba(255, 107, 107, 0.6); }
.term-bar span:nth-child(2) { background: rgba(255, 209, 102, 0.6); }
.term-bar span:nth-child(3) { background: rgba(76, 217, 148, 0.6); }

.term-body {
  padding: 10px 14px;
  font-size: 0.7rem;
  line-height: 1.8;
  font-family: 'Courier New', monospace;
  color: rgba(255, 255, 255, 0.8);
}

.tl .prompt { color: #00d4ff; margin-right: 5px; }

/* 手势 */
.gesture-field {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 0.7rem;
  overflow: hidden;
}

.hand-note {
  font-size: 0.72rem;
  color: rgba(255, 255, 255, 0.6);
  font-family: 'Courier New', monospace;
}

.wave-line {
  width: 150px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.4), transparent);
  animation: waveSweep 1.8s ease-in-out infinite;
}

.wave-line:nth-child(2) { animation-delay: 0.25s; }
.wave-line:nth-child(3) { animation-delay: 0.5s; }
.wave-line:nth-child(4) { animation-delay: 0.75s; }
.wave-line:nth-child(5) { animation-delay: 1s; }

@keyframes waveSweep {
  0%, 100% { transform: scaleX(0.3); opacity: 0.3; }
  50% { transform: scaleX(1.15); opacity: 1; }
}

/* 卡片内容 */
.show-body { padding: 0 0.4rem; transform: translateZ(14px); }

.show-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
}

.show-title { font-size: 1.2rem; font-weight: 700; color: #fff; }

.show-arrow {
  font-size: 1.25rem;
  color: rgba(255, 255, 255, 0.2);
  transition: all 0.3s;
}

.show-card:hover .show-arrow { color: #00d4ff; transform: translate(3px, -3px); }

.show-desc {
  font-size: 0.84rem;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 0.9rem;
}

.show-tags { display: flex; flex-wrap: wrap; gap: 0.5rem; }

.tag {
  font-size: 0.68rem;
  padding: 0.28rem 0.65rem;
  border-radius: 999px;
  color: #7fd9ff;
  background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.22);
  font-family: 'Courier New', monospace;
}

.show-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1.2rem;
  transform: translateZ(10px);
}

.action-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.12), transparent);
}

.demo-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: #9db2ff;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.35);
  border-radius: 999px;
  cursor: pointer;
  transform: translate(var(--mx, 0), var(--my, 0));
  transition: transform 0.25s cubic-bezier(0.23, 1, 0.32, 1), color 0.3s, box-shadow 0.3s;
}

.demo-btn:hover {
  color: #fff;
  box-shadow: 0 0 20px rgba(102, 126, 234, 0.45);
}

.caret-b { color: inherit; }

/* ============ Overlay ============ */
.show-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(4, 4, 12, 0.78);
  backdrop-filter: blur(14px);
  display: flex;
  animation: overlayFade 0.4s ease both;
}

@keyframes overlayFade { from { opacity: 0; } to { opacity: 1; } }

.overlay-inner {
  position: relative;
  width: min(760px, 92vw);
  margin: auto;
  padding: 3rem 3.5rem;
  background: linear-gradient(160deg, rgba(16, 16, 36, 0.95), rgba(8, 8, 20, 0.97));
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 0 80px rgba(102, 126, 234, 0.2);
}

.overlay-glow {
  position: absolute;
  inset: -20% 0 auto 0;
  height: 140%;
  pointer-events: none;
}

.overlay-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2.2rem;
  position: relative;
}

.overlay-idx {
  font-size: 0.7rem;
  letter-spacing: 0.25em;
  color: rgba(255, 255, 255, 0.4);
  font-family: 'Courier New', monospace;
}

.overlay-close {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s;
}

.overlay-close:hover { background: rgba(255, 107, 107, 0.15); border-color: rgba(255, 107, 107, 0.5); color: #ff6b6b; }

.overlay-title {
  position: relative;
  font-size: clamp(1.6rem, 4vw, 2.4rem);
  font-weight: 800;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, #fff, #9db2ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.overlay-desc {
  position: relative;
  font-size: 0.95rem;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 1.6rem;
  max-width: 60ch;
}

.overlay-tags { position: relative; display: flex; flex-wrap: wrap; gap: 0.55rem; margin-bottom: 2.2rem; }

.overlay-feats {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem 1.5rem;
  margin-bottom: 2.4rem;
}

.feat {
  display: flex;
  gap: 0.8rem;
  align-items: center;
  padding: 0.8rem 1rem;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 10px;
}

.feat-num {
  font-size: 0.68rem;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
  letter-spacing: 0.1em;
}

.feat-txt { font-size: 0.8rem; color: rgba(255, 255, 255, 0.75); }

.overlay-cta {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem 1.8rem;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: #fff;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 8px 30px rgba(102, 126, 234, 0.4);
  transition: transform 0.25s cubic-bezier(0.23, 1, 0.32, 1), box-shadow 0.3s;
  transform: translate(var(--mx, 0), var(--my, 0));
}

.overlay-cta:hover { box-shadow: 0 12px 44px rgba(102, 126, 234, 0.6); }

.cta-caret { letter-spacing: -0.1em; }

@media (max-width: 900px) {
  .show-grid { grid-template-columns: 1fr; }
}

@media (max-width: 640px) {
  .showcase { padding: 4rem 1.25rem; }
  .overlay-inner { padding: 2rem 1.5rem; }
  .overlay-feats { grid-template-columns: 1fr; }
}
</style>
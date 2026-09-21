<template>
  <section id="showcase" class="showcase">
    <div class="section-bg">
      <div class="bg-grid"></div>
      <div class="bg-glow"></div>
    </div>

    <div class="section-container">
      <SectionHead num="[ 02 ]" title="核心作品与 Demo 橱窗" sub="FEATURED SHOWCASE" />

      <p class="show-intro">
        六个代表性项目的实机 Demo：从 AI 智能体到 3D 粒子云，从微服务脚手架到量化策略回测，
        每张卡片都嵌入可即时观察的预览动画，点击卡片展开完整介绍。
      </p>

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

            <div class="demo-panel" :class="`demo-${card.type}`">
              <!-- AI 对话打字机 -->
              <template v-if="card.type === 'chat'">
                <div class="chat-row q">Q: {{ card.demo.q }}</div>
                <div class="chat-row a typing" v-for="(line, li) in typedLines[i]" :key="li">{{ line }}</div>
                <span class="caret" v-if="typingIdx[i] < card.demo.a.length"></span>
              </template>

              <!-- 星系 -->
              <template v-else-if="card.type === 'galaxy'">
                <div class="galaxy">
                  <span class="star" v-for="s in 18" :key="s" :style="starStyle(s)"></span>
                  <div class="orbit o1"><div class="planet"></div></div>
                  <div class="orbit o2"><div class="planet p2"></div></div>
                  <div class="orbit o3"><div class="planet p3"></div></div>
                </div>
              </template>

              <!-- 终端 -->
              <template v-else-if="card.type === 'terminal'">
                <div class="term">
                  <div class="term-bar"><span></span><span></span><span></span></div>
                  <div class="term-body">
                    <div class="tl" v-for="(line, li) in typedLines[i]" :key="li"><span class="prompt">$</span> {{ line }}</div>
                    <span class="caret" v-if="typingIdx[i] < card.demo.a.length"></span>
                  </div>
                </div>
              </template>

              <!-- 手势场 -->
              <template v-else-if="card.type === 'gesture'">
                <div class="gesture-field">
                  <div class="wave-line" v-for="w in 5" :key="w"></div>
                  <div class="hand-note">✋ {{ typedLines[i].join(' ') }}<span class="caret"></span></div>
                </div>
              </template>

              <!-- 代码编辑器 -->
              <template v-else-if="card.type === 'code'">
                <div class="code-block">
                  <div class="code-head">
                    <span class="code-tab">▸ strategy.py</span>
                    <span class="code-lang">PYTHON</span>
                  </div>
                  <pre class="code-body"><code v-for="(line, li) in typedLines[i]" :key="li">{{ line }}<span class="caret" v-if="li === typedLines[i].length - 1 && typingIdx[i] < card.demo.a.length"></span></code></pre>
                </div>
              </template>

              <!-- 数据仪表盘 -->
              <template v-else-if="card.type === 'chart'">
                <div class="dashboard">
                  <div class="dash-row">
                    <div class="dash-kpi">
                      <span class="kpi-val">{{ kpi[i].value }}</span>
                      <span class="kpi-label">净值</span>
                    </div>
                    <div class="dash-kpi">
                      <span class="kpi-val up">{{ kpi[i].roi }}%</span>
                      <span class="kpi-label">年化</span>
                    </div>
                  </div>
                  <div class="bars">
                    <span v-for="(b, bi) in bars[i]" :key="bi" class="bar" :style="{ height: b + '%' }"></span>
                  </div>
                  <div class="dash-foot">
                    <span class="dot up"></span><span>实盘运行中</span>
                    <span class="dash-time">{{ dashTime[i] }}</span>
                  </div>
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
// 六张 Demo 卡片：AI 对话 / 星系 / 终端 / 手势 / 代码 / 仪表盘
// 支持 3D 倾斜跟随、光圈 spotlight、全屏展开层（FLIP 动画）
// ============================================================
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import SectionHead from './SectionHead.vue'

gsap.registerPlugin(ScrollTrigger)

const router = useRouter()

// 橱窗卡片配置：标题 / 描述 / 标签 / 跳转路径 / 类型 / 演示文案 / 展开层特性
const cards = [
  {
    type: 'chat',
    title: 'AI 知识库智能体',
    desc: '接入 RAG 检索与 LLM 推理的问答体，支持长期记忆、工具调用与多轮对话。',
    tags: ['FastAPI', 'RAG', 'LLM'],
    path: '/ai',
    demo: { q: '如何快速搭建微服务网关？', a: '引入 Spring Cloud Gateway，配置路由熔断与限流策略，两步即可上线。' },
    features: ['RAG 文档检索与引用溯源', '多轮对话 + 上下文记忆', '函数调用 / 工具编排', '流式输出与语音交互'],
  },
  {
    type: 'galaxy',
    title: '3D 交互粒子云 · 星系模拟',
    desc: '数万粒子在 GPU 中实时演化，鼠标拖拽旋转视角，体验沉浸式星云环绕。',
    tags: ['Three.js', 'WebGL', 'GLSL'],
    path: '/three',
    demo: { q: '', a: '' },
    features: ['GPU 粒子流体模拟', '轨道力学 + 引力牵引', 'Shader 星云着色', '手势拖拽视点控制'],
  },
  {
    type: 'terminal',
    title: 'Spring Cloud 微服务脚手架',
    desc: '一键生成网关、注册中心、配置中心与监控链路，开箱即用的生产级工程模板。',
    tags: ['Spring Cloud', 'Gateway', 'Docker'],
    path: '/architecture',
    demo: { q: '', a: 'mvn clean package -DskipTests\ndocker compose up -d --build\n[OK] Nacos 注册中心已就绪\ngateway 启动成功 -> :8030\n[OK] 6 个微服务全部上线' },
    features: ['Nacos 注册 / 配置中心', 'Gateway 统一网关', 'OpenFeign + 熔断降级', 'Docker Compose 一键编排'],
  },
  {
    type: 'gesture',
    title: '手势控制 3D 虚拟展厅',
    desc: 'MediaPipe 手势捕捉驱动 WebGL 场景，挥手旋转展品、握拳拉近视角。',
    tags: ['MediaPipe', 'WebGL', 'Gesture'],
    path: '/three',
    demo: { q: '', a: '手部关键点 21 个已锁定\n握拳拉近 · 挥手旋转\n展厅视角实时跟随中' },
    features: ['21 点手部关键点追踪', '手势 → 视角映射算法', '近场交互力反馈', '多语言展厅场景'],
  },
  {
    type: 'code',
    title: '量化策略回测平台',
    desc: '多因子选股 + 趋势识别策略，按日/分钟级回测，自动生成净值曲线与归因报告。',
    tags: ['Python', 'Pandas', 'Backtrader'],
    path: '/quant',
    demo: { q: '', a: 'class MomentumStrategy:\n    def on_bar(self, bar):\n        if self.cross_up():\n            self.buy(size=100)\n        elif self.cross_down():\n            self.sell_all()' },
    features: ['多因子打分模型', 'Tick/分钟级撮合引擎', '夏普 / 最大回撤归因', '策略参数网格搜索'],
  },
  {
    type: 'chart',
    title: '实盘数据监控仪表盘',
    desc: 'ECharts + WebSocket 实时驱动，监控策略持仓、PnL 与风险敞口，毫秒级刷新。',
    tags: ['ECharts', 'WebSocket', 'Vue3'],
    path: '/quant',
    demo: { q: '', a: '' },
    features: ['WebSocket 实时行情推送', '持仓与盈亏可视化', '风险敞口热力图', '多策略对比面板'],
  },
]

const cardEls = ref([])
const overlay = reactive({ open: false, index: 0 })
// 每张卡片独立的打字机进度
const typingIdx = ref(cards.map(() => 0))
const typedLines = ref(cards.map(() => []))
// 仪表盘卡片实时数据
const kpi = ref([
  { value: '0', roi: '0' }, { value: '0', roi: '0' }, { value: '0', roi: '0' },
  { value: '0', roi: '0' }, { value: '¥1.28M', roi: '+24.6' }, { value: '¥960K', roi: '+18.2' },
])
const bars = ref(cards.map(() => Array.from({ length: 14 }, () => 20 + Math.random() * 60)))
const dashTime = ref(cards.map(() => '--:--:--'))
let timers = new Map()
let dashTimer = 0
let ctx = null
let lastRect = null

const overlayCard = computed(() => cards[overlay.index])
const overlayGlowStyle = computed(() => ({
  background: `radial-gradient(60% 55% at 50% 0%, rgba(102, 126, 234, 0.22), transparent 70%)`,
}))

function starStyle(i) {
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

function onTiltLeave(e) {
  const el = e.currentTarget
  el.style.transform = ''
  el.style.transition = 'transform 0.5s cubic-bezier(0.23,1,0.32,1)'
}

function go(path) {
  router.push(path)
}

// 启动各卡片的打字机演示
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

// 仪表盘数据滚动刷新（每 1.2s 更新一次）
function startDashboard() {
  if (dashTimer) clearInterval(dashTimer)
  dashTimer = setInterval(() => {
    cards.forEach((card, i) => {
      if (card.type !== 'chart') return
      bars.value[i] = bars.value[i].map(() => 20 + Math.random() * 70)
      const base = 960 + Math.floor(Math.random() * 360)
      kpi.value[i] = {
        value: '¥' + (base + 'K'),
        roi: '+' + (15 + Math.random() * 12).toFixed(1),
      }
      dashTime.value[i] = new Date().toLocaleTimeString('en-US', { hour12: false })
    })
  }, 1200)
}

// 展开全屏层：FLIP 动画从卡片放大到全屏
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
  startDashboard()
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
  if (dashTimer) clearInterval(dashTimer)
  if (ctx) ctx.revert()
})
</script>

<style scoped>
.showcase {
  position: relative;
  padding: 6.5rem 2rem;
  overflow: hidden;
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

.show-intro {
  max-width: 64ch;
  margin: -2rem auto 2.4rem;
  text-align: center;
  font-size: 0.84rem;
  line-height: 1.95;
  color: rgba(255, 255, 255, 0.5);
}

.show-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  perspective: 1300px;
}

.show-card-outer { position: relative; border-radius: 20px; }

.show-card {
  position: relative;
  z-index: 1;
  padding: 1.1rem;
  background: rgba(12, 12, 28, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.1s ease-out, border-color 0.35s, box-shadow 0.35s;
  transform-style: preserve-3d;
  height: 100%;
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
  height: 180px;
  border-radius: 14px;
  overflow: hidden;
  margin-bottom: 1.05rem;
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

.o1 { width: 90px; height: 90px; }
.o2 { width: 130px; height: 130px; animation-duration: 14s; animation-direction: reverse; }
.o3 { width: 170px; height: 170px; animation-duration: 20s; }

@keyframes orbitSpin { from { transform: translate(-50%, -50%) rotate(0deg); } to { transform: translate(-50%, -50%) rotate(360deg); } }

.planet {
  position: absolute;
  left: 50%;
  top: 4px;
  width: 8px;
  height: 8px;
  margin-left: -4px;
  border-radius: 50%;
  background: #00d4ff;
  box-shadow: 0 0 14px rgba(0, 212, 255, 0.9);
}

.planet.p2 { background: #8b7cf6; box-shadow: 0 0 14px rgba(139, 124, 246, 0.9); }
.planet.p3 { background: #ffd166; box-shadow: 0 0 14px rgba(255, 209, 102, 0.9); }

/* 终端 */
.term {
  position: absolute;
  inset: 12px;
  border-radius: 10px;
  background: rgba(4, 4, 12, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.07);
  overflow: hidden;
}

.term-bar {
  display: flex;
  gap: 5px;
  padding: 7px 11px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.term-bar span { width: 7px; height: 7px; border-radius: 50%; background: rgba(255, 255, 255, 0.15); }
.term-bar span:first-child { background: rgba(255, 107, 107, 0.6); }
.term-bar span:nth-child(2) { background: rgba(255, 209, 102, 0.6); }
.term-bar span:nth-child(3) { background: rgba(76, 217, 148, 0.6); }

.term-body {
  padding: 8px 12px;
  font-size: 0.66rem;
  line-height: 1.7;
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
  gap: 0.6rem;
  overflow: hidden;
}

.hand-note {
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.6);
  font-family: 'Courier New', monospace;
}

.wave-line {
  width: 140px;
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

/* 代码编辑器 */
.code-block {
  position: absolute;
  inset: 10px;
  border-radius: 10px;
  background: rgba(4, 4, 12, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.07);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.code-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  font-family: 'Courier New', monospace;
  font-size: 0.66rem;
}

.code-tab { color: #9db2ff; }
.code-lang { color: rgba(255, 209, 102, 0.7); letter-spacing: 0.18em; font-size: 0.58rem; }

.code-body {
  margin: 0;
  padding: 8px 12px;
  font-family: 'Courier New', monospace;
  font-size: 0.64rem;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.78);
  overflow: hidden;
  flex: 1;
}

.code-body code {
  display: block;
  white-space: pre;
}

/* 仪表盘 */
.dashboard {
  position: absolute;
  inset: 10px;
  border-radius: 10px;
  background: rgba(4, 4, 12, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.07);
  display: flex;
  flex-direction: column;
  padding: 10px 12px;
  font-family: 'Courier New', monospace;
}

.dash-row {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.7rem;
}

.dash-kpi { display: flex; flex-direction: column; }

.kpi-val {
  font-size: 0.95rem;
  font-weight: 800;
  color: #00d4ff;
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.55);
}

.kpi-val.up { color: #4cd964; text-shadow: 0 0 8px rgba(76, 217, 100, 0.55); }

.kpi-label {
  font-size: 0.55rem;
  letter-spacing: 0.18em;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 2px;
}

.bars {
  flex: 1;
  display: flex;
  align-items: flex-end;
  gap: 3px;
  padding-top: 4px;
  min-height: 60px;
}

.bar {
  flex: 1;
  background: linear-gradient(180deg, rgba(0, 212, 255, 0.85), rgba(102, 126, 234, 0.45));
  border-radius: 2px 2px 0 0;
  transition: height 0.6s cubic-bezier(0.23, 1, 0.32, 1);
  box-shadow: 0 0 6px rgba(0, 212, 255, 0.35);
}

.dash-foot {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.58rem;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 0.4rem;
  justify-content: space-between;
}

.dash-foot > span:first-child { display: inline-flex; align-items: center; gap: 0.4rem; }

.dot { width: 6px; height: 6px; border-radius: 50%; background: #4cd964; box-shadow: 0 0 6px rgba(76, 217, 100, 0.7); animation: dotPulse 1.4s ease-in-out infinite; }
.dot.up { background: #4cd964; }

@keyframes dotPulse { 50% { opacity: 0.4; } }

.dash-time { letter-spacing: 0.1em; }

/* 卡片内容 */
.show-body { padding: 0 0.4rem 0.4rem; transform: translateZ(14px); }

.show-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.show-title { font-size: 1.05rem; font-weight: 700; color: #fff; }

.show-arrow {
  font-size: 1.2rem;
  color: rgba(255, 255, 255, 0.2);
  transition: all 0.3s;
}

.show-card:hover .show-arrow { color: #00d4ff; transform: translate(3px, -3px); }

.show-desc {
  font-size: 0.78rem;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 0.85rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.show-tags { display: flex; flex-wrap: wrap; gap: 0.45rem; }

.tag {
  font-size: 0.64rem;
  padding: 0.25rem 0.6rem;
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
  margin-top: 1rem;
  padding-top: 0.85rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
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
  padding: 0.45rem 0.95rem;
  font-size: 0.62rem;
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

@media (max-width: 1100px) {
  .show-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .show-grid { grid-template-columns: 1fr; }
  .showcase { padding: 4rem 1.25rem; }
  .overlay-inner { padding: 2rem 1.5rem; }
  .overlay-feats { grid-template-columns: 1fr; }
}
</style>

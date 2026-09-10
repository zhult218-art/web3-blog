<template>
  <section id="tech" class="tech-matrix">
    <div class="section-bg">
      <div class="bg-grid"></div>
      <div class="beam-rays"></div>
    </div>

    <div class="section-container">
      <SectionHead num="[ 01 ]" title="技术矩阵与系统架构" sub="SYSTEM ARCHITECTURE" />

      <div class="matrix-grid">
        <div
          v-for="(item, i) in cards"
          :key="item.title"
          class="matrix-card matrix-card-outer"
          :class="{ 'tilt-wrap': true }"
          data-glow
          @click="go(item.path)"
        >
          <div class="border-beam"></div>
          <div class="matrix-card" ref="cardEls" @mousemove="onTilt" @mouseleave="onTiltLeave">
            <div class="spotlight"></div>
            <div class="card-top">
              <span class="card-idx">0{{ i + 1 }}</span>
              <span class="card-icon">{{ item.icon }}</span>
            </div>
            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-desc">{{ item.desc }}</p>
            <div class="card-tags">
              <span v-for="t in item.tags" :key="t" class="tag">{{ t }}</span>
            </div>
            <div class="card-foot">
              <span>{{ item.mode }}</span>
              <span class="foot-arrow">↗</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
// ============================================================
// 首页"技术矩阵"架构卡片区（TechMatrix）
// 四大技术栈卡片：AI 引擎 / 3D 空间 / 微服务 / 干货开源
// 卡片支持 3D 倾斜、聚光跟随与流光边框，点击跳转对应模块
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import SectionHead from './SectionHead.vue'

gsap.registerPlugin(ScrollTrigger)

const router = useRouter()

// 技术矩阵卡片配置：标题 / 描述 / 标签 / 模式 / 跳转路径
const cards = [
  {
    icon: '🧠',
    title: 'AI 智能引擎',
    desc: 'FastAPI 驱动的多模态 Agent 服务，LLM 推理 + RAG 知识库检索，让知识问答拥有长期记忆与工具调用能力。',
    tags: ['FastAPI', 'LLM', 'RAG', 'Python'],
    mode: 'AI ENGINE',
    path: '/ai',
  },
  {
    icon: '🌌',
    title: '元宇宙 & 3D 空间',
    desc: 'Three.js / WebGL 实时渲染引擎，粒子系统与手势识别驱动，构建可交互的沉浸式虚拟空间。',
    tags: ['Three.js', 'WebGL', 'Hand Gesture'],
    mode: '3D SPACE',
    path: '/three',
  },
  {
    icon: '⚙️',
    title: '微服务分布式后端',
    desc: 'Spring Cloud 全家桶微服务治理，网关统一路由，容器化弹性部署，支撑高并发业务场景。',
    tags: ['Spring Cloud', 'Gateway', 'Docker'],
    mode: 'MICROSERVICE',
    path: '/architecture',
  },
  {
    icon: '📚',
    title: '硬核干货与开源',
    desc: '从架构设计到源码拆解，沉淀可复用的工程实践，持续输出高质量技术内容与开源项目。',
    tags: ['Architecture', 'Open Source'],
    mode: 'DEVOPS',
    path: '/blog',
  },
]

const cardEls = ref([])
let ctx = null

// 点击卡片跳转到对应模块
function go(path) {
  if (path) router.push(path)
}

// 鼠标移动时卡片 3D 倾斜 + 聚光跟随
function onTilt(e) {
  const el = e.currentTarget
  const r = el.getBoundingClientRect()
  const px = (e.clientX - r.left) / r.width - 0.5
  const py = (e.clientY - r.top) / r.height - 0.5
  el.style.transform = `perspective(900px) rotateY(${px * 7}deg) rotateX(${-py * 7}deg) translateY(-3px)`
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

onMounted(() => {
  ctx = gsap.context(() => {
    gsap.from('.matrix-card', {
      y: 70,
      opacity: 0,
      rotateX: -14,
      duration: 1.1,
      stagger: 0.12,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: '.matrix-grid',
        start: 'top 80%',
        once: true,
      },
    })
  })
})

onBeforeUnmount(() => {
  if (ctx) ctx.revert()
})
</script>

<style scoped>
.tech-matrix {
  position: relative;
  padding: 6.5rem 2rem;
  overflow: hidden;
  /* 上下边缘渐隐：与相邻区块自然融合，避免生硬切分 */
  background: linear-gradient(180deg,
    transparent 0%, rgba(10, 10, 26, 0.18) 16%,
    rgba(6, 6, 14, 0.42) 84%, transparent 100%);
}

.section-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image: none;
  mask-image: radial-gradient(ellipse 80% 70% at 50% 30%, #000 20%, transparent 75%);
}

/* Background Beams —— 三条斜向流光 */
.beam-rays {
  position: absolute;
  inset: -30% -10%;
  background:
    linear-gradient(115deg, transparent 42%, rgba(102, 126, 234, 0.09) 47%, rgba(102, 126, 234, 0.03) 52%, transparent 57%),
    linear-gradient(65deg, transparent 58%, rgba(0, 212, 255, 0.07) 63%, rgba(0, 212, 255, 0.02) 68%, transparent 73%),
    linear-gradient(140deg, transparent 70%, rgba(118, 75, 162, 0.08) 75%, transparent 80%);
  animation: beamsShift 9s ease-in-out infinite alternate;
}

@keyframes beamsShift {
  from { transform: translateX(-2%) rotate(0.5deg); }
  to { transform: translateX(2%) rotate(-0.5deg); }
}

.section-container {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
}

.matrix-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.5rem;
  perspective: 1200px;
}

.matrix-card-outer {
  position: relative;
  border-radius: 18px;
}

.matrix-card {
  position: relative;
  z-index: 1;
  padding: 1.75rem 1.5rem;
  background: rgba(12, 12, 28, 0.65);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.1s ease-out, border-color 0.35s, box-shadow 0.35s;
  transform-style: preserve-3d;
  will-change: transform;
}

.matrix-card:hover {
  border-color: rgba(102, 126, 234, 0.45);
  box-shadow:
    0 0 34px rgba(102, 126, 234, 0.18),
    0 22px 50px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

/* Spotlight —— 光斑跟随鼠标 */
.spotlight {
  position: absolute;
  inset: 0;
  background: radial-gradient(280px circle at var(--spot-x, 50%) var(--spot-y, 50%), rgba(102, 126, 234, 0.16), transparent 70%);
  opacity: 0;
  transition: opacity 0.35s;
  pointer-events: none;
}

.matrix-card:hover .spotlight { opacity: 1; }

/* Border Beam —— 流光边框 */
.border-beam {
  position: absolute;
  inset: 0;
  border-radius: 18px;
  padding: 1.5px;
  background: conic-gradient(from var(--beam-angle, 0deg),
    transparent 0deg, transparent 300deg,
    rgba(102, 126, 234, 0.9) 345deg, rgba(0, 212, 255, 0.8) 360deg);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask-composite: exclude;
  opacity: 0;
  transition: opacity 0.4s;
  animation: beamSpin 4.5s linear infinite;
  pointer-events: none;
}

@property --beam-angle {
  syntax: '<angle>';
  initial-value: 0deg;
  inherits: false;
}

@keyframes beamSpin {
  from { --beam-angle: 0deg; }
  to { --beam-angle: 360deg; }
}

.matrix-card-outer:hover .border-beam { opacity: 1; }

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.1rem;
  transform: translateZ(24px);
}

.card-idx {
  font-size: 0.7rem;
  letter-spacing: 0.2em;
  color: rgba(255, 255, 255, 0.25);
  font-family: 'Courier New', monospace;
}

.card-icon {
  font-size: 1.7rem;
  filter: drop-shadow(0 0 12px rgba(102, 126, 234, 0.55));
}

.card-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 0.7rem;
  transform: translateZ(16px);
}

.card-desc {
  font-size: 0.83rem;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 1.25rem;
  transform: translateZ(8px);
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  transform: translateZ(12px);
}

.tag {
  font-size: 0.66rem;
  letter-spacing: 0.06em;
  padding: 0.28rem 0.6rem;
  border-radius: 999px;
  color: #8fa3ff;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.25);
  font-family: 'Courier New', monospace;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.4rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 0.62rem;
  letter-spacing: 0.22em;
  color: rgba(255, 255, 255, 0.3);
  font-family: 'Courier New', monospace;
}

.foot-arrow {
  color: rgba(102, 126, 234, 0.7);
  transition: transform 0.3s;
}

.matrix-card:hover .foot-arrow { transform: translate(2px, -2px); }

@media (max-width: 1100px) {
  .matrix-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .matrix-grid { grid-template-columns: 1fr; }
  .tech-matrix { padding: 4rem 1.25rem; }
}
</style>
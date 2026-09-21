<template>
  <section id="tech" class="tech-matrix">
    <div class="section-bg">
      <div class="bg-grid"></div>
      <div class="beam-rays"></div>
    </div>

    <div class="section-container">
      <SectionHead num="[ 01 ]" title="技术矩阵与系统架构" sub="SYSTEM ARCHITECTURE" />

      <!-- 4 大架构支柱 -->
      <div class="matrix-grid">
        <div
          v-for="(item, i) in cards"
          :key="item.title"
          class="matrix-card-outer"
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

      <!-- 技能矩阵：6 大分类 + 进度条 -->
      <div class="skill-matrix" v-reveal>
        <div class="sm-head">
          <span class="sm-tag">SKILL MATRIX</span>
          <h3 class="sm-title">技能矩阵 · 熟练度图谱</h3>
          <p class="sm-sub">按技术栈分类，星级与进度条双重维度展示能力分布</p>
        </div>

        <div class="sm-grid">
          <article
            v-for="(group, gi) in skillGroups"
            :key="group.name"
            class="sm-panel motions-reveal"
            :class="`delay-${(gi % 4) * 100 + 100}`"
            data-glow
          >
            <div class="sm-panel-head">
              <span class="sm-icon">{{ group.icon }}</span>
              <div>
                <h4 class="sm-name">{{ group.name }}</h4>
                <span class="sm-en">{{ group.en }}</span>
              </div>
              <span class="sm-count">{{ group.items.length }} 项</span>
            </div>

            <div class="sm-skills">
              <div v-for="(s, si) in group.items" :key="s.name" class="skill-row">
                <div class="skill-row-top">
                  <span class="skill-name">{{ s.name }}</span>
                  <span class="skill-stars">
                    <i v-for="n in 5" :key="n" class="star" :class="{ on: n <= s.stars }">★</i>
                  </span>
                </div>
                <div class="skill-bar">
                  <span class="skill-bar-fill" :style="{ '--lvl': s.level + '%' }"></span>
                  <span class="skill-bar-glow"></span>
                </div>
                <div class="skill-row-foot">
                  <span class="skill-level">{{ levelLabel(s.level) }}</span>
                  <span class="skill-pct">{{ s.level }}%</span>
                </div>
              </div>
            </div>
          </article>
        </div>

        <!-- 工具链横向滚动条 -->
        <div class="toolchain" v-reveal>
          <span class="tc-label">TOOLCHAIN</span>
          <div class="tc-track">
            <span v-for="t in toolchain" :key="t" class="tc-chip">
              <span class="tc-dot"></span>{{ t }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
// ============================================================
// 首页"技术矩阵"架构卡片区（TechMatrix）
// 4 大架构支柱卡片（AI / 3D / 微服务 / 开源） + 6 大技能矩阵
// 卡片支持 3D 倾斜、聚光跟随与流光边框，点击跳转对应模块
// 技能矩阵：6 大类 × 4-6 项技能，星级 + 进度条 + 等级标签
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import SectionHead from './SectionHead.vue'

gsap.registerPlugin(ScrollTrigger)

const router = useRouter()

// 4 大架构支柱卡片配置
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

// 6 大技能分类矩阵：前端 / 后端 / 数据库 / DevOps / AI / 工具
const skillGroups = [
  {
    name: '前端工程', en: 'FRONTEND', icon: '🎨',
    items: [
      { name: 'Vue 3 / Composition API', level: 92, stars: 5 },
      { name: 'TypeScript / JavaScript', level: 88, stars: 5 },
      { name: 'TailwindCSS / SCSS', level: 86, stars: 4 },
      { name: 'Three.js / WebGL/GLSL', level: 78, stars: 4 },
      { name: 'Vite / Webpack / Rollup', level: 82, stars: 4 },
    ],
  },
  {
    name: '后端服务', en: 'BACKEND', icon: '🛰️',
    items: [
      { name: 'Spring Boot / Cloud Alibaba', level: 90, stars: 5 },
      { name: 'FastAPI / Python', level: 80, stars: 4 },
      { name: 'Node.js / Express', level: 75, stars: 4 },
      { name: 'WebSocket / gRPC', level: 72, stars: 4 },
    ],
  },
  {
    name: '数据存储', en: 'DATABASE', icon: '🗄️',
    items: [
      { name: 'MySQL / 索引调优', level: 85, stars: 5 },
      { name: 'Redis / 缓存策略', level: 82, stars: 4 },
      { name: 'PostgreSQL', level: 70, stars: 4 },
      { name: 'MongoDB / 向量库', level: 75, stars: 4 },
    ],
  },
  {
    name: 'DevOps', en: 'DEVOPS', icon: '🚀',
    items: [
      { name: 'Docker / Compose', level: 88, stars: 5 },
      { name: 'Kubernetes / K8s', level: 72, stars: 4 },
      { name: 'Jenkins / GitHub Actions', level: 80, stars: 4 },
      { name: 'Nacos / Prometheus / Grafana', level: 78, stars: 4 },
    ],
  },
  {
    name: 'AI 与算法', en: 'AI · ML', icon: '🧠',
    items: [
      { name: 'LLM Prompt / Function Call', level: 86, stars: 5 },
      { name: 'RAG 检索增强生成', level: 82, stars: 4 },
      { name: 'LangChain / Agent 编排', level: 78, stars: 4 },
      { name: 'Python 数据科学栈', level: 80, stars: 4 },
    ],
  },
  {
    name: '工具与协作', en: 'TOOLING', icon: '🛠️',
    items: [
      { name: 'Git / 协作流程', level: 90, stars: 5 },
      { name: 'Figma / 设计还原', level: 76, stars: 4 },
      { name: 'Linux / Shell', level: 82, stars: 4 },
      { name: 'Postman / 性能压测', level: 80, stars: 4 },
    ],
  },
]

// 横向工具链
const toolchain = [
  'Vue 3', 'Vite', 'TypeScript', 'TailwindCSS', 'Three.js', 'GSAP',
  'Spring Boot', 'Spring Cloud', 'FastAPI', 'Node.js',
  'MySQL', 'Redis', 'MongoDB', 'RabbitMQ', 'Nacos',
  'Docker', 'K8s', 'Jenkins', 'Nginx',
  'OpenAI', 'LangChain', 'Hugging Face', 'MediaPipe',
]

const cardEls = ref([])
let ctx = null

// 点击卡片跳转到对应模块
function go(path) {
  if (path) router.push(path)
}

// 根据百分比返回等级标签
function levelLabel(lv) {
  if (lv >= 90) return 'EXPERT'
  if (lv >= 80) return 'ADVANCED'
  if (lv >= 70) return 'PROFICIENT'
  if (lv >= 60) return 'FAMILIAR'
  return 'LEARNING'
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
    // 架构支柱入场
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

    // 技能进度条进入视口时填充
    gsap.utils.toArray('.skill-bar-fill').forEach(bar => {
      const lv = bar.style.getPropertyValue('--lvl') || '0%'
      gsap.set(bar, { width: '0%' })
      gsap.to(bar, {
        width: lv,
        duration: 1.4,
        ease: 'power3.out',
        scrollTrigger: {
          trigger: bar,
          start: 'top 92%',
          once: true,
        },
      })
    })

    // 工具链横向滚动入场
    gsap.from('.tc-chip', {
      y: 18,
      opacity: 0,
      duration: 0.6,
      stagger: 0.04,
      ease: 'power2.out',
      scrollTrigger: {
        trigger: '.toolchain',
        start: 'top 90%',
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

.spotlight {
  position: absolute;
  inset: 0;
  background: radial-gradient(280px circle at var(--spot-x, 50%) var(--spot-y, 50%), rgba(102, 126, 234, 0.16), transparent 70%);
  opacity: 0;
  transition: opacity 0.35s;
  pointer-events: none;
}

.matrix-card:hover .spotlight { opacity: 1; }

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

/* ============================================================
   技能矩阵 SKILL MATRIX
   ============================================================ */
.skill-matrix {
  margin-top: 6rem;
}

.sm-head {
  text-align: center;
  margin-bottom: 2.6rem;
}

.sm-tag {
  font-size: 0.7rem;
  letter-spacing: 0.3em;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.5);
}

.sm-title {
  font-size: clamp(1.5rem, 3vw, 2.1rem);
  font-weight: 800;
  margin: 0.6rem 0 0.4rem;
  background: linear-gradient(135deg, #fff, #9db2ff 60%, #00d4ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sm-sub {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 0.04em;
}

.sm-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.4rem;
}

.sm-panel {
  position: relative;
  padding: 1.5rem 1.5rem 1.6rem;
  background: rgba(12, 12, 28, 0.65);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 18px;
  overflow: hidden;
  transition: border-color 0.35s, box-shadow 0.35s, transform 0.35s;
}

.sm-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.55), transparent);
}

.sm-panel:hover {
  border-color: rgba(102, 126, 234, 0.45);
  box-shadow: 0 0 30px rgba(102, 126, 234, 0.18), 0 18px 40px rgba(0, 0, 0, 0.5);
  transform: translateY(-3px);
}

.sm-panel-head {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding-bottom: 1rem;
  margin-bottom: 1.1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sm-icon {
  font-size: 1.6rem;
  filter: drop-shadow(0 0 10px rgba(102, 126, 234, 0.5));
  flex-shrink: 0;
}

.sm-name {
  font-size: 1rem;
  font-weight: 700;
  color: #fff;
}

.sm-en {
  display: block;
  font-size: 0.6rem;
  letter-spacing: 0.22em;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'Courier New', monospace;
  margin-top: 2px;
}

.sm-count {
  margin-left: auto;
  font-size: 0.62rem;
  letter-spacing: 0.1em;
  padding: 0.25rem 0.55rem;
  border-radius: 999px;
  color: #8fa3ff;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.25);
  font-family: 'Courier New', monospace;
  white-space: nowrap;
}

.sm-skills {
  display: flex;
  flex-direction: column;
  gap: 0.95rem;
}

.skill-row {
  position: relative;
}

.skill-row-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.42rem;
}

.skill-name {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.78);
  font-weight: 500;
}

.skill-stars {
  display: inline-flex;
  gap: 1px;
  font-size: 0.68rem;
  letter-spacing: 1px;
}

.skill-stars .star {
  color: rgba(255, 255, 255, 0.16);
  transition: color 0.3s, text-shadow 0.3s;
}

.skill-stars .star.on {
  color: #ffd166;
  text-shadow: 0 0 6px rgba(255, 209, 102, 0.7);
}

.skill-bar {
  position: relative;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.skill-bar-fill {
  display: block;
  height: 100%;
  width: 0;
  border-radius: 999px;
  background: linear-gradient(90deg, #667eea, #00d4ff 70%, #8b7cf6);
  box-shadow: 0 0 8px rgba(0, 212, 255, 0.55);
  position: relative;
  z-index: 1;
}

.skill-bar-glow {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 30%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.55), transparent);
  animation: barSweep 3.5s ease-in-out infinite;
  z-index: 2;
  pointer-events: none;
}

@keyframes barSweep {
  0% { transform: translateX(-100%); }
  60%, 100% { transform: translateX(420%); }
}

.skill-row-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 0.35rem;
  font-size: 0.6rem;
  letter-spacing: 0.12em;
  color: rgba(255, 255, 255, 0.3);
  font-family: 'Courier New', monospace;
}

.skill-level { color: #8fa3ff; }

/* 工具链 */
.toolchain {
  margin-top: 3rem;
  padding: 1.5rem 1.6rem;
  background: rgba(8, 8, 20, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.tc-label {
  font-size: 0.65rem;
  letter-spacing: 0.28em;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
  white-space: nowrap;
  padding-right: 1rem;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.tc-track {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tc-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.32rem 0.7rem;
  font-size: 0.68rem;
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.62);
  background: rgba(102, 126, 234, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 999px;
  font-family: 'Courier New', monospace;
  transition: all 0.25s ease;
}

.tc-chip:hover {
  color: #fff;
  background: rgba(102, 126, 234, 0.16);
  border-color: rgba(102, 126, 234, 0.5);
  box-shadow: 0 0 12px rgba(102, 126, 234, 0.35);
  transform: translateY(-2px);
}

.tc-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #00d4ff;
  box-shadow: 0 0 6px rgba(0, 212, 255, 0.8);
}

@media (max-width: 1100px) {
  .matrix-grid { grid-template-columns: repeat(2, 1fr); }
  .sm-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .matrix-grid { grid-template-columns: 1fr; }
  .sm-grid { grid-template-columns: 1fr; }
  .tech-matrix { padding: 4rem 1.25rem; }
  .toolchain { flex-direction: column; align-items: flex-start; gap: 0.8rem; }
  .tc-label { border-right: none; padding-right: 0; }
}
</style>

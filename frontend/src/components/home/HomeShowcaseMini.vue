<template>
  <section id="home-showcase-mini" class="showcase-mini">
    <div class="section-bg">
      <div class="bg-glow"></div>
    </div>

    <div class="section-container">
      <SectionHead num="[ 01 ]" title="核心作品预览" sub="FEATURED PROJECTS" />

      <p class="mini-intro">
        三件代表作抢先预览，更多精彩尽在综合项目展示页。
      </p>

      <div class="mini-grid">
        <article
          v-for="(card, i) in cards"
          :key="card.title"
          class="mini-card"
          data-glow
          @click="go(card.path)"
        >
          <div class="card-icon" v-html="card.icon"></div>
          <div class="card-body">
            <h3 class="card-title">{{ card.title }}</h3>
            <p class="card-desc">{{ card.desc }}</p>
            <div class="card-tags">
              <span v-for="t in card.tags" :key="t" class="tag">#{{ t }}</span>
            </div>
          </div>
          <div class="card-cta">
            <span>查看详情</span>
            <span class="cta-arrow">→</span>
          </div>
        </article>
      </div>

      <div class="view-all">
        <router-link to="/project" class="view-all-link" data-glow data-magnetic>
          <span>查看全部项目</span>
          <span class="link-arrow">→</span>
        </router-link>
      </div>

      <!-- 数据统计条：让内容更饱满 -->
      <div class="stats-row">
        <div v-for="s in stats" :key="s.label" class="stat-item">
          <div class="stat-num">{{ s.num }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>

      <!-- 技术栈徽章云 -->
      <div class="tech-cloud">
        <span class="tech-title">技术栈</span>
        <div class="tech-chips">
          <span v-for="t in techs" :key="t" class="tech-chip">{{ t }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
// ============================================================
// 首页"核心作品预览"精简卡片（HomeShowcaseMini）
// 3 件代表作：AI 聊天 / 3D 星系 / 量化图表
// 卡片点击 router.push 跳转对应项目页
// ============================================================
import { useRouter } from 'vue-router'
import SectionHead from './SectionHead.vue'

const router = useRouter()

const cards = [
  {
    title: 'AI 知识库智能体',
    desc: '接入 RAG 检索与 LLM 推理的问答体，支持长期记忆与工具调用。',
    tags: ['FastAPI', 'RAG', 'LLM'],
    path: '/agent',
    icon: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>',
  },
  {
    title: '3D 交互粒子云 · 星系模拟',
    desc: '数万粒子在 GPU 中实时演化，鼠标拖拽旋转视角，沉浸式星云环绕。',
    tags: ['Three.js', 'WebGL', 'GLSL'],
    path: '/three',
    icon: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="2.5"/><ellipse cx="12" cy="12" rx="10" ry="4"/><ellipse cx="12" cy="12" rx="10" ry="4" transform="rotate(60 12 12)"/><ellipse cx="12" cy="12" rx="10" ry="4" transform="rotate(120 12 12)"/></svg>',
  },
  {
    title: '实盘数据监控仪表盘',
    desc: 'ECharts + WebSocket 实时驱动，监控持仓、PnL 与风险敞口，毫秒级刷新。',
    tags: ['ECharts', 'WebSocket', 'Vue3'],
    path: '/quant',
    icon: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 3v18h18"/><path d="M7 14l3-3 3 3 5-6"/></svg>',
  },
]

function go(path) {
  router.push(path)
}

// 数据统计（展示性数字，让页面更丰满）
const stats = [
  { num: '12+', label: '项目作品' },
  { num: '50+', label: '技术文章' },
  { num: '8', label: '在线服务' },
  { num: '99.9%', label: '可用率' },
]

// 技术栈徽章
const techs = [
  'Vue3', 'TypeScript', 'Spring Cloud', 'Three.js', 'WebGL',
  'Python', 'FastAPI', 'ECharts', 'TailwindCSS', 'Docker',
  'Redis', 'MySQL', 'Nacos', 'RAG', 'LLM',
]
</script>

<style scoped>
.showcase-mini {
  position: relative;
  padding: 6rem 2rem;
  overflow: hidden;
  background: linear-gradient(180deg,
    transparent 0%, rgba(6, 6, 14, 0.15) 16%,
    rgba(10, 10, 26, 0.32) 84%, transparent 100%);
}

.section-bg { position: absolute; inset: 0; z-index: 0; pointer-events: none; }

.bg-glow {
  position: absolute;
  width: 620px;
  height: 620px;
  left: 50%;
  top: 40%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.06), rgba(102, 126, 234, 0.04) 40%, transparent 70%);
  filter: blur(90px);
}

.section-container { position: relative; z-index: 1; max-width: 1200px; margin: 0 auto; }

.mini-intro {
  max-width: 60ch;
  margin: -2rem auto 2.4rem;
  text-align: center;
  font-size: 0.84rem;
  line-height: 1.95;
  color: rgba(255, 255, 255, 0.5);
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
}

.mini-card {
  position: relative;
  padding: 1.6rem 1.4rem 1.2rem;
  background: rgba(12, 12, 28, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1), border-color 0.35s, box-shadow 0.35s;
}

.mini-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(340px circle at 50% 0%, rgba(0, 212, 255, 0.08), transparent 70%);
  opacity: 0;
  transition: opacity 0.35s;
  pointer-events: none;
}

.mini-card:hover {
  transform: translateY(-6px);
  border-color: rgba(0, 212, 255, 0.4);
  box-shadow:
    0 0 40px rgba(0, 212, 255, 0.14),
    0 26px 60px rgba(0, 0, 0, 0.5);
}

.mini-card:hover::before { opacity: 1; }

.card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  color: #67e8f9;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.12), rgba(102, 126, 234, 0.06));
  border: 1px solid rgba(0, 212, 255, 0.25);
  box-shadow: 0 0 18px rgba(0, 212, 255, 0.18);
}

.card-body { display: flex; flex-direction: column; gap: 0.55rem; flex: 1; }

.card-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #fff;
}

.card-desc {
  font-size: 0.78rem;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.55);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.2rem;
}

.tag {
  font-size: 0.62rem;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  color: #7fd9ff;
  background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.22);
  font-family: 'Courier New', monospace;
}

.card-cta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 0.85rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 0.72rem;
  letter-spacing: 0.15em;
  color: #9db2ff;
  font-family: 'Courier New', monospace;
  transition: color 0.3s;
}

.mini-card:hover .card-cta { color: #67e8f9; }

.cta-arrow {
  transition: transform 0.3s;
}

.mini-card:hover .cta-arrow { transform: translateX(4px); }

.view-all {
  margin-top: 2.6rem;
  text-align: center;
}

.view-all-link {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.7rem 1.6rem;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: #67e8f9;
  background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.3);
  border-radius: 999px;
  text-decoration: none;
  font-family: 'Courier New', monospace;
  transition: all 0.3s cubic-bezier(0.23, 1, 0.32, 1);
}

.view-all-link:hover {
  color: #fff;
  background: rgba(0, 212, 255, 0.18);
  border-color: rgba(0, 212, 255, 0.6);
  box-shadow: 0 0 28px rgba(0, 212, 255, 0.4);
}

.link-arrow { transition: transform 0.3s; }

.view-all-link:hover .link-arrow { transform: translateX(4px); }

@media (max-width: 1024px) {
  .mini-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .showcase-mini { padding: 4rem 1.25rem; }
  .mini-grid { grid-template-columns: 1fr; }
}

/* 数据统计条 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-top: 3rem;
  padding: 1.5rem;
  background: rgba(12, 12, 28, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  backdrop-filter: blur(8px);
}

.stat-item {
  text-align: center;
}

.stat-num {
  font-size: 1.8rem;
  font-weight: 800;
  background: linear-gradient(135deg, #67e8f9, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-family: 'Courier New', monospace;
}

.stat-label {
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.45);
  margin-top: 4px;
  letter-spacing: 0.05em;
}

/* 技术栈云 */
.tech-cloud {
  margin-top: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center;
}

.tech-title {
  font-size: 0.72rem;
  letter-spacing: 0.2em;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'Courier New', monospace;
}

.tech-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
}

.tech-chip {
  font-size: 0.68rem;
  padding: 0.25rem 0.7rem;
  border-radius: 999px;
  color: #9db2ff;
  background: rgba(102, 126, 234, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.2);
  font-family: 'Courier New', monospace;
  transition: all 0.3s;
}

.tech-chip:hover {
  color: #fff;
  background: rgba(0, 212, 255, 0.15);
  border-color: rgba(0, 212, 255, 0.4);
  transform: translateY(-2px);
}

@media (max-width: 640px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
</style>

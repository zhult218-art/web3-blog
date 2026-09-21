<template>
  <div class="about-page" v-reveal>
    <!-- 背景层 -->
    <div class="about-bg" aria-hidden="true">
      <div class="bg-grid-overlay"></div>
      <div class="bg-glow-a"></div>
      <div class="bg-glow-b"></div>
    </div>

    <div class="about-container">
      <!-- ============================================================
           1. HERO 区
           ============================================================ -->
      <section class="hero motions-reveal">
        <div class="hero-bg-ring"></div>
        <div class="avatar">
          <div class="avatar-ring a"></div>
          <div class="avatar-ring b"></div>
          <img src="/images/avatar.jpg" alt="AURORA-朱" class="avatar-core" />
        </div>
        <span class="hero-badge">◈ IDENTITY · v2.0</span>
        <h1 class="hero-name">AURORA-朱</h1>
        <p class="hero-tagline">
          以代码构建无限可能 · 让每一个想法都拥有沉浸式表达
        </p>
        <div class="hero-socials">
          <a href="https://github.com/zhult218-art" target="_blank" rel="noopener" class="sc-btn" data-glow>
            <span>🐙</span> GitHub
          </a>
          <a href="mailto:zhult218@gmail.com" class="sc-btn" data-glow>
            <span>✉️</span> Gmail
          </a>
          <a href="mailto:m17357515408@163.com" class="sc-btn" data-glow>
            <span>📮</span> 163 邮箱
          </a>
          <router-link to="/comments" class="sc-btn" data-glow>
            <span>💬</span> 留言板
          </router-link>
        </div>
        <div class="hero-coords">
          <span>SYS.LAT 39.9042°N</span>
          <span class="divider">│</span>
          <span>LNG 116.4074°E</span>
          <span class="divider">│</span>
          <span class="hero-status">● ONLINE</span>
        </div>
      </section>

      <!-- ============================================================
           2. 关于我
           ============================================================ -->
      <section class="block motions-reveal" v-reveal>
        <div class="block-head">
          <span class="block-num">[ 01 ]</span>
          <h2 class="block-title">关于我</h2>
          <span class="block-en">ABOUT</span>
        </div>
        <div class="glass-panel about-text">
          <!-- 滚动打字机 · 进度映射字符：随下滑逐字写出，倒滚回收 -->
          <ScrollTypewriter :text="aboutText" />
        </div>
      </section>

      <!-- ============================================================
           3. 技能矩阵
           ============================================================ -->
      <section class="block motions-reveal" v-reveal>
        <div class="block-head">
          <span class="block-num">[ 02 ]</span>
          <h2 class="block-title">技能矩阵</h2>
          <span class="block-en">SKILL MATRIX</span>
        </div>
        <div class="skills-grid">
          <article v-for="g in skillGroups" :key="g.name" class="glass-panel-sm skill-panel">
            <div class="sp-head">
              <span class="sp-icon">{{ g.icon }}</span>
              <div>
                <h3 class="sp-name">{{ g.name }}</h3>
                <span class="sp-en">{{ g.en }}</span>
              </div>
            </div>
            <div class="sp-items">
              <div v-for="s in g.items" :key="s.name" class="sp-item">
                <div class="sp-item-top">
                  <span class="sp-item-name">{{ s.name }}</span>
                  <span class="sp-stars">
                    <i v-for="n in 5" :key="n" class="star" :class="{ on: n <= s.stars }">★</i>
                  </span>
                </div>
                <div class="sp-bar">
                  <span class="sp-bar-fill" :style="{ width: s.level + '%' }"></span>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>

      <!-- ============================================================
           4. 时间线
           ============================================================ -->
      <section class="block motions-reveal" v-reveal>
        <div class="block-head">
          <span class="block-num">[ 03 ]</span>
          <h2 class="block-title">个人时间线</h2>
          <span class="block-en">TIMELINE</span>
        </div>
        <div class="timeline">
          <div v-for="(t, i) in timeline" :key="i" class="tl-node motions-reveal" :class="`delay-${(i % 4) * 100 + 100}`">
            <div class="tl-dot"></div>
            <div class="tl-line" v-if="i < timeline.length - 1"></div>
            <div class="glass-panel-sm tl-card">
              <span class="tl-year">{{ t.year }}</span>
              <h3 class="tl-title">{{ t.title }}</h3>
              <p class="tl-desc">{{ t.desc }}</p>
              <div class="tl-tags">
                <span v-for="tag in t.tags" :key="tag" class="tl-tag">{{ tag }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ============================================================
           5. 项目展示
           ============================================================ -->
      <section class="block motions-reveal" v-reveal>
        <div class="block-head">
          <span class="block-num">[ 04 ]</span>
          <h2 class="block-title">代表作品</h2>
          <span class="block-en">PROJECTS</span>
        </div>
        <div class="projects-grid">
          <article v-for="(p, i) in projects" :key="p.title" class="glass-panel-sm project-card" data-glow>
            <div class="pc-cover" :style="{ background: p.cover }">
              <span class="pc-icon">{{ p.icon }}</span>
              <span class="pc-idx">0{{ i + 1 }}</span>
            </div>
            <div class="pc-body">
              <h3 class="pc-title">{{ p.title }}</h3>
              <p class="pc-desc">{{ p.desc }}</p>
              <div class="pc-tags">
                <span v-for="t in p.tags" :key="t" class="pc-tag">{{ t }}</span>
              </div>
              <router-link v-if="p.path" :to="p.path" class="pc-link">
                查看项目 <span class="pc-arrow">↗</span>
              </router-link>
            </div>
          </article>
        </div>
      </section>

      <!-- ============================================================
           6. 数据统计
           ============================================================ -->
      <section class="block motions-reveal" v-reveal>
        <div class="block-head">
          <span class="block-num">[ 05 ]</span>
          <h2 class="block-title">数据仪表盘</h2>
          <span class="block-en">DASHBOARD</span>
        </div>
        <div class="stats-grid">
          <div v-for="(s, i) in dashboard" :key="s.label" class="glass-panel-sm stat-card">
            <span class="stat-icon">{{ s.icon }}</span>
            <span class="stat-value">{{ s.value }}</span>
            <span class="stat-label">{{ s.label }}</span>
            <span class="stat-bar"><span :style="{ width: s.bar + '%' }"></span></span>
          </div>
        </div>
      </section>

      <!-- ============================================================
           7. 兴趣爱好
           ============================================================ -->
      <section class="block motions-reveal" v-reveal>
        <div class="block-head">
          <span class="block-num">[ 06 ]</span>
          <h2 class="block-title">兴趣爱好</h2>
          <span class="block-en">INTERESTS</span>
        </div>
        <div class="interests-cloud">
          <span v-for="(it, i) in interests" :key="it" class="ic-chip" :class="`ic-size-${(i % 3) + 1}`">
            {{ it }}
          </span>
        </div>
      </section>

      <!-- ============================================================
           8. 联系方式
           ============================================================ -->
      <section class="block motions-reveal" v-reveal>
        <div class="block-head">
          <span class="block-num">[ 07 ]</span>
          <h2 class="block-title">联系方式</h2>
          <span class="block-en">CONTACT</span>
        </div>
        <div class="glass-panel contact-panel">
          <p class="contact-intro">如有问题、合作意向或想交流技术，欢迎通过以下方式联系：</p>
          <div class="contact-grid">
            <a href="https://github.com/zhult218-art" target="_blank" rel="noopener" class="contact-card" data-glow>
              <span class="cc-icon">🐙</span>
              <div class="cc-text">
                <span class="cc-label">GitHub</span>
                <span class="cc-value">github.com/zhult218-art</span>
              </div>
              <span class="cc-arrow">↗</span>
            </a>
            <a href="mailto:zhult218@gmail.com" class="contact-card" data-glow>
              <span class="cc-icon">✉️</span>
              <div class="cc-text">
                <span class="cc-label">Gmail</span>
                <span class="cc-value">zhult218@gmail.com</span>
              </div>
              <span class="cc-arrow">↗</span>
            </a>
            <a href="mailto:m17357515408@163.com" class="contact-card" data-glow>
              <span class="cc-icon">📮</span>
              <div class="cc-text">
                <span class="cc-label">163 邮箱</span>
                <span class="cc-value">m17357515408@163.com</span>
              </div>
              <span class="cc-arrow">↗</span>
            </a>
            <router-link to="/comments" class="contact-card" data-glow>
              <span class="cc-icon">💬</span>
              <div class="cc-text">
                <span class="cc-label">留言板</span>
                <span class="cc-value">在线留言，期待你的消息</span>
              </div>
              <span class="cc-arrow">↗</span>
            </router-link>
          </div>
        </div>
      </section>

      <!-- ============================================================
           9. 站点导览
           ============================================================ -->
      <section class="block motions-reveal" v-reveal>
        <div class="block-head">
          <span class="block-num">[ 08 ]</span>
          <h2 class="block-title">站点导览</h2>
          <span class="block-en">SITE NAVIGATION</span>
        </div>
        <div class="modules-grid">
          <router-link v-for="m in modules" :key="m.path" :to="m.path" class="module-card" data-glow>
            <div class="mc-icon">{{ m.icon }}</div>
            <div class="mc-text">
              <div class="mc-name">{{ m.name }}</div>
              <div class="mc-sub">{{ m.sub }}</div>
            </div>
            <span class="mc-arrow">→</span>
          </router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 关于页：沉浸式个人展示页
// 九大板块：Hero / 关于我 / 技能矩阵 / 时间线 / 项目展示 /
// 数据仪表盘 / 兴趣爱好 / 联系方式 / 站点导览
// 所有板块使用 v-reveal + motions-reveal 触发滚动入场动画
// ============================================================

import ScrollTypewriter from '@/components/ScrollTypewriter.vue'

// 「关于我」滚动打字机文本（随下滑逐字写出，倒滚回收）
const aboutText = `我是一名沉迷于「沉浸式 Web 体验」的前端工程师与全栈爱好者。
从 Vue 3 的 Composition API 到 Three.js 的 GLSL 着色器，从 Spring Cloud 微服务到 FastAI 驱动的 LLM Agent，
我始终相信代码不仅是工具，更是一种表达——它能把抽象的思维变成可触摸的视觉与交互。

这里的每一行代码、每一篇文章、每一个 Demo，都来自日常的真实积累：
没有速成的捷径，也没有轻松成功的保证。我把学习中遇到的问题与解法沉淀在这个站点，
希望对正在探索同样路径的你，能有一点帮助或灵感。

当前阶段，我正在深入 AI Agent 编排、RAG 检索增强与 WebGL 实时渲染的结合，
探索如何让 Web 不只是「展示信息的容器」，而是「承载智能与感知的现场」。
如果你也在做类似的事情，欢迎在留言板或邮箱聊聊。`

const skillGroups = [
  {
    name: '前端工程', en: 'FRONTEND', icon: '🎨',
    items: [
      { name: 'Vue 3 / Composition API', level: 92, stars: 5 },
      { name: 'TypeScript / JavaScript', level: 88, stars: 5 },
      { name: 'TailwindCSS / SCSS', level: 86, stars: 4 },
      { name: 'Three.js / WebGL', level: 78, stars: 4 },
      { name: 'Vite / Webpack', level: 82, stars: 4 },
    ],
  },
  {
    name: '后端服务', en: 'BACKEND', icon: '🛰️',
    items: [
      { name: 'Spring Boot / Cloud', level: 90, stars: 5 },
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
      { name: 'Jenkins / Actions', level: 80, stars: 4 },
      { name: 'Nacos / Grafana', level: 78, stars: 4 },
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

// 时间线：至少 4 个节点
const timeline = [
  {
    year: '2019',
    title: '初识编程 · 自学起步',
    desc: '从 Python 入门，写第一个爬虫与自动化脚本，开启「用代码解决问题」的旅程。',
    tags: ['Python', '自学', '爬虫'],
  },
  {
    year: '2021',
    title: '深入 Java 生态 · 后端工程化',
    desc: '系统学习 Spring 全家桶，从单体到微服务，理解分布式系统中的注册、配置、熔断、限流。',
    tags: ['Java', 'Spring Cloud', '微服务'],
  },
  {
    year: '2023',
    title: '前端沉浸 · Vue3 + Three.js',
    desc: '转向 Vue 3 组合式 API 与 Three.js 实时渲染，专注沉浸式 Web 体验与动效叙事。',
    tags: ['Vue3', 'Three.js', 'GSAP'],
  },
  {
    year: '2024',
    title: 'AI 探索 · LLM Agent 与 RAG',
    desc: '接入大模型推理与向量检索，搭建具备长期记忆与工具调用能力的知识库智能体。',
    tags: ['LLM', 'RAG', 'Agent'],
  },
  {
    year: '2026',
    title: '当下 · 综合门户与开源沉淀',
    desc: '将所有学习与项目沉淀为这个 Web3 门户站点，持续输出技术文章与开源实践。',
    tags: ['Aurora-朱', 'Open Source', '持续输出'],
  },
]

// 代表作品（占位，用户后期填充）
const projects = [
  {
    icon: '🧠',
    title: 'AI 知识库智能体',
    desc: '接入 RAG 检索与 LLM 推理的问答体，支持长期记忆、工具调用与多轮对话。',
    tags: ['FastAPI', 'RAG', 'LLM'],
    path: '/ai',
    cover: 'linear-gradient(135deg, #1c1240 0%, #2a1a5e 45%, #4a2a8a 100%)',
  },
  {
    icon: '🌌',
    title: '3D 粒子云 · 星系模拟',
    desc: '数万粒子在 GPU 中实时演化，鼠标拖拽旋转视角，沉浸式星云环绕。',
    tags: ['Three.js', 'WebGL', 'GLSL'],
    path: '/three',
    cover: 'linear-gradient(135deg, #061a2c 0%, #0a2840 45%, #0e4a6e 100%)',
  },
  {
    icon: '⚙️',
    title: 'Spring Cloud 脚手架',
    desc: '一键生成网关、注册中心、配置中心与监控链路，生产级微服务工程模板。',
    tags: ['Spring Cloud', 'Gateway', 'Docker'],
    path: '/architecture',
    cover: 'linear-gradient(135deg, #2c0a2e 0%, #4a1040 45%, #7a1e58 100%)',
  },
  {
    icon: '💹',
    title: '量化策略回测平台',
    desc: '多因子选股 + 趋势识别策略，按日/分钟级回测，自动生成归因报告。',
    tags: ['Python', 'Pandas', 'Backtrader'],
    path: '/quant',
    cover: 'linear-gradient(135deg, #0a2c12 0%, #144a20 45%, #1e7a3a 100%)',
  },
]

// 数据仪表盘
const dashboard = [
  { icon: '💻', value: '128K', label: '代码行数', bar: 82 },
  { icon: '🚀', value: '40+', label: '完成项目', bar: 76 },
  { icon: '📝', value: '120+', label: '技术文章', bar: 68 },
  { icon: '🔥', value: '640', label: '贡献天数', bar: 88 },
  { icon: '⭐', value: '2.1K', label: 'GitHub Stars', bar: 72 },
  { icon: '🏆', value: '15', label: '开源仓库', bar: 60 },
]

// 兴趣爱好标签云
const interests = [
  '🎬 电影', '🎵 音乐', '📚 阅读', '🎮 游戏', '☕ 咖啡',
  '🧗 攀岩', '📷 摄影', '🌌 天文', '🎲 桌游', '🪴 养植',
  '🍳 烹饪', '🏃 跑步', '🎧 播客', '🐱 猫咪', '🗺️ 旅行',
  '🔭 观星', '🖌️ 数字绘画',
]

// 站点核心模块快速入口
const modules = [
  { path: '/blog', icon: '📖', name: '技术博客', sub: '深度文章与干货' },
  { path: '/community', icon: '🍺', name: '技术论坛', sub: '社区讨论' },
  { path: '/ai', icon: '🧠', name: 'AI 引擎', sub: '多模态智能助手' },
  { path: '/three', icon: '🌌', name: '3D 空间', sub: 'WebGL 粒子漫游' },
  { path: '/architecture', icon: '⚙️', name: '架构图鉴', sub: '微服务拓扑' },
  { path: '/tools', icon: '🛠️', name: '实用工具', sub: '炼金道具集' },
]
</script>

<style scoped>
.about-page {
  position: relative;
  min-height: 100vh;
  padding: 4rem 1.5rem 5rem;
  color: #e0e0f0;
  overflow-x: hidden;
}

.about-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
}

.bg-grid-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 50% 0%, rgba(168, 85, 247, 0.08), transparent 70%),
    radial-gradient(ellipse 60% 40% at 80% 80%, rgba(6, 182, 212, 0.06), transparent 70%);
}

.bg-glow-a, .bg-glow-b {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
}

.bg-glow-a {
  width: 600px;
  height: 600px;
  top: -10%;
  left: -10%;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.15), transparent 70%);
  animation: glowFloat 18s ease-in-out infinite;
}

.bg-glow-b {
  width: 500px;
  height: 500px;
  bottom: -10%;
  right: -10%;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.12), transparent 70%);
  animation: glowFloat 22s ease-in-out infinite reverse;
}

@keyframes glowFloat {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(40px, 30px); }
}

.about-container {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 0 auto;
}

/* ============================================================
   1. HERO
   ============================================================ */
.hero {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 3rem 1rem 4rem;
  margin-bottom: 4rem;
}

.hero-bg-ring {
  position: absolute;
  top: 0;
  left: 50%;
  width: 380px;
  height: 380px;
  transform: translateX(-50%);
  border: 1px solid rgba(168, 85, 247, 0.18);
  border-radius: 50%;
  animation: ringRotate 30s linear infinite;
}

.hero-bg-ring::before {
  content: '';
  position: absolute;
  inset: 30px;
  border: 1px dashed rgba(6, 182, 212, 0.15);
  border-radius: 50%;
}

@keyframes ringRotate {
  to { transform: translateX(-50%) rotate(360deg); }
}

.avatar {
  position: relative;
  width: 140px;
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.avatar-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid transparent;
}

.avatar-ring.a {
  border-color: rgba(168, 85, 247, 0.6);
  animation: ringSpin 8s linear infinite;
}

.avatar-ring.b {
  inset: 8px;
  border: 1px dashed rgba(6, 182, 212, 0.4);
  animation: ringSpin 5s linear infinite reverse;
}

@keyframes ringSpin {
  to { transform: rotate(360deg); }
}

.avatar-core {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  box-shadow: 0 0 40px rgba(168, 85, 247, 0.55), inset 0 4px 16px rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.15);
}

.hero-badge {
  font-size: 0.65rem;
  letter-spacing: 0.28em;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
  padding: 0.35rem 0.85rem;
  border: 1px solid rgba(0, 212, 255, 0.3);
  border-radius: 999px;
  background: rgba(0, 212, 255, 0.05);
  margin-bottom: 1rem;
}

.hero-name {
  font-size: clamp(2.5rem, 6vw, 4.5rem);
  font-weight: 900;
  letter-spacing: 0.08em;
  background: linear-gradient(135deg, #06b6d4 0%, #a855f7 50%, #ec4899 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 0.6rem;
  text-shadow: 0 0 60px rgba(168, 85, 247, 0.3);
}

.hero-tagline {
  font-size: 0.92rem;
  color: rgba(255, 255, 255, 0.65);
  letter-spacing: 0.04em;
  max-width: 56ch;
  margin-bottom: 1.6rem;
  line-height: 1.75;
}

.hero-socials {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  justify-content: center;
  margin-bottom: 2rem;
}

.sc-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.55rem 1rem;
  font-size: 0.74rem;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(12, 12, 28, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  text-decoration: none;
  transition: all 0.3s;
}

.sc-btn:hover {
  color: #fff;
  border-color: rgba(168, 85, 247, 0.6);
  background: rgba(168, 85, 247, 0.12);
  box-shadow: 0 0 20px rgba(168, 85, 247, 0.35);
  transform: translateY(-2px);
}

.hero-coords {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  justify-content: center;
  font-size: 0.66rem;
  letter-spacing: 0.15em;
  color: rgba(255, 255, 255, 0.4);
  font-family: 'Courier New', monospace;
}

.hero-coords .divider { color: rgba(255, 255, 255, 0.18); }

.hero-status { color: #4cd964; }

/* ============================================================
   通用板块
   ============================================================ */
.block {
  margin-bottom: 4rem;
}

.block-head {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  padding-bottom: 0.8rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.block-num {
  font-size: 0.7rem;
  letter-spacing: 0.2em;
  color: #a855f7;
  font-family: 'Courier New', monospace;
  text-shadow: 0 0 8px rgba(168, 85, 247, 0.5);
}

.block-title {
  font-size: clamp(1.4rem, 3vw, 2rem);
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #fff, #9db2ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.block-en {
  margin-left: auto;
  font-size: 0.62rem;
  letter-spacing: 0.28em;
  color: rgba(255, 255, 255, 0.3);
  font-family: 'Courier New', monospace;
}

/* ============================================================
   2. 关于我
   ============================================================ */
.about-text {
  padding: 2rem 2.2rem;
}

.about-text p {
  font-size: 0.9rem;
  line-height: 1.95;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 1rem;
}

.about-text p:last-child { margin-bottom: 0; }

/* ============================================================
   3. 技能矩阵
   ============================================================ */
.skills-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.2rem;
}

.skill-panel {
  padding: 1.4rem 1.4rem 1.5rem;
}

.sp-head {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding-bottom: 0.85rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sp-icon { font-size: 1.4rem; }

.sp-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #fff;
}

.sp-en {
  display: block;
  font-size: 0.55rem;
  letter-spacing: 0.2em;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'Courier New', monospace;
  margin-top: 2px;
}

.sp-items {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.sp-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.35rem;
}

.sp-item-name {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.75);
}

.sp-stars {
  display: inline-flex;
  gap: 1px;
  font-size: 0.62rem;
}

.sp-stars .star { color: rgba(255, 255, 255, 0.16); }
.sp-stars .star.on {
  color: #ffd166;
  text-shadow: 0 0 5px rgba(255, 209, 102, 0.6);
}

.sp-bar {
  height: 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.sp-bar-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #a855f7, #06b6d4 70%, #ec4899);
  box-shadow: 0 0 8px rgba(6, 182, 212, 0.5);
}

/* ============================================================
   4. 时间线
   ============================================================ */
.timeline {
  position: relative;
  padding-left: 1.5rem;
}

.tl-node {
  position: relative;
  padding-left: 2rem;
  padding-bottom: 1.4rem;
}

.tl-dot {
  position: absolute;
  left: -5px;
  top: 8px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a855f7, #06b6d4);
  box-shadow: 0 0 12px rgba(168, 85, 247, 0.7);
  z-index: 2;
}

.tl-line {
  position: absolute;
  left: 0;
  top: 20px;
  width: 2px;
  height: 100%;
  background: linear-gradient(180deg, rgba(168, 85, 247, 0.4), transparent);
  z-index: 1;
}

.tl-card {
  padding: 1rem 1.2rem 1.1rem;
}

.tl-year {
  font-size: 0.66rem;
  letter-spacing: 0.2em;
  color: #00d4ff;
  font-family: 'Courier New', monospace;
  padding: 0.18rem 0.5rem;
  background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.25);
  border-radius: 999px;
  display: inline-block;
  margin-bottom: 0.5rem;
}

.tl-title {
  font-size: 1rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 0.4rem;
}

.tl-desc {
  font-size: 0.78rem;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.55);
  margin-bottom: 0.7rem;
}

.tl-tags { display: flex; flex-wrap: wrap; gap: 0.35rem; }

.tl-tag {
  font-size: 0.6rem;
  padding: 0.18rem 0.5rem;
  border-radius: 999px;
  color: #8fa3ff;
  background: rgba(168, 85, 247, 0.1);
  border: 1px solid rgba(168, 85, 247, 0.25);
  font-family: 'Courier New', monospace;
}

/* ============================================================
   5. 项目展示
   ============================================================ */
.projects-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.2rem;
}

.project-card {
  overflow: hidden;
  transition: transform 0.35s, border-color 0.35s, box-shadow 0.35s;
}

.project-card:hover {
  transform: translateY(-4px);
  border-color: rgba(168, 85, 247, 0.5);
  box-shadow: 0 0 30px rgba(168, 85, 247, 0.25), 0 12px 30px rgba(0, 0, 0, 0.4);
}

.pc-cover {
  position: relative;
  height: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.pc-cover::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.12), transparent 50%),
    radial-gradient(circle at 75% 80%, rgba(0, 212, 255, 0.14), transparent 45%);
}

.pc-icon {
  font-size: 2.8rem;
  filter: drop-shadow(0 0 14px rgba(255, 255, 255, 0.35));
  position: relative;
  z-index: 1;
}

.pc-idx {
  position: absolute;
  right: 1rem;
  top: 0.6rem;
  font-size: 1.2rem;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.18);
  font-family: 'Courier New', monospace;
  z-index: 1;
}

.pc-body { padding: 1rem 1.2rem 1.2rem; }

.pc-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 0.45rem;
}

.pc-desc {
  font-size: 0.78rem;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 0.7rem;
}

.pc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.85rem;
}

.pc-tag {
  font-size: 0.62rem;
  padding: 0.22rem 0.55rem;
  border-radius: 999px;
  color: #7fd9ff;
  background: rgba(0, 212, 255, 0.08);
  border: 1px solid rgba(0, 212, 255, 0.22);
  font-family: 'Courier New', monospace;
}

.pc-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  transition: color 0.3s;
}

.pc-link:hover { color: #00d4ff; }
.pc-arrow { transition: transform 0.3s; }
.pc-link:hover .pc-arrow { transform: translate(2px, -2px); }

/* ============================================================
   6. 数据仪表盘
   ============================================================ */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.stat-card {
  padding: 1.2rem 1.3rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  transition: transform 0.3s, border-color 0.3s;
}

.stat-card:hover {
  transform: translateY(-3px);
  border-color: rgba(168, 85, 247, 0.5);
}

.stat-icon { font-size: 1.3rem; margin-bottom: 0.3rem; }

.stat-value {
  font-size: 1.6rem;
  font-weight: 800;
  font-family: 'Courier New', monospace;
  background: linear-gradient(135deg, #fff, #9db2ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat-label {
  font-size: 0.66rem;
  letter-spacing: 0.16em;
  color: rgba(255, 255, 255, 0.45);
  font-family: 'Courier New', monospace;
  margin-bottom: 0.5rem;
}

.stat-bar {
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.stat-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #a855f7, #06b6d4);
  box-shadow: 0 0 6px rgba(6, 182, 212, 0.6);
}

/* ============================================================
   7. 兴趣爱好
   ============================================================ */
.interests-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  justify-content: center;
  padding: 1rem;
}

.ic-chip {
  display: inline-flex;
  padding: 0.5rem 1rem;
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(12, 12, 28, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  transition: all 0.3s ease;
  cursor: default;
}

.ic-size-1 { font-size: 0.78rem; padding: 0.45rem 0.95rem; }
.ic-size-2 { font-size: 0.92rem; padding: 0.55rem 1.1rem; color: rgba(255, 255, 255, 0.85); }
.ic-size-3 { font-size: 1.05rem; padding: 0.65rem 1.25rem; color: #fff; font-weight: 600; }

.ic-chip:hover {
  color: #fff;
  border-color: rgba(168, 85, 247, 0.6);
  background: rgba(168, 85, 247, 0.15);
  box-shadow: 0 0 18px rgba(168, 85, 247, 0.3);
  transform: translateY(-2px);
}

/* ============================================================
   8. 联系方式
   ============================================================ */
.contact-panel { padding: 1.8rem 2rem 2rem; }

.contact-intro {
  font-size: 0.86rem;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 1.4rem;
  line-height: 1.7;
}

.contact-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.85rem;
}

.contact-card {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.9rem 1.1rem;
  background: rgba(255, 255, 255, 0.025);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  text-decoration: none;
  transition: all 0.3s;
}

.contact-card:hover {
  border-color: rgba(168, 85, 247, 0.5);
  background: rgba(168, 85, 247, 0.08);
  box-shadow: 0 0 18px rgba(168, 85, 247, 0.25);
  transform: translateY(-2px);
}

.cc-icon { font-size: 1.4rem; flex-shrink: 0; }

.cc-text { display: flex; flex-direction: column; flex: 1; min-width: 0; }

.cc-label {
  font-size: 0.62rem;
  letter-spacing: 0.18em;
  color: rgba(255, 255, 255, 0.4);
  font-family: 'Courier New', monospace;
}

.cc-value {
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cc-arrow {
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.3);
  transition: transform 0.3s, color 0.3s;
}

.contact-card:hover .cc-arrow {
  color: #00d4ff;
  transform: translate(2px, -2px);
}

/* ============================================================
   9. 站点导览
   ============================================================ */
.modules-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.module-card {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 1rem 1.2rem;
  background: rgba(12, 12, 28, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  text-decoration: none;
  transition: all 0.3s;
}

.module-card:hover {
  border-color: rgba(168, 85, 247, 0.5);
  background: rgba(168, 85, 247, 0.08);
  box-shadow: 0 0 22px rgba(168, 85, 247, 0.25);
  transform: translateY(-2px);
}

.mc-icon { font-size: 1.5rem; }

.mc-text { flex: 1; min-width: 0; }

.mc-name {
  font-size: 0.88rem;
  font-weight: 700;
  color: #fff;
}

.mc-sub {
  font-size: 0.66rem;
  color: rgba(255, 255, 255, 0.45);
  margin-top: 2px;
}

.mc-arrow {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.3);
  transition: transform 0.3s, color 0.3s;
}

.module-card:hover .mc-arrow {
  color: #00d4ff;
  transform: translateX(3px);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 900px) {
  .skills-grid { grid-template-columns: repeat(2, 1fr); }
  .projects-grid { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .modules-grid { grid-template-columns: repeat(2, 1fr); }
  .contact-grid { grid-template-columns: 1fr; }
}

@media (max-width: 600px) {
  .about-page { padding: 2rem 1rem 3rem; }
  .skills-grid { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: 1fr; }
  .modules-grid { grid-template-columns: 1fr; }
  .hero { padding: 2rem 0 3rem; }
  .about-text { padding: 1.5rem 1.4rem; }
  .contact-panel { padding: 1.4rem 1.4rem; }
  .block-en { display: none; }
}
</style>

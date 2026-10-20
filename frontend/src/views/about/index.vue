<template>
  <div class="about-page" v-reveal>
    <!-- 背景层 -->
    <div class="about-bg" aria-hidden="true">
      <div class="bg-grid-overlay"></div>
      <div class="bg-glow-a"></div>
      <div class="bg-glow-b"></div>
    </div>

    <div class="about-container">
      <!-- 页头：与全站内页同一套排版语言（面包屑 + 渐变标题 + 英文副标题）。
           此前本页没有页面级标题，直接以「[ 01 ] 关于我」区块开头，
           层级上是乱的 —— 区块标题被误读成页面标题 -->
      <PageHeader
        title="关于"
        subtitle="ABOUT · 技能 · 足迹 · 联系方式"
        :crumbs="[{ label: '首页', to: '/' }, { label: '关于' }]"
      />

      <div class="about-grid">
        <!-- ============================================================
             左侧 · 个人档案卡（吸顶）
             ============================================================ -->
        <aside class="profile-card motions-reveal">
          <div class="avatar">
            <div class="avatar-ring a"></div>
            <div class="avatar-ring b"></div>
            <img src="/images/avatar.jpg" alt="AURORA-朱" class="avatar-core" />
          </div>
          <span class="hero-badge">◈ IDENTITY · v2.0</span>
          <h1 class="profile-name">AURORA-朱</h1>
          <p class="profile-tagline">以代码构建无限可能 · 让每一个想法都拥有沉浸式表达</p>
          <div class="hero-coords">
            <span>SYS.LAT 39.9042°N</span>
            <span class="divider">│</span>
            <span>LNG 116.4074°E</span>
            <span class="divider">│</span>
            <span class="hero-status">● ONLINE</span>
          </div>
          <div class="profile-links">
            <a href="https://github.com/zhult218-art" target="_blank" rel="noopener" class="sc-btn" data-glow>
              <span>🐙</span> GitHub
            </a>
            <a href="mailto:zhult218@gmail.com" class="sc-btn" data-glow>
              <span>✉️</span> Gmail
            </a>
            <router-link to="/link" class="sc-btn" data-glow>
              <span>🤝</span> 友链
            </router-link>
            <a :href="feedUrl" target="_blank" rel="noopener" class="sc-btn" data-glow>
              <span>📡</span> RSS 订阅
            </a>
          </div>

          <!-- 社群入口（预留）：配置 url 后自动变为可点击，未配置时展示「敬请期待」 -->
          <div class="profile-communities">
            <p class="pc-block-title">加入社群</p>
            <div class="community-list">
              <a v-for="c in communities" :key="c.name" class="community-btn"
                :class="{ disabled: !c.url }"
                :href="c.url || undefined"
                :target="c.url ? '_blank' : undefined"
                :rel="c.url ? 'noopener' : undefined">
                <span class="community-icon">{{ c.icon }}</span>
                <span class="community-name">{{ c.name }}</span>
                <span class="community-state">{{ c.url ? '加入 →' : '敬请期待' }}</span>
              </a>
            </div>
          </div>
        </aside>

        <!-- ============================================================
             右侧 · 内容主体
             ============================================================ -->
        <div class="about-main">

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
          <article v-for="g in skillGroups" :key="g.name"
            class="glass-panel-sm skill-panel"
            @mousemove="onTilt" @mouseleave="onTiltLeave">
            <span class="sp-spot" aria-hidden="true"></span>
            <div class="sp-head">
              <span class="sp-icon">{{ g.icon }}</span>
              <div class="sp-title">
                <h3 class="sp-name">{{ g.name }}</h3>
                <span class="sp-en">{{ g.en }}</span>
              </div>
              <span class="sp-role">{{ g.role }}</span>
            </div>
            <div class="sp-items">
              <div v-for="s in g.items" :key="s.name" class="sp-item">
                <div class="sp-item-top">
                  <span class="sp-item-name">{{ s.name }}</span>
                  <span class="sp-item-meta">
                    <span class="sp-years">{{ yearsOf(s.since) }} 年</span>
                    <span class="sp-lv" :class="'lv-' + levelBand(s.level)">{{ levelLabel(s.level) }}</span>
                  </span>
                </div>
                <div class="sp-bar">
                  <span class="sp-bar-fill" :style="{ width: s.level + '%' }"></span>
                </div>
                <p class="sp-note">{{ s.note }}</p>
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
           8. 足迹地图
           ============================================================ -->
      <section class="block motions-reveal" v-reveal>
        <div class="block-head">
          <span class="block-num">[ 08 ]</span>
          <h2 class="block-title">足迹地图</h2>
          <span class="block-en">FOOTPRINTS</span>
        </div>
        <FootprintMap />
      </section>

      <!-- ============================================================
           9. 站点导览
           ============================================================ -->
      <section class="block motions-reveal" v-reveal>
        <div class="block-head">
          <span class="block-num">[ 09 ]</span>
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
        </div><!-- /about-main -->
      </div><!-- /about-grid -->
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 关于页：沉浸式个人展示页（左右分栏）
// 左：个人档案卡（吸顶）· 右：内容主体
// 板块：关于我 / 技能矩阵 / 时间线 / 项目展示 / 数据仪表盘 /
// 兴趣爱好 / 联系方式 / 足迹地图 / 站点导览
// 所有板块使用 v-reveal + motions-reveal 触发滚动入场动画
// ============================================================

import PageHeader from '@/components/common/PageHeader.vue'
import ScrollTypewriter from '@/components/ScrollTypewriter.vue'
import FootprintMap from '@/components/about/FootprintMap.vue'
import { FEED_URL } from '@/api/blog'
// 个人档案数据与首页「关于我」共用一份（src/data/profile.js），避免两处各写一套走形
import {
  aboutText, communities, skillGroups, timeline, projects, interests,
  levelLabel, levelBand, yearsOf,
} from '@/data/profile'

// RSS 订阅地址（后端动态生成，/blog/feed.xml 经网关转发）
const feedUrl = FEED_URL

// 技能矩阵交互：鼠标移动 3D 倾斜 + 聚光跟随
function onTilt(e) {
  const el = e.currentTarget
  const r = el.getBoundingClientRect()
  const px = (e.clientX - r.left) / r.width - 0.5
  const py = (e.clientY - r.top) / r.height - 0.5
  el.style.transform = `perspective(900px) rotateY(${px * 6}deg) rotateX(${-py * 6}deg) translateY(-3px)`
  el.style.transition = 'transform 0.1s ease-out'
  el.style.setProperty('--spot-x', (px + 0.5) * 100 + '%')
  el.style.setProperty('--spot-y', (py + 0.5) * 100 + '%')
}
function onTiltLeave(e) {
  const el = e.currentTarget
  el.style.transform = ''
  el.style.transition = 'transform 0.5s cubic-bezier(0.23,1,0.32,1)'
}
// 数据仪表盘
const dashboard = [
  { icon: '💻', value: '128K', label: '代码行数', bar: 82 },
  { icon: '🚀', value: '40+', label: '完成项目', bar: 76 },
  { icon: '📝', value: '120+', label: '技术文章', bar: 68 },
  { icon: '🔥', value: '640', label: '贡献天数', bar: 88 },
  { icon: '⭐', value: '2.1K', label: 'GitHub Stars', bar: 72 },
  { icon: '🏆', value: '15', label: '开源仓库', bar: 60 },
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
  max-width: 1180px;
  margin: 0 auto;
}

/* ============================================================
   左右分栏 · 左个人档案卡（吸顶）/ 右内容主体
   ============================================================ */
.about-grid {
  display: grid;
  grid-template-columns: 292px 1fr;
  gap: 2.4rem;
  align-items: start;
}

.profile-card {
  position: sticky;
  top: 92px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2rem 1.35rem 1.5rem;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(160deg, rgba(26, 22, 52, 0.78), rgba(10, 10, 26, 0.62));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.4);
  overflow: hidden;
}

/* 卡片顶部流光分隔线 */
.profile-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 18%;
  right: 18%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(168, 85, 247, 0.7), rgba(6, 182, 212, 0.7), transparent);
}

.profile-card .avatar { width: 118px; height: 118px; margin-bottom: 1.1rem; }
.profile-card .avatar-core { width: 92px; height: 92px; }
.profile-card .hero-badge { margin-bottom: 0.85rem; }

.profile-name {
  font-size: 1.6rem;
  font-weight: 900;
  letter-spacing: 0.06em;
  background: linear-gradient(135deg, #06b6d4 0%, #a855f7 50%, #ec4899 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 0.55rem;
}

.profile-tagline {
  font-size: 0.76rem;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 1.1rem;
}

.profile-card .hero-coords {
  font-size: 0.56rem;
  gap: 0.4rem;
  letter-spacing: 0.1em;
  margin-bottom: 1.4rem;
}

.profile-links {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  width: 100%;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

.profile-links .sc-btn {
  justify-content: center;
  padding: 0.5rem 0.5rem;
  font-size: 0.7rem;
}

/* 社群入口（预留） */
.profile-communities {
  width: 100%;
  margin-top: 1.2rem;
  text-align: left;
}

.pc-block-title {
  font-size: 0.58rem;
  letter-spacing: 0.22em;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'Courier New', monospace;
  margin-bottom: 0.65rem;
}

.community-list { display: flex; flex-direction: column; gap: 0.45rem; }

.community-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.55rem 0.75rem;
  font-size: 0.72rem;
  color: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.03);
  text-decoration: none;
  transition: all 0.3s;
}

.community-btn:hover:not(.disabled) {
  color: #fff;
  border-color: rgba(168, 85, 247, 0.55);
  background: rgba(168, 85, 247, 0.12);
  transform: translateX(3px);
}

.community-btn.disabled { opacity: 0.4; cursor: not-allowed; }
.community-icon { font-size: 0.95rem; }
.community-state {
  margin-left: auto;
  font-size: 0.58rem;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'Courier New', monospace;
}

.about-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 窄屏：取消吸顶，改为单列堆叠 */
@media (max-width: 980px) {
  .about-grid { grid-template-columns: 1fr; gap: 2rem; }
  .profile-card { position: static; }
  .profile-links { grid-template-columns: repeat(4, 1fr); }
}

@media (max-width: 560px) {
  .profile-links { grid-template-columns: 1fr 1fr; }
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

/* 标题与英文标签之间的渐隐细线：把「[01] 关于我」与「ABOUT」连起来，
   读起来是一条带编号的分节标题，而不是两段各自飘着的字 */
.block-head::after {
  content: '';
  order: 3;
  flex: 1;
  height: 1px;
  margin: 0 0.9rem;
  align-self: center;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.16), transparent);
}

.block-num {
  font-size: 0.7rem;
  letter-spacing: 0.2em;
  color: #a855f7;
  font-family: 'Courier New', monospace;
  text-shadow: 0 0 8px rgba(168, 85, 247, 0.5);
}

.block-title {
  font-size: clamp(1.15rem, 1.7vw, 1.4rem);
  font-weight: 800;
  letter-spacing: 0.01em;
  color: #f2f5ff;
  text-shadow: 0 1px 10px rgba(4, 4, 14, 0.75);
}

.block-en {
  order: 4;
  font-size: 0.62rem;
  letter-spacing: 0.28em;
  color: rgba(255, 255, 255, 0.45);
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
  position: relative;
  padding: 1.4rem 1.4rem 1.5rem;
  transform-style: preserve-3d;
  will-change: transform;
  overflow: hidden;
}

/* 聚光层：随鼠标移动的径向高光 */
.sp-spot {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(
    220px circle at var(--spot-x, 50%) var(--spot-y, 50%),
    rgba(168, 85, 247, 0.16),
    transparent 60%
  );
  opacity: 0;
  transition: opacity 0.3s ease;
}
.skill-panel:hover .sp-spot { opacity: 1; }

.sp-head {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding-bottom: 0.85rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sp-icon { font-size: 1.4rem; }

.sp-title { min-width: 0; }

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

/* 分组定位标签：主力方向 / 熟练运用 / … */
.sp-role {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 0.56rem;
  letter-spacing: 0.1em;
  color: #9db2ff;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  background: rgba(120, 140, 255, 0.1);
  border: 1px solid rgba(120, 140, 255, 0.22);
  white-space: nowrap;
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

.sp-item-meta {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  flex-shrink: 0;
}

/* 投入年限 */
.sp-years {
  font-size: 0.54rem;
  letter-spacing: 0.06em;
  color: rgba(255, 255, 255, 0.42);
  font-family: 'Courier New', monospace;
}

/* 等级标签：按熟练度分色 */
.sp-lv {
  font-size: 0.5rem;
  letter-spacing: 0.12em;
  padding: 1px 5px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.5);
}
.sp-lv.lv-expert {
  background: rgba(236, 72, 153, 0.18);
  color: #f9a8d4;
}
.sp-lv.lv-advanced {
  background: rgba(168, 85, 247, 0.18);
  color: #c4b5fd;
}
.sp-lv.lv-steady {
  background: rgba(6, 182, 212, 0.16);
  color: #7fd9ff;
}

/* 具体做过什么：一句话经历，替代抽象星级 */
.sp-note {
  margin-top: 0.4rem;
  font-size: 0.62rem;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.4);
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
  transform-origin: left center;
  animation: spBarGrow 1.2s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes spBarGrow {
  from { transform: scaleX(0); opacity: 0; }
  to   { transform: scaleX(1); opacity: 1; }
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

<template>
  <section id="about-me" class="about-band">
    <div class="about-inner">
      <div class="about-grid">
        <!-- ============================================================
             左侧 · 个人档案卡（吸顶）
             与「关于」页同一套版式：头像 + 名牌 + 坐标 + 快捷入口 + 社群
             ============================================================ -->
        <aside class="profile-card" data-reveal>
          <div class="avatar">
            <span class="avatar-ring a"></span>
            <span class="avatar-ring b"></span>
            <img v-if="!avatarFailed" :src="avatarSrc" :alt="brandName" class="avatar-core" @error="avatarFailed = true" />
            <span v-else class="avatar-mono">{{ monogram }}</span>
          </div>

          <span class="hero-badge">◈ IDENTITY · v2.0</span>
          <h2 class="profile-name">{{ brandName }}</h2>
          <p class="profile-tagline">{{ tagline }}</p>

          <div class="hero-coords">
            <span>SYS.LAT 39.9042°N</span>
            <span class="divider">│</span>
            <span>LNG 116.4074°E</span>
            <span class="divider">│</span>
            <span class="hero-status">● ONLINE</span>
          </div>

          <div class="profile-links">
            <a v-if="githubUrl" :href="githubUrl" target="_blank" rel="noopener" class="sc-btn">
              <span>🐙</span> GitHub
            </a>
            <a v-if="mail" :href="'mailto:' + mail" class="sc-btn">
              <span>✉️</span> 邮箱
            </a>
            <router-link to="/link" class="sc-btn"><span>🤝</span> 友链</router-link>
            <router-link to="/about" class="sc-btn"><span>📄</span> 关于</router-link>
          </div>

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
          <!-- [ 01 ] 关于我 -->
          <section class="block" data-reveal>
            <div class="block-head">
              <span class="bh-num">01</span>
              <h3 class="bh-title">关于我</h3>
              <span class="bh-line"></span>
              <span class="bh-en">ABOUT</span>
            </div>
            <div class="glass-panel about-text">
              <p v-for="(p, i) in bio" :key="i">{{ p }}</p>
            </div>
          </section>

          <!-- [ 02 ] 技能矩阵 -->
          <section class="block" data-reveal>
            <div class="block-head">
              <span class="bh-num">02</span>
              <h3 class="bh-title">技能矩阵</h3>
              <span class="bh-line"></span>
              <span class="bh-en">SKILL MATRIX</span>
            </div>
            <div class="skills-grid">
              <article v-for="g in skillGroups" :key="g.name" class="glass-panel-sm skill-panel"
                @mousemove="onTilt" @mouseleave="onTiltLeave">
                <span class="sp-spot" aria-hidden="true"></span>
                <div class="sp-head">
                  <span class="sp-icon">{{ g.icon }}</span>
                  <div class="sp-title">
                    <h4 class="sp-name">{{ g.name }}</h4>
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
                    <div class="sp-bar"><span class="sp-bar-fill" :style="{ width: s.level + '%' }"></span></div>
                    <p class="sp-note">{{ s.note }}</p>
                  </div>
                </div>
              </article>
            </div>
          </section>

          <!-- [ 03 ] 个人时间线 -->
          <section class="block" data-reveal>
            <div class="block-head">
              <span class="bh-num">03</span>
              <h3 class="bh-title">个人时间线</h3>
              <span class="bh-line"></span>
              <span class="bh-en">TIMELINE</span>
            </div>
            <div class="timeline">
              <div v-for="(t, i) in timeline" :key="t.year" class="tl-node">
                <span class="tl-dot"></span>
                <span v-if="i < timeline.length - 1" class="tl-line"></span>
                <div class="glass-panel-sm tl-card">
                  <span class="tl-year">{{ t.year }}</span>
                  <h4 class="tl-title">{{ t.title }}</h4>
                  <p class="tl-desc">{{ t.desc }}</p>
                  <div class="tl-tags">
                    <span v-for="tag in t.tags" :key="tag" class="tl-tag">{{ tag }}</span>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- [ 04 ] 代表作品 -->
          <section class="block" data-reveal>
            <div class="block-head">
              <span class="bh-num">04</span>
              <h3 class="bh-title">代表作品</h3>
              <span class="bh-line"></span>
              <span class="bh-en">PROJECTS</span>
            </div>
            <div class="projects-grid">
              <article v-for="(p, i) in projects" :key="p.title" class="glass-panel-sm project-card">
                <div class="pc-cover" :style="{ background: p.cover }">
                  <span class="pc-icon">{{ p.icon }}</span>
                  <span class="pc-idx">0{{ i + 1 }}</span>
                </div>
                <div class="pc-body">
                  <h4 class="pc-title">{{ p.title }}</h4>
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

          <!-- [ 05 ] 数据仪表盘（真实接口值） -->
          <section class="block" data-reveal>
            <div class="block-head">
              <span class="bh-num">05</span>
              <h3 class="bh-title">数据仪表盘</h3>
              <span class="bh-line"></span>
              <span class="bh-en">DASHBOARD</span>
            </div>
            <div class="stats-grid">
              <div v-for="s in stats" :key="s.label" class="glass-panel-sm stat-card">
                <span class="stat-icon">{{ s.icon }}</span>
                <span class="stat-value">{{ s.value }}</span>
                <span class="stat-label">{{ s.label }}</span>
                <span class="stat-bar"><span :style="{ width: s.bar + '%' }"></span></span>
              </div>
            </div>
          </section>

          <!-- [ 06 ] 联系方式 -->
          <section class="block block-last" data-reveal>
            <div class="block-head">
              <span class="bh-num">06</span>
              <h3 class="bh-title">联系方式</h3>
              <span class="bh-line"></span>
              <span class="bh-en">CONTACT</span>
            </div>
            <div class="glass-panel contact-panel">
              <p class="contact-intro">想聊聊技术、合作，或者只是交个朋友？</p>
              <div class="contact-grid">
                <a v-if="githubUrl" :href="githubUrl" target="_blank" rel="noopener" class="contact-card">
                  <span class="cc-icon">🐙</span>
                  <div class="cc-text">
                    <span class="cc-label">GitHub</span>
                    <span class="cc-value">{{ githubUrl.replace(/^https?:\/\//, '') }}</span>
                  </div>
                  <span class="cc-arrow">↗</span>
                </a>
                <button v-if="mail" type="button" class="contact-card" @click="copyMail">
                  <span class="cc-icon">✉️</span>
                  <div class="cc-text">
                    <span class="cc-label">邮箱{{ copied ? ' · 已复制 ✓' : ' · 点击复制' }}</span>
                    <span class="cc-value">{{ mail }}</span>
                  </div>
                  <span class="cc-arrow">⧉</span>
                </button>
                <router-link to="/comments" class="contact-card">
                  <span class="cc-icon">💬</span>
                  <div class="cc-text">
                    <span class="cc-label">留言板</span>
                    <span class="cc-value">在线留言，期待你的消息</span>
                  </div>
                  <span class="cc-arrow">↗</span>
                </router-link>
                <router-link to="/about" class="contact-card">
                  <span class="cc-icon">📄</span>
                  <div class="cc-text">
                    <span class="cc-label">完整关于页</span>
                    <span class="cc-value">足迹地图 · 站点导览 · 更多细节</span>
                  </div>
                  <span class="cc-arrow">↗</span>
                </router-link>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
// ============================================================
// 首页「关于我」区块（首屏之后）
// 版式与「关于」页对齐：左个人档案卡（吸顶）+ 右内容主体，
// 数据统一取自 src/data/profile.js，两页共用一份，不会各写一套慢慢走形。
// 视觉：深空底 + 细网格 + 玻璃面板 + 青紫渐变，与首屏同为暗色体系；
// 首屏与这里的衔接由 GhibliHome 里那条极光波浪完成（见 .hero-wave）
// ============================================================
import { ref, computed, onMounted } from 'vue'
import { useSiteSettingsStore } from '@/stores/modules/siteSettings'
import { getBlogStats, getBlogCategories, getBlogTags } from '@/api/blog'
import {
  aboutText, communities, skillGroups, timeline, projects,
  levelLabel, levelBand, yearsOf,
} from '@/data/profile'

const site = useSiteSettingsStore()
const brandName = computed(() => site.brandName)
const githubUrl = computed(() => site.social.github || '')
const mail = computed(() => {
  const raw = site.footer.contactEmail || site.social.email || ''
  return /^mailto:/i.test(raw) ? raw.slice(7) : raw
})
const copied = ref(false)

// 头像：优先后台配置的站点 Logo，未配置时用 public/images/avatar.jpg；
// 两者都取不到才退回品牌首字徽记（图片 onerror 也会走到这里）
const avatarFailed = ref(false)
const avatarSrc = computed(() => site.site.logoUrl || '/images/avatar.jpg')
const monogram = computed(() => (brandName.value || 'A').trim().charAt(0))

const tagline = computed(() => site.site.slogan || '用一个站点，装下所有好奇心。')

// 「关于我」正文：取共用文本的前两段，首页不宜太长
const bio = computed(() => aboutText.split(/\n\s*\n/).slice(0, 2))

// 技能矩阵交互：鼠标移动 3D 倾斜 + 聚光跟随
function onTilt(e) {
  const el = e.currentTarget
  const r = el.getBoundingClientRect()
  const px = (e.clientX - r.left) / r.width - 0.5
  const py = (e.clientY - r.top) / r.height - 0.5
  el.style.transform = `perspective(900px) rotateY(${px * 5}deg) rotateX(${-py * 5}deg) translateY(-3px)`
  el.style.transition = 'transform 0.1s ease-out'
  el.style.setProperty('--spot-x', (px + 0.5) * 100 + '%')
  el.style.setProperty('--spot-y', (py + 0.5) * 100 + '%')
}
function onTiltLeave(e) {
  const el = e.currentTarget
  el.style.transform = ''
  el.style.transition = 'transform 0.5s cubic-bezier(0.23,1,0.32,1)'
}

// 数据仪表盘：文章 / 分类 / 标签 / 累计字数，全部取真实接口值
// bar 为相对占比，只用来给卡片一条刻度感，不代表绝对值
const stats = ref([
  { icon: '📝', label: '篇文章', value: '—', raw: 0, bar: 0 },
  { icon: '🗂️', label: '个分类', value: '—', raw: 0, bar: 0 },
  { icon: '🏷️', label: '个标签', value: '—', raw: 0, bar: 0 },
  { icon: '✍️', label: '累计字数', value: '—', raw: 0, bar: 0 },
])

function fmtNum(n) {
  if (n == null || Number.isNaN(n)) return '—'
  return n >= 10000 ? (n / 10000).toFixed(1) + 'w' : String(n)
}

function applyStats(list) {
  const max = Math.max(1, ...list.map(i => i.raw || 0))
  stats.value = list.map(i => ({ ...i, bar: Math.max(6, Math.round(((i.raw || 0) / max) * 100)) }))
}

async function loadStats() {
  const [s, cats, tags] = await Promise.all([
    getBlogStats().catch(() => null),
    getBlogCategories().catch(() => null),
    getBlogTags().catch(() => null),
  ])
  const d = s?.data ?? s ?? {}
  const catArr = cats?.data ?? cats ?? []
  const tagArr = tags?.data ?? tags ?? []
  const arts = d.totalArticles ?? d.articleCount ?? 0
  const words = d.totalWords ?? 0
  applyStats([
    { icon: '📝', label: '篇文章', value: fmtNum(arts), raw: arts },
    { icon: '🗂️', label: '个分类', value: fmtNum(Array.isArray(catArr) ? catArr.length : 0), raw: Array.isArray(catArr) ? catArr.length : 0 },
    { icon: '🏷️', label: '个标签', value: fmtNum(Array.isArray(tagArr) ? tagArr.length : 0), raw: Array.isArray(tagArr) ? tagArr.length : 0 },
    { icon: '✍️', label: '累计字数', value: fmtNum(words || null), raw: words },
  ])
}

async function copyMail() {
  if (!mail.value) return
  try {
    await navigator.clipboard.writeText(mail.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 1600)
  } catch { /* 剪贴板不可用时保持原文展示，用户可手动选中 */ }
}

// 挂载后拉取真实统计数据；进场动效由纯 CSS 一次性 keyframes 完成（见样式区 [data-reveal]），
// 不依赖 IntersectionObserver 摘除 opacity —— 观察器未触发时区块会整体不可见
onMounted(() => { loadStats() })
</script>

<style scoped>
/* ============================================================
   深空底 + 细网格 + 玻璃面板 + 青紫渐变
   整块不透明，把固定的全局背景层（星野 / 云海）完全遮住，
   于是首屏的星野到这里的网格是一次干净的转场。
   注意 overflow 用 clip 而非 hidden：hidden 会生成滚动容器，
   让左侧档案卡的 position:sticky 失效。
   ============================================================ */
.about-band {
  --bg: #08090f;
  --acc1: #22d3ee;
  --acc2: #8b7cf6;
  --line: rgba(255, 255, 255, 0.07);
  --txt: #eef2f9;
  --dim: rgba(238, 242, 249, 0.72);
  --dim2: rgba(238, 242, 249, 0.44);
  --card: linear-gradient(160deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.014));

  position: relative;
  z-index: 3;
  background: var(--bg);
  color: var(--txt);
  overflow: clip;
}

.about-band::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    radial-gradient(76% 56% at 50% 24%, rgba(34, 211, 238, 0.07), transparent 70%),
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: auto, 46px 46px, 46px 46px;
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, #000 9%, #000 86%, transparent 100%);
  mask-image: linear-gradient(180deg, transparent 0%, #000 9%, #000 86%, transparent 100%);
}
.about-band::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 220px;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.06) 0%, rgba(139, 124, 246, 0.026) 44%, transparent 100%);
}

.about-inner {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin: 0 auto;
  padding: clamp(52px, 6.6vw, 92px) clamp(20px, 4vw, 40px) clamp(48px, 6vw, 84px);
}

/* 左右分栏：左档案卡吸顶，右内容主体 */
.about-grid {
  display: grid;
  grid-template-columns: 296px minmax(0, 1fr);
  gap: clamp(20px, 2.6vw, 38px);
  align-items: start;
}
.about-main { min-width: 0; }

/* 进场：淡入 + 上移，--d 提供错峰延迟。
   用一次性 keyframes 而非「常驻 opacity:0 + JS 摘除」：
   填充模式取 backwards —— 起始前按 from 态隐性错峰，动画结束后回归元素自身样式（即可见），
   即便动画被禁用或未触发，区块内容也不会长期隐身 */
@keyframes about-rise {
  from { opacity: 0; transform: translateY(22px); }
  to   { opacity: 1; transform: none; }
}
[data-reveal] {
  animation: about-rise 0.78s cubic-bezier(0.22, 0.61, 0.36, 1) backwards;
  animation-delay: var(--d, 0ms);
}
.about-main > [data-reveal]:nth-child(1) { --d: 60ms; }
.about-main > [data-reveal]:nth-child(2) { --d: 130ms; }
.about-main > [data-reveal]:nth-child(3) { --d: 190ms; }
.about-main > [data-reveal]:nth-child(4) { --d: 240ms; }
.about-main > [data-reveal]:nth-child(5) { --d: 290ms; }
.about-main > [data-reveal]:nth-child(6) { --d: 330ms; }

/* ── 玻璃面板 ── */
.glass-panel,
.glass-panel-sm {
  position: relative;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: var(--card);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}
.glass-panel-sm { border-radius: 14px; }

/* ── 左：档案卡 ── */
.profile-card {
  position: sticky;
  top: 96px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 26px 20px 20px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: var(--card);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 20px 52px -32px rgba(0, 0, 0, 0.9);
  overflow: hidden;
}
/* 顶部流光分隔线 */
.profile-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 16%;
  right: 16%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(34, 211, 238, 0.7), rgba(139, 124, 246, 0.7), transparent);
}
.profile-card > * { position: relative; }

/* 头像：双层旋转描边 + 实拍图 */
.avatar {
  position: relative;
  width: 118px;
  height: 118px;
  display: grid;
  place-items: center;
  margin-bottom: 16px;
}
.avatar-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid rgba(34, 211, 238, 0.22);
}
.avatar-ring.a {
  inset: -6px;
  border-style: dashed;
  border-color: rgba(34, 211, 238, 0.28);
  animation: ringSpin 26s linear infinite;
}
.avatar-ring.b {
  inset: 2px;
  border-color: rgba(139, 124, 246, 0.28);
  animation: ringSpin 18s linear infinite reverse;
}
@keyframes ringSpin { to { transform: rotate(360deg); } }

.avatar-core,
.avatar-mono {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 0 34px -10px rgba(34, 211, 238, 0.8);
}
.avatar-mono {
  display: grid;
  place-items: center;
  font-family: var(--font-mono);
  font-size: 34px;
  color: #9fe4ff;
  background: linear-gradient(150deg, rgba(34, 211, 238, 0.18), rgba(139, 124, 246, 0.18));
}

.hero-badge {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.18em;
  color: var(--acc1);
  padding: 3px 9px;
  margin-bottom: 12px;
  border-radius: 999px;
  border: 1px solid rgba(34, 211, 238, 0.26);
  background: rgba(34, 211, 238, 0.07);
}

.profile-name {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.02em;
  background: linear-gradient(120deg, #7fe6ff 0%, #a89bfb 60%, #f0a6ff 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #9fe4ff;
  margin-bottom: 8px;
}

.profile-tagline {
  font-size: 12px;
  line-height: 1.8;
  color: var(--dim);
  margin-bottom: 14px;
}

.hero-coords {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 5px;
  font-family: var(--font-mono);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  color: var(--dim2);
  margin-bottom: 16px;
}
.hero-coords .divider { opacity: 0.4; }
.hero-status { color: #4ade80; }

.profile-links {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 7px;
  width: 100%;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}
.sc-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 8px 6px;
  font-size: 11.5px;
  color: rgba(238, 242, 249, 0.82);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
  transition: all 0.24s ease;
}
.sc-btn:hover {
  color: #c8f7ff;
  border-color: rgba(34, 211, 238, 0.45);
  background: rgba(34, 211, 238, 0.09);
  transform: translateY(-1px);
}

.profile-communities { width: 100%; margin-top: 16px; text-align: left; }
.pc-block-title {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.2em;
  color: var(--dim2);
  margin-bottom: 9px;
}
.community-list { display: flex; flex-direction: column; gap: 7px; }
.community-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  font-size: 11.5px;
  color: rgba(238, 242, 249, 0.82);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  background: rgba(255, 255, 255, 0.025);
  transition: all 0.26s ease;
}
.community-btn:hover:not(.disabled) {
  color: #fff;
  border-color: rgba(139, 124, 246, 0.5);
  background: rgba(139, 124, 246, 0.12);
  transform: translateX(3px);
}
.community-btn.disabled { opacity: 0.42; cursor: not-allowed; }
.community-icon { font-size: 13px; }
.community-state {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 8.5px;
  color: var(--dim2);
}

/* ── 右：区块 ── */
.block + .block { margin-top: clamp(34px, 4.4vw, 56px); }
.block-last { margin-bottom: 0; }

/* 标题行：编号方牌 + 中文标题 + 渐隐细线 + 英文小标 */
.block-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}
.bh-num {
  flex: none;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--acc1);
  padding: 3px 6px;
  border-radius: 6px;
  border: 1px solid rgba(34, 211, 238, 0.26);
  background: rgba(34, 211, 238, 0.07);
}
.bh-title {
  font-size: clamp(17px, 1.9vw, 20px);
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #fff;
  white-space: nowrap;
}
.bh-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0));
}
.bh-en {
  flex: none;
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.24em;
  color: var(--dim2);
}

/* ── [ 01 ] 关于我 ── */
.about-text { padding: 22px 26px; }
.about-text p {
  font-size: 14px;
  line-height: 1.95;
  color: var(--dim);
}
.about-text p + p { margin-top: 13px; }
/* 首段首行加一点提亮，读起来有落点 */
.about-text p:first-child { color: rgba(238, 242, 249, 0.82); }

/* ── [ 02 ] 技能矩阵 ── */
.skills-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.skill-panel {
  position: relative;
  padding: 18px 18px 16px;
  overflow: hidden;
  transition: border-color 0.26s ease, box-shadow 0.26s ease;
}
.skill-panel:hover {
  border-color: rgba(34, 211, 238, 0.24);
  box-shadow: 0 24px 50px -34px rgba(34, 211, 238, 0.8);
}
/* 聚光跟随鼠标 */
.sp-spot {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0;
  background: radial-gradient(220px circle at var(--spot-x, 50%) var(--spot-y, 0%), rgba(34, 211, 238, 0.1), transparent 70%);
  transition: opacity 0.3s ease;
}
.skill-panel:hover .sp-spot { opacity: 1; }

.sp-head {
  display: flex;
  align-items: center;
  gap: 9px;
  padding-bottom: 12px;
  margin-bottom: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.sp-icon { font-size: 17px; line-height: 1; }
.sp-title { min-width: 0; flex: 1; }
.sp-name { font-size: 13px; font-weight: 600; color: #fff; }
.sp-en {
  display: block;
  margin-top: 2px;
  font-family: var(--font-mono);
  font-size: 8.5px;
  letter-spacing: 0.18em;
  color: var(--dim2);
}
.sp-role {
  flex: none;
  font-size: 9.5px;
  color: #c8b8ff;
  padding: 2px 7px;
  border-radius: 999px;
  border: 1px solid rgba(139, 124, 246, 0.28);
  background: rgba(139, 124, 246, 0.1);
}

.sp-items { display: flex; flex-direction: column; gap: 13px; }
.sp-item-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}
.sp-item-name { font-size: 12px; color: rgba(238, 242, 249, 0.84); }
.sp-item-meta { display: inline-flex; align-items: center; gap: 6px; flex: none; }
.sp-years { font-family: var(--font-mono); font-size: 9.5px; color: var(--dim2); }
.sp-lv {
  font-family: var(--font-mono);
  font-size: 8px;
  letter-spacing: 0.1em;
  padding: 1px 5px;
  border-radius: 4px;
}
.sp-lv.lv-expert { color: #7fe6ff; background: rgba(34, 211, 238, 0.14); }
.sp-lv.lv-advanced { color: #c8b8ff; background: rgba(139, 124, 246, 0.14); }
.sp-lv.lv-steady { color: #8fc0d9; background: rgba(143, 192, 217, 0.1); }

.sp-bar {
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}
.sp-bar-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #22d3ee, #8b7cf6);
  box-shadow: 0 0 9px rgba(34, 211, 238, 0.5);
}
.sp-note {
  margin-top: 6px;
  font-size: 10.5px;
  line-height: 1.7;
  color: var(--dim2);
}

/* ── [ 03 ] 时间线 ── */
.timeline { position: relative; }
.tl-node { position: relative; padding-left: 26px; padding-bottom: 12px; }
.tl-node:last-child { padding-bottom: 0; }
.tl-dot {
  position: absolute;
  left: 0;
  top: 18px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--acc1);
  box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.13), 0 0 14px rgba(34, 211, 238, 0.85);
}
.tl-line {
  position: absolute;
  left: 4px;
  top: 30px;
  width: 1px;
  height: calc(100% - 26px);
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.34), rgba(139, 124, 246, 0.06));
}
.tl-card {
  padding: 15px 18px 16px;
  transition: border-color 0.24s ease, transform 0.24s ease, background 0.24s ease;
}
.tl-card:hover {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
  transform: translateX(2px);
}
.tl-year {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--acc1);
  padding: 2px 8px;
  margin-bottom: 7px;
  border-radius: 5px;
  background: rgba(34, 211, 238, 0.09);
}
.tl-title { font-size: 14.5px; font-weight: 600; color: #fff; }
.tl-desc { margin-top: 6px; font-size: 12.5px; line-height: 1.8; color: var(--dim); }
.tl-tags { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 10px; }
.tl-tag {
  font-size: 10px;
  color: rgba(238, 242, 249, 0.6);
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  background: rgba(255, 255, 255, 0.03);
}

/* ── [ 04 ] 代表作品 ── */
.projects-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.project-card {
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.28s cubic-bezier(0.22, 0.61, 0.36, 1), box-shadow 0.28s ease, border-color 0.28s ease;
}
.project-card:hover {
  transform: translateY(-4px);
  border-color: rgba(34, 211, 238, 0.3);
  box-shadow: 0 28px 54px -32px rgba(34, 211, 238, 0.6);
}
.pc-cover {
  position: relative;
  height: 86px;
  display: grid;
  place-items: center;
  overflow: hidden;
}
.pc-cover::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 18px 18px;
  -webkit-mask-image: radial-gradient(80% 80% at 50% 50%, #000, transparent 78%);
  mask-image: radial-gradient(80% 80% at 50% 50%, #000, transparent 78%);
}
.pc-icon { position: relative; font-size: 26px; filter: drop-shadow(0 4px 10px rgba(0, 0, 0, 0.5)); }
.pc-idx {
  position: absolute;
  right: 12px;
  top: 10px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: rgba(255, 255, 255, 0.34);
}
.pc-body { padding: 14px 18px 16px; display: flex; flex-direction: column; flex: 1; }
.pc-title { font-size: 14.5px; font-weight: 600; color: #fff; }
.pc-desc { margin-top: 7px; font-size: 12.5px; line-height: 1.8; color: var(--dim); }
.pc-tags { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 10px; }
.pc-tag {
  font-size: 9.5px;
  font-family: var(--font-mono);
  color: #9fe4ff;
  padding: 2px 7px;
  border-radius: 5px;
  background: rgba(34, 211, 238, 0.08);
}
.pc-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: auto;
  padding-top: 12px;
  font-size: 12px;
  color: var(--acc1);
}
.pc-arrow { transition: transform 0.26s cubic-bezier(0.22, 0.61, 0.36, 1); }
.project-card:hover .pc-arrow { transform: translate(2px, -2px); }

/* ── [ 05 ] 数据仪表盘 ── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.stat-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 15px 16px 14px;
  overflow: hidden;
  transition: border-color 0.24s ease, transform 0.24s ease;
}
.stat-card:hover { border-color: rgba(34, 211, 238, 0.26); transform: translateY(-2px); }
.stat-icon { font-size: 15px; }
.stat-value {
  font-family: var(--font-mono);
  font-size: clamp(22px, 2.4vw, 28px);
  font-weight: 700;
  line-height: 1.1;
  background: linear-gradient(96deg, #ffffff 4%, #7fe6ff 54%, #a89bfb 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #cfefff;
}
.stat-label { font-family: var(--font-mono); font-size: 10px; letter-spacing: 0.12em; color: var(--dim2); }
.stat-bar {
  display: block;
  margin-top: 8px;
  height: 3px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}
.stat-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #22d3ee, #8b7cf6);
  transition: width 0.6s cubic-bezier(0.22, 0.61, 0.36, 1);
}

/* ── [ 06 ] 联系方式 ── */
.contact-panel { padding: 22px 24px 24px; }
.contact-intro { font-size: 13px; color: var(--dim); margin-bottom: 16px; }
.contact-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.contact-card {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 14px;
  text-align: left;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  background: rgba(255, 255, 255, 0.025);
  cursor: pointer;
  transition: all 0.24s ease;
}
.contact-card:hover {
  border-color: rgba(34, 211, 238, 0.4);
  background: rgba(34, 211, 238, 0.07);
  transform: translateY(-2px);
}
.cc-icon { font-size: 17px; flex: none; }
.cc-text { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.cc-label { font-size: 11.5px; color: #fff; }
.cc-value {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--dim2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cc-arrow { margin-left: auto; font-size: 12px; color: var(--dim2); transition: transform 0.24s ease, color 0.24s ease; }
.contact-card:hover .cc-arrow { color: var(--acc1); transform: translate(2px, -2px); }

/* ── 响应式 ── */
@media (max-width: 1080px) {
  .about-grid { grid-template-columns: 1fr; }
  .profile-card { position: static; }
  .profile-links { grid-template-columns: repeat(4, 1fr); }
  .profile-communities { max-width: 420px; }
}
@media (max-width: 860px) {
  .skills-grid { grid-template-columns: 1fr; }
  .projects-grid { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 680px) {
  .contact-grid { grid-template-columns: 1fr; }
  .bh-en { display: none; }
  .about-text { padding: 18px 18px; }
  .profile-links { grid-template-columns: 1fr 1fr; }
}

/* 尊重系统减弱动效设置 */
@media (prefers-reduced-motion: reduce) {
  [data-reveal] { animation: none; }
  .avatar-ring { animation: none; }
  .project-card:hover, .stat-card:hover, .contact-card:hover,
  .sc-btn:hover, .community-btn:hover:not(.disabled), .tl-card:hover { transform: none; }
  .project-card:hover .pc-arrow, .contact-card:hover .cc-arrow { transform: none; }
}
</style>

<template>
  <section class="about-band">
    <div class="about-inner">
      <!-- ── 0. 档案卡：头像 + 姓名 + 签名 + 身份标签 ── -->
      <header class="pf" data-reveal>
        <div class="pf-avatar">
          <img v-if="site.site.logoUrl" :src="site.site.logoUrl" :alt="brandName" />
          <span v-else class="pf-mono">{{ monogram }}</span>
        </div>
        <div class="pf-body">
          <p class="pf-eyebrow"><span class="pf-rule"></span>ABOUT ME · 关于我</p>
          <h2 class="pf-name">{{ brandName }}</h2>
          <p class="pf-tagline">{{ tagline }}</p>
          <ul class="pf-chips">
            <li v-for="c in identity" :key="c">{{ c }}</li>
          </ul>
        </div>
      </header>

      <!-- ── [ 01 ] 关于我 ── -->
      <section class="block" data-reveal>
        <div class="block-head">
          <span class="block-num">[ 01 ]</span>
          <h3 class="block-title">关于我</h3>
          <span class="block-en">ABOUT</span>
        </div>
        <div class="bio">
          <p v-for="(p, i) in bio" :key="i">{{ p }}</p>
        </div>
      </section>

      <!-- ── [ 02 ] 技能矩阵 ── -->
      <section class="block" data-reveal>
        <div class="block-head">
          <span class="block-num">[ 02 ]</span>
          <h3 class="block-title">技能矩阵</h3>
          <span class="block-en">SKILL MATRIX</span>
        </div>
        <div class="sk-grid">
          <div v-for="g in skillGroups" :key="g.name" class="sk-group">
            <div class="sk-head">
              <span class="sk-icon">{{ g.icon }}</span>
              <div class="sk-titles">
                <p class="sk-name">{{ g.name }}</p>
                <span class="sk-en">{{ g.en }}</span>
              </div>
            </div>
            <div class="sk-items">
              <div v-for="s in g.items" :key="s.name" class="sk-item">
                <div class="sk-item-top">
                  <span class="sk-item-name">{{ s.name }}</span>
                  <span class="sk-years">{{ yearsOf(s.since) }} 年</span>
                </div>
                <span class="sk-bar"><i :style="{ width: s.level + '%' }"></i></span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ── [ 03 ] 个人时间线 ── -->
      <section class="block" data-reveal>
        <div class="block-head">
          <span class="block-num">[ 03 ]</span>
          <h3 class="block-title">个人时间线</h3>
          <span class="block-en">TIMELINE</span>
        </div>
        <div class="timeline">
          <div v-for="(t, i) in timeline" :key="t.year" class="tl-node">
            <span class="tl-dot"></span>
            <span v-if="i < timeline.length - 1" class="tl-line"></span>
            <div class="tl-card">
              <span class="tl-year">{{ t.year }}</span>
              <h4 class="tl-title">{{ t.title }}</h4>
              <p class="tl-desc">{{ t.desc }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- ── [ 04 ] 代表作品 ── -->
      <section class="block" data-reveal>
        <div class="block-head">
          <span class="block-num">[ 04 ]</span>
          <h3 class="block-title">代表作品</h3>
          <span class="block-en">PROJECTS</span>
        </div>
        <div class="pj-grid">
          <article v-for="(p, i) in projects" :key="p.title" class="pj-card">
            <span class="pj-idx">{{ String(i + 1).padStart(2, '0') }}</span>
            <h4 class="pj-title"><span class="pj-icon">{{ p.icon }}</span>{{ p.title }}</h4>
            <p class="pj-desc">{{ p.desc }}</p>
            <router-link v-if="p.to" :to="p.to" class="pj-link">去看看 →</router-link>
          </article>
        </div>
      </section>

      <!-- ── [ 05 ] 数据仪表盘（取真实接口值） ── -->
      <section class="block" data-reveal>
        <div class="block-head">
          <span class="block-num">[ 05 ]</span>
          <h3 class="block-title">数据仪表盘</h3>
          <span class="block-en">DASHBOARD</span>
        </div>
        <div class="stats">
          <div v-for="s in stats" :key="s.label" class="stat">
            <span class="stat-num">{{ s.value }}</span>
            <span class="stat-label">{{ s.label }}</span>
          </div>
        </div>
      </section>

      <!-- ── [ 06 ] 联系方式 ── -->
      <section class="block block-last" data-reveal>
        <div class="block-head">
          <span class="block-num">[ 06 ]</span>
          <h3 class="block-title">联系方式</h3>
          <span class="block-en">CONTACT</span>
        </div>
        <div class="contact">
          <div class="contact-text">
            <p class="contact-lead">想聊聊技术、合作，或者只是交个朋友？</p>
            <p class="contact-sub">{{ contactSub }}</p>
          </div>
          <div class="contact-actions">
            <a v-if="githubUrl" :href="githubUrl" target="_blank" rel="noopener" class="cn-btn">GitHub ↗</a>
            <button v-if="mail" type="button" class="cn-btn" @click="copyMail">
              {{ copied ? '已复制 ✓' : '复制邮箱' }}
            </button>
            <a v-if="mail" :href="'mailto:' + mail" class="cn-btn is-ghost">发邮件</a>
            <router-link to="/comments" class="cn-btn is-ghost">留言板</router-link>
            <router-link to="/about" class="cn-btn is-ghost">完整关于页 →</router-link>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
// ============================================================
// 首页「关于我」区块（首屏之后）
// 板块结构对齐站点「关于」页：关于我 / 技能矩阵 / 时间线 / 代表作品 / 数据仪表盘 / 联系方式
// 视觉：极简未来（深空底 + 细网格 + 玻璃面板 + 青紫渐变），与首屏同为暗色体系；
// 首屏与这里的衔接由 GhibliHome 里那条极光波浪完成（见 .hero-wave）
// ============================================================
import { ref, computed, onMounted } from 'vue'
import { useSiteSettingsStore } from '@/stores/modules/siteSettings'
import { getBlogStats, getBlogCategories, getBlogTags } from '@/api/blog'

const site = useSiteSettingsStore()
const brandName = computed(() => site.brandName)
const githubUrl = computed(() => site.social.github || '')
const mail = computed(() => {
  const raw = site.footer.contactEmail || site.social.email || ''
  return /^mailto:/i.test(raw) ? raw.slice(7) : raw
})
const copied = ref(false)

// 头像：优先站点 Logo，未配置则用品牌名首字作单字徽记
const monogram = computed(() => (brandName.value || 'A').trim().charAt(0))

const tagline = computed(() => site.site.slogan || '用一个站点，装下所有好奇心。')
const contactSub = computed(() => site.footer.footerNote || '欢迎邮件交流 · 合作 / 反馈 / 交个朋友')

// 身份标签（与站点实际模块对应）
const identity = ['全栈开发', 'Web3', '数据与量化', '语音交互', '3D 可视化', '安全工具', '效率控']

const bio = [
  '你好，我是 Aurora-朱 —— 一个把「折腾」当日常的全栈开发者。白天写业务代码，晚上搭自己的数字空间：从博客、社区、知识库，到量化研究台与语音助手，能自己实现的就绝不将就。',
  '我喜欢把复杂的东西做简单：把数据、交互与工程细节揉在一起，做成顺手又好用的工具。这个站点既是我的实验场，也是一本公开的记录本 —— 技术、思考，还有一路踩过的坑。',
]

// 技能矩阵：按「投入年限 + 熟练度」呈现，比星级打分更接近真实。
// 图标用几何记号而非 emoji —— 与极简未来的视觉语言一致
const skillGroups = [
  {
    name: '前端与交互', en: 'FRONTEND', icon: '◆',
    items: [
      { name: 'Vue 3 / Pinia', level: 92, since: 2020 },
      { name: 'Tailwind / 动效', level: 86, since: 2021 },
      { name: 'Three.js / WebGL', level: 78, since: 2021 },
    ],
  },
  {
    name: '服务端与数据', en: 'BACKEND', icon: '◇',
    items: [
      { name: 'Spring Cloud', level: 90, since: 2020 },
      { name: 'MySQL / Redis', level: 86, since: 2020 },
      { name: 'Socket.IO 实时通信', level: 74, since: 2022 },
    ],
  },
  {
    name: '数据与量化', en: 'DATA · QUANT', icon: '▲',
    items: [
      { name: '多源聚合 / 熔断降级', level: 84, since: 2022 },
      { name: '策略回测 / 因子看板', level: 80, since: 2022 },
      { name: 'Python / FastAPI', level: 82, since: 2023 },
    ],
  },
  {
    name: '安全与运维', en: 'SECURITY · OPS', icon: '●',
    items: [
      { name: 'Web 安全审计', level: 80, since: 2021 },
      { name: '权限模型 / SSRF 防护', level: 82, since: 2022 },
      { name: '进程内存治理', level: 78, since: 2023 },
    ],
  },
]

const timeline = [
  { year: '2019', title: '初识编程 · 自学起步', desc: '从 Python 入门，写第一个爬虫与自动化脚本，开启「用代码解决问题」的旅程。' },
  { year: '2021', title: '深入 Java 生态 · 后端工程化', desc: '系统学习 Spring 全家桶，理解注册、配置、熔断、限流背后的取舍。' },
  { year: '2023', title: '前端沉浸 · Vue3 + Three.js', desc: '转向组合式 API 与实时渲染，专注沉浸式 Web 体验与动效叙事。' },
  { year: '2024', title: 'AI 探索 · LLM Agent 与 RAG', desc: '接入大模型推理与向量检索，搭建具备长期记忆与工具调用能力的知识库智能体。' },
  { year: '2026', title: '当下 · 综合门户与开源沉淀', desc: '把学习与项目沉淀为这个 Web3 门户，持续输出技术文章与开源实践。' },
]

// 代表作品：全部指向站内真实模块，不做占位
const projects = [
  { icon: '▲', title: '量化研究台', to: '/quant', desc: '多源行情聚合与熔断降级、策略引擎与回测，把零散行情整理成能直接看懂的决策视图。' },
  { icon: '◆', title: 'AI 知识库智能体', to: '/ai', desc: '检索增强 + 多模型路由的问答体，支持长期记忆与工具调用，答案带引用溯源。' },
  { icon: '◈', title: '3D 粒子空间', to: '/three', desc: '数万粒子在 GPU 中实时演化，鼠标拖拽旋转视角，沉浸式星云环绕。' },
  { icon: '◇', title: '星途语音助手', to: '/tools', desc: '多通道 LLM 轮询兜底 + 神经网络语音流水线，1 秒内首字响应、句间无静音空档。' },
]

// 数据统计：文章 / 分类 / 标签 / 累计字数，全部取真实接口值，不可用时降级为「—」
const stats = ref([
  { label: '篇文章', value: '—' },
  { label: '个分类', value: '—' },
  { label: '个标签', value: '—' },
  { label: '累计字数', value: '—' },
])

function fmtNum(n) {
  if (n == null || Number.isNaN(n)) return '—'
  return n >= 10000 ? (n / 10000).toFixed(1) + 'w' : String(n)
}

// 投入年限：从开始接触的年份算起
function yearsOf(since) {
  return Math.max(1, new Date().getFullYear() - since)
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
  stats.value = [
    { label: '篇文章', value: fmtNum(d.totalArticles ?? d.articleCount ?? 0) },
    { label: '个分类', value: fmtNum(Array.isArray(catArr) ? catArr.length : 0) },
    { label: '个标签', value: fmtNum(Array.isArray(tagArr) ? tagArr.length : 0) },
    { label: '累计字数', value: fmtNum(d.totalWords ?? null) },
  ]
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
   极简未来：深空底 + 细网格 + 玻璃面板 + 青紫渐变
   整块不透明，把固定的全局背景层（星野 / 云海）完全遮住，
   于是首屏的星野到这里的网格是一次干净的转场；
   交界由 GhibliHome 的 .hero-wave 负责（波浪底色与本区块底色同为 --bg）
   ============================================================ */
.about-band {
  --bg: #08090f;
  --acc1: #00d4ff;
  --acc2: #8b7cf6;
  --line: rgba(255, 255, 255, 0.08);
  --txt: #e8ecf4;
  --dim: rgba(232, 236, 244, 0.6);
  --dim2: rgba(232, 236, 244, 0.38);
  --card: rgba(255, 255, 255, 0.035);

  position: relative;
  z-index: 3;
  background: var(--bg);
  color: var(--txt);
  overflow: hidden;
}

/* 底纹：中心一片极淡的青色辉光 + 细网格；网格自上而下渐显，
   让区块顶边保持纯色，与上方波浪的收尾严丝合缝 */
.about-band::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    radial-gradient(76% 56% at 50% 24%, rgba(0, 212, 255, 0.08), transparent 70%),
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: auto, 44px 44px, 44px 44px;
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, #000 9%, #000 86%, transparent 100%);
  mask-image: linear-gradient(180deg, transparent 0%, #000 9%, #000 86%, transparent 100%);
}

/* 顶部一层极柔的极光带，让「星野 → 网格」的换场像是有意为之 */
.about-band::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 220px;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(0, 212, 255, 0.075) 0%, rgba(139, 124, 246, 0.03) 44%, transparent 100%);
}

.about-inner {
  position: relative;
  z-index: 1;
  max-width: 1080px;
  margin: 0 auto;
  padding: clamp(58px, 7.5vw, 104px) clamp(20px, 4vw, 40px) clamp(48px, 6vw, 84px);
}

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
/* 各板块自上而下依次错峰入场 */
.about-inner > [data-reveal]:nth-child(1) { --d: 0ms; }
.about-inner > [data-reveal]:nth-child(2) { --d: 80ms; }
.about-inner > [data-reveal]:nth-child(3) { --d: 150ms; }
.about-inner > [data-reveal]:nth-child(4) { --d: 210ms; }
.about-inner > [data-reveal]:nth-child(5) { --d: 270ms; }
.about-inner > [data-reveal]:nth-child(6) { --d: 320ms; }
.about-inner > [data-reveal]:nth-child(7) { --d: 370ms; }

/* ── 0. 档案卡 ── */
.pf {
  display: flex;
  align-items: center;
  gap: clamp(22px, 3.4vw, 44px);
}

.pf-avatar {
  flex: none;
  position: relative;
  width: clamp(96px, 12vw, 132px);
  height: clamp(96px, 12vw, 132px);
  border-radius: 50%;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: linear-gradient(150deg, rgba(0, 212, 255, 0.14), rgba(139, 124, 246, 0.14));
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow:
    inset 0 0 0 1px rgba(0, 212, 255, 0.12),
    0 0 40px -12px rgba(0, 212, 255, 0.6),
    0 22px 40px -26px rgba(0, 0, 0, 0.9);
}
.pf-avatar img { width: 100%; height: 100%; object-fit: cover; }
.pf-mono {
  font-family: var(--font-mono);
  font-size: clamp(38px, 5vw, 56px);
  line-height: 1;
  background: linear-gradient(150deg, #d6f6ff, #7cc6ff 52%, #a89bfb);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #9fe4ff;
}

.pf-body { min-width: 0; }

.pf-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.24em;
  color: var(--acc1);
  margin-bottom: 12px;
}
.pf-rule { width: 30px; height: 1px; background: linear-gradient(90deg, var(--acc1), transparent); }

.pf-name {
  font-family: var(--font-sans);
  font-size: clamp(30px, 4.4vw, 46px);
  font-weight: 700;
  line-height: 1.16;
  letter-spacing: -0.015em;
  color: #fff;
  text-shadow: 0 0 44px rgba(0, 212, 255, 0.22);
}

.pf-tagline {
  font-size: clamp(13.5px, 1.5vw, 16px);
  color: var(--dim);
  margin-top: 10px;
}

.pf-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}
.pf-chips li {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: rgba(232, 236, 244, 0.76);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.09);
  transition: all 0.22s ease;
}
.pf-chips li:hover {
  color: var(--acc1);
  background: rgba(0, 212, 255, 0.08);
  border-color: rgba(0, 212, 255, 0.42);
  box-shadow: 0 0 18px -6px rgba(0, 212, 255, 0.7);
  transform: translateY(-1px);
}

/* ── 通用板块 + 区块标题（沿用「关于」页的 [ 0x ] + 中文 + 英文 三段式） ── */
.block { margin-top: clamp(46px, 6vw, 78px); }
.block-last { margin-bottom: 0; }

.block-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 12px;
  margin-bottom: clamp(20px, 2.6vw, 28px);
  border-bottom: 1px solid var(--line);
}
.block-num {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.2em;
  color: var(--acc1);
  text-shadow: 0 0 16px rgba(0, 212, 255, 0.5);
}
.block-title {
  font-family: var(--font-sans);
  font-size: clamp(19px, 2.4vw, 25px);
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #fff;
}
.block-en {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.26em;
  color: var(--dim2);
}

/* ── [ 01 ] 关于我 ── */
.bio p {
  max-width: 68ch;
  font-size: clamp(14px, 1.45vw, 15.5px);
  line-height: 2.05;
  color: var(--dim);
}
.bio p + p { margin-top: 14px; }

/* ── [ 02 ] 技能矩阵 ── */
.sk-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: clamp(22px, 3vw, 40px) clamp(26px, 4vw, 56px);
}
.sk-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 10px;
  margin-bottom: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}
.sk-icon { font-size: 13px; color: var(--acc2); text-shadow: 0 0 14px rgba(139, 124, 246, 0.7); }
.sk-titles { min-width: 0; }
.sk-name {
  font-size: 13.5px;
  font-weight: 600;
  color: rgba(232, 236, 244, 0.94);
}
.sk-en {
  display: block;
  margin-top: 2px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.2em;
  color: var(--dim2);
}
.sk-items { display: flex; flex-direction: column; gap: 12px; }
.sk-item-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
}
.sk-item-name { font-size: 12.5px; color: rgba(232, 236, 244, 0.7); }
.sk-years {
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: var(--dim2);
}
.sk-bar {
  display: block;
  height: 3px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
}
.sk-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--acc1), var(--acc2));
  box-shadow: 0 0 9px rgba(0, 212, 255, 0.45);
}

/* ── [ 03 ] 个人时间线 ── */
.timeline { position: relative; padding-left: 6px; }
.tl-node { position: relative; padding-left: 26px; padding-bottom: 20px; }
.tl-node:last-child { padding-bottom: 0; }
.tl-dot {
  position: absolute;
  left: 0;
  top: 7px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--acc1);
  box-shadow: 0 0 0 3px rgba(0, 212, 255, 0.14), 0 0 14px rgba(0, 212, 255, 0.8);
}
.tl-line {
  position: absolute;
  left: 4px;
  top: 18px;
  width: 1px;
  height: calc(100% - 18px);
  background: linear-gradient(180deg, rgba(0, 212, 255, 0.36), rgba(139, 124, 246, 0.12));
}
.tl-year {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.16em;
  color: var(--acc1);
  margin-bottom: 5px;
}
.tl-title {
  font-size: 15.5px;
  font-weight: 600;
  color: #fff;
}
.tl-desc {
  margin-top: 5px;
  max-width: 62ch;
  font-size: 13px;
  line-height: 1.8;
  color: var(--dim);
}

/* ── [ 04 ] 代表作品 ── */
.pj-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: clamp(14px, 1.8vw, 20px);
}
.pj-card {
  position: relative;
  padding: 20px 20px 18px;
  border-radius: 14px;
  background: var(--card);
  border: 1px solid var(--line);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: transform 0.28s cubic-bezier(0.22, 0.61, 0.36, 1), box-shadow 0.28s ease, border-color 0.28s ease;
}
.pj-card:hover {
  transform: translateY(-3px);
  border-color: rgba(0, 212, 255, 0.36);
  box-shadow: 0 22px 44px -26px rgba(0, 212, 255, 0.55);
}
.pj-idx {
  position: absolute;
  right: 16px; top: 14px;
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.14em;
  color: rgba(232, 236, 244, 0.3);
}
.pj-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  padding-right: 28px;
}
.pj-icon { font-size: 12px; color: var(--acc1); }
.pj-desc {
  margin-top: 9px;
  font-size: 13px;
  line-height: 1.85;
  color: var(--dim);
}
.pj-link {
  display: inline-block;
  margin-top: 12px;
  font-size: 12px;
  color: var(--acc1);
  border-bottom: 1px solid rgba(0, 212, 255, 0.32);
  padding-bottom: 1px;
  transition: border-color 0.2s ease, color 0.2s ease;
}
.pj-link:hover { color: #7fe6ff; border-color: #7fe6ff; }

/* ── [ 05 ] 数据仪表盘 ── */
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  padding-top: clamp(4px, 1vw, 10px);
}
.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 16px;
  border-left: 1px solid rgba(255, 255, 255, 0.09);
}
.stat:first-child { padding-left: 0; border-left: 0; }
.stat-num {
  font-family: var(--font-mono);
  font-size: clamp(22px, 2.6vw, 30px);
  font-weight: 700;
  line-height: 1.1;
  background: linear-gradient(96deg, #ffffff 6%, #7fe6ff 52%, #a89bfb 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #cfefff;
}
.stat-label {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--dim2);
}

/* ── [ 06 ] 联系方式 ── */
.contact {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
.contact-lead {
  font-size: clamp(15px, 1.7vw, 18px);
  font-weight: 600;
  color: #fff;
}
.contact-sub { margin-top: 6px; font-size: 12.5px; color: var(--dim2); }
.contact-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.cn-btn {
  padding: 7px 16px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 600;
  color: #06080f;
  background: linear-gradient(100deg, var(--acc1), var(--acc2));
  border: 1px solid transparent;
  transition: all 0.22s ease;
}
.cn-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px -12px rgba(0, 212, 255, 0.8);
  filter: brightness(1.08);
}
.cn-btn.is-ghost {
  color: rgba(232, 236, 244, 0.86);
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.12);
  font-weight: 500;
}
.cn-btn.is-ghost:hover {
  color: var(--acc1);
  background: rgba(0, 212, 255, 0.08);
  border-color: rgba(0, 212, 255, 0.42);
  box-shadow: none;
  filter: none;
}

/* ── 响应式 ── */
@media (max-width: 900px) {
  .sk-grid { grid-template-columns: 1fr; }
  .pj-grid { grid-template-columns: 1fr; }
}
@media (max-width: 680px) {
  .pf { flex-direction: column; align-items: flex-start; }
  .stats { grid-template-columns: repeat(2, 1fr); gap: 16px 14px; }
  .stat:nth-child(3) { padding-left: 0; border-left: 0; }
  .block-en { display: none; }
  .contact { flex-direction: column; align-items: flex-start; }
}

/* 尊重系统减弱动效设置 */
@media (prefers-reduced-motion: reduce) {
  [data-reveal] { animation: none; }
  .pj-card:hover, .cn-btn:hover, .pf-chips li:hover { transform: none; }
}
</style>

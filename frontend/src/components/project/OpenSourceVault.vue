<template>
  <section class="vault">
    <!-- 区头 -->
    <div class="sec-head">
      <span class="sec-num">[ 04 ]</span>
      <h2>开源仓库馆</h2>
      <p>OPEN SOURCE VAULT · 13 REPOS</p>
      <p class="sec-desc">
        收录于 <code>F:\project\github-myblog</code> 的 13 个开源项目，按体积从小到大整理入库：
        轻量项目已内嵌为站内可运行的工具页，大型项目提供深度介绍与模块导览。
      </p>
    </div>

    <!-- 统计条 -->
    <div class="vault-stats">
      <div class="vstat"><span class="num">{{ repos.length }}</span><span class="lab">仓库总数</span></div>
      <div class="vstat"><span class="num">{{ embeddedCount }}</span><span class="lab">已内嵌运行</span></div>
      <div class="vstat"><span class="num">{{ totalSizeText }}</span><span class="lab">总体积</span></div>
      <div class="vstat"><span class="num">{{ categoryCounts.quant }}</span><span class="lab">量化类</span></div>
    </div>

    <!-- 筛选 + 排序 -->
    <div class="vault-ctrl">
      <div class="tabs">
        <button v-for="c in filterTabs" :key="c.id" :class="['tab', { on: filter === c.id }]" @click="filter = c.id">
          {{ c.name }}<i>{{ c.count }}</i>
        </button>
      </div>
      <button class="sort-btn" @click="asc = !asc">
        {{ asc ? '▲ 体积从小到大' : '▼ 体积从大到小' }}
      </button>
    </div>

    <!-- 仓库卡片 -->
    <div class="repo-grid">
      <article v-for="(r, i) in filteredRepos" :key="r.name" class="repo-card" :class="{ open: expanded === r.name }">
        <div class="rc-top" @click="toggle(r.name)">
          <span class="rc-rank">{{ String(i + 1).padStart(2, '0') }}</span>
          <div class="rc-main">
            <div class="rc-title-row">
              <h3>{{ r.title }}</h3>
              <span v-if="r.embedded" class="badge embedded">● 已内嵌</span>
              <span class="badge size">{{ r.sizeText }}</span>
            </div>
            <p class="rc-desc">{{ r.desc }}</p>
            <div class="rc-tags">
              <span v-for="t in r.tags" :key="t">{{ t }}</span>
            </div>
          </div>
          <span class="rc-cat" :data-cat="r.cat">{{ catLabel(r.cat) }}</span>
          <span class="rc-arrow">{{ expanded === r.name ? '−' : '+' }}</span>
        </div>

        <transition name="fold">
          <div v-if="expanded === r.name" class="rc-body">
            <ul class="rc-points">
              <li v-for="p in r.points" :key="p">{{ p }}</li>
            </ul>
            <div class="rc-links">
              <router-link v-if="r.embedded" :to="r.embedded.to" class="rc-link primary">▶ 站内{{ r.embedded.label }}</router-link>
              <a v-if="r.repoUrl" :href="r.repoUrl" target="_blank" rel="noopener" class="rc-link">↗ 原仓库</a>
              <a v-if="r.docsUrl" :href="r.docsUrl" target="_blank" rel="noopener" class="rc-link">↗ 文档</a>
              <span class="rc-folder">本地：{{ r.name }}</span>
            </div>
          </div>
        </transition>
      </article>
    </div>
    <div v-if="!filteredRepos.length" class="empty">该分类下暂无仓库</div>
  </section>
</template>

<script setup>
// ============================================================
// 开源仓库馆：github-myblog 13 个仓库的站内集成总览
// 按体积排序、分类筛选、展开看亮点，已内嵌项目可直达工具页
// ============================================================
import { ref, computed } from 'vue'

const repos = [
  {
    name: 'public-apis-master', title: 'Public APIs', cat: 'tool', sizeKB: 50,
    desc: 'GitHub 140k+ Star 的公共 API 目录镜像，汇聚互联网上可免费调用的开放接口。',
    tags: ['API 目录', '文档合集'], repoUrl: 'https://github.com/public-apis/public-apis',
    points: ['覆盖动物、金融、地理、开放数据等 40+ 领域的免费 API 索引', '本地副本含接口校验脚本，可验证 API 可用性', '与站内「API 工具中心」互补，作为外部接口选型参考'],
  },
  {
    name: 'public-api-lists-master', title: 'Public API Lists', cat: 'tool', sizeKB: 93,
    desc: '另一个社区维护的公共 API 合集，与 Public APIs 互为补充的接口名录。',
    tags: ['API 目录', '文档合集'], repoUrl: 'https://github.com/public-api-lists/public-api-lists',
    points: ['社区持续维护的免费/无密钥 API 清单', '按主题分类，适合快速寻找原型验证用接口', '可配合站内 JSON 格式化工具调试返回结果'],
  },
  {
    name: 'carrot-main', title: 'Carrot · 免费 AI 站点导航', cat: 'ai', sizeKB: 140,
    desc: '免费 ChatGPT / Claude / Gemini 等数百个 AI 站点的分类导航合集，已提炼并入站内「网站分享」。',
    tags: ['AI 导航', '静态数据'],
    embedded: { to: '/tools/sites', label: '网站分享 · AI导航' },
    points: ['6 大分组：热门精选 / Agent与自动化 / 模型API算力 / 编程开发 / 图像视觉 / 音视频数字人', '站内 /tools/sites 的 AI导航 tab 提炼 32 个高频优质站点，支持搜索过滤', '数据源为本地静态提炼，加载零延迟'],
  },
  {
    name: 'leak-check-main', title: 'Leak Check · 信息泄露查询', cat: 'tool', sizeKB: 158,
    desc: '个人信息泄露检查工具：查询邮箱/手机号是否出现在公开泄露库中。',
    tags: ['Python', 'SQLite', '安全'],
    points: ['Python 后端 + SQLite 泄露数据库架构', '适合自部署，查询逻辑与脱敏展示值得借鉴', '作为安全类工具的后端服务形态保留介绍'],
  },
  {
    name: 'GPT_API_free-main', title: 'GPT-API-free / DeepSeek-API-free', cat: 'ai', sizeKB: 231,
    desc: '免费使用 GPT / DeepSeek / Claude / Gemini 的 OpenAI 协议中转服务，国内直连无需代理。',
    tags: ['API 中转', 'OpenAI 协议', '免费'], repoUrl: 'https://github.com/chatanywhere/GPT_API_free',
    docsUrl: 'https://docs.chatanywhere.tech/',
    points: ['统一 OpenAI 标准协议，换模型名即可接入各厂商', '免费版支持 gpt-5/4o、deepseek-r1/v3 等系列', '本站 AI 模块的第三方渠道配置可直接对接'],
  },
  {
    name: 'worth-buy-stocks-main', title: '什么值得买 · 美股版', cat: 'quant', sizeKB: 1712,
    desc: '把趋势交易纪律落成可复现的美股评分流程：按价量结构与风险约束判断当前是否适合参与。',
    tags: ['Python', 'Alpaca', '纪律评分'], repoUrl: 'https://github.com/starriv/worth-buy-stocks',
    points: ['输出 是/观察/否/持仓需减风险 四级明确结论', '自动生成入场、回踩、止损与 2R/3R 止盈价位', '全市场扫描 NYSE/NASDAQ + 期权价差组合筛选'],
  },
  {
    name: '60s-main', title: '60秒 API · 每天60秒读懂世界', cat: 'tool', sizeKB: 3001,
    desc: '知名每日新闻快报 API 服务，9 组 75 个端点已全部接入站内 API 广场，可在线传参调用。',
    tags: ['Deno/Bun/Node', 'REST API', '新闻'],
    embedded: { to: '/tools/api-plaza', label: '60秒 API 广场' },
    points: ['每日速览/热榜聚合/资讯排行/轻松一刻/生活查询/影音文娱/开发者工具/计算类等 75 个端点', 'API 广场按 JSON/图片/RSS 分类型渲染，参数自动生成表单', '一个端点不少，全部可在线调用'],
  },
  {
    name: 'Financial-API-main', title: 'Financial-API · 同花顺金融数据', cat: 'quant', sizeKB: 6190,
    desc: '同花顺开放数据服务封装：A 股行情、财务报表、基金数据的 API/SDK 合集。',
    tags: ['Python SDK', 'A股数据', '金融'],
    points: ['覆盖实时行情、K 线历史、财务三表与基金净值', '与站内 quant-py-service 的行情源互补做数据冗余', '适合作为量化数据底座的备选数据通道'],
  },
  {
    name: 'netease-cloud-music-master', title: 'Netease Cloud Music (Go)', cat: 'media', sizeKB: 11963,
    desc: '网易云音乐 Golang API 接口 + 命令行工具套件 ncmctl，一键完成每日任务。',
    tags: ['Go', 'CLI', '音乐 API'], repoUrl: 'https://github.com/chaunsin/netease-cloud-music',
    points: ['扫码/Cookie/CookieCloud 多种登录方式', '签到、刷歌、云盘、歌单等全量接口覆盖', '本站音乐馆使用的前端播放链路与其接口设计同源'],
  },
  {
    name: 'go-stock-dev', title: 'Go-Stock · AI 股票分析', cat: 'quant', sizeKB: 61356,
    desc: 'Go + Vue3 + Wails 的桌面端 AI 股票分析工具，集成大模型摘要与实时盯盘。',
    tags: ['Go', 'Vue3', 'Wails', 'LLM'], repoUrl: 'https://github.com/ArvinLovegood/go-stock',
    points: ['自选股实时行情 + AI 消息面摘要与情绪分析', '数据源覆盖新浪/腾讯/东财多通道', '架构上前端 Vue3+NaiveUI+ECharts，与本站量化页技术栈一致'],
  },
  {
    name: 'abu-master', title: '阿布量化 AbuQuant', cat: 'quant', sizeKB: 74589,
    desc: '经典 Python 量化系统 abupy：覆盖数据、因子、回测、 optimization、实盘全流程的量化框架。',
    tags: ['Python', 'abupy', '回测框架'], repoUrl: 'https://github.com/bbfamily/abu',
    points: ['《量化交易之路》配套源码 + abupy_lecture 教程', '内置择时/选股/仓位管理/滑点资金曲线全流程', '官方提供上证/纳指/恒生等 25+ 指数实时 AI 研报'],
  },
  {
    name: 'ai_quant_trade-master', title: 'AI量化交易操盘手', cat: 'quant', sizeKB: 126249,
    desc: '一站式 AI 量化平台：大模型应用、因子挖掘、传统/机器学习/强化学习策略与辅助操盘工具。',
    tags: ['Python', 'LLM', '因子挖掘'], repoUrl: 'https://github.com/charliedream1/ai_quant_trade',
    points: ['从学习、模拟到实盘全流程覆盖', '策略谱系：LLM/因子/传统/ML/DL/RL/图网络/高频', '含盯盘助手、股票推荐等实用操盘工具集'],
  },
  {
    name: 'QuantMind-master', title: 'QuantMind 量化大脑', cat: 'quant', sizeKB: 137532,
    desc: 'AI 原生量化投研平台：深度集成微软 Qlib / RD-Agent / TradingAgents，打通量化全流程闭环。',
    tags: ['Python', 'TypeScript', 'Qlib', 'Multi-Agent'],
    points: ['数据底座→因子挖掘→模型训练→推理→回测→QMT 实盘→监控', '13 种模型工场，300+ 维特征自动挖掘 Alpha', '支持 A 股、港股、美股、期货与区块链五大市场'],
  },
]

const filter = ref('all')
const asc = ref(true)
const expanded = ref('')

const filterTabs = computed(() => {
  const count = id => repos.filter(r => id === 'all' || r.cat === id).length
  return [
    { id: 'all', name: '全部', count: count('all') },
    { id: 'quant', name: '量化', count: count('quant') },
    { id: 'ai', name: 'AI', count: count('ai') },
    { id: 'tool', name: '工具', count: count('tool') },
    { id: 'media', name: '媒体', count: count('media') },
  ]
})

const filteredRepos = computed(() => {
  const list = repos.filter(r => filter.value === 'all' || r.cat === filter.value)
  return [...list].sort((a, b) => (asc.value ? a.sizeKB - b.sizeKB : b.sizeKB - a.sizeKB))
})

const embeddedCount = computed(() => repos.filter(r => r.embedded).length)
const totalSizeText = computed(() => formatSize(repos.reduce((n, r) => n + r.sizeKB, 0)))
const categoryCounts = {
  quant: repos.filter(r => r.cat === 'quant').length,
}

function formatSize(kb) {
  if (kb >= 1024 * 1024) return (kb / 1024 / 1024).toFixed(2) + ' GB'
  if (kb >= 1024) return (kb / 1024).toFixed(1) + ' MB'
  return kb + ' KB'
}

function catLabel(c) {
  return { quant: '量化', ai: 'AI', tool: '工具', media: '媒体' }[c] || c
}

function toggle(name) { expanded.value = expanded.value === name ? '' : name }
</script>

<style scoped>
.vault { position: relative; padding: 5rem 2rem; max-width: 1080px; margin: 0 auto; }

.sec-head { text-align: center; margin-bottom: 2.6rem; }
.sec-num { color: #667eea; font-family: monospace; font-size: 0.75rem; letter-spacing: 0.25em; }
.sec-head h2 { font-size: clamp(1.6rem, 3.4vw, 2.4rem); font-weight: 800; margin: 0.5rem 0 0.3rem; color: #fff; }
.sec-head p { font-size: 0.7rem; letter-spacing: 0.3em; color: rgba(255,255,255,0.6); }
.sec-desc { max-width: 62ch; margin: 1rem auto 0; font-size: 0.85rem; line-height: 1.9; letter-spacing: normal; color: rgba(255,255,255,0.55); }
.sec-desc code { color: #7fd9ff; font-size: 0.78rem; background: rgba(0,212,255,0.08); padding: 0.1rem 0.4rem; border-radius: 4px; }

/* 统计条 */
.vault-stats { display: flex; justify-content: center; gap: 2.2rem; flex-wrap: wrap; margin-bottom: 2.2rem; padding: 1.1rem 2rem; background: rgba(12,12,28,0.6); border: 1px solid rgba(103,232,249,0.16); border-radius: 16px; backdrop-filter: blur(12px); }
.vstat { display: flex; flex-direction: column; align-items: center; gap: 0.25rem; }
.vstat .num { font-size: 1.5rem; font-weight: 800; color: #67e8f9; font-family: 'Courier New', monospace; text-shadow: 0 0 12px rgba(103,232,249,0.5); }
.vstat .lab { font-size: 0.66rem; letter-spacing: 0.2em; color: rgba(255,255,255,0.5); }

/* 筛选 + 排序 */
.vault-ctrl { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.6rem; }
.tabs { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.tab { padding: 0.42rem 1rem; border-radius: 999px; font-size: 0.76rem; color: rgba(255,255,255,0.6); background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); cursor: pointer; transition: all 0.25s; }
.tab i { font-style: normal; margin-left: 0.35rem; font-size: 0.62rem; opacity: 0.6; }
.tab:hover { color: #9db2ff; border-color: rgba(102,126,234,0.4); }
.tab.on { color: #fff; background: linear-gradient(135deg, rgba(102,126,234,0.35), rgba(0,212,255,0.22)); border-color: rgba(0,212,255,0.45); box-shadow: 0 0 16px rgba(0,212,255,0.12); }
.sort-btn { padding: 0.42rem 1rem; border-radius: 999px; font-size: 0.76rem; color: #7fd9ff; background: rgba(0,212,255,0.06); border: 1px solid rgba(0,212,255,0.28); cursor: pointer; transition: all 0.25s; }
.sort-btn:hover { background: rgba(0,212,255,0.12); }

/* 卡片 */
.repo-grid { display: flex; flex-direction: column; gap: 0.8rem; }
.repo-card { background: rgba(12,12,28,0.65); border: 1px solid rgba(255,255,255,0.07); border-radius: 16px; overflow: hidden; transition: border-color 0.3s, box-shadow 0.3s; }
.repo-card:hover { border-color: rgba(102,126,234,0.45); box-shadow: 0 0 28px rgba(102,126,234,0.1); }
.repo-card.open { border-color: rgba(0,212,255,0.4); }
.rc-top { display: flex; align-items: center; gap: 1.1rem; padding: 1.05rem 1.3rem; cursor: pointer; }
.rc-rank { font-family: 'Courier New', monospace; font-size: 0.85rem; color: #667eea; flex-shrink: 0; width: 2rem; }
.rc-main { flex: 1; min-width: 0; }
.rc-title-row { display: flex; align-items: center; gap: 0.6rem; flex-wrap: wrap; }
.rc-title-row h3 { font-size: 0.98rem; font-weight: 700; color: #fff; }
.badge { font-size: 0.62rem; padding: 0.14rem 0.55rem; border-radius: 999px; letter-spacing: 0.06em; }
.badge.embedded { color: #6ee7b7; background: rgba(16,185,129,0.12); border: 1px solid rgba(16,185,129,0.35); }
.badge.size { color: #7fd9ff; background: rgba(0,212,255,0.07); border: 1px solid rgba(0,212,255,0.25); font-family: monospace; }
.rc-desc { margin-top: 0.35rem; font-size: 0.78rem; line-height: 1.7; color: rgba(255,255,255,0.6); }
.rc-tags { display: flex; gap: 0.4rem; flex-wrap: wrap; margin-top: 0.55rem; }
.rc-tags span { font-size: 0.62rem; font-family: monospace; color: rgba(157,178,255,0.85); padding: 0.12rem 0.5rem; border-radius: 4px; background: rgba(102,126,234,0.1); border: 1px solid rgba(102,126,234,0.2); }
.rc-cat { flex-shrink: 0; font-size: 0.68rem; letter-spacing: 0.15em; color: rgba(255,255,255,0.55); }
.rc-cat[data-cat="quant"] { color: #f0abfc; }
.rc-cat[data-cat="ai"] { color: #7fd9ff; }
.rc-cat[data-cat="tool"] { color: #6ee7b7; }
.rc-cat[data-cat="media"] { color: #fda4af; }
.rc-arrow { flex-shrink: 0; width: 1.6rem; height: 1.6rem; display: grid; place-items: center; border-radius: 50%; border: 1px solid rgba(255,255,255,0.12); color: rgba(255,255,255,0.6); font-size: 0.9rem; transition: all 0.3s; }
.repo-card.open .rc-arrow { background: rgba(0,212,255,0.15); color: #67e8f9; border-color: rgba(0,212,255,0.4); }

/* 展开体 */
.rc-body { padding: 0 1.3rem 1.2rem 3.65rem; }
.rc-points { list-style: none; display: flex; flex-direction: column; gap: 0.4rem; margin-bottom: 0.9rem; }
.rc-points li { position: relative; padding-left: 1.1rem; font-size: 0.78rem; line-height: 1.7; color: rgba(255,255,255,0.68); }
.rc-points li::before { content: '◆'; position: absolute; left: 0; top: 0.05rem; font-size: 0.55rem; color: #67e8f9; }
.rc-links { display: flex; align-items: center; gap: 0.7rem; flex-wrap: wrap; }
.rc-link { display: inline-flex; align-items: center; gap: 0.3rem; padding: 0.4rem 0.95rem; border-radius: 8px; font-size: 0.74rem; color: #9db2ff; background: rgba(102,126,234,0.1); border: 1px solid rgba(102,126,234,0.3); text-decoration: none; transition: all 0.25s; }
.rc-link:hover { color: #fff; background: rgba(102,126,234,0.25); }
.rc-link.primary { color: #04121a; font-weight: 700; background: linear-gradient(135deg, #67e8f9, #22d3ee); border-color: transparent; box-shadow: 0 4px 18px rgba(0,212,255,0.25); }
.rc-link.primary:hover { box-shadow: 0 6px 26px rgba(0,212,255,0.45); color: #04121a; transform: translateY(-1px); }
.rc-folder { font-size: 0.66rem; font-family: monospace; color: rgba(255,255,255,0.35); }

/* 展开动画 */
.fold-enter-active, .fold-leave-active { transition: all 0.3s ease; overflow: hidden; }
.fold-enter-from, .fold-leave-to { opacity: 0; transform: translateY(-8px); }

.empty { text-align: center; padding: 3rem 0; font-size: 0.8rem; color: rgba(255,255,255,0.4); }

@media (max-width: 640px) {
  .vault { padding: 3.5rem 1.1rem; }
  .vault-stats { gap: 1.2rem; padding: 0.9rem 1.2rem; }
  .rc-top { flex-wrap: wrap; gap: 0.7rem; }
  .rc-cat { order: 3; }
  .rc-body { padding-left: 1.3rem; }
}
</style>

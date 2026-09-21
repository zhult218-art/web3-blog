<template>
  <!-- 全站导览地图：同类功能归组陈列，让所有界面位置一目了然 -->
  <div class="sitemap-section" v-reveal>
    <div class="sitemap-inner">
      <div class="sm-head motions-reveal">
        <span class="sm-num">[ 05 · SITE MAP ]</span>
        <h2>全站导览地图</h2>
        <p>AURORA-朱 · ALL LOCATIONS</p>
      </div>

      <!-- 站点统计：模块数 / 页面数 / 分类数 -->
      <div class="sm-stats motions-reveal delay-100">
        <div class="sm-stat" v-for="s in stats" :key="s.label">
          <span class="sm-stat-val">{{ s.value }}</span>
          <span class="sm-stat-label">{{ s.label }}</span>
        </div>
      </div>

      <div class="sm-grid">
        <div
          v-for="(g, gi) in groups"
          :key="g.name"
          class="sm-group motions-reveal"
          :class="`delay-${(gi % 4) * 100 + 100}`"
        >
          <div class="sm-group-head">
            <span class="sm-group-icon">{{ g.icon }}</span>
            <div>
              <h3 class="sm-group-title">{{ g.name }}</h3>
              <span class="sm-group-en">{{ g.en }}</span>
            </div>
            <span class="sm-group-count">{{ g.items.length }}</span>
          </div>
          <div class="sm-links">
            <router-link
              v-for="item in g.items"
              :key="item.path"
              :to="item.path"
              class="sm-link"
            >
              <span class="sm-link-icon">{{ item.icon }}</span>
              <span class="sm-link-text">
                <span class="sm-link-name">{{ item.label }}</span>
                <span class="sm-link-sub">{{ item.sub }}</span>
              </span>
              <span class="sm-link-arrow">→</span>
            </router-link>
          </div>
        </div>
      </div>

      <!-- 底部签名 -->
      <div class="sm-footer motions-reveal delay-300">
        <span class="sm-footer-line"></span>
        <span class="sm-footer-text">END OF MAP · 共 {{ totalCount }} 个入口 · 持续扩展中</span>
        <span class="sm-footer-line"></span>
      </div>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 全站导览地图（SiteMap）：按内容类型分组陈列全部页面，
// 插在首页各区块与页脚之间，帮助用户快速定位每个功能的位置。
// 含站点统计、8 大分组、每组多链接卡片、滚动入场动画。
// ============================================================
import { computed } from 'vue'

const groups = [
  {
    name: '主航道', en: 'MAIN NAVIGATION', icon: '🧭',
    items: [
      { path: '/', label: '主城广场', sub: '首页', icon: '🏠' },
      { path: '/blog', label: '冒险日志', sub: '博客', icon: '📖' },
      { path: '/community', label: '冒险者酒馆', sub: '社区', icon: '🍺' },
      { path: '/shop', label: '魔法杂货铺', sub: '商城', icon: '🛒' },
      { path: '/media', label: '幻镜水晶', sub: '书影音', icon: '🎬' },
    ],
  },
  {
    name: '智能分析', en: 'AI & QUANT', icon: '🤖',
    items: [
      { path: '/quant', label: '占星推演', sub: '量化分析', icon: '📈' },
      { path: '/ai-station', label: 'AI 中转站', sub: '令牌·模型', icon: '🛰️' },
      { path: '/ai', label: 'AI 引擎', sub: '多模态助手', icon: '🧠' },
      { path: '/architecture', label: '架构图鉴', sub: '微服务拓扑', icon: '⚙️' },
    ],
  },
  {
    name: '工具资源', en: 'TOOLS & RESOURCES', icon: '🛠️',
    items: [
      { path: '/tools', label: '炼金道具', sub: '实用工具', icon: '🔧' },
      { path: '/tools/api', label: 'API 中心', sub: '第三方接口', icon: '🔌' },
      { path: '/tools/sites', label: '分享网站', sub: '优质外链', icon: '🔗' },
      { path: '/software', label: '魔导工坊', sub: '软件中心', icon: '📦' },
      { path: '/resources', label: '宝物仓库', sub: '资源库', icon: '🗃️' },
      { path: '/upload', label: '星空仓库', sub: '资源上传', icon: '☁️' },
    ],
  },
  {
    name: '影音相册', en: 'MEDIA & ALBUM', icon: '🎵',
    items: [
      { path: '/album', label: '回忆水晶', sub: '相册集', icon: '🖼️' },
      { path: '/media', label: '影音图鉴', sub: '电影·剧集', icon: '🎞️' },
    ],
  },
  {
    name: '创作展示', en: 'CREATIONS', icon: '🎨',
    items: [
      { path: '/nails', label: '美甲小铺', sub: '预约·作品集', icon: '💅' },
      { path: '/three', label: '星空漫游', sub: '3D 粒子空间', icon: '🌌' },
      { path: '/architecture', label: '架构图鉴', sub: '微服务拓扑', icon: '🗺️' },
    ],
  },
  {
    name: '交流关于', en: 'COMMUNITY & ABOUT', icon: '💬',
    items: [
      { path: '/link', label: '同伴名册', sub: '友人帐', icon: '🤝' },
      { path: '/comments', label: '传音魔石', sub: '留言板', icon: '✉️' },
      { path: '/about', label: '世界设定集', sub: '关于我们', icon: '📜' },
    ],
  },
  {
    name: '个人与支付', en: 'PROFILE & ORDERS', icon: '👤',
    items: [
      { path: '/profile', label: '个人中心', sub: '资料·公会', icon: '🪪' },
      { path: '/profile/orders', label: '我的订单', sub: '订单管理', icon: '🧾' },
      { path: '/shop/cart', label: '购物车', sub: '待结算商品', icon: '🛍️' },
    ],
  },
  {
    name: '特别企划', en: 'SPECIAL PROJECTS', icon: '✨',
    items: [
      { path: '/three', label: '粒子剧场', sub: '沉浸式 3D', icon: '✨' },
      { path: '/ai', label: 'AI 知识库', sub: 'RAG 问答', icon: '🔮' },
      { path: '/quant', label: '量化策略', sub: '实盘监控', icon: '💹' },
    ],
  },
]

const stats = [
  { value: '8', label: '功能分组' },
  { value: '28+', label: '页面入口' },
  { value: '∞', label: '持续扩展' },
]

const totalCount = computed(() => groups.reduce((n, g) => n + g.items.length, 0))
</script>

<style scoped>
.sitemap-section {
  position: relative;
  z-index: 1;
  padding: 4rem 2rem 3.5rem;
  background: linear-gradient(180deg,
    transparent 0%, rgba(10, 10, 26, 0.18) 12%,
    rgba(6, 6, 14, 0.4) 50%,
    rgba(6, 6, 14, 0.55) 100%);
}

.sitemap-inner {
  max-width: 1200px;
  margin: 0 auto;
}

.sm-head {
  text-align: center;
  margin-bottom: 2.6rem;
}

.sm-num {
  font-family: 'Courier New', monospace;
  font-size: 0.65rem;
  letter-spacing: 0.28em;
  color: #00d4ff;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.5);
}

.sm-head h2 {
  font-size: clamp(1.8rem, 4vw, 2.4rem);
  font-weight: 800;
  margin-top: 0.55rem;
  letter-spacing: 0.06em;
  background: linear-gradient(135deg, #00d4ff, #667eea 55%, #ec4899);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sm-head p {
  font-size: 0.6rem;
  letter-spacing: 0.26em;
  color: rgba(184, 197, 216, 0.75);
  margin-top: 0.4rem;
  font-family: 'Courier New', monospace;
}

/* 站点统计 */
.sm-stats {
  display: flex;
  justify-content: center;
  gap: 2.5rem;
  margin-bottom: 3rem;
  padding: 1.4rem 2rem;
  background: rgba(12, 12, 28, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  max-width: 540px;
  margin-left: auto;
  margin-right: auto;
}

.sm-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.sm-stat-val {
  font-size: 1.8rem;
  font-weight: 800;
  background: linear-gradient(135deg, #fff, #9db2ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-family: 'Courier New', monospace;
}

.sm-stat-label {
  font-size: 0.6rem;
  letter-spacing: 0.2em;
  color: rgba(255, 255, 255, 0.4);
  font-family: 'Courier New', monospace;
}

.sm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1rem;
}

.sm-group {
  padding: 1.1rem 1.2rem 1.2rem;
  border: 1px solid rgba(168, 85, 247, 0.25);
  background: linear-gradient(160deg, rgba(168, 85, 247, 0.12), transparent 60%);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  transition: border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
  position: relative;
  overflow: hidden;
}

.sm-group::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(168, 85, 247, 0.5), transparent);
}

.sm-group:hover {
  border-color: rgba(168, 85, 247, 0.6);
  box-shadow: 0 8px 28px rgba(168, 85, 247, 0.18);
  transform: translateY(-3px);
}

.sm-group-head {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding-bottom: 0.85rem;
  margin-bottom: 0.8rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sm-group-icon {
  font-size: 1.3rem;
  filter: drop-shadow(0 0 8px rgba(168, 85, 247, 0.5));
  flex-shrink: 0;
}

.sm-group-title {
  font-size: 0.82rem;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.04em;
}

.sm-group-en {
  display: block;
  font-size: 0.55rem;
  letter-spacing: 0.2em;
  color: rgba(255, 255, 255, 0.35);
  font-family: 'Courier New', monospace;
  margin-top: 2px;
}

.sm-group-count {
  margin-left: auto;
  font-size: 0.6rem;
  padding: 0.18rem 0.5rem;
  border-radius: 999px;
  color: #8fa3ff;
  background: rgba(168, 85, 247, 0.12);
  border: 1px solid rgba(168, 85, 247, 0.25);
  font-family: 'Courier New', monospace;
  white-space: nowrap;
}

.sm-links {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.sm-link {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.5rem 0.6rem;
  border-radius: 10px;
  font-size: 0.74rem;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.025);
  transition: all 0.25s ease;
  text-decoration: none;
}

.sm-link:hover {
  border-color: rgba(168, 85, 247, 0.6);
  background: rgba(168, 85, 247, 0.1);
  box-shadow: 0 0 14px rgba(168, 85, 247, 0.2);
  transform: translateX(2px);
}

.sm-link-icon {
  font-size: 0.95rem;
  flex-shrink: 0;
}

.sm-link-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
  min-width: 0;
}

.sm-link-name {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.92);
}

.sm-link-sub {
  font-size: 0.6rem;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 0.04em;
}

.sm-link-arrow {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.3);
  transition: transform 0.25s, color 0.25s;
}

.sm-link:hover .sm-link-arrow {
  color: #00d4ff;
  transform: translateX(3px);
}

/* 底部签名 */
.sm-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 2.5rem;
  padding-top: 1.5rem;
}

.sm-footer-line {
  flex: 1;
  max-width: 180px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.18), transparent);
}

.sm-footer-text {
  font-size: 0.62rem;
  letter-spacing: 0.22em;
  color: rgba(255, 255, 255, 0.4);
  font-family: 'Courier New', monospace;
  white-space: nowrap;
}

@media (min-width: 1024px) {
  .sm-grid { grid-template-columns: repeat(4, 1fr); }
  .sm-group:nth-child(1) { grid-column: span 2; }
  .sm-group:nth-child(3) { grid-column: span 2; }
  .sm-group:nth-child(8) { grid-column: span 4; }
}

@media (max-width: 640px) {
  .sitemap-section { padding: 3rem 1rem 2.5rem; }
  .sm-stats { gap: 1.5rem; padding: 1rem 1.5rem; }
  .sm-stat-val { font-size: 1.4rem; }
  .sm-footer { flex-wrap: wrap; }
  .sm-footer-text { font-size: 0.55rem; }
}
</style>

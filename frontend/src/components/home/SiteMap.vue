<template>
  <!-- 全站导览地图：同类功能归组陈列，让所有界面位置一目了然 -->
  <div class="sitemap-section">
    <div class="sitemap-inner">
      <div class="sm-head">
        <span class="sm-num">[ SITE MAP ]</span>
        <h2>全站导览</h2>
        <p>VERSE NOTE · ALL LOCATIONS</p>
      </div>

      <div class="sm-grid">
        <div v-for="g in groups" :key="g.name" class="sm-group">
          <h3 class="sm-group-title">{{ g.icon }} {{ g.name }}</h3>
          <div class="sm-links">
            <router-link v-for="item in g.items" :key="item.path" :to="item.path" class="sm-link">
              <span class="sm-link-name">{{ item.label }}</span>
              <span class="sm-link-sub">{{ item.sub }}</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 全站导览地图（SiteMap）：按内容类型分组陈列全部页面，
// 插在首页各区块与页脚之间，帮助用户快速定位每个功能的位置。
// ============================================================
const groups = [
  {
    name: '主航道', icon: '🧭',
    items: [
      { path: '/', label: '主城广场', sub: '首页' },
      { path: '/blog', label: '冒险日志', sub: '博客' },
      { path: '/community', label: '冒险者酒馆', sub: '社区' },
      { path: '/shop', label: '魔法杂货铺', sub: '商城' },
      { path: '/media', label: '幻镜水晶', sub: '书影音' },
    ],
  },
  {
    name: '智能分析', icon: '🤖',
    items: [
      { path: '/quant', label: '占星推演', sub: '量化分析' },
      { path: '/ai-station', label: 'AI 中转站', sub: '令牌·模型' },
      { path: '/ai', label: 'AI 引擎', sub: '多模态助手' },
    ],
  },
  {
    name: '工具资源', icon: '🛠️',
    items: [
      { path: '/tools', label: '炼金道具', sub: '实用工具' },
      { path: '/tools/api', label: 'API 中心', sub: '第三方接口' },
      { path: '/tools/sites', label: '分享网站', sub: '优质外链' },
      { path: '/software', label: '魔导工坊', sub: '软件中心' },
      { path: '/resources', label: '宝物仓库', sub: '资源库' },
      { path: '/upload', label: '星空仓库', sub: '资源上传' },
    ],
  },
  {
    name: '影音相册', icon: '🎵',
    items: [
      { path: '/album', label: '回忆水晶', sub: '相册集' },
    ],
  },
  {
    name: '创作展示', icon: '🎨',
    items: [
      { path: '/nails', label: '美甲小铺', sub: '预约·作品集' },
      { path: '/three', label: '星空漫游', sub: '3D 粒子空间' },
      { path: '/architecture', label: '架构图鉴', sub: '微服务拓扑' },
    ],
  },
  {
    name: '交流关于', icon: '💬',
    items: [
      { path: '/link', label: '同伴名册', sub: '友人帐' },
      { path: '/comments', label: '传音魔石', sub: '留言板' },
      { path: '/about', label: '世界设定集', sub: '关于我们' },
    ],
  },
  {
    name: '个人与支付', icon: '👤',
    items: [
      { path: '/profile', label: '个人中心', sub: '资料·公会' },
      { path: '/profile/orders', label: '我的订单', sub: '订单管理' },
      { path: '/shop/cart', label: '购物车', sub: '待结算商品' },
    ],
  },
]
</script>

<style scoped>
.sitemap-section {
  position: relative;
  z-index: 1;
  padding: 0 2rem 3.5rem;
}
.sitemap-inner {
  max-width: 1200px;
  margin: 0 auto;
}
.sm-head {
  text-align: center;
  margin-bottom: 2.2rem;
}
.sm-num {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.28em;
  color: rgba(184, 197, 216, 0.9);
}
.sm-head h2 {
  font-size: 1.6rem;
  font-weight: 800;
  margin-top: 0.4rem;
  letter-spacing: 0.08em;
  background: linear-gradient(135deg, var(--color-accent), var(--color-primary) 55%, var(--color-pink));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.sm-head p {
  font-size: 0.6rem;
  letter-spacing: 0.26em;
  color: rgba(184, 197, 216, 0.75);
  margin-top: 0.4rem;
}
.sm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
}
.sm-group {
  padding: 1.1rem 1.2rem 1.2rem;
  border: 1px solid var(--color-primary-border);
  background: linear-gradient(160deg, var(--color-primary-soft), transparent 60%);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  transition: border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}
.sm-group:hover {
  border-color: var(--color-primary);
  box-shadow: 0 8px 28px var(--color-glow-soft);
  transform: translateY(-2px);
}
@media (min-width: 1024px) {
  .sm-grid { grid-template-columns: repeat(4, 1fr); }
  .sm-group:nth-child(1) { grid-column: span 2; }
  .sm-group:nth-child(3) { grid-column: span 2; }
  .sm-group:nth-child(7) { grid-column: span 4; grid-row: 2/3; align-self: start; }
}
.sm-group-title {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--ink);
  margin-bottom: 0.7rem;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}
.sm-links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.sm-link {
  display: inline-flex;
  align-items: baseline;
  gap: 0.35rem;
  padding: 0.32rem 0.6rem;
  border-radius: 9px;
  font-size: 0.72rem;
  border: 1px solid rgba(255, 255, 255, 0.09);
  background: rgba(255, 255, 255, 0.04);
  transition: all 0.25s ease;
  white-space: nowrap;
}
.sm-link:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  box-shadow: 0 0 14px var(--color-glow-soft);
  transform: translateY(-1px);
}
.sm-link-name { font-weight: 600; color: var(--ink); }
.sm-link-sub { font-size: 0.6rem; color: var(--ink-dim); letter-spacing: 0.03em; }
@media (max-width: 640px) {
  .sitemap-section { padding: 0 1rem 2.5rem; }
  .sm-link { white-space: normal; }
}
</style>
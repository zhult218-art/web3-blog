<template>
  <section id="home-quick-nav" class="quick-nav">
    <div class="section-container">
      <SectionHead num="[ 03 ]" title="全站快速导航" sub="QUICK NAVIGATION" />

      <nav class="chip-row" aria-label="全站快速导航">
        <router-link
          v-for="item in items"
          :key="item.path"
          :to="item.path"
          class="chip"
          data-glow
        >
          <span class="chip-icon" v-html="item.icon"></span>
          <span class="chip-text">
            <span class="chip-label">{{ item.label }}</span>
            <span class="chip-desc">{{ item.desc }}</span>
          </span>
        </router-link>
      </nav>

      <!-- 次级入口：更细粒度的快捷跳转 -->
      <div class="sub-nav">
        <router-link v-for="s in subs" :key="s.path" :to="s.path" class="sub-chip">{{ s.label }}</router-link>
      </div>
    </div>
  </section>
</template>

<script setup>
// ============================================================
// 首页全站快速导航条（HomeQuickNav）
// 8 个主入口：首页 / 项目 / 博客 / 社区 / 量化 / 商城 / 工具 / 关于
// ============================================================
import SectionHead from './SectionHead.vue'

const items = [
  { label: '首页', path: '/', desc: '星际入口', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12l9-9 9 9"/><path d="M5 10v10h14V10"/></svg>' },
  { label: '项目', path: '/project', desc: '作品集', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>' },
  { label: '博客', path: '/blog', desc: '技术文章', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h12a4 4 0 0 1 4 4v12H8a4 4 0 0 1-4-4V4z"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>' },
  { label: '社区', path: '/community', desc: '交流广场', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>' },
  { label: '量化', path: '/quant', desc: '实盘监控', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 3v18h18"/><path d="M7 14l3-3 3 3 5-6"/></svg>' },
  { label: '商城', path: '/shop', desc: '数字商品', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>' },
  { label: '工具', path: '/tools', desc: '在线工具箱', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.85 2.85-2.7-.45-.45-2.7 2.85-2.85z"/></svg>' },
  { label: '关于', path: '/about', desc: '关于我', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>' },
]

// 次级快捷入口
const subs = [
  { label: '📷 相册', path: '/album' },
  { label: '💅 美甲小铺', path: '/nail' },
  { label: '🎵 音乐', path: '/music' },
  { label: '🎬 视频', path: '/media' },
  { label: '💻 软件中心', path: '/software' },
  { label: '📊 资源库', path: '/resource' },
  { label: '🤖 AI 智能体', path: '/agent' },
  { label: '🌌 3D 星系', path: '/three' },
]
</script>

<style scoped>
.quick-nav {
  position: relative;
  padding: 6rem 2rem;
  background: linear-gradient(180deg,
    transparent 0%, rgba(10, 10, 26, 0.32) 50%, transparent 100%);
}

.section-container { position: relative; max-width: 1200px; margin: 0 auto; }

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.85rem;
  justify-content: center;
  margin-top: 0.5rem;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.7rem 1.2rem;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(12, 12, 28, 0.6);
  backdrop-filter: blur(8px);
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.82rem;
  letter-spacing: 0.05em;
  text-decoration: none;
  font-family: 'Courier New', monospace;
  transition: all 0.3s cubic-bezier(0.23, 1, 0.32, 1);
}

.chip-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.chip-label { font-size: 0.82rem; font-weight: 600; }

.chip-desc {
  font-size: 0.58rem;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 0.08em;
}

.chip-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(103, 232, 249, 0.7);
  transition: color 0.3s;
}

.chip:hover {
  color: #fff;
  border-color: rgba(103, 232, 249, 0.6);
  background: rgba(0, 212, 255, 0.1);
  box-shadow: 0 0 22px rgba(0, 212, 255, 0.28);
  transform: translateY(-2px);
}

.chip:hover .chip-icon { color: #67e8f9; }

/* 次级快捷入口 */
.sub-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
  margin-top: 1.5rem;
}

.sub-chip {
  font-size: 0.72rem;
  padding: 0.35rem 0.9rem;
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  text-decoration: none;
  transition: all 0.3s;
}

.sub-chip:hover {
  color: #c4b5fd;
  border-color: rgba(168, 85, 247, 0.3);
  background: rgba(168, 85, 247, 0.08);
}

@media (max-width: 640px) {
  .quick-nav { padding: 4rem 1.25rem; }
  .chip { padding: 0.55rem 0.9rem; font-size: 0.74rem; }
}
</style>

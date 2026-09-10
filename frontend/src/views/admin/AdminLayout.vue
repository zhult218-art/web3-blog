<template>
  <div class="admin-shell min-h-screen">
    <aside class="admin-side">
      <router-link to="/" class="admin-logo">
        <span class="logo-badge">W</span>
        <div>
          <div class="logo-title">VERSE<span class="logo-accent">NOTE</span></div>
          <div class="logo-sub matrix-text">ADMIN CONTROL</div>
        </div>
      </router-link>
      <nav class="admin-menu">
        <router-link v-for="m in menus" :key="m.path" :to="m.path"
          class="admin-menu-item" :class="{ active: isActive(m.path) }">
          <span class="menu-icon">{{ m.icon }}</span>
          <span class="menu-label">{{ m.label }}</span>
          <span v-if="isActive(m.path)" class="menu-caret"></span>
        </router-link>
      </nav>
      <div class="admin-side-foot matrix-text">SYS // ONLINE</div>
    </aside>
    <div class="admin-main">
      <header class="admin-topbar">
        <div class="flex items-center gap-2 min-w-0">
          <span class="topbar-dot"></span>
          <span class="topbar-crumb text-sm text-gray-400 truncate">{{ currentCrumb }}</span>
        </div>
        <div class="flex items-center gap-3">
          <button @click="reloadPage" title="刷新数据" class="topbar-btn">⟳</button>
          <div class="flex items-center gap-2 px-2 py-1 rounded-lg bg-[#0e0e26] border border-white/[0.05]">
            <span class="w-7 h-7 rounded-full bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-accent)] flex items-center justify-center text-[11px] font-bold text-white">
              {{ (auth.user?.nickname || auth.user?.username || '?')[0]?.toUpperCase() }}
            </span>
            <span class="text-xs text-gray-300 hidden sm:inline">{{ auth.user?.nickname || auth.user?.username }}</span>
          </div>
          <router-link to="/" class="topbar-btn" title="返回站点">⌂</router-link>
          <button @click="doLogout" class="topbar-btn text-red-400 hover:text-red-300" title="退出登录">⎋</button>
        </div>
      </header>
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 后台管理布局：侧边菜单导航 + 面包屑，
// 顶部含刷新与退出登录操作
// ====================================================
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'

const route = useRoute()
const auth = useAuthStore()

// 后台侧边导航菜单配置
const menus = [
  { path: '/admin', label: '仪表盘', icon: '◉' },
  { path: '/admin/users', label: '用户管理', icon: '👤' },
  { path: '/admin/blogs', label: '文章管理', icon: '📝' },
  { path: '/admin/products', label: '商品管理', icon: '🛍️' },
  { path: '/admin/orders', label: '订单管理', icon: '🛒' },
  { path: '/admin/media', label: '媒体管理', icon: '🎬' },
  { path: '/admin/quant', label: '量化管理', icon: '📊' },
  { path: '/admin/traffic', label: '访问统计', icon: '📡' },
  { path: '/admin/blog-links', label: '友链管理', icon: '🤝' },
  { path: '/admin/blog-notice', label: '公告管理', icon: '📢' },
  { path: '/admin/blog-settings', label: '博客配置', icon: '🧩' },
  { path: '/admin/ai-proxy', label: 'API中转站', icon: '🛰️' },
  { path: '/admin/site-share', label: '分享网站管理', icon: '🔖' },
  { path: '/admin/settings', label: '系统设置', icon: '⚙️' },
  { path: '/admin/services', label: '服务管理', icon: '🔧' }
]

// 路径与面包屑名称映射
const crumbMap = {
  '/admin': '仪表盘', '/admin/users': '用户管理', '/admin/blogs': '文章管理',
  '/admin/orders': '订单管理', '/admin/products': '商品管理', '/admin/media': '媒体管理', '/admin/quant': '量化管理',
  '/admin/traffic': '访问统计', '/admin/settings': '系统设置',
  '/admin/blog-links': '友链管理', '/admin/blog-notice': '公告管理', '/admin/blog-settings': '博客配置',
  '/admin/site-share': '分享网站管理', '/admin/services': '服务管理'
}

// 当前页面面包屑名称
const currentCrumb = computed(() => crumbMap[route.path] || '管理后台')

// 判断指定菜单项是否处于激活状态
function isActive(path) {
  if (path === '/admin') return route.path === '/admin'
  return route.path.startsWith(path)
}

// 重新加载当前页面（刷新界面与数据）
function reloadPage() { window.location.reload() }

// 退出登录：通知后端作废会话并清空登录态后回到首页
function doLogout() {
  auth.logout()
  window.location.href = '/'
}
</script>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background:
    radial-gradient(ellipse 70% 50% at 20% 0%, var(--color-glow-soft), transparent 60%),
    radial-gradient(ellipse 50% 60% at 90% 90%, var(--color-accent-soft), transparent 60%),
    #06060e;
}

.admin-side {
  width: 216px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(8, 8, 18, 0.85);
  backdrop-filter: blur(16px);
  z-index: 20;
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 1.1rem 1.1rem 0.9rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  text-decoration: none;
}

.logo-badge {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  color: #fff;
  box-shadow: 0 0 14px var(--color-glow);
}

.logo-title { font-size: 0.95rem; font-weight: 800; color: #fff; line-height: 1.1; }
.logo-accent { color: var(--color-primary); }
.logo-sub { font-size: 0.62rem; color: rgba(255,255,255,0.35); letter-spacing: 0.2em; }

.admin-menu {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 0.6rem;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.admin-menu-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.55rem 0.8rem;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.45);
  text-decoration: none;
  font-size: 0.84rem;
  transition: all 0.25s;
  border: 1px solid transparent;
}

.admin-menu-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.04);
}

.admin-menu-item.active {
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary-soft), var(--color-accent-soft));
  border-color: var(--color-primary-border);
  box-shadow: 0 0 16px var(--color-glow-soft);
}

.menu-icon { font-size: 1rem; width: 20px; text-align: center; }
.menu-caret {
  position: absolute;
  right: 0.55rem;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-accent);
  box-shadow: 0 0 8px var(--color-accent);
}

.admin-side-foot {
  padding: 0.8rem 1.1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 0.62rem;
  color: rgba(255, 255, 255, 0.25);
  letter-spacing: 0.2em;
}

.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-topbar {
  position: sticky;
  top: 0;
  z-index: 15;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.6rem 1.2rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(8, 8, 18, 0.75);
  backdrop-filter: blur(14px);
}

.topbar-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.7);
  animation: blink 2s infinite;
}

@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.35; } }

.topbar-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.03);
  font-size: 0.95rem;
  text-decoration: none;
  transition: all 0.25s;
}

.topbar-btn:hover {
  color: #fff;
  border-color: var(--color-primary-border);
  box-shadow: 0 0 10px var(--color-glow-soft);
}

.admin-content {
  flex: 1;
  padding: 1.25rem;
}

@media (max-width: 860px) {
  .admin-shell { flex-direction: column; }
  .admin-side {
    width: 100%;
    height: auto;
    position: static;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }
  .admin-logo { padding: 0.7rem 1rem; }
  .admin-menu { flex-direction: row; overflow-x: auto; padding: 0.5rem; }
  .admin-menu-item { flex-shrink: 0; }
  .admin-side-foot { display: none; }
  .admin-content { padding: 0.85rem; }
}
</style>
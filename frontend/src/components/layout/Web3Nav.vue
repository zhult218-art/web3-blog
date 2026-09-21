<template>
  <header class="xhdr sticky top-0 z-40">
    <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-3">
      <!-- Logo：极光渐变 "A" + 朱红点（Aurora-朱 品牌标） -->
      <router-link to="/" class="group flex items-center gap-2.5" title="Aurora-朱">
        <svg viewBox="0 0 40 40" class="relative w-9 h-9 drop-shadow-[0_0_10px_rgba(168,85,247,0.45)] transition-transform duration-300 group-hover:scale-110" aria-label="Aurora-朱 logo">
          <defs>
            <linearGradient id="logoAurora" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#22d3ee"/>
              <stop offset="0.52" stop-color="#a855f7"/>
              <stop offset="1" stop-color="#34d399"/>
            </linearGradient>
          </defs>
          <rect x="2" y="2" width="36" height="36" rx="11" fill="#0b0b1e"/>
          <rect x="2.8" y="2.8" width="34.4" height="34.4" rx="10.2" fill="none" stroke="url(#logoAurora)" stroke-width="1.5" opacity="0.75"/>
          <!-- 极光拱形 A -->
          <path d="M11.5 28.5 L20 10.5 L28.5 28.5" fill="none" stroke="url(#logoAurora)" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M15 23 H25" stroke="url(#logoAurora)" stroke-width="2.6" stroke-linecap="round" opacity="0.9"/>
          <!-- 朱红落点 -->
          <circle cx="31.6" cy="9.6" r="3.2" fill="#fb3b5c"/>
          <circle cx="31.6" cy="9.6" r="5.4" fill="#fb3b5c" opacity="0.25"/>
        </svg>
        <span class="text-base font-bold group-hover:opacity-80 transition-all duration-300 hidden sm:inline">
          <span class="bg-gradient-to-r from-cyan-300 via-purple-300 to-emerald-300 bg-clip-text text-transparent">Aurora</span><span class="text-rose-400">-朱</span>
        </span>
      </router-link>

      <!-- Desktop Nav -->
      <nav class="hidden md:flex items-center gap-1">
        <router-link v-for="item in visibleNav" :key="item.path" :to="item.path"
          class="nav-link relative px-3.5 py-2 text-sm rounded-lg group"
          :class="isActive(item.path) ? 'nav-active' : ''"
        >
          <span class="nav-label relative z-10">
            <span class="nl-main">{{ item.label }}</span>
            <span class="nl-sub">{{ item.sub }}</span>
          </span>
          <span class="nav-bar relative z-10"></span>
        </router-link>

        <!-- Services dropdown (量化/工具/软件 + 管理服务) -->
        <div v-if="visibleServiceGroups.length || isAdmin" class="relative group">
          <button class="nav-link relative px-3.5 py-1.5 text-sm rounded-lg group-btn"
            :class="{ 'nav-active': isAnyActive(serviceGroupPaths) }">
            <span class="nav-label relative z-10">
              <span class="nl-main">公会服务</span>
              <span class="nl-sub">功能入口</span>
            </span>
            <svg class="nav-caret w-3 h-3 opacity-60" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
          </button>
          <div class="nav-drop absolute top-full left-0 mt-1 w-60 p-2 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200">
            <template v-for="g in visibleServiceGroups" :key="g.name">
              <p class="drop-head">{{ g.name }}</p>
              <router-link v-for="item in g.items" :key="item.path" :to="item.path"
                class="drop-item flex items-center justify-between gap-2 px-3 py-2 text-sm rounded-lg transition"
              >
                <span class="flex items-center gap-2"><span class="drop-ico">{{ item.icon }}</span>{{ item.label }}</span>
                <small class="di-sub">{{ item.sub }}</small>
              </router-link>
            </template>
            <template v-if="isAdmin">
              <div class="my-1.5 border-t border-white/[0.08]"></div>
              <p class="drop-head">站务管理</p>
              <router-link to="/admin"
                class="drop-item flex items-center justify-between gap-2 px-3 py-2 text-sm rounded-lg transition text-[color:var(--color-primary)]"
              >
                <span class="flex items-center gap-2"><span class="drop-ico">⚙️</span>贤者圣所</span><small class="di-sub">管理服务</small>
              </router-link>
            </template>
          </div>
        </div>

        <div v-if="visibleMoreGroups.length" class="relative group">
          <button class="nav-link relative px-3.5 py-1.5 text-sm rounded-lg group-btn"
            :class="{ 'nav-active': isAnyActive(moreGroupPaths) }">
            <span class="nav-label relative z-10">
              <span class="nl-main">秘藏更多</span>
              <span class="nl-sub">探索发现</span>
            </span>
            <svg class="nav-caret w-3 h-3 opacity-60" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
          </button>
          <div class="nav-drop absolute top-full right-0 mt-1 w-60 p-2 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200">
            <template v-for="g in visibleMoreGroups" :key="g.name">
              <p class="drop-head">{{ g.name }}</p>
              <router-link v-for="item in g.items" :key="item.path" :to="item.path"
                class="drop-item flex items-center justify-between gap-2 px-3 py-2 text-sm rounded-lg transition"
                :class="item.path === '/nails' ? 'text-pink-300 hover:text-white hover:bg-pink-400/[0.08]' : 'text-gray-400 hover:text-white hover:bg-[#16163a]'"
              >
                <span class="flex items-center gap-2"><span class="drop-ico">{{ item.icon }}</span>{{ item.label }}</span>
                <small class="di-sub">{{ item.sub }}</small>
              </router-link>
            </template>
            <div class="my-1.5 border-t border-white/[0.08]"></div>
            <p class="drop-head">视觉体验</p>
            <a href="/experience/index.html" target="_blank" rel="noopener"
              class="drop-item flex items-center justify-between gap-2 px-3 py-2 text-sm text-emerald-300/90 hover:text-white hover:bg-[#16163a] rounded-lg transition"
            ><span class="flex items-center gap-2"><span class="drop-ico">🌿</span>异界之门</span><small class="di-sub">3D 体验</small></a>
          </div>
        </div>
      </nav>

      <!-- Right Side -->
      <div class="flex items-center gap-3">
        <!-- Theme Switcher -->
        <div class="relative">
          <button class="theme-toggle-btn" title="切换主题配色" @click="themeOpen = !themeOpen">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 3a9 9 0 109 9c0-.46-.04-.92-.1-1.36a5.39 5.39 0 01-4.4 2.26 5.4 5.4 0 01-5.4-5.4c0-1.81.89-3.42 2.26-4.4A9 9 0 0012 3z"/></svg>
          </button>
          <transition name="theme-pop">
            <div v-if="themeOpen" class="absolute right-0 top-full mt-2 w-72 glass-panel p-2 z-50"
              @click.stop>
              <p class="text-[10px] text-gray-500 px-2 pt-1 pb-2 uppercase tracking-widest matrix-text">Color Scheme</p>
              <button v-for="t in themes" :key="t.id" @click="applyTheme(t.id)"
                :class="['w-full flex items-center gap-2.5 px-2 py-1.5 rounded-lg text-xs transition',
                  theme === t.id ? 'bg-[#141432] text-white' : 'text-gray-400 hover:text-white hover:bg-[#121230]']">
                <span class="w-4 h-4 rounded-full" :style="{ background: t.swatch, boxShadow: theme === t.id ? '0 0 10px var(--color-glow)' : 'none' }"></span>
                {{ t.label }}
                <span v-if="theme === t.id" class="ml-auto text-cyan-400">✓</span>
              </button>
              <p class="text-[10px] text-gray-500 px-2 pt-3 pb-2 uppercase tracking-widest matrix-text">Wallpaper</p>
              <div class="wallpaper-panel px-1.5 pb-1">
                <div v-for="g in wallGroups" :key="g.id" class="mb-2">
                  <p class="text-[9px] text-white/35 px-0.5 pt-2 pb-1.5 tracking-widest">{{ g.name }}</p>
                  <div class="grid grid-cols-3 gap-1.5">
                    <div v-for="w in groupWallpapers(g.id)" :key="w.id"
                      class="relative aspect-[16/10] rounded-md overflow-hidden cursor-pointer border-2 transition-all group"
                      :class="wallpaper === w.id ? 'border-cyan-400 shadow-[0_0_10px_rgba(34,211,238,0.4)]' : 'border-white/10 hover:border-white/30'"
                      :title="w.name + '（' + w.desc + '）'"
                      @click="applyWallpaper(w.id)">
                      <img v-if="w.file || w.poster" :src="w.file || w.poster" class="w-full h-full object-cover" alt="" />
                      <div v-else class="w-full h-full bg-[#06060e] flex items-center justify-center text-[10px] text-gray-500">无</div>
                      <div class="absolute bottom-0 inset-x-0 bg-black/55 text-center text-[9px] text-white/90 py-0.5 truncate">{{ w.name }}</div>
                      <span v-if="wallpaper === w.id" class="absolute top-0.5 right-1 text-cyan-300 text-[10px]">✓</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </transition>
        </div>

        <!-- Cart indicator -->
        <router-link v-if="hasPerm(auth.user, 'shop')" to="/shop/cart" class="relative text-gray-400 hover:text-white transition-colors">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 100 4 2 2 0 000-4z"/></svg>
          <span v-if="cart.totalCount" class="absolute -top-1.5 -right-2 min-w-4 h-4 px-1 rounded-full bg-pink-500 text-[10px] font-bold flex items-center justify-center">{{ cart.totalCount }}</span>
        </router-link>

        <!-- User -->
        <template v-if="auth.isLoggedIn">
          <div class="relative">
            <button class="user-menu-btn flex items-center gap-2.5 rounded-xl hover:bg-[#121230] px-2 py-1 transition-all" @click="userOpen = !userOpen">
              <div class="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-cyan-500 flex items-center justify-center text-xs font-bold text-white shadow-lg shadow-purple-500/20">
                {{ (auth.user?.nickname || auth.user?.username || '?')[0]?.toUpperCase() }}
              </div>
              <span class="text-sm text-gray-300 hidden sm:inline">{{ auth.user?.nickname || auth.user?.username }}</span>
              <svg class="w-3.5 h-3.5 text-gray-500 hidden sm:inline" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
            </button>
            <!-- Dropdown -->
            <transition name="user-pop">
              <div v-if="userOpen" class="absolute top-full right-0 mt-2 w-52 glass-panel p-1.5 z-50" @click.stop>
                <div class="px-3 py-2.5 border-b border-white/[0.06] mb-1">
                  <p class="text-sm font-medium text-white">{{ auth.user?.nickname || auth.user?.username }}</p>
                  <p class="text-[11px] text-gray-500">@{{ auth.user?.username }}</p>
                </div>
                <router-link to="/profile" class="flex items-center gap-2.5 px-3 py-2 text-sm text-gray-300 hover:text-white hover:bg-[#121230] rounded-lg transition">
                  <span class="text-base">👤</span> 个人中心
                </router-link>
                <router-link to="/profile/orders" class="flex items-center gap-2.5 px-3 py-2 text-sm text-gray-300 hover:text-white hover:bg-[#121230] rounded-lg transition">
                  <span class="text-base">📋</span> 我的订单
                </router-link>
                <router-link v-if="isAdmin" to="/admin" class="flex items-center gap-2.5 px-3 py-2 text-sm text-[color:var(--color-primary)] hover:text-white hover:bg-[#121230] rounded-lg transition">
                  <span class="text-base">⚙️</span> 管理服务
                </router-link>
                <div class="border-t border-white/[0.06] mt-1 pt-1">
                  <button @click="handleLogout" class="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-red-400 hover:text-red-300 hover:bg-red-500/[0.06] rounded-lg transition text-left">
                    <span class="text-base">🚪</span> 退出登录
                  </button>
                </div>
              </div>
            </transition>
          </div>
        </template>
        <router-link v-else to="/login" class="web3-btn text-xs !px-4 !py-1.5">登录</router-link>

        <!-- Mobile toggle -->
        <button class="md:hidden text-gray-400 hover:text-white transition-colors p-1" @click="mobileOpen = !mobileOpen">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="!mobileOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 6h16M4 12h16M4 18h16"/>
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- Mobile Dropdown -->
    <transition name="mobile-nav">
      <div v-if="mobileOpen" class="xhdr-mobile md:hidden">
        <div class="px-4 py-3 space-y-1">
          <p class="drop-head px-1 pt-1 pb-1">主航道</p>
          <router-link v-for="item in visibleNav" :key="item.path" :to="item.path"
            class="flex items-center justify-between px-4 py-2.5 text-sm rounded-xl transition-colors text-gray-400 hover:text-white hover:bg-[#121230]"
            @click="mobileOpen = false"
          >
            <span class="m-main">{{ item.label }}</span>
            <span class="m-sub">{{ item.sub }}</span>
          </router-link>

          <template v-for="g in visibleServiceGroups" :key="'s-' + g.name">
            <p class="drop-head px-1 pt-2 pb-0.5">{{ g.name }}</p>
            <router-link v-for="item in g.items" :key="item.path" :to="item.path"
              class="flex items-center justify-between px-4 py-2.5 text-sm rounded-xl transition-colors"
              :class="item.path === '/nails' ? 'text-pink-300 hover:text-pink-200 hover:bg-pink-400/[0.06]' : 'text-gray-400 hover:text-white hover:bg-[#121230]'"
              @click="mobileOpen = false"
            >
              <span class="m-main"><span>{{ item.icon }}</span> {{ item.label }}<span class="m-sub">{{ item.sub }}</span></span>
            </router-link>
          </template>

          <template v-for="g in visibleMoreGroups" :key="'m-' + g.name">
            <p class="drop-head px-1 pt-2 pb-0.5">{{ g.name }}</p>
            <router-link v-for="item in g.items" :key="item.path" :to="item.path"
              class="flex items-center justify-between px-4 py-2.5 text-sm rounded-xl transition-colors"
              :class="item.path === '/nails' ? 'text-pink-300 hover:text-pink-200 hover:bg-pink-400/[0.06]' : 'text-gray-400 hover:text-white hover:bg-[#121230]'"
              @click="mobileOpen = false"
            >
              <span class="m-main"><span>{{ item.icon }}</span> {{ item.label }}<span class="m-sub">{{ item.sub }}</span></span>
            </router-link>
          </template>

          <p class="drop-head px-1 pt-2 pb-0.5">视觉体验</p>
          <a href="/experience/index.html" target="_blank" rel="noopener" @click="mobileOpen = false"
            class="block px-4 py-2.5 text-sm text-emerald-300/90 hover:text-white hover:bg-[#121230] rounded-xl transition-colors"
          >🌿 异界之门 <span class="m-sub">3D 体验</span></a>
          <router-link v-if="isAdmin" to="/admin" @click="mobileOpen = false"
            class="block px-4 py-2.5 text-sm text-[color:var(--color-primary)] hover:text-white hover:bg-[#121230] rounded-xl transition-colors"
          >⚙️ 贤者圣所 <span class="m-sub">管理服务</span></router-link>
        </div>
      </div>
    </transition>
  </header>
</template>

<script setup>
// ============================================================
// 顶部导航栏（Web3Nav）
// 桌面端导航 + "服务/更多"下拉 + 移动端抽屉展开
// 按用户权限过滤菜单项；含主题配色切换、购物车角标、用户菜单
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useCartStore } from '@/stores/modules/cart'
import { hasPerm } from '@/utils/permissions'
import { useTheme } from '@/composables/useTheme'
import { useServiceHealth, isServiceUp } from '@/composables/useServiceHealth'
import { WALLPAPERS, WALLPAPER_GROUPS } from '@/config/wallpapers'

const auth = useAuthStore()
const cart = useCartStore()
const route = useRoute()
const mobileOpen = ref(false)
const userOpen = ref(false)
useServiceHealth()

// 主题配色方案（写入 body[data-theme]，由全局 CSS 变量驱动）
const themes = [
  { id: 'aurora', label: '曙光白', swatch: 'linear-gradient(135deg,#fdfeff,#cdd8ef)' },
  { id: 'nebula', label: '深空紫', swatch: 'linear-gradient(135deg,#a855f7,#06b6d4)' },
  { id: 'cyber', label: '霓虹青', swatch: 'linear-gradient(135deg,#22d3ee,#8b5cf6)' },
  { id: 'inferno', label: '熔岩橙', swatch: 'linear-gradient(135deg,#fb923c,#ef4444)' },
  { id: 'quantum', label: '量子绿', swatch: 'linear-gradient(135deg,#34d399,#22d3ee)' }
]
const brand = 'Aurora-朱'
const { theme, wallpaper, setTheme, setWallpaper } = useTheme()
const wallpapers = WALLPAPERS
const wallGroups = WALLPAPER_GROUPS
const groupWallpapers = gid => WALLPAPERS.filter(w => w.group === gid)
const themeOpen = ref(false)

// 应用主题：写入 localStorage 与 body 数据集
function applyTheme(id) {
  setTheme(id)
  themeOpen.value = false
}

// 应用壁纸（面板保持展开方便预览比较）
function applyWallpaper(id) {
  setWallpaper(id)
}

// 点击外部区域时收起主题/用户下拉
function onDocClick(e) {
  if (themeOpen.value && !e.target.closest('.theme-toggle-btn')) themeOpen.value = false
  if (userOpen.value && !e.target.closest('.user-menu-btn')) userOpen.value = false
}

onMounted(() => {
  document.body.dataset.theme = theme.value
  document.addEventListener('click', onDocClick)
})
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))
// 主导航（桌面端常驻）—— 异世界冒险风：label 为冒险语术，sub 为直观说明
// svc: 关联的微服务名称，服务关闭时隐藏该项
const nav = [
  { path: '/', label: '主城广场', sub: '首页', perm: 'home' },
  { path: '/blog', label: '冒险日志', sub: '博客', perm: 'community', svc: 'blog-service' },
  { path: '/community', label: '冒险者酒馆', sub: '社区', perm: 'community', svc: 'forum-service' },
  { path: '/shop', label: '魔法杂货铺', sub: '商城', perm: 'shop', svc: 'shop-service' },
  { path: '/media', label: '幻镜水晶', sub: '书影音', perm: 'media', svc: 'media-service' }
]

// 主航道过滤
const visibleNav = computed(() => nav.filter(i => hasPerm(auth.user, i.perm) && (!i.svc || isServiceUp(i.svc))))

// "服务"下拉菜单 —— 分组展示，同类功能聚簇，层次清晰。
// svc: 关联微服务按健康状态显隐；icon: 视觉快速定位；perm: 权限过滤
const serviceGroups = [
  { name: '智能分析', items: [
    { path: '/quant', label: '占星推演', sub: '量化分析', perm: 'quant', svc: 'quant-service', icon: '📊' },
    { path: '/agent', label: 'Agent 会话台', sub: 'Workflow 智能体', perm: 'home', svc: 'ai-proxy-service', icon: '🧠' },
    { path: '/ai-station', label: 'AI 中转站', sub: '令牌·模型', perm: 'home', svc: 'ai-proxy-service', icon: '🤖' },
  ]},
  { name: '工具资源', items: [
    { path: '/knowledge', label: '星图知识库', sub: '持续成长·学习地图', perm: 'home', icon: '🧭' },
    { path: '/tools', label: '炼金道具', sub: '实用工具', perm: 'tools', svc: 'tool-service', icon: '🛠️' },
    { path: '/software', label: '魔导工坊', sub: '软件中心', perm: 'software', svc: 'software-service', icon: '💾' },
    { path: '/resources', label: '宝物仓库', sub: '资源库', perm: 'resources', svc: 'resource-service', icon: '📦' },
    { path: '/upload', label: '星空仓库', sub: '资源上传', perm: 'home', icon: '☁️' },
  ]},
]

// "更多"下拉菜单 —— 分组展示
const moreGroups = [
  { name: '影音相册', items: [
    { path: '/album', label: '回忆水晶', sub: '相册集', perm: 'album', icon: '📸' },
  ]},
  { name: '创作展示', items: [
    { path: '/nails', label: '美甲小铺', sub: '预约·作品集', perm: 'home', svc: 'media-service', icon: '💅' },
    { path: '/three', label: '星空漫游', sub: '3D 粒子空间', perm: 'home', icon: '🌀' },
    { path: '/architecture', label: '架构图鉴', sub: '微服务拓扑', perm: 'home', icon: '🏛️' },
    { path: '/twin', label: '世界之眼', sub: '服务数字孪生', perm: 'home', icon: '🌐' },
  ]},
  { name: '交流关于', items: [
    { path: '/link', label: '同伴名册', sub: '友人帐', perm: 'link', icon: '🤝' },
    { path: '/comments', label: '传音魔石', sub: '留言板', perm: 'comments', icon: '💬' },
    { path: '/about', label: '世界设定集', sub: '关于我们', perm: 'about', icon: '📖' },
  ]},
]

// 按权限 + 服务健康状态过滤分组内项目，隐藏空分组
const groupVisible = (g) => ({
  ...g,
  items: g.items.filter(i => hasPerm(auth.user, i.perm) && (!i.svc || isServiceUp(i.svc)))
})
const visibleServiceGroups = computed(() => serviceGroups.map(groupVisible).filter(g => g.items.length))
const visibleMoreGroups = computed(() => moreGroups.map(groupVisible).filter(g => g.items.length))

// 下拉按钮高亮：当前路由命中组内任何路径（含子路径）
const serviceGroupPaths = computed(() => visibleServiceGroups.value.flatMap(g => g.items.map(i => i.path)))
const moreGroupPaths = computed(() => visibleMoreGroups.value.flatMap(g => g.items.map(i => i.path)))
function isAnyActive(paths) {
  return paths.some(p => isActive(p))
}

// Admin link shown only for logged-in admin users
const isAdmin = computed(() => {
  return auth.isLoggedIn && auth.user?.role === 'ADMIN'
})

// 判断路由是否处于某菜单项（含子路径）
function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

// 退出登录：通知后端作废会话并清空登录态后回首页
function handleLogout() {
  auth.logout()
  // 清除后跳转到首页
  window.location.href = '/'
}
</script>

<style scoped>
/* 壁纸面板：分组滚动，避免过高溢出 */
.wallpaper-panel {
  max-height: 62vh;
  overflow-y: auto;
  scrollbar-width: thin;
  padding-right: 2px;
}

.mobile-nav-enter-active { transition: all 0.25s ease; }
.mobile-nav-leave-active { transition: all 0.2s ease; }
.mobile-nav-enter-from { opacity: 0; max-height: 0; }
.mobile-nav-enter-to { opacity: 1; max-height: 500px; }
.mobile-nav-leave-from { opacity: 1; max-height: 500px; }
.mobile-nav-leave-to { opacity: 0; max-height: 0; }

/* ── 层叠玻璃导航：多层背景 + 高光边 + 主题光晕 ── */
.xhdr {
  position: relative;
}
.xhdr::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  /* 偏透明：整体降不透明度，玻璃更通透，模糊与高光保留 */
  opacity: 0.55;
  background:
    linear-gradient(180deg, var(--nav-hi), rgba(255, 255, 255, 0) 38%),
    linear-gradient(180deg, var(--nav-a), var(--nav-b) 70%, var(--nav-a));
  backdrop-filter: blur(20px) saturate(1.5);
  -webkit-backdrop-filter: blur(20px) saturate(1.5);
  border-bottom: 1px solid var(--nav-line);
  box-shadow:
    0 16px 48px var(--nav-shadow),
    inset 0 1px 0 var(--nav-hi),
    inset 0 -1px 0 rgba(255, 255, 255, 0.04);
}
.xhdr::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  z-index: -1;
  background: linear-gradient(90deg, transparent 4%, var(--color-glow-soft) 25%, var(--color-glow) 50%, var(--color-glow-soft) 75%, transparent 96%);
  opacity: 0.85;
}

/* 导航项：醒目文字 + 按压反馈；hover 呈 3D 玻璃面前倾 */
.nav-link {
  color: var(--nav-fg);
  font-weight: 600;
  letter-spacing: 0.01em;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.28);
  transition: color 0.25s ease, background 0.25s ease,
    transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
    text-shadow 0.25s ease, box-shadow 0.25s ease;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.nav-link:hover {
  color: var(--nav-fg-hi);
  background: var(--nav-pill);
  text-shadow: 0 0 16px var(--color-glow-soft);
  transform: perspective(500px) rotateX(10deg) translateY(-2px) scale(1.04);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.22),
    0 10px 22px -10px rgba(0, 0, 0, 0.55),
    0 0 20px var(--color-glow-soft);
}
/* 按下回弹：交互反馈 */
.nav-link:active {
  transform: translateY(0) scale(0.94);
}

/* 双行标签：冒险风主名 + 直观副标题 */
.nav-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  line-height: 1.05;
}
.nl-main { font-size: 13.5px; }
.nl-sub {
  font-size: 9px;
  letter-spacing: 0.16em;
  opacity: 0.75;
  font-weight: 500;
}
.di-sub { font-size: 10px; opacity: 0.62; flex-shrink: 0; letter-spacing: 0.04em; }
.m-main { display: block; font-weight: 600; color: inherit; }
.m-sub { display: block; font-size: 11px; opacity: 0.68; margin-top: 1px; letter-spacing: 0.08em; }
.m-go { font-size: 11px; opacity: 0.8; }
.nav-caret { transition: transform 0.25s ease; }
.group:hover > .nav-caret { transform: rotate(180deg); }

/* 选中态：高对比 —— 满饱和渐变实底 + 纯白粗体字（背景与文字颜色差异最大化） */
.nav-link.nav-active {
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 6px 20px -6px var(--color-glow),
    0 0 28px var(--color-glow-soft),
    inset 0 1px 0 rgba(255, 255, 255, 0.38);
  transform: translateY(-1px);
  animation: navBreath 2.6s ease-in-out infinite;
}
@keyframes navBreath {
  50% {
    box-shadow:
      0 6px 20px -6px var(--color-glow),
      0 0 46px var(--color-glow),
      inset 0 1px 0 rgba(255, 255, 255, 0.38);
  }
}
/* 选中项按下同样回弹 */
.nav-link.nav-active:active { transform: translateY(0) scale(0.94); animation: none; }
.nav-link.nav-active .nav-label,
.nav-link.nav-active .nl-main {
  color: #fff !important;
  background: none !important;
  -webkit-background-clip: initial !important;
  background-clip: initial !important;
  -webkit-text-fill-color: #fff !important;
  filter: none;
}
.nav-link.nav-active .nl-main,
.nav-link.nav-active .nav-label { font-weight: 800; text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4); }
.nav-link.nav-active .nl-sub { color: rgba(255, 255, 255, 0.9); opacity: 1; }
.nav-bar {
  position: absolute;
  left: 50%;
  bottom: 1px;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
  box-shadow: 0 0 10px var(--color-glow);
  transition: width 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.nav-link:hover .nav-bar { width: 55%; }
.nav-link.nav-active .nav-bar { width: 70%; }

/* 下拉：玻璃浮层 */
.nav-drop {
  background:
    linear-gradient(180deg, var(--nav-a), var(--nav-b));
  backdrop-filter: blur(20px) saturate(1.5);
  -webkit-backdrop-filter: blur(20px) saturate(1.5);
  border: 1px solid var(--nav-line);
  border-radius: 14px;
  box-shadow: 0 20px 44px var(--nav-shadow), inset 0 1px 0 var(--nav-hi);
  z-index: 60;
}
.drop-item:hover { background: var(--nav-hi); color: var(--nav-fg-hi); }
.drop-ico { font-size: 13px; line-height: 1; opacity: 0.95; }
.drop-head {
  padding: 6px 10px 4px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--color-primary);
  opacity: 0.88;
}

/* 移动端抽屉：同款玻璃层次 */
.xhdr-mobile {
  background: linear-gradient(180deg, var(--nav-b), var(--nav-a));
  backdrop-filter: blur(22px) saturate(1.5);
  -webkit-backdrop-filter: blur(22px) saturate(1.5);
  border-bottom: 1px solid var(--nav-line);
  box-shadow: 0 20px 40px var(--nav-shadow), inset 0 1px 0 var(--nav-hi);
}

.theme-pop-enter-active, .theme-pop-leave-active { transition: all 0.2s ease; }
.theme-pop-enter-from, .theme-pop-leave-to { opacity: 0; transform: translateY(-6px) scale(0.96); }

.user-pop-enter-active, .user-pop-leave-active { transition: all 0.2s ease; }
.user-pop-enter-from, .user-pop-leave-to { opacity: 0; transform: translateY(-6px) scale(0.96); }
</style>

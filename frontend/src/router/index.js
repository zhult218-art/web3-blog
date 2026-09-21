// ============================================================
// 路由配置（vue-router 4）
// 修复：所有 Web3Layout 页面合并为单一父路由，避免切换时
// ThreeBackground / ParticleBackground 被反复销毁重建导致白闪
// ============================================================
import { createRouter, createWebHistory } from 'vue-router'
import Web3Layout from '@/layouts/Web3Layout.vue'
import BlogLayout from '@/layouts/BlogLayout.vue'
import LoginView from '@/views/login/index.vue'
import { useAuthStore } from '@/stores/modules/auth'
import { hasPerm } from '@/utils/permissions'

// ── Web3Layout 子路由（共享同一份 ThreeBackground / ParticleBackground / Web3Nav）
const web3Routes = [
  // 首页
  { path: '', name: 'Home', component: () => import('@/views/home/GhibliHome.vue'), meta: { requiresAuth: false, perm: 'home' } },
  // 论坛 / 社区
  { path: 'forum', redirect: '/community' },
  { path: 'forum/:path*', redirect: '/community' },
  { path: 'community', name: 'Community', component: () => import('@/views/community/index.vue'), meta: { requiresAuth: false, perm: 'community' } },
  { path: 'community/chat', name: 'CommunityChat', component: () => import('@/views/community/ChatRoom.vue'), meta: { requiresAuth: false, perm: 'community' } },
  { path: 'community/create', name: 'CommunityCreate', component: () => import('@/views/community/create.vue'), meta: { requiresAuth: true, perm: 'community' } },
  { path: 'community/:type/:id', name: 'CommunityDetail', component: () => import('@/views/community/detail.vue'), meta: { requiresAuth: false, perm: 'community' } },
  // 商城
  { path: 'shop', name: 'Shop', component: () => import('@/views/shop/index.vue'), meta: { requiresAuth: false, perm: 'shop' } },
  { path: 'shop/cart', name: 'Cart', component: () => import('@/views/shop/cart.vue'), meta: { requiresAuth: false, perm: 'shop' } },
  { path: 'shop/:id', name: 'ShopDetail', component: () => import('@/views/shop/detail.vue'), meta: { requiresAuth: false, perm: 'shop' } },
  // AI
  { path: 'ai', name: 'AIPage', component: () => import('@/views/ai/index.vue'), meta: { requiresAuth: false } },
  { path: 'ai-station', name: 'AiStation', component: () => import('@/views/ai-station/index.vue'), meta: { requiresAuth: false } },
  // Agent 聊天（规则路由 + 多专用 Agent + Workflow）
  { path: 'agent', name: 'AgentChat', component: () => import('@/views/agent/index.vue'), meta: { requiresAuth: false } },
  // 媒体 / 书影音
  { path: 'media', name: 'Media', component: () => import('@/views/media/index.vue'), meta: { requiresAuth: false, perm: 'media' } },
  { path: 'media/book/:id', name: 'MediaBook', component: () => import('@/views/media/book.vue'), meta: { requiresAuth: false, perm: 'media' } },
  { path: 'album', name: 'Album', component: () => import('@/views/album/index.vue'), meta: { requiresAuth: false, perm: 'album' } },
  // 音乐（已并入媒体页的音乐馆模块；/music 兼容跳转到媒体）
  { path: 'music', name: 'Music', redirect: '/media' },
  { path: 'music/player/:id', name: 'MusicPlayerPage', component: () => import('@/views/music/player.vue'), meta: { requiresAuth: false, perm: 'music' } },
  // 工具 / 软件 / 资源
  { path: 'tools', name: 'Tools', component: () => import('@/views/tools/index.vue'), meta: { requiresAuth: false, perm: 'tools' } },
  { path: 'tools/api', name: 'ToolsApi', component: () => import('@/views/tools/ApiTools.vue'), meta: { requiresAuth: false, perm: 'tools' } },
  { path: 'tools/api-plaza', name: 'ToolsApiPlaza', component: () => import('@/views/tools/ApiPlaza.vue'), meta: { requiresAuth: false, perm: 'tools' } },
  { path: 'tools/sites', name: 'ToolsSites', component: () => import('@/views/tools/sites.vue'), meta: { requiresAuth: false, perm: 'tools' } },
  { path: 'tools/:tool', name: 'ToolPage', component: () => import('@/views/tools/ToolPage.vue'), meta: { requiresAuth: false, perm: 'tools' } },
  { path: 'software', name: 'Software', component: () => import('@/views/software/index.vue'), meta: { requiresAuth: false, perm: 'software' } },
  { path: 'resources', name: 'Resources', component: () => import('@/views/resources/index.vue'), meta: { requiresAuth: false, perm: 'resources' } },
  { path: 'upload', name: 'Upload', component: () => import('@/views/upload/index.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  // 展示 / 可视化
  { path: 'nails', name: 'Nails', component: () => import('@/views/nails/index.vue'), meta: { requiresAuth: false, perm: 'home' } },
  { path: 'three', name: 'ThreePage', component: () => import('@/views/three/index.vue'), meta: { requiresAuth: false } },
  { path: 'architecture', name: 'ArchPage', component: () => import('@/views/architecture/index.vue'), meta: { requiresAuth: false } },
  { path: 'twin', name: 'DigitalTwin', component: () => import('@/views/twin/index.vue'), meta: { requiresAuth: false, perm: 'home' } },
  { path: 'project', name: 'Project', component: () => import('@/views/project/index.vue'), meta: { requiresAuth: false } },
  // 量化
  { path: 'quant', name: 'Quant', component: () => import('@/views/quant/index.vue'), meta: { requiresAuth: false, perm: 'quant' } },
  { path: 'quant/dashboard', name: 'QuantDashboard', component: () => import('@/views/quant/Dashboard.vue'), meta: { requiresAuth: false, perm: 'quant' } },
  { path: 'stock/:code', name: 'StockDetail', component: () => import('@/views/quant/StockDetail.vue'), meta: { requiresAuth: false, perm: 'quant' } },
  // 用户 / 支付
  { path: 'profile', name: 'Profile', component: () => import('@/views/profile/index.vue'), meta: { requiresAuth: true } },
  { path: 'profile/orders', name: 'ProfileOrders', component: () => import('@/views/profile/orders.vue'), meta: { requiresAuth: true } },
  { path: 'pay', name: 'Pay', component: () => import('@/views/pay/index.vue'), meta: { requiresAuth: true } },
  { path: 'pay/success', name: 'PaySuccess', component: () => import('@/views/pay/success.vue'), meta: { requiresAuth: true } },
  // 其他
  { path: 'link', name: 'Link', component: () => import('@/views/link/index.vue'), meta: { requiresAuth: false, perm: 'link' } },
  { path: 'comments', name: 'Comments', component: () => import('@/views/comments/index.vue'), meta: { requiresAuth: false, perm: 'comments' } },
  { path: 'about', name: 'About', component: () => import('@/views/about/index.vue'), meta: { requiresAuth: false, perm: 'about' } },
  // 个人知识库（Supabase 存储，持续成长的学习地图）
  { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/knowledge/index.vue'), meta: { requiresAuth: false, perm: 'home' } },
  { path: 'knowledge/:id', name: 'KnowledgeDetail', component: () => import('@/views/knowledge/detail.vue'), meta: { requiresAuth: false, perm: 'home' } },
]

const routes = [
  // 统一 Web3Layout（单一实例，切换子路由不销毁背景）
  { path: '/', component: Web3Layout, children: web3Routes },
  // 博客三栏布局
  { path: '/blog', component: BlogLayout, children: [
    { path: '', name: 'BlogHome', component: () => import('@/views/blog/index.vue'), meta: { requiresAuth: false, perm: 'community' } },
    { path: 'categories', name: 'BlogCategories', component: () => import('@/views/blog/categories.vue'), meta: { requiresAuth: false, perm: 'community' } },
    { path: 'tags', name: 'BlogTags', component: () => import('@/views/blog/tags.vue'), meta: { requiresAuth: false, perm: 'community' } },
    { path: 'archives', name: 'BlogArchives', component: () => import('@/views/blog/archives.vue'), meta: { requiresAuth: false, perm: 'community' } },
    { path: 'link', name: 'BlogLink', component: () => import('@/views/blog/link.vue'), meta: { requiresAuth: false, perm: 'community' } },
    { path: 'comments', name: 'BlogComments', component: () => import('@/views/blog/comments.vue'), meta: { requiresAuth: false, perm: 'community' } }
  ]},
  // 博客详情页（独立全宽布局 + anime.js 特效）
  { path: '/blog/post/:id', name: 'BlogDetail', component: () => import('@/views/blog/post.vue'), meta: { requiresAuth: false, perm: 'community' } },
  // 后台管理
  { path: '/admin', name: 'AdminLayout', component: () => import('@/views/admin/AdminLayout.vue'), meta: { requiresAuth: true, requiresAdmin: true }, children: [
    { path: '', name: 'AdminDashboard', component: () => import('@/views/admin/dashboard.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/users.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'blogs', name: 'AdminBlogs', component: () => import('@/views/admin/blogs.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'products', name: 'AdminProducts', component: () => import('@/views/admin/products.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/orders.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'media', name: 'AdminMedia', component: () => import('@/views/admin/media.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'quant', name: 'AdminQuant', component: () => import('@/views/admin/quant.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'traffic', name: 'AdminTraffic', component: () => import('@/views/admin/traffic.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'chat', name: 'AdminChat', component: () => import('@/views/admin/chatApproval.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'settings', name: 'AdminSettings', component: () => import('@/views/admin/settings.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'blog-links', name: 'AdminBlogLinks', component: () => import('@/views/admin/blog-links.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'blog-notice', name: 'AdminBlogNotice', component: () => import('@/views/admin/blog-notice.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'blog-settings', name: 'AdminBlogSettings', component: () => import('@/views/admin/blog-settings.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'ai-proxy', name: 'AdminAiProxy', component: () => import('@/views/admin/ai-proxy.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'site-share', name: 'AdminSiteShare', component: () => import('@/views/admin/site-share.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: 'services', name: 'AdminServices', component: () => import('@/views/admin/services.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  ]},
  // 独立页面（无需 Web3Layout 背景）
  { path: '/login', name: 'Login', component: LoginView, meta: { requiresAuth: false } },
  { path: '/privacy', name: 'Privacy', component: () => import('@/views/privacy/index.vue'), meta: { requiresAuth: false } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash && to.hash.length > 1) {
      try {
        const el = document.querySelector(to.hash)
        if (el) el.scrollIntoView({ behavior: 'smooth' })
      } catch {}
    }
    return { top: 0 }
  }
})

// 全局前置守卫：登录校验 + 页面权限校验 + 管理员角色校验
let sessionProbeDone = false
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  // 会话恢复：首次导航时若内存无令牌，尝试经 HttpOnly Cookie 静默续期
  if (!sessionProbeDone) {
    sessionProbeDone = true
    if (!auth.token) {
      try { await auth.tryRefresh() } catch {}
    }
  }
  // 需要登录的页面：未登录跳转登录页并记录回跳地址
  if (to.meta?.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 页面权限校验：无用户缓存时先拉取用户资料，无权限则回首页
  const permMeta = to.matched.find(r => r.meta.perm)
  if (permMeta) {
    if (!auth.user && auth.isLoggedIn) {
      try { await auth.fetchProfile() } catch {}
    }
    // 首页永远可达：即便权限数据异常也不重定向到自身（防止无限重定向）
    if (permMeta.meta.perm !== 'home' && !hasPerm(auth.user, permMeta.meta.perm)) {
      return { path: '/' }
    }
  }
  // 管理员校验：仅 ADMIN 角色可访问（后端接口另有 requireAdmin 兜底）
  if (to.matched.some(r => r.meta.requiresAdmin)) {
    if (!auth.user && auth.isLoggedIn) {
      await auth.fetchProfile()
    }
    if (auth.user?.role !== 'ADMIN') {
      return { path: '/' }
    }
  }
  // 已登录用户访问登录页时直接回首页
  if (to.path === '/login' && auth.isLoggedIn) {
    return { path: '/' }
  }
  return true
})

export default router

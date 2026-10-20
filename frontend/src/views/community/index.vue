<template>
  <div class="min-h-screen px-4 md:px-6 py-6">
    <div class="mx-auto max-w-7xl">
      <!-- Header：统一 PageHeader 排版语言（面包屑 + 渐变标题 + 右侧操作区） -->
      <PageHeader
        title="社区广场"
        subtitle="TECHNICAL COMMUNITY · 探索 · 创作 · 交流"
        :crumbs="[{ label: '首页', to: '/' }, { label: '社区' }]"
        data-voice-target
      >
        <template #actions>
          <div class="hidden sm:flex items-center gap-1.5 bg-[#0e0e26] rounded-xl px-3 py-2 border border-white/[0.06]">
            <span class="w-2 h-2 rounded-full bg-emerald-400 shadow-[0_0_8px_rgba(16,185,129,0.5)] animate-pulse"></span>
            <span class="text-[11px] text-gray-400">{{ onlineCount }} 在线</span>
          </div>
          <button class="web3-btn text-xs !px-4 !py-2.5 flex items-center gap-1.5" @click="router.push('/community/chat')">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
            群聊
          </button>
          <button class="web3-btn text-xs !px-4 !py-2.5 flex items-center gap-1.5" @click="showPublish = true">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
            发布内容
          </button>
        </template>
      </PageHeader>

      <!-- 板块导航：全部 + 7 个主题共 8 项，固定 4 列铺排保证每行整齐不残缺 -->
      <div class="board-bar">
        <button v-for="b in boardTabs" :key="b.key" @click="switchBoard(b.key)"
          class="board-chip" :class="{ 'board-chip-active': activeBoard === b.key }"
          :style="{ '--accent': b.color }">
          <span class="bc-icon">{{ b.icon }}</span>
          <span class="bc-name">{{ b.name }}</span>
          <span class="bc-count">{{ boardCount(b.key) }}</span>
        </button>
      </div>

      <!-- 当前板块说明：把原大卡片里的描述收到这一行，避免重复占位 -->
      <div class="board-intro">
        <span class="bi-bar" :style="{ background: currentBoard.color }"></span>
        <span class="bi-name">{{ currentBoard.name }}</span>
        <span class="bi-en">{{ currentBoard.en }}</span>
        <span class="bi-desc">{{ currentBoard.desc }}</span>
      </div>

      <!-- 标签筛选提示 -->
      <div v-if="tagFilter" class="mb-4 flex items-center gap-2 text-[11px]">
        <span class="px-2.5 py-1 rounded-lg bg-cyan-500/10 text-cyan-300 border border-cyan-400/20"># {{ tagFilter }}</span>
        <button class="text-gray-500 hover:text-white transition-colors" @click="tagFilter = ''">清除筛选 ×</button>
      </div>

      <!-- Hot Tag Chips -->
      <div v-if="trendingTags.length" class="flex items-center gap-2 mb-6 overflow-x-auto pb-1 scrollbar-hide">
        <span class="text-[10px] text-gray-600 matrix-text flex-shrink-0">热门标签</span>
        <button v-for="t in trendingTags" :key="t.tag" @click="searchTag(t.tag)"
          class="flex-shrink-0 text-[11px] px-2.5 py-1 rounded-lg bg-[#0e0e26] text-gray-400 border border-white/[0.06] hover:border-purple-400/25 hover:text-purple-300 hover:shadow transition-all duration-200">
          #{{ t.tag }}
          <span class="ml-1 text-[9px] text-gray-600">{{ t.cnt }}</span>
        </button>
      </div>
      <div v-else class="mb-6"></div>

      <div class="flex gap-6">
        <!-- Main Feed -->
        <div class="flex-1 min-w-0">
          <!-- Sort bar -->
          <div class="flex items-center gap-2 mb-5">
            <button v-for="s in sorts" :key="s.key" @click="sort = s.key; visibleCount = size"
              :class="['text-[11px] px-3 py-1.5 rounded-lg transition-all duration-200',
                sort === s.key ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-400/20' : 'text-gray-500 hover:text-gray-300 border border-transparent']">
              {{ s.label }}
            </button>
            <div class="ml-auto text-[10px] text-gray-600 matrix-text">
              {{ feed.length > 0 ? `共 ${total} 条` : '' }}
            </div>
          </div>

          <!-- Content Cards -->
          <div v-if="feed.length" class="space-y-4">
            <div v-for="item in feed" :key="item.id" v-tilt
              class="group glass-panel overflow-hidden cursor-pointer hover:border-purple-400/25 transition-all duration-400"
              @click="goDetail(item)">
              <!-- Rich media area -->
              <div v-if="item.coverImage || item.mediaUrl" class="relative overflow-hidden">
                <img v-if="item.coverImage" :src="item.coverImage" class="w-full h-48 md:h-56 object-cover group-hover:scale-[1.03] transition-transform duration-700" loading="lazy" />
                <video v-else-if="item.mediaUrl && item.mediaType === 'video'" :src="item.mediaUrl" class="w-full h-48 md:h-56 object-cover" muted />
                <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent pointer-events-none"></div>
                <div class="absolute bottom-3 left-4 flex items-center gap-2">
                  <span :class="['text-[10px] px-2 py-0.5 rounded-lg font-medium',
                    item.type === 'video'
                      ? 'bg-pink-500/20 text-pink-300 border border-pink-400/20'
                      : 'bg-cyan-500/20 text-cyan-300 border border-cyan-400/20']">
                    {{ item.type === 'video' ? '视频' : '讨论' }}
                  </span>
                </div>
              </div>

              <!-- Content body -->
              <div class="p-5 md:p-6">
                <div class="flex items-center gap-2 mb-3">
                  <div class="w-7 h-7 rounded-full bg-gradient-to-br from-purple-500 to-cyan-500 flex items-center justify-center text-[10px] font-bold text-white">
                    {{ (item.authorName || 'U')[0].toUpperCase() }}
                  </div>
                  <span class="text-xs text-gray-400">{{ item.authorName || '匿名用户' }}</span>
                  <span class="text-[10px] text-gray-600">·</span>
                  <span class="text-[10px] text-gray-600">{{ formatDate(item.createdAt) }}</span>
                  <span class="text-[10px] text-gray-600">·</span>
                  <span class="text-[10px] text-gray-600">{{ item.category || '综合' }}</span>
                  <span v-if="item.isSeed" class="text-[10px] px-1.5 py-0.5 rounded bg-white/[0.06] text-gray-400 border border-white/[0.08]">示例</span>
                </div>

                <h3 class="text-base md:text-lg font-bold text-white group-hover:text-cyan-300 transition-colors duration-300 leading-snug mb-2">
                  {{ item.title }}
                </h3>

                <p class="text-sm text-gray-400 leading-relaxed line-clamp-3 mb-4">{{ item.summary || item.content }}</p>

                <!-- Tags -->
                <div v-if="item.tags" class="flex flex-wrap gap-1.5 mb-4">
                  <span v-for="tag in item.tags.split(',')" :key="tag" class="text-[10px] px-2 py-0.5 rounded-md bg-[#10102a] text-gray-500 border border-white/[0.06]">
                    {{ tag.trim() }}
                  </span>
                </div>

                <!-- Interaction bar -->
                <div class="flex items-center gap-5 pt-3 border-t border-white/[0.04]">
                  <button class="flex items-center gap-1.5 text-xs text-gray-500 hover:text-pink-400 transition-colors" @click.stop="like(item)">
                    <svg class="w-4 h-4" :class="item.liked ? 'text-pink-400 fill-pink-400/20' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/></svg>
                    <span>{{ item.likeCount || 0 }}</span>
                  </button>
                  <span class="flex items-center gap-1.5 text-xs text-gray-500">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
                    {{ item.replyCount || 0 }}
                  </span>
                  <span class="flex items-center gap-1.5 text-xs text-gray-500">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>
                    {{ item.viewCount || 0 }}
                  </span>
                  <button class="ml-auto flex items-center gap-1 text-xs text-gray-600 hover:text-cyan-400 transition-colors" @click.stop="bookmark(item)">
                    <svg class="w-4 h-4" :class="item.bookmarked ? 'text-cyan-400' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/></svg>
                    {{ item.bookmarked ? '已收藏' : '收藏' }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Loading skeleton -->
          <div v-else-if="loading" class="space-y-4">
            <div v-for="i in 4" :key="i" class="glass-panel p-5">
              <div class="flex items-center gap-2 mb-3">
                <div class="skeleton w-7 h-7 rounded-full"></div>
                <div class="skeleton h-3 w-24"></div>
                <div class="skeleton h-2 w-12 ml-auto"></div>
              </div>
              <div class="skeleton h-5 w-3/4 mb-3"></div>
              <div class="skeleton h-3 w-full mb-2"></div>
              <div class="skeleton h-3 w-2/3 mb-4"></div>
              <div class="flex gap-3 pt-3 border-t border-white/[0.04]">
                <div class="skeleton h-3 w-10"></div>
                <div class="skeleton h-3 w-10"></div>
                <div class="skeleton h-3 w-10"></div>
              </div>
            </div>
          </div>
          <!-- Empty state：统一 EmptyState 组件 -->
          <EmptyState
            v-else
            text="这里还很安静，成为第一个分享内容的人"
            d="M9 13h6m-3-3v6m-9 0V5a2 2 0 012-2h10a2 2 0 012 2v14l-3-1.5L12 21l-3-1.5L6 21V5z"
          >
            <template #action>
              <button @click="showPublish = true" class="web3-btn text-xs !px-5 !py-2.5 inline-flex items-center gap-1.5">
                去发布
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"/></svg>
              </button>
            </template>
          </EmptyState>

          <!-- Pagination -->
          <div v-if="feed.length < total" class="mt-8 flex justify-center">
            <button @click="loadMore" :disabled="loadingMore" class="web3-btn-outline text-xs !px-6 !py-2.5 flex items-center gap-2">
              <span v-if="loadingMore" class="inline-block h-3 w-3 rounded-full border-2 border-white/20 border-t-white animate-spin"></span>
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </button>
          </div>
        </div>

        <!-- Right Sidebar -->
        <aside class="hidden xl:block w-72 space-y-5 flex-shrink-0">
          <!-- Trending -->
          <div class="glass-panel p-5">
            <h3 class="text-xs font-bold text-gray-300 mb-4 flex items-center gap-2">
              <span class="text-amber-400">🔥</span> 热门话题
            </h3>
            <div class="space-y-2.5">
              <div v-for="(t, i) in trendingTags" :key="t.tag" @click="searchTag(t.tag)"
                class="flex items-center gap-3 py-2 px-3 rounded-lg bg-[#0c0c22] hover:bg-[#121230] cursor-pointer transition-all group">
                <span :class="['text-xs font-bold w-5 text-center', i < 3 ? 'text-amber-400' : 'text-gray-600']">{{ i + 1 }}</span>
                <span class="text-xs text-gray-400 group-hover:text-white transition-colors flex-1">{{ t.tag }}</span>
                <span class="text-[10px] text-gray-600">{{ t.cnt }}</span>
              </div>
            </div>
          </div>

          <!-- Active Authors -->
          <div class="glass-panel p-5">
            <h3 class="text-xs font-bold text-gray-300 mb-4 flex items-center gap-2">
              <span class="text-purple-400">⭐</span> 活跃作者
            </h3>
            <div class="space-y-3">
              <div v-for="author in activeAuthors" :key="author.id" class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500/30 to-cyan-500/30 flex items-center justify-center text-xs font-bold text-white/80">
                  {{ author.name[0] }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-xs text-white truncate">{{ author.name }}</p>
                  <p class="text-[10px] text-gray-600">{{ author.posts }} 篇文章</p>
                </div>
                <button class="text-[10px] px-2 py-1 rounded-md border border-purple-400/20 text-purple-400 hover:bg-purple-500/10 transition">关注</button>
              </div>
            </div>
          </div>

          <!-- Quick Stats -->
          <div class="glass-panel p-5">
            <h3 class="text-xs font-bold text-gray-300 mb-4">📊 社区统计</h3>
            <div class="grid grid-cols-2 gap-3">
              <div class="bg-[#0c0c22] rounded-xl p-3 text-center">
                <div class="text-lg font-black text-cyan-400 matrix-text">{{ totalPosts }}</div>
                <div class="text-[10px] text-gray-500 mt-0.5">总帖子</div>
              </div>
              <div class="bg-[#0c0c22] rounded-xl p-3 text-center">
                <div class="text-lg font-black text-purple-400 matrix-text">{{ totalComments }}</div>
                <div class="text-[10px] text-gray-500 mt-0.5">总评论</div>
              </div>
              <div class="bg-[#0c0c22] rounded-xl p-3 text-center">
                <div class="text-lg font-black text-pink-400 matrix-text">{{ todayPosts }}</div>
                <div class="text-[10px] text-gray-500 mt-0.5">今日新增</div>
              </div>
              <div class="bg-[#0c0c22] rounded-xl p-3 text-center">
                <div class="text-lg font-black text-emerald-400 matrix-text">{{ onlineCount }}</div>
                <div class="text-[10px] text-gray-500 mt-0.5">在线用户</div>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>

    <!-- 发布弹窗：覆盖在本页上层，不再跳转独立页面 -->
    <PublishModal v-model="showPublish" @published="fetch" />
  </div>
</template>

<script setup>
// ====================================================
// 社区广场页：按主题板块（技术/抽象/工作/感情/金融/吃瓜/搞笑）
// 聚合讨论内容，支持板块切换、标签筛选、排序、点赞、收藏与分页加载
// ====================================================
import { ref, computed, onMounted, onActivated, onDeactivated, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getForumList, likePost, getPostStats } from '@/api/forum'
import { getOnlineCount } from '@/api/admin'
import { useToastStore } from '@/stores/modules/toast'
import { boards, seedPosts } from '@/data/communityBoards'
import PublishModal from '@/components/community/PublishModal.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { formatRelative as formatDate } from '@/utils/date.js'

// 显式命名以供外层 <keep-alive :include="..."> 缓存本组件：
// 进入社区页时复用已加载的列表/stats，不再每次重挂重发请求，显著加快"每次进去慢"。
defineOptions({ name: 'Community' })

const router = useRouter()
const toast = useToastStore()

const showPublish = ref(false)

const loading = ref(false)
const loadingMore = ref(false)
const backendList = ref([])    // 服务端真实帖子
const backendTotal = ref(0)    // 服务端帖子总数（用于判断是否还有下一页）
const page = ref(1)
const size = ref(10)
const sort = ref('latest')
const activeBoard = ref('all')
const tagFilter = ref('')
const visibleCount = ref(10)
const onlineCount = ref(0)
const totalPosts = ref(0)
const totalComments = ref(0)
const todayPosts = ref(0)

const sorts = [
  { key: 'latest', label: '最新发布' },
  { key: 'hot', label: '最热讨论' },
  { key: 'top', label: '置顶优先' },
]

const trendingTags = ref([])
const activeAuthors = ref([])
const savedBookmarkIds = new Set()

// ── 板块聚合 ────────────────────────────────────────────────
// 某板块的预置话题（all 表示全部）
function seedsOfBoard(key) {
  return key === 'all' ? seedPosts : seedPosts.filter(p => p.board === key)
}

// 判断一条真实帖子属于哪个板块：分类或标签里命中板块名/键名即算
function postInBoard(p, key) {
  if (key === 'all') return true
  const b = boards.find(x => x.key === key)
  if (!b) return true
  const hay = `${p.category || ''},${p.tags || ''}`
  return hay.includes(b.name) || hay.includes(b.key)
}

// 完整候选列表：预置话题 + 真实帖子，按板块/标签/排序合并
const allFeed = computed(() => {
  const seeds = seedsOfBoard(activeBoard.value)
  const posts = backendList.value.filter(p => postInBoard(p, activeBoard.value))
  let merged = activeBoard.value === 'all' ? [...posts, ...seeds] : [...seeds, ...posts]
  if (tagFilter.value) merged = merged.filter(p => (p.tags || '').includes(tagFilter.value))
  if (sort.value === 'hot') merged = [...merged].sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0))
  return merged
})

// 当前展示的帖子（客户端分页切片）
const feed = computed(() => allFeed.value.slice(0, visibleCount.value))
const total = computed(() => allFeed.value.length)

// 板块角标：预置话题数 + 命中的真实帖子数
function boardCount(key) {
  return seedsOfBoard(key).length + backendList.value.filter(p => postInBoard(p, key)).length
}

// 导航项：全部 + 7 个主题板块（共 8 项，固定 4 列正好两行）
const boardTabs = computed(() => [
  { key: 'all', name: '全部', en: 'ALL', icon: '✦', color: '#8b9dff', desc: '社区里的全部讨论' },
  ...boards,
])

// 当前选中板块（用于顶部说明行）
const currentBoard = computed(() =>
  boardTabs.value.find(b => b.key === activeBoard.value) || boardTabs.value[0]
)

// 格式化时间：1 分钟内显示「刚刚」，1 小时内显示「x 分钟前」，今天显示「HH:mm」，其余显示短日期

// 拉取服务端真实帖子（web3_forum），社区与博客分库，互不混用。
// 结果统一放入 backendList，与预置话题在 allFeed 中合并后再客户端分页
async function fetch({ append = false } = {}) {
  if (append) loadingMore.value = true
  else loading.value = true
  try {
    const res = await getForumList({ page: page.value, size: size.value }).catch(() => ({ data: { records: [] } }))
    const data = res?.data || {}
    const records = (data.records || data || [])
      .map(r => ({ ...r, type: r.mediaType === 'video' ? 'video' : 'discussion' }))

    if (append) {
      const seen = new Set(backendList.value.map(i => i.id))
      backendList.value = [...backendList.value, ...records.filter(i => !seen.has(i.id))]
    } else {
      backendList.value = records
    }
    backendTotal.value = Number(data.total) || backendList.value.length
  } catch (e) {
    if (import.meta.env.DEV) console.warn('fetch fail', e)
  } finally {
    loading.value = false
    loadingMore.value = false
    // 同步本地收藏状态（真实帖子 + 预置话题）
    for (const item of [...backendList.value, ...seedPosts]) {
      item.bookmarked = savedBookmarkIds.has(item.id)
    }
  }
}

// 加载社区统计数据：在线人数、帖子总数、评论总数、今日发帖
async function loadStats() {
  try {
    const res = await getPostStats()
    const stats = res.data || {}
    totalPosts.value = stats.totalPosts || 0
    totalComments.value = stats.totalComments || 0
    todayPosts.value = stats.todayPosts || 0
    trendingTags.value = (stats.topTags || []).map(t => ({ tag: t.tag, cnt: t.cnt }))
    activeAuthors.value = (stats.topAuthors || []).map(a => ({ id: a.name, name: a.name, posts: a.cnt }))
  } catch {}
}

// 切换板块：重置标签筛选与分页，回到该板块第一屏
function switchBoard(key) {
  activeBoard.value = key
  tagFilter.value = ''
  visibleCount.value = size.value
}

// 点击条目跳转：预置话题为示例内容，不跳详情；真实帖子进详情页
function goDetail(item) {
  if (item.isSeed) {
    toast.info('这是预置的示例话题，欢迎发布你的第一条真实内容')
    return
  }
  router.push(`/community/post/${item.id}`)
}

// 点赞/取消点赞：预置话题仅本地计数，真实帖子走 likePost（web3_forum）
async function like(item) {
  if (item.isSeed) {
    item.liked = !item.liked
    item.likeCount = Math.max(0, (item.likeCount || 0) + (item.liked ? 1 : -1))
    return
  }
  try {
    await likePost(item.id)
    item.likeCount = (item.likeCount || 0) + (item.liked ? -1 : 1)
    item.liked = !item.liked
    toast.success(item.liked ? '点赞成功' : '已取消点赞')
  } catch { toast.error('请先登录') }
}

// 收藏/取消收藏：保存在本地 localStorage
function bookmark(item) {
  item.bookmarked = !item.bookmarked
  if (item.bookmarked) {
    savedBookmarkIds.add(item.id)
  } else {
    savedBookmarkIds.delete(item.id)
  }
  try { localStorage.setItem('bookmarkedPostIds', JSON.stringify([...savedBookmarkIds])) } catch {}
  toast.success(item.bookmarked ? '已加入收藏' : '已取消收藏')
}

// 点击热门标签：按标签筛选当前板块内容（再次点击取消）
function searchTag(tag) {
  tagFilter.value = tagFilter.value === tag ? '' : tag
  visibleCount.value = size.value
}

// 加载更多：先放开客户端展示条数；服务端仍有下一页时继续追加真实帖子
async function loadMore() {
  if (loadingMore.value) return
  visibleCount.value += size.value
  if (backendList.value.length < backendTotal.value) {
    page.value++
    await fetch({ append: true })
  }
}

// 加载真实在线人数（admin-service 实时统计，近 10 分钟去重 IP）
async function loadOnlineCount() {
  try {
    const res = await getOnlineCount()
    const count = Number(res?.data?.onlineCount)
    if (Number.isFinite(count)) onlineCount.value = count
  } catch {}
}

// 在线人数轮询定时器：组件被 <keep-alive> 缓存时，切走只触发 onDeactivated 而非 onUnmounted，
// 必须在 onDeactivated 里停掉，否则后台会持续每 30s 请求在线人数接口（定时器泄漏）。
let onlineTimer = null

function startOnlineTimer() {
  if (onlineTimer) clearInterval(onlineTimer)
  loadOnlineCount()
  onlineTimer = setInterval(loadOnlineCount, 30000)
}
function stopOnlineTimer() {
  if (onlineTimer) { clearInterval(onlineTimer); onlineTimer = null }
}

onMounted(() => {
  // Load persisted bookmark IDs
  try {
    const saved = JSON.parse(localStorage.getItem('bookmarkedPostIds') || '[]')
    if (Array.isArray(saved)) saved.forEach(id => savedBookmarkIds.add(id))
  } catch {}
  fetch()
  loadStats()
  // 首次挂载也启动一次；onActivated 在首次挂载后同样会触发，startOnlineTimer 内部先清后建，幂等
  startOnlineTimer()
})

// keep-alive 重新激活：重启在线人数轮询（列表/stats 由缓存保留，不重复请求）
onActivated(() => {
  startOnlineTimer()
})

// keep-alive 切走：停掉轮询，避免后台泄漏
onDeactivated(() => {
  stopOnlineTimer()
})

onUnmounted(() => {
  stopOnlineTimer()
})
</script>

<style scoped>
/* ── 板块导航条：8 项固定 4 列，两行整齐；窄屏降为 2 列 ── */
.board-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.board-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border-radius: 11px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  /* 透明度从 0.7/0.58 提到 0.94/0.9：壁纸上的浅色发丝会直接透过来，
     板块名读不清。加一层 backdrop-blur 保留「玻璃」质感，同时保证字压得住 */
  background: linear-gradient(150deg, rgba(18, 18, 40, 0.94), rgba(10, 10, 24, 0.9));
  backdrop-filter: blur(10px);
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.74rem;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), border-color 0.25s, color 0.25s, box-shadow 0.25s;
}
.board-chip:hover {
  transform: translateY(-2px);
  color: #fff;
  border-color: color-mix(in srgb, var(--accent) 45%, transparent);
}
.board-chip-active {
  color: #fff;
  border-color: color-mix(in srgb, var(--accent) 58%, transparent);
  background: linear-gradient(150deg, color-mix(in srgb, var(--accent) 22%, rgba(16, 16, 38, 0.95)), rgba(10, 10, 24, 0.92));
  box-shadow: 0 0 16px color-mix(in srgb, var(--accent) 24%, transparent);
}

.bc-icon { flex-shrink: 0; font-size: 0.92rem; line-height: 1; }
.bc-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bc-count {
  flex-shrink: 0;
  font-size: 0.58rem;
  font-family: 'Courier New', monospace;
  color: color-mix(in srgb, var(--accent) 88%, #fff);
  padding: 1px 6px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--accent) 30%, transparent);
  background: color-mix(in srgb, var(--accent) 12%, transparent);
}

/* ── 当前板块说明行 ── */
.board-intro {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 18px;
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.42);
}
.bi-bar { flex-shrink: 0; width: 3px; height: 13px; border-radius: 2px; }
.bi-name { font-weight: 700; color: rgba(255, 255, 255, 0.86); }
.bi-en {
  font-size: 0.54rem;
  letter-spacing: 0.22em;
  color: rgba(255, 255, 255, 0.4);
  font-family: 'Courier New', monospace;
}
.bi-desc {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .board-bar { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .bi-en { display: none; }
}

.scrollbar-hide::-webkit-scrollbar { display: none; }
.scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

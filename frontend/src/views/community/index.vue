<template>
  <div class="min-h-screen px-4 md:px-6 py-6">
    <div class="mx-auto max-w-7xl">
      <!-- Header -->
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-3xl md:text-4xl font-black text-gradient-cyber">社区广场</h1>
          <p class="text-xs text-gray-500 matrix-text mt-1.5 tracking-wider">TECHNICAL COMMUNITY · 探索 · 创作 · 交流</p>
        </div>
        <div class="flex items-center gap-3">
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
        </div>
      </div>

      <!-- Filter Tabs -->
      <div class="flex items-center gap-2 mb-4 overflow-x-auto pb-2 scrollbar-hide">
        <button v-for="tab in tabs" :key="tab.key" @click="switchTab(tab.key)"
          :class="['px-4 py-2 rounded-xl text-xs font-medium whitespace-nowrap transition-all duration-300 border',
            activeTab === tab.key
              ? 'bg-gradient-to-r from-purple-500/20 to-cyan-500/10 border-purple-400/30 text-white shadow-[0_0_20px_rgba(168,85,247,0.15)]'
              : 'border-white/[0.06] text-gray-500 hover:text-gray-300 hover:bg-[#0e0e26]']">
          {{ tab.label }}
          <span v-if="tab.count" class="ml-1.5 text-[10px] opacity-60">{{ tab.count }}</span>
        </button>
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
            <button v-for="s in sorts" :key="s.key" @click="sort = s.key; fetch()"
              :class="['text-[11px] px-3 py-1.5 rounded-lg transition-all duration-200',
                sort === s.key ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-400/20' : 'text-gray-500 hover:text-gray-300 border border-transparent']">
              {{ s.label }}
            </button>
            <div class="ml-auto text-[10px] text-gray-600 matrix-text">
              {{ list.length > 0 ? `共 ${total} 条` : '' }}
            </div>
          </div>

          <!-- Content Cards -->
          <div v-if="list.length" class="space-y-4">
            <div v-for="item in list" :key="item.id"
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
          <!-- Empty state -->
          <div v-else class="py-20 text-center">
            <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-[#0e0e26] border border-white/[0.06] mb-5">
              <svg class="w-8 h-8 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 13h6m-3-3v6m-9 0V5a2 2 0 012-2h10a2 2 0 012 2v14l-3-1.5L12 21l-3-1.5L6 21V5z"/>
              </svg>
            </div>
            <p class="text-sm text-gray-400 mb-1">这里还很安静</p>
            <p class="text-xs text-gray-600 mb-5">成为第一个分享内容的人</p>
            <button @click="showPublish = true" class="web3-btn text-xs !px-5 !py-2.5 inline-flex items-center gap-1.5">
              去发布
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"/></svg>
            </button>
          </div>

          <!-- Pagination -->
          <div v-if="total > page * size" class="mt-8 flex justify-center">
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
// 社区广场页：聚合博客文章、论坛讨论与视频内容，
// 支持 tab/排序切换、点赞、收藏、标签搜索与分页加载
// ====================================================
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getForumList, likePost, getPostStats } from '@/api/forum'
import { getOnlineCount } from '@/api/admin'
import { useToastStore } from '@/stores/modules/toast'
import PublishModal from '@/components/community/PublishModal.vue'

const router = useRouter()
const toast = useToastStore()

const showPublish = ref(false)

const loading = ref(false)
const loadingMore = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const sort = ref('latest')
const activeTab = ref('all')
const onlineCount = ref(0)
const totalPosts = ref(0)
const totalComments = ref(0)
const todayPosts = ref(0)

const tabs = [
  { key: 'all', label: '全部', count: null },
  { key: 'discussion', label: '讨论', count: null },
  { key: 'video', label: '视频', count: null },
  { key: 'hot', label: '热门', count: null },
]

const sorts = [
  { key: 'latest', label: '最新发布' },
  { key: 'hot', label: '最热讨论' },
  { key: 'top', label: '置顶优先' },
]

const trendingTags = ref([])
const activeAuthors = ref([])

// 格式化时间：1 分钟内显示「刚刚」，1 小时内显示「x 分钟前」，今天显示「HH:mm」，其余显示短日期
function formatDate(d) {
  if (!d) return ''
  const date = new Date(d)
  const now = new Date()
  const diff = (now - date) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 按当前 tab/排序请求论坛帖子（web3_forum），社区与博客分库，互不混用
async function fetch() {
  loading.value = true
  try {
    const res = await getForumList({ page: page.value, size: size.value * 3 }).catch(() => ({ data: { records: [] } }))
    const records = (res.data?.records || res.data || [])
    const combined = []
    records.forEach(r => {
      if (activeTab.value === 'discussion' && r.mediaType === 'video') return
      if (activeTab.value === 'video' && r.mediaType !== 'video') return
      combined.push({ ...r, type: r.mediaType === 'video' ? 'video' : 'discussion' })
    })

    // Sort
    if (sort.value === 'hot') {
      combined.sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0))
    }
    list.value = combined.slice(0, size.value)
    total.value = combined.length * 3
  } catch (e) {
    console.warn('fetch fail', e)
  } finally {
    loading.value = false
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

// 切换顶部 tab（全部/文章/讨论/视频/热门）并重新加载列表
function switchTab(key) {
  activeTab.value = key
  page.value = 1
  fetch()
}

// 点击条目跳转到帖子详情页（社区只读 web3_forum）
function goDetail(item) {
  router.push(`/community/post/${item.id}`)
}

// 点赞/取消点赞：社区帖子走 likePost（web3_forum）
async function like(item) {
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
  toast.success(item.bookmarked ? '已加入收藏' : '已取消收藏')
}

// 点击热门标签跳转到搜索页
function searchTag(tag) {
  toast.info(`搜索标签: ${tag}`)
}

// 滚动到底部加载下一页数据
function loadMore() {
  loadingMore.value = true
  page.value++
  setTimeout(() => {
    fetch()
    loadingMore.value = false
  }, 800)
}

// 加载真实在线人数（admin-service 实时统计，近 10 分钟去重 IP）
async function loadOnlineCount() {
  try {
    const res = await getOnlineCount()
    const count = Number(res?.data?.onlineCount)
    if (Number.isFinite(count)) onlineCount.value = count
  } catch {}
}

onMounted(() => {
  fetch()
  loadStats()
  loadOnlineCount()
  setInterval(loadOnlineCount, 30000)
})
</script>

<style scoped>
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

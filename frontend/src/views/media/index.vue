<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl space-y-8">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <h1 class="text-3xl font-bold text-gradient-cyber">在线媒体</h1>
        <!-- 顶部标签页：音乐馆 / 图书 / 视频 -->
        <div class="flex gap-1 p-1 rounded-xl glass-panel-sm">
          <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
            class="px-4 py-1.5 rounded-lg text-sm font-medium transition"
            :class="activeTab === tab.key ? 'bg-purple-500/25 text-white border border-purple-400/30' : 'text-gray-500 hover:text-white border border-transparent'">
            <span class="mr-1">{{ tab.icon }}</span>{{ tab.label }}
          </button>
        </div>
      </div>

      <!-- 音乐馆模块 -->
      <template v-if="activeTab === 'music'">
        <MusicHall />
      </template>

      <!-- 图书模块：全屏书桌场景（物品 → 书架弹窗 → 3D 阅读器） -->
      <template v-else-if="activeTab === 'book'">
        <BookDesk />
      </template>

      <!-- 视频模块 -->
      <template v-else>
        <section class="glass-panel p-6">
          <div class="flex flex-wrap items-center justify-between gap-3 mb-5">
            <h2 class="text-xl font-bold text-white flex items-center gap-2"><span>🎬</span>视频</h2>
            <div class="flex gap-1 p-1 rounded-xl glass-panel-sm">
              <button v-for="tab in videoTabs" :key="tab.key" @click="videoTab = tab.key"
                class="px-3 py-1.5 rounded-lg text-sm font-medium transition"
                :class="videoTab === tab.key ? 'bg-cyan-500/25 text-white border border-cyan-400/30' : 'text-gray-500 hover:text-white border border-transparent'">
                <span class="mr-1">{{ tab.icon }}</span>{{ tab.label }}
              </button>
            </div>
          </div>

          <!-- 在线视频（后端上传） -->
          <template v-if="videoTab === 'online'">
            <div v-if="videoList.length" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="item in videoList" :key="item.id" class="glass-panel-sm overflow-hidden group cursor-pointer">
                <div class="relative">
                  <EnhancedVideoPlayer :src="item.url" :poster="item.cover" :video-id="item.id" @play="onVideoPlay" />
                </div>
                <div class="p-4">
                  <h4 class="font-semibold text-white text-sm group-hover:text-cyan-300 transition">{{ item.title }}</h4>
                  <p class="text-xs text-gray-500 mt-1.5 line-clamp-2">{{ item.description }}</p>
                  <div class="flex flex-wrap gap-3 mt-2 text-[10px] text-gray-600">
                    <span v-if="item.duration">{{ formatDuration(item.duration) }}</span>
                    <span v-if="item.tags" class="flex flex-wrap gap-1">
                      <span v-for="tag in String(item.tags).split(',').map(s => s.trim()).filter(Boolean)" :key="tag"
                        class="px-1.5 py-0.5 rounded-full border border-cyan-400/20 text-cyan-300/80">{{ tag }}</span>
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="py-16 text-center">
              <div class="text-4xl mb-3 opacity-15">🎬</div>
              <p class="text-xs text-gray-600">暂无在线视频，去「下载」标签粘贴 URL 下载吧</p>
            </div>
          </template>

          <!-- URL 下载（HomeTube 风格：粘贴 URL → yt-dlp 下载 → 自动入库） -->
          <template v-else-if="videoTab === 'download'">
            <div class="glass-panel-sm p-5 max-w-2xl mx-auto">
              <h3 class="text-base font-bold text-cyan-300 mb-1 flex items-center gap-2">⬇ 视频下载器</h3>
              <p class="text-[11px] text-gray-500 mb-4">粘贴 YouTube / B站 / 抖音等平台 URL，自动下载并入库到「在线视频」。支持 1800+ 站点。</p>
              <div class="space-y-3">
                <input v-model="dlUrl" class="web3-input text-sm" placeholder="https://www.youtube.com/watch?v=xxx 或 https://www.bilibili.com/video/xxx" />
                <div class="flex flex-wrap gap-3 items-center">
                  <label class="text-xs text-gray-400">清晰度：</label>
                  <select v-model="dlQuality" class="web3-input text-xs py-1.5" style="width:auto">
                    <option value="best">自动最佳</option>
                    <option value="1080p">1080p</option>
                    <option value="720p">720p</option>
                    <option value="480p">480p</option>
                  </select>
                  <label class="flex items-center gap-1.5 text-xs text-gray-400 cursor-pointer">
                    <input type="checkbox" v-model="dlSubtitle" /> 下载字幕
                  </label>
                  <input v-model="dlTags" class="web3-input text-xs py-1.5" style="width:auto;max-width:160px" placeholder="标签（可选）" />
                </div>
                <button class="web3-btn px-6" :disabled="dlLoading || !dlUrl" @click="startDownload">
                  <span v-if="dlLoading" class="inline-block h-3.5 w-3.5 rounded-full border-2 border-white/30 border-t-white animate-spin"></span>
                  {{ dlLoading ? '下载中...' : '开始下载' }}
                </button>
                <div v-if="dlResult" class="text-xs mt-2" :class="dlResult.ok ? 'text-emerald-400' : 'text-red-400'">
                  {{ dlResult.msg }}
                </div>
              </div>
              <div class="mt-5 pt-4 border-t border-white/5">
                <p class="text-[11px] text-gray-600 leading-relaxed">
                  💡 提示：后端需安装 <code class="text-cyan-400">yt-dlp</code> 与 <code class="text-cyan-400">ffmpeg</code>。下载可能需要数秒到数分钟，完成后视频会出现在「在线视频」列表。
                </p>
              </div>
            </div>
          </template>

          <!-- 动漫推荐 -->
          <template v-else-if="videoTab === 'anime'">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="item in ANIME_RECOMMENDS" :key="item.title"
                class="glass-panel-sm p-5 group cursor-pointer transition-all duration-300 hover:-translate-y-0.5 hover:border-pink-400/30">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex items-center gap-2.5">
                    <span class="text-2xl">🍙</span>
                    <div>
                      <h4 class="font-semibold text-white text-sm group-hover:text-pink-300 transition">{{ item.title }}</h4>
                      <p class="text-[11px] text-gray-500 mt-0.5">{{ item.year }} · {{ item.tags.join(' / ') }}</p>
                    </div>
                  </div>
                  <span class="text-[11px] font-bold text-pink-300/90 bg-pink-500/10 border border-pink-400/20 rounded-full px-2 py-0.5">{{ item.rating }}</span>
                </div>
                <p class="text-[11px] text-gray-500 mt-3 line-clamp-3 leading-relaxed">{{ item.desc }}</p>
                <div class="flex flex-wrap items-center gap-2 mt-4">
                  <a :href="item.link" target="_blank" rel="noopener"
                    class="text-[11px] px-2.5 py-1 rounded-lg bg-pink-500/15 text-pink-300 border border-pink-400/20 hover:bg-pink-500/25 transition">▶ 去观看</a>
                  <a v-for="ln in item.links" :key="ln.label" :href="ln.url" target="_blank" rel="noopener"
                    class="text-[11px] px-2.5 py-1 rounded-lg border border-white/10 text-gray-400 hover:text-white hover:border-pink-400/20 transition">{{ ln.label }} ↗</a>
                </div>
              </div>
            </div>
          </template>

          <!-- 影视剧集 -->
          <template v-else-if="videoTab === 'movie'">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="item in MOVIE_LINKS" :key="item.title"
                class="glass-panel-sm p-5 group cursor-pointer transition-all duration-300 hover:-translate-y-0.5 hover:border-cyan-400/30">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex items-center gap-2.5">
                    <span class="text-2xl">🎬</span>
                    <div>
                      <h4 class="font-semibold text-white text-sm group-hover:text-cyan-300 transition">{{ item.title }}</h4>
                      <p class="text-[11px] text-gray-500 mt-0.5">{{ item.year }} · {{ item.tags.join(' / ') }}</p>
                    </div>
                  </div>
                  <span class="text-[11px] font-bold text-cyan-300/90 bg-cyan-500/10 border border-cyan-400/20 rounded-full px-2 py-0.5">{{ item.rating }}</span>
                </div>
                <p class="text-[11px] text-gray-500 mt-3 line-clamp-3 leading-relaxed">{{ item.desc }}</p>
                <div class="flex flex-wrap items-center gap-2 mt-4">
                  <a v-for="ln in item.links" :key="ln.label" :href="ln.url" target="_blank" rel="noopener"
                    class="text-[11px] px-2.5 py-1 rounded-lg bg-cyan-500/15 text-cyan-300 border border-cyan-400/20 hover:bg-cyan-500/25 transition">{{ ln.label }} ▶</a>
                </div>
              </div>
            </div>
          </template>

          <!-- 磁力资源 -->
          <template v-else>
            <p class="text-[11px] text-gray-500 mb-3">点击「复制磁力」即可一键复制到剪贴板，粘贴到下载器（如 qBittorrent / 迅雷）解析下载。</p>
            <div class="space-y-2.5">
              <div v-for="m in MAGNET_RESOURCES" :key="m.name"
                class="glass-panel-sm p-4 group flex flex-wrap items-center gap-3">
                <span class="text-xl">🧲</span>
                <div class="flex-1 min-w-0">
                  <h4 class="font-semibold text-white text-sm truncate group-hover:text-cyan-300 transition">{{ m.name }}</h4>
                  <p class="text-[11px] text-gray-500 mt-0.5">{{ m.count }} 个文件 · {{ m.size }} · {{ m.source }}</p>
                  <p class="text-[10px] text-gray-600 font-mono mt-1 truncate">{{ m.hash }}</p>
                </div>
                <button @click="copyHash(m.hash, m.name)"
                  class="text-[11px] px-3 py-1.5 rounded-lg bg-cyan-500/15 text-cyan-300 border border-cyan-400/25 hover:bg-cyan-500/25 transition whitespace-nowrap">
                  {{ copiedHash === m.hash ? '✓ 已复制' : '复制磁力' }}
                </button>
              </div>
            </div>
          </template>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 在线媒体页：音乐馆 / 图书 / 视频 三大模块标签页
// 音乐馆为完整音乐馆组件；图书与视频在此内联
// 播放条由 App.vue 级 GlobalPlayer 承载
// ============================================================
import { ref, onMounted } from 'vue'
import MusicHall from './MusicHall.vue'
import BookDesk from '@/components/media/BookDesk.vue'
import EnhancedVideoPlayer from '@/components/EnhancedVideoPlayer.vue'
import { getVideoList, downloadVideoFromUrl } from '@/api/media'
import { usePlayerStore } from '@/stores/modules/player'
import { VIDEO_TABS, ANIME_RECOMMENDS, MOVIE_LINKS, MAGNET_RESOURCES } from '@/data/mediaResources'

const player = usePlayerStore()
const videoList = ref([])
const videoTab = ref('online')
const copiedHash = ref('')

// 视频下载（HomeTube 风格）
const dlUrl = ref('')
const dlQuality = ref('best')
const dlSubtitle = ref(false)
const dlTags = ref('')
const dlLoading = ref(false)
const dlResult = ref(null)
async function startDownload() {
  if (!dlUrl.value.trim()) return
  dlLoading.value = true
  dlResult.value = null
  try {
    const res = await downloadVideoFromUrl({
      url: dlUrl.value.trim(),
      quality: dlQuality.value,
      subtitle: dlSubtitle.value,
      tags: dlTags.value || '网络下载',
    })
    const data = res?.data || res
    dlResult.value = { ok: true, msg: `✓ 下载成功：${data?.title || '视频已入库'}，去「在线视频」查看` }
    dlUrl.value = ''
    // 刷新列表
    await loadVideos()
  } catch (e) {
    dlResult.value = { ok: false, msg: '✗ 下载失败：' + (e?.response?.data?.message || e?.message || '网络错误') }
  }
  dlLoading.value = false
}

const tabs = [
  { key: 'music', label: '音乐馆', icon: '🎵' },
  { key: 'book', label: '图书', icon: '📜' },
  { key: 'video', label: '视频', icon: '🎬' },
]
const activeTab = ref('music')
const videoTabs = VIDEO_TABS

// 秒数格式化为「m:ss」
function formatDuration(secs) {
  if (!secs) return '--:--'
  const n = Number(secs)
  if (!isFinite(n)) return String(secs)
  const m = Math.floor(n / 60)
  const s = Math.floor(n % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

// 复制磁力链接到剪贴板
async function copyHash(hash, name) {
  try {
    await navigator.clipboard.writeText(hash)
    copiedHash.value = hash
    setTimeout(() => { copiedHash.value = '' }, 2000)
  } catch {
    const el = document.createElement('textarea')
    el.value = hash
    el.style.position = 'fixed'
    el.style.opacity = '0'
    document.body.appendChild(el)
    el.select()
    try { document.execCommand('copy') } catch {}
    document.body.removeChild(el)
    copiedHash.value = hash
    setTimeout(() => { copiedHash.value = '' }, 2000)
  }
}

// 播放视频时自动暂停全局音乐
function onVideoPlay() { if (player.isPlaying) player.pause() }

// 挂载时加载视频列表（音乐列表由 MusicHall 自行加载，图书由 BookDesk 自行加载）
async function loadVideos() {
  try {
    const res = await getVideoList({ page: 1, size: 50 })
    const data = res.data?.records || res.data || []
    videoList.value = data
  } catch { videoList.value = [] }
}
onMounted(() => {
  loadVideos()
})
</script>

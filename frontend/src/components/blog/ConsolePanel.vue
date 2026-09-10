<template>
  <Teleport to="body">
    <!-- 面板（悬浮按钮已移除，使用 Shift+A 呼出） -->
    <Transition name="console">
      <div v-if="ui.consoleOpen" class="fixed bottom-20 right-5 z-[110] w-[320px] max-w-[calc(100vw-24px)] rounded-2xl border border-white/10 bg-[#0a0a18]/95 backdrop-blur-xl shadow-2xl overflow-hidden">
        <!-- 顶栏：时钟 -->
        <div class="flex items-center justify-between px-4 py-3 border-b border-white/[0.06]">
          <div>
            <p class="text-lg font-mono text-white tracking-wider">{{ time }}</p>
            <p class="text-[10px] text-gray-600">{{ date }}</p>
          </div>
          <button class="text-gray-500 hover:text-white transition text-xs" @click="closeAll">✕</button>
        </div>

        <div class="max-h-[60vh] overflow-y-auto p-4 space-y-4">
          <!-- 站点统计 -->
          <section>
            <h4 class="ctl-title">站点统计</h4>
            <div class="grid grid-cols-3 gap-2">
              <div class="stat-box"><p class="stat-num">{{ stats.totalArticles }}</p><p class="stat-label">文章</p></div>
              <div class="stat-box"><p class="stat-num">{{ categories.length }}</p><p class="stat-label">分类</p></div>
              <div class="stat-box"><p class="stat-num">{{ tags.length }}</p><p class="stat-label">标签</p></div>
            </div>
          </section>

          <!-- 主题 -->
          <section>
            <h4 class="ctl-title">主题切换</h4>
            <div class="flex gap-2 flex-wrap">
              <button v-for="t in THEMES" :key="t" class="theme-chip"
                :class="{ 'theme-active': theme === t }" @click="setTheme(t)">
                {{ t }}
              </button>
            </div>
          </section>

          <!-- 音乐控制 -->
          <section>
            <h4 class="ctl-title">音乐</h4>
            <p class="text-xs text-gray-500 truncate mb-2">{{ player.currentTrack?.title || '未在播放' }}</p>
            <div class="flex gap-2">
              <button class="ctl-btn" @click="player.prev">⏮</button>
              <button class="ctl-btn ctl-btn-main" @click="player.toggle">{{ player.isPlaying ? '⏸' : '▶' }}</button>
              <button class="ctl-btn" @click="player.next">⏭</button>
              <button class="ctl-btn" @click="goMusic">🎧</button>
            </div>
          </section>

          <!-- 快捷入口 -->
          <section>
            <h4 class="ctl-title">快捷入口</h4>
            <div class="grid grid-cols-2 gap-2">
              <button class="ctl-link" @click="go('/blog/categories')">📁 分类</button>
              <button class="ctl-link" @click="go('/blog/tags')">🏷 标签</button>
              <button class="ctl-link" @click="go('/blog/archives')">🗂 归档</button>
              <button class="ctl-link" @click="randomPost">🎲 随机</button>
              <button class="ctl-link" @click="ui.toggleSearch()">🔎 搜索</button>
              <button class="ctl-link" @click="go('/blog/comments')">💬 留言板</button>
            </div>
          </section>

          <!-- 当前地址 -->
          <section>
            <h4 class="ctl-title">当前页面</h4>
            <div class="flex items-center gap-2">
              <p class="flex-1 text-[11px] text-gray-500 font-mono truncate bg-[#10102a] rounded-lg px-2.5 py-2">{{ location.href }}</p>
              <button class="ctl-btn text-xs" @click="copyUrl">📋</button>
            </div>
          </section>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
// ============================================================
// 中控台（ConsolePanel，Shift+A 打开）
// 时钟 / 站点统计（真实接口）/ 主题切换 / 音乐控制 / 快捷入口 / 当前 URL
// 右下角悬浮球，Esc 或外部点击关闭
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUiStore } from '@/stores/modules/ui'
import { usePlayerStore } from '@/stores/modules/player'
import { useTheme, THEMES } from '@/composables/useTheme'
import { getBlogStats, getBlogCategories, getBlogTags, getBlogRandom } from '@/api/blog'
import { useToastStore } from '@/stores/modules/toast'

const ui = useUiStore()
const player = usePlayerStore()
const router = useRouter()
const toast = useToastStore()
const { theme, setTheme } = useTheme()

const time = ref('')
const date = ref('')
const stats = ref({ totalArticles: '-', totalWords: '-', lastUpdated: '' })
const categories = ref([])
const tags = ref([])

let clockTimer = null
let dataTimer = null

function tick() {
  const now = new Date()
  time.value = now.toTimeString().slice(0, 8)
  date.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

// 数据节流：面板打开后每 60s 拉一次真实统计
async function loadStats() {
  try { stats.value = (await getBlogStats()) || stats.value } catch { /* 静默 */ }
  try { categories.value = (await getBlogCategories()) || [] } catch { /* 静默 */ }
  try { tags.value = (await getBlogTags()) || [] } catch { /* 静默 */ }
}

function go(path) { ui.setConsole(false); router.push(path) }
function goMusic() { ui.setConsole(false); router.push('/music') }

async function randomPost() {
  ui.setConsole(false)
  try {
    const a = await getBlogRandom()
    if (a?.id) router.push(`/blog/post/${a.id}`)
  } catch { /* 静默 */ }
}

async function copyUrl() {
  try {
    await navigator.clipboard.writeText(location.href)
    toast.success('地址已复制')
  } catch {
    toast.error('复制失败')
  }
}

function onKeydown(e) {
  if (e.key === 'Escape') ui.setConsole(false)
}

function closeAll() {
  ui.setConsole(false)
}

let booted = false
onMounted(() => {
  tick()
  clockTimer = setInterval(tick, 1000)
  if (!booted) {
    loadStats()
    booted = true
  }
  dataTimer = setInterval(() => { if (ui.consoleOpen) loadStats() }, 60000)
  document.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => {
  clearInterval(clockTimer)
  clearInterval(dataTimer)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.ctl-title { @apply text-[11px] uppercase tracking-wider text-gray-600 mb-2; }
.stat-box { @apply rounded-lg bg-[#10102a] border border-white/[0.05] py-2 text-center; }
.stat-num { @apply text-lg font-black text-white font-mono; }
.stat-label { @apply text-[10px] text-gray-600 mt-0.5; }
.theme-chip {
  @apply px-2.5 py-1 rounded-md text-[11px] capitalize text-gray-500 border border-white/[0.08]
    hover:text-white hover:border-white/[0.18] transition-colors;
}
.theme-active { @apply text-cyan-300 border-cyan-400/40 bg-cyan-400/10; }
.ctl-btn {
  @apply flex-1 h-9 rounded-lg text-sm text-gray-300 bg-[#12122e] border border-white/[0.08]
    hover:text-white hover:bg-[#1e1e4a] transition-colors flex items-center justify-center;
}
.ctl-btn-main {
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  border: none;
}
.ctl-link {
  @apply px-2.5 py-2 rounded-lg text-xs text-left text-gray-400 bg-[#0e0e26] border border-white/[0.05]
    hover:text-white hover:bg-[#181840] transition-colors;
}
.console-enter-active, .console-leave-active { transition: all 0.22s cubic-bezier(0.23, 1, 0.32, 1); }
.console-enter-from, .console-leave-to { opacity: 0; transform: translateY(10px) scale(0.97); }
</style>
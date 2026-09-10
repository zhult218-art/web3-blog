<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed z-[140]" :style="{ left: x + 'px', top: y + 'px' }">
      <div class="ctx-menu" @click.stop>
        <!-- 命中链接 -->
        <template v-if="ctx.link">
          <button class="mi" @click="act('window.open', ctx.link.href)">↗ 新窗口打开</button>
          <button class="mi" @click="act('copy', ctx.link.href)">🔗 复制链接地址</button>
          <div class="divider"></div>
        </template>

        <!-- 命中图片 -->
        <template v-if="ctx.img">
          <button class="mi" @click="act('img-open', ctx.img.src)">🖼 新窗口打开图片</button>
          <button class="mi" @click="act('img-copy', ctx.img.src)">📋 复制图片</button>
          <button class="mi" @click="act('img-download', ctx.img.src)">⬇ 下载图片</button>
          <div class="divider"></div>
        </template>

        <!-- 选中文本 -->
        <template v-if="selectedText">
          <button class="mi" @click="act('copy', selectedText)">📋 复制选中文本</button>
          <button class="mi" @click="act('baidu', selectedText)">🔍 百度搜索「{{ brief(selectedText) }}」</button>
          <button class="mi" @click="act('blog-search', selectedText)">📖 站内搜索「{{ brief(selectedText) }}」</button>
          <div class="divider"></div>
        </template>

        <!-- 音乐控制 -->
        <template v-if="player.hasTrack">
          <button class="mi" @click="act('toggle')">{{ player.isPlaying ? '⏸ 暂停音乐' : '▶ 播放音乐' }}</button>
          <button class="mi" @click="act('prev')">⏮ 上一首</button>
          <button class="mi" @click="act('next')">⏭ 下一首</button>
          <button class="mi" @click="act('playlist')">🎧 查看所有歌曲</button>
          <button class="mi" @click="act('copy', player.currentTrack?.title)">📝 复制歌名</button>
          <div class="divider"></div>
        </template>

        <!-- 全局项 -->
        <button class="mi" @click="act('blog-random')">🎲 随便逛逛</button>
        <button class="mi" @click="act('blog-categories')">📁 博客分类</button>
        <button class="mi" @click="act('blog-tags')">🏷 文章标签</button>
        <div class="divider"></div>
        <button class="mi" @click="act('copy', location.href)">🔗 复制地址</button>
        <button class="mi" @click="act('theme')">🎨 {{ themeLabel }}</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
// ============================================================
// 自定义右键菜单（CustomContextMenu）
// 命中元素动态生成菜单项：
//   链接→新窗口/复制地址；图片→新窗口/复制/下载；文本→复制/百度/站内搜索
//   音乐控制 + 全局项（随便逛逛/分类/标签/复制地址/主题切换）
// Shift+I 切换 自定义/原生 右键；Esc 或点击外部关闭
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUiStore } from '@/stores/modules/ui'
import { usePlayerStore } from '@/stores/modules/player'
import { useTheme } from '@/composables/useTheme'
import { getBlogRandom } from '@/api/blog'
import { useToastStore } from '@/stores/modules/toast'

const ui = useUiStore()
const player = usePlayerStore()
const router = useRouter()
const toast = useToastStore()
const { theme, cycle } = useTheme()

const visible = ref(false)
const x = ref(0)
const y = ref(0)
const ctx = ref({ link: null, img: null })
const selectedText = ref('')

const themeLabel = ref('')

function brief(s) { return s.length > 12 ? s.slice(0, 12) + '…' : s }

function onContextMenu(e) {
  if (!ui.ctxMenuEnabled) return
  // 输入框内保留原生菜单（方便剪切/粘贴）
  const t = e.target
  if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA')) return

  e.preventDefault()
  const link = e.target.closest?.('a') || null
  const img = e.target.closest?.('img') || null
  ctx.value = { link, img }

  selection()
  const menuW = 210
  const menuH = estimateHeight()
  x.value = Math.min(e.clientX, window.innerWidth - menuW - 8)
  y.value = Math.min(e.clientY, window.innerHeight - menuH - 8)
  visible.value = true
}

function estimateHeight() {
  let rows = 0
  const has = (n) => n
  rows += has(ctx.value.link) ? 3 : 0
  rows += has(ctx.value.img) ? 4 : 0
  rows += selectedText.value ? 4 : 0
  rows += player.hasTrack ? 6 : 0
  rows += 6 // 全局项: 随便逛逛/分类/标签 + divider + 复制地址/主题
  return rows * 34 + 12
}

function selection() {
  const s = String(window.getSelection?.() || '').trim()
  selectedText.value = s
}

async function act(name, arg) {
  visible.value = false
  switch (name) {
    case 'window.open':
      window.open(arg, '_blank', 'noopener')
      break
    case 'copy':
      try {
        await navigator.clipboard.writeText(String(arg))
        toast.success('已复制')
      } catch {
        toast.error('复制失败')
      }
      break
    case 'img-open':
      window.open(arg, '_blank', 'noopener')
      break
    case 'img-copy':
      copyImage(arg)
      break
    case 'img-download':
      downloadImage(arg)
      break
    case 'baidu':
      window.open(`https://www.baidu.com/s?wd=${encodeURIComponent(arg)}`, '_blank', 'noopener')
      break
    case 'blog-search':
      ui.pendingSearch = arg
      ui.setSearch(true)
      break
    case 'toggle': player.toggle(); break
    case 'prev': player.prev(); break
    case 'next': player.next(); break
    case 'playlist': openPlaylist(); break
    case 'blog-random': randomPost(); break
    case 'blog-categories': router.push('/blog/categories'); break
    case 'blog-tags': router.push('/blog/tags'); break
    case 'theme': cycle(); break
  }
}

function openPlaylist() {
  // 播放列表 UI 由 GlobalPlayer 承载，这里统一跳到音乐馆浏览全部曲目
  router.push('/music')
}

function copyImage(src) {
  fetch(src)
    .then(r => r.blob())
    .then(blob => navigator.clipboard.write([new ClipboardItem({ 'image/png': blob })]))
    .then(() => toast.success('图片已复制'))
    .catch(() => {
      // 跨域不允许时降级为下载
      downloadImage(src)
    })
}

function downloadImage(src) {
  const a = document.createElement('a')
  a.href = src
  a.download = src.split('/').pop() || 'image'
  a.target = '_blank'
  document.body.appendChild(a)
  a.click()
  a.remove()
}

async function randomPost() {
  try {
    const a = await getBlogRandom()
    if (a?.id) router.push(`/blog/post/${a.id}`)
  } catch { /* 静默 */ }
}

function onKeydown(e) {
  if (e.key === 'Escape') visible.value = false
}

function onDocClick() {
  visible.value = false
}

onMounted(() => {
  document.addEventListener('contextmenu', onContextMenu)
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('click', onDocClick)
  window.addEventListener('blur', onDocClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('contextmenu', onContextMenu)
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('click', onDocClick)
  window.removeEventListener('blur', onDocClick)
})
</script>

<style scoped>
.ctx-menu {
  @apply min-w-[200px] py-1.5 rounded-xl border border-white/10 bg-[#0b0b1a]/95 backdrop-blur-xl shadow-2xl;
}
.mi {
  @apply flex items-center gap-2 w-full px-3.5 py-2 text-left text-xs text-gray-300
    hover:text-white hover:bg-[#16163a] transition-colors;
}
.divider { @apply mx-3 my-1 h-px bg-[#141432]; }
</style>
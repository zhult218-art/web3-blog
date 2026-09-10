// ============================================================
// 主题切换（useTheme）
// 本博客主题：nebula / cyber / inferno / quantum
// 通过 body[data-theme] + localStorage['theme'] 持久化
// 提供 cycle() 顺延切换（Shift+D 触发）
// 背景壁纸：body.has-wallpaper + --wallpaper-img + localStorage['wallpaper']
// ============================================================
import { ref } from 'vue'
import { WALLPAPERS, DEFAULT_WALLPAPER } from '@/config/wallpapers'

export const THEMES = ['aurora', 'nebula', 'cyber', 'inferno', 'quantum']

const KEY = 'theme'
const WKEY = 'wallpaper_v3' // v3：动态壁纸集（升级旧键让老用户直接看到新默认动态壁纸）

// 模块级响应式壁纸状态：App.vue（背景层渲染）与各组件共享同一引用
export const currentWallpaper = ref(getStoredWallpaper())

export function getStoredTheme() {
  try {
    const t = localStorage.getItem(KEY)
    return THEMES.includes(t) ? t : 'nebula'
  } catch {
    return 'nebula'
  }
}

export function getStoredWallpaper() {
  try {
    const w = localStorage.getItem(WKEY)
    return WALLPAPERS.some(x => x.id === w) ? w : DEFAULT_WALLPAPER
  } catch {
    return DEFAULT_WALLPAPER
  }
}

export function applyTheme(t) {
  document.body.dataset.theme = t
  try { localStorage.setItem(KEY, t) } catch { /* 忽略 */ }
}

export function applyWallpaper(id) {
  const w = WALLPAPERS.find(x => x.id === id)
  if (w && (w.file || w.video)) {
    // 图片壁纸 → CSS 背景变量；视频壁纸 → App.vue #bg-layer 渲染 <video>（poster 兜底）
    // 两类都保留 has-wallpaper（渐变暗角 + 云光层，保证前景可读且不加全屏动画）
    document.body.classList.add('has-wallpaper')
    if (w.file) document.body.style.setProperty('--wallpaper-img', `url('${w.file}')`)
    else document.body.style.removeProperty('--wallpaper-img')
  } else {
    document.body.classList.remove('has-wallpaper')
    document.body.style.removeProperty('--wallpaper-img')
  }
  currentWallpaper.value = id
  try { localStorage.setItem(WKEY, id) } catch { /* 忽略 */ }
}

// 初始化：把持久化的主题与壁纸应用回 body（App.vue 挂载时调用）
export function initApplied() {
  applyTheme(getStoredTheme())
  applyWallpaper(getStoredWallpaper())
}

export function useTheme() {
  const theme = ref(getStoredTheme())
  const wallpaper = currentWallpaper // 与 App.vue 背景层共享同一响应式状态

  function setTheme(t) {
    theme.value = t
    applyTheme(t)
  }

  function setWallpaper(id) {
    wallpaper.value = id
    applyWallpaper(id)
  }

  // 顺延切到下一个主题
  function cycle() {
    const idx = THEMES.indexOf(theme.value)
    setTheme(THEMES[(idx + 1) % THEMES.length])
  }

  return { theme, wallpaper, setTheme, setWallpaper, cycle }
}
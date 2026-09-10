// ============================================================
// 全站快捷键注册器（useShortcuts）
// Shift+字母 组合，优先级与目标站（anzhiyu/lololowe）一致：
//   S 站内搜索  D 切换主题  R 随机文章  H 首页  M 播放/暂停
//   A 中控台    L 友链      P 关于      I 右键菜单切换  K 快捷键面板
// 规则：
//   - 输入框（input/textarea/select/contenteditable）聚焦时跳过
//   - localStorage['shortcuts_disabled'] === '1' 时整体禁用（面板可开关）
//   - keydown 监听注册于 window，组件销毁自动解绑
// ============================================================
import { onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUiStore } from '@/stores/modules/ui'
import { usePlayerStore } from '@/stores/modules/player'
import { useTheme } from '@/composables/useTheme'
import { getBlogRandom } from '@/api/blog'

const DISABLE_KEY = 'blog_shortcuts_disabled'

export function isShortcutsDisabled() {
  try { return localStorage.getItem(DISABLE_KEY) === '1' } catch { return false }
}

export function setShortcutsDisabled(v) {
  try { localStorage.setItem(DISABLE_KEY, v ? '1' : '0') } catch { /* 忽略 */ }
}

// 供快捷键面板展示的统一绑定表
export const SHORTCUT_BINDINGS = [
  { key: 'Shift+S', label: '站内搜索' },
  { key: 'Shift+D', label: '切换主题' },
  { key: 'Shift+R', label: '随机文章' },
  { key: 'Shift+H', label: '回到首页' },
  { key: 'Shift+M', label: '播放/暂停音乐' },
  { key: 'Shift+A', label: '中控台' },
  { key: 'Shift+L', label: '友人帐' },
  { key: 'Shift+P', label: '关于' },
  { key: 'Shift+I', label: '切换右键菜单' },
  { key: 'Shift+K', label: '快捷键面板' }
]

function isEditableTarget(e) {
  const t = e.target
  if (!t) return false
  const tag = t.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || t.isContentEditable
}

export function useShortcuts() {
  const router = useRouter()
  const ui = useUiStore()
  const player = usePlayerStore()
  const { cycle } = useTheme()

  function handler(e) {
    if (!e.shiftKey || e.ctrlKey || e.altKey || e.metaKey) return
    if (isShortcutsDisabled() || isEditableTarget(e)) return
    const key = e.key.toUpperCase()
    const run = {
      S: () => ui.toggleSearch(),
      D: () => cycle(),
      R: () => randomPost(),
      H: () => router.push('/'),
      M: () => player.toggle(),
      A: () => ui.toggleConsole(),
      L: () => router.push('/blog/link'),
      P: () => router.push('/about'),
      I: () => ui.toggleNative(),
      K: () => ui.toggleShortcuts()
    }[key]
    if (run) {
      e.preventDefault()
      run()
    }
  }

  async function randomPost() {
    try {
      const a = await getBlogRandom()
      if (a?.id) router.push(`/blog/post/${a.id}`)
    } catch { /* 无文章时静默 */ }
  }

  onMounted(() => window.addEventListener('keydown', handler))
  onBeforeUnmount(() => window.removeEventListener('keydown', handler))
}
// ============================================================
// 全站氛围组件 UI Store（Pinia）
// 中控台 / 快捷键面板 / 站内搜索 / 自定义右键菜单 的开关状态
// ctxMenuEnabled 持久化到 localStorage（Shift+I 切换）
// ============================================================
import { defineStore } from 'pinia'
import { ref } from 'vue'

const CTX_KEY = 'blog_ctx_menu_enabled'

function readCtxFlag() {
  try {
    const v = localStorage.getItem(CTX_KEY)
    return v === null ? true : v === '1'
  } catch {
    return true
  }
}

export const useUiStore = defineStore('ui', () => {
  const consoleOpen = ref(false)
  const shortcutsOpen = ref(false)
  const searchOpen = ref(false)
  const pendingSearch = ref('')
  const ctxMenuEnabled = ref(readCtxFlag())

  function toggleConsole() { consoleOpen.value = !consoleOpen.value }
  function setConsole(v) { consoleOpen.value = v }
  function toggleShortcuts() { shortcutsOpen.value = !shortcutsOpen.value }
  function toggleSearch() { searchOpen.value = !searchOpen.value }
  function setSearch(v) { searchOpen.value = v }

  // Shift+I：切换 自定义/原生 右键菜单，状态持久化
  function toggleNative() {
    ctxMenuEnabled.value = !ctxMenuEnabled.value
    try {
      localStorage.setItem(CTX_KEY, ctxMenuEnabled.value ? '1' : '0')
    } catch { /* 忽略隐私模式 */ }
  }

  return {
    consoleOpen, shortcutsOpen, searchOpen, ctxMenuEnabled, pendingSearch,
    toggleConsole, setConsole, toggleShortcuts, toggleSearch, setSearch, toggleNative
  }
})
// ============================================================
// 全局音乐快捷键 composable
// 参考 YesPlayMusic 的键盘控制习惯：
//   Space       播放 / 暂停（聚焦 input/textarea/contenteditable 时不拦截）
//   Ctrl + ←    上一首
//   Ctrl + →    下一首
//   ↑ / ↓       音量加 / 减 5%
//   L           喜欢 / 取消
//   M           静音切换
//   S           聚焦搜索框（emit 'focus-search' 让宿主处理）
// 在音乐页 onMounted 启用、onBeforeUnmount 卸载，避免与全局冲突
// ============================================================
import { onMounted, onBeforeUnmount } from 'vue'
import { usePlayerStore } from '@/stores/modules/player'

// 判断当前是否聚焦在可编辑元素上（input/textarea/contenteditable）
function isEditableTarget(el) {
  if (!el) return false
  const tag = el.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return true
  if (el.isContentEditable) return true
  return false
}

/**
 * 启用音乐全局快捷键
 * @param {Object} opts
 * @param {Function} [opts.onToggleLike]  按 L 触发（宿主实现：调接口切换收藏）
 * @param {Function} [opts.onFocusSearch] 按 S 触发（宿主聚焦搜索框）
 */
export function useMusicHotkeys(opts = {}) {
  const player = usePlayerStore()
  const onToggleLike = opts.onToggleLike || (() => {})
  const onFocusSearch = opts.onFocusSearch || (() => {})

  // 静音前的音量记忆
  let savedVol = player.volume > 0 ? player.volume : 0.7

  function onKeyDown(e) {
    // 修饰键组合：仅处理 Ctrl+← 与 Ctrl+→
    if (e.ctrlKey || e.metaKey) {
      if (e.code === 'ArrowLeft') { e.preventDefault(); player.prev(); return }
      if (e.code === 'ArrowRight') { e.preventDefault(); player.next(); return }
      return // 其他 Ctrl 组合不处理
    }

    // 聚焦在可编辑元素时，只放行 Esc，避免拦截输入
    if (isEditableTarget(e.target)) return

    switch (e.code) {
      case 'Space':
        e.preventDefault()
        player.toggle()
        break
      case 'ArrowUp':
        e.preventDefault()
        player.setVolume(Math.min(1, (player.volume ?? 0.7) + 0.05))
        break
      case 'ArrowDown':
        e.preventDefault()
        player.setVolume(Math.max(0, (player.volume ?? 0.7) - 0.05))
        break
      case 'KeyL':
        e.preventDefault()
        onToggleLike()
        break
      case 'KeyM':
        e.preventDefault()
        if (player.volume > 0) { savedVol = player.volume; player.setVolume(0) }
        else { player.setVolume(savedVol > 0 ? savedVol : 0.7) }
        break
      case 'KeyS':
        e.preventDefault()
        onFocusSearch()
        break
      default:
        break
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', onKeyDown)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', onKeyDown)
  })
}

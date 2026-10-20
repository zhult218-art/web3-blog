// ============================================================
// 区块按需挂载（useLazyMount）
//
// 借鉴自 Astro 博客在正文中用 IntersectionObserver + rootMargin 800px
// 提前挂载音频播放器空壳的做法：把评论区、可视化这类"重"组件从首屏渲染
// 路径中移出，等元素进入视口前 800px 的预加载窗口内再挂载真实组件。
//
// 用法：
//   const { target, shown } = useLazyMount()
//   <div ref="target"><CommentPanel v-if="shown" ... /></div>
//
// 注意：锚点元素本身必须常驻（不要用 v-if 包住 ref 所在元素），
// 否则 observer 在挂载时就拿不到 target。
// 不支持 IntersectionObserver 的环境（老浏览器 / 测试）直接置为已挂载。
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'

export function useLazyMount(options = {}) {
  const { rootMargin = '800px', threshold = 0 } = options
  const target = ref(null)
  const shown = ref(false)
  let observer = null

  onMounted(() => {
    const el = target.value
    if (!el || typeof IntersectionObserver === 'undefined') {
      shown.value = true
      return
    }
    observer = new IntersectionObserver((entries) => {
      if (entries.some(e => e.isIntersecting)) {
        shown.value = true
        // 挂载后立刻断开，避免停留观察带来的额外开销
        observer?.disconnect()
        observer = null
      }
    }, { rootMargin, threshold })
    observer.observe(el)
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
  })

  return { target, shown }
}

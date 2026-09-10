// ============================================================
// 滚动显现指令 v-reveal（及左右 / 缩放变体）
// 元素进入视口后，为其内部所有 .motions-reveal* 节点添加 .visible
// 四个指令为同一实现，仅命名不同，由 main.js 全局注册
// ============================================================

// 核心实现：挂载时用 IntersectionObserver 监听，3 秒后自动断开兜底
export const vReveal = {
  mounted(el) {
    const reveal = () => {
      el.querySelectorAll('.motions-reveal, .motions-reveal-left, .motions-reveal-right, .motions-reveal-scale').forEach(c => c.classList.add('visible'))
    }
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) { reveal(); observer.disconnect() }
    }, { threshold: 0.01, rootMargin: '0px 0px -10px 0px' })
    observer.observe(el)
    setTimeout(() => { observer.disconnect() }, 3000)
  }
}
// 变体别名：v-reveal-left / v-reveal-right / v-reveal-scale
export const vRevealLeft = vReveal
export const vRevealRight = vReveal
export const vRevealScale = vReveal

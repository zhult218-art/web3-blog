// ============================================================
// 3D 倾斜指令 v-tilt
// 鼠标在卡片上移动时产生轻微的 3D 跟随倾斜（rAF 节流 + 指针离开复位）。
// - 仅在「鼠标 + 精确指针」设备启用，触屏 / 键盘用户不受影响
// - 遵循 prefers-reduced-motion：用户要求减少动效时自动禁用
// - 用法：<div v-tilt> 或 <div v-tilt="8">（数值 = 最大倾角，默认 5°，单位度）
// ============================================================

export const vTilt = {
  mounted(el, binding) {
    if (typeof window === 'undefined') return
    // 尊重系统「减少动态效果」设置
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    // 仅精确指针（鼠标 / 触控板）启用，避免移动端手指遮挡 + 无效查询
    if (!window.matchMedia('(hover: hover) and (pointer: fine)').matches) return

    const max = Number(binding.value) > 0 ? Number(binding.value) : 5
    el.classList.add('tilt-3d')

    let raf = 0
    const onMove = e => {
      const rect = el.getBoundingClientRect()
      if (!rect.width || !rect.height) return
      const px = (e.clientX - rect.left) / rect.width - 0.5
      const py = (e.clientY - rect.top) / rect.height - 0.5
      cancelAnimationFrame(raf)
      raf = requestAnimationFrame(() => {
        el.style.transform =
          `perspective(900px) rotateX(${(-py * max).toFixed(2)}deg) ` +
          `rotateY(${(px * max).toFixed(2)}deg) translateY(-3px)`
      })
    }
    const onLeave = () => {
      cancelAnimationFrame(raf)
      el.style.transform = ''
    }

    el.addEventListener('pointermove', onMove)
    el.addEventListener('pointerleave', onLeave)
    el.__vTilt = { onMove, onLeave }
  },
  unmounted(el) {
    const handlers = el.__vTilt
    if (!handlers) return
    el.removeEventListener('pointermove', handlers.onMove)
    el.removeEventListener('pointerleave', handlers.onLeave)
    delete el.__vTilt
  }
}

// ============================================================
// 设备能力判断（useDeviceCapability）
// 供全屏背景等「重型动画」组件复用：
//   isMobile      触摸屏 / 窄屏设备（关抗锯齿、降 DPR、降粒子数）
//   reducedMotion 系统开启了「减少动效」偏好
//   lowEnd        低配设备（CPU 核数 ≤ 4）
//   staticBackground  以上任一命中 → 背景只渲染一帧静态画面，不跑动画循环
// 说明：这些能力在页面生命周期内基本恒定，故在模块加载时求值一次即可。
// ============================================================

function detectMobile() {
  if (typeof window === 'undefined' || !window.matchMedia) return false
  return window.matchMedia('(max-width: 768px)').matches ||
    window.matchMedia('(pointer: coarse)').matches
}

function detectReducedMotion() {
  if (typeof window === 'undefined' || !window.matchMedia) return false
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

const isMobile = detectMobile()
const reducedMotion = detectReducedMotion()
const cpuCores = (typeof navigator !== 'undefined' && navigator.hardwareConcurrency) || null
const lowEnd = cpuCores != null && cpuCores <= 4

// 是否降级为「静态一帧」：移动端 / 减少动效 / 低配
const staticBackground = isMobile || reducedMotion || lowEnd

export function useDeviceCapability() {
  return { isMobile, reducedMotion, cpuCores, lowEnd, staticBackground }
}

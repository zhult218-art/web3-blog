// 页面高亮引导：语音导航跳到目标路由后，定位页面元素、滚动到视野并高亮，附一句提示气泡。
//
// 定位优先级：显式 selector → 页面 [data-voice-target] → <main> → 兜底只显示顶部提示气泡。
// 高亮层独立于原元素样式（绝对定位覆盖层），退出时彻底移除，不污染页面。

import { nextTick } from 'vue'

const STYLE_ID = 'voice-guide-style'
let overlayEl = null
let bubbleEl = null
let targetEl = null
let autoHideTimer = 0
let repositionBound = false

function ensureStyle() {
  if (document.getElementById(STYLE_ID)) return
  const style = document.createElement('style')
  style.id = STYLE_ID
  style.textContent = `
.voice-guide-overlay{position:fixed;z-index:9998;pointer-events:none;border-radius:14px;
  border:2px solid rgba(96,220,255,.95);box-shadow:0 0 0 3px rgba(96,220,255,.18),0 0 22px 6px rgba(96,220,255,.55),inset 0 0 18px rgba(96,220,255,.25);
  transition:all .28s cubic-bezier(.22,1,.36,1);animation:voice-guide-pulse 1.6s ease-in-out infinite}
@keyframes voice-guide-pulse{0%,100%{box-shadow:0 0 0 3px rgba(96,220,255,.16),0 0 18px 4px rgba(96,220,255,.42)}
  50%{box-shadow:0 0 0 5px rgba(96,220,255,.28),0 0 30px 10px rgba(96,220,255,.68)}}
.voice-guide-bubble{position:fixed;z-index:9999;max-width:280px;padding:10px 14px;border-radius:12px;
  background:linear-gradient(135deg,rgba(14,26,46,.96),rgba(24,42,72,.96));color:#dff2ff;font-size:13px;line-height:1.5;
  border:1px solid rgba(96,220,255,.5);box-shadow:0 10px 30px rgba(0,0,0,.45);pointer-events:none;
  transition:opacity .25s ease;animation:voice-guide-in .3s ease}
@keyframes voice-guide-in{from{opacity:0;transform:translateY(6px)}to{opacity:1;transform:none}}`
  document.head.appendChild(style)
}

function findTarget(selector) {
  if (selector) {
    try { const el = document.querySelector(selector); if (el) return el } catch { /* 非法 selector */ }
  }
  const attr = document.querySelector('[data-voice-target]')
  if (attr) return attr
  return document.querySelector('main') || null
}

function positionOverlay() {
  if (!targetEl || !overlayEl) return
  const r = targetEl.getBoundingClientRect()
  const pad = 6
  overlayEl.style.left = `${Math.max(0, r.left - pad)}px`
  overlayEl.style.top = `${Math.max(0, r.top - pad)}px`
  overlayEl.style.width = `${r.width + pad * 2}px`
  overlayEl.style.height = `${r.height + pad * 2}px`
}

function positionBubble() {
  if (!bubbleEl) return
  const vw = window.innerWidth
  let top = 16
  if (targetEl) {
    const r = targetEl.getBoundingClientRect()
    top = r.top > 90 ? r.top - 12 - bubbleEl.offsetHeight : Math.min(r.bottom + 12, window.innerHeight - 80)
  }
  const w = bubbleEl.offsetWidth || 260
  bubbleEl.style.left = `${Math.max(12, Math.min(vw / 2 - w / 2, vw - w - 12))}px`
  bubbleEl.style.top = `${Math.max(12, top)}px`
}

function onReposition() {
  positionOverlay()
  positionBubble()
}

function bindReposition() {
  if (repositionBound) return
  repositionBound = true
  window.addEventListener('scroll', onReposition, true)
  window.addEventListener('resize', onReposition)
}

function unbindReposition() {
  if (!repositionBound) return
  repositionBound = false
  window.removeEventListener('scroll', onReposition, true)
  window.removeEventListener('resize', onReposition)
}

/** 清除高亮与提示气泡（引导结束/组件卸载时调用） */
export function clearVoiceHighlight() {
  if (autoHideTimer) { clearTimeout(autoHideTimer); autoHideTimer = 0 }
  unbindReposition()
  if (overlayEl) { try { overlayEl.remove() } catch { /* ignore */ } overlayEl = null }
  if (bubbleEl) { try { bubbleEl.remove() } catch { /* ignore */ } bubbleEl = null }
  targetEl = null
}

/**
 * 跳转并高亮引导。
 * @param {import('vue-router').Router} router
 * @param {{route?:string, tip?:string, selector?:string}} guide
 * @param {number} [autoHideMs] 自动清除时间
 * @returns {Promise<boolean>} 是否成功定位到元素并高亮
 */
export async function guideTo(router, guide = {}, autoHideMs = 9000) {
  clearVoiceHighlight()
  ensureStyle()

  if (guide.route) {
    try { await router.push(guide.route) } catch { /* 导航失败不阻断提示 */ }
  }
  await nextTick()
  await new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r)))

  targetEl = findTarget(guide.selector)

  // 顶部提示气泡
  bubbleEl = document.createElement('div')
  bubbleEl.className = 'voice-guide-bubble'
  bubbleEl.textContent = guide.tip || '看看这里'
  document.body.appendChild(bubbleEl)

  const hit = !!targetEl
  if (hit) {
    try { targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' }) } catch { /* ignore */ }
    overlayEl = document.createElement('div')
    overlayEl.className = 'voice-guide-overlay'
    document.body.appendChild(overlayEl)
    positionOverlay()
    // 等平滑滚动落定后再校正一次位置
    setTimeout(() => { positionOverlay(); positionBubble() }, 420)
  } else {
    bubbleEl.style.left = '50%'
    bubbleEl.style.transform = 'translateX(-50%)'
    bubbleEl.style.top = '18px'
  }
  positionBubble()
  bindReposition()

  if (autoHideMs > 0) {
    autoHideTimer = setTimeout(() => clearVoiceHighlight(), autoHideMs)
  }
  return hit
}

<template>
  <div v-if="pending" class="fixed inset-0 z-[95] flex items-center justify-center bg-black/70 backdrop-blur-sm"
    @click.self="cancel">
    <div class="portal-panel w-full max-w-md mx-4 p-6 text-center" role="alertdialog" aria-modal="true">
      <!-- 顶部符文环 -->
      <div class="rune-ring mx-auto mb-4">
        <span class="rune-spin">✦</span>
        <span class="rune-core">🌀</span>
      </div>

      <h3 class="text-lg font-bold tracking-widest text-white">异界传送阵</h3>
      <p class="mt-2 text-sm leading-relaxed text-gray-300">
        冒险者，此链接通往<strong class="text-cyan-300">结界之外</strong>的位面：<br />
        <span class="text-amber-300 font-semibold">{{ host }}</span>
      </p>

      <div class="mt-3 rounded-lg border border-white/10 bg-black/40 px-3 py-2 text-left">
        <p class="text-[10px] uppercase tracking-widest text-gray-500 mb-0.5">传送坐标</p>
        <p class="text-xs break-all text-gray-300">{{ pendingUrl }}</p>
      </div>

      <div class="mt-5 flex items-center justify-center gap-3">
        <button class="btn-confirm flex-1 py-2.5 rounded-xl font-bold tracking-wider"
          @click="confirmGo">✧ 确认传送</button>
        <button class="flex-1 py-2.5 rounded-xl font-bold tracking-wider border border-white/15 text-gray-300 hover:bg-white/5 transition"
          @click="cancel">留在此界</button>
      </div>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 全局外链传送确认（异世界风格）
// 双通道拦截所有指向站外 http(s) 的跳转：
//   1) 捕获阶段拦截 <a> 链接点击
//   2) 劫持 window.open（JS 直调的跳转，如轮播卡片、右键菜单）
// 弹出「异界传送阵」确认弹窗，确认后新窗口打开目标站
// ============================================================
import { ref, onMounted, onUnmounted } from 'vue'

const pending = ref(false)
const pendingUrl = ref('')
const host = ref('')

// 确认后放行的内部开关：置位期间 window.open 直接放行
let bypass = false

function askPermission(href) {
  pendingUrl.value = href
  try { host.value = new URL(href).hostname } catch { host.value = href }
  pending.value = true
}

function isExternal(raw) {
  if (!raw || raw.startsWith('#')) return null
  let url
  try {
    url = new URL(String(raw), window.location.href)
  } catch {
    return null
  }
  // 仅拦截 http/https 且跨源（含不同端口）的链接
  if (!/^https?:$/.test(url.protocol)) return null
  if (url.origin === window.location.origin) return null
  return url.href
}

function onClick(e) {
  if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return
  const a = e.target?.closest?.('a[href]')
  if (!a) return
  const href = isExternal(a.getAttribute('href'))
  if (!href) return
  e.preventDefault()
  e.stopPropagation()
  askPermission(href)
}

function confirmGo() {
  const url = pendingUrl.value
  pending.value = false
  bypass = true
  try {
    window.open(url, '_blank', 'noopener,noreferrer')
  } finally {
    bypass = false
  }
}

function cancel() {
  pending.value = false
  pendingUrl.value = ''
}

function onKeydown(e) {
  if (!pending.value) return
  if (e.key === 'Escape') cancel()
}

// 劫持 window.open：业务代码直调时同样先弹确认
const origOpen = window.open.bind(window)
window.open = function (url, target, features) {
  if (!bypass) {
    const href = isExternal(typeof url === 'string' ? url : url?.href || '')
    if (href) {
      askPermission(href)
      return null
    }
  }
  return origOpen(url, target, features)
}

onMounted(() => {
  document.addEventListener('click', onClick, true)
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  document.removeEventListener('click', onClick, true)
  document.removeEventListener('keydown', onKeydown)
  window.open = origOpen
})
</script>

<style scoped>
.portal-panel {
  position: relative;
  background: linear-gradient(160deg, rgba(14, 12, 34, 0.96), rgba(24, 16, 46, 0.96));
  border: 1px solid color-mix(in srgb, var(--color-accent, #8b7cf6) 45%, transparent);
  border-radius: 20px;
  box-shadow: 0 0 40px color-mix(in srgb, var(--color-glow, #6d5dfc) 35%, transparent),
              inset 0 0 30px rgba(109, 93, 252, 0.08);
  animation: portal-in 0.28s cubic-bezier(0.22, 1.4, 0.36, 1);
}

@keyframes portal-in {
  from { opacity: 0; transform: translateY(14px) scale(0.94); }
  to { opacity: 1; transform: none; }
}

.rune-ring {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 2px dashed color-mix(in srgb, var(--color-accent, #8b7cf6) 60%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
}

.rune-spin {
  position: absolute;
  inset: -9px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-accent, #8b7cf6);
  font-size: 13px;
  animation: rune-rotate 6s linear infinite;
}

.rune-core {
  font-size: 26px;
  filter: drop-shadow(0 0 8px var(--color-glow, #6d5dfc));
  animation: rune-pulse 2.2s ease-in-out infinite;
}

@keyframes rune-rotate {
  to { transform: rotate(360deg); }
}

@keyframes rune-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.12); }
}

.btn-confirm {
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary, #00d4ff), var(--color-accent, #8b7cf6));
  box-shadow: 0 4px 18px color-mix(in srgb, var(--color-glow, #6d5dfc) 45%, transparent);
  transition: transform 0.15s ease, box-shadow 0.2s ease;
}

.btn-confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px color-mix(in srgb, var(--color-glow, #6d5dfc) 60%, transparent);
}

.btn-confirm:active {
  transform: scale(0.96);
}
</style>

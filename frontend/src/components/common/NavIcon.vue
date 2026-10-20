<template>
  <div class="nav-icon" :style="boxStyle">
    <img v-if="src" :src="src" :alt="name" loading="lazy" referrerpolicy="no-referrer" @error="onError" />
    <span v-else class="nav-icon-letter">{{ letter }}</span>
  </div>
</template>

<script setup>
// ====================================================
// NavIcon —— 导航条目统一图标
// 取图顺序：iconUrl（后台抓取并持久化） → 官网 /favicon.ico → 首字母渐变块。
// 逐级 onerror 回退，保证任何情况下都不会出现空白图标。
// ====================================================
import { computed, ref, watch } from 'vue'

const props = defineProps({
  /** 名称，用于首字母回退与 alt */
  name: { type: String, default: '' },
  /** 官网地址，用于自动推导 favicon 地址 */
  url: { type: String, default: '' },
  /** 后台维护的图标地址，优先级最高 */
  iconUrl: { type: String, default: '' },
  /** 尺寸（px） */
  size: { type: Number, default: 34 },
  /** 圆角 */
  radius: { type: String, default: '10px' },
  /** 首字母块的主色（渐变基色） */
  color: { type: String, default: '#7c3aed' }
})

// 从官网地址推导同域 favicon 地址（仅 http/https）
function originFavicon(url) {
  try {
    const u = new URL(url)
    if (u.protocol !== 'http:' && u.protocol !== 'https:') return ''
    return `${u.origin}/favicon.ico`
  } catch {
    return ''
  }
}

// 站外图标一律经后端同源代理抓取：浏览器不再直连第三方站点，
// 从根源消除 ORB 拦截、证书异常、连接重置等控制台报错；
// 代理抓取失败时后端返回首字母色块，因此 <img> 几乎不会加载失败。
const API_BASE = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '')

function proxyIcon(target, letter, color) {
  const q = new URLSearchParams({ url: target, letter, color })
  return `${API_BASE}/blog/nav/icon?${q.toString()}`
}

// 候选图标地址列表，按优先级排列
const candidates = computed(() => {
  const list = []
  const icon = (props.iconUrl || '').trim()
  if (icon.startsWith('data:image/')) {
    // 内联图标本身就是同源数据，无需代理
    list.push(icon)
  } else if (/^https?:\/\//i.test(icon)) {
    list.push(proxyIcon(icon, letter.value, props.color))
  }
  const fav = originFavicon(props.url)
  if (fav && fav !== icon) list.push(proxyIcon(fav, letter.value, props.color))
  return list
})

// 当前尝试的下标；越界即表示全部失败，回退首字母
const stage = ref(0)

// 当前实际使用的图标地址（为空则展示首字母）
const src = computed(() => candidates.value[stage.value] || '')

// 首字母（取名称首个非空字符，大写）
const letter = computed(() => {
  const s = (props.name || '').trim()
  return s ? s[0].toUpperCase() : '?'
})

const boxStyle = computed(() => ({
  width: props.size + 'px',
  height: props.size + 'px',
  borderRadius: props.radius,
  fontSize: Math.round(props.size * 0.42) + 'px',
  '--nav-icon-color': props.color
}))

// 图片加载失败：切换到下一个候选
function onError() {
  stage.value += 1
}

// 条目数据变化（如搜索过滤重建 DOM）时重置回退状态
watch(() => [props.iconUrl, props.url, props.name], () => { stage.value = 0 })
</script>

<style scoped>
.nav-icon {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.nav-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.nav-icon-letter {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 1em;
  color: #fff;
  background: linear-gradient(135deg, var(--nav-icon-color) 0%, rgba(0, 0, 0, 0.45) 100%);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
}
</style>

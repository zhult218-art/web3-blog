<template>
  <div class="jobboard-bg min-h-screen relative overflow-hidden">
    <!-- 背景光晕 -->
    <div class="absolute top-0 left-1/4 w-[500px] h-[500px] rounded-full bg-cyan-500/[0.05] blur-[120px] pointer-events-none"></div>
    <div class="absolute bottom-0 right-1/4 w-[400px] h-[400px] rounded-full bg-fuchsia-500/[0.05] blur-[100px] pointer-events-none"></div>
    <div class="absolute top-1/3 right-0 w-[450px] h-[450px] rounded-full bg-violet-500/[0.05] blur-[110px] pointer-events-none"></div>

    <div class="relative z-10 mx-auto max-w-7xl px-4 md:px-6 py-10">
      <!-- 返回 -->
      <div class="mb-5"><PageBack label="返回首页" to="/" /></div>

      <!-- ═════ Hero ═════ -->
      <div ref="heroBox" class="text-center mb-8 opacity-0">
        <div class="inline-flex items-center mb-3 px-3 py-1 rounded-full border border-cyan-400/30 bg-cyan-400/[0.05] text-[11px] text-cyan-300 font-bold tracking-wider">
          <span class="inline-block w-1.5 h-1.5 rounded-full bg-cyan-400 animate-pulse mr-1.5"></span>
          ADVENTURERS GUILD · 招募亭
        </div>
        <h1 class="text-4xl md:text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-cyan-200 via-violet-200 to-fuchsia-200 mb-2 leading-tight">
          冒险者招募亭
        </h1>
        <p class="text-slate-400 text-sm">各大公会与势力招募人才 · 点击直达官网投递简历</p>

        <!-- 统计仪表盘 -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 max-w-3xl mx-auto mt-6">
          <div class="stat-mini">
            <div class="text-xl mb-1 opacity-80">🗂️</div>
            <p class="text-lg font-black text-cyan-300">{{ totalCount }}</p>
            <p class="text-[10px] text-slate-500 uppercase tracking-wider">收录单位</p>
          </div>
          <div class="stat-mini">
            <div class="text-xl mb-1 opacity-80">🏷️</div>
            <p class="text-lg font-black text-violet-300">{{ catCount }}</p>
            <p class="text-[10px] text-slate-500 uppercase tracking-wider">分类数量</p>
          </div>
          <div class="stat-mini">
            <div class="text-xl mb-1 opacity-80">⚡</div>
            <p class="text-lg font-black text-fuchsia-300">即时</p>
            <p class="text-[10px] text-slate-500 uppercase tracking-wider">实时搜索</p>
          </div>
          <div class="stat-mini">
            <div class="text-xl mb-1 opacity-80">🔗</div>
            <p class="text-lg font-black text-emerald-300">官方</p>
            <p class="text-[10px] text-slate-500 uppercase tracking-wider">直达官网</p>
          </div>
        </div>

        <!-- 分类分布条 -->
        <div class="max-w-3xl mx-auto mt-4 flex h-1.5 rounded-full overflow-hidden bg-white/[0.04]">
          <div v-for="c in categories.filter(x => x.key !== 'all')" :key="c.key"
            :style="{ width: (c.items.length / totalCount * 100) + '%', background: catColor(c.key).grad }"
            :title="c.name + ' ' + c.items.length" class="dist-seg"></div>
        </div>
        <div class="max-w-3xl mx-auto mt-2 flex flex-wrap gap-x-3 gap-y-1 justify-center text-[10px] text-slate-500">
          <span v-for="c in categories.filter(x => x.key !== 'all')" :key="c.key" class="inline-flex items-center gap-1">
            <span class="w-2 h-2 rounded-sm" :style="{ background: catColor(c.key).color }"></span>{{ c.name }}
          </span>
        </div>
      </div>

      <!-- ═════ 热门直投 ═════ -->
      <div v-if="!search" class="mb-8">
        <h2 class="section-title mb-3 flex items-center gap-2">
          <span class="text-base">🔥</span><span>热门直投</span>
          <span class="flex-1 h-px bg-gradient-to-r from-white/[0.1] to-transparent ml-2"></span>
        </h2>
        <div class="flex flex-wrap gap-2">
          <a v-for="item in hotPicks" :key="item.url" :href="item.url" target="_blank" rel="noopener"
            class="hot-chip" :style="{ '--c': item.color }">
            <NavIcon class="hot-dot" :name="item.name" :url="item.url" :size="22" radius="9999px" :color="item.color" />
            <span class="hot-name">{{ item.name }}</span>
            <span class="hot-arrow">→</span>
          </a>
        </div>
      </div>

      <!-- ═════ Sticky 搜索 + 分类栏 ═════ -->
      <div class="sticky-bar mb-8">
        <div class="sticky-inner">
          <!-- 搜索 -->
          <div class="relative max-w-xl mx-auto">
            <input v-model="search" type="text" placeholder="搜索企业名称或关键词..." class="isekai-input w-full pl-10 pr-10" />
            <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-4.35-4.35M16 11a5 5 0 11-10 0 5 5 0 0110 0z" />
            </svg>
            <button v-if="search" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 text-lg leading-none" @click="search = ''">×</button>
          </div>
          <!-- 分类 -->
          <div class="flex flex-wrap gap-2 justify-center mt-3">
            <button v-for="c in categories" :key="c.key" @click="activeCat = c.key"
              :class="['cat-tab', activeCat === c.key ? 'cat-tab-active' : '']"
              :style="activeCat === c.key && c.key !== 'all' ? { '--accent': catColor(c.key).color, '--accent-bg': catColor(c.key).bg, '--accent-grad': catColor(c.key).grad } : {}">
              <span class="text-sm">{{ c.icon }}</span>
              <span>{{ c.name }}</span>
              <span class="text-[10px] opacity-60 ml-0.5">{{ countByCat(c.key) }}</span>
            </button>
          </div>
          <!-- 搜索结果计数 -->
          <div v-if="search" class="text-center mt-2 text-xs text-slate-500">
            搜索「<span class="text-cyan-300 font-medium">{{ search }}</span>」找到 <span class="text-cyan-300 font-bold">{{ searchResultCount }}</span> 家 ·
            <button class="text-slate-400 hover:text-slate-200 underline underline-offset-2" @click="search = ''">清除</button>
          </div>
        </div>
      </div>

      <!-- ═════ 列表 ═════ -->
      <template v-if="search">
        <!-- 搜索结果：按分类聚合 -->
        <div v-for="c in categories.filter(x => x.key !== 'all' && filterCompanies(x.items).length)" :key="c.key" class="mb-10">
          <h2 class="section-title mb-4 flex items-center gap-2">
            <span class="text-base">{{ c.icon }}</span>
            <span>{{ c.name }}</span>
            <span class="text-xs text-cyan-300">{{ filterCompanies(c.items).length }}</span>
            <span class="flex-1 h-px bg-gradient-to-r from-white/[0.1] to-transparent ml-2"></span>
          </h2>
          <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            <a v-for="item in filterCompanies(c.items)" :key="item.url" :href="item.url" target="_blank" rel="noopener" v-tilt
              class="company-card" :style="{ '--c1': catColor(c.key).c1, '--c2': catColor(c.key).c2, '--cc': catColor(c.key).color }"
              @mouseenter="showTip($event, item, c)" @mouseleave="hideTip" @focus="showTip($event, item, c)" @blur="hideTip">
              <NavIcon class="company-icon" :name="item.name" :url="item.url" :icon-url="item.iconUrl || ''" :size="38" radius="10px" :color="catColor(c.key).color" />
              <p class="company-name" v-html="highlight(item.name)"></p>
              <p class="company-tag" v-if="item.tag" v-html="highlight(item.tag)"></p>
              <p class="company-desc" v-if="item.description">{{ item.description }}</p>
              <div class="company-go">↗</div>
            </a>
          </div>
        </div>
        <!-- 搜索无结果：统一 EmptyState 组件 -->
        <EmptyState
          v-if="searchResultCount === 0"
          :text="`未找到匹配「${search}」的企业 · 试试搜索：阿里、华为、国家电网、谷歌...`"
        >
          <template #icon><span class="text-2xl">🔍</span></template>
        </EmptyState>
      </template>

      <template v-else>
        <!-- 全部 -->
        <template v-if="activeCat === 'all'">
          <div v-for="c in categories.filter(x => x.key !== 'all')" :key="c.key" class="mb-10">
            <h2 class="section-title mb-4 flex items-center gap-2">
              <span class="text-base">{{ c.icon }}</span>
              <span>{{ c.name }}</span>
              <span class="text-xs text-cyan-300">{{ c.items.length }}</span>
              <span class="flex-1 h-px bg-gradient-to-r from-white/[0.1] to-transparent ml-2"></span>
            </h2>
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
              <a v-for="item in c.items" :key="item.url" :href="item.url" target="_blank" rel="noopener" v-tilt
                class="company-card" :style="{ '--c1': catColor(c.key).c1, '--c2': catColor(c.key).c2, '--cc': catColor(c.key).color }"
                @mouseenter="showTip($event, item, c)" @mouseleave="hideTip" @focus="showTip($event, item, c)" @blur="hideTip">
                <NavIcon class="company-icon" :name="item.name" :url="item.url" :icon-url="item.iconUrl || ''" :size="38" radius="10px" :color="catColor(c.key).color" />
                <p class="company-name">{{ item.name }}</p>
                <p class="company-tag" v-if="item.tag">{{ item.tag }}</p>
                <p class="company-desc" v-if="item.description">{{ item.description }}</p>
                <div class="company-go">↗</div>
              </a>
            </div>
          </div>
        </template>

        <!-- 单个分类 -->
        <template v-else>
          <div v-for="c in categories.filter(x => x.key === activeCat)" :key="c.key" class="mb-10">
            <h2 class="section-title mb-4 flex items-center gap-2">
              <span class="text-base">{{ c.icon }}</span>
              <span>{{ c.name }}</span>
              <span class="text-xs text-cyan-300">{{ c.items.length }}</span>
              <span class="flex-1 h-px bg-gradient-to-r from-white/[0.1] to-transparent ml-2"></span>
            </h2>
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
              <a v-for="item in c.items" :key="item.url" :href="item.url" target="_blank" rel="noopener" v-tilt
                class="company-card" :style="{ '--c1': catColor(c.key).c1, '--c2': catColor(c.key).c2, '--cc': catColor(c.key).color }"
                @mouseenter="showTip($event, item, c)" @mouseleave="hideTip" @focus="showTip($event, item, c)" @blur="hideTip">
                <NavIcon class="company-icon" :name="item.name" :url="item.url" :icon-url="item.iconUrl || ''" :size="38" radius="10px" :color="catColor(c.key).color" />
                <p class="company-name">{{ item.name }}</p>
                <p class="company-tag" v-if="item.tag">{{ item.tag }}</p>
                <p class="company-desc" v-if="item.description">{{ item.description }}</p>
                <div class="company-go">↗</div>
              </a>
            </div>
          </div>
        </template>
      </template>

      <!-- 免责声明 -->
      <div class="mt-12 p-5 rounded-2xl border border-amber-400/15 bg-amber-400/[0.03] text-center">
        <p class="text-[11px] text-slate-500 leading-relaxed">
          ⚠️ 链接均指向企业官方招聘网站，本站不收集任何个人信息，投递请以官网为准。<br/>
          如发现链接失效或希望补充企业，请联系站点管理员。
        </p>
      </div>
    </div>

    <!-- 悬停详情浮层（Teleport 到 body，避免被页面容器裁剪） -->
    <Teleport to="body">
      <transition name="tip-fade">
        <div v-if="tip.show" class="nav-tip" :class="'place-' + tip.place" :style="tipStyle">
          <div class="nav-tip-top">
            <span v-if="tip.cat" class="nav-tip-cat">{{ tip.cat }}</span>
            <span v-if="tip.tag" class="nav-tip-tag">{{ tip.tag }}</span>
          </div>
          <div class="nav-tip-name">{{ tip.name }}</div>
          <p class="nav-tip-desc">{{ tip.desc }}</p>
          <div v-if="tip.host" class="nav-tip-link">{{ tip.host }} ↗</div>
        </div>
      </transition>
    </Teleport>

    <!-- 回到顶部 -->
    <transition name="fade">
      <button v-if="showTop" class="back-top" @click="scrollTop" title="回到顶部">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"/></svg>
      </button>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { animate, stagger } from 'animejs'
import PageBack from '@/components/PageBack.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import NavIcon from '@/components/common/NavIcon.vue'
import navData from '@/data/navData.json'
import { getNavPage } from '@/api/nav'

defineOptions({ name: 'JobBoard' })

const search = ref('')
const activeCat = ref('all')
const heroBox = ref(null)
const showTop = ref(false)

// ════════════════════════════════════════════════════════════
// 企业数据：优先从后端接口读取（MySQL 持久化，跨设备/跨浏览器一致），
// 接口不可用时回退到内置数据 src/data/navData.json，保证页面不白屏
// ════════════════════════════════════════════════════════════

const FALLBACK = navData.jobs

// 页面数据（分类元信息 + 条目）
const pageData = ref({
  categories: normalizeCats(FALLBACK.categories),
  items: FALLBACK.items
})

// 拉取远端数据；失败静默保留内置兜底数据
async function loadPage() {
  try {
    const res = await getNavPage('jobs')
    const payload = res && res.data
    if (payload && Array.isArray(payload.items) && payload.items.length) {
      pageData.value = {
        categories: normalizeCats(payload.categories),
        items: payload.items
      }
    }
  } catch { /* 静默回退内置数据 */ }
}

// 分类视图：把分类元信息与条目关联，并补充「全部」分组
const categories = computed(() => {
  const items = pageData.value.items || []
  return [
    { key: 'all', name: '全部', icon: '🌐', desc: '', items: [] },
    ...(pageData.value.categories || []).map(m => ({ ...m, items: items.filter(i => i.cat === m.key) }))
  ]
})

// 统一分类结构：接口与内置数据都用 description 字段，视图统一用 desc
function normalizeCats(list) {
  return (list || []).map(c => ({ key: c.key, name: c.name, icon: c.icon, desc: c.description || '' }))
}

// 悬停详情浮层状态
const TIP_WIDTH = 288
const tip = ref({ show: false, x: 0, y: 0, place: 'top', name: '', desc: '', tag: '', cat: '', host: '', color: '#22d3ee' })
const tipStyle = computed(() => {
  const half = TIP_WIDTH / 2
  const maxX = Math.max((window.innerWidth || 1024) - half - 12, half + 12)
  return {
    left: Math.min(Math.max(tip.value.x, half + 12), maxX) + 'px',
    top: tip.value.y + 'px',
    width: TIP_WIDTH + 'px',
    '--tc': tip.value.color
  }
})

// 鼠标进入卡片：在卡片上方或下方弹出详细介绍
function showTip(e, item, cat) {
  const el = e.currentTarget
  if (!el || !el.getBoundingClientRect) return
  const rect = el.getBoundingClientRect()
  const place = rect.top > 210 ? 'top' : 'bottom'
  let host = ''
  try { host = new URL(item.url).host } catch { /* 忽略非法链接 */ }
  tip.value = {
    show: true,
    x: rect.left + rect.width / 2,
    y: place === 'top' ? rect.top - 12 : rect.bottom + 12,
    place,
    name: item.name,
    desc: item.description || '暂无详细介绍',
    tag: item.tag || '',
    cat: (cat && cat.name) || '',
    host,
    color: catColor(cat ? cat.key : '').color
  }
}
function hideTip() { if (tip.value.show) tip.value.show = false }

const totalCount = computed(() => pageData.value.items.length)
const catCount = computed(() => (pageData.value.categories || []).length)

// 分类主题色映射（每个分类独立配色，增强视觉区分度）
const catColorMap = {
  central: { color: '#fbbf24', bg: 'rgba(251,191,36,0.12)', c1: 'rgba(251,191,36,0.2)', c2: 'rgba(251,146,60,0.2)', grad: 'linear-gradient(90deg,#fbbf24,#f59e0b)' },
  tech:    { color: '#22d3ee', bg: 'rgba(34,211,238,0.12)', c1: 'rgba(34,211,238,0.2)', c2: 'rgba(167,139,250,0.2)', grad: 'linear-gradient(90deg,#22d3ee,#a855f7)' },
  foreign: { color: '#60a5fa', bg: 'rgba(96,165,250,0.12)', c1: 'rgba(96,165,250,0.2)', c2: 'rgba(59,130,246,0.2)', grad: 'linear-gradient(90deg,#60a5fa,#3b82f6)' },
  bank:    { color: '#34d399', bg: 'rgba(52,211,153,0.12)', c1: 'rgba(52,211,153,0.2)', c2: 'rgba(16,185,129,0.2)', grad: 'linear-gradient(90deg,#34d399,#10b981)' },
  inst:    { color: '#f472b6', bg: 'rgba(244,114,182,0.12)', c1: 'rgba(244,114,182,0.2)', c2: 'rgba(236,72,153,0.2)', grad: 'linear-gradient(90deg,#f472b6,#ec4899)' },
  defense: { color: '#f87171', bg: 'rgba(248,113,113,0.12)', c1: 'rgba(248,113,113,0.2)', c2: 'rgba(239,68,68,0.2)', grad: 'linear-gradient(90deg,#f87171,#ef4444)' },
  auto:    { color: '#a78bfa', bg: 'rgba(167,139,250,0.12)', c1: 'rgba(167,139,250,0.2)', c2: 'rgba(139,92,246,0.2)', grad: 'linear-gradient(90deg,#a78bfa,#8b5cf6)' },
  bio:     { color: '#5eead4', bg: 'rgba(94,234,212,0.12)', c1: 'rgba(94,234,212,0.2)', c2: 'rgba(45,212,191,0.2)', grad: 'linear-gradient(90deg,#5eead4,#2dd4bf)' },
}
function catColor(key) { return catColorMap[key] || catColorMap.tech }

// 热门直投快捷入口
const hotPicks = [
  { name: '阿里巴巴', url: 'https://talent.alibaba.com/', color: '#fb923c' },
  { name: '华为', url: 'https://career.huawei.com/', color: '#22d3ee' },
  { name: '国家电网', url: 'https://zhaopin.sgcc.com.cn/', color: '#fbbf24' },
  { name: '腾讯', url: 'https://careers.tencent.com/', color: '#60a5fa' },
  { name: '字节跳动', url: 'https://jobs.bytedance.com/', color: '#a855f7' },
  { name: '比亚迪', url: 'https://job.byd.com/', color: '#34d399' },
  { name: '工商银行', url: 'https://job.icbc.com.cn/', color: '#f472b6' },
  { name: '中国航天', url: 'https://www.spacechina.com/', color: '#f87171' },
]

function countByCat(key) {
  if (key === 'all') return totalCount.value
  const c = categories.value.find(x => x.key === key)
  return c ? c.items.length : 0
}

function filterCompanies(items) {
  if (!search.value.trim()) return items
  const kw = search.value.trim().toLowerCase()
  return items.filter(i =>
    i.name.toLowerCase().includes(kw) ||
    (i.tag && i.tag.toLowerCase().includes(kw)) ||
    (i.description && i.description.toLowerCase().includes(kw))
  )
}

const searchResultCount = computed(() =>
  categories.value.reduce((sum, c) => sum + (c.key !== 'all' ? filterCompanies(c.items).length : 0), 0)
)

// 关键词高亮（搜索结果中标记匹配文字；v-html 输出前先做 HTML 转义，防数据源注入）
function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
}
function highlight(text) {
  if (!text) return ''
  const kw = search.value.trim()
  if (!kw) return escapeHtml(text)
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escapeHtml(text).replace(new RegExp(escaped, 'gi'), m => `<span class="hl">${m}</span>`)
}

// 滚动监听 + 回到顶部
function onScroll() {
  showTop.value = window.scrollY > 600
  hideTip()
}
function scrollTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 卡片入场动画（分类切换/搜索后触发）
let animTimer = null
function animateCards() {
  clearTimeout(animTimer)
  animTimer = setTimeout(() => {
    nextTick(() => {
      const cards = document.querySelectorAll('.company-card')
      if (cards.length && cards.length <= 200) {
        // 大列表只动画前若干张避免性能问题
        animate(cards, {
          opacity: [0, 1], translateY: [12, 0], scale: [0.96, 1],
          duration: 350, ease: 'outCubic', delay: stagger(25, { start: 50 })
        })
      }
    })
  }, 50)
}

watch([search, activeCat], () => {
  // 先重置卡片可见性，让动画能从 0 渐入
  document.querySelectorAll('.company-card').forEach(el => { el.style.opacity = '0' })
  animateCards()
})

onMounted(async () => {
  await nextTick()
  animate(heroBox.value, { opacity: [0, 1], translateY: [20, 0], duration: 800, ease: 'outCubic' })
  setTimeout(animateCards, 300)
  window.addEventListener('scroll', onScroll, { passive: true })
  loadPage()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  clearTimeout(animTimer)
})
</script>

<style scoped>
.jobboard-bg {
  background: radial-gradient(ellipse at top, #0a0a2e 0%, #050510 100%);
  color: #e2e8f0;
}

.section-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #cbd5e1;
  letter-spacing: 0.02em;
}

/* ───── 统计迷你卡 ───── */
.stat-mini {
  position: relative;
  padding: 0.85rem 0.5rem;
  border-radius: 0.85rem;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: linear-gradient(135deg, rgba(12, 12, 36, 0.7) 0%, rgba(18, 16, 48, 0.7) 100%);
  text-align: center;
  transition: all 0.25s ease;
  overflow: hidden;
}
.stat-mini::after {
  content: '';
  position: absolute;
  top: -1rem; right: -1rem;
  width: 3rem; height: 3rem;
  border-radius: 9999px;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.18), transparent 70%);
  filter: blur(8px);
  opacity: 0.5;
}
.stat-mini:hover { transform: translateY(-2px); border-color: rgba(34, 211, 238, 0.25); }

/* ───── 分类分布条 ───── */
.dist-seg {
  transition: width 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  min-width: 2px;
}
.dist-seg:hover { filter: brightness(1.25); }

/* ───── 热门直投芯片 ───── */
.hot-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.75rem 0.4rem 0.4rem;
  border-radius: 9999px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  background: rgba(12, 12, 36, 0.7);
  color: #cbd5e1;
  font-size: 0.78rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.22s ease;
}
.hot-dot {
  width: 1.4rem;
  height: 1.4rem;
  border-radius: 9999px;
  box-shadow: 0 0 12px color-mix(in srgb, var(--c) 40%, transparent);
}
.hot-arrow {
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.22s ease;
  color: var(--c);
  font-weight: 700;
}
.hot-chip:hover {
  border-color: var(--c);
  background: color-mix(in srgb, var(--c) 8%, rgba(12, 12, 36, 0.9));
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px -6px color-mix(in srgb, var(--c) 45%, transparent);
}
.hot-chip:hover .hot-arrow { opacity: 1; transform: translateX(0); }

/* ───── 粘性搜索栏 ───── */
.sticky-bar {
  position: sticky;
  top: 0.5rem;
  z-index: 20;
}
.sticky-inner {
  padding: 0.85rem;
  border-radius: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(8, 8, 24, 0.85);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 8px 32px -12px rgba(0, 0, 0, 0.6);
}

/* ───── 分类标签 ───── */
.cat-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.85rem;
  border-radius: 9999px;
  font-size: 0.78rem;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(12, 12, 36, 0.6);
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s ease;
}
.cat-tab:hover {
  border-color: rgba(255, 255, 255, 0.18);
  color: #e2e8f0;
  transform: translateY(-1px);
}
.cat-tab-active {
  border-color: var(--accent, rgba(34, 211, 238, 0.5));
  background: var(--accent-bg, rgba(34, 211, 238, 0.1));
  color: var(--accent, #67e8f9);
  box-shadow: 0 0 16px color-mix(in srgb, var(--accent, #22d3ee) 25%, transparent);
}

/* ───── 企业卡片 ───── */
.company-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1rem 0.75rem;
  border-radius: 0.9rem;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: linear-gradient(135deg, rgba(12, 12, 36, 0.85) 0%, rgba(18, 16, 48, 0.85) 100%);
  text-align: center;
  text-decoration: none;
  color: #e2e8f0;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), border-color 0.25s ease, box-shadow 0.25s ease;
  overflow: hidden;
  will-change: transform;
}
.company-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--c1, rgba(34, 211, 238, 0)) 0%, var(--c2, rgba(167, 139, 250, 0)) 100%);
  opacity: 0;
  transition: opacity 0.25s ease;
  pointer-events: none;
}
.company-card::after {
  content: '';
  position: absolute;
  top: -50%; right: -50%;
  width: 100%; height: 100%;
  background: radial-gradient(circle, var(--cc, rgba(34, 211, 238, 0.2)), transparent 60%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.company-card:hover {
  transform: translateY(-4px) scale(1.02);
  border-color: var(--cc, rgba(34, 211, 238, 0.4));
  box-shadow: 0 12px 28px -10px color-mix(in srgb, var(--cc, #22d3ee) 50%, transparent);
}
.company-card:hover::before { opacity: 1; }
.company-card:hover::after { opacity: 0.5; }

.company-icon {
  margin-bottom: 0.5rem;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.company-card:hover .company-icon {
  transform: scale(1.08) rotate(-3deg);
  box-shadow: 0 0 16px color-mix(in srgb, var(--cc, #22d3ee) 40%, transparent);
}
.company-name {
  font-size: 0.82rem;
  font-weight: 600;
  color: #e2e8f0;
  line-height: 1.2;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.company-tag {
  font-size: 0.65rem;
  color: #64748b;
  margin-top: 0.25rem;
}
.company-go {
  position: absolute;
  top: 0.4rem;
  right: 0.5rem;
  font-size: 0.7rem;
  color: var(--cc, #475569);
  opacity: 0;
  transform: translate(-2px, 2px);
  transition: all 0.2s ease;
}
.company-card:hover .company-go {
  opacity: 1;
  transform: translate(0, 0);
}

/* 关键词高亮 */
:deep(.hl) {
  color: #fde047;
  background: rgba(253, 224, 71, 0.12);
  border-radius: 3px;
  padding: 0 2px;
  font-weight: 700;
}

/* ───── 输入框 ───── */
:deep(.isekai-input) {
  background: rgba(12, 12, 36, 0.7);
  border: 1px solid rgba(34, 211, 238, 0.15);
  color: #e2e8f0;
  border-radius: 0.6rem;
  padding: 0.55rem 0.85rem;
  font-size: 0.85rem;
  transition: all 0.2s ease;
}
:deep(.isekai-input:focus) {
  outline: none;
  border-color: rgba(34, 211, 238, 0.5);
  box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.1);
  background: rgba(12, 12, 36, 0.9);
}

/* ───── 回到顶部 ───── */
.back-top {
  position: fixed;
  right: 1.25rem;
  bottom: 1.25rem;
  z-index: 50;
  width: 2.6rem;
  height: 2.6rem;
  border-radius: 9999px;
  border: 1px solid rgba(34, 211, 238, 0.3);
  background: rgba(12, 12, 36, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: #67e8f9;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 8px 24px -8px rgba(34, 211, 238, 0.3);
  transition: all 0.2s ease;
}
.back-top:hover {
  transform: translateY(-2px);
  border-color: rgba(34, 211, 238, 0.6);
  box-shadow: 0 12px 28px -8px rgba(34, 211, 238, 0.5);
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(8px); }
/* ───── 卡片描述：移动端展示（桌面端走悬停浮层） ───── */
.company-desc {
  display: none;
  margin-top: 0.35rem;
  font-size: 0.62rem;
  line-height: 1.4;
  color: #64748b;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
@media (max-width: 640px) {
  .company-desc { display: -webkit-box; }
}

/* ───── 悬停详情浮层 ───── */
.nav-tip {
  position: fixed;
  z-index: 9999;
  padding: 0.7rem 0.85rem;
  border-radius: 0.8rem;
  border: 1px solid color-mix(in srgb, var(--tc, #22d3ee) 45%, transparent);
  background: rgba(8, 8, 24, 0.97);
  box-shadow: 0 18px 40px -14px rgba(0, 0, 0, 0.85), 0 0 22px -10px color-mix(in srgb, var(--tc, #22d3ee) 60%, transparent);
  transform: translate(-50%, -100%);
  pointer-events: none;
  text-align: left;
}
.nav-tip.place-bottom { transform: translate(-50%, 0); }
.nav-tip::after {
  content: '';
  position: absolute;
  left: 50%;
  width: 10px;
  height: 10px;
  background: rgba(8, 8, 24, 0.97);
  border-right: 1px solid color-mix(in srgb, var(--tc, #22d3ee) 45%, transparent);
  border-bottom: 1px solid color-mix(in srgb, var(--tc, #22d3ee) 45%, transparent);
}
.nav-tip.place-top::after { bottom: -6px; transform: translateX(-50%) rotate(45deg); }
.nav-tip.place-bottom::after { top: -6px; transform: translateX(-50%) rotate(225deg); }
.nav-tip-top {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.3rem;
}
.nav-tip-cat {
  font-size: 0.6rem;
  font-weight: 700;
  color: #05050f;
  background: var(--tc, #22d3ee);
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
}
.nav-tip-tag {
  font-size: 0.6rem;
  color: var(--tc, #22d3ee);
  opacity: 0.85;
}
.nav-tip-name {
  font-size: 0.82rem;
  font-weight: 700;
  color: #f1f5f9;
  line-height: 1.3;
}
.nav-tip-desc {
  margin: 0.3rem 0 0;
  font-size: 0.7rem;
  line-height: 1.55;
  color: #94a3b8;
}
.nav-tip-link {
  margin-top: 0.4rem;
  font-size: 0.6rem;
  color: #475569;
  word-break: break-all;
}
.tip-fade-enter-active, .tip-fade-leave-active { transition: opacity 0.18s ease; }
.tip-fade-enter-from, .tip-fade-leave-to { opacity: 0; }

</style>

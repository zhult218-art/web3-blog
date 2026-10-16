<template>
  <div>
    <!-- Index Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div v-for="idx in indexCards" :key="idx.name" class="scroll-card p-4 motions-reveal" v-reveal>
        <div class="text-[10px] text-amber-600/70 mb-1.5 font-serif">{{ idx.name }}</div>
        <div class="text-xl font-black text-amber-100 scroll-title">{{ idx.price || '--' }}</div>
        <div class="flex items-center gap-2 mt-1">
          <span :class="['text-xs font-mono', idx.change >= 0 ? 'text-red-400' : 'text-green-400']">
            {{ idx.change >= 0 ? '+' : '' }}{{ idx.change?.toFixed(2) || '0.00' }}%
          </span>
        </div>
      </div>
    </div>

    <!-- Global Indices + Market Sentiment -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
      <div class="scroll-card overflow-hidden">
        <div class="p-4 border-b border-amber-800/20">
          <h3 class="text-sm font-bold text-amber-200 font-serif">热门异动</h3>
        </div>
        <div class="p-4">
          <div v-if="globalIndices.length" class="space-y-2.5">
            <div v-for="g in globalIndices" :key="g.name"
              class="flex items-center justify-between py-2 border-b border-amber-800/10 last:border-0">
              <div>
                <span class="text-xs text-amber-100/80">{{ g.name }}</span>
                <span class="text-[10px] text-amber-700/60 ml-2">{{ g.symbol }}</span>
              </div>
              <div class="text-right">
                <div class="text-xs font-mono text-amber-100">{{ g.price?.toFixed(2) }}</div>
                <div :class="['text-[10px] font-mono', (g.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400']">
                  {{ (g.change_pct || 0) >= 0 ? '+' : '' }}{{ g.change_pct?.toFixed(2) }}%
                </div>
              </div>
            </div>
          </div>
          <div v-else class="text-xs text-amber-700/50 py-6 text-center">暂无全球市场数据</div>
        </div>
      </div>

      <div class="scroll-card overflow-hidden h-full">
        <div class="p-4 border-b border-amber-800/20">
          <h3 class="text-sm font-bold text-amber-200 font-serif">市场情绪</h3>
        </div>
        <div class="p-4">
          <div v-if="marketEmotion" class="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div v-for="(val, key) in marketEmotion" :key="key"
              class="rounded-lg bg-[#1e150a] border border-amber-800/15 p-3 min-w-0">
              <div class="flex items-center justify-between gap-2 mb-2">
                <span class="text-[10px] text-amber-600/70 uppercase tracking-wider truncate font-serif" :title="key">{{ formatEmotionKey(key) }}</span>
                <span class="text-[11px] font-mono text-right flex-shrink-0" :class="getEmotionTextColor(key)">{{ typeof val === 'number' ? val.toFixed(1) : val }}</span>
              </div>
              <div class="h-1.5 rounded-full bg-amber-800/20 overflow-hidden">
                <div class="h-full rounded-full transition-all duration-700" :class="getEmotionColor(key)"
                  :style="{ width: getEmotionWidth(key, val) + '%' }"></div>
              </div>
            </div>
          </div>
          <div v-else class="text-xs text-amber-700/50 py-6 text-center">暂无情绪数据</div>
        </div>
      </div>
    </div>
    <!-- ========== 合并自「市场热点」Tab ========== -->
    <!-- THS Sector Heat -->
    <div class="scroll-card overflow-hidden mb-5">
      <div class="p-4 border-b border-amber-800/20 flex items-center justify-between">
        <h3 class="text-sm font-bold text-amber-200 font-serif">行业板块热度 <span class="text-[10px] text-amber-400/70 ml-1">同花顺 · {{ thsSectorSource }}</span></h3>
        <button @click="emit('refresh-sectors')" class="text-[10px] px-2.5 py-1 rounded-lg bg-[#332314] text-amber-400 hover:text-amber-300 border border-amber-400/20 transition font-serif">刷新板块</button>
      </div>
      <div class="p-4">
        <div v-if="thsSectors.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
          <div v-for="(s, i) in thsSectors" :key="s.name"
            class="flex items-center gap-2.5 rounded-lg bg-[#1e150a] border border-amber-800/15 px-3 py-2 min-w-0"
            :title="s.name">
            <span class="text-[10px] text-amber-700/60 font-mono w-6 flex-shrink-0">{{ i + 1 }}</span>
            <span class="text-xs text-amber-100/80 truncate flex-1 min-w-0">{{ s.name }}</span>
            <span class="text-[10px] font-mono w-16 flex-shrink-0" :class="s.change_pct >= 0 ? 'text-red-400' : 'text-green-400'">
              {{ s.change_pct >= 0 ? '+' : '' }}{{ s.change_pct.toFixed(2) }}%
            </span>
            <div class="w-14 h-1 rounded-full bg-amber-800/20 overflow-hidden flex-shrink-0">
              <div class="h-full rounded-full" :class="s.change_pct >= 0 ? 'bg-red-400' : 'bg-green-400'"
                :style="{ width: Math.min(Math.abs(s.change_pct) * 8, 100) + '%' }"></div>
            </div>
          </div>
        </div>
        <div v-else class="text-xs text-amber-700/50 py-6 text-center">板块数据加载中或源暂不可用...</div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
      <!-- Hot Concepts -->
      <div class="scroll-card overflow-hidden">
        <div class="p-4 border-b border-amber-800/20">
          <h3 class="text-sm font-bold text-amber-200 font-serif">领涨个股 Top 10</h3>
        </div>
        <div class="p-4">
          <div v-if="hotConcepts.length" class="space-y-2">
            <div v-for="(c, i) in hotConcepts" :key="c.name + i"
              class="flex items-center justify-between py-2.5 px-3 rounded-lg bg-[#1e150a] hover:bg-[#1a1208] transition">
              <div class="flex items-center gap-3 min-w-0">
                <span :class="['text-xs font-bold w-5 text-center flex-shrink-0',
                  i < 3 ? 'text-amber-300' : 'text-amber-700/60']">{{ i + 1 }}</span>
                <span class="text-xs text-amber-100/80 truncate">{{ c.name }}</span>
              </div>
              <span :class="['text-xs font-mono flex-shrink-0', (c.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400']">
                {{ (c.change_pct || 0) >= 0 ? '+' : '' }}{{ c.change_pct?.toFixed(2) }}%
              </span>
            </div>
          </div>
          <div v-else class="text-xs text-amber-700/50 py-8 text-center">暂无领涨数据</div>
        </div>
      </div>

      <!-- Industry Ranking -->
      <div class="scroll-card overflow-hidden">
        <div class="p-4 border-b border-amber-800/20">
          <h3 class="text-sm font-bold text-amber-200 font-serif">行业板块排行</h3>
        </div>
        <div class="p-4">
          <div v-if="industryList.length" class="space-y-2">
            <div v-for="(ind, i) in industryList" :key="ind.name"
              class="flex items-center justify-between py-2.5 px-3 rounded-lg bg-[#1e150a] hover:bg-[#1a1208] transition">
              <div class="flex items-center gap-3 min-w-0">
                <span :class="['text-xs font-bold w-5 text-center flex-shrink-0',
                  i < 3 ? 'text-amber-300' : 'text-amber-700/60']">{{ i + 1 }}</span>
                <span class="text-xs text-amber-100/80 truncate">{{ ind.name }}</span>
              </div>
              <span :class="['text-xs font-mono flex-shrink-0', (ind.change_pct || 0) >= 0 ? 'text-red-400' : 'text-green-400']">
                {{ (ind.change_pct || 0) >= 0 ? '+' : '' }}{{ ind.change_pct?.toFixed(2) }}%
              </span>
            </div>
          </div>
          <div v-else class="text-xs text-amber-700/50 py-8 text-center">暂无行业板块数据</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 市场概览 Tab —— 展示型子组件（批 3 拆分第 1 块）
// 数据由父级 views/quant/index.vue 通过 props 传入；刷新动作 emit 上抛。
// 样式依赖父页面根节点的 .quant-terminal 命名空间（非 scoped 全局主题）。
defineProps({
  indexCards: { type: Array, default: () => [] },
  globalIndices: { type: Array, default: () => [] },
  marketEmotion: { type: Object, default: null },
  thsSectors: { type: Array, default: () => [] },
  thsSectorSource: { type: String, default: '' },
  hotConcepts: { type: Array, default: () => [] },
  industryList: { type: Array, default: () => [] },
})
const emit = defineEmits(['refresh-sectors'])

// 情绪指标 key 转为中文名称
function formatEmotionKey(key) {
  const map = { sentiment: '综合情绪', fear: '恐慌', greed: '贪婪', volume: '成交量比', up: '上涨家数', down: '下跌家数', limit_up: '涨停', limit_down: '跌停' }
  return map[key] || key
}

// 情绪指标对应的进度条颜色类
function getEmotionColor(key) {
  if (key === 'fear' || key === 'limit_down' || key === 'down') return 'bg-red-400'
  if (key === 'greed' || key === 'limit_up' || key === 'up') return 'bg-green-400'
  return 'bg-amber-400'
}

// 情绪指标对应的文本颜色类
function getEmotionTextColor(key) {
  if (key === 'fear' || key === 'limit_down' || key === 'down') return 'text-red-400'
  if (key === 'greed' || key === 'limit_up' || key === 'up') return 'text-green-400'
  return 'text-amber-400'
}

// 按指标类型计算情绪进度条宽度百分比
function getEmotionWidth(key, val) {
  if (typeof val !== 'number') return 50
  if (key === 'sentiment') return Math.min(Math.max(val, 0), 100)
  if (key === 'volume') return Math.min(Math.max(val * 25, 5), 100)
  return Math.min(Math.max((val / 200) * 100, 5), 100)
}
</script>

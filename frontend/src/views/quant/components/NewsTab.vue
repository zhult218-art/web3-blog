<template>
  <div class="space-y-5">
    <!-- 研报速递（全量） -->
    <div class="scroll-card overflow-hidden">
      <div class="p-4 border-b border-amber-800/20 flex items-center justify-between">
        <h3 class="text-sm font-bold text-amber-100 font-serif flex items-center gap-2">
          <span class="w-2 h-2 rounded-full bg-cyan-400"></span>
          券商研报
        </h3>
        <span class="text-[10px] text-amber-600/40">东财研报中心元数据 · 仅供研究，不构成投资建议</span>
      </div>
      <div class="p-4">
        <div v-if="reports.length" class="space-y-2">
          <div v-for="r in reports" :key="r.report_id + r.publish_date"
            class="p-3 rounded-xl bg-[#1a1208] border border-amber-800/15 hover:border-cyan-400/30 transition">
            <a :href="r.url" target="_blank" rel="noopener noreferrer"
              class="text-xs text-amber-100/90 leading-relaxed hover:text-cyan-300">{{ r.title }}</a>
            <div class="flex flex-wrap items-center gap-3 mt-2">
              <span class="text-[10px] text-amber-100/80">{{ r.stock_name }}</span>
              <span :class="['text-[10px] px-1.5 py-0.5 rounded-full', ratingClass(r.rating)]">{{ r.rating || '未评级' }}</span>
              <span class="text-[10px] text-amber-600/40">{{ r.org_name }}</span>
              <span v-if="r.author" class="text-[10px] text-amber-600/40">{{ r.author }}</span>
              <span class="text-[10px] text-amber-600/40">{{ r.publish_date }}</span>
              <span v-if="r.target_price" class="text-[10px] text-cyan-300">目标价 {{ r.target_price }}</span>
              <span v-if="r.eps_y1" class="text-[10px] text-green-400">EPS {{ r.eps_y1 }}</span>
            </div>
          </div>
        </div>
        <div v-else class="text-xs text-amber-700/40 py-8 text-center">暂无研报数据</div>
      </div>
    </div>

    <!-- 行业研报 -->
    <div v-if="industryReports.length" class="scroll-card overflow-hidden">
      <div class="p-4 border-b border-amber-800/20">
        <h3 class="text-sm font-bold text-amber-100 font-serif flex items-center gap-2">
          <span class="w-2 h-2 rounded-full bg-cyan-400"></span>
          行业研报
        </h3>
      </div>
      <div class="p-4 space-y-2">
        <div v-for="r in industryReports" :key="r.report_id + r.publish_date"
          class="p-3 rounded-xl bg-[#1a1208] border border-amber-800/15 hover:border-cyan-400/30 transition">
          <a :href="r.url" target="_blank" rel="noopener noreferrer"
            class="text-xs text-amber-100/90 leading-relaxed hover:text-cyan-300">{{ r.title }}</a>
          <div class="flex flex-wrap items-center gap-3 mt-2">
            <span :class="['text-[10px] px-1.5 py-0.5 rounded-full', ratingClass(r.rating)]">{{ r.rating || '未评级' }}</span>
            <span class="text-[10px] text-amber-600/40">{{ r.org_name }}</span>
            <span class="text-[10px] text-amber-600/40">{{ r.publish_date }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 投资资讯 -->
    <div class="scroll-card overflow-hidden">
      <div class="p-4 border-b border-amber-800/20">
        <h3 class="text-sm font-bold text-amber-100">投资资讯雷达</h3>
      </div>
      <div class="p-4">
        <div v-if="items.length" class="space-y-3">
          <div v-for="n in items" :key="n.title"
            class="p-4 rounded-xl bg-[#1a1208] border border-amber-800/15 hover:border-amber-700/25 transition cursor-pointer">
            <div class="flex items-start justify-between gap-3">
              <div class="flex-1">
                <h4 class="text-xs font-medium text-amber-100/90 leading-relaxed">{{ n.title }}</h4>
                <div class="flex items-center gap-3 mt-2">
                  <span class="text-[10px] text-amber-700/50">{{ n.source }}</span>
                  <span class="text-[10px] text-amber-700/50">{{ n.time }}</span>
                </div>
              </div>
              <span v-if="n.sentiment" :class="['text-[10px] px-2 py-0.5 rounded-full shrink-0', sentimentClass(n.sentiment)]">
                {{ sentimentText(n.sentiment) }}
              </span>
            </div>
          </div>
        </div>
        <div v-else class="text-xs text-amber-700/50 py-8 text-center">暂无新闻数据</div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 研报 / 资讯 Tab —— 展示型子组件（批 3 拆分第 3 块）
// 数据由父级 views/quant/index.vue 通过 props 传入，本组件不发起请求、不持有业务状态。
// 样式依赖父页面根节点的 .quant-terminal 命名空间（非 scoped 全局主题）。
defineProps({
  reports: { type: Array, default: () => [] },
  industryReports: { type: Array, default: () => [] },
  items: { type: Array, default: () => [] },
})

/** 评级徽章配色：买入偏红、增持偏橙、持有偏黄，其余紫色（与 A 股涨红跌绿一致） */
function ratingClass(rating) {
  if (rating?.includes('买入')) return 'bg-red-500/10 text-red-400'
  if (rating?.includes('增持')) return 'bg-orange-500/10 text-orange-400'
  if (rating?.includes('持有')) return 'bg-yellow-500/10 text-yellow-400'
  return 'bg-[#a855f7]/15 text-[#c084fc]'
}

/** 情绪徽章配色 */
function sentimentClass(sentiment) {
  if (sentiment === 'positive') return 'bg-red-500/10 text-red-400'
  if (sentiment === 'negative') return 'bg-green-500/10 text-green-400'
  return 'bg-[#332314] text-amber-500/70'
}

/** 情绪文案 */
function sentimentText(sentiment) {
  if (sentiment === 'positive') return '利好'
  if (sentiment === 'negative') return '利空'
  return '中性'
}
</script>

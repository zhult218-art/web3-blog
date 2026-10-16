<template>
  <div class="flex items-center justify-between gap-3 rounded-xl border border-white/10 bg-white/5 p-3">
    <button class="web3-btn !px-3 sm:!px-5" :disabled="page <= 1" @click="change(page - 1)">
      <span class="hidden sm:inline">上一页</span>
      <span class="sm:hidden" aria-hidden="true">‹</span>
    </button>

    <!-- 移动端：页码文本；桌面端：页码按钮组 -->
    <span class="text-xs text-gray-400 sm:hidden">第 {{ page }} / {{ totalPages }} 页</span>
    <div class="hidden items-center gap-1 sm:flex">
      <template v-for="p in visiblePages" :key="p">
        <span v-if="p === '...'" class="px-1 text-white/40">…</span>
        <button v-else
          class="min-w-[36px] rounded-lg border px-2.5 py-1.5 text-xs transition-all duration-300"
          :class="p === page
            ? 'border-purple-400/50 bg-purple-500/25 font-semibold text-white shadow-[0_0_16px_rgba(168,85,247,0.35)]'
            : 'border-white/10 text-gray-400 hover:border-white/30 hover:text-white'"
          :disabled="p === page" :aria-current="p === page ? 'page' : undefined" @click="change(p)">{{ p }}</button>
      </template>
    </div>

    <button class="web3-btn !px-3 sm:!px-5" :disabled="page >= totalPages" @click="change(page + 1)">
      <span class="hidden sm:inline">下一页</span>
      <span class="sm:hidden" aria-hidden="true">›</span>
    </button>
  </div>
</template>

<script setup>
// ============================================================
// 分页组件（Pagination）
// 上一页 / 页码 / 下一页；page 双向绑定，翻页触发 update:page 事件。
// - 当前页高亮（紫色辉光），非当前页 hover 提亮
// - 桌面端显示页码按钮组，移动端收敛为「第 x / y 页」文本
// - 大量页码时以省略号折叠，保持界面简洁
// ============================================================
import { computed } from 'vue'

const props = defineProps({
  page: Number,
  pageSize: Number,
  total: Number,
  totalPages: Number,  // 可选：直接传入总页数
  maxVisible: { type: Number, default: 5 }  // 最多显示的页码数
})
const emit = defineEmits(['update:page'])

// 总页数（优先使用 props.totalPages，否则计算）
const totalPages = computed(() => {
  if (props.totalPages) return Math.max(1, props.totalPages)
  return Math.max(1, Math.ceil((props.total || 0) / (props.pageSize || 20)))
})

// 计算可见页码（含省略号）
const visiblePages = computed(() => {
  const total = totalPages.value
  const current = props.page || 1
  const max = props.maxVisible

  // 页数少于最大显示数，全部显示
  if (total <= max) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  // 计算显示范围
  const half = Math.floor(max / 2)
  let start = Math.max(1, current - half)
  let end = Math.min(total, start + max - 1)

  // 调整起始位置
  if (end - start < max - 1) {
    start = Math.max(1, end - max + 1)
  }

  const pages = []

  // 第一页 + 省略号
  if (start > 1) {
    pages.push(1)
    if (start > 2) pages.push('...')
  }

  // 中间页码
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }

  // 省略号 + 最后一页
  if (end < total) {
    if (end < total - 1) pages.push('...')
    pages.push(total)
  }

  return pages
})

// 翻页：校验边界后通知父组件
function change(p) {
  if (p < 1 || p > totalPages.value || p === props.page) return
  emit('update:page', p)
}
</script>

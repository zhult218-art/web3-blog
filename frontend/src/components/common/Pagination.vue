<template>
  <div class="flex items-center justify-between rounded-xl border border-white/10 bg-white/5 p-3">
    <button class="web3-btn" :disabled="page <= 1" @click="change(page - 1)">上一页</button>
    
    <!-- 页码按钮 -->
    <div class="flex items-center gap-1">
      <template v-for="p in visiblePages" :key="p">
        <span v-if="p === '...'" class="text-white/50 px-1">...</span>
        <button v-else class="web3-btn px-3 py-1 min-w-[36px]" :class="{ 'opacity-50 cursor-not-allowed': p === page }"
          :disabled="p === page" @click="change(p)">{{ p }}</button>
      </template>
    </div>
    
    <button class="web3-btn" :disabled="page >= totalPages" @click="change(page + 1)">下一页</button>
  </div>
</template>

<script setup>
// ============================================================
// 分页组件（Pagination）
// 上一页 / 页码 / 下一页；page 双向绑定，翻页触发 update:page 事件
// 支持大量页码时显示省略号，保持界面简洁
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

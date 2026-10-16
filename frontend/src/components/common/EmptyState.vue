<template>
  <div class="py-16 text-center">
    <div class="mb-4 inline-flex h-14 w-14 items-center justify-center rounded-2xl border border-white/[0.06] bg-[#0e0e26]">
      <slot name="icon">
        <svg class="h-7 w-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" :d="d" />
        </svg>
      </slot>
    </div>
    <p class="text-sm text-gray-500">{{ text }}</p>
    <div v-if="$slots.action" class="mt-5"><slot name="action" /></div>
  </div>
</template>

<script setup>
// ============================================================
// 空状态组件（EmptyState）
// 统一列表页「暂无数据」占位：图标 + 文案 + 可选操作按钮
// text 为提示文案；d 为 SVG 路径（默认空盒子图标）；
// 可通过 #icon 插槽完全自定义图标，#action 插槽附加操作按钮
// ============================================================
defineProps({
  text: { type: String, default: '暂无内容' },
  d: {
    type: String,
    default:
      'M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 009.586 13H7'
  }
})
</script>

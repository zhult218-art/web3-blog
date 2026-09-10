<template>
  <div class="flex items-center justify-center" :class="extra === 'block' ? 'py-10' : ''">
    <div
      class="rounded-full border border-white/15 border-t-web3-accent animate-spin"
      :class="size === 'lg' ? 'h-12 w-12 border-4' : size === 'sm' ? 'h-4 w-4 border-2' : 'h-10 w-10 border-2'"
    />
    <span v-if="text" class="ml-3 text-xs text-gray-500">{{ text }}</span>
  </div>
</template>

<script setup>
// ============================================================
// 加载动画组件（Loading）
// 通用 loading 转圈，支持尺寸与文字：
// size: md 默认 / lg 大 / sm 小；text 可选提示文字；extra='block' 增加上下留白
// ============================================================
defineProps({
  size: { type: String, default: 'md' },
  text: { type: String, default: '' },
  extra: { type: String, default: '' }
})
</script>
<template>
  <div class="rounded-xl overflow-hidden glass-panel">
    <div v-if="hasImage" class="skeleton" :style="{ aspectRatio: imageRatio }"></div>
    <div class="p-4 space-y-3">
      <div class="skeleton h-5 w-3/4 rounded"></div>
      <div v-for="i in lines" :key="i" class="skeleton h-3.5 rounded"
        :class="{ 'w-5/6': i % 2 === 0, 'w-2/3': i % 3 === 0 }"></div>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 骨架屏卡片组件（SkeletonCard）
// 内容加载前的占位卡片，复用全局 .skeleton 微光动画
// hasImage 控制图片区域；lines 控制文本行数；imageRatio 控制图片比例
// ============================================================
defineProps({
  lines: { type: Number, default: 3 },
  hasImage: { type: Boolean, default: true },
  imageRatio: { type: String, default: '16/9' }
})
</script>

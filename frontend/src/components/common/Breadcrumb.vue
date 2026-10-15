<template>
  <nav aria-label="面包屑导航" class="flex items-center gap-1.5 text-sm text-white/50 mb-4 overflow-x-auto">
    <template v-for="(item, i) in items" :key="i">
      <span v-if="i > 0" class="text-white/30 shrink-0">/</span>
      <router-link v-if="item.to && i < items.length - 1" :to="item.to"
        class="hover:text-white/80 transition-colors whitespace-nowrap">{{ item.label }}</router-link>
      <span v-else class="text-white/70 whitespace-nowrap">{{ item.label }}</span>
    </template>
  </nav>
</template>

<script setup>
// ============================================================
// 面包屑导航组件（Breadcrumb）
// 传入 items 数组，每项含 { label, to? }
// 最后一项为当前页面（纯文本），其余为可点击链接
// ============================================================
defineProps({
  items: {
    type: Array,
    required: true,
    validator: (val) => val.every(item => item.label)
  }
})
</script>

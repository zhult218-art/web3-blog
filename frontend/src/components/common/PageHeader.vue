<template>
  <header class="mb-8">
    <!-- 面包屑：crumbs = [{ label: '首页', to: '/' }, { label: '当前页' }] -->
    <nav v-if="crumbs.length" class="mb-4 flex items-center gap-1.5 text-sm text-white/50">
      <template v-for="(c, i) in crumbs" :key="i">
        <router-link v-if="c.to" :to="c.to" class="transition-colors hover:text-white/80">{{ c.label }}</router-link>
        <span v-else class="text-white/70">{{ c.label }}</span>
        <span v-if="i < crumbs.length - 1" class="text-white/30">/</span>
      </template>
    </nav>

    <div class="flex flex-wrap items-start justify-between gap-4">
      <div class="min-w-0">
        <h1 class="text-gradient-cyber mb-2 text-3xl font-bold">{{ title }}</h1>
        <p v-if="subtitle" class="matrix-text text-sm text-gray-500">{{ subtitle }}</p>
      </div>
      <!-- 右侧操作区（如：上传按钮 / 购物车入口） -->
      <div v-if="$slots.actions" class="flex items-center gap-3"><slot name="actions" /></div>
    </div>
  </header>
</template>

<script setup>
// ============================================================
// 页面头部组件（PageHeader）
// 统一内页排版语言：面包屑 + 渐变标题 + 英文副标题 + 右侧操作槽
// title 标题；subtitle 英文副标题；crumbs 面包屑数组
// 非 prop 属性（如 data-voice-target）自动透传到根元素
// ============================================================
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  crumbs: { type: Array, default: () => [] }
})
</script>

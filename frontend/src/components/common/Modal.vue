<template>
  <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
    <div class="glass-panel w-full max-w-lg p-6">
      <div class="flex items-center justify-between">
        <h3 class="text-lg font-semibold">{{ title }}</h3>
        <button class="text-gray-300" @click="close">关闭</button>
      </div>
      <div class="mt-4">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 通用弹窗组件（Modal）
// 通过 v-model 控制显示；title 为标题；内容由默认插槽提供
// ============================================================
defineProps({ modelValue: Boolean, title: String })
const emit = defineEmits(['update:modelValue'])
// 关闭弹窗：向父组件发出 v-model 更新
function close() { emit('update:modelValue', false) }
</script>

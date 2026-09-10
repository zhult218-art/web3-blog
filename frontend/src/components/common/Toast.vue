<template>
  <div v-if="toastStore.messages.length" class="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 flex flex-col gap-2">
    <div v-for="msg in toastStore.messages" :key="msg.id"
      :class="['rounded-xl border px-4 py-2 text-sm text-white backdrop-blur-xl transition-all duration-300',
        msg.type === 'success' ? 'border-green-400/30 bg-green-500/10' :
        msg.type === 'error' ? 'border-red-400/30 bg-red-500/10' :
        msg.type === 'warning' ? 'border-yellow-400/30 bg-yellow-500/10' :
        'border-white/10 bg-white/10']">
      <span v-if="msg.type === 'success'">✓</span>
      <span v-else-if="msg.type === 'error'">✕</span>
      <span v-else-if="msg.type === 'warning'">⚠</span>
      <span v-else>ℹ</span>
      {{ msg.message }}
    </div>
  </div>
</template>

<script setup>
// ============================================================
// 全局消息提示渲染组件（Toast）
// 读取 toast store 的消息队列并居中展示在页面底部
// 按类型显示不同色系图标（成功✓ / 错误✕ / 警告⚠ / 信息ℹ）
// ============================================================
import { useToastStore } from '@/stores/modules/toast'
const toastStore = useToastStore()
</script>

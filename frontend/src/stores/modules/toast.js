// ============================================================
// 全局消息提示 Store（Pinia）
// 维护消息队列，由 components/common/Toast.vue 渲染
// 全站统一通过 toast.success / error / warning / info 调用
// ============================================================
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const messages = ref([])
  let idCounter = 0

  // 展示一条消息，duration 毫秒后自动移除
  function show(message, type = 'info', duration = 2500) {
    const id = ++idCounter
    messages.value.push({ id, message, type })
    setTimeout(() => {
      messages.value = messages.value.filter(m => m.id !== id)
    }, duration)
  }

  // 以下为不同类型消息的便捷方法
  function success(msg) { show(msg, 'success') }
  function error(msg) { show(msg, 'error') }
  function warning(msg) { show(msg, 'warning') }
  function info(msg) { show(msg, 'info') }

  return { messages, show, success, error, warning, info }
})

<template>
  <!-- 全局统一返回按钮：
       to 指定时跳转到目标路由（可读性最好）；
       未指定时优先返回上一页，无历史则回首页 -->
  <button class="page-back" type="button" @click="go">
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
    <span>{{ label }}</span>
  </button>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  label: { type: String, default: '返回' },
  to: { type: [String, Object], default: null }
})

const router = useRouter()

function go() {
  if (props.to) {
    router.push(__props.to)
    return
  }
  if (window.history.length > 1) router.back()
  else router.push('/')
}
</script>
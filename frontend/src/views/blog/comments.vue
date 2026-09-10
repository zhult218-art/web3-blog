<template>
  <div class="space-y-6">
    <PageBack label="返回博客" to="/blog" />
    <h1 class="text-2xl font-black text-white">留言板</h1>
    <p class="text-xs text-gray-500">在这里留下你想说的话（匿名需填写昵称，登录后可正常参与）</p>
    <CommentPanel target-id="0" target-type="GUESTBOOK" />
  </div>
</template>

<script setup>
import CommentPanel from '@/components/blog/CommentPanel.vue'
import PageBack from '@/components/PageBack.vue'
</script>
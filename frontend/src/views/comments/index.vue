<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-3xl">
      <h1 class="text-3xl font-bold text-gradient-cyber mb-2">留言板</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">Comments · 欢迎留言交流</p>

      <div class="glass-panel p-6 mb-8">
        <h2 class="text-lg font-semibold text-white mb-4">发表留言</h2>
        <div class="space-y-4">
          <div>
            <label class="text-xs text-gray-400">昵称</label>
            <input v-model="form.nickname" class="web3-input mt-1" placeholder="你的昵称" />
          </div>
          <div>
            <label class="text-xs text-gray-400">邮箱</label>
            <input v-model="form.email" class="web3-input mt-1" placeholder="your@email.com" />
          </div>
          <div>
            <label class="text-xs text-gray-400">内容</label>
            <textarea v-model="form.content" class="web3-input mt-1 h-32" placeholder="说点什么..." />
          </div>
          <button class="web3-btn w-full" @click="submit" :disabled="submitting">{{ submitting ? '提交中...' : '提交留言' }}</button>
        </div>
      </div>

      <div class="space-y-4">
        <div v-for="c in comments" :key="c.id" class="glass-panel-sm p-4">
          <div class="flex items-center gap-3 mb-2">
            <div class="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-cyan-500 flex items-center justify-center text-xs font-bold">
              {{ c.nickname?.[0] || 'U' }}
            </div>
            <div>
              <p class="text-sm text-white">{{ c.nickname }}</p>
              <p class="text-[10px] text-gray-500">{{ c.date }}</p>
            </div>
          </div>
          <p class="text-sm text-gray-300 pl-11">{{ c.content }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 留言板页：展示示例留言并支持本地提交新留言
// ====================================================
import { ref } from 'vue'
import { useToastStore } from '@/stores/modules/toast'

const toast = useToastStore()
const form = ref({ nickname: '', email: '', content: '' })
const submitting = ref(false)

// 示例留言数据
const comments = ref([
  { id: 1, nickname: '访客A', date: '2026-06-20', content: '博客内容很详细，学到了很多网络知识，感谢分享！' },
  { id: 2, nickname: 'CoderX', date: '2026-06-18', content: '能不能多出一些关于量化交易的实战文章？' },
  { id: 3, nickname: 'NetSec', date: '2026-06-15', content: '渗透测试系列写得很系统，期待更新。' }
])

// 校验并提交新留言（本地模拟，成功后插入列表顶部）
function submit() {
  if (!form.value.nickname || !form.value.content) {
    toast.warning('请填写昵称和留言内容')
    return
  }
  submitting.value = true
  setTimeout(() => {
    comments.value.unshift({
      id: Date.now(),
      nickname: form.value.nickname,
      date: new Date().toLocaleDateString('zh-CN'),
      content: form.value.content
    })
    form.value = { nickname: '', email: '', content: '' }
    submitting.value = false
    toast.success('留言成功')
  }, 600)
}
</script>

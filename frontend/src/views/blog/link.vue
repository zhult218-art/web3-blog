<template>
  <div class="space-y-6">
    <PageBack label="返回博客" to="/blog" />
    <h1 class="text-2xl font-black text-white">友人帐</h1>

    <!-- 友链 -->
    <div class="link-grid">
      <a v-for="l in links" :key="l.id" :href="l.url" target="_blank" rel="noopener"
        class="panel p-5 group flex items-center gap-4">
        <div class="w-12 h-12 shrink-0 rounded-xl bg-gradient-to-br from-purple-500/40 to-cyan-500/40 flex items-center justify-center text-lg font-black text-white overflow-hidden">
          <img v-if="l.avatar" :src="l.avatar" :alt="l.name" class="w-full h-full object-cover" />
          <span v-else>{{ (l.name || '友')[0].toUpperCase() }}</span>
        </div>
        <div class="min-w-0">
          <div class="text-sm font-bold text-white group-hover:text-[color:var(--color-primary)] transition-colors truncate">{{ l.name }}</div>
          <div class="text-xs text-gray-500 truncate mt-0.5">{{ l.description }}</div>
        </div>
      </a>
    </div>
    <p v-if="!links.length" class="text-center text-xs text-gray-600 py-8">暂无友链，欢迎申请</p>

    <!-- 申请友链 -->
    <div class="panel p-6">
      <h3 class="text-sm font-bold text-white mb-4">申请友链</h3>
      <div class="space-y-3">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <input v-model="form.name" placeholder="站点名称 *" class="input" maxlength="40" />
          <input v-model="form.url" placeholder="站点链接 *（https://）" class="input" maxlength="200" />
        </div>
        <input v-model="form.description" placeholder="一句话描述（可选）" class="input" maxlength="100" />
        <div class="flex justify-end">
          <button class="submit-btn" :disabled="submitting || !form.name || !form.url" @click="submit">
            {{ submitting ? '提交中…' : '提交申请' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBlogLinks, applyBlogLink } from '@/api/blog'
import { useToastStore } from '@/stores/modules/toast'
import PageBack from '@/components/PageBack.vue'

const toast = useToastStore()
const links = ref([])
const submitting = ref(false)
const form = ref({ name: '', url: '', description: '' })

async function submit() {
  let url = form.value.url.trim()
  if (!/^https?:\/\//i.test(url)) url = `https://${url}`
  submitting.value = true
  try {
    await applyBlogLink({ name: form.value.name.trim(), url, description: form.value.description.trim() })
    toast.success('申请已提交，等待审核')
    form.value = { name: '', url: '', description: '' }
  } catch {
    toast.error('提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try { links.value = (await getBlogLinks()) || [] } catch {}
})
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }
.link-grid { @apply grid grid-cols-1 sm:grid-cols-2 gap-4; }
.input {
  @apply w-full px-3 py-2 rounded-lg text-sm bg-[#10102a] border border-white/[0.08]
    text-gray-200 placeholder:text-gray-600 focus:outline-none focus:border-purple-500/40 focus:bg-[#141432] transition-colors;
}
.submit-btn {
  @apply px-5 py-2 rounded-lg text-xs font-medium text-white disabled:opacity-40 disabled:cursor-not-allowed transition-all;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
}
</style>
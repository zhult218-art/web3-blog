<template>
  <article class="article-card group">
    <router-link :to="`/blog/post/${article.id}`" class="cover">
      <div v-if="coverFailed" class="aspect-video w-full flex flex-col items-center justify-center gap-1 bg-gradient-to-br from-purple-500/25 via-[rgba(10,10,40,0.6)] to-cyan-500/20 text-white/50">
        <span class="text-2xl">✦</span>
        <span class="text-[10px] tracking-widest opacity-70">{{ article.category || 'VERSE' }}</span>
      </div>
      <img
        v-else-if="article.cover"
        :src="article.cover"
        :alt="article.title"
        loading="lazy"
        referrerpolicy="no-referrer"
        @error="coverFailed = true"
        class="aspect-video w-full object-cover"
      />
      <div v-else class="aspect-video w-full flex items-center justify-center text-3xl bg-gradient-to-br from-purple-500/20 to-cyan-500/20 text-white/40">✦</div>
      <span v-if="article.category" class="badge">{{ article.category }}</span>
      <span v-if="article.isTop" class="badge badge-top">置顶</span>
    </router-link>
    <div class="body">
      <h2 class="title">
        <router-link :to="`/blog/post/${article.id}`" class="group-hover:text-[color:var(--color-primary)] transition-colors">
          {{ article.title }}
        </router-link>
      </h2>
      <p class="summary">{{ article.summary || '暂无摘要' }}</p>
      <div class="meta">
        <time>{{ formatDate(article.createdAt) }}</time>
        <span v-for="t in tagList.slice(0, 3)" :key="t" class="tag" @click.prevent.stop="goTag(t)">{{ t }}</span>
        <span v-if="!read && !isDetail" class="unread-dot" title="未读"></span>
        <span class="read-time">{{ readingMin }} 分钟阅读</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useReadMark } from '@/composables/useReadMark'

const props = defineProps({
  article: { type: Object, required: true },
  isDetail: { type: Boolean, default: false }
})

const coverFailed = ref(false)

const router = useRouter()
const { isRead } = useReadMark()
const read = computed(() => isRead(props.article.id))

const tagList = computed(() => (props.article.tags || '').split(',').map(t => t.trim()).filter(Boolean))

const readingMin = computed(() => {
  const words = props.article.content?.replace(/[#>*`_\-\[\]()!|]/g, '').length || 0
  return Math.max(1, Math.round(words / 300))
})

function formatDate(d) {
  if (!d) return ''
  return String(d).slice(0, 10)
}

function goTag(tag) {
  router.push({ path: '/blog', query: { tag } })
}
</script>

<style scoped>
.article-card {
  @apply rounded-2xl overflow-hidden border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm hover:border-white/[0.18] hover:shadow-xl hover:shadow-purple-500/10 transition-all duration-300;
}
.cover { @apply relative block overflow-hidden; }
.cover img { @apply transition-transform duration-500 group-hover:scale-105; }
.badge {
  @apply absolute top-3 left-3 px-2 py-0.5 rounded-md text-[11px] font-medium backdrop-blur-md;
  background: rgba(168, 85, 247, 0.75); color: #fff;
}
.badge-top { background: rgba(236, 72, 153, 0.8); }
.body { @apply p-4; }
.title { @apply text-base font-bold text-white leading-snug mb-1.5; }
.summary { @apply text-sm text-gray-400 leading-relaxed mb-2.5 line-clamp-2; }
.meta { @apply flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-gray-500; }
.tag { @apply cursor-pointer hover:text-[color:var(--color-primary)] transition-colors; }
.unread-dot { @apply w-1.5 h-1.5 rounded-full bg-pink-500 inline-block; }
</style>

<template>
  <article class="article-card group flex flex-col">
    <router-link :to="`/blog/post/${article.id}`" class="cover">
      <div v-if="coverFailed" class="cover-fallback">
        <span class="text-3xl">✦</span>
        <span class="text-[10px] tracking-[0.25em] opacity-70 uppercase mt-1">{{ article.category || 'Verse' }}</span>
      </div>
      <img
        v-else-if="article.cover"
        :src="article.cover"
        :alt="article.title"
        loading="lazy"
        referrerpolicy="no-referrer"
        @error="coverFailed = true"
        class="cover-img"
      />
      <div v-else class="cover-fallback">
        <span class="text-3xl">✦</span>
        <span class="text-[10px] tracking-[0.25em] opacity-70 uppercase mt-1">{{ article.category || 'Verse' }}</span>
      </div>
      <div class="cover-overlay"></div>
      <span v-if="article.category" class="badge">{{ article.category }}</span>
      <span v-if="article.isTop" class="badge badge-top">置顶</span>
      <span v-if="!read && !isDetail" class="unread-dot" title="未读"></span>
    </router-link>
    <div class="body flex-1 flex flex-col">
      <h2 class="title">
        <router-link :to="`/blog/post/${article.id}`" class="title-link">
          {{ article.title }}
        </router-link>
      </h2>
      <p class="summary">{{ article.summary || '暂无摘要' }}</p>
      <div class="meta mt-auto">
        <time class="meta-item">
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
          {{ formatDate(article.createdAt) }}
        </time>
        <span class="meta-divider"></span>
        <span class="meta-item read-time">
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          {{ readingMin }} 分钟
        </span>
        <span v-if="article.viewCount != null" class="meta-item">
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>
          {{ article.viewCount }}
        </span>
      </div>
      <div v-if="tagList.length" class="tags-row">
        <span v-for="t in tagList.slice(0, 3)" :key="t" class="tag" @click.prevent.stop="goTag(t)"># {{ t }}</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useReadMark } from '@/composables/useReadMark'
import { formatDay } from '@/utils/date'

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

// 日期格式化（统一走 utils/date）
function formatDate(d) { return formatDay(d, '') }

function goTag(tag) {
  router.push({ path: '/blog', query: { tag } })
}
</script>

<style scoped>
.article-card {
  @apply rounded-2xl overflow-hidden border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm transition-all duration-300;
}
.article-card:hover {
  @apply border-white/[0.18];
  transform: translateY(-2px);
  box-shadow: 0 10px 30px -10px rgba(168, 85, 247, 0.25), 0 4px 12px -6px rgba(6, 182, 212, 0.18);
}
.cover { @apply relative block overflow-hidden; }
.cover-img,
.cover-fallback {
  @apply aspect-video w-full object-cover transition-transform duration-500 group-hover:scale-105;
}
.cover-fallback {
  @apply flex flex-col items-center justify-center text-white/50;
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.25), rgba(10, 10, 40, 0.6) 50%, rgba(6, 182, 212, 0.2));
}
.cover-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, transparent 60%, rgba(0, 0, 0, 0.45));
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
}
.article-card:hover .cover-overlay { opacity: 1; }
.badge {
  @apply absolute top-3 left-3 px-2 py-0.5 rounded-md text-[11px] font-medium backdrop-blur-md;
  background: rgba(168, 85, 247, 0.75); color: #fff;
}
.badge-top { background: rgba(236, 72, 153, 0.85); }
.unread-dot {
  @apply absolute top-3 right-3 w-2 h-2 rounded-full;
  background: #ec4899;
  box-shadow: 0 0 6px rgba(236, 72, 153, 0.8);
}
.body { @apply p-4; }
.title { @apply text-base font-bold text-white leading-snug mb-1.5; }
.title-link {
  @apply transition-colors;
  background-image: linear-gradient(transparent 95%, var(--color-primary) 95%);
  background-size: 0% 100%;
  background-repeat: no-repeat;
  padding-bottom: 1px;
}
.article-card:hover .title-link {
  @apply text-[color:var(--color-primary)];
  background-size: 100% 100%;
}
.summary { @apply text-sm text-gray-400 leading-relaxed mb-3 line-clamp-2; }
.meta { @apply flex items-center gap-2 text-[11px] text-gray-500; }
.meta-item { @apply inline-flex items-center gap-1; }
.meta-item svg { @apply opacity-70; }
.meta-divider {
  @apply w-px h-3 bg-white/10;
}
.tags-row { @apply flex flex-wrap gap-1.5 mt-3 pt-3 border-t border-white/[0.06]; }
.tag {
  @apply cursor-pointer px-2 py-0.5 rounded text-[11px] text-gray-500
    hover:text-purple-300 hover:bg-purple-500/10 transition-colors;
}
</style>

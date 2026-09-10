<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <!-- Header / book meta -->
      <div class="mb-8">
        <button class="text-xs text-gray-500 hover:text-amber-300 transition flex items-center gap-1 mb-4" @click="router.push('/media')">
          <span class="inline-block w-0 h-0 border-y-[4px] border-y-transparent border-r-[6px] border-r-current"></span>
          返回在线媒体
        </button>
        <div class="flex flex-wrap items-end gap-x-4 gap-y-1">
          <h1 class="text-3xl md:text-4xl font-bold text-white font-serif tracking-wide">{{ book?.title }}</h1>
          <p class="text-sm text-gray-500 mb-1">
            <span v-if="book?.author">{{ book.author }}</span>
            <span v-if="book?.dynasty" class="text-amber-400/80 ml-2">{{ book.dynasty }}</span>
            <span v-if="book?.category" class="ml-2 text-xs px-2 py-0.5 rounded-full border border-white/10">{{ book.category }}</span>
          </p>
        </div>
        <p v-if="book?.description" class="text-sm text-gray-500 mt-2 max-w-2xl">{{ book.description }}</p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-[280px_1fr] gap-8 items-start">
        <!-- TOC -->
        <aside class="lg:sticky lg:top-24 glass-panel p-4">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-sm font-bold text-white">目录 <span class="text-gray-600 font-normal">共 {{ chapters.length }} 章</span></h3>
            <span class="text-[10px] text-gray-600">第 {{ currentChapter }} / {{ chapters.length }} 章</span>
          </div>

          <div class="lg:max-h-[calc(100vh-280px)] lg:overflow-y-auto space-y-1">
            <div v-for="ch in chapters" :key="ch.id" @click="goChapter(ch.chapterNo)"
              class="flex items-center justify-between gap-2 px-3 py-2 rounded-lg cursor-pointer text-[13px] transition-all duration-150"
              :class="ch.chapterNo === currentChapter ? 'bg-amber-500/15 text-amber-200 border border-amber-400/25' : 'text-gray-400 hover:text-white hover:bg-[#0e0e26] border border-transparent'">
              <span class="truncate">{{ ch.chapterTitle }}</span>
              <span class="text-[10px] text-gray-600 flex-shrink-0">{{ ch.chapterNo }}</span>
            </div>
          </div>
        </aside>

        <!-- Content -->
        <article class="glass-panel p-6 md:p-10">
          <template v-if="chapter">
            <div class="text-center mb-8">
              <p class="text-[11px] tracking-[0.4em] text-amber-400/70 mb-2">{{ book?.title }}</p>
              <h2 class="text-2xl md:text-3xl font-bold text-white font-serif">{{ chapter.chapterTitle }}</h2>
              <div class="flex items-center justify-center gap-2 mt-4 text-amber-400/40">
                <span class="h-px w-10 bg-gradient-to-r from-transparent to-amber-400/40"></span>
                <span class="text-xs">✦</span>
                <span class="h-px w-10 bg-gradient-to-l from-transparent to-amber-400/40"></span>
              </div>
            </div>

            <div class="text-[17px] leading-[2.1] text-gray-200 font-serif whitespace-pre-wrap">
              {{ chapter.content }}
            </div>

            <footer class="mt-10 pt-6 border-t border-white/5">
              <div class="flex items-center justify-between">
                <button :disabled="currentChapter <= 1" @click="goChapter(currentChapter - 1)" class="px-4 py-2 rounded-lg text-xs border border-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition hover:border-amber-400/40 hover:text-amber-200">
                  ← 上一章
                </button>
                <span class="text-[11px] text-gray-600">本章约 {{ chapter.wordCount }} 字</span>
                <button :disabled="currentChapter >= chapters.length" @click="goChapter(currentChapter + 1)" class="px-4 py-2 rounded-lg text-xs border border-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition hover:border-amber-400/40 hover:text-amber-200">
                  下一章 →
                </button>
              </div>
            </footer>
          </template>
          <div v-else class="py-20 text-center">
            <div class="text-4xl mb-3 opacity-15">📜</div>
            <p class="text-xs text-gray-600">正在加载章节…</p>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 电子书阅读页：展示书籍信息、章节列表，
// 支持章节切换与阅读进度记录
// ====================================================
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBookDetail, getBookChapters, getBookChapter } from '@/api/media'

const route = useRoute()
const router = useRouter()

const book = ref(null)
const chapters = ref([])
const chapter = ref(null)
const currentChapter = ref(1)

// 加载书籍章节列表
async function loadChapters() {
  const res = await getBookChapters(route.params.id)
  chapters.value = res.data || []
}

// 切换并加载指定章节的阅读内容
async function goChapter(chapterNo) {
  if (!chapterNo || chapterNo < 1 || chapterNo > chapters.value.length) return
  currentChapter.value = chapterNo
  try {
    const res = await getBookChapter(route.params.id, chapterNo)
    chapter.value = res.data
    chapters.value.forEach(c => { if (c.chapterNo === chapterNo) c.wordCount = chapter.value.wordCount })
  } catch (e) {
    /* 章节缺失时停留空态 */
    chapter.value = null
  }
}

onMounted(async () => {
  // 挂载时加载书籍详情、章节列表并默认打开第一章
  const res = await getBookDetail(route.params.id)
  book.value = res.data
  await loadChapters()
  const first = chapters.value[0]
  await goChapter(first ? first.chapterNo : 1)
})

watch(chapter, () => {
  window.scrollTo({ top: 0, behavior: 'auto' })
})
</script>
<template>
  <div class="relative min-h-screen">
    <ThreeBackground />
    <ParticleBackground />
    <Web3Nav />
    <ReadingProgress />
    <main class="relative z-10">
      <div class="mx-auto max-w-[1400px] px-4 md:px-6 py-8 grid grid-cols-1 xl:grid-cols-[1fr_300px] gap-6 items-start">
        <!-- 主内容 -->
        <main class="min-w-0">
          <router-view v-slot="{ Component }">
            <Suspense :timeout="0">
              <transition name="page" mode="out-in">
                <component :is="Component" v-if="Component" />
              </transition>
              <template #fallback>
                <div class="flex items-center justify-center min-h-[50vh]">
                  <div class="h-10 w-10 rounded-full border-2 border-white/15 border-t-cyan-400 animate-spin"></div>
                </div>
              </template>
            </Suspense>
          </router-view>
        </main>
        <!-- 右栏 -->
        <aside class="hidden xl:block">
          <div class="space-y-5 sticky top-24">
            <div class="panel p-5">
              <h4 class="widget-title">站点</h4>
              <div class="grid grid-cols-2 gap-2 text-xs">
                <router-link to="/blog/categories" class="link-item">📁 分类</router-link>
                <router-link to="/blog/tags" class="link-item">🏷️ 标签</router-link>
                <router-link to="/blog/archives" class="link-item">🗂️ 归档</router-link>
                <router-link to="/blog/link" class="link-item">🤝 友人帐</router-link>
                <router-link to="/blog/comments" class="link-item">💬 留言板</router-link>
                <span class="link-item cursor-pointer" @click="randomPost">🎲 随便逛逛</span>
              </div>
            </div>
            <SidebarWidgets />
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
// ============================================================
// 博客三栏布局（BlogLayout）
// 左栏 Widgets / 中栏内容流 / 右栏导航（宽屏显示）
// 顶层挂载：公告条、阅读进度条、背景、光标、导航
// 音乐播放由 App 级 GlobalPlayer 全局接管（跨页不断流）
// ============================================================
import { useRouter } from 'vue-router'
import { getBlogRandom } from '@/api/blog'
import ParticleBackground from '@/components/layout/ParticleBackground.vue'
import ThreeBackground from '@/components/layout/ThreeBackground.vue'
import Web3Nav from '@/components/layout/Web3Nav.vue'
import SidebarWidgets from '@/components/blog/SidebarWidgets.vue'
import ReadingProgress from '@/components/blog/ReadingProgress.vue'

const router = useRouter()

async function randomPost() {
  try {
    const a = await getBlogRandom()
    if (a?.id) router.push(`/blog/post/${a.id}`)
  } catch { /* 无文章时静默 */ }
}
</script>

<style scoped>
.panel { @apply rounded-2xl border border-white/[0.10] bg-[rgba(12,12,45,0.82)] backdrop-blur-sm; }
.widget-title { @apply text-sm font-bold text-white mb-2.5; }
.link-item { @apply block px-2.5 py-1.5 rounded-lg text-gray-400 hover:text-white hover:bg-[#121230] transition-colors; }
</style>

<template>
  <div class="relative min-h-screen">
    <ThreeBackground />
    <ParticleBackground />
    <Web3Nav />
    <main class="relative z-10 min-h-[calc(100vh-64px)]">
      <router-view v-slot="{ Component }">
        <Suspense :timeout="0">
          <transition name="page" mode="out-in">
            <component :is="Component" v-if="Component" />
          </transition>
          <template #fallback>
            <div class="flex items-center justify-center min-h-[calc(100vh-64px)]">
              <div class="h-10 w-10 rounded-full border-2 border-white/15 border-t-cyan-400 animate-spin"></div>
            </div>
          </template>
        </Suspense>
      </router-view>
    </main>
    <audio ref="audioEl" preload="auto" @ended="onEnded" @timeupdate="onTimeUpdate"></audio>
  </div>
</template>

<script setup>
// ============================================================
// 全局布局组件（Web3Layout）
// 全站业务页面统一挂载在此布局下：
// - 背景层：Three.js 粒子 + Canvas 连线粒子
// - 顶部导航 Web3Nav
// - 内含全局 <audio> 元素，驱动全局播放器（player store）的真实播放
// ============================================================
import { ref, watch, onMounted } from 'vue'
import { usePlayerStore } from '@/stores/modules/player'
import ParticleBackground from '@/components/layout/ParticleBackground.vue'
import ThreeBackground from '@/components/layout/ThreeBackground.vue'
import Web3Nav from '@/components/layout/Web3Nav.vue'

const player = usePlayerStore()
const audioEl = ref(null)

// 曲目切换：将播放源写入 audio 元素并触发播放
watch(() => player.currentTrack, (track) => {
  const audio = audioEl.value
  if (!audio || !track) return
  const src = track.url || track.audioUrl || track.audio_url
  if (src) {
    audio.src = src
    audio.load()
    if (player.isPlaying) audio.play().catch(() => {})
  }
})

// 播放/暂停状态同步到 audio 元素
watch(() => player.isPlaying, (playing) => {
  const audio = audioEl.value
  if (!audio) return
  if (playing) audio.play().catch(() => {})
  else audio.pause()
})

// 播放结束：通知 store 停止播放态
function onEnded() {
  player.pause()
}

// 进度回调：把 audio 的当前进度写回当前曲目，供进度条/歌词使用
function onTimeUpdate() {
  const audio = audioEl.value
  if (audio && player.currentTrack) {
    player.currentTrack._currentTime = audio.currentTime
    player.currentTrack._duration = audio.duration || 0
  }
}
</script>

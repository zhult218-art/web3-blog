<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <h1 class="text-3xl font-bold text-gradient-cyber mb-2">相册集</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">Album · 记录瞬间</p>

      <div class="columns-1 sm:columns-2 lg:columns-3 gap-4 space-y-4">
        <div v-for="(photo, i) in photos" :key="i" class="break-inside-avoid glass-panel-sm overflow-hidden group cursor-pointer hover:border-cyan-400/30 transition"
          @click="open(photo)">
          <div class="aspect-[4/3] bg-gradient-to-br" :class="photo.gradient" />
          <div class="p-3">
            <p class="text-sm text-white group-hover:text-cyan-300 transition">{{ photo.title }}</p>
            <p class="text-[10px] text-gray-500 mt-1">{{ photo.date }}</p>
          </div>
        </div>
      </div>

      <Modal v-model="showModal" :title="selected?.title || '图片预览'">
        <div v-if="selected" class="aspect-video rounded-xl bg-gradient-to-br" :class="selected.gradient" />
        <p class="text-xs text-gray-500 mt-2">{{ selected?.date }}</p>
      </Modal>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 相册页：本地照片墙浏览，点击放大预览
// ====================================================
import { ref } from 'vue'
import Modal from '@/components/common/Modal.vue'

const showModal = ref(false)
const selected = ref(null)

// 本地相册照片数据（标题/日期/渐变色）
const photos = [
  { title: '赛博朋克城市', date: '2026-06-01', gradient: 'from-purple-500/40 to-pink-500/40' },
  { title: '极光之夜', date: '2026-05-20', gradient: 'from-cyan-500/40 to-blue-500/40' },
  { title: '未来科技', date: '2026-05-15', gradient: 'from-green-500/40 to-emerald-500/40' },
  { title: '数字海洋', date: '2026-05-10', gradient: 'from-blue-500/40 to-indigo-500/40' },
  { title: '霓虹街道', date: '2026-04-28', gradient: 'from-pink-500/40 to-rose-500/40' },
  { title: '量子空间', date: '2026-04-15', gradient: 'from-violet-500/40 to-purple-500/40' }
]

// 点击照片打开大图预览弹窗
function open(photo) {
  selected.value = photo
  showModal.value = true
}
</script>

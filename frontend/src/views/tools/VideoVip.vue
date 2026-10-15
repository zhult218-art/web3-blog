<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <header class="mb-8">
        <h1 class="text-3xl font-bold text-gradient-cyber">🎬 VIP 视频解析</h1>
        <p class="text-sm text-gray-500 matrix-text mt-1">
          全网主流视频平台 VIP 内容免费解析 · 多线路智能切换 · 数据来源
          <a href="https://github.com/zhult218-art/video_vip" target="_blank" rel="noopener"
             class="text-cyan-400 hover:text-cyan-300 underline">video_vip</a>
        </p>
      </header>

      <!-- 输入区 -->
      <section class="glass-panel p-5 mb-6">
        <label class="text-xs text-gray-400 mb-2 block">视频页面链接</label>
        <div class="flex flex-col sm:flex-row gap-3">
          <input v-model="videoUrl" class="web3-input flex-1 text-sm"
            :placeholder="urlPlaceholder"
            @keyup.enter="parse" />
          <button class="web3-btn text-sm !px-6 whitespace-nowrap" :disabled="!canParse" @click="parse">
            {{ parsing ? '解析中...' : '立即解析' }}
          </button>
        </div>
        <div class="mt-3 flex flex-wrap gap-1.5">
          <span v-for="p in vipPlatforms" :key="p.host"
            class="text-[11px] px-2 py-0.5 rounded-full border border-white/10 text-gray-500 hover:text-cyan-300 hover:border-cyan-400/30 cursor-pointer transition"
            @click="videoUrl = ''">
            {{ p.icon }} {{ p.name }}
          </span>
        </div>
      </section>

      <!-- 播放区 -->
      <section v-if="playerUrl" class="glass-panel p-4 mb-6">
        <div class="flex items-center justify-between mb-3">
          <span class="text-xs text-gray-400">当前线路：<span class="text-cyan-300">{{ currentSource?.name }}</span></span>
          <button class="text-xs text-gray-500 hover:text-white" @click="playerUrl = ''">✕ 关闭</button>
        </div>
        <div class="relative w-full rounded-xl overflow-hidden bg-black" style="aspect-ratio: 16 / 9;">
          <iframe :src="playerUrl" class="absolute inset-0 w-full h-full" frameborder="0"
            allowfullscreen allow="autoplay; encrypted-media; fullscreen"
            :referrerpolicy="'no-referrer'"></iframe>
        </div>
      </section>

      <!-- 线路分类 -->
      <section class="glass-panel p-5">
        <h2 class="text-sm font-semibold text-white mb-4">解析线路 <span class="text-gray-500 font-normal">（{{ activeSources.length }} 条可用）</span></h2>

        <div class="flex flex-wrap gap-2 mb-5">
          <button v-for="cat in vipCategories" :key="cat.key"
            @click="activeCategory = cat.key"
            :class="['px-3 py-1.5 rounded-lg text-xs transition border',
              activeCategory === cat.key
                ? 'bg-purple-500/20 text-purple-200 border-purple-400/40'
                : 'border-white/10 text-gray-500 hover:text-white hover:border-white/20']">
            {{ cat.label }}
            <span class="ml-1 text-[10px] opacity-70">{{ countByCat(cat.key) }}</span>
          </button>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2.5">
          <button v-for="src in filteredSources" :key="src.name"
            @click="selectSource(src)"
            :class="['p-3 rounded-xl text-left transition border group',
              currentSource?.name === src.name
                ? 'border-cyan-400/50 bg-cyan-500/10'
                : 'border-white/10 bg-white/[0.03] hover:border-cyan-400/30 hover:bg-white/[0.06]']">
            <div class="flex items-center justify-between">
              <span class="text-sm font-medium text-white">{{ src.name }}</span>
              <span v-if="currentSource?.name === src.name" class="text-[10px] text-cyan-400">● 播放中</span>
            </div>
            <p v-if="src.note" class="text-[10px] text-gray-500 mt-1">{{ src.note }}</p>
          </button>
        </div>

        <p class="text-[11px] text-gray-600 mt-4 leading-relaxed">
          ⚠️ 所有解析接口均收集自互联网，画质与速度取决于接口提供方，本站无法保证。
          如某线路无法播放，请切换其他线路。本工具仅供学习交流，请支持正版。
        </p>
      </section>

      <!-- 油猴脚本安装 -->
      <section class="glass-panel p-5 mt-6">
        <h2 class="text-sm font-semibold text-white mb-2">🔧 安装油猴脚本（体验更佳）</h2>
        <p class="text-xs text-gray-500 mb-3">安装后可直接在视频网站页面内点击解析按钮，自动匹配选集，无需手动复制链接。</p>
        <div class="flex gap-3">
          <a class="web3-btn text-xs !px-5" :href="userscriptUrl" target="_blank" rel="noopener">安装脚本</a>
          <span class="text-[11px] text-gray-500 self-center">需先安装 <a href="https://www.tampermonkey.net/" target="_blank" rel="noopener" class="text-cyan-400 hover:underline">Tampermonkey</a> 浏览器扩展</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { vipSources, vipCategories, vipPlatforms } from '@/data/videoVipSources'
import { useToastStore } from '@/stores/modules/toast'

const toast = useToastStore()
const videoUrl = ref('')
const playerUrl = ref('')
const currentSource = ref(null)
const activeCategory = ref('recommend')
const parsing = ref(false)

// 线路均可用，无需过滤
const activeSources = computed(() => vipSources)

// 按分类筛选
const filteredSources = computed(() =>
  vipSources.filter(s => s.category === activeCategory.value)
)

// 统计某分类的线路数量
function countByCat(cat) {
  return vipSources.filter(s => s.category === cat).length
}

const canParse = computed(() => /^https?:\/\//.test(videoUrl.value.trim()))

// 「无水印播放器」等 direct 线路接收的是 m3u8 直链，输入提示需相应变化
const urlPlaceholder = computed(() =>
  currentSource.value?.direct
    ? '粘贴 m3u8 直链，如 https://xxx.com/index.m3u8（该线路不做网页解析）'
    : '粘贴爱奇艺/腾讯/优酷/芒果/B站等视频播放页 URL，如 https://www.iqiyi.com/v_xxx.html'
)

// 选择线路：若已输入视频链接则立即播放，否则仅记录当前线路
function selectSource(src) {
  currentSource.value = src
  if (canParse.value) {
    playerUrl.value = src.url + encodeURIComponent(videoUrl.value.trim())
    toast.success(`已切换到「${src.name}」`)
  } else {
    toast.info(src.direct ? '请输入 m3u8 直链后再解析' : `已选择「${src.name}」`)
  }
}

// 解析：用当前选中线路（默认取当前分类第一条）拼接 URL
function parse() {
  if (!canParse.value) {
    toast.error('请输入有效的视频页面链接')
    return
  }
  parsing.value = true
  const src = currentSource.value
    ? currentSource.value
    : vipSources.find(s => s.category === activeCategory.value)
  if (!src) {
    parsing.value = false
    toast.error('当前分类暂无可用线路')
    return
  }
  currentSource.value = src
  playerUrl.value = src.url + encodeURIComponent(videoUrl.value.trim())
  setTimeout(() => { parsing.value = false }, 400)
}

// 油猴脚本地址（jsdelivr CDN，长期维护）
const userscriptUrl = 'https://cdn.jsdelivr.net/gh/88lin/video_vip@main/video_vip.user.js'
</script>

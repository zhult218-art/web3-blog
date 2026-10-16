<template>
  <!-- 市场研究驾驶舱：同源 iframe 嵌入 marketingdashboard（React）全部功能 -->
  <div class="relative w-full" :style="{ height: frameHeight }">
    <!-- 顶栏：标题 + 返回 + 新窗打开 -->
    <div class="flex items-center justify-between gap-3 px-4 py-2.5 border-b border-cyan-500/20 bg-[#070b12]/80 backdrop-blur">
      <div class="flex items-center gap-3 min-w-0">
        <button @click="goBack" title="返回量化终端"
          class="flex items-center gap-1 text-xs px-2.5 py-1.5 rounded-lg bg-cyan-900/30 text-cyan-300 border border-cyan-500/30 hover:bg-cyan-500/20 transition flex-shrink-0">
          <span>←</span><span class="hidden sm:inline">返回</span>
        </button>
        <div class="min-w-0">
          <h1 class="text-sm font-bold text-cyan-200 truncate">市场研究驾驶舱</h1>
          <p class="text-[10px] text-cyan-500/60 truncate">沪深港美 · 大宗 · 美债 · 板块 · 资金流 · 快讯 · 产业链 · AI 观察 · 财报</p>
        </div>
      </div>
      <div class="flex items-center gap-2 flex-shrink-0">
        <span class="hidden md:flex items-center gap-1.5 text-[10px] text-cyan-500/50">
          <span class="w-1.5 h-1.5 rounded-full" :class="serviceUp ? 'bg-emerald-400 animate-pulse' : 'bg-rose-400'"></span>
          {{ serviceUp ? '服务在线' : (checking ? '探测中' : '服务未启动') }}
        </span>
        <a :href="cockpitSrc" target="_blank" rel="noopener"
          class="text-[10px] px-2.5 py-1.5 rounded-lg bg-cyan-900/30 text-cyan-300 border border-cyan-500/30 hover:bg-cyan-500/20 transition">
          新窗打开 ↗
        </a>
      </div>
    </div>

    <!-- iframe 容器 -->
    <div class="relative w-full" :style="{ height: 'calc(' + frameHeight + ' - 49px)' }">
      <!-- 乐观渲染：挂载即并行加载 iframe（不等 health 探测返回），消除串行等待 -->
      <iframe :key="frameKey" ref="iframeRef" :src="cockpitSrc" @load="onLoad"
        class="w-full h-full border-0 block bg-[#070b12]"
        title="市场研究驾驶舱" />
      <!-- 加载遮罩 -->
      <div v-if="(!loaded || !serviceUp) && !showDownState" class="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-[#070b12] text-cyan-300">
        <div class="w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin"></div>
        <div class="text-xs text-cyan-500/70">正在加载驾驶舱…</div>
      </div>
      <!-- 服务未启动提示 -->
      <div v-if="showDownState" class="absolute inset-0 flex flex-col items-center justify-center gap-4 bg-[#070b12] text-cyan-200 p-6 text-center">
        <div class="text-4xl">📡</div>
        <div class="text-sm font-bold">驾驶舱数据服务未启动</div>
        <p class="text-xs text-cyan-500/70 max-w-md leading-relaxed">
          市场研究驾驶舱依赖 py-service（端口 9006）的 cockpit 模块。请确认
          <code class="px-1.5 py-0.5 rounded bg-cyan-900/40 text-cyan-300 text-[11px]">py-service</code>
          已启动后刷新本页。
        </p>
        <button @click="recheck" class="text-xs px-4 py-2 rounded-lg bg-cyan-500/20 text-cyan-200 border border-cyan-500/30 hover:bg-cyan-500/30 transition">
          重新探测
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
// iframe 源：同源代理 /cockpit/ → py-service cockpit 模块（9006）
// React 前端已构建为 base='/cockpit/'，API 走 /cockpit/api/* 经 Vite 代理回源
// _v= 构建版本戳: 强制浏览器每次取最新 index.html(避免旧 bundle 缓存导致 ERR_ABORTED)
const cockpitSrc = '/cockpit/?_v=20261012'
const iframeRef = ref(null)
const loaded = ref(false)
const checking = ref(true)
const serviceUp = ref(false)
const showDownState = ref(false)
const frameKey = ref(0)   // 变更该值强制重建 iframe（服务恢复 / 手动重试时整页刷新）
let probeTimer = null
let downTimer = null

// 全屏可用高度：移动端避开地址栏，桌面端铺满视口下方导航区
const frameHeight = '100vh'

function onLoad() {
  loaded.value = true
  checking.value = false
}

async function probe() {
  checking.value = true
  try {
    // 不使用 AbortController: 服务不可达时 fetch 快速失败(连接拒绝),
    // 无需超时; 任何 abort 都会在响应体未消费时触发 ERR_ABORTED 控制台污染。
    // 始终消费响应体(r.json)防止 Chrome 标记未消费 body stream 为 cancelled。
    const r = await fetch('/cockpit/health')
    const j = await r.json().catch(() => ({}))
    serviceUp.value = !!(j?.status === 'ok' || r.ok)
  } catch {
    serviceUp.value = false
  }
  checking.value = false
  if (serviceUp.value) {
    // 服务恢复：若曾显示降级层，重建 iframe 拉取最新页面
    if (showDownState.value) {
      showDownState.value = false
      loaded.value = false
      frameKey.value++
    }
  } else if (!downTimer) {
    // 服务未就绪：5s 后展示降级提示，期间持续探测
    downTimer = setTimeout(() => { if (!serviceUp.value) showDownState.value = true }, 5000)
  }
}

function recheck() {
  showDownState.value = false
  loaded.value = false
  frameKey.value++   // 强制重建 iframe，触发整页重载
  probe()
}

function goBack() {
  router.push('/quant')
}

onMounted(() => {
  probe()
  // 每 15s 复探，服务恢复后自动加载 iframe
  probeTimer = setInterval(() => { if (!serviceUp.value) probe() }, 15000)
})

onBeforeUnmount(() => {
  if (probeTimer) clearInterval(probeTimer)
  if (downTimer) clearTimeout(downTimer)
})
</script>

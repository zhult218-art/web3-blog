<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-6xl">
      <PageHeader title="简历工坊" subtitle="Dnd Resume · 拖拽式简历生成器 · 在线编辑与导出"
        :crumbs="[{ label: '首页', to: '/' }, { label: '运维工具箱', to: '/tools' }, { label: '简历工坊' }]">
        <template #actions>
          <a :href="SITE_URL" target="_blank" rel="noopener" class="web3-btn text-xs !px-4 !py-1.5">↗ 新窗口打开</a>
        </template>
      </PageHeader>

      <!-- 功能亮点 -->
      <section class="glass-panel p-5 mb-5">
        <h2 class="text-sm font-bold text-white mb-3 flex items-center gap-2"><span>🧩</span>功能亮点</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          <div v-for="f in FEATURES" :key="f.title" class="rounded-xl border border-white/10 bg-white/[0.02] p-3">
            <span class="text-lg">{{ f.icon }}</span>
            <p class="text-xs text-white/90 mt-1.5 font-medium">{{ f.title }}</p>
            <p class="text-[11px] text-gray-500 mt-0.5 leading-relaxed">{{ f.desc }}</p>
          </div>
        </div>
        <p class="mt-4 pt-3 border-t border-white/10 text-[11px] text-gray-500">
          本工具为开源项目 <span class="text-cyan-300">dnd-resume</span> 的在线演示站点，采用同源 iframe 内嵌方式接入。
          若下方区域长时间空白，说明该站点限制了 iframe 嵌入，请点击右上角「新窗口打开」使用。
        </p>
      </section>

      <!-- 内嵌编辑器 -->
      <section class="glass-panel p-3 md:p-4">
        <div class="relative rounded-xl overflow-hidden border border-white/10 bg-black/40"
          style="height: calc(100vh - 220px); min-height: 620px;">
          <div v-if="loading" class="absolute inset-0 z-10 flex flex-col items-center justify-center gap-3 bg-[rgba(12,12,45,0.9)]">
            <div class="h-8 w-8 rounded-full border-2 border-white/15 border-t-cyan-400 animate-spin"></div>
            <span class="text-xs text-gray-500">正在加载简历编辑器…</span>
            <span class="text-[11px] text-gray-600">若长时间无法加载，请使用新窗口打开</span>
          </div>
          <iframe :src="SITE_URL" title="dnd-resume 拖拽式简历生成器"
            class="w-full h-full" referrerpolicy="no-referrer"
            allow="clipboard-read; clipboard-write"
            @load="loading = false"></iframe>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 简历工坊：以 iframe 内嵌 dnd-resume 在线演示站，
// 本地环境无法构建该项目（需 node>=24 + pnpm），故走官方演示接入
// ====================================================
import { ref } from 'vue'
import PageHeader from '@/components/common/PageHeader.vue'

const SITE_URL = 'https://dnd-resume.cn'
const loading = ref(true)

// 功能亮点（与官方仓库说明保持一致）
const FEATURES = [
  { icon: '🖱️', title: '拖拽排序', desc: '基于 dnd-kit，模块与条目均可自由拖拽重排' },
  { icon: '✍️', title: '富文本编辑', desc: 'tiptap 驱动，支持加粗、列表、链接等常用格式' },
  { icon: '🎨', title: '多套模板', desc: '内置多款版式，一键切换并实时预览效果' },
  { icon: '📤', title: '导出交付', desc: '编辑完成后可直接导出 PDF 投递使用' },
]
</script>

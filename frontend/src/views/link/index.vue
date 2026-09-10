<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-5xl">
      <h1 class="text-3xl font-bold text-gradient-cyber mb-2">友人帐</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">Friendly Links · 友情链接</p>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <a v-for="link in links" :key="link.name" :href="link.url" target="_blank" rel="noopener"
          class="glass-panel p-5 group hover:border-cyan-400/30 transition-all duration-300 hover:-translate-y-1">
          <div class="flex items-center gap-3 mb-3">
            <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500/20 to-cyan-500/20 flex items-center justify-center text-lg">
              {{ link.icon }}
            </div>
            <div>
              <h3 class="text-base font-semibold text-white group-hover:text-cyan-300 transition">{{ link.name }}</h3>
              <p class="text-[10px] text-gray-500 truncate max-w-[180px]">{{ link.url }}</p>
            </div>
          </div>
          <p class="text-xs text-gray-400 line-clamp-2">{{ link.desc }}</p>
        </a>
      </div>

      <div class="mt-12 glass-panel p-6 text-center">
        <p class="text-sm text-gray-400">想要交换友链？欢迎在留言板留言或通过关于页面联系我。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 友链页：展示本站友情链接列表
// ====================================================
const links = [
  { name: '个人主页', url: 'https://www.lololowe.com/', icon: '🏠', desc: '我的个人主页' },
  { name: 'LLM', url: 'https://ai.lololowe.com/', icon: '🤖', desc: '大语言模型服务' },
  { name: '哪吒监控', url: 'https://uptime.lololowe.com/', icon: '📡', desc: '服务状态监控' },
  { name: '网页探针', url: 'https://status.lololowe.com/', icon: '📊', desc: '网站可用性探针' },
  { name: 'IT书库', url: 'https://book.lololowe.com/', icon: '📚', desc: 'IT 技术书籍' },
  { name: '随机图片', url: 'https://hkd.lololowe.com/', icon: '🖼️', desc: '随机美图' },
  { name: '云盘', url: 'https://pan.lololowe.com/', icon: '☁️', desc: '个人云盘' },
  { name: '图床', url: 'https://img.lololowe.com/', icon: '📷', desc: '图片托管' },
  { name: 'Linux命令大全', url: 'https://linux.lololowe.com/', icon: '🐧', desc: 'Linux 命令查询' }
]
</script>

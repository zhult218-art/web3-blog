<template>
  <div class="min-h-screen px-4 md:px-6 py-10">
    <div class="mx-auto max-w-3xl">
      <h1 class="text-3xl font-bold text-gradient-cyber mb-2">关于本人</h1>
      <p class="text-sm text-gray-500 matrix-text mb-8">About · 关于这个站点</p>

      <div class="glass-panel p-8 mb-8">
        <div class="flex flex-col md:flex-row items-center gap-6">
          <div class="w-24 h-24 rounded-full bg-gradient-to-br from-purple-500 to-cyan-500 flex items-center justify-center text-4xl shadow-lg shadow-purple-500/30">
            W
          </div>
          <div class="text-center md:text-left">
            <h2 class="text-xl font-bold text-white">Web3 Portal</h2>
            <p class="text-sm text-gray-400 mt-2">这里没有速成的捷径，也没有轻松成功的保证。我把日常学习中积累的笔记分享在这里，希望能对你有一些帮助。</p>
          </div>
        </div>
      </div>

      <div class="space-y-6">
        <section class="glass-panel p-6">
          <h3 class="text-lg font-semibold text-white mb-3">📌 站点简介</h3>
          <p class="text-sm text-gray-400 leading-relaxed">
            这是一个 Web3 极简未来科技风的个人综合技术门户，包含技术博客、技术论坛、在线商城、金融量化、运维工具、多媒体中心、软件资源和资源中心等模块。
            后端采用 Spring Cloud Alibaba 微服务架构，前端使用 Vue3 + Vite 构建。
          </p>
        </section>

        <section class="glass-panel p-6">
          <h3 class="text-lg font-semibold text-white mb-3">🛠 技术栈</h3>
          <div class="flex flex-wrap gap-2">
            <span v-for="tech in techs" :key="tech" class="px-3 py-1 rounded-full text-xs border border-white/10 text-gray-400">{{ tech }}</span>
          </div>
        </section>

        <section class="glass-panel p-6">
          <h3 class="text-lg font-semibold text-white mb-3">📬 联系方式</h3>
          <p class="text-sm text-gray-400">如有问题或合作意向，欢迎通过留言板交流。</p>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
// ====================================================
// 关于页：站点介绍与前端技术栈展示
// ====================================================
const techs = ['Vue3', 'Vite', 'TailwindCSS', 'Spring Boot', 'Spring Cloud Alibaba', 'Nacos', 'MySQL', 'Redis', 'RabbitMQ', 'Docker']
</script>

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  cacheDir: 'node_modules/.vite',
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      // 开发环境：本地网关。生产 Cloudflare Pages 部署时，前端会直接走绝对地址
      // （由 VITE_API_BASE 控制，在 Cloudflare Pages 控制台配环境变量即可切换后端）。
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, '')
      },
      // 群聊 WebSocket：直连 forum-service（REST 走 /api/chat，互不冲突）
      '/chat': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        ws: true
      },
      // 聊天图片/群相册静态资源
      '/uploads': {
        target: 'http://localhost:8083',
        changeOrigin: true
      },
      '/pyquant': {
        target: 'http://localhost:9006',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/pyquant/, '')
      },
      // Vibe-Research A 股扩展数据服务（端口 8900）
      // 修复：此前缺少 /vr 代理导致"市场概览/热点"等 Tab 的请求全部 404/超时；
      // 8900 端全部接口带 /api 前缀，故 /vr/xxx -> /api/xxx
      '/vr': {
        target: 'http://127.0.0.1:8900',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/vr/, '/api'),
        // 行情聚合接口偶发较慢，代理层不设超时限制
        timeout: 120000,
        proxyTimeout: 120000
      },
      '/whisper': {
        target: 'http://127.0.0.1:9011',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/whisper/, '')
      },
      // 60s API 公共实例（github-myblog/60s-main 同款服务）
      // 浏览器虽可直连，但部分图片/二级路由 CORS 不稳定，统一走代理同源
      '/sixty': {
        target: 'https://60s.viki.moe',
        changeOrigin: true,
        secure: true,
        rewrite: path => path.replace(/^\/sixty/, ''),
        timeout: 60000,
        proxyTimeout: 60000
      },
      // iTunes 免费试听音频同源代理：跨域音频接入 Web Audio 后会静音，
      // 走本地代理变为同源，AudioWave 可视化才能拿到实时频谱
      '/itunes-audio': {
        target: 'https://audio-ssl.itunes.apple.com',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/itunes-audio/, '')
      },
      // Audius 免费完整曲库 JSON 接口同源代理（音频流走其内容节点签名直链）
      '/audius-api': {
        target: 'https://api.audius.co',
        changeOrigin: true,
        secure: true,
        rewrite: path => path.replace(/^\/audius-api/, ''),
        timeout: 30000,
        proxyTimeout: 30000
      }
    }
  },
  build: {
    // 2026-09-17 修订：开启 manualChunks，把大型 vendor 拆为独立 chunk，
    // 让浏览器并行下载、未变更的 chunk 长缓存。
    // 注：原注释说 manualChunks 会破坏 three.js 的 dev-mode 异步导入，
    // 实际上 manualChunks 只作用于 build 产物，对 dev server 无影响，
    // 故可以安全开启。three/echarts/markdown-it 各自独立 chunk，
    // 防止单 chunk 超过 1MB 影响首屏。
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          // 2026-09-17 修订：移除 element-plus / @element-plus/icons-vue，
          // 项目 package.json 未声明这两个依赖，原配置会导致 rollup 找不到 entry module 而 build 失败。
          'vendor-ui': ['echarts'],
          'vendor-three': ['three'],
          // 2026-09-17 修订：移除 vendor-markdown，markdown-it / highlight.js / html2canvas / jspdf
          // 这 4 个依赖项目 package.json 未声明，node_modules 也未安装，原配置会让 rollup build 失败。
        },
      },
    },
    chunkSizeWarningLimit: 1024,
  }
})

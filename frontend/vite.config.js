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
    // SakuraFrp 等内网穿透以隧道域名（如 frp-six.com）回源，Vite 默认会
    // 拦截 Host 头不在白名单的请求（403 Blocked request）。放行 frp-six.com
    // 及其所有子域名；新增穿透域名时在此追加即可（true 可放开全部域名）。
    allowedHosts: ['frp-six.com', '.frp-six.com', '.ngrok-free.dev', '.ngrok-free.app', '.ngrok.app', '.ngrok.io'],
    proxy: {
      // 开发环境：本地网关。生产 Cloudflare Pages 部署时，前端会直接走绝对地址
      // （由 VITE_API_BASE 控制，在 Cloudflare Pages 控制台配环境变量即可切换后端）。
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, '')
      },
      // 相册/媒体文件：photo.url 为 /photo/file/{name}（不带 /api），
      // 经网关转发到 media-service（8085），开发环境需单独代理
      '/photo': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 120000,
        proxyTimeout: 120000
      },
      // 聊天实时通道：Socket.IO 网关（node-service/chat-gateway，:3010）。
      // 默认 polling 起手 + 自动升级 websocket，故 ws 与 http 都要放行；
      // 同源代理使 httpOnly Cookie 随握手自动携带，前端无需（也不允许）传 token。
      '/socket.io': {
        target: 'http://127.0.0.1:3010',
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
      // 全站免费 AI 通用通道（py-service ai 模块）：/pyai/chat → /api/ai/chat。
      // 免费模型偶发较慢，放大超时；SSE 流式禁用缓冲由下游响应头控制。
      '/pyai': {
        target: 'http://localhost:9006',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/pyai/, '/api/ai'),
        timeout: 240000,
        proxyTimeout: 240000
      },
      // 系统监控（统一 py-service 9006 的 monitor 模块）。
      // 页面路由是 /admin/monitor，故 API 前缀用 /monitapi 错开，
      // /monitapi/xxx -> /api/monitor/xxx。
      '/monitapi': {
        target: 'http://localhost:9006',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/monitapi/, '/api/monitor'),
        timeout: 60000,
        proxyTimeout: 60000
      },
      '/whisper': {
        target: 'http://127.0.0.1:9006',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/whisper/, '/api/whisper')
      },
      // 时光回溯 QQ 空间备份（已并入统一 py-service 9006 的 qzone 模块）
      // 注意：页面路由是 /qzone，故 API 代理前缀不能用 /qzone（会吞掉页面请求），
      // 使用 /qzapi → 后端 /api/qzone。
      '/qzapi': {
        target: 'http://127.0.0.1:9006',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/qzapi/, '/api/qzone'),
        timeout: 120000,
        proxyTimeout: 120000
      },
      // WiFi 安全审计（统一 py-service 9006 的 wifi 模块）。
      // 页面路由是 /wifi，故 API 前缀错开用 /wifiapi；开启 ws 以支持实时日志。
      '/wifiapi': {
        target: 'http://127.0.0.1:9006',
        changeOrigin: true,
        ws: true,
        rewrite: path => path.replace(/^\/wifiapi/, '/api/wifi'),
        timeout: 120000,
        proxyTimeout: 120000
      },
      // OSINT 信息收集（统一 py-service 9006 的 osint 模块）。
      // 页面路由是 /osint，故 API 前缀用 /osintapi；开启 ws 支持实时扫描日志。
      '/osintapi': {
        target: 'http://127.0.0.1:9006',
        changeOrigin: true,
        ws: true,
        rewrite: path => path.replace(/^\/osintapi/, '/api/osint'),
        timeout: 120000,
        proxyTimeout: 120000
      },
      // 星图助手语音全双工（统一 py-service 9006 的 voice 模块）。
      // 网关（8080）无 WebSocket 路由，故前端 WS 必须经 Vite 直连 9006；
      // /voiceapi/ws -> /api/voice/ws。
      '/voiceapi': {
        target: 'http://127.0.0.1:9006',
        changeOrigin: true,
        ws: true,
        rewrite: path => path.replace(/^\/voiceapi/, '/api/voice'),
        timeout: 120000,
        proxyTimeout: 120000
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
      },
      // 网页采集（轻量档）：py-service 9006 的 scrape 模块，curl_cffi 抓取 + scrapling 解析。
      // /pyscrape/xxx -> /api/scrape/xxx（页面路由是 /tools/scraper，前缀与页面错开）
      '/pyscrape': {
        target: 'http://localhost:9006',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/pyscrape/, '/api/scrape'),
        timeout: 120000,
        proxyTimeout: 120000
      },
      // GhostTrack 网络追踪（py-service 9006 的 ghosttrack 模块）。
      // /gtapi/xxx -> /api/ghosttrack/xxx（页面路由是 /tools/ghosttrack，前缀与页面错开）
      '/gtapi': {
        target: 'http://localhost:9006',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/gtapi/, '/api/ghosttrack'),
        // 用户名跨站检测要并发探测 22 个站点，单站 8s 超时 + 限流让整体偏慢
        timeout: 120000,
        proxyTimeout: 120000
      },
      // 市场驾驶舱（tick-stock-panel）：代码级移植到 py-service（端口 9006）。
      // 前端通过 iframe 加载 /cockpit/，React 应用内 API 调用走 /cockpit/api/*。
      // 此代理统一转发静态资源与 API 接口到 py-service（不剥离前缀，路由在 py-service 侧匹配）。
      '/cockpit': {
        target: 'http://localhost:9006',
        changeOrigin: true,
        // 行情聚合接口偶发较慢，代理层不设超时限制
        timeout: 120000,
        proxyTimeout: 120000,
        // WebSocket 支持（SSE 流式复盘等）
        ws: true
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

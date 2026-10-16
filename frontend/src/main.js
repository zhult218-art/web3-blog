// ============================================================
// 前端应用入口
// 创建 Vue 应用，注册 Pinia、路由与全局指令，最终挂载 #app
// ============================================================
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { vReveal, vRevealLeft, vRevealRight, vRevealScale } from '@/directives/reveal'
import { vTilt } from '@/directives/tilt'
import '@/assets/styles/main.css'
import 'highlight.js/styles/atom-one-dark.css'

const app = createApp(App)
// 抑制 Vue 开发模式非关键警告（Suspense experimental、异步组件等）
app.config.warnHandler = (msg) => {
  if (msg.includes('experimental') || msg.includes('Suspense')) return
  if (import.meta.env.DEV) console.warn(msg)
}
app.use(createPinia())
app.use(router)
// 全局注册滚动显现指令（模板中使用 v-reveal / v-reveal-left / v-reveal-right / v-reveal-scale）
app.directive('reveal', vReveal)
app.directive('reveal-left', vRevealLeft)
app.directive('reveal-right', vRevealRight)
app.directive('reveal-scale', vRevealScale)
// 全局注册 3D 倾斜指令（卡片 hover 微倾斜：v-tilt / v-tilt="8"）
app.directive('tilt', vTilt)
app.mount('#app')

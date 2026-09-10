// ============================================================
// 前端应用入口
// 创建 Vue 应用，注册 Pinia、路由与全局指令，最终挂载 #app
// ============================================================
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { vReveal, vRevealLeft, vRevealRight, vRevealScale } from '@/directives/reveal'
import '@/assets/styles/main.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
// 全局注册滚动显现指令（模板中使用 v-reveal / v-reveal-left / v-reveal-right / v-reveal-scale）
app.directive('reveal', vReveal)
app.directive('reveal-left', vRevealLeft)
app.directive('reveal-right', vRevealRight)
app.directive('reveal-scale', vRevealScale)
app.mount('#app')

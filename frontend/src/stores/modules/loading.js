// ============================================================
// 全局加载状态（Pinia）：汇总真实加载进度
//   · axios 在途请求：请求拦截器 start()，响应/错误拦截器 done()
//   · 路由切换：beforeEach navStart()，afterEach/onError navEnd()
// 开场卷轴加载层只在首屏出现一次，不消费本状态，故此处仅作计数留存
// ============================================================
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useLoadingStore = defineStore('loading', () => {
  const reqCount = ref(0)      // 在途 API 请求数
  const navLoading = ref(false) // 是否处于路由切换中
  const loading = computed(() => reqCount.value > 0 || navLoading.value)

  function start() { reqCount.value++ }
  function done() { if (reqCount.value > 0) reqCount.value-- }
  function navStart() { navLoading.value = true }
  function navEnd() { navLoading.value = false }

  return { reqCount, navLoading, loading, start, done, navStart, navEnd }
})

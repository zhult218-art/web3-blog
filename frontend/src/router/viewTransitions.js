// ============================================================
// View Transitions API 路由过渡（浏览器级快照过渡）
//
// 为什么用这个而不是 Vue 的 <transition>：
//   布局内层路由此前用 `<transition mode="out-in">` 包裹 Suspense + keep-alive 时，
//   异步组件挂起与离开动画互相阻塞，出现过"返回后大框架在、内容区空白，需刷新才恢复"，
//   并破坏 keep-alive 缓存。View Transitions 由浏览器在 DOM 更新前后各拍一次快照做动画，
//   完全不参与 Vue 组件树，因此与 Suspense / keep-alive / <transition> 互不干扰。
//
// 启用条件（三重收敛，避免意料外的视觉或卡顿问题）：
//   1) 浏览器支持 document.startViewTransition；
//   2) 用户未开启"减少动效"（prefers-reduced-motion: reduce）；
//   3) 仅在布局内切页（根路由记录相同）时启用 —— 登录页 ↔ 主布局这类跨布局跳转
//      仍走 App.vue 既有的 page 过渡，避免两套动画叠加。
//
// 常驻元素通过 CSS view-transition-name 参与过渡：
//   main(#vt-content)  内容区 —— 淡入上移 / 淡出下沉
//   GlobalPlayer       播放器 —— 位置不变，显式禁用动画
// 导航 / 壁纸 / 页脚在两份快照中一致，交叉淡入淡出后视觉上完全静止。
// ============================================================
import { nextTick } from 'vue'

export function installViewTransitions(router) {
  if (typeof document === 'undefined' || typeof document.startViewTransition !== 'function') return

  const reducedMotion = () =>
    typeof window !== 'undefined' && !!window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches

  // 同一布局内：根路由记录一致（且该记录真实存在）
  const sameLayout = (to, from) => !!to.matched[0] && to.matched[0] === from.matched[0]

  router.beforeResolve((to, from) => {
    // 页面不在前台时不启用：后台标签页不产生渲染帧，startViewTransition 的回调
    // 可能迟迟不触发，会拖住导航（表现为模块请求被 abort、内容区空白）。
    if (reducedMotion() || !sameLayout(to, from) || to.fullPath === from.fullPath) return
    if (typeof document.visibilityState === 'string' && document.visibilityState !== 'visible') return

    return new Promise((resolve) => {
      const body = document.body
      // 过渡期间压暗背景层，与既有 route-transitioning 观感保持一致
      body.classList.add('vt-active')
      let settled = false
      const done = () => {
        if (settled) return
        settled = true
        body.classList.remove('vt-active')
      }
      // 放行导航（Promise 重复 resolve 是空操作，可安全多次调用）
      const release = () => resolve()
      // 兜底 1：回调迟迟不触发时也要放行导航，绝不能把路由卡住
      const guardTimer = setTimeout(() => { release(); done() }, 500)
      // 兜底 2：过渡异常中断 / finished 不 resolve 时恢复压暗层
      const restoreTimer = setTimeout(done, 1500)

      try {
        const transition = document.startViewTransition(async () => {
          clearTimeout(guardTimer)
          // 先放行路由继续完成导航，再等 Vue 把新页面写进 DOM，
          // 浏览器据此拍下"新状态"快照并播放动画。
          release()
          await nextTick()
        })
        transition.finished?.then(() => { clearTimeout(restoreTimer); done() }, () => { clearTimeout(restoreTimer); done() })
      } catch {
        // 已有过渡进行中 / API 不可用时按普通导航继续，不做任何延迟
        clearTimeout(guardTimer)
        clearTimeout(restoreTimer)
        release()
        done()
      }
    })
  })
}

<template>
  <div class="section-header" data-glow ref="rootEl">
    <div class="section-number">{{ num }}</div>
    <h2 class="section-title">{{ title }}</h2>
    <p class="section-subtitle">{{ sub }}</p>
  </div>
</template>

<script setup>
// ============================================================
// 首页区块标题组件（SectionHead）
// 显示 [编号] + 主标题 + 英文副标题；
// 进入视口时主标题逐字错峰入场（SplitText + ScrollTrigger），
// 编号与副标题随后淡入。组件卸载时 gsap.context 自动回收。
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { SplitText } from 'gsap/SplitText'
gsap.registerPlugin(ScrollTrigger, SplitText)

defineProps({
  num: { type: String, required: true },
  title: { type: String, required: true },
  sub: { type: String, default: '' },
})

const rootEl = ref(null)
let ctx

onMounted(() => {
  ctx = gsap.context(() => {
    const titleEl = rootEl.value.querySelector('.section-title')
    if (!titleEl) return

    // 逐字拆分；字符用纯色填充（避免 background-clip:text 在位移后字形消失导致标题不可见）
    const split = new SplitText(titleEl, { type: 'chars' })
    gsap.set(split.chars, {
      display: 'inline-block',
      color: '#fff',
      WebkitTextFillColor: '#fff',
      transformPerspective: 600,
      willChange: 'transform, opacity',
    })

    const tl = gsap.timeline({
      scrollTrigger: { trigger: rootEl.value, start: 'top 82%', once: true },
    })

    tl.from(split.chars, {
      yPercent: 120,
      opacity: 0,
      rotationX: -60,
      transformOrigin: '50% 100%',
      stagger: 0.04,
      duration: 0.7,
      ease: 'back.out(1.7)',
    }, 0)

    tl.from(rootEl.value.querySelector('.section-number'), {
      opacity: 0,
      x: -18,
      duration: 0.5,
      ease: 'power2.out',
    }, 0.05)

    tl.from(rootEl.value.querySelector('.section-subtitle'), {
      opacity: 0,
      y: 14,
      duration: 0.5,
      ease: 'power2.out',
    }, 0.4)
  }, rootEl.value)
})

onBeforeUnmount(() => ctx?.revert())
</script>

<style scoped>
.section-header {
  margin-bottom: 3.5rem;
}

.section-number {
  font-size: 0.75rem;
  letter-spacing: 0.3em;
  color: #667eea;
  margin-bottom: 0.75rem;
  font-family: 'Courier New', monospace;
  text-shadow: 0 0 10px rgba(102, 126, 234, 0.6);
}

.section-number::before { content: '[ '; color: rgba(102, 126, 234, 0.5); }
.section-number::after { content: ' ]'; color: rgba(102, 126, 234, 0.5); }

.section-title {
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 800;
  margin-bottom: 0.5rem;
  color: #fff;
  position: relative;
  display: inline-block;
  text-shadow: 0 0 24px rgba(120, 160, 255, 0.35);
}

.section-subtitle {
  font-size: 0.8rem;
  letter-spacing: 0.3em;
  color: rgba(255, 255, 255, 0.3);
  font-family: 'Courier New', monospace;
}
</style>

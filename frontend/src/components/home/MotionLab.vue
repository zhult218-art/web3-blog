<template>
  <section id="motionlab" class="motion-lab">
    <div class="section-bg">
      <div class="bg-grid"></div>
      <div class="ml-glow"></div>
    </div>

    <SectionHead num="[ 04 ]" title="动效实验室" sub="MOTION LAB" />

    <p class="lab-intro">
      滚动即叙事。这一区把六种现代滚动动效范式做成可交互的实机演示：
      流体扭曲、全屏扩展、场景切换、3D 环形阵列、关键态吸附与叠层转场，
      全部由 GSAP ScrollTrigger 驱动，随滚动进度实时演算。
    </p>

    <!-- EFFECT_01 交互式流体扭曲（非 pin，常规流） -->
    <FluidWarp />

    <!-- EFFECT_02 滚动驱动全屏扩展转场（pin + scrub + snap） -->
    <ExpandReveal />

    <!-- EFFECT_03 滚动驱动场景切换 + 吸附（pin + labels snap） -->
    <SceneSwitcher />

    <!-- EFFECT_04 滚动驱动 3D 环形轮播（pin + scrub + snap） -->
    <RingCarousel />

    <!-- EFFECT_05 滚动叠层转场（sticky 堆叠 + 覆盖压暗） -->
    <StackDeck />
  </section>
</template>

<script setup>
// ============================================================
// [04] 动效实验室：滚动特效合集容器
// 组装五个特效子组件，并统一提供区块背景与说明文案。
// 各子组件自管 gsap.context 生命周期，卸载时自动回收。
// ============================================================
import SectionHead from './SectionHead.vue'
import FluidWarp from './effects/FluidWarp.vue'
import ExpandReveal from './effects/ExpandReveal.vue'
import SceneSwitcher from './effects/SceneSwitcher.vue'
import RingCarousel from './effects/RingCarousel.vue'
import StackDeck from './effects/StackDeck.vue'

import { onMounted } from 'vue'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

// 字体/图片加载完成后重算各 pin 区块位置
onMounted(() => {
  const t = setTimeout(() => ScrollTrigger.refresh(), 600)
  window.addEventListener('load', () => ScrollTrigger.refresh(), { once: true })
  return () => clearTimeout(t)
})
</script>

<style scoped>
.motion-lab {
  position: relative;
  padding: clamp(4rem, 9vh, 7rem) clamp(1.2rem, 5vw, 4rem) 0;
}

.section-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image: none;
  mask-image: linear-gradient(180deg, transparent, #000 8%, #000 92%, transparent);
  -webkit-mask-image: linear-gradient(180deg, transparent, #000 8%, #000 92%, transparent);
}

.ml-glow {
  position: absolute;
  top: -10%;
  left: 50%;
  width: 70vw;
  height: 40vh;
  transform: translateX(-50%);
  background: radial-gradient(ellipse at center, rgba(102,126,234,.1), transparent 65%);
}

.lab-intro {
  max-width: 68ch;
  margin: -1.6rem auto 3.2rem;
  text-align: center;
  font-size: .86rem;
  line-height: 2;
  color: rgba(255,255,255,.5);
}
</style>

<template>
  <div class="deck-wrap" ref="rootEl">
    <div
      v-for="(card, i) in deck"
      :key="card.title"
      class="deck-slot" :style="{ '--i': i }"
    >
      <article class="deck-card" :class="`dc-${i}`">
        <div class="dc-inner">
          <div class="dc-head">
            <span class="dc-idx">{{ String(i + 1).padStart(2, '0') }}</span>
            <span class="dc-tag">EFFECT_05 · STACKING</span>
          </div>
          <h3 class="dc-title">{{ card.title }}</h3>
          <p class="dc-desc">{{ card.desc }}</p>
          <div class="dc-metrics">
            <div v-for="m in card.metrics" :key="m.label" class="dc-metric">
              <b>{{ m.value }}</b>
              <span>{{ m.label }}</span>
            </div>
          </div>
        </div>
        <div class="dc-glow"></div>
      </article>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// EFFECT_06 滚动叠层转场（Stacking Deck）
// 卡片槽位用 position: sticky 依次钉在视口顶部并保留阶梯偏移，
// 后卡上滑覆盖前卡；GSAP 只负责「被覆盖时」的缩小 + 压暗，
// 让堆叠产生真实的纸张层次感。无 pin，天然兼容移动端。
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
gsap.registerPlugin(ScrollTrigger)

const deck = [
  {
    title: '性能工程',
    desc: '以 GPU 合成层为第一原则：transform 与 opacity 驱动动画，will-change 精准投放，长列表虚拟化，让每一帧都落在 16ms 预算之内。',
    metrics: [{ value: '60fps', label: '帧率预算' }, { value: '-42%', label: '首屏耗时' }, { value: 'A+', label: 'Lighthouse' }],
  },
  {
    title: '视觉语言',
    desc: '深空底色、玻璃拟态与霓虹辉光构成统一语法；渐变文字、光束描边与噪点纹理负责氛围，留白与网格负责秩序。',
    metrics: [{ value: '4', label: '主题层级' }, { value: '12', label: '动效曲线' }, { value: '∞', label: '组合可能' }],
  },
  {
    title: '交互范式',
    desc: '滚动即叙事：scrub 映射进度、pin 固化舞台、snap 收束状态。指针轨迹成为输入信号，页面随行为而非点击而变化。',
    metrics: [{ value: '7', label: '滚动特效' }, { value: '3D', label: '空间维度' }, { value: '0', label: '额外插件' }],
  },
  {
    title: '工程交付',
    desc: '组件自包含、上下文自动清理、降级策略完备——prefers-reduced-motion 与离屏暂停内建其中，炫技不牺牲可维护性。',
    metrics: [{ value: 'Vue3', label: '技术栈' }, { value: 'GSAP', label: '动效内核' }, { value: '100%', label: '卸载回收' }],
  },
]

const rootEl = ref(null)
let ctx

onMounted(() => {
  ctx = gsap.context(() => {
    const slots = gsap.utils.toArray('.deck-slot')
    const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches

    slots.forEach((slot, i) => {
      const card = slot.querySelector('.dc-inner')

      // 入场：首次进入视口时上浮揭示（一次性）
      if (!reduced) {
        gsap.from(card, {
          yPercent: 14,
          opacity: 0,
          duration: .8,
          ease: 'power3.out',
          scrollTrigger: { trigger: slot, start: 'top 88%', once: true },
        })
      }

      // 被下一张覆盖时：缩小 + 压暗（scrub 双向可逆）
      if (i < slots.length - 1 && !reduced) {
        gsap.to(card, {
          scale: .9,
          filter: 'brightness(.45)',
          transformOrigin: 'center top',
          ease: 'none',
          scrollTrigger: {
            trigger: slots[i + 1],
            start: 'top 92%',
            end: 'top 30%',
            scrub: true,
          },
        })
      }
    })
  }, rootEl.value)
})

onBeforeUnmount(() => ctx?.revert())
</script>

<style scoped>
.deck-wrap {
  display: flex;
  flex-direction: column;
}

.deck-slot {
  position: sticky;
  /* 每张卡顶部留出递增偏移，形成可见的层叠边缘 */
  top: calc(5.5rem + var(--i) * 1.1rem);
  padding-bottom: clamp(2rem, 6vh, 4rem);
}

.deck-card {
  position: relative;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,.1);
  background:
    radial-gradient(120% 130% at 15% 0%, rgba(102,126,234,.16), transparent 55%),
    radial-gradient(110% 120% at 90% 110%, rgba(118,75,162,.14), transparent 55%),
    #0c0c20;
  box-shadow: 0 -18px 50px rgba(0,0,0,.45);
  will-change: transform, filter;
}

.dc-inner {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, .9fr);
  gap: clamp(1.4rem, 4vw, 3.5rem);
  align-items: center;
  padding: clamp(1.6rem, 4.5vw, 3.4rem);
  min-height: min(62vh, 480px);
}

.dc-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(115deg, transparent 42%, rgba(0,212,255,.07) 50%, transparent 58%);
}

.dc-head {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}
.dc-idx {
  font-family: 'Courier New', monospace;
  font-size: 2.2rem;
  font-weight: 700;
  color: transparent;
  -webkit-text-stroke: 1px rgba(0,212,255,.55);
}
.dc-tag {
  font-family: 'Courier New', monospace;
  font-size: .62rem;
  letter-spacing: .25em;
  color: rgba(0,212,255,.7);
}

.dc-title {
  font-size: clamp(1.7rem, 4vw, 2.7rem);
  font-weight: 800;
  margin-bottom: .9rem;
  background: linear-gradient(135deg, #fff 25%, rgba(255,255,255,.6));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.dc-desc {
  font-size: clamp(.84rem, 1.3vw, .96rem);
  line-height: 1.95;
  color: rgba(255,255,255,.6);
  max-width: 52ch;
}

.dc-metrics {
  display: flex;
  flex-direction: column;
  gap: .9rem;
  justify-self: end;
  width: 100%;
  max-width: 260px;
}
.dc-metric {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: .7rem;
  border-bottom: 1px solid rgba(255,255,255,.08);
}
.dc-metric b {
  font-family: 'Courier New', monospace;
  font-size: 1.35rem;
  color: #00d4ff;
  letter-spacing: .04em;
}
.dc-metric span { font-size: .74rem; color: rgba(255,255,255,.42); }

@media (max-width: 768px) {
  .dc-inner { grid-template-columns: 1fr; }
  .dc-metrics { justify-self: start; max-width: none; flex-direction: row; flex-wrap: wrap; gap: 1rem; }
  .dc-metric { flex-direction: column; border: 0; padding: 0; gap: .2rem; }
}
</style>

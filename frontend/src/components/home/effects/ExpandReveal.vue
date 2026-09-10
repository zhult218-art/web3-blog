<template>
  <div class="expand-wrap" ref="stageEl">
    <div class="expand-viewport" ref="pinEl">
      <div class="expand-card" ref="cardEl">
        <!-- 收起态：卡片预览 -->
        <div class="ex-teaser">
          <span class="ex-tag">EFFECT_02</span>
          <h3 class="ex-title">滚动展开</h3>
          <p class="ex-hint">继续向下滚动 · SCROLL TO EXPAND</p>
          <div class="ex-pulse"></div>
        </div>
        <!-- 展开态：满屏内容 -->
        <div class="ex-full">
          <span class="ex-full-tag">FULLSCREEN MODE</span>
          <h2 class="ex-full-title">从一张卡片，到整个世界</h2>
          <p class="ex-full-desc">
            全屏扩展转场将「聚焦」交给滚动：元素随滚动进度平滑生长为全屏画布，
            内容分层浮现。松手后自动吸附到最近的关键状态，收放自如。
          </p>
          <div class="ex-stats">
            <div class="ex-stat"><b>60fps</b><span>GPU 合成驱动</span></div>
            <div class="ex-stat"><b>SCRUB</b><span>滚动进度映射</span></div>
            <div class="ex-stat"><b>SNAP</b><span>关键态吸附</span></div>
          </div>
          <button class="ex-cta" type="button">开始探索 →</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ============================================================
// EFFECT_02 滚动驱动的全屏扩展转场
// pin 住视口，卡片尺寸随 scrub 从小卡长成全屏；
// 预览内容先淡出，满屏内容后入场；两端状态做 snap 吸附。
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
gsap.registerPlugin(ScrollTrigger)

const stageEl = ref(null)
const pinEl = ref(null)
const cardEl = ref(null)
let ctx

onMounted(() => {
  ctx = gsap.context(() => {
    const card = cardEl.value
    const tl = gsap.timeline({
      defaults: { ease: 'none' },
      scrollTrigger: {
        trigger: stageEl.value,
        start: 'top top',
        end: '+=1600',
        scrub: .6,
        pin: pinEl.value,
        anticipatePin: 1,
        invalidateOnRefresh: true,
        // 关键态吸附：要么收起、要么完全展开
        snap: { snapTo: [0, 1], duration: .45, ease: 'power2.inOut', delay: .06 },
      },
    })

    // 尺寸生长：小卡 -> 全屏
    tl.fromTo(card,
      { width: () => Math.min(520, innerWidth * .82), height: () => Math.min(340, innerHeight * .44), borderRadius: 22 },
      { width: () => innerWidth, height: () => innerHeight, borderRadius: 0, duration: 1 }, 0)

    // 预览层先退场
    tl.to('.ex-teaser', { opacity: 0, scale: .9, filter: 'blur(6px)', duration: .3 }, 0)

    // 满屏内容后进：标题 / 描述 / 数据 / 按钮 错峰上浮
    tl.fromTo('.ex-full > *',
      { y: 46, opacity: 0 },
      { y: 0, opacity: 1, stagger: .09, duration: .35 },
      .55)
  }, stageEl.value)
})

onBeforeUnmount(() => ctx?.revert())
</script>

<style scoped>
.expand-wrap { position: relative; }

.expand-viewport {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.expand-card {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(120% 140% at 20% 0%, rgba(102,126,234,.28), transparent 55%),
    radial-gradient(110% 130% at 85% 100%, rgba(118,75,162,.26), transparent 55%),
    #0b0b1c;
  border: 1px solid rgba(255,255,255,.12);
  box-shadow: 0 30px 80px rgba(0,0,0,.5);
  will-change: width, height;
}

/* ---- 收起态 ---- */
.ex-teaser {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: .7rem;
  text-align: center;
}

.ex-tag {
  font-family: 'Courier New', monospace;
  font-size: .66rem;
  letter-spacing: .25em;
  color: #00d4ff;
}

.ex-title {
  font-size: clamp(1.4rem, 3vw, 2rem);
  font-weight: 800;
  background: linear-gradient(135deg, #fff, rgba(255,255,255,.65));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.ex-hint {
  font-family: 'Courier New', monospace;
  font-size: .68rem;
  letter-spacing: .2em;
  color: rgba(255,255,255,.4);
}

.ex-pulse {
  width: 42px; height: 42px;
  margin-top: .8rem;
  border-radius: 50%;
  border: 1px solid rgba(0,212,255,.5);
  animation: exPulse 2s ease-out infinite;
}
.ex-pulse::after {
  content: '';
  position: absolute;
  width: 42px; height: 42px;
  border-radius: 50%;
  border: 1px solid rgba(0,212,255,.35);
  animation: exRing 2s ease-out infinite;
}
@keyframes exPulse { 50% { transform: scale(.72); opacity: .5; } }
@keyframes exRing { to { transform: scale(1.9); opacity: 0; } }

/* ---- 展开态 ---- */
.ex-full {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.2rem;
  padding: 2rem;
  text-align: center;
}

.ex-full-tag {
  font-family: 'Courier New', monospace;
  font-size: .7rem;
  letter-spacing: .3em;
  color: rgba(0,212,255,.85);
}

.ex-full-title {
  font-size: clamp(2rem, 5.5vw, 3.6rem);
  font-weight: 800;
  line-height: 1.15;
  max-width: 18ch;
  background: linear-gradient(135deg, #fff 20%, rgba(255,255,255,.6));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.ex-full-desc {
  max-width: 56ch;
  font-size: clamp(.85rem, 1.4vw, 1rem);
  line-height: 1.9;
  color: rgba(255,255,255,.62);
}

.ex-stats {
  display: flex;
  gap: clamp(1.2rem, 4vw, 3.5rem);
  margin-top: .6rem;
}

.ex-stat {
  display: flex;
  flex-direction: column;
  gap: .3rem;
}
.ex-stat b {
  font-family: 'Courier New', monospace;
  font-size: clamp(1.1rem, 2.4vw, 1.7rem);
  color: #00d4ff;
  letter-spacing: .08em;
}
.ex-stat span { font-size: .74rem; color: rgba(255,255,255,.45); }

.ex-cta {
  margin-top: 1.2rem;
  padding: .8rem 2.2rem;
  border-radius: 999px;
  border: 1px solid rgba(0,212,255,.45);
  background: rgba(0,212,255,.1);
  color: #7fe7ff;
  font-size: .85rem;
  letter-spacing: .12em;
  cursor: pointer;
  transition: all .3s;
}
.ex-cta:hover {
  background: rgba(0,212,255,.22);
  box-shadow: 0 0 32px rgba(0,212,255,.35);
  transform: translateY(-2px);
}
</style>

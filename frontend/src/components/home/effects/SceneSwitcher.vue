<template>
  <div class="scenes-wrap" ref="stageEl">
    <div class="scene-viewport" ref="pinEl">
      <!-- 场景层 -->
      <div
        v-for="(sc, i) in scenes"
        :key="sc.key"
        class="scene-panel" :class="`scene-${sc.key}`"
      >
        <div class="scene-visual">
          <template v-if="sc.key === 'nebula'">
            <div class="neb-orb o1"></div><div class="neb-orb o2"></div><div class="neb-orb o3"></div>
            <span v-for="k in 40" :key="k" class="neb-star" :style="starStyle(k)"></span>
          </template>
          <template v-else-if="sc.key === 'gridcity'">
            <div class="gc-floor"></div>
            <div class="gc-skyline">
              <span v-for="(h, k) in skyline" :key="k" class="gc-bar" :style="{ height: h + '%' }"></span>
            </div>
          </template>
          <template v-else-if="sc.key === 'stream'">
            <div v-for="(col, k) in streamCols" :key="k" class="st-col" :style="{ left: col.left, animationDuration: col.dur, animationDelay: col.delay }">
              <span v-for="ch in 14" :key="ch">{{ randChar(k * 31 + ch) }}</span>
            </div>
          </template>
          <template v-else>
            <div class="au-band b1"></div><div class="au-band b2"></div><div class="au-band b3"></div>
          </template>
        </div>

        <div class="scene-caption">
          <span class="sc-no">SCENE 0{{ i + 1 }}</span>
          <h3 class="sc-title">{{ sc.title }}</h3>
          <p class="sc-desc">{{ sc.desc }}</p>
        </div>
      </div>

      <!-- 进度指示 -->
      <div class="scene-progress">
        <button
          v-for="(sc, i) in scenes" :key="'dot' + i"
          class="sp-dot" :class="{ on: active === i }"
          :aria-label="sc.title"
        ></button>
        <span class="sp-count">0{{ active + 1 }} / 0{{ scenes.length }}</span>
      </div>
    </div>
    <p class="scenes-tip">滚动切换场景 · 松手自动吸附到最近一幕</p>
  </div>
</template>

<script setup>
// ============================================================
// EFFECT_03 滚动驱动的场景切换 + 滚动吸附
// pin 视口后用 scrub 时间线串联多幕：上一幕缩小淡出、下一幕上滑揭入；
// snap 到每幕标签位置，进度点与计数随 onUpdate 同步。
// 四幕视觉全部由 CSS 程序化绘制，零图片资源。
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
gsap.registerPlugin(ScrollTrigger)

const scenes = [
  { key: 'nebula', title: '星云漫游', desc: '粒子星尘在深空缓慢呼吸，光斑如极夜般晕染。' },
  { key: 'gridcity', title: '网格都市', desc: '透视地平线延伸至无穷远，数据楼宇在地平线上生长。' },
  { key: 'stream', title: '数据流', desc: '字符瀑布倾泻而下，信息以光速穿行于终端之间。' },
  { key: 'aurora', title: '极光帷幕', desc: '色带在天幕上流动折射，柔和的光扫过每一个像素。' },
]

const stageEl = ref(null)
const pinEl = ref(null)
const active = ref(0)
let ctx

const skyline = [34, 58, 42, 76, 52, 88, 46, 66, 38, 72, 55, 82, 44, 60]
const streamCols = Array.from({ length: 12 }, (_, i) => ({
  left: (i / 12) * 100 + '%',
  dur: (2.4 + (i % 4) * .8) + 's',
  delay: -(i * .7 % 3) + 's',
}))
const CH = '01<>/{}#@$%&*+=~ABCDEF'
function randChar(seed) { return CH[seed % CH.length] }
function starStyle(i) {
  const s = (i * 89 + 13) % 100
  return { left: (s % 96) + '%', top: ((s * 7) % 92) + '%', animationDelay: (s % 30) / 10 + 's' }
}

onMounted(() => {
  ctx = gsap.context(() => {
    const panels = gsap.utils.toArray('.scene-panel')
    // 首幕之外全部藏起（clip 揭入由时间线控制）
    gsap.set(panels.slice(1), { clipPath: 'inset(100% 0% 0% 0%)' })
    gsap.set('.scene-progress', { autoAlpha: 0, y: 16 })

    const tl = gsap.timeline({
      defaults: { ease: 'none' },
      scrollTrigger: {
        trigger: stageEl.value,
        start: 'top top',
        end: () => '+=' + panels.length * 900,
        scrub: .5,
        pin: pinEl.value,
        anticipatePin: 1,
        invalidateOnRefresh: true,
        // 每一幕一个吸附位（labels 均匀分布）
        snap: { snapTo: 'labels', duration: .4, ease: 'power2.inOut', delay: .08 },
        onUpdate(self) {
          const idx = Math.min(scenes.length - 1, Math.round(self.progress * (scenes.length - 1)))
          if (idx !== active.value) active.value = idx
        },
      },
    })

    tl.to('.scene-progress', { autoAlpha: 1, y: 0, duration: .25 }, 0)

    for (let i = 1; i < panels.length; i++) {
      tl.addLabel('s' + i)
      // 上一幕退场：轻微下沉 + 模糊暗化
      tl.to(panels[i - 1], { scale: .94, filter: 'brightness(.45)', duration: .6 }, '<')
        .to(panels[i - 1].querySelectorAll('.scene-caption > *'), { y: -26, opacity: 0, stagger: .05, duration: .5 }, '<')
      // 下一幕揭入：自下而上 clip 展开 + 内容错峰回正
      tl.fromTo(panels[i], { clipPath: 'inset(100% 0% 0% 0%)' }, { clipPath: 'inset(0% 0% 0% 0%)', duration: .7 }, '<.1')
        .fromTo(panels[i].querySelectorAll('.scene-caption > *'),
          { y: 40, opacity: 0 }, { y: 0, opacity: 1, stagger: .07, duration: .45 }, '<.25')
    }
    tl.addLabel('end')
  }, stageEl.value)
})

onBeforeUnmount(() => ctx?.revert())
</script>

<style scoped>
.scenes-wrap { position: relative; }

.scene-viewport {
  position: relative;
  height: 100vh;
  overflow: hidden;
}

.scene-panel {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: #07071a;
  will-change: clip-path, transform, filter;
}

.scene-visual { position: absolute; inset: 0; }

.scene-caption {
  position: absolute;
  left: clamp(1.2rem, 6vw, 5rem);
  bottom: clamp(4.5rem, 14vh, 9rem);
  z-index: 3;
  max-width: min(520px, 80vw);
}
.sc-no {
  display: block;
  font-family: 'Courier New', monospace;
  font-size: .68rem;
  letter-spacing: .3em;
  color: #00d4ff;
  margin-bottom: .6rem;
}
.sc-title {
  font-size: clamp(1.8rem, 4.6vw, 3.2rem);
  font-weight: 800;
  line-height: 1.15;
  margin-bottom: .8rem;
  background: linear-gradient(135deg, #fff 30%, rgba(255,255,255,.62));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.sc-desc {
  font-size: clamp(.82rem, 1.3vw, .95rem);
  line-height: 1.85;
  color: rgba(255,255,255,.6);
}

/* ---- 进度指示 ---- */
.scene-progress {
  position: absolute;
  right: clamp(1rem, 4vw, 2.6rem);
  bottom: clamp(4.5rem, 14vh, 9rem);
  z-index: 4;
  display: flex;
  align-items: center;
  gap: .55rem;
}
.sp-dot {
  width: 22px; height: 4px;
  border-radius: 2px;
  border: 0;
  padding: 0;
  cursor: pointer;
  background: rgba(255,255,255,.16);
  transition: all .35s;
}
.sp-dot.on {
  background: #00d4ff;
  box-shadow: 0 0 10px rgba(0,212,255,.7);
}
.sp-count {
  margin-left: .4rem;
  font-family: 'Courier New', monospace;
  font-size: .68rem;
  letter-spacing: .15em;
  color: rgba(255,255,255,.5);
}

/* ---- 幕1 星云 ---- */
.neb-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  animation: nebFloat 9s ease-in-out infinite alternate;
}
.neb-orb.o1 { width: 46vw; height: 46vw; left: -8%; top: -12%; background: radial-gradient(circle, rgba(102,126,234,.5), transparent 65%); }
.neb-orb.o2 { width: 40vw; height: 40vw; right: -6%; bottom: -14%; background: radial-gradient(circle, rgba(118,75,162,.48), transparent 65%); animation-delay: -3s; }
.neb-orb.o3 { width: 24vw; height: 24vw; left: 36%; top: 30%; background: radial-gradient(circle, rgba(0,212,255,.32), transparent 65%); animation-delay: -6s; }
@keyframes nebFloat { to { transform: translate(4vw, 3vh) scale(1.12); } }

.neb-star {
  position: absolute;
  width: 2px; height: 2px;
  border-radius: 50%;
  background: #fff;
  opacity: .7;
  animation: starTw 3s ease-in-out infinite;
}
@keyframes starTw { 50% { opacity: .15; transform: scale(.6); } }

/* ---- 幕2 网格都市 ---- */
.gc-floor {
  position: absolute;
  left: -50%; right: -50%; bottom: 0;
  height: 55%;
  background:
    linear-gradient(rgba(0,212,255,.22) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,212,255,.22) 1px, transparent 1px);
  background-size: 56px 56px;
  transform: perspective(420px) rotateX(62deg);
  transform-origin: center top;
  animation: gcMove 3.2s linear infinite;
}
@keyframes gcMove { to { background-position-y: 56px; } }

.gc-skyline {
  position: absolute;
  left: 0; right: 0; bottom: 34%;
  height: 40%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: clamp(8px, 2vw, 26px);
  padding: 0 6vw;
}
.gc-bar {
  width: clamp(18px, 3.4vw, 46px);
  border-radius: 4px 4px 0 0;
  background: linear-gradient(180deg, rgba(102,126,234,.85), rgba(118,75,162,.25));
  box-shadow: 0 0 24px rgba(102,126,234,.35);
  animation: barGlow 2.6s ease-in-out infinite alternate;
}
.gc-bar:nth-child(odd) { animation-delay: -1.3s; }
@keyframes barGlow { to { filter: brightness(1.6); } }

/* ---- 幕3 数据流 ---- */
.st-col {
  position: absolute;
  top: -110%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-family: 'Courier New', monospace;
  font-size: .78rem;
  color: rgba(0,255,178,.75);
  text-shadow: 0 0 8px rgba(0,255,178,.5);
  writing-mode: vertical-rl;
  letter-spacing: .2em;
  animation: stFall linear infinite;
}
.st-col span:nth-child(3n) { color: rgba(0,212,255,.7); }
.st-col span:nth-child(4n) { opacity: .4; }
@keyframes stFall { to { top: 110%; } }

/* ---- 幕4 极光 ---- */
.au-band {
  position: absolute;
  left: -20%;
  right: -20%;
  height: 26vh;
  border-radius: 50%;
  filter: blur(46px);
  opacity: .8;
}
.au-band.b1 {
  top: 6%;
  background: linear-gradient(90deg, transparent, rgba(64,255,180,.5), rgba(0,212,255,.5), transparent);
  animation: auSway 7s ease-in-out infinite alternate;
}
.au-band.b2 {
  top: 26%;
  background: linear-gradient(90deg, transparent, rgba(118,75,162,.55), rgba(64,150,255,.45), transparent);
  animation: auSway 9s ease-in-out infinite alternate-reverse;
}
.au-band.b3 {
  top: 46%;
  background: linear-gradient(90deg, transparent, rgba(0,212,255,.4), rgba(160,100,255,.4), transparent);
  animation: auSway 11s ease-in-out infinite alternate;
}
@keyframes auSway {
  from { transform: translateX(-6%) skewX(-8deg); }
  to   { transform: translateX(8%) skewX(10deg); }
}

.scenes-tip {
  margin-top: 0;
  padding: 1rem 0 .4rem;
  text-align: center;
  font-size: .72rem;
  letter-spacing: .1em;
  color: rgba(255,255,255,.32);
}
</style>

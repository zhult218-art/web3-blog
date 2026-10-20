<template>
  <Teleport to="body">
    <div v-if="visible" class="inkloader" :class="{ 'is-open': opened }"
      role="status" aria-live="polite" aria-label="载入中">
      <!-- SVG 滤镜：feTurbulence 位移，让笔触带上毛笔的毛边与飞白 -->
      <svg class="ink-defs" aria-hidden="true" focusable="false">
        <defs>
          <filter id="inkRough" x="-25%" y="-25%" width="150%" height="150%">
            <feTurbulence type="fractalNoise" baseFrequency="0.016 0.024" numOctaves="2" seed="6" result="n" />
            <feDisplacementMap in="SourceGraphic" in2="n" scale="3.6"
              xChannelSelector="R" yChannelSelector="G" />
          </filter>
          <!-- 浓墨半圈：起笔焦黑、收笔渐淡，一条弧自带浓淡过渡 -->
          <linearGradient id="inkDark" x1="0.1" y1="1" x2="0.95" y2="0.05">
            <stop offset="0" stop-color="#0f1117" stop-opacity="1" />
            <stop offset="0.5" stop-color="#181a21" stop-opacity="0.94" />
            <stop offset="0.84" stop-color="#2b2d36" stop-opacity="0.62" />
            <stop offset="1" stop-color="#3a3c45" stop-opacity="0.3" />
          </linearGradient>
          <!-- 朱砂红：末笔长捺与「朱」字取同一色系。
               中段压到 #a52e1a，正是字形自身渐变落在末笔处的颜色 ——
               于是长捺压在原捺上时两处红调衔接得上，读作一笔写成，而不是两种红拼起来 -->
          <linearGradient id="zhuRed" gradientUnits="userSpaceOnUse" x1="140" y1="80" x2="260" y2="290">
            <stop offset="0" stop-color="#d9553a" />
            <stop offset="0.45" stop-color="#a52e1a" />
            <stop offset="1" stop-color="#7d2113" />
          </linearGradient>
        </defs>
      </svg>

      <!-- 整屏画卷两份，各自被裁掉一半：中缝天然对齐，展开时像卷轴向两侧卷开 -->
      <div v-for="side in ['l', 'r']" :key="side" class="ink-scene" :class="`ink-scene-${side}`"
        aria-hidden="true">
        <span class="ink-paper"></span>
        <span class="ink-wash w1"></span>
        <span class="ink-wash w2"></span>
        <span class="ink-wash w3"></span>
        <!-- 底景：雪舟《破墨山水图》（1481，公有领域）局部。
             取原画下段的山岩与飞白，以叠色压进纸面，上方用遮罩渐隐，只留一道水墨远山 -->
        <span class="ink-photo"></span>
      </div>

      <!-- 卷轴中缝：一道极淡的折痕线 -->
      <span class="ink-spine" aria-hidden="true"></span>

      <!-- 中央：墨环（太极双半，一黑一白，两笔同时起）包围朱字 -->
      <div class="ink-core">
        <div class="ink-mark">
          <span class="ink-ensobox">
            <svg class="ink-enso" viewBox="0 0 300 300" aria-hidden="true">
              <!-- 太极两半：同为顺时针 180°，各自从对顶点起笔，两笔笔尖同时行进，收笔时合为一整圈。
                   浓墨半 —— 实心黑；素白半 —— 宣纸白配一线淡墨轮廓，读作留白的那一半。
                   两笔各 351.86 单位弧长（π × 半径 112），dashoffset 由全长归零即成圆 -->
              <g>
                <!-- 浓墨半（左半幅，自下顶点顺时针向上） -->
                <path class="tj-ink"
                  d="M150 262 A112 112 0 0 1 150 38" />
                <!-- 素白半（右半幅，自上顶点顺时针向下）：先淡墨轮廓，再覆宣纸白 -->
                <path class="tj-pale-rim"
                  d="M150 38 A112 112 0 0 1 150 262" />
                <path class="tj-pale"
                  d="M150 38 A112 112 0 0 1 150 262" />
              </g>
              <!-- 末笔「捺」：自朱字中宫出锋，一路斜扫至环外 —— 长而重，是这枚记号的狂放处。
                   与字形的捺同向、且更宽，故覆盖原捺并向外延展，读作一笔写成的长捺 -->
              <path class="zhu-nata"
                d="M150 168 C176 178 208 192 232 204 C256 214 278 238 294 256 C276 252 254 242 232 232 C206 220 178 198 156 184 C150 178 148 172 150 168 Z" />
            </svg>
          </span>
          <span class="ink-char">朱</span>
        </div>
        <div class="ink-caption">
          <span class="ink-title">墨 韵 初 开</span>
          <span class="ink-rule"><i></i></span>
          <span class="ink-sub">AURORA · 极光数字空间</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
// ============================================================
// 全局开场加载层 · 水墨圆相
// 画面构成（全部落在宣纸底上，靠浓淡墨色说话）：
//   宣纸底 + 纸纤维颗粒 + 浓淡墨晕 + 雪舟《破墨山水图》局部作底景
//   中央：太极双半墨环（浓墨半 + 素白半，两笔同时起笔把「朱」字围住）→ 朱字 → 题字 + 朱线
// 时序：整页铺满 → 两笔同时落定成环、朱字收妥 → 应用就绪 → 画卷左右两半向两侧卷开，露出页面
// 仅在首次进入触发一次；路由切换交由页面过渡处理，不叠加整屏遮罩
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import router from '@/router'
import { useSiteSettingsStore } from '@/stores/modules/siteSettings'

const MIN_SHOW = 1800   // 最短展示时长：留出墨环两笔合围（约 1.6s）+ 朱字落定的时间
const OPEN_ANIM = 950   // 展开动画时长，与 CSS 过渡保持一致

const visible = ref(true)
const opened = ref(false)

let revealTimer = null
let finishTimer = null
let revealed = false

function reveal() {
  if (revealed) return
  revealed = true
  opened.value = true
  finishTimer = setTimeout(() => { visible.value = false }, OPEN_ANIM)
}

onMounted(async () => {
  // 接管 index.html 里的静态护屏，避免首帧出现空白/闪白
  document.getElementById('boot-shield')?.remove()

  const started = Date.now()
  // 等待：首页路由解析完成 + 站点基础配置到位（失败也不阻塞，超时兜底）
  await Promise.allSettled([
    router.isReady(),
    Promise.race([
      Promise.resolve(useSiteSettingsStore().load()),
      new Promise(r => setTimeout(r, 4000)),
    ]),
  ])
  const rest = Math.max(0, MIN_SHOW - (Date.now() - started))
  revealTimer = setTimeout(reveal, rest)
})

onBeforeUnmount(() => {
  clearTimeout(revealTimer)
  clearTimeout(finishTimer)
})
</script>

<style scoped>
.inkloader {
  position: fixed;
  inset: 0;
  z-index: 100000;
  overflow: hidden;
  /* 刻意不设背景色：画卷左右两半滑开时，底下已就绪的页面便从缝隙里渐次露出。
     若在此铺一层不透明底色，滑开的只是一层“幕布”，网站要等遮罩整体移除才出现 */
  pointer-events: auto;             /* 加载期间拦截点击，避免误触下层页面 */
}
.inkloader.is-open { pointer-events: none; }

/* 滤镜容器：不占布局，只提供 defs */
.ink-defs { position: absolute; width: 0; height: 0; }

/* ── 画卷：两份同源整屏画面，各裁一半，展开时向两侧卷开 ── */
.ink-scene {
  position: absolute;
  inset: 0;
  /* 冷调宣纸：偏灰的米白，比暖黄更「酷」，也更像旧纸 */
  background-image: linear-gradient(100deg, #e9e6dc 0%, #ddd8ca 26%, #c9c3b4 58%, #aeaa9c 84%, #9d9a8d 100%);
  transition: transform 0.95s cubic-bezier(0.76, 0, 0.24, 1);
  will-change: transform;
}
.ink-scene-l { clip-path: inset(0 50% 0 0); }
.ink-scene-r { clip-path: inset(0 0 0 50%); }
/* 包浆暗角：两份各画整幅的一份，合拢即完整，展开时随之卷走 */
.ink-scene::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(118% 104% at 50% 46%, transparent 50%, rgba(32, 30, 24, 0.2) 100%);
}
.inkloader.is-open .ink-scene-l { transform: translateX(-101%); }
.inkloader.is-open .ink-scene-r { transform: translateX(101%); }

/* 纸纤维颗粒：极细噪点做乘法叠加，破掉纯数字色块的塑料感 */
.ink-paper {
  position: absolute;
  inset: 0;
  opacity: 0.45;
  mix-blend-mode: multiply;
  background-repeat: repeat;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='200'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='200' height='200' filter='url(%23n)' opacity='0.35'/%3E%3C/svg%3E");
}

/* 底景水墨：整幅宽 200%（＝整屏宽），左右两半各取一半，合拢是一条完整的山岩。
   上方 0→40% 渐隐，让画与纸自然接续；multiply 让墨色融进纸纹而不是浮在上面 */
.ink-photo {
  position: absolute;
  bottom: 0;
  width: 200%;
  height: 52%;
  background-image: url('/images/ink-haboku.jpg');
  background-size: cover;
  background-position: center 42%;
  background-repeat: no-repeat;
  mix-blend-mode: multiply;
  opacity: 0.9;
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.55) 26%, #000 62%, #000 100%);
  mask-image: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.55) 26%, #000 62%, #000 100%);
}
.ink-scene-l .ink-photo { left: 0; }
.ink-scene-r .ink-photo { left: -100%; }

/* 淡墨晕：整块大范围晕开，只作氛围。整层极缓地明灭，让静止画面有呼吸 */
.ink-wash {
  position: absolute;
  mix-blend-mode: multiply;
  filter: blur(46px);
  background: radial-gradient(closest-side, rgba(28, 30, 38, 0.24), rgba(28, 30, 38, 0.08) 58%, transparent 82%);
  animation: washBreath 9s ease-in-out infinite alternate;
}
.ink-wash.w1 {
  width: 46%; height: 36%;
  top: -12%; left: -12%;
  border-radius: 60% 40% 52% 48% / 50% 62% 38% 50%;
}
.ink-wash.w2 {
  width: 42%; height: 32%;
  bottom: 0; right: -10%;
  border-radius: 46% 54% 38% 62% / 56% 44% 60% 40%;
  opacity: 0.8;
  animation-duration: 12s;
  animation-delay: -4s;
}
.ink-wash.w3 {
  width: 34%; height: 20%;
  top: 30%; left: 3%;
  opacity: 0.5;
  border-radius: 52% 48% 44% 56% / 60% 40% 58% 42%;
  animation-duration: 15s;
  animation-delay: -8s;
}
@keyframes washBreath {
  from { opacity: 0.62; }
  to   { opacity: 1; }
}

/* ── 卷轴中缝 ── */
.ink-spine {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 44px;
  transform: translateX(-50%);
  background: linear-gradient(90deg, transparent, rgba(38, 36, 30, 0.05) 40%, rgba(38, 36, 30, 0.1) 50%, rgba(38, 36, 30, 0.05) 60%, transparent);
  transition: opacity 0.5s ease;
}
.ink-spine::after {
  content: '';
  position: absolute;
  top: 4%;
  bottom: 4%;
  left: 50%;
  width: 1px;
  transform: translateX(-50%);
  background: linear-gradient(180deg, transparent, rgba(46, 42, 34, 0.26) 14%, rgba(46, 42, 34, 0.26) 86%, transparent);
}
.inkloader.is-open .ink-spine { opacity: 0; }

/* ── 中央主体 ── */
.ink-core {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 30px;
  /* 核心记号稍晚、稍慢地淡出：画卷已经动了它还在场，
     避免出现“中间先空、两半后开”的断档 */
  transition: opacity 0.8s ease 0.15s, transform 0.95s cubic-bezier(0.76, 0, 0.24, 1);
}
/* 中心留白：把圆心周围提亮，给墨环一个落处，也让画面透气 */
.ink-core::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(40% 40% at 50% 46%, rgba(255, 253, 248, 0.5), rgba(255, 253, 248, 0.16) 58%, transparent 80%);
}
.inkloader.is-open .ink-core {
  opacity: 0;
  transform: scale(1.08);
}

/* ── 环 + 字 ── */
/* 整组记号微微倾斜 3.5°，取书写的势；因为环、字、捺同在一组内，
   倾斜后彼此的相对位置不变，捺依然严丝合缝地覆在字形的捺上 */
.ink-mark {
  --ms: clamp(228px, 33vmin, 350px);
  position: relative;
  width: var(--ms);
  height: var(--ms);
  display: grid;
  place-items: center;
  transform: rotate(-3.5deg);
}
/* 落地影：给墨环一个“按在纸上”的座 */
.ink-mark::before {
  content: '';
  position: absolute;
  left: 14%;
  right: 14%;
  bottom: 5%;
  height: 40%;
  border-radius: 50%;
  background: radial-gradient(closest-side, rgba(64, 48, 36, 0.18), transparent 76%);
  filter: blur(14px);
}

.ink-ensobox {
  position: absolute;
  inset: 0;
  display: block;
}
.ink-enso { width: 100%; height: 100%; }

/* 两笔同时落：dasharray 取半圆弧长 351.86（π × 112），dashoffset 351.86 → 0 即成半圆。
   三条路径共用同一段动画、同一延时 —— 浓墨半与素白半的两处笔尖由对顶点同时出发、相向合围。
   基准样式即「已收笔」（dashoffset 0），动效只补上从无到有的过程：
   填充模式用 backwards，延迟期间停在 from 帧（未落笔），动画结束回归基准（可见）。
   即便动画被禁用或未播放，也只会呈现画好的整环，不会留下一圈空白 */
.tj-ink,
.tj-pale-rim,
.tj-pale {
  fill: none;
  stroke-dasharray: 351.86;
  stroke-dashoffset: 0;
  animation: tjDraw 1.6s cubic-bezier(0.42, 0.02, 0.3, 1) 0.3s backwards;
}
@keyframes tjDraw {
  from { stroke-dashoffset: 351.86; }
  to   { stroke-dashoffset: 0; }
}

/* 浓墨半：实心黑，渐变描边（起焦黑、收淡）+ 毛边 */
.tj-ink {
  stroke: url(#inkDark);
  stroke-width: 16;
  stroke-linecap: round;
  filter: url(#inkRough);
}
/* 素白半：先用一线淡墨勾出轮廓，再覆宣纸白 —— 于是这一半读作「留白」
   而不是一根灰线，与浓墨半形成真正的黑白各半 */
.tj-pale-rim {
  stroke: rgba(27, 29, 36, 0.34);
  stroke-width: 18;
  stroke-linecap: round;
  filter: url(#inkRough);
}
.tj-pale {
  stroke: #f4f1e8;
  stroke-width: 12.6;
  stroke-linecap: round;
}

/* 末笔长捺：与「朱」字同色系（朱砂红 #zhuRed，非墨黑），否则红字配黑末笔会读作断笔。
   不套毛边滤镜（位移会错开覆盖关系）、也不描边（描边会在覆盖交界处画出可见接缝），
   让长捺压在原捺上时红对红自然续上，读作一笔写成 */
.zhu-nata {
  fill: url(#zhuRed);
}

/* 「朱」字用的行书字体：龙藏行楷，只子集了这一个字形（不到 1KB，index.html 已 preload）。
   该字面仅常规一档字重，故由浏览器均匀加粗，换来与墨环相称的分量与更接近苏轼的重墨 */
@font-face {
  font-family: 'AuroraZhu';
  src: url('/fonts/longcang-zhu.woff2') format('woff2');
  font-display: block;
}

/* 「朱」字：放大了近一半，笔意更足；末笔的长捺由上面的 zhu-nata 接续出去。
   兜底栈里预留了「方正苏轼行书」等苏体/行楷族名 —— 本机若装了更贴近苏轼的字体，会优先取用 */
.ink-char {
  position: relative;
  font-family: 'AuroraZhu', 'Long Cang', '方正苏轼行书', 'FZSuShiXingShu', '华文行楷', 'STXingkai', 'Xingkai SC', 'KaiTi', 'STKaiti', serif;
  font-size: calc(var(--ms) * 0.44);
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.01em;
  background: linear-gradient(158deg, #e66247 0%, #c23a22 44%, #8f2413 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #bf3423;                              /* 不支持 background-clip: text 时的兜底 */
  filter: drop-shadow(0 2px 1.5px rgba(104, 34, 22, 0.26));
  /* 等两笔合围到七八成时才落字，像写完一圈再在圆心盖章。
     同样用 backwards：延迟期间不可见，动画结束后回归基准（自然可见） */
  animation: charIn 0.72s cubic-bezier(0.22, 1.3, 0.4, 1) 1.4s backwards;
}
@keyframes charIn {
  from { opacity: 0; transform: scale(1.28); }
  to   { opacity: 1; transform: scale(1); }
}

/* ── 题字与落款 ── */
.ink-caption {
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: inkFade 0.9s ease 1.55s backwards;
}
.ink-title {
  font-family: 'STZhongsong', 'Songti SC', 'SimSun', serif;
  font-size: 15px;
  letter-spacing: 0.5em;
  text-indent: 0.5em;
  color: #212127;
}
/* 落款朱线：由淡墨底线 + 朱红笔锋扫过，兼作进度提示 */
.ink-rule {
  position: relative;
  display: block;
  width: 140px;
  height: 2px;
  margin: 13px 0 12px;
  background: rgba(33, 33, 39, 0.13);
  overflow: hidden;
}
.ink-rule i {
  display: block;
  height: 100%;
  width: 100%;
  transform-origin: left center;
  background: linear-gradient(90deg, rgba(168, 49, 36, 0.15), #a83124 88%);
  animation: inkSweep 1.7s cubic-bezier(0.62, 0, 0.3, 1) 0.3s backwards;
}
@keyframes inkSweep {
  from { transform: scaleX(0); }
  to   { transform: scaleX(1); }
}
.ink-sub {
  font-size: 10px;
  letter-spacing: 0.34em;
  color: rgba(33, 33, 39, 0.5);
  font-family: 'Courier New', monospace;
}
@keyframes inkFade {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

@media (max-width: 640px) {
  .ink-photo { height: 44%; background-position: center 55%; }
  .ink-title { font-size: 13px; letter-spacing: 0.4em; }
}

@media (prefers-reduced-motion: reduce) {
  .ink-scene, .ink-core, .ink-spine { transition-duration: 0.35s; }
  .ink-wash, .ink-char, .ink-caption, .ink-rule i,
  .tj-ink, .tj-pale-rim, .tj-pale { animation: none; }
  .ink-rule i { transform: scaleX(1); }
}
</style>

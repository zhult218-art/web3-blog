<template>
  <Teleport to="body">
    <div v-if="visible" class="inkloader" :class="{ 'is-open': opened }"
      role="status" aria-live="polite" aria-label="载入中">
      <!-- SVG 滤镜：feTurbulence 位移，让笔触带上毛笔的毛边与飞白 -->
      <svg class="ink-defs" aria-hidden="true" focusable="false">
        <defs>
          <filter id="inkRough" x="-25%" y="-25%" width="150%" height="150%">
            <feTurbulence type="fractalNoise" baseFrequency="0.016 0.024" numOctaves="2" seed="6" result="n" />
            <feDisplacementMap in="SourceGraphic" in2="n" scale="3.2"
              xChannelSelector="R" yChannelSelector="G" />
          </filter>
          <!-- 朱字专用：频率更低、位移更大，边缘才会被撕出毛笔的锯齿与飞白，
               而不是一圈均匀的毛边（scale 小＝只是模糊，大才叫「写」） -->
          <!-- 书写遮罩：沿椭圆中线的一条粗白线，靠 stroke-dashoffset 从无到有地描一遍。
               白线比笔触更宽，所以描到哪儿、笔触就露到哪儿 —— 这就是「写」的过程。
               之前用锥形扫描遮罩（conic-gradient + @property）实测在 Chrome 里不做插值，
               环会到点突然整圈出现，等于没有书写动效 -->
          <mask id="ringMask" maskUnits="userSpaceOnUse" x="-40" y="-40" width="380" height="380">
            <path class="mask-line" d="M90.4 242.9 L85.8 241.0 L81.4 238.9 L77.0 236.6 L72.9 234.2 L68.8 231.6 L65.0 228.9 L61.3 226.0 L57.7 222.9 L54.4 219.7 L51.2 216.4 L48.3 212.9 L45.5 209.3 L42.9 205.6 L40.6 201.8 L38.4 197.9 L36.5 193.9 L34.8 189.8 L33.3 185.6 L32.1 181.4 L31.1 177.0 L30.3 172.7 L29.7 168.3 L29.4 163.9 L29.4 159.4 L29.5 154.9 L29.9 150.4 L30.5 145.9 L31.4 141.4 L32.5 137.0 L33.8 132.5 L35.3 128.1 L37.1 123.8 L39.1 119.4 L41.3 115.2 L43.7 111.0 L46.4 106.9 L49.2 102.9 L52.2 98.9 L55.5 95.1 L58.9 91.4 L62.5 87.8 L66.2 84.3 L70.1 80.9 L74.2 77.7 L78.4 74.6 L82.8 71.7 L87.3 68.9 L91.9 66.3 L96.6 63.8 L101.5 61.5 L106.4 59.4 L111.4 57.4 L116.5 55.7 L121.7 54.1 L126.9 52.7 L132.1 51.5 L137.4 50.5 L142.7 49.7 L148.1 49.1 L153.4 48.6 L158.7 48.4 L164.0 48.4 L169.3 48.6 L174.6 48.9 L179.8 49.5 L184.9 50.3 L190.0 51.2 L195.0 52.4 L199.9 53.7 L204.7 55.3 L209.4 57.0 L214.0 58.9 L218.4 61.0 L222.7 63.2 L226.9 65.6 L231.0 68.2 L234.8 71.0 L238.5 73.9 L242.1 76.9 L245.4 80.1 L248.6 83.5 L251.6 86.9 L254.4 90.5 L256.9 94.2 L259.3 98.0 L261.5 101.9 L263.4 105.9 L265.1 110.0 L266.6 114.2 L267.8 118.4 L268.9 122.7 L269.7 127.1 L270.2 131.5 L270.6 135.9 L270.7 140.4 L270.5 144.9 L270.1 149.3 L269.5 153.8 L268.7 158.3 L267.6 162.8 L266.3 167.2 L264.7 171.6 L263.0 176.0 L261.0 180.3 L258.8 184.6 L256.4 188.8 L253.8 192.9 L250.9 196.9 L247.9 200.9 L244.7 204.7 L241.3 208.4 L237.7 212.0 L234.0 215.5 L230.1 218.9 L226.0 222.1 L221.8 225.2 L217.4 228.2 L213.0 231.0 L208.3 233.6 L203.6 236.1" fill="none" stroke="#fff"
              stroke-width="46" stroke-linecap="round" />
          </mask>
          <!-- 笔触专用：位移更大，填充块的轮廓才会被撕出毛笔的毛边与缺口 -->
          <filter id="ringRough" x="-22%" y="-22%" width="144%" height="144%">
            <feTurbulence type="fractalNoise" baseFrequency="0.019 0.028" numOctaves="3" seed="7" result="n" />
            <feDisplacementMap in="SourceGraphic" in2="n" scale="5.2"
              xChannelSelector="R" yChannelSelector="G" />
          </filter>
          <filter id="zhuRough" x="-34%" y="-34%" width="168%" height="168%">
            <feTurbulence type="fractalNoise" baseFrequency="0.014 0.018" numOctaves="2" seed="11" result="n" />
            <feDisplacementMap in="SourceGraphic" in2="n" scale="2.6"
              xChannelSelector="R" yChannelSelector="G" result="d" />
            <!-- 落纸投影放在位移之后，才不会被一起揉碎 -->
            <feDropShadow in="d" dx="0" dy="2.4" stdDeviation="1.6" flood-color="#6b2414" flood-opacity="0.3" />
          </filter>
          <!-- 墨色：起笔焦黑、行笔饱满、收笔拖成淡灰的枯锋。
               纯黑白，不掺色相 —— 让「墨」自己说话 -->
          <linearGradient id="brushInk" gradientUnits="userSpaceOnUse" x1="70" y1="250" x2="248" y2="62">
            <stop offset="0" stop-color="#0a0a0b" />
            <stop offset="0.38" stop-color="#121213" />
            <stop offset="0.72" stop-color="#232325" />
            <stop offset="1" stop-color="#4a4a4d" />
          </linearGradient>
          <!-- 字用更实的一档：中心近墨黑，边缘略提，避免整块死黑 -->
          <linearGradient id="zhuInk" gradientUnits="userSpaceOnUse" x1="96" y1="70" x2="212" y2="238">
            <stop offset="0" stop-color="#08080a" />
            <stop offset="0.55" stop-color="#111113" />
            <stop offset="1" stop-color="#26262a" />
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
             整幅按等比缩放落在底部，左右两半各取一半，合拢是一条完整的水墨远山 -->
        <span class="ink-photo"></span>
        <!-- 山脚一层极淡的雾气，把底图与纸面接起来 -->
        <span class="ink-mist"></span>
      </div>

      <!-- 卷轴中缝：只留一道极淡的折痕阴影，不画线（画线会像一道划痕） -->
      <span class="ink-spine" aria-hidden="true"></span>

      <!-- 中央：一笔写成的墨环，围住朱字 -->
      <div class="ink-core">
        <div class="ink-mark">
          <span class="ink-ensobox">
            <svg class="ink-enso" viewBox="0 0 300 300" aria-hidden="true">
              <!-- 笔刷环：不是「等宽描边绕一圈」，而是一条真正有粗细变化的笔触 ——
                   外沿与内沿各走一遍椭圆弧，围成一条带宽随角度变化的闭合带；
                   起笔是一条发丝细的枯锋，中段饱满，收笔前聚成一个墨团再拖尖。
                   椭圆整体倾斜 -13°（字仍正立），弧度 302°，左下留缺口。
                   路径由参数方程生成（rx112 / ry104 / 带宽 1.1→25→1.1），
                   改半径或倾角时重新生成即可，不要手改这些点 -->
              <path class="brush-ring" mask="url(#ringMask)" d="M90.3 243.1 L84.7 240.9 L79.3 238.5 L74.1 235.8 L69.0 232.9 L64.2 229.7 L59.6 226.3 L55.2 222.7 L51.1 218.8 L47.2 214.7 L43.7 210.5 L40.4 206.0 L37.4 201.4 L34.7 196.6 L32.3 191.6 L30.3 186.6 L28.5 181.3 L27.2 176.0 L26.1 170.6 L25.4 165.1 L25.1 159.6 L25.1 153.9 L25.4 148.3 L26.2 142.6 L27.2 137.0 L28.6 131.3 L30.4 125.7 L32.5 120.2 L34.9 114.7 L37.7 109.2 L40.8 103.9 L44.3 98.7 L48.0 93.6 L52.0 88.7 L56.3 83.9 L60.9 79.3 L65.8 74.9 L70.9 70.7 L76.3 66.7 L81.9 62.9 L87.7 59.3 L93.7 56.1 L99.8 53.0 L106.1 50.3 L112.6 47.8 L119.2 45.6 L125.9 43.7 L132.6 42.1 L139.5 40.8 L146.3 39.8 L153.2 39.2 L160.1 38.8 L167.0 38.8 L173.8 39.1 L180.6 39.7 L187.3 40.7 L193.9 41.9 L200.4 43.5 L206.8 45.4 L212.9 47.6 L218.9 50.0 L224.8 52.8 L230.4 55.9 L235.7 59.2 L240.8 62.8 L245.7 66.6 L250.3 70.7 L254.6 75.0 L258.5 79.6 L262.2 84.3 L265.5 89.2 L268.5 94.3 L271.2 99.5 L273.5 104.9 L275.4 110.4 L277.0 116.0 L278.1 121.7 L278.9 127.4 L279.4 133.2 L279.5 139.0 L279.2 144.9 L278.7 150.7 L277.9 156.6 L277.0 162.5 L276.0 168.4 L274.9 174.4 L273.4 180.5 L271.3 186.4 L268.3 192.1 L264.4 197.3 L259.7 202.1 L254.5 206.4 L249.1 210.5 L243.7 214.3 L238.3 218.1 L232.9 221.8 L227.4 225.3 L221.8 228.6 L216.0 231.6 L210.0 234.3 L203.7 236.3 L203.5 235.9 L208.5 231.9 L213.6 228.1 L218.4 224.3 L223.0 220.2 L227.3 216.0 L231.2 211.6 L234.7 206.9 L237.7 201.9 L240.1 196.9 L242.2 191.8 L244.2 186.8 L246.4 182.1 L249.0 177.6 L251.8 173.3 L254.6 168.8 L257.1 164.3 L259.1 159.5 L260.5 154.7 L261.4 149.8 L261.8 144.8 L261.8 139.9 L261.5 135.1 L260.9 130.2 L260.0 125.5 L258.7 120.9 L257.2 116.3 L255.4 111.8 L253.3 107.5 L251.0 103.3 L248.3 99.2 L245.4 95.3 L242.3 91.5 L238.9 87.9 L235.3 84.4 L231.4 81.1 L227.3 78.1 L223.0 75.2 L218.5 72.5 L213.8 70.0 L209.0 67.7 L203.9 65.7 L198.8 63.9 L193.5 62.3 L188.0 61.0 L182.5 59.9 L176.8 59.0 L171.1 58.4 L165.3 58.1 L159.4 58.0 L153.6 58.1 L147.7 58.5 L141.7 59.1 L135.8 60.0 L130.0 61.2 L124.1 62.6 L118.3 64.2 L112.6 66.1 L107.0 68.2 L101.5 70.6 L96.1 73.2 L90.9 76.0 L85.8 79.0 L80.8 82.2 L76.0 85.6 L71.5 89.2 L67.1 93.0 L62.9 97.0 L59.0 101.2 L55.4 105.4 L51.9 109.9 L48.8 114.4 L45.9 119.1 L43.3 123.9 L41.0 128.8 L39.0 133.7 L37.3 138.7 L35.9 143.8 L34.8 148.9 L34.0 154.1 L33.6 159.2 L33.5 164.4 L33.8 169.5 L34.3 174.6 L35.2 179.6 L36.4 184.6 L38.0 189.5 L39.8 194.4 L42.0 199.1 L44.5 203.7 L47.3 208.1 L50.5 212.4 L53.9 216.6 L57.5 220.6 L61.5 224.4 L65.7 228.0 L70.2 231.4 L75.0 234.6 L79.9 237.6 L85.1 240.3 L90.5 242.7 Z" />
              <!-- 收笔处的三点溅墨：落在环外，不干扰字形 -->
              <circle class="ink-speck s1" cx="226" cy="254" r="3" />
              <circle class="ink-speck s2" cx="239" cy="241" r="1.7" />
              <circle class="ink-speck s3" cx="215" cy="263" r="1.2" />
              <!-- 「朱」字直接画在 SVG 里：fill 取 zhuInk 渐变，正立。
                   不走 CSS 的 background-clip:text —— 那套在滤镜/合成层上下文里会把文字
                   渲染成半透明色叠在透明黑底上（实测整体暗掉约七成） -->
              <text class="zhu-t" x="150" y="150" text-anchor="middle" dominant-baseline="central"
                filter="url(#zhuRough)">朱</text>
            </svg>
          </span>
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
//   宣纸底 + 纸纤维颗粒 + 浓淡墨晕 + 雪舟《破墨山水图》作底部远山
//   中央：一笔成环的墨相 → 朱字 → 题字 + 朱线进度
// 时序：整页铺满 → 墨环落笔、朱字收妥 → 应用就绪 → 画卷左右两半向两侧卷开，露出页面
// 仅在首次进入触发一次；路由切换交由页面过渡处理，不叠加整屏遮罩
// ============================================================
import { ref, onMounted, onBeforeUnmount } from 'vue'
import router from '@/router'
import { useSiteSettingsStore } from '@/stores/modules/siteSettings'

const MIN_SHOW = 2000   // 最短展示时长：墨环落笔 → 朱字落定 → 题字浮现，收妥后再定格一拍
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
  background: radial-gradient(120% 106% at 50% 44%, transparent 46%, rgba(32, 30, 24, 0.24) 100%);
}
.inkloader.is-open .ink-scene-l { transform: translateX(-101%); }
.inkloader.is-open .ink-scene-r { transform: translateX(101%); }

/* 纸纤维颗粒：极细噪点做乘法叠加，破掉纯数字色块的塑料感 */
.ink-paper {
  position: absolute;
  inset: 0;
  opacity: 0.42;
  mix-blend-mode: multiply;
  background-repeat: repeat;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='200'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='200' height='200' filter='url(%23n)' opacity='0.32'/%3E%3C/svg%3E");
}

/* 底景水墨：两份画卷里各画一张位置完全相同的底图，左右各裁一半后天然对齐 ——
   于是整幅山水在屏幕上仍是 1:1，不会被拉伸成两倍宽。
   cover + bottom 对齐：底图几乎完整落在屏幕下缘，只裁掉顶部一线留白；
   上方渐隐让画与纸自然接续；multiply 让墨色融进纸纹而不是浮在上面 */
.ink-photo {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 62%;
  background-image: url('/images/ink-haboku.jpg');
  background-size: cover;
  /* 主景偏右：墨块落在画面右下方，与居中的墨环形成虚实对照，而不是糊在正中间 */
  background-position: 58% bottom;
  background-repeat: no-repeat;
  mix-blend-mode: multiply;
  opacity: 0.38;
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.22) 22%, rgba(0, 0, 0, 0.8) 58%, #000 100%);
  mask-image: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.22) 22%, rgba(0, 0, 0, 0.8) 58%, #000 100%);
}

/* 山脚雾气：一层横向的浅色渐隐，压住底图与纸面的接缝 */
.ink-mist {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 16%;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(226, 223, 212, 0) 0%, rgba(226, 223, 212, 0.22) 60%, rgba(226, 223, 212, 0.3) 100%);
}

/* 淡墨晕：整块大范围晕开，只作氛围。整层极缓地明灭，让静止画面有呼吸 */
.ink-wash {
  position: absolute;
  mix-blend-mode: multiply;
  filter: blur(52px);
  background: radial-gradient(closest-side, rgba(28, 30, 38, 0.26), rgba(28, 30, 38, 0.08) 58%, transparent 82%);
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
  opacity: 0.45;
  border-radius: 52% 48% 44% 56% / 60% 40% 58% 42%;
  animation-duration: 15s;
  animation-delay: -8s;
}
@keyframes washBreath {
  from { opacity: 0.62; }
  to   { opacity: 1; }
}

/* ── 卷轴中缝：只有一层极淡的折痕阴影，不画实线 ── */
.ink-spine {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 70px;
  transform: translateX(-50%);
  background: linear-gradient(90deg, transparent, rgba(38, 36, 30, 0.022) 42%, rgba(38, 36, 30, 0.05) 50%, rgba(38, 36, 30, 0.022) 58%, transparent);
  transition: opacity 0.5s ease;
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
  gap: 34px;
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
  background: radial-gradient(36% 36% at 50% 45%, rgba(255, 253, 248, 0.4), rgba(255, 253, 248, 0.12) 58%, transparent 80%);
}
.inkloader.is-open .ink-core {
  opacity: 0;
  transform: scale(1.08);
}

/* ── 环 + 字 ── */
/* 整组记号微微倾斜 3.5°，取书写的势 */
.ink-mark {
  --ms: clamp(236px, 36vmin, 380px);
  position: relative;
  width: var(--ms);
  height: var(--ms);
  display: grid;
  place-items: center;
  /* 这里不再整体旋转：环的倾斜已经烘进路径里，字必须保持正立 */
}
/* 落地影：给墨环一个“按在纸上”的座 */
.ink-mark::before {
  content: '';
  position: absolute;
  left: 16%;
  right: 16%;
  bottom: 6%;
  height: 38%;
  border-radius: 50%;
  background: radial-gradient(closest-side, rgba(64, 48, 36, 0.14), transparent 76%);
  filter: blur(16px);
}

.ink-ensobox {
  position: absolute;
  inset: 0;
  display: block;
}
.ink-enso { width: 100%; height: 100%; overflow: visible; }

/* 笔触本体：实心填充 + 毛边位移滤镜。带宽由路径自身决定，不再靠 stroke-width */
.brush-ring {
  fill: url(#brushInk);
  filter: url(#ringRough);
}

/* 书写动效：遮罩里的白线沿中线描一遍（弧长 582.4），描到哪儿笔触露到哪儿。
   基准即「已写完」（offset 0）；backwards 让延迟期间停在 from 帧（还没落笔），
   动画结束回归基准。这套 dashoffset 的写法在老版本里已经验证过，比锥形遮罩稳 */
.mask-line {
  stroke-dasharray: 582.4;
  stroke-dashoffset: 0;
  animation: ringDraw 1.15s cubic-bezier(0.42, 0.02, 0.3, 1) 0.2s backwards;
}
@keyframes ringDraw {
  from { stroke-dashoffset: 582.4; }
  to   { stroke-dashoffset: 0; }
}

/* 收笔飞白：三点溅墨，落笔末了才出现，给环一个「写完」的尾音。
   SVG 里 transform-origin 默认落在用户坐标系原点，故显式改到自身包围盒中心，
   否则缩放动画会让墨点从画面左上角飞入 */
.ink-speck {
  fill: rgba(24, 26, 33, 0.7);
  transform-box: fill-box;
  transform-origin: center;
  animation: speckIn 0.45s ease 1.3s backwards;
}
.ink-speck.s2 { fill: rgba(24, 26, 33, 0.5); }
.ink-speck.s3 { fill: rgba(24, 26, 33, 0.36); }
@keyframes speckIn {
  from { opacity: 0; transform: scale(0.4); }
  to   { opacity: 1; transform: scale(1); }
}

/* 「朱」字用的楷体：霞鹜文楷（LXGW WenKai Lite，OFL 授权），只子集了这一个字形，
   不到 1KB，index.html 已 preload。楷体笔画起收分明，比行书更「正」，
   与倾斜的笔刷环形成动静对照 */
@font-face {
  font-family: 'AuroraZhu';
  src: url('/fonts/lxgw-zhu.woff2') format('woff2');
  font-display: block;
}

/* 「朱」字：正立的楷体，落在环心 —— 环斜、字正，一张一弛。
   取环径的七成左右（font-size 148 / 画布 300），笔画边缘靠 #zhuRough 轻微撕开，
   让它像写上去的，而不是贴上去的一个字。
   兜底栈里留了各平台的楷体族名，字体没加载到时也不会掉成黑体 */
.zhu-t {
  font-family: 'AuroraZhu', 'Kaiti SC', 'KaiTi', 'STKaiti', '楷体', 'LXGW WenKai', serif;
  font-size: 142px;
  font-weight: 400;
  fill: url(#zhuInk);
  /* SVG 里 transform-origin 默认落在用户坐标系原点，改到自身包围盒中心，
     缩放才是「原地砸下去」而不是从画布左上角飞进来 */
  transform-box: fill-box;
  transform-origin: center;
  /* 等环写到七八成时才落字，像写完一圈再在圆心落笔。
     落笔是「砸」下去的：从大到小带一次回弹，收在正常尺寸。
     同样用 backwards：延迟期间不可见，动画结束后回归基准（自然可见） */
  animation: charIn 0.7s cubic-bezier(0.2, 1.5, 0.36, 1) 0.95s backwards;
}
@keyframes charIn {
  from { opacity: 0; transform: scale(1.55); }
  62%  { opacity: 1; transform: scale(0.95); }
  to   { opacity: 1; transform: scale(1); }
}

/* ── 题字与落款 ── */
.ink-caption {
  display: flex;
  flex-direction: column;
  align-items: center;
  /* 0.6s / 1.15s 起：留 250ms 的静止，让题字落定后再卷开画卷，
     不至于刚浮出来就被切走 */
  animation: inkFade 0.6s ease 1.15s backwards;
}
.ink-title {
  font-family: 'STZhongsong', 'Songti SC', 'SimSun', serif;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.42em;
  text-indent: 0.42em;
  color: #1c1c22;
}
/* 落款细线：淡墨底线 + 浓墨笔锋扫过，兼作进度提示（整幅黑白，不掺朱红） */
.ink-rule {
  position: relative;
  display: block;
  width: 148px;
  height: 2px;
  margin: 14px 0 13px;
  background: rgba(33, 33, 39, 0.11);
  overflow: hidden;
}
.ink-rule i {
  display: block;
  height: 100%;
  width: 100%;
  transform-origin: left center;
  background: linear-gradient(90deg, rgba(20, 20, 24, 0.10), #14141a 88%);
  animation: inkSweep 1.5s cubic-bezier(0.62, 0, 0.3, 1) 0.25s backwards;
}
@keyframes inkSweep {
  from { transform: scaleX(0); }
  to   { transform: scaleX(1); }
}
.ink-sub {
  font-size: 10.5px;
  letter-spacing: 0.3em;
  text-indent: 0.3em;
  color: rgba(33, 33, 39, 0.46);
  font-family: 'Courier New', monospace;
}
@keyframes inkFade {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

@media (max-width: 640px) {
  /* 窄屏改用 cover 并压淡：等比 108% 会把画放大到只剩右下一团墨块，
     在手机上读作「一块污渍」而不是远山 */
  .ink-photo {
    height: 44%;
    background-size: cover;
    background-position: 72% bottom;
    opacity: 0.3;
  }
  .ink-title { font-size: 14px; letter-spacing: 0.34em; text-indent: 0.34em; }
  .ink-sub { font-size: 9.5px; letter-spacing: 0.24em; }
}

@media (prefers-reduced-motion: reduce) {
  .ink-scene, .ink-core, .ink-spine { transition-duration: 0.35s; }
  .ink-wash, .zhu-t, .ink-caption, .ink-rule i, .ink-speck,
  .mask-line { animation: none; }
  .ink-rule i { transform: scaleX(1); }
}
</style>

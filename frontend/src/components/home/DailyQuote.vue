<template>
  <!-- 每日一句打字机：每日随机一句哲理/诗句，逐字打出，带作者署名 -->
  <div class="daily-quote">
    <span class="quote-mark">❝</span>
    <span class="typed-text">{{ displayed }}</span>
    <span class="cursor" v-if="typing">|</span>
    <span class="quote-mark close">❞</span>
    <span v-if="done" class="author">—— {{ quote.author }}</span>
  </div>
</template>

<script setup>
// ============================================================
// 每日一句打字机：按日期取一句，逐字打出后显示作者
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getDailyQuote } from '@/utils/dailyQuotes'

const quote = ref(getDailyQuote())
const displayed = ref('')
const typing = ref(true)
const idx = ref(0)
let timer = 0

const done = computed(() => idx.value >= quote.value.text.length && !typing.value)

// 逐字打印：每个字 60~120ms 随机，更自然
function typeLoop() {
  const text = quote.value.text
  if (idx.value < text.length) {
    displayed.value += text[idx.value]
    idx.value++
    const delay = 60 + Math.random() * 60
    timer = setTimeout(typeLoop, delay)
  } else {
    typing.value = false
  }
}

onMounted(() => {
  // 等首屏动画稍起再开始打字
  timer = setTimeout(typeLoop, 1200)
})

onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
.daily-quote {
  position: relative;
  max-width: 660px;
  margin: 1.2rem auto 0;
  padding: 0.7rem 1.4rem;
  text-align: center;
  font-size: 1.02rem;
  line-height: 1.95;
  color: rgba(255, 255, 255, 0.92);
  /* 字形级暗投影：诗句是渐变透明填充，压在浅色衣料上需要一层分离度。
     放在父级才能保住渐变（见 .typed-text 注释） */
  filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.75))
          drop-shadow(0 0 10px rgba(139, 92, 246, 0.28));
  letter-spacing: 0.05em;
  /* 柔和暗底光晕：任何壁纸下都保证诗句可读，且无生硬边框 */
  background: radial-gradient(ellipse 92% 88% at 50% 45%,
    rgba(5, 7, 20, 0.55) 0%, rgba(5, 7, 20, 0.28) 55%, transparent 78%);
  border-radius: 1.1rem;
  animation: fadeIn 1.4s ease-out 0.6s both;
}

.typed-text {
  background: linear-gradient(120deg, #ddd6fe 0%, #c4b5fd 28%, #a5f3fc 62%, #fbcfe8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  /* 投影挂在父级 .daily-quote 上（见下）：直接给 background-clip:text 的元素加 filter，
     Chrome 会退回用 color 填充，渐变整个失效 —— 诗句会变成一片灰白 */
  font-weight: 500;
}

.quote-mark {
  font-size: 1.2rem;
  color: rgba(196, 181, 253, 0.92);
  vertical-align: super;
  margin: 0 2px;
  filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.7));
}
.quote-mark.close { vertical-align: sub; }

.cursor {
  display: inline-block;
  color: #a5f3fc;
  animation: blink 0.8s steps(2) infinite;
  margin-left: 2px;
  text-shadow: 0 0 8px rgba(165, 243, 252, 0.7);
}

.author {
  display: block;
  margin-top: 0.55rem;
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.76);
  letter-spacing: 0.14em;
  font-family: 'Courier New', monospace;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.75);
  animation: fadeIn 0.8s ease-out;
}

/* 手机端：略收字号与留白，保持单行不拥挤 */
@media (max-width: 640px) {
  .daily-quote {
    max-width: 92vw;
    font-size: 0.94rem;
    line-height: 1.85;
    padding: 0.6rem 0.9rem;
  }
  .author { font-size: 0.74rem; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

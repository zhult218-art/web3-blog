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
  max-width: 640px;
  margin: 1.2rem auto 0;
  text-align: center;
  font-size: 0.92rem;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.78);
  letter-spacing: 0.05em;
  animation: fadeIn 1.4s ease-out 0.6s both;
}

.typed-text {
  background: linear-gradient(120deg, #a78bfa, #67e8f9, #f9a8d4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.quote-mark {
  font-size: 1.1rem;
  color: rgba(167, 139, 250, 0.6);
  vertical-align: super;
  margin: 0 2px;
}
.quote-mark.close { vertical-align: sub; }

.cursor {
  display: inline-block;
  color: #67e8f9;
  animation: blink 0.8s steps(2) infinite;
  margin-left: 2px;
}

.author {
  display: block;
  margin-top: 0.5rem;
  font-size: 0.72rem;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 0.12em;
  font-family: 'Courier New', monospace;
  animation: fadeIn 0.8s ease-out;
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

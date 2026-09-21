<template>
  <div class="welcome-greeting">
    <span class="greet-prefix">👋 你好，</span>
    <span class="greet-location" v-if="location">来自 <b>{{ location }}</b> 的</span>
    <span class="greet-nickname">{{ nickname }}</span>
    <span class="greet-cursor" v-if="typing">|</span>
  </div>
</template>

<script setup>
// ============================================================
// 首页问候语：获取用户 IP 归属地 + 昵称，逐字打出"你好，来自XX的XX"
// IP 归属地走免费接口 ipapi.co；昵称取登录用户，未登录显示"访客"
// ============================================================
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/modules/auth'

const auth = useAuthStore()
const location = ref('')
const nickname = ref('')
const typing = ref(true)
const displayed = ref('')
let timer = 0

function getNickname() {
  const u = auth.user
  if (u) {
    return u.nickname || u.username || u.name || '冒险者'
  }
  return '访客'
}

// 逐字打字
function typeText(full) {
  let i = 0
  const tick = () => {
    if (i < full.length) {
      displayed.value = full.slice(0, i + 1)
      i++
      timer = setTimeout(tick, 45)
    } else {
      typing.value = false
    }
  }
  tick()
}

onMounted(async () => {
  nickname.value = getNickname()
  // 获取 IP 归属地（并行，不阻塞太久）
  let ipLocation = ''
  try {
    const res = await Promise.race([
      fetch('https://ipwho.is/').then(r => r.json()),
      new Promise((_, rej) => setTimeout(() => rej(new Error('timeout')), 3000))
    ])
    if (res && res.success && res.city) {
      const region = res.region && res.region !== res.city ? `${res.region}·` : ''
      ipLocation = `${region}${res.city}`
    } else if (res && res.country) {
      ipLocation = res.country
    }
  } catch {
    // 超时或失败则不显示归属地
  }
  location.value = ipLocation
  // 延迟 800ms 等首屏动画起来后开始打字
  setTimeout(() => {
    const full = `你好，${ipLocation ? '来自' + ipLocation + '的' : ''}${nickname.value}`
    typeText(full)
  }, 800)
})
</script>

<style scoped>
.welcome-greeting {
  margin: 0 auto 0.4rem;
  text-align: center;
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.04em;
  min-height: 1.5em;
}
.greet-prefix { color: rgba(255, 255, 255, 0.6); }
.greet-location b {
  color: #67e8f9;
  font-weight: 600;
  text-shadow: 0 0 10px rgba(103, 232, 249, 0.4);
}
.greet-nickname {
  color: #f9a8d4;
  font-weight: 600;
  text-shadow: 0 0 10px rgba(249, 168, 212, 0.4);
}
.greet-cursor {
  display: inline-block;
  color: #67e8f9;
  animation: blink 0.8s steps(2) infinite;
  margin-left: 1px;
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>

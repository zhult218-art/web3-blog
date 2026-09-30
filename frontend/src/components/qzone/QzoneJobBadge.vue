<template>
  <transition name="qzb">
    <div v-if="visible" class="qzb-wrap" @click="goPage">
      <!-- 90s 终端风迷你进度窗 -->
      <div class="qzb-bar">
        <span class="qzb-dot"></span>
        <span class="qzb-title">QZONE://BACKUP</span>
        <button class="qzb-x" @click.stop="hide" title="收起（任务继续后台运行）">×</button>
      </div>
      <div class="qzb-body">
        <div class="qzb-ascii">{{ asciiBar }}</div>
        <div class="qzb-meta">
          <span>{{ pct }}%</span>
          <span class="qzb-count">{{ job.fetched }}/{{ job.total ?? '?' }} 条</span>
        </div>
        <div class="qzb-sub" v-if="job.total_pages">
          PAGE {{ job.page }}/{{ job.total_pages }} · {{ job.workers }}×THREAD · 点击返回
        </div>
        <div class="qzb-sub" v-else>CONNECTING...</div>
      </div>
    </div>
  </transition>
</template>

<script setup>
// 全局悬浮进度条：抓取任务运行期间挂在所有页面上，
// 用户切去任何页面都能看到进度，点击回 /qzone 详情页。
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQzoneStore } from '@/stores/modules/qzone'

const store = useQzoneStore()
const router = useRouter()

const visible = computed(() => store.jid && store.job && store.job.status === 'running' && !store.badgeHidden)

const pct = computed(() => {
  const j = store.job
  if (!j?.total) return 0
  return Math.min(100, Math.round((j.fetched / j.total) * 100))
})

// 90 年代 BIOS 风格 ASCII 进度条
const asciiBar = computed(() => {
  const filled = Math.round((pct.value / 100) * 20)
  return '[' + '█'.repeat(filled) + '░'.repeat(20 - filled) + ']'
})

function goPage() {
  router.push('/qzone')
}
function hide() {
  store.badgeHidden = true
}

onMounted(() => store.restore())
</script>

<style scoped>
.qzb-wrap {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 80;
  width: 252px;
  cursor: pointer;
  user-select: none;
  background: linear-gradient(165deg, rgba(9, 17, 36, 0.92), rgba(6, 11, 24, 0.95));
  border: 1px solid rgba(56, 189, 248, 0.3);
  border-radius: 10px;
  overflow: hidden;
  box-shadow:
    0 10px 34px rgba(2, 8, 20, 0.6),
    0 0 18px rgba(56, 189, 248, 0.12);
  backdrop-filter: blur(10px);
}
.qzb-wrap::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: repeating-linear-gradient(0deg, rgba(125, 211, 252, 0.03) 0 1px, transparent 1px 3px);
}
.qzb-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-bottom: 1px solid rgba(56, 189, 248, 0.16);
  background: rgba(56, 189, 248, 0.07);
}
.qzb-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22d3ee;
  box-shadow: 0 0 8px #22d3ee;
  animation: qzb-pulse 1.2s ease-in-out infinite;
}
.qzb-title {
  font:
    700 10px/1 ui-monospace,
    Consolas,
    monospace;
  color: #67e8f9;
  letter-spacing: 1px;
}
.qzb-x {
  margin-left: auto;
  width: 18px;
  height: 18px;
  line-height: 16px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 5px;
  color: #94a3b8;
  font-size: 12px;
  background: transparent;
  cursor: pointer;
}
.qzb-x:hover {
  color: #f87171;
  border-color: rgba(248, 113, 113, 0.4);
}
.qzb-body {
  padding: 10px 12px 12px;
  position: relative;
}
.qzb-ascii {
  font:
    700 13px/1.2 ui-monospace,
    Consolas,
    monospace;
  color: #22d3ee;
  text-shadow: 0 0 10px rgba(34, 211, 238, 0.45);
  letter-spacing: 0.5px;
}
.qzb-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font:
    11px/1 ui-monospace,
    Consolas,
    monospace;
  color: #a5f3fc;
}
.qzb-count {
  color: rgba(165, 243, 252, 0.65);
}
.qzb-sub {
  margin-top: 5px;
  font:
    9px/1 ui-monospace,
    Consolas,
    monospace;
  color: rgba(103, 232, 249, 0.45);
  letter-spacing: 1px;
}
@keyframes qzb-pulse {
  50% {
    opacity: 0.35;
  }
}
.qzb-enter-active,
.qzb-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.qzb-enter-from,
.qzb-leave-to {
  opacity: 0;
  transform: translateY(14px) scale(0.94);
}
</style>

<template>
  <div class="chat-approval-page">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h2 class="text-xl font-bold text-white">群聊申请审批</h2>
        <p class="text-xs text-gray-500 mt-1">管理用户的群聊加入申请</p>
      </div>
      <button class="web3-btn text-xs" @click="loadApplications">刷新</button>
    </div>

    <div v-if="!applications.length" class="empty-state">
      <div class="empty-icon">✅</div>
      <p>暂无待审批申请</p>
    </div>

    <div v-else class="approval-list">
      <div v-for="app in applications" :key="app.id" class="approval-card">
        <div class="app-avatar">{{ (app.nickname || app.username || '?').charAt(0).toUpperCase() }}</div>
        <div class="app-info">
          <div class="app-name">{{ app.nickname || app.username }}</div>
          <div class="app-meta">
            <span>用户名: {{ app.username }}</span>
            <span>用户ID: {{ app.userId }}</span>
            <span>申请时间: {{ formatTime(app.createdAt) }}</span>
          </div>
        </div>
        <div class="app-actions">
          <button class="web3-btn approve-btn" @click="approve(app.id)">通过</button>
          <button class="web3-btn reject-btn" @click="reject(app.id)">拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPendingApplications, approveApplication, rejectApplication } from '@/api/chat'

const applications = ref([])

async function loadApplications() {
  try {
    const res = await getPendingApplications()
    applications.value = res.data || []
  } catch { applications.value = [] }
}

async function approve(id) {
  try {
    await approveApplication(id)
    applications.value = applications.value.filter(a => a.id !== id)
  } catch (e) {
    alert(e?.response?.data?.message || '操作失败')
  }
}

async function reject(id) {
  try {
    await rejectApplication(id)
    applications.value = applications.value.filter(a => a.id !== id)
  } catch (e) {
    alert(e?.response?.data?.message || '操作失败')
  }
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => { loadApplications() })
</script>

<style scoped>
.chat-approval-page { padding: 1.5rem; }
.empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 4rem 0; gap: 1rem; color: rgba(255,255,255,0.4);
}
.empty-icon { font-size: 3rem; opacity: 0.5; }
.approval-list { display: flex; flex-direction: column; gap: 0.75rem; }
.approval-card {
  display: flex; align-items: center; gap: 1rem;
  background: rgba(15, 18, 40, 0.6); border: 1px solid rgba(255,255,255,0.06);
  border-radius: 12px; padding: 1rem 1.25rem;
}
.app-avatar {
  width: 42px; height: 42px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 1rem;
}
.app-info { flex: 1; }
.app-name { color: #fff; font-weight: 600; font-size: 0.95rem; }
.app-meta { display: flex; gap: 1rem; margin-top: 4px; flex-wrap: wrap; }
.app-meta span { color: rgba(255,255,255,0.4); font-size: 0.78rem; }
.app-actions { display: flex; gap: 0.5rem; flex-shrink: 0; }
.approve-btn { background: rgba(52, 211, 153, 0.2) !important; color: #34d399 !important; border: 1px solid rgba(52,211,153,0.3) !important; }
.reject-btn { background: rgba(239, 68, 68, 0.2) !important; color: #f87171 !important; border: 1px solid rgba(239,68,68,0.3) !important; }
</style>

<template>
  <!-- 右侧悬浮公告：右侧缓弹出的最新公告 + 右侧「公告」标签 + 历史公告面板 -->
  <div class="notice-root">
    <!-- 右侧缓弹吐司：弹出 5 秒后隐藏到右侧（transfrom 过渡实现滑动动画） -->
    <div class="notice-toast fixed right-0 top-24 z-[80]" :class="{ 'notice-toast-show': toastVisible && latestNotice }">
      <div class="notice-toast-card glass-panel border-l-0 border-r-0 rounded-l-2xl rounded-r-none px-4 py-3 w-72 cursor-pointer shadow-2xl"
        @click="openPanel">
        <div class="flex items-center gap-2 mb-1.5">
          <span class="text-base">{{ alertToast ? '🚨' : '📢' }}</span>
          <span class="text-[11px] font-bold tracking-wider text-purple-300 uppercase">{{ alertToast ? '行情告警' : '公告通知' }}</span>
          <span v-if="alertToast" :class="['text-[9px] px-1.5 py-0.5 rounded-full ml-1',
            alertToast.level === 'critical' ? 'bg-red-500/20 text-red-300 border border-red-400/30' :
            alertToast.level === 'warning' ? 'bg-amber-500/20 text-amber-300 border border-amber-400/30' :
            'bg-green-500/20 text-green-300 border border-green-400/30']">
            {{ alertToast.level === 'critical' ? '严重' : alertToast.level === 'warning' ? '警告' : '提示' }}
          </span>
          <button class="ml-auto text-gray-500 hover:text-white transition text-xs" @click.stop="hideToast" title="关闭">✕</button>
        </div>
        <p class="text-xs text-gray-200 line-clamp-2 leading-relaxed">{{ alertToast ? alertText(alertToast) : latestNotice?.content }}</p>
        <div class="flex items-center justify-between mt-2">
          <span class="text-[10px] text-gray-500">{{ formatDate(alertToast?.created_at || latestNotice?.createdAt) }}</span>
          <span class="text-[10px] text-purple-300/80">点击查看全部 ›</span>
        </div>
        <div class="absolute left-0 top-0 h-full w-1 bg-gradient-to-b from-purple-500 to-cyan-400 rounded-l-2xl"></div>
      </div>
    </div>

    <!-- 右侧「公告」标签（始终可见，有未读时脉冲高亮） -->
    <Transition name="notice-fade">
      <button v-if="hasNotices"
        class="notice-tab fixed right-0 z-[80] rounded-l-xl rounded-r-none px-3 py-4 inline-flex flex-col items-center gap-2 cursor-pointer group"
        :class="{ 'notice-tab-hot': unreadCount > 0 }"
        @click="openPanel">
        <span class="text-lg leading-none">📢</span>
        <span class="text-[11px] font-semibold tracking-[0.2em] text-white" style="writing-mode: vertical-lr;">公告</span>
        <span v-if="noticeBadge !== '0'" class="notice-badge">{{ noticeBadge }}</span>
        <span v-else class="text-[8px] text-gray-400" style="writing-mode: vertical-lr;">查看</span>
      </button>
    </Transition>

    <!-- 历史公告面板 -->
    <Teleport to="body">
      <Transition name="notice-panel">
        <div v-if="panelOpen" class="notice-mask fixed inset-0 z-[90] flex justify-end" @click.self="panelOpen = false">
          <div class="notice-panel w-80 sm:w-96 h-full bg-[#0a0a18]/95 backdrop-blur-2xl border-l border-white/10 flex flex-col shadow-2xl">
            <!-- 头部 -->
            <div class="flex items-center gap-3 px-5 py-4 border-b border-white/10">
              <span class="text-lg">📢</span>
              <div class="flex-1">
                <h3 class="text-sm font-bold text-white">公告中心</h3>
                <p class="text-[10px] text-gray-500 mt-0.5">
                  {{ activeView === 'notice' ? `历史公告 · ${sortedNotices.length} 条` : `行情同步告警 · ${alerts.length} 条` }}
                </p>
              </div>
              <div class="flex gap-1">
                <button @click="activeView = 'notice'"
                  :class="['text-[10px] px-2.5 py-1 rounded-lg transition',
                    activeView === 'notice' ? 'bg-purple-500/20 text-purple-300 border border-purple-400/30' : 'border border-white/10 text-gray-400 hover:text-white']">
                  公告{{ unreadCount ? ` (${unreadCount})` : '' }}
                </button>
                <button @click="activeView = 'alerts'"
                  :class="['text-[10px] px-2.5 py-1 rounded-lg transition',
                    activeView === 'alerts' ? 'bg-pink-500/20 text-pink-300 border border-pink-400/30' : 'border border-white/10 text-gray-400 hover:text-white']">
                  🚨 行情告警{{ alerts.length ? ` (${alerts.length})` : '' }}
                </button>
              </div>
              <button @click="panelOpen = false" class="text-gray-500 hover:text-white transition text-sm p-1" title="关闭">✕</button>
            </div>
            <!-- 列表 -->
            <div class="flex-1 overflow-y-auto notice-scroll px-3 py-3 space-y-2.5">
              <template v-if="activeView === 'notice'">
                <div v-if="sortedNotices.length" v-for="(n, i) in sortedNotices" :key="n.id"
                  class="group rounded-xl border p-3.5 transition cursor-pointer"
                  :class="i === 0
                    ? 'border-purple-400/25 bg-purple-500/8'
                    : 'border-white/8 hover:border-white/15 bg-white/[0.02]'"
                  @click="markRead(n)">
                  <div class="flex items-center gap-2 mb-1.5">
                    <span v-if="i === 0" class="text-[9px] px-1.5 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-400/30">最新</span>
                    <span v-else-if="isUnread(n)" class="text-[9px] px-1.5 py-0.5 rounded-full bg-pink-500/20 text-pink-300 border border-pink-400/30">新</span>
                    <span class="text-[10px] text-gray-500 ml-auto">{{ formatDate(n.createdAt) }}</span>
                  </div>
                  <p class="text-xs text-gray-200 leading-relaxed">{{ n.content }}</p>
                </div>
                <div v-else class="py-16 text-center">
                  <div class="text-4xl mb-3 opacity-15">📢</div>
                  <p class="text-xs text-gray-600">暂无公告</p>
                </div>
              </template>

              <template v-else>
                <div v-if="alerts.length" v-for="a in alerts" :key="a.id"
                  class="group rounded-xl border p-3.5 transition cursor-default"
                  :class="[alertCls(a), a.id === newestAlertId ? 'ring-1 ring-pink-400/30' : '']">
                  <div class="flex items-center gap-2 mb-1.5">
                    <span :class="['text-[9px] px-1.5 py-0.5 rounded-full border',
                      a.level === 'critical' ? 'bg-red-500/20 text-red-300 border-red-400/30' :
                      a.level === 'warning' ? 'bg-amber-500/20 text-amber-300 border-amber-400/30' :
                      'bg-green-500/20 text-green-300 border-green-400/30']">
                      {{ a.level === 'critical' ? '严重' : a.level === 'warning' ? '警告' : '提示' }}
                    </span>
                    <span v-if="a.id === newestAlertId" class="text-[9px] px-1.5 py-0.5 rounded-full bg-pink-500/20 text-pink-300 border border-pink-400/30">新</span>
                    <span class="text-[10px] text-gray-500 ml-auto">{{ formatDate(a.created_at) }}</span>
                  </div>
                  <p class="text-xs text-gray-200 leading-relaxed">{{ alertText(a) }}</p>
                  <div v-if="a.symbol" class="text-[10px] text-gray-500 mt-1">🚨 {{ a.symbol }}{{ a.name ? ' · ' + a.name : '' }} · {{ a.source || 'quant-py' }}</div>
                </div>
                <div v-else class="py-16 text-center">
                  <div class="text-4xl mb-3 opacity-15">🚨</div>
                  <p class="text-xs text-gray-600">暂无行情告警</p>
                </div>
              </template>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
// ============================================================
// 整站公告通知（NoticePanel）：挂载于 App.vue，全局生效
// - 右侧缓弹：加载到「未读最新公告」时从右侧滑入，5 秒后隐藏回右侧
// - 右侧「公告」标签：常驻，点击打开历史公告面板
// - 历史公告：GET /blog/notice/list（启用中，倒序）
// - 已读记录存 localStorage（'web3_notice_read'），避免每次刷新都弹
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getBlogNotices } from '@/api/blog'
import { getPyMarketAlerts } from '@/api/quant'
import { formatMinute } from '@/utils/date'

const notices = ref([])
const alerts = ref([])
const activeView = ref('notice')
const alertToast = ref(null)
const newestAlertId = ref(null)
const previousAlertId = ref(null)
const toastVisible = ref(false)
const panelOpen = ref(false)
const READ_KEY = 'web3_notice_read' // 存 { id: latestReadId, list: [已读id] }
const ACK_KEY = 'web3_notice_ack' // 本会话已确认（不重复弹）的公告 id 集合
const toastTimer = ref(null)
const pollTimer = ref(null)
const stopRouteWatch = ref(null)

const route = useRoute()

// 最新一条启用公告
const latestNotice = computed(() => notices.value[0] || null)

// 倒序列表（后端已倒序，但保险起见）——保持后端顺序即可
const sortedNotices = computed(() => notices.value)

// 已读集合
function readState() {
  try { return JSON.parse(localStorage.getItem(READ_KEY)) || { id: 0, list: [] } }
  catch { return { id: 0, list: [] } }
}
function saveRead(state) {
  try { localStorage.setItem(READ_KEY, JSON.stringify(state)) } catch {}
}
// 本会话已确认集合
function ackSet() {
  try { return new Set(JSON.parse(sessionStorage.getItem(ACK_KEY)) || []) }
  catch { return new Set() }
}
function saveAck(set) {
  try { sessionStorage.setItem(ACK_KEY, JSON.stringify([...set])) } catch {}
}

const isUnread = n => !readState().list.includes(String(n.id))
const isAcked = id => ackSet().has(String(id))
const unreadCount = computed(() => notices.value.filter(n => isUnread(n)).length)
const hasNotices = computed(() => notices.value.length > 0)
// 标签角标：未读公告 + 未解决行情告警
const noticeBadge = computed(() => {
  const total = unreadCount.value + alerts.value.length
  return total > 99 ? '99+' : total
})

function alertCls(a) {
  if (a?.level === 'critical') return 'border-red-500/25 bg-red-500/5 hover:border-red-500/40'
  if (a?.level === 'warning') return 'border-amber-500/25 bg-amber-500/5 hover:border-amber-500/40'
  return 'border-white/8 bg-white/[0.02] hover:border-white/15'
}
function alertText(a) {
  if (!a) return ''
  return a.message || `${a.rule_type || '行情'}告警${a.symbol ? ' · ' + a.symbol : ''}${a.name ? ' ' + a.name : ''}${a.source ? ' (' + a.source + ')' : ''}`
}

function markRead(n) {
  if (!n) return
  const st = readState()
  if (!st.list.includes(String(n.id))) st.list.push(String(n.id))
  const newestId = sortedNotices.value[0]?.id
  if (String(n.id) === String(newestId)) st.id = newestId
  st.list = [...new Set(st.list)]
  saveRead(st)
}

// 本会话确认（点击面板 / 关闭吐司后不再重复弹）
function ack(id) {
  const s = ackSet()
  s.add(String(id))
  saveAck(s)
}

// 隐藏吐司（滑回右侧）
function hideToast() {
  toastVisible.value = false
  alertToast.value = null
  clearTimeout(toastTimer.value)
}

// 打开历史面板并收起吐司
function openPanel() {
  hideToast()
  panelOpen.value = true
  // 打开即视为已读最新一条并确认本会话
  if (latestNotice.value) {
    markRead(latestNotice.value)
    ack(latestNotice.value.id)
  }
}

// 是否应该缓弹最新公告
function shouldToast() {
  if (!notices.value.length) return false
  const newest = notices.value[0]
  if (String(newest.id) === String(readState().id || 0)) return false // 已读过最新 → 不弹
  if (isAcked(newest.id)) return false // 本会话已确认过 → 不弹
  return true
}

// 弹出最新公告（5 秒后滑回右侧）
function toastIfNeeded() {
  if (!shouldToast()) return
  toastVisible.value = true
  clearTimeout(toastTimer.value)
  toastTimer.value = setTimeout(() => {
    toastVisible.value = false
    if (latestNotice.value) ack(latestNotice.value.id)
  }, 5000)
}

// 日期格式化（统一走 utils/date）
function formatDate(v) { return formatMinute(v, '') }

async function loadAlerts() {
  try {
    const res = await getPyMarketAlerts()
    const list = Array.isArray(res?.data) ? res.data : []
    const topId = list[0]?.id ?? null
    // 出现更新的告警 → 缓弹提示（跳过首次加载）
    if (alerts.value.length && topId && topId !== previousAlertId.value) {
      alertToast.value = list[0]
      toastVisible.value = true
      clearTimeout(toastTimer.value)
      toastTimer.value = setTimeout(() => {
        toastVisible.value = false
        alertToast.value = null
      }, 5000)
    }
    alerts.value = list
    newestAlertId.value = topId
    previousAlertId.value = topId
  } catch (e) {
    alertToast.value = null
  }
}

async function loadNotices() {
  try {
    const res = await getBlogNotices()
    const data = res?.data || []
    if (!Array.isArray(data)) return
    const prevNewest = notices.value[0]?.id
    notices.value = data
    // 有新公告推出 → 立刻缓弹
    const newest = notices.value[0]
    if (newest && String(newest.id) !== String(prevNewest) && String(newest.id) !== String(readState().id || 0)) {
      ackSet().clear() // 新公告，重置本会话确认
      saveAck(new Set())
    }
  } catch (e) {
    console.warn('[NoticePanel] load failed:', e?.message || e)
  }
}

onMounted(async () => {
  await loadNotices()
  await loadAlerts()
  toastIfNeeded()
  console.info(
    '[NoticePanel] loaded:', {
      total: notices.value.length,
      newestId: notices.value[0]?.id || null,
      read: readState(),
      hasNotices: hasNotices.value,
      unread: unreadCount.value,
      acked: [...ackSet()]
    }
  )
  // 每 30s 轮询：期间发布的新公告/新告警也能即时缓弹
  pollTimer.value = setInterval(async () => {
    await Promise.allSettled([loadNotices(), loadAlerts()])
    toastIfNeeded()
  }, 30000)
  // 回到页面 / 窗口聚焦时，若有未确认的新公告再提示一次
  const onFocus = () => toastIfNeeded()
  window.addEventListener('focus', onFocus)
  document.addEventListener('visibilitychange', onVisibilityGlobal)
  function onVisibilityGlobal() {
    if (!document.hidden) toastIfNeeded()
  }
  window.__noticeFocusHandler = onFocus
  window.__noticeVisHandler = onVisibilityGlobal
  // 路由切换时若有未确认的新公告，再次缓弹提示（由会话确认控制次数）
  stopRouteWatch.value = watch(() => route.fullPath, () => {
    toastIfNeeded()
  })
})

onBeforeUnmount(() => {
  clearTimeout(toastTimer.value)
  clearInterval(pollTimer.value)
  if (stopRouteWatch.value) stopRouteWatch.value()
  if (window.__noticeFocusHandler) window.removeEventListener('focus', window.__noticeFocusHandler)
  if (window.__noticeVisHandler) document.removeEventListener('visibilitychange', window.__noticeVisHandler)
})
</script>

<style scoped>
/* 缓弹吐司：从右侧滑入，5 秒后滑回 */
.notice-toast {
  transform: translateX(105%);
  transition: transform 0.5s cubic-bezier(0.23, 1, 0.32, 1);
}
.notice-toast-show {
  transform: translateX(0);
}

/* 右侧标签淡入 */
.notice-fade-enter-active,
.notice-fade-leave-active { transition: opacity 0.35s ease, transform 0.35s ease; }
.notice-fade-enter-from,
.notice-fade-leave-to { opacity: 0; transform: translateX(12px); }

/* 右侧标签 */
.notice-tab {
  top: 45%;
  transform: translateY(-50%);
  background: linear-gradient(180deg, rgba(124, 58, 237, 0.75), rgba(168, 85, 247, 0.6));
  border: 1px solid rgba(217, 70, 239, 0.45);
  border-right: none;
  box-shadow: 0 6px 24px rgba(168, 85, 247, 0.35);
  backdrop-filter: blur(12px);
  transition: background 0.3s, box-shadow 0.3s;
}
.notice-tab:hover {
  background: linear-gradient(180deg, rgba(124, 58, 237, 0.95), rgba(217, 70, 239, 0.75));
  box-shadow: 0 6px 30px rgba(217, 70, 239, 0.5);
}
.notice-tab-hot {
  animation: tabGlow 2s ease-in-out infinite;
}
@keyframes tabGlow {
  0%, 100% { box-shadow: 0 6px 24px rgba(168, 85, 247, 0.35); }
  50% { box-shadow: 0 6px 34px rgba(217, 70, 239, 0.75), 0 0 18px rgba(236, 72, 153, 0.5); }
}
.notice-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ec4899;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  animation: badgePulse 1.6s ease-in-out infinite;
}
@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

/* 面板遮罩与滑入 */
.notice-mask {
  background: rgba(4, 4, 14, 0.45);
  backdrop-filter: blur(2px);
}
.notice-panel-panel-enter-active,
.notice-panel-panel-leave-active { transition: opacity 0.3s ease; }
.notice-panel-panel-enter-from,
.notice-panel-panel-leave-to { opacity: 0; }

.notice-panel {
  animation: panelIn 0.38s cubic-bezier(0.23, 1, 0.32, 1);
}
@keyframes panelIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

/* 滚动条 */
.notice-scroll { scrollbar-width: thin; scrollbar-color: rgba(255, 255, 255, 0.15) transparent; }
.notice-scroll::-webkit-scrollbar { width: 5px; }
.notice-scroll::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.15); border-radius: 999px; }
</style>
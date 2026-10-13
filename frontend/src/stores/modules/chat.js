// ============================================================
// 全局群聊 Store（Pinia）
// 「极光广场」固定单群：登录即自动入群、全局单条 WebSocket 常驻，
// 路由切换不断线；供左侧聊天 Dock 与 /community/chat 完整页共用。
// 状态：dockState(open/mini/closed) / messages / members(含在线) / unread
// ============================================================
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useAuthStore } from './auth'
import { useToastStore } from './toast'
import { hasPerm } from '@/utils/permissions'
import { getChatRooms, getChatMembers, getChatMessages, uploadChatImage, getChatEnabled, setChatEnabled as apiSetChatEnabled } from '@/api/chat'
import { getUserInfo } from '@/api/user'

export const useChatStore = defineStore('chat', () => {
  const auth = useAuthStore()
  const toast = useToastStore()

  // ---------- 面板状态：open=展开侧栏 / mini=窄条 / closed=完全关闭 ----------
  const dockState = ref(localStorage.getItem('chat-dock') || 'mini')
  const room = ref(null)
  const members = ref([])        // MemberVO：{userId,username,nickname,avatar,role,status,online,joinedAt}
  const messages = ref([])
  const wsConnected = ref(false)
  const loading = ref(false)
  const loadError = ref('')
  const unread = ref(0)
  const roomEmoji = '🌌'
  const chatClosed = ref(false)   // 管理员关闭群聊开关

  let ws = null
  let started = false
  let initPromise = null   // in-flight 的 init Promise：并发调用共享同一次初始化，避免竞态误判
  let reconnectTimer = null
  let reconnectDelay = 3000
  let verifyTimer = null
  let initRetryTimer = null
  let initRetryCount = 0

  const myUserId = computed(() => auth.user?.id)
  const onlineCount = computed(() => members.value.filter(m => m.online).length)
  /** 是否可发言：ADMIN 恒可；普通用户需 verified=2（已实名认证） */
  const canChat = computed(() => {
    const u = auth.user
    if (!u) return false
    if (u.role === 'ADMIN') return true
    return u.verified === 2
  })
  /** userId -> member，便于消息补头像/昵称 */
  const memberMap = computed(() => {
    const m = new Map()
    for (const mem of members.value) m.set(mem.userId, mem)
    return m
  })

  // ---------- Dock 三态切换（记忆到 localStorage） ----------
  function setDock(state) {
    if (!['open', 'mini', 'closed'].includes(state)) return
    dockState.value = state
    localStorage.setItem('chat-dock', state)
    if (state === 'open') {
      unread.value = 0
      // 展开时立即同步一次实名认证状态（管理员刚审核通过可立刻解锁发言）
      syncVerified()
    }
  }
  const openDock = () => setDock('open')
  const miniDock = () => setDock('mini')
  const closeDock = () => setDock('closed')

  // ---------- 初始化：登录后调用一次，全局常驻 ----------
  // - 并发调用共享同一次 in-flight 初始化（页面进入时 init 可能仍在进行，不能立即判定失败）
  // - 首次失败自动重试最多 3 次（间隔 3s/6s/9s），防止 forum-service 短暂不可用导致群聊永久不启动
  function init() {
    if (!auth.isLoggedIn) return Promise.resolve(false)
    if (initPromise) return initPromise
    initPromise = doInit().finally(() => { initPromise = null })
    return initPromise
  }

  async function doInit() {
    if (started) return true
    started = true
    loading.value = true
    loadError.value = ''
    try {
      // 开关检查与群列表并行拉取，进群少一轮串行等待
      const [enRes, roomsRes] = await Promise.all([
        getChatEnabled().catch(() => null),
        getChatRooms(),
      ])
      const enabled = (enRes?.data ?? enRes)?.enabled !== false
      chatClosed.value = !enabled
      if (!enabled) {
        loadError.value = '管理员已关闭群聊'
        started = false
        return false
      }
      const list = roomsRes?.data ?? roomsRes
      room.value = Array.isArray(list) ? list[0] : null
      if (!room.value) { loadError.value = '群聊服务尚未就绪'; started = false; scheduleInitRetry(); return false }
      await Promise.all([loadMembers(), loadHistory()])
      connect()
      startVerifyWatcher()
      initRetryCount = 0
      return true
    } catch {
      loadError.value = '群聊连接失败'
      started = false
      scheduleInitRetry()
      return false
    } finally {
      loading.value = false
    }
  }

  function scheduleInitRetry() {
    if (!auth.isLoggedIn) return
    initRetryCount++
    if (initRetryTimer) clearTimeout(initRetryTimer)
    // 无限后台重试（3s/6s/9s…封顶 15s）：加载爆发期挤占结束后自动恢复进入，
    // 不再 3 次放弃后只能手动点"重新连接"
    const delay = Math.min(3000 * initRetryCount, 15000)
    initRetryTimer = setTimeout(() => {
      initRetryTimer = null
      init()
    }, delay)
  }

  // ---------- 管理员聊天开关 ----------
  /** 管理员一键关闭/恢复群聊：关闭立即断线，恢复后重新初始化 */
  async function adminToggleChat(enabled) {
    await apiSetChatEnabled(enabled)
    chatClosed.value = !enabled
    if (enabled) {
      loadError.value = ''
      initRetryCount = 0
      started = false
      init()
    } else {
      disconnect(true)
      room.value = null
      loadError.value = '管理员已关闭群聊'
    }
    return enabled
  }

  async function loadMembers() {
    if (!room.value) return
    try {
      const res = await getChatMembers(room.value.id)
      members.value = res?.data ?? res ?? []
    } catch {}
  }

  async function loadHistory() {
    if (!room.value) return
    try {
      const res = await getChatMessages(room.value.id, 100)
      messages.value = res?.data ?? res ?? []
    } catch {}
  }

  // ---------- 实名认证状态同步（未认证用户轮询，管理员审核通过后自动续期重连） ----------
  async function syncVerified() {
    if (canChat.value || !auth.isLoggedIn) return
    try {
      const res = await getUserInfo()
      const profile = res?.data ?? res
      if (!profile) return
      const fresh = profile.verified
      if (fresh === 2) {
        // 已认证：强制续签拿含 verified claim 的新 token，再重建 WS
        const ok = await auth.forceRefreshToken()
        if (ok && started) connect()
        toast.success('实名认证已通过，现在可以发言了')
      } else if (fresh !== undefined && fresh !== auth.user?.verified) {
        // 仅同步审核中/已拒绝等状态变化
        auth.mergeUser({ verified: fresh })
      }
    } catch {}
  }

  function startVerifyWatcher() {
    if (verifyTimer) return
    verifyTimer = setInterval(syncVerified, 45000)
  }

  // ---------- WebSocket（全局单连接 + 断线重连） ----------
  function connect() {
    if (!room.value || !auth.token) return
    disconnect(false)
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    const url = `${proto}://${location.host}/chat/ws/${room.value.id}?token=${encodeURIComponent(auth.token)}`
    try {
      ws = new WebSocket(url)
      ws.onopen = () => { wsConnected.value = true; reconnectDelay = 3000 }
      ws.onmessage = (e) => {
        let data
        try { data = JSON.parse(e.data) } catch { return }
        if (data.type === 'presence') return onPresence(data)
        if (data.type === 'error') { toast.warning(data.content || '操作被拒绝'); return }
        messages.value.push(data)
        if (messages.value.length > 500) messages.value.splice(0, messages.value.length - 500)
        // 面板未展开且非自己发的消息 → 未读 +1
        if (dockState.value !== 'open' && data.userId !== myUserId.value) unread.value++
      }
      ws.onclose = () => { wsConnected.value = false; scheduleReconnect() }
      ws.onerror = () => { wsConnected.value = false }
    } catch { wsConnected.value = false; scheduleReconnect() }
  }

  function scheduleReconnect() {
    if (!started || reconnectTimer || !auth.isLoggedIn) return
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      reconnectDelay = Math.min(reconnectDelay * 1.5, 30000)
      connect()
    }, reconnectDelay)
  }

  function disconnect(markStopped = true) {
    if (markStopped) started = false
    if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
    if (ws) { try { ws.onclose = null; ws.close() } catch {} ws = null }
    wsConnected.value = false
  }

  /** presence 事件：实时更新成员在线状态（新成员自动并入列表） */
  function onPresence(p) {
    const uid = typeof p.userId === 'string' ? Number(p.userId) : p.userId
    const mem = memberMap.value.get(uid)
    if (mem) {
      mem.online = !!p.online
      if (p.avatar) mem.avatar = p.avatar
      if (p.nickname) mem.nickname = p.nickname
    } else if (p.online) {
      members.value.unshift({
        userId: uid, username: p.username, nickname: p.nickname || p.username,
        avatar: p.avatar || null, role: 'member', status: 'approved', online: true, joinedAt: null,
      })
    }
    // 在线置顶排序
    members.value.sort((a, b) => Number(!!b.online) - Number(!!a.online))
  }

  // ---------- 发送 ----------
  function ensureChatPerm() {
    if (!canChat.value) { toast.warning('请先完成实名认证后再发言（个人中心 → 实名认证）'); return false }
    if (!hasPerm(auth.user, 'chat')) { toast.warning('你已被限制聊天，请联系管理员'); return false }
    return true
  }

  function send(type, content) {
    if (!ws || ws.readyState !== WebSocket.OPEN || !content) return false
    if (!ensureChatPerm()) return false
    ws.send(JSON.stringify({ type, content }))
    return true
  }

  const sendText = (text) => send('text', String(text || '').trim())
  const sendEmoji = (emoji) => send('emoji', emoji)

  /** 上传聊天图片并发送图片消息，返回图片 URL（失败返回 null） */
  async function sendImage(file) {
    if (!ensureChatPerm()) return null
    try {
      const res = await uploadChatImage(file)
      const url = res?.url ?? res?.data?.url
      if (url) send('image', url)
      return url || null
    } catch { return null }
  }

  // ---------- 消息展示辅助 ----------
  function nameOf(msg) { return msg.nickname || msg.username || '匿名' }
  /** 消息头像：消息冗余优先 → 成员目录 → null（组件层兜底首字母） */
  function avatarOf(msg) {
    if (msg.avatar) return msg.avatar
    const mem = memberMap.value.get(typeof msg.userId === 'string' ? Number(msg.userId) : msg.userId)
    return mem?.avatar || null
  }

  /** 登出时彻底断开并复位 */
  function reset() {
    disconnect()
    if (verifyTimer) { clearInterval(verifyTimer); verifyTimer = null }
    if (initRetryTimer) { clearTimeout(initRetryTimer); initRetryTimer = null }
    initRetryCount = 0
    room.value = null
    members.value = []
    messages.value = []
    unread.value = 0
    loadError.value = ''
    chatClosed.value = false
  }

  return {
    dockState, room, members, messages, wsConnected, loading, loadError, unread, roomEmoji, chatClosed,
    myUserId, onlineCount, canChat,
    setDock, openDock, miniDock, closeDock,
    init, loadMembers, loadHistory, connect, disconnect, reset, syncVerified, adminToggleChat,
    sendText, sendEmoji, sendImage,
    nameOf, avatarOf,
  }
})

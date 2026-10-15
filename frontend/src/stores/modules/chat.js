// ============================================================
// 全局群聊 Store（Pinia）
// 「极光广场」多房间聊天：登录即入群，全局单条 Socket.IO 常驻连接，
// 路由切换不断线；供左侧聊天 Dock 与 /community/chat 完整页共用。
//
// 传输层：Socket.IO v4（node-service/chat-gateway，:3010，经 Vite /socket.io 代理）
//   • 自带心跳（ping/pong）与自动重连（指数退避）→ 抵抗网络抖动、休眠假死
//   • 默认 polling 起手再升级 websocket → 代理不支持 WS 时自动长轮询降级
//   • 鉴权：仅 httpOnly Cookie 中的 JWT（withCredentials），URL/参数一律不带 token
//
// 状态：dockState(open/mini/closed) / rooms / room / messages / members / unread / connState
// ============================================================
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { io } from 'socket.io-client'
import { useAuthStore } from './auth'
import { useToastStore } from './toast'
import { hasPerm } from '@/utils/permissions'
import { getChatRooms, getChatMembers, getChatMessages, uploadChatImage, getChatEnabled, setChatEnabled as apiSetChatEnabled, joinChatRoom } from '@/api/chat'
import { getUserInfo } from '@/api/user'

export const useChatStore = defineStore('chat', () => {
  const auth = useAuthStore()
  const toast = useToastStore()

  // ---------- 面板状态：open=展开侧栏 / mini=窄条 / closed=完全关闭 ----------
  const dockState = ref(localStorage.getItem('chat-dock') || 'mini')
  const rooms = ref([])          // 可加入的群列表
  const room = ref(null)         // 当前群
  const members = ref([])        // MemberVO：{userId,username,nickname,avatar,role,status,online,joinedAt}
  const messages = ref([])
  const loading = ref(false)
  const loadError = ref('')
  const unread = ref(0)
  const roomEmoji = '🌌'
  const chatClosed = ref(false)   // 管理员关闭群聊开关

  // ---------- 连接状态（页面展示用） ----------
  const connState = ref('idle')   // idle | connecting | connected | reconnecting | offline | error
  const connTransport = ref('')   // websocket | polling
  const wsConnected = computed(() => connState.value === 'connected')
  const connLabel = computed(() => ({
    idle: '未连接',
    connecting: '连接中…',
    connected: connTransport.value === 'polling' ? '在线 · 长轮询' : '在线',
    reconnecting: '重连中…',
    offline: '已离线',
    error: '连接失败',
  })[connState.value] || '未连接')

  // ---------- 私聊会话（复用同一条共享连接，按 roomId 分流） ----------
  const privateSessions = ref({})   // roomId -> { messages: [], joined: bool, onlineIds: Set<userId> }

  let socket = null
  let started = false
  let initPromise = null   // in-flight 的 init Promise：并发调用共享同一次初始化，避免竞态误判
  let verifyTimer = null
  let initRetryTimer = null
  let initRetryCount = 0
  let authRefreshTried = false

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
  // - 首次失败自动重试（3s/6s/9s… 封顶 15s），防止 forum-service 短暂不可用导致群聊永久不启动
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
      rooms.value = Array.isArray(list) ? list : []
      room.value = rooms.value[0] || null
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
    // 无限后台重试（3s/6s/9s…封顶 15s）：加载爆发期挤占结束后自动恢复进入
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

  // ---------- 多房间：切换当前群 ----------
  /** 切换到指定房间：先 REST 入群（幂等），再让共享连接重订阅 */
  async function switchRoom(roomId) {
    const target = rooms.value.find(r => r.id === Number(roomId))
    if (!target || target.id === room.value?.id) return false
    try {
      await joinChatRoom(target.id).catch(() => {})
      room.value = target
      messages.value = []
      members.value = []
      unread.value = 0
      await Promise.all([loadMembers(), loadHistory()])
      await joinCurrentRoom()
      return true
    } catch {
      toast.error('切换房间失败')
      return false
    }
  }

  async function loadMembers(force = false) {
    if (!room.value) return
    try {
      const res = await getChatMembers(room.value.id, force)
      members.value = res?.data ?? res ?? []
      // 成员列表的 online 由 Java 侧向网关查询，连接就绪后以网关实测为准
      if (wsConnected.value) overlayOnline(lastOnlineIds)
    } catch {}
  }

  async function loadHistory() {
    if (!room.value) return
    const targetId = room.value.id
    try {
      const res = await getChatMessages(targetId, 100)
      // 拉取期间用户可能已切走房间，避免旧响应覆盖新房间消息
      if (room.value?.id !== targetId) return
      messages.value = res?.data ?? res ?? []
    } catch {}
  }

  // ---------- 实名认证状态同步（未认证用户轮询，审核通过后自动续期重连） ----------
  async function syncVerified() {
    if (canChat.value || !auth.isLoggedIn) return
    try {
      const res = await getUserInfo()
      const profile = res?.data ?? res
      if (!profile) return
      const fresh = profile.verified
      if (fresh === 2) {
        // 已认证：强制续签拿含 verified claim 的新 token + 新 Cookie，再重建连接
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

  // ---------- Socket.IO（全局单连接 + 自动重连 + 长轮询降级） ----------
  let lastOnlineIds = new Set()

  /** 建立连接：Cookie 在握手时自动携带（withCredentials），无需也不允许传 token */
  function connect() {
    if (!room.value) return
    teardownSocket()
    authRefreshTried = false
    connState.value = 'connecting'
    // 不显式指定 transports：Socket.IO 默认 polling 起手 + 自动升级 websocket，
    // 代理/网络不支持 WS 时自动停留在长轮询，实现降级可用。
    const s = io({ path: '/socket.io', withCredentials: true, timeout: 10000 })
    socket = s
    s.on('connect', () => onConnect())
    s.on('disconnect', reason => onDisconnect(reason))
    s.on('connect_error', err => onConnectError(err))
    s.io.on('reconnect_attempt', () => { if (started) connState.value = 'reconnecting' })
    s.io.on('reconnect_failed', () => { if (started) connState.value = 'offline' })
    s.on('chat:message', onMessage)
    s.on('presence', onPresence)
    s.on('chat:error', d => toast.warning(d?.content || '操作被拒绝'))
  }

  function teardownSocket() {
    if (socket) {
      try { socket.removeAllListeners(); socket.disconnect() } catch {}
      socket = null
    }
  }

  function updateTransport() {
    try { connTransport.value = socket?.io?.engine?.transport?.name || '' } catch { connTransport.value = '' }
  }

  async function onConnect() {
    if (!started) return
    connState.value = 'connected'
    updateTransport()
    // 传输方式由长轮询升级为 websocket 时同步刷新状态徽标
    try {
      socket.io.engine.on('upgrade', () => updateTransport())
    } catch {}
    await joinCurrentRoom()
    // 重连后自动拉取历史消息，补齐断线期间遗漏（按 id 去重，避免与实时消息重复）
    await loadHistory()
    loadMembers()
    // 重连后重新订阅打开中的私聊房间（否则断线期间私聊收不到消息、在线状态失效）
    for (const [id, sess] of Object.entries(privateSessions.value)) {
      socket.emit('join', { roomId: Number(id) }, res => {
        if (res?.ok) {
          sess.joined = true
          sess.onlineIds = new Set((res.onlineUserIds || []).map(String))
          // 补拉断线期间遗漏的私聊消息（按 id 去重）
          getChatMessages(Number(id), 100).then(hisRes => {
            const list = hisRes?.data ?? hisRes ?? []
            for (const m of list) {
              if (!sess.messages.some(x => x.id === m.id)) sess.messages.push(m)
            }
            sess.messages.sort((a, b) => (a.id || 0) - (b.id || 0))
          }).catch(() => {})
        }
      })
    }
  }

  function onDisconnect(reason) {
    if (!started) { connState.value = 'idle'; return }
    // 服务端主动断开（如管理员关闭群聊）不自动重连
    if (reason === 'io server disconnect') { connState.value = 'offline'; return }
    connState.value = 'reconnecting'
  }

  async function onConnectError(err) {
    if (!started) return
    connState.value = 'error'
    // 令牌过期：静默续期（刷新 Cookie）后重建连接；一次会话内只尝试一次，避免风暴
    const code = err?.data?.code
    if (code === 'UNAUTHORIZED' && !authRefreshTried) {
      authRefreshTried = true
      const ok = await auth.tryRefresh().catch(() => false)
      if (ok) { connect(); return }
    }
    const msg = err?.message || '聊天服务连接失败'
    if (msg && !/xhr poll error|websocket error/i.test(msg)) toast.warning(msg)
  }

  /** 重新订阅当前房间；返回网关确认的在线用户列表 */
  function joinCurrentRoom() {
    return new Promise(resolve => {
      if (!socket?.connected || !room.value) return resolve(false)
      const roomId = room.value.id
      socket.emit('join', { roomId }, res => {
        if (res?.ok) {
          lastOnlineIds = new Set(res.onlineUserIds || [])
          overlayOnline(lastOnlineIds)
          resolve(true)
          return
        }
        if (res?.code === 'CHAT_DISABLED') {
          chatClosed.value = true
          loadError.value = '管理员已关闭群聊'
        } else if (res?.message) {
          toast.warning(res.message)
        }
        resolve(false)
      })
    })
  }

  /** 用网关实测在线集合覆盖成员列表的 online 标记 */
  function overlayOnline(idSet) {
    for (const m of members.value) m.online = idSet.has(m.userId)
    members.value.sort((a, b) => Number(!!b.online) - Number(!!a.online))
  }

  function disconnect(markStopped = true) {
    if (markStopped) started = false
    teardownSocket()
    connState.value = markStopped ? 'idle' : connState.value
  }

  // ---------- 消息分流 ----------
  function onMessage(msg) {
    if (!msg) return
    const roomId = Number(msg.roomId)
    if (room.value && roomId === room.value.id) {
      // 幂等：重连回拉历史与实时广播可能重叠，按 id 去重
      if (messages.value.some(m => m.id === msg.id)) return
      messages.value.push(msg)
      if (messages.value.length > 500) messages.value.splice(0, messages.value.length - 500)
      if (dockState.value !== 'open' && msg.userId !== myUserId.value) unread.value++
      return
    }
    const sess = privateSessions.value[roomId]
    if (sess) {
      if (sess.messages.some(m => m.id === msg.id)) return
      sess.messages.push(msg)
      if (sess.messages.length > 300) sess.messages.splice(0, sess.messages.length - 300)
    }
  }

  /** presence 事件：实时更新在线状态（presence 按房间广播，须按 roomId 分流，
   *  否则私聊房间的上下线会错误污染群成员列表，反之亦然） */
  function onPresence(p) {
    // 雪花 ID 超出 JS 安全整数范围，全程按字符串比较，禁止 Number() 转换
    const uid = String(p.userId)
    const pRoomId = Number(p.roomId)

    // 1) 私聊房间：维护该会话的在线集合（私聊头部「在线/离线」的数据源）
    const sess = privateSessions.value[pRoomId]
    if (sess?.onlineIds) {
      if (p.online) sess.onlineIds.add(uid)
      else sess.onlineIds.delete(uid)
    }

    // 2) 当前群房间：更新成员列表在线标记（新成员自动并入）
    if (room.value && pRoomId === room.value.id) {
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
  }

  // ---------- 发送 ----------
  function ensureChatPerm() {
    if (!canChat.value) { toast.warning('请先完成实名认证后再发言（个人中心 → 实名认证）'); return false }
    if (!hasPerm(auth.user, 'chat')) { toast.warning('你已被限制聊天，请联系管理员'); return false }
    return true
  }

  function send(type, content, quoteId = null) {
    if (!socket?.connected || !room.value || !content) return false
    if (!ensureChatPerm()) return false
    const payload = { roomId: room.value.id, msgType: type, content }
    if (quoteId) payload.quoteId = quoteId
    socket.emit('chat:message', payload, res => {
      if (res && !res.ok && res.message) toast.warning(res.message)
    })
    return true
  }

  const sendText = (text, quoteId = null) => send('text', String(text || '').trim(), quoteId)
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

  // ---------- 私聊会话（共用共享连接） ----------
  /** 打开私聊房间：入群 + 拉历史 + 复用全局 socket 订阅 */
  async function openPrivateSession(roomId) {
    const id = Number(roomId)
    if (!privateSessions.value[id]) {
      privateSessions.value[id] = { messages: [], joined: false, onlineIds: new Set() }
    }
    const sess = privateSessions.value[id]
    try {
      const hisRes = await getChatMessages(id, 100)
      sess.messages = hisRes?.data ?? hisRes ?? []
    } catch {}
    if (socket?.connected) {
      socket.emit('join', { roomId: id }, res => {
        if (res?.ok) {
          sess.joined = true
          // 网关返回该房间实时在线用户（雪花 ID 一律按字符串），私聊头部据此显示对方在线/离线
          sess.onlineIds = new Set((res.onlineUserIds || []).map(String))
        } else if (res?.message) {
          toast.warning(res.message)
        }
      })
    }
    return sess
  }

  /** 关闭私聊房间：退订（不动全局连接） */
  function closePrivateSession(roomId) {
    const id = Number(roomId)
    if (socket?.connected) socket.emit('leave', { roomId: id })
    delete privateSessions.value[id]
  }

  /** 私聊发送：与群聊共用同一连接与同一套消息落库逻辑 */
  function sendPrivate(roomId, type, content, quoteId = null) {
    const id = Number(roomId)
    if (!socket?.connected || !content) return false
    if (!ensureChatPerm()) return false
    const payload = { roomId: id, msgType: type, content }
    if (quoteId) payload.quoteId = quoteId
    socket.emit('chat:message', payload, res => {
      if (res && !res.ok && res.message) toast.warning(res.message)
    })
    return true
  }

  // ---------- 消息展示辅助 ----------
  function nameOf(msg) { return msg.nickname || msg.username || '匿名' }
  /** 消息头像：优先成员目录（最新头像，资料变更后立即生效）→ 消息冗余 → null
   *  原实现先取 msg.avatar（发送时冗余），导致用户在个人中心换头像后历史消息仍是旧头像。
   *  改为优先成员目录，目录由 listAllMembersWithStatus 60s TTL 拉自 user-service，
   *  且 syncAvatar() 在头像更新后立即触发 loadMembers() 强制刷新目录缓存。 */
  function avatarOf(msg) {
    // 同样按字符串索引成员目录（雪花 ID 不可 Number 转换）
    const uid = String(msg.userId)
    const mem = memberMap.value.get(uid)
    if (mem?.avatar) return mem.avatar
    return msg.avatar || null
  }

  /** 头像/资料变更后调用：强制刷新群成员目录（绕过 60s 缓存）使新头像立即生效 */
  async function syncAvatar() {
    if (!room.value) return
    try { await loadMembers(true) } catch {}
  }

  /** 登出时彻底断开并复位 */
  function reset() {
    disconnect()
    if (verifyTimer) { clearInterval(verifyTimer); verifyTimer = null }
    if (initRetryTimer) { clearTimeout(initRetryTimer); initRetryTimer = null }
    initRetryCount = 0
    rooms.value = []
    room.value = null
    members.value = []
    messages.value = []
    privateSessions.value = {}
    unread.value = 0
    loadError.value = ''
    chatClosed.value = false
    connState.value = 'idle'
    connTransport.value = ''
  }

  /** 内联群聊 Dock 用到的「重连」入口（保持既有调用方语义） */
  function reconnect() {
    started = true
    connect()
  }

  return {
    dockState, rooms, room, members, messages, loading, loadError, unread, roomEmoji, chatClosed,
    connState, connTransport, connLabel, privateSessions,
    myUserId, onlineCount, canChat, wsConnected,
    setDock, openDock, miniDock, closeDock,
    init, loadMembers, loadHistory, connect, reconnect, disconnect, reset, syncVerified, syncAvatar,
    adminToggleChat, switchRoom,
    sendText, sendEmoji, sendImage,
    openPrivateSession, closePrivateSession, sendPrivate,
    nameOf, avatarOf,
  }
})

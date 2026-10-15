'use strict'
// ============================================================
// 「极光广场」聊天网关（Socket.IO v4，端口 3010）
//   • 传输：WebSocket 优先，自动降级 HTTP 长轮询（断网/代理不支持 WS 时仍可用）
//   • 心跳：Socket.IO 自带 ping/pong（25s/20s），休眠假死后自动探测断线
//   • 重连：客户端 exponential backoff，重连后自动重新 join 并回拉历史
//   • 鉴权：仅 httpOnly Cookie 中的 JWT，URL/query/auth 一律不收 token
//   • 持久化：MySQL web3_forum.chat_message（房间/成员表由 Java 侧共用）
//   • 集群：广播统一走 pubsub 总线，配置 Redis 后即自动升级为 PubSub 多实例广播
//
// 事件契约（与前端 stores/modules/chat.js 对齐）：
//   client → server : join{roomId,ack} / leave{roomId} / chat:message{roomId,msgType,content,quoteId}
//   server → client : chat:message(消息对象) / presence{roomId,online,userId,...} / chat:error{content}
// ============================================================
const path = require('path')
// 根目录 .env（已存在的环境变量优先，不覆盖启动脚本注入的值）
require('dotenv').config({ path: path.resolve(__dirname, '..', '..', '.env') })

const http = require('http')
const { Server } = require('socket.io')
const { verifyHandshake } = require('./lib/auth')
const db = require('./lib/db')
const { createBus } = require('./lib/pubsub')

const PORT = Number(process.env.CHAT_GATEWAY_PORT || 3010)
const ALLOWED_MSG_TYPES = new Set(['text', 'emoji', 'image'])
const MAX_CONTENT_LEN = 2000

/** roomId → Map<userId, Set<socketId>>：用于在线状态去重与成员在线列表 */
const presence = new Map()
const roomKey = roomId => `room:${roomId}`

const httpServer = http.createServer((req, res) => {
  const json = body => {
    res.writeHead(200, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify(body))
  }
  if (req.url === '/health') {
    return json({ status: 'ok', service: 'chat-gateway', bus: bus.mode })
  }
  // 在线成员查询：供 Java forum-service 的 /chat/rooms/{id}/members 拼接在线标记
  const m = req.url.match(/^\/online\/(\d+)/)
  if (m) return json({ roomId: Number(m[1]), userIds: onlineUserIds(Number(m[1])) })
  res.writeHead(404, { 'Content-Type': 'application/json' })
  res.end(JSON.stringify({ message: 'not found' }))
})

const io = new Server(httpServer, {
  // 长轮询 → WebSocket 自动升级；仅在同源反向代理失效时才需要直连 3010
  cors: { origin: true, credentials: true },
  pingInterval: 25000,
  pingTimeout: 20000,
  maxHttpBufferSize: 1e6,
})

// ---------- 广播总线：本机直投 + 跨节点转发 ----------
const bus = createBus((channel, payload) => {
  // 来自其它节点的事件：本地投递（channel 形如 room:1）
  const roomId = channel.split(':')[1]
  for (const { event, data, except } of payload) {
    let target = io.to(channel)
    if (except) target = target.except(except)
    target.emit(event, { ...data, roomId: Number(roomId) })
  }
})

// ---------- 握手鉴权：只认 httpOnly Cookie ----------
io.use((socket, next) => {
  try {
    socket.data.user = verifyHandshake(socket.handshake)
    next()
  } catch (err) {
    // 前端识别 UNAUTHORIZED 后走 auth.tryRefresh() 续期并重连
    next(Object.assign(new Error(err.message || '鉴权失败'), { data: { code: 'UNAUTHORIZED' } }))
  }
})

// ---------- 房间在线簿记 ----------
function markOnline(roomId, userId, socketId) {
  let users = presence.get(roomId)
  if (!users) { users = new Map(); presence.set(roomId, users) }
  let sockets = users.get(userId)
  if (!sockets) { sockets = new Set(); users.set(userId, sockets) }
  const first = sockets.size === 0
  sockets.add(socketId)
  return first   // 该用户在本房间的首条连接（用于去重 presence 广播）
}

function markOffline(roomId, userId, socketId) {
  const users = presence.get(roomId)
  if (!users) return false
  const sockets = users.get(userId)
  if (!sockets) return false
  sockets.delete(socketId)
  if (sockets.size === 0) users.delete(userId)
  if (users.size === 0) presence.delete(roomId)
  return sockets.size === 0   // 该用户在本房间的最后一条连接已断开
}

const onlineUserIds = roomId => {
  const users = presence.get(roomId)
  return users ? [...users.keys()] : []
}

/** 本机直投 + 跨节点转发 */
async function broadcast(roomId, event, data, exceptSocketId) {
  let target = io.to(roomKey(roomId))
  if (exceptSocketId) target = target.except(exceptSocketId)
  target.emit(event, { ...data, roomId })
  await bus.publish(roomKey(roomId), [{ event, data, except: exceptSocketId || null }])
}

const presencePayload = (user, online) => ({
  type: 'presence',
  online,
  userId: user.userId,
  username: user.username,
  nickname: user.nickname,
  avatar: user.avatar || null,
})

// ---------- 连接生命周期 ----------
io.on('connection', socket => {
  const user = socket.data.user
  socket.data.rooms = new Set()

  const sendError = content => socket.emit('chat:error', { type: 'error', content })

  socket.on('join', async (payload, ack) => {
    const respond = typeof ack === 'function' ? ack : () => {}
    const roomId = Number(payload?.roomId)
    if (!roomId) return respond({ ok: false, message: '缺少 roomId' })
    try {
      if (!(await db.isChatEnabled())) return respond({ ok: false, message: '管理员已关闭群聊', code: 'CHAT_DISABLED' })
      const room = await db.getRoom(roomId)
      if (!room) return respond({ ok: false, message: '房间不存在' })
      // 用户资料以 user 表为准（成员记录随之保鲜）；用户不存在则拒绝（防止伪造 token 落脏数据）
      const fresh = await db.loadUserProfile(user.userId, user)
      if (!fresh) return respond({ ok: false, message: '用户不存在或已注销' })
      user.username = fresh.username
      user.nickname = fresh.nickname
      user.avatar = fresh.avatar
      const member = await db.ensureMembership(roomId, user)
      if (!member || member.status !== 'approved') return respond({ ok: false, message: '尚未加入该房间' })
      if (member.avatar) user.avatar = member.avatar

      socket.join(roomKey(roomId))
      socket.data.rooms.add(roomId)
      const first = markOnline(roomId, user.userId, socket.id)
      if (first) await broadcast(roomId, 'presence', presencePayload(user, true), socket.id)
      console.log(`[chat] join room=${roomId} user=${user.userId}(${user.nickname})`)
      respond({ ok: true, roomId, onlineUserIds: onlineUserIds(roomId) })
    } catch (e) {
      console.error('[chat] join 失败:', e.message)
      respond({ ok: false, message: '加入房间失败：' + e.message })
    }
  })

  socket.on('leave', async payload => {
    const roomId = Number(payload?.roomId)
    if (!roomId || !socket.data.rooms.has(roomId)) return
    await leaveRoom(roomId)
  })

  socket.on('chat:message', async (payload, ack) => {
    const respond = typeof ack === 'function' ? ack : () => {}
    const roomId = Number(payload?.roomId)
    const msgType = ALLOWED_MSG_TYPES.has(payload?.msgType) ? payload.msgType : 'text'
    let content = typeof payload?.content === 'string' ? payload.content : ''
    if (!roomId || !socket.data.rooms.has(roomId)) return respond({ ok: false, message: '尚未加入该房间' })
    if (content.trim().length === 0) return respond({ ok: false, message: '消息内容不能为空' })
    if (content.length > MAX_CONTENT_LEN) content = content.slice(0, MAX_CONTENT_LEN)
    try {
      if (!(await db.isChatEnabled())) { sendError('管理员已关闭群聊'); return respond({ ok: false }) }
      if (!user.admin && user.verified !== 2) {
        sendError('请先完成实名认证后再发言（个人中心 → 实名认证）')
        return respond({ ok: false })
      }
      if (!user.admin && user.perms && user.perms.split(',').map(s => s.trim()).includes('-chat')) {
        sendError('你已被管理员限制聊天')
        return respond({ ok: false })
      }
      const quoteId = Number(payload?.quoteId) || null
      const msg = await db.saveMessage(roomId, user, msgType, content, quoteId)
      // 本机直投 + 跨节点广播（同节点不重复：总线按 nodeId 过滤回声）
      await broadcast(roomId, 'chat:message', msg)
      respond({ ok: true, id: msg.id })
    } catch (e) {
      console.error('[chat] 保存消息失败:', e.message)
      sendError('消息发送失败：' + e.message)
      respond({ ok: false, message: '消息发送失败' })
    }
  })

  socket.on('disconnect', async reason => {
    for (const roomId of [...socket.data.rooms]) await leaveRoom(roomId)
    if (reason !== 'client namespace disconnect') {
      console.log(`[chat] disconnect user=${user.userId} reason=${reason}`)
    }
  })

  async function leaveRoom(roomId) {
    socket.leave(roomKey(roomId))
    socket.data.rooms.delete(roomId)
    const last = markOffline(roomId, user.userId, socket.id)
    if (last) await broadcast(roomId, 'presence', presencePayload(user, false))
  }
})

// ---------- 启动 ----------
async function start() {
  try {
    await db.ping()
    console.log('[db] MySQL 连接正常（web3_forum）')
  } catch (e) {
    console.error('[db] MySQL 连接失败（消息将无法持久化）:', e.message)
  }
  // 不显式绑定 IPv4：默认监听双栈（::），否则 Java 侧 HttpClient 解析 127.0.0.1
  // 时可能走 IPv6 回环 ::1 而连接失败（在线成员查询会静默降级为空）
  httpServer.listen(PORT, () => {
    console.log(`[chat-gateway] 已启动 http://127.0.0.1:${PORT} (Socket.IO v4, 长轮询降级已开启)`)
  })
}

async function shutdown() {
  console.log('[chat-gateway] 正在关闭…')
  io.close()
  await bus.close()
  await db.pool.end().catch(() => {})
  process.exit(0)
}
process.on('SIGINT', shutdown)
process.on('SIGTERM', shutdown)

start()

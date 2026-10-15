'use strict'
// ============================================================
// MySQL 直连（chat_room / chat_room_member / chat_message / chat_config）。
// 网关只承担"实时投递 + 落库"两件事，房间/成员/历史查询等 REST 仍由
// Java forum-service 提供，两边共用同一份表结构，互不写冲突。
// ============================================================
const mysql = require('mysql2/promise')

const pool = mysql.createPool({
  host: process.env.MYSQL_HOST || '127.0.0.1',
  port: Number(process.env.MYSQL_PORT || 3306),
  user: process.env.MYSQL_USERNAME || 'root',
  password: process.env.MYSQL_PASSWORD || '',
  database: 'web3_forum',
  charset: 'utf8mb4',
  // 雪花 ID 为 BIGINT，超出 JS Number 安全范围：一律以字符串读取，避免精度丢失
  supportBigNumbers: true,
  bigNumberStrings: true,
  waitForConnections: true,
  connectionLimit: 8,
  timezone: '+08:00',
})

/** 管理员是否开启群聊（chat_config.chatEnabled，缺失视为开启） */
async function isChatEnabled() {
  const [rows] = await pool.query("SELECT `value` FROM chat_config WHERE `key` = 'chatEnabled' LIMIT 1")
  if (!rows.length) return true
  return rows[0].value !== '0'
}

async function getRoom(roomId) {
  const [rows] = await pool.query('SELECT * FROM chat_room WHERE id = ? LIMIT 1', [roomId])
  return rows[0] || null
}

/**
 * 确保用户是群成员并返回最新成员记录。
 * 与 Java ChatService.ensureMembership 行为一致：存在即校正为 approved 并同步资料，
 * 不存在则直接创建 approved（固定公开群免审批）。
 */
async function ensureMembership(roomId, user) {
  const [rows] = await pool.query(
    'SELECT * FROM chat_room_member WHERE room_id = ? AND user_id = ? LIMIT 1',
    [roomId, user.userId]
  )
  const now = new Date()
  const avatar = user.avatar || null
  if (rows.length) {
    const m = rows[0]
    const dirty =
      m.status !== 'approved' ||
      (user.username && m.username !== user.username) ||
      (user.nickname && m.nickname !== user.nickname) ||
      (avatar && m.avatar !== avatar)
    if (dirty) {
      await pool.query(
        'UPDATE chat_room_member SET status = ?, username = ?, nickname = ?, avatar = ?, updated_at = ? WHERE id = ?',
        ['approved', user.username || m.username || '', user.nickname || m.nickname || '', avatar || m.avatar, now, m.id]
      )
      m.status = 'approved'
      m.username = user.username || m.username
      m.nickname = user.nickname || m.nickname
      m.avatar = avatar || m.avatar
    }
    return m
  }
  // 昵称/用户名兜底空串：列若为 NOT NULL，传 null 会被 MySQL 拒绝（1038/1048）
  const [res] = await pool.query(
    'INSERT INTO chat_room_member (room_id, user_id, username, nickname, avatar, role, status, created_at, updated_at) ' +
    'VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
    [roomId, user.userId, user.username || '', user.nickname || '', avatar, 'member', 'approved', now, now]
  )
  return { id: res.insertId, roomId, userId: user.userId, username: user.username, nickname: user.nickname, avatar, role: 'member', status: 'approved' }
}

/** 用户资料（昵称/头像以 user 表为准，消息冗余字段随之保鲜）；用户不存在返回 null */
async function loadUserProfile(userId, fallback) {
  try {
    const [rows] = await pool.query(
      'SELECT username, nickname, avatar FROM `web3_user`.`user` WHERE id = ? LIMIT 1',
      [userId]
    )
    if (!rows.length) return null
    const u = rows[0]
    return {
      username: u.username || fallback.username || '',
      nickname: u.nickname || u.username || fallback.nickname || '',
      avatar: u.avatar || null,
    }
  } catch {
    return null
  }
}

/** 落库并返回完整消息对象（字段与 Java ChatMessage 对齐，前端零改造） */
async function saveMessage(roomId, user, msgType, content, quoteId) {
  let quotePreview = null
  if (quoteId) {
    try {
      const [rows] = await pool.query('SELECT nickname, content FROM chat_message WHERE id = ? LIMIT 1', [quoteId])
      if (rows.length) {
        let p = rows[0].content || ''
        if (p.length > 60) p = p.slice(0, 60) + '…'
        quotePreview = `${rows[0].nickname || '匿名'}：${p}`
      }
    } catch { /* 引用目标不存在时忽略 */ }
  }
  const createdAt = new Date()
  const [res] = await pool.query(
    'INSERT INTO chat_message (room_id, user_id, username, nickname, avatar, msg_type, content, quote_id, quote_preview, created_at) ' +
    'VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
    [roomId, user.userId, user.username, user.nickname, user.avatar || null, msgType, content, quoteId || null, quotePreview, createdAt]
  )
  return {
    // id/roomId/quoteId 均为自增小整数，统一按 Number 下发，与 Java REST 返回的数字一致，
    // 否则重连回拉历史时「按 id 去重」会因字符串/数字类型不同而失效，出现重复消息。
    // userId 是雪花 ID，必须保持字符串（见 lib/auth.js 的无损提取）。
    id: Number(res.insertId),
    roomId: Number(roomId),
    userId: String(user.userId),
    username: user.username,
    nickname: user.nickname,
    avatar: user.avatar || null,
    msgType,
    content,
    quoteId: quoteId ? Number(quoteId) : null,
    quotePreview,
    createdAt: toIso(createdAt),
  }
}

/** MySQL DATETIME → 前端可解析的本地 ISO 字符串（不带 Z，避免时区偏移） */
function toIso(d) {
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

async function ping() {
  const conn = await pool.getConnection()
  try { await conn.ping() } finally { conn.release() }
}

module.exports = { pool, isChatEnabled, getRoom, ensureMembership, loadUserProfile, saveMessage, toIso, ping }

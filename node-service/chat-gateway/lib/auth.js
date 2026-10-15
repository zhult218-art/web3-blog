'use strict'
// ============================================================
// Socket.IO 握手鉴权：只认 httpOnly Cookie 中的 token，
// 禁止 URL / query / auth payload 携带 token（防日志与 Referer 泄露）。
// JWT 与 Java 侧 JwtUtil 同算法（HS256 + JWT_SECRET 原始 UTF-8 字节）。
// ============================================================
const jwt = require('jsonwebtoken')

const COOKIE_NAME = 'token'

/** 从原始 Cookie 头解析指定键（不引入 cookie-parser 依赖） */
function parseCookie(header, name) {
  if (!header) return null
  for (const part of header.split(';')) {
    const idx = part.indexOf('=')
    if (idx < 0) continue
    if (part.slice(0, idx).trim() === name) {
      const raw = part.slice(idx + 1).trim()
      try { return decodeURIComponent(raw) } catch { return raw }
    }
  }
  return null
}

/** 校验 Cookie 中的 JWT，返回归一化的会话主体；失败抛错 */
function verifyHandshake(handshake) {
  const secret = process.env.JWT_SECRET
  if (!secret) throw new Error('JWT_SECRET 未配置')
  const token = parseCookie(handshake.headers?.cookie, COOKIE_NAME)
  if (!token) throw new Error('未登录：Cookie 中缺少 token')
  const claims = jwt.verify(token, secret, { algorithms: ['HS256'] })
  // userId 为雪花算法生成的长整型（19 位），超出 JS Number 安全范围（2^53）。
  // JWT payload 里它是 JSON number，jsonwebtoken 解析时已丢精度（…386 → …400），
  // 因此不能直接用 claims.userId：改为对原始 payload 文本做无损提取。
  const userId = extractRawUserId(token, claims)
  if (!userId) throw new Error('令牌缺少 userId')
  return {
    userId,
    username: claims.username || '',
    nickname: claims.nickname || claims.username || '',
    perms: claims.perms || null,
    admin: claims.authorities === 'ROLE_ADMIN',
    verified: Number(claims.verified ?? 0),
  }
}

/**
 * 从 JWT 原始 payload 段无损提取 userId（大整数必须以字符串取出）。
 * 先试 "userId":数字 形式，再兼容 "userId":"字符串" 的签发方式。
 * @param {string} token   原始 JWT
 * @param {object} claims  jwt.verify 结果（仅作兜底，可能已丢精度）
 */
function extractRawUserId(token, claims) {
  try {
    const seg = token.split('.')[1]
    if (seg) {
      // base64url → UTF-8 文本（payload 为 ASCII，直接解码即可）
      const json = Buffer.from(seg.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8')
      const m = json.match(/"userId"\s*:\s*"?(\d{1,20})"?/)
      if (m) return m[1]
    }
  } catch { /* 解码失败则退回 claims */ }
  return claims.userId == null ? '' : String(claims.userId)
}

module.exports = { verifyHandshake, parseCookie, COOKIE_NAME }

// ============================================================
// 公会系统接口（guild，走网关 /api）
// 公会列表 / 详情 / 申请入会 / 审批 / 成员列表 / 创建 / 退出
// ============================================================
import request from './request'

// ───────── 公会信息 ─────────
// 公会列表
export const listGuilds = () => request.get('/guild/list')
// 公会详情
export const getGuild = id => request.get(`/guild/${id}`)
// 当前用户的公会状态
export const getMyGuild = () => request.get('/guild/my')

// ───────── 入会申请 ─────────
// 申请加入公会
export const applyGuild = (guildId, message) => request.post('/guild/apply', { guildId, message })
// 退出公会
export const leaveGuild = () => request.post('/guild/leave')

// ───────── 会长操作 ─────────
// 待审核申请列表
export const getPendingMembers = guildId => request.get(`/guild/${guildId}/pending`)
// 审批申请（approve: true 通过 / false 拒绝）
export const approveMember = (memberId, approve) => request.post('/guild/approve', { memberId, approve })
// 已通过成员列表
export const getApprovedMembers = guildId => request.get(`/guild/${guildId}/members`)

// ───────── 创建公会 ─────────
export const createGuild = (name, description) => request.post('/guild/create', { name, description })

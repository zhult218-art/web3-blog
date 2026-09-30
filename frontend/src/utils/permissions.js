// ============================================================
// 页面权限工具
// 默认权限（注册即拥有）：首页 / 社区 / 媒体 / 音乐 / 资源 / 相册 / 友链 / 留言板 / 关于
// 互动权限（注册即拥有，管理员可收回，存储为 -code 负向标记）：评论 / 发帖 / 群聊
// 上传权限（默认仅管理员，需管理员显式授权）：照片 / 音乐 / 视频 / 资源
// 服务权限（需管理员授权）：商城 / 量化 / 工具 / 软件
// 被路由守卫 hasPerm 与后台"用户权限管理"页面共用
// ============================================================

// 默认权限码（注册即拥有，无需授权，不可收回）
export const DEFAULT_PERMS = ['home', 'community', 'media', 'music', 'resources', 'album', 'link', 'comments', 'about']

// 互动权限码（注册即拥有；管理员可收回，权限串中以 "-code" 负向标记存储）
export const ACTION_PERMS = ['comment', 'post', 'chat']

// 上传权限码（默认仅管理员；显式授权 "code" 后该用户才可操作）
export const UPLOAD_PERMS = ['upload:photo', 'upload:music', 'upload:video', 'upload:resource']

// "默认勾选、可收回"的权限集合（权限弹窗据此决定勾选语义）
export const REVOCABLE_PERMS = ACTION_PERMS

// 服务权限码（需管理员在后台授权才可访问）
export const SERVICE_PERMS = ['shop', 'quant', 'tools', 'software', 'qzone']

// 权限分组定义（后台权限设置弹窗的数据源）
export const PERMISSIONS = [
  {
    group: '默认内容（注册即拥有，不可收回）',
    items: [
      { code: 'home', label: '首页' },
      { code: 'community', label: '社区' },
      { code: 'media', label: '媒体' },
      { code: 'music', label: '音乐馆' },
      { code: 'resources', label: '资源' },
      { code: 'album', label: '相册集' },
      { code: 'link', label: '友人帐' },
      { code: 'comments', label: '留言板' },
      { code: 'about', label: '关于' },
    ],
  },
  {
    group: '互动（默认拥有，可收回）',
    items: [
      { code: 'comment', label: '发表评论' },
      { code: 'post', label: '发布帖子' },
      { code: 'chat', label: '群聊发言' },
    ],
  },
  {
    group: '上传（需管理员授权）',
    items: [
      { code: 'upload:photo', label: '上传照片' },
      { code: 'upload:music', label: '上传音乐' },
      { code: 'upload:video', label: '上传视频' },
      { code: 'upload:resource', label: '上传资源' },
    ],
  },
  {
    group: '服务（需管理员授权）',
    items: SERVICE_PERMS.map(code => ({
      code,
      label: { shop: '商城', quant: '量化分析', tools: '实用工具', software: '软件中心', qzone: 'QQ空间备份' }[code] || code,
    })),
  },
]

// 权限码 → 中文名（未匹配时原样返回）
export function permLabel(code) {
  for (const g of PERMISSIONS) {
    const item = g.items.find(i => i.code === code)
    if (item) return item.label
  }
  // 负向码（-comment）展示为收回语义
  if (code && code.startsWith('-')) return '收回' + permLabel(code.slice(1))
  return code
}

// 计算用户实际生效的权限：ADMIN 返回 null（拥有全部）；
// 计算规则：
//   1. 基础集 = 默认权限 + 互动权限（注册即拥有）；
//   2. 权限串中的正向码（shop、upload:photo 等）叠加进入基础集；
//   3. 负向码（-comment 等）从基础集中移除（管理员收回互动权限）。
// （修复：此前显式权限会替代默认权限，导致仅有 quant/tools 的用户回首页触发"无限重定向"）
export function effectivePerms(user) {
  const base = [...DEFAULT_PERMS, ...ACTION_PERMS]
  if (!user) return base
  if (user.role === 'ADMIN') return null
  if (Array.isArray(user.permissions) && user.permissions.length) {
    const set = new Set([...base, ...user.permissions.filter(p => p && !p.startsWith('-'))])
    user.permissions.filter(p => p && p.startsWith('-')).forEach(p => set.delete(p.slice(1)))
    return [...set]
  }
  return base
}

// 判断用户是否拥有某权限（页面/互动/上传通用；ADMIN 恒为 true）
export function hasPerm(user, code) {
  if (user && user.role === 'ADMIN') return true
  return effectivePerms(user).includes(code)
}

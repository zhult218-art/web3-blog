// ============================================================
// 页面权限工具
// 默认权限（注册即拥有）：首页 / 社区 / 媒体 / 音乐 / 资源 / 相册 / 友链 / 留言板 / 关于
// 服务权限（需管理员授权）：商城 / 量化 / 工具 / 软件
// 被路由守卫 hasPerm 与后台"用户权限管理"页面共用
// ============================================================

// 默认权限码（注册即拥有，无需授权）
export const DEFAULT_PERMS = ['home', 'community', 'media', 'music', 'resources', 'album', 'link', 'comments', 'about']

// 服务权限码（需管理员在后台授权才可访问）
export const SERVICE_PERMS = ['shop', 'quant', 'tools', 'software']

// 权限分组定义（后台权限设置弹窗的数据源）
export const PERMISSIONS = [
  {
    group: '默认内容（注册即拥有）',
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
    group: '服务（需管理员授权）',
    items: SERVICE_PERMS.map(code => ({
      code,
      label: { shop: '商城', quant: '量化分析', tools: '实用工具', software: '软件中心' }[code] || code,
    })),
  },
]

// 权限码 → 中文名（未匹配时原样返回）
export function permLabel(code) {
  for (const g of PERMISSIONS) {
    const item = g.items.find(i => i.code === code)
    if (item) return item.label
  }
  return code
}

// 计算用户实际生效的权限：ADMIN 返回 null（拥有全部）；
// 默认权限人人恒有，显式授权在此之上叠加（修复：此前显式权限会替代默认权限，
// 导致仅有 quant/tools 的用户回首页触发"无限重定向"）
export function effectivePerms(user) {
  if (!user) return [...DEFAULT_PERMS]
  if (user.role === 'ADMIN') return null
  if (Array.isArray(user.permissions) && user.permissions.length) {
    return [...new Set([...DEFAULT_PERMS, ...user.permissions])]
  }
  return [...DEFAULT_PERMS]
}

// 判断用户是否拥有某页面权限（ADMIN 恒为 true）
export function hasPerm(user, code) {
  if (user && user.role === 'ADMIN') return true
  return effectivePerms(user).includes(code)
}

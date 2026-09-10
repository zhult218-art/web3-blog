// ============================================================
// 分页工具函数（对应后端 MyBatis-Plus 分页响应结构）
// ============================================================

// 构造分页查询参数
export function buildPageParams(page = 1, size = 20) {
  return { page, size }
}

// 解析后端分页响应 { records, total, current, size } → 统一结构
export function parsePageResponse(res) {
  return {
    list: res.data?.records || [],
    total: res.data?.total || 0,
    page: res.data?.current || 1,
    size: res.data?.size || 20
  }
}

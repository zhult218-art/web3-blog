// ============================================================
// 商城与支付接口（shop-service，走网关 /api）
// 商品 / 订单 / 支付
// ============================================================
import request from './request'

// 分页查询商品列表
export const getProductList = params => request.get('/product/list', { params })
// 商品详情
export const getProductDetail = id => request.get(`/product/${id}`)
// 后台：商品管理（仅管理员）
export const createProduct = data => request.post('/product', data)
export const updateProduct = (id, data) => request.put(`/product/${id}`, data)
export const deleteProduct = id => request.delete(`/product/${id}`)
// 创建订单
export const createOrder = data => request.post('/order', data)
// 查询当前用户订单列表
export const getOrderList = params => request.get('/order/list', { params })
// 后台：全量订单列表
export const getAdminOrderList = params => request.get('/order/admin/list', { params })
// 后台：修改订单状态（status: PENDING | PAID | COMPLETED | CANCELLED）
export const updateOrderStatus = (id, status) => request.put(`/order/${id}/status`, null, { params: { status } })
// 订单详情
export const getOrderDetail = id => request.get(`/order/${id}`)
// 取消当前用户的待支付订单（自动回补库存）
export const cancelOrder = id => request.post(`/order/${id}/cancel`)
// 删除当前用户订单（逻辑删除，仅本人）
export const deleteOrder = id => request.delete(`/order/${id}`)
// 发起支付宝支付（返回支付跳转链接）
export const createAlipay = orderId => request.post('/pay/alipay', null, { params: { orderId } })
// 发起微信支付
export const createWechatPay = orderId => request.post('/pay/wechat', null, { params: { orderId } })
// 查询支付结果（支付页轮询使用）
export const getPayStatus = orderId => request.get('/pay/status', { params: { orderId } })

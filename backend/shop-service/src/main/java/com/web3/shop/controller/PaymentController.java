package com.web3.shop.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.BusinessException;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.shop.entity.Order;
import com.web3.shop.service.AlipayService;
import com.web3.shop.service.OrderService;
import com.web3.shop.service.WechatPayService;
import com.web3.shop.vo.OrderVO;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * PaymentController —— 支付接口控制器
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：对接支付宝/微信支付官方 SDK，提供下单、异步回调验签与支付状态查询。
 * <p>
 * 接口分组：
 * <ul>
 *   <li>下单（需登录）：POST /pay/alipay → 返回支付宝支付表单 HTML；POST /pay/wechat → 返回 code_url</li>
 *   <li>异步回调（无需登录，第三方调用）：POST /pay/notify/alipay、POST /pay/notify/wechat</li>
 *   <li>状态查询（需登录）：GET /pay/status</li>
 * </ul>
 * <p>
 * 回调路径放行：网关 JwtManager 对无 token 请求匿名放行，shop-service PublicSecurityConfig 全部 permitAll，
 * 故 /pay/notify/** 可被支付宝/微信服务器直接调用。
 */
@RestController
@RequestMapping("/pay")
@Tag(name = "支付管理", description = "支付宝/微信官方支付下单与异步回调")
public class PaymentController {
    private final OrderService orderService;
    private final AlipayService alipayService;
    private final WechatPayService wechatPayService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public PaymentController(OrderService orderService, AlipayService alipayService,
                             WechatPayService wechatPayService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.orderService = orderService;
        this.alipayService = alipayService;
        this.wechatPayService = wechatPayService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 订单支付状态查询（前端路由：GET /pay/status，需登录）
     *
     * @param orderId 订单 ID
     * @param auth    请求头 Authorization 的 JWT 令牌
     * @return 订单支付状态数据
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(@RequestParam Long orderId, @RequestHeader(value = "Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        OrderVO order = orderService.getDetailOwned(orderId, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("status", order.getStatus());
        data.put("paidAt", order.getPaidAt());
        data.put("tradeNo", order.getTradeNo());
        data.put("payChannel", order.getPayChannel());
        return ApiResponse.ok(data);
    }

    /**
     * 支付宝电脑网站支付下单（前端路由：POST /pay/alipay，需登录）
     * <p>
     * 调用支付宝 SDK 生成支付表单 HTML，前端用新窗口 document.write(html) 跳转到支付宝收银台。
     *
     * @param orderId 订单 ID
     * @param auth    请求头 Authorization 的 JWT 令牌
     * @return 含 formHtml 字段（完整 form 表单 HTML 字符串）
     */
    @PostMapping("/alipay")
    public ApiResponse<Map<String, Object>> alipay(@RequestParam Long orderId, @RequestHeader(value = "Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        OrderVO owned = orderService.getDetailOwned(orderId, userId);
        if (!"PENDING".equals(owned.getStatus())) {
            throw new BusinessException(409, "订单状态不可支付: " + owned.getStatus());
        }
        Order order = orderService.getById(orderId);
        String formHtml = alipayService.createPagePay(order);
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("channel", "alipay");
        data.put("formHtml", formHtml);
        data.put("status", owned.getStatus());
        return ApiResponse.ok(data);
    }

    /**
     * 微信 Native 扫码支付下单（前端路由：POST /pay/wechat，需登录）
     * <p>
     * 调用微信支付 SDK 生成 code_url，前端用此字符串生成二维码展示，用户微信扫码完成支付。
     *
     * @param orderId 订单 ID
     * @param auth    请求头 Authorization 的 JWT 令牌
     * @return 含 codeUrl 字段（weixin://wxpay/bizpayurl?pr=xxx）
     */
    @PostMapping("/wechat")
    public ApiResponse<Map<String, Object>> wechat(@RequestParam Long orderId, @RequestHeader(value = "Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        OrderVO owned = orderService.getDetailOwned(orderId, userId);
        if (!"PENDING".equals(owned.getStatus())) {
            throw new BusinessException(409, "订单状态不可支付: " + owned.getStatus());
        }
        Order order = orderService.getById(orderId);
        String codeUrl = wechatPayService.createNativePay(order);
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("channel", "wechat");
        data.put("codeUrl", codeUrl);
        data.put("status", owned.getStatus());
        return ApiResponse.ok(data);
    }

    /**
     * 支付宝异步回调（前端路由：POST /pay/notify/alipay，无需登录）
     * <p>
     * 支付宝在用户支付成功后 POST 此地址（form 参数），需用支付宝公钥验签。
     * 验签通过后用 out_trade_no（订单 ID）+ trade_no（支付宝交易号）更新订单为 PAID。
     * 返回 "success" 字符串（非 JSON），支付宝收到后停止重试，否则按 1m/5m/10m/...重试最多 8 次。
     *
     * @param request HTTP 请求（含支付宝回调的所有 form 参数）
     * @return "success" 表示处理成功；"fail" 表示验签失败或订单不存在
     */
    @PostMapping("/notify/alipay")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v != null && v.length > 0 ? v[0] : ""));
        if (!alipayService.verifyNotify(params)) {
            return "fail";
        }
        String tradeStatus = params.get("trade_status");
        // 只在 TRADE_SUCCESS / TRADE_FINISHED 时更新订单（WAIT_BUYER_PAYMENT 等中间态忽略）
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "success";
        }
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        if (outTradeNo == null || outTradeNo.isBlank()) {
            return "fail";
        }
        try {
            Long orderId = Long.parseLong(outTradeNo);
            // 幂等：已支付订单返回 0，仍返回 success 让支付宝停止重试
            orderService.markPaidByNotify(orderId, "alipay", tradeNo);
            return "success";
        } catch (NumberFormatException e) {
            return "fail";
        }
    }

    /**
     * 微信支付异步回调（前端路由：POST /pay/notify/wechat，无需登录）
     * <p>
     * 微信支付在用户支付成功后 POST 此地址（JSON body + 4 个 Wechatpay-* 请求头），
     * SDK 内部用平台证书公钥验签 + APIv3 密钥解密 resource.ciphertext，返回 Transaction。
     * 解析成功后用 out_trade_no（订单 ID）+ transaction_id 更新订单为 PAID。
     * 返回 200 + {"code":"SUCCESS"} 让微信停止重试。
     *
     * @param request HTTP 请求（含 Wechatpay-Signature/Serial/Nonce/Timestamp 头与 JSON body）
     * @return {"code":"SUCCESS"} 表示处理成功；{"code":"FAIL","message":"..."} 表示失败
     */
    @PostMapping("/notify/wechat")
    public Map<String, Object> wechatNotify(HttpServletRequest request) {
        String serial = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String signature = request.getHeader("Wechatpay-Signature");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String body;
        try {
            body = request.getReader().lines().reduce("", (a, b) -> a + b);
        } catch (Exception e) {
            Map<String, Object> r = new HashMap<>();
            r.put("code", "FAIL");
            r.put("message", "读取请求体失败: " + e.getMessage());
            return r;
        }
        Transaction transaction;
        try {
            transaction = wechatPayService.parseNotify(serial, nonce, signature, timestamp, body);
        } catch (BusinessException e) {
            Map<String, Object> r = new HashMap<>();
            r.put("code", "FAIL");
            r.put("message", e.getMessage());
            return r;
        }
        if (transaction == null || transaction.getOutTradeNo() == null) {
            Map<String, Object> r = new HashMap<>();
            r.put("code", "FAIL");
            r.put("message", "回调解析为空");
            return r;
        }
        // 仅在支付成功时更新订单；其他状态（NOTPAY/CLOSED/REVOKED 等）记录日志但仍 ack
        if (transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS) {
            Map<String, Object> r = new HashMap<>();
            r.put("code", "SUCCESS");
            return r;
        }
        try {
            Long orderId = Long.parseLong(transaction.getOutTradeNo());
            String transactionId = transaction.getTransactionId();
            // 幂等：已支付订单返回 0，仍返回 SUCCESS 让微信停止重试
            orderService.markPaidByNotify(orderId, "wechat", transactionId);
            Map<String, Object> r = new HashMap<>();
            r.put("code", "SUCCESS");
            return r;
        } catch (NumberFormatException e) {
            Map<String, Object> r = new HashMap<>();
            r.put("code", "FAIL");
            r.put("message", "out_trade_no 非法");
            return r;
        }
    }
}

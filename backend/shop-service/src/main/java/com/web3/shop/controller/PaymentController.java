/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.web3.common.core.ApiResponse
 *  io.swagger.v3.oas.annotations.tags.Tag
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.shop.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.UnauthorizedException;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.shop.service.OrderService;
import com.web3.shop.vo.OrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/pay"})
@Tag(name="\u652f\u4ed8\u7ba1\u7406", description="\u8ba2\u5355\u652f\u4ed8\u72b6\u6001\u67e5\u8be2\u4e0e\u6a21\u62df\u652f\u4ed8")
/**
 * PaymentController —— 支付接口控制器
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：提供订单支付相关的对外 REST 接口，包括支付状态查询与模拟支付（支付宝/微信渠道），
 * 模拟支付成功后将订单状态置为 PAID 并记录支付时间。所有操作仅限订单所有者本人。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/pay")：对外接口统一前缀为 /pay</li>
 *   <li>@Tag：OpenAPI/Swagger 文档分组标签（支付管理）</li>
 * </ul>
 */
public class PaymentController {
    /** 订单业务服务 */
    private final OrderService orderService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public PaymentController(OrderService orderService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.orderService = orderService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 校验订单归属并返回订单（内部工具方法）
     * 从令牌解析用户 ID，仅订单所有者可访问，否则抛 403 无权限错误
     *
     * @param orderId 订单 ID
     * @param auth    Authorization 请求头原始值（Bearer token）
     * @return 当前用户自己的订单视图对象
     */
    private OrderVO requireOwnedOrder(Long orderId, String auth) {
        Long userId = AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        return this.orderService.getDetailOwned(orderId, userId);
    }

    /**
     * 订单支付状态查询（前端路由：GET /pay/status，需登录）
     * 返回订单 ID、支付状态与支付时间（仅限订单所有者）
     *
     * @param orderId 订单 ID
     * @param auth    请求头 Authorization 的 JWT 令牌
     * @return 订单支付状态数据
     */
    @GetMapping(value={"/status"})
    public ApiResponse<Map<String, Object>> status(@RequestParam Long orderId, @RequestHeader(value="Authorization") String auth) {
        OrderVO order = this.requireOwnedOrder(orderId, auth);
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("orderId", orderId);
        data.put("status", order.getStatus());
        data.put("paidAt", order.getPaidAt());
        return ApiResponse.ok(data);
    }

    @PostMapping(value={"/alipay"})
    public ApiResponse<Map<String, Object>> alipay(@RequestParam Long orderId, @RequestHeader(value="Authorization") String auth) {
        return this.pay(orderId, "alipay", auth);
    }

    /**
     * 微信渠道模拟支付（前端路由：POST /pay/wechat，需登录）
     *
     * @param orderId 订单 ID
     * @param auth    请求头 Authorization 的 JWT 令牌
     * @return 支付结果数据（订单 ID、渠道、状态、支付时间）
     */
    @PostMapping(value={"/wechat"})
    public ApiResponse<Map<String, Object>> wechat(@RequestParam Long orderId, @RequestHeader(value="Authorization") String auth) {
        return this.pay(orderId, "wechat", auth);
    }

    /**
     * 模拟支付核心逻辑（内部工具方法）
     * 订单必须属于当前用户且处于 PENDING 状态；通过原子 UPDATE 完成状态转换，
     * 防止并发重复支付与对已取消订单的支付；成功后记录渠道与支付时间
     *
     * @param orderId 订单 ID
     * @param channel 支付渠道（alipay/wechat）
     * @param auth    Authorization 请求头原始值（Bearer token）
     * @return 支付结果数据
     */
    private ApiResponse<Map<String, Object>> pay(Long orderId, String channel, String auth) {
        OrderVO paid = this.orderService.payOrder(orderId, AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties), channel);
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("orderId", orderId);
        data.put("channel", channel);
        data.put("status", paid.getStatus());
        data.put("paidAt", paid.getPaidAt());
        return ApiResponse.ok(data);
    }
}

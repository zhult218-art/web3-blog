package com.web3.shop.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.shop.dto.OrderCreateDTO;
import com.web3.shop.service.OrderService;
import com.web3.shop.vo.OrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OrderController —— 订单接口控制器
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：管理商城订单的对外 REST 接口，包括创建订单（扣减库存）、当前用户订单分页列表、
 * 本人订单详情、管理员全量订单列表与订单状态更新。普通用户接口均从令牌解析用户 ID，
 * 仅能访问属于自己的订单（getDetailOwned 校验归属）。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>{@code @RestController}：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>{@code @RequestMapping("/order")}：对外接口统一前缀为 /order</li>
 *   <li>{@code @Tag}：OpenAPI/Swagger 文档分组标签（订单管理）</li>
 * </ul>
 */
@RestController
@RequestMapping("/order")
@Tag(name = "订单管理", description = "订单创建与查询")
public class OrderController {
    /** 订单业务服务 */
    private final OrderService orderService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public OrderController(OrderService orderService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.orderService = orderService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 创建订单（前端路由：POST /order，需登录）
     * 从令牌解析用户 ID，创建初始状态为 PENDING 的订单并原子扣减商品库存
     *
     * @param dto           订单创建参数（商品 ID、数量）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 创建成功后的订单视图对象
     */
    @PostMapping
    public ApiResponse<OrderVO> create(@RequestBody OrderCreateDTO dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthUtils.requireUserId(authorization, jwtUtil, jwtProperties);
        return ApiResponse.ok(orderService.createOrder(dto, userId));
    }

    /**
     * 当前用户订单分页列表（前端路由：GET /order/list，需登录）
     * 按创建时间倒序返回当前用户自己的订单
     *
     * @param page          页码，默认 1
     * @param size          每页条数，默认 20
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 分页的订单视图对象列表
     */
    @GetMapping("/list")
    public ApiResponse<PageResult<OrderVO>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size, @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthUtils.requireUserId(authorization, jwtUtil, jwtProperties);
        return ApiResponse.ok(orderService.pageByUser(userId, page, size));
    }

    /**
     * 订单详情（前端路由：GET /order/{id}，需登录）
     * 仅能查看属于当前用户的订单，否则抛 403 无权限错误
     *
     * @param id            订单 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 订单视图对象
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderVO> getDetail(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthUtils.requireUserId(authorization, jwtUtil, jwtProperties);
        return ApiResponse.ok(orderService.getDetailOwned(id, userId));
    }

    /**
     * 用户取消订单（前端路由：POST /order/{id}/cancel，需登录）
     * 仅订单所有者可取消，且订单必须处于 PENDING 状态；取消后自动回补商品库存
     *
     * @param id            订单 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 取消后的订单视图对象
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderVO> cancel(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthUtils.requireUserId(authorization, jwtUtil, jwtProperties);
        return ApiResponse.ok(orderService.cancelOwned(id, userId));
    }

    /**
     * 用户删除订单（前端路由：DELETE /order/{id}，需登录，仅本人可删）
     * 逻辑删除，仅订单所有者可操作；删除后不再出现在个人订单列表中。
     *
     * @param id            订单 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = AuthUtils.requireUserId(authorization, jwtUtil, jwtProperties);
        orderService.deleteOwned(id, userId);
        return ApiResponse.ok();
    }

    /**
     * 管理端订单全量列表（前端路由：GET /order/admin/list，需管理员）
     * 可按订单状态筛选，按创建时间倒序返回
     *
     * @param page          页码，默认 1
     * @param size          每页条数，默认 20
     * @param status        订单状态筛选（如 PENDING/PAID），可为空
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 分页的订单视图对象列表
     */
    @GetMapping("/admin/list")
    public ApiResponse<PageResult<OrderVO>> adminList(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String status, @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUtils.requireAdmin(authorization, jwtUtil, jwtProperties);
        return ApiResponse.ok(orderService.pageAll(page, size, status));
    }

    /**
     * 更新订单状态（前端路由：PUT /order/{id}/status，需管理员）
     * 状态变为 PAID 时自动记录支付时间
     *
     * @param id            订单 ID
     * @param status        目标状态（如 PAID/SHIPPED/COMPLETED/CANCELLED）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Object> updateStatus(@PathVariable Long id, @RequestParam String status, @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUtils.requireAdmin(authorization, jwtUtil, jwtProperties);
        orderService.updateStatus(id, status);
        return ApiResponse.ok();
    }
}

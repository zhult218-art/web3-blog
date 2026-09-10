/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.web3.common.core.ApiResponse
 *  com.web3.common.core.PageResult
 *  io.swagger.v3.oas.annotations.tags.Tag
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.shop.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.shop.dto.ProductCreateDTO;
import com.web3.shop.service.ProductService;
import com.web3.shop.vo.ProductVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/product"})
@Tag(name="\u5546\u54c1\u7ba1\u7406", description="\u5546\u57ce\u5546\u54c1\u7684\u589e\u5220\u6539\u67e5")
/**
 * ProductController —— 商品接口控制器
 * <p>
 * 所属模块：shop-service（商城模块）。
 * <p>
 * 职责：管理商城商品的对外 REST 接口，包括商品分页列表、详情查询，
 * 以及商品的创建/删除/更新（仅管理员）。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/product")：对外接口统一前缀为 /product</li>
 *   <li>@Tag：OpenAPI/Swagger 文档分组标签（商品管理）</li>
 * </ul>
 */
public class ProductController {
    /** 商品业务服务 */
    private final ProductService productService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public ProductController(ProductService productService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.productService = productService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 商品分页列表（前端路由：GET /product/list）
     * 支持按商品名称关键字与分类筛选，按创建时间倒序返回
     *
     * @param page     页码，默认 1
     * @param size     每页条数，默认 20
     * @param keyword  商品名称关键字，可为空
     * @param category 分类，可为空
     * @return 分页的商品视图对象列表
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<ProductVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="") String keyword, @RequestParam(defaultValue="") String category) {
        return ApiResponse.ok(this.productService.page(page, size, keyword, category));
    }

    /**
     * 商品详情（前端路由：GET /product/{id}）
     *
     * @param id 商品 ID
     * @return 商品视图对象
     */
    @GetMapping(value={"/{id}"})
    public ApiResponse<ProductVO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(this.productService.getDetail(id));
    }

    /**
     * 创建商品（前端路由：POST /product，需管理员）
     *
     * @param dto          商品创建参数
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 创建成功后的商品视图对象
     */
    @PostMapping
    public ApiResponse<ProductVO> create(@RequestBody ProductCreateDTO dto, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.productService.create(dto));
    }

    /**
     * 删除商品（前端路由：DELETE /product/{id}，需管理员）
     *
     * @param id           商品 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        this.productService.delete(id);
        return ApiResponse.ok();
    }

    /**
     * 更新商品（前端路由：PUT /product/{id}，需管理员）
     * 仅更新传入的非空且合法字段（名称/描述/价格/库存/封面/分类）
     *
     * @param id           商品 ID
     * @param dto          商品更新参数
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 更新后的商品视图对象
     */
    @org.springframework.web.bind.annotation.PutMapping(value={"/{id}"})
    public ApiResponse<ProductVO> update(@PathVariable Long id, @RequestBody ProductCreateDTO dto, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.productService.update(id, dto));
    }
}

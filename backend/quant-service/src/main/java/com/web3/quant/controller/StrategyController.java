/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.web3.common.core.ApiResponse
 *  com.web3.common.core.PageResult
 *  com.web3.common.security.JwtUtil
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.quant.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.quant.entity.Strategy;
import com.web3.quant.service.StrategyService;
import com.web3.quant.vo.StrategyVO;
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
 * 策略管理控制器(quant-service 模块)。
 *
 * <p>负责对外暴露量化策略的增删改查与运行接口,管理用户自定义的量化交易策略。
 * 所有接口统一以 {@code /strategy} 为前缀,由 {@code @RestController} 声明为
 * Spring MVC 控制器,{@code @RequestMapping("/strategy")} 定义接口前缀。</p>
 *
 * <p>创建、更新、删除、运行等写操作接口均要求请求头携带 {@code Authorization}
 * 登录令牌,并通过 {@link com.web3.common.security.AuthUtils} 校验 JWT 有效性。</p>
 */
@RestController
@RequestMapping(value={"/strategy"})
public class StrategyController {
    private final StrategyService strategyService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public StrategyController(StrategyService strategyService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.strategyService = strategyService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 分页查询策略列表(前端路由 GET /strategy/list)。
     *
     * @param page 页码,从 1 开始,默认 1
     * @param size 每页条数,默认 20
     * @return 统一响应体,data 为策略分页结果 PageResult&lt;StrategyVO&gt;
     */
    @GetMapping(value={"/list"})
    public ApiResponse<PageResult<StrategyVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        return ApiResponse.ok(this.strategyService.page(page, size));
    }

    /**
     * 根据 ID 查询策略详情(前端路由 GET /strategy/{id})。
     *
     * @param id 策略 ID(路径参数)
     * @return 策略详情;不存在时返回 code=404 与提示信息
     */
    @GetMapping(value={"/{id}"})
    public ApiResponse<StrategyVO> getById(@PathVariable Long id) {
        StrategyVO vo = this.strategyService.findById(id);
        if (vo == null) {
            return ApiResponse.fail((int)404, (String)"Strategy not found");
        }
        return ApiResponse.ok(vo);
    }

    /**
     * 新建策略(前端路由 POST /strategy)。
     *
     * @param strategy     策略信息(request body)
     * @param authorization 登录令牌(请求头),必须是已登录用户
     * @return 创建成功后的策略详情
     */
    @PostMapping
    public ApiResponse<StrategyVO> create(@RequestBody Strategy strategy, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireUserId(authorization, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.strategyService.create(strategy));
    }

    /**
     * 更新策略(前端路由 PUT /strategy/{id})。
     *
     * @param id       策略 ID(路径参数,覆盖 request body 中的 id)
     * @param strategy 更新后的策略信息
     * @param authorization 登录令牌
     * @return 更新后的策略详情;策略不存在时返回 code=404
     */
    @PutMapping(value={"/{id}"})
    public ApiResponse<StrategyVO> update(@PathVariable Long id, @RequestBody Strategy strategy, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireUserId(authorization, this.jwtUtil, this.jwtProperties);
        StrategyVO vo = this.strategyService.updateStrategy(id, strategy);
        if (vo == null) {
            return ApiResponse.fail((int)404, (String)"Strategy not found");
        }
        return ApiResponse.ok(vo);
    }

    /**
     * 删除策略(前端路由 DELETE /strategy/{id})。
     *
     * @param id 策略 ID
     * @param authorization 登录令牌
     * @return 删除成功返回空 data 的成功响应
     */
    @DeleteMapping(value={"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireUserId(authorization, this.jwtUtil, this.jwtProperties);
        this.strategyService.deleteStrategy(id);
        return ApiResponse.ok();
    }

    /**
     * 运行/回测策略(前端路由 POST /strategy/{id}/run),
     * 执行时会向 quant_log 表写入一条执行日志。
     *
     * @param id 策略 ID
     * @param authorization 登录令牌
     * @return 触发成功返回空 data 的成功响应
     */
    @PostMapping(value={"/{id}/run"})
    public ApiResponse<Object> run(@PathVariable Long id, @RequestHeader(value="Authorization") String authorization) {
        AuthUtils.requireUserId(authorization, this.jwtUtil, this.jwtProperties);
        this.strategyService.runStrategy(id);
        return ApiResponse.ok();
    }
}

package com.web3.quant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.common.core.ApiResponse;
import com.web3.common.core.UnauthorizedException;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.quant.entity.Strategy;
import com.web3.quant.mapper.StrategyMapper;
import com.web3.quant.entity.QuantLog;
import com.web3.quant.mapper.QuantLogMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 量化日志控制器(quant-service 模块)。
 *
 * <p>负责对外暴露量化策略的运行日志查询接口,方便前端展示策略执行的
 * 历史记录,接口前缀为 {@code /quant}(由 {@code @RestController} 与
 * {@code @RequestMapping("/quant")} 声明)。</p>
 */
@RestController
@RequestMapping(value={"/quant"})
public class QuantLogController {
    private final QuantLogMapper quantLogMapper;
    private final StrategyMapper strategyMapper;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public QuantLogController(QuantLogMapper quantLogMapper, StrategyMapper strategyMapper,
                              JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.quantLogMapper = quantLogMapper;
        this.strategyMapper = strategyMapper;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 按策略 ID 查询运行日志(前端路由 GET /quant/logs?strategyId=xxx),
     * 按创建时间倒序返回该策略的全部日志。
     *
     * @param strategyId 策略 ID(必填查询参数)
     * @return 该策略的 QuantLog 日志列表
     */
    @GetMapping(value={"/logs"})
    public ApiResponse<List<QuantLog>> logs(@RequestParam Long strategyId,
                                             @RequestHeader("Authorization") String authorization) {
        Long userId = AuthUtils.requireUserId(authorization, jwtUtil, jwtProperties);
        Strategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) {
            return ApiResponse.fail(404, "Strategy not found");
        }
        boolean admin = false;
        try {
            AuthUtils.requireAdmin(authorization, jwtUtil, jwtProperties);
            admin = true;
        } catch (UnauthorizedException ex) {
            if (ex.getCode() != 403) throw ex;
        }
        if (!admin && (strategy.getUserId() == null || !strategy.getUserId().equals(userId))) {
            throw new UnauthorizedException(403, "无权查看该策略的运行日志");
        }
        List<QuantLog> logs = this.quantLogMapper.selectList(new LambdaQueryWrapper<QuantLog>()
                .eq(QuantLog::getStrategyId, strategyId)
                .orderByDesc(QuantLog::getCreatedAt));
        return ApiResponse.ok(logs);
    }
}

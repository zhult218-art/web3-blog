/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  com.web3.common.core.ApiResponse
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.quant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.common.core.ApiResponse;
import com.web3.quant.entity.QuantLog;
import com.web3.quant.mapper.QuantLogMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public QuantLogController(QuantLogMapper quantLogMapper) {
        this.quantLogMapper = quantLogMapper;
    }

    /**
     * 按策略 ID 查询运行日志(前端路由 GET /quant/logs?strategyId=xxx),
     * 按创建时间倒序返回该策略的全部日志。
     *
     * @param strategyId 策略 ID(必填查询参数)
     * @return 该策略的 QuantLog 日志列表
     */
    @GetMapping(value={"/logs"})
    public ApiResponse<List<QuantLog>> logs(@RequestParam Long strategyId) {
        List<QuantLog> logs = this.quantLogMapper.selectList(new LambdaQueryWrapper<QuantLog>().eq(QuantLog::getStrategyId, (Object)strategyId).orderByDesc(QuantLog::getCreatedAt));
        return ApiResponse.ok(logs);
    }
}

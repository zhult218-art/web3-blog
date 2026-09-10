package com.web3.aiproxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.aiproxy.entity.ProxyRequestLog;
import com.web3.aiproxy.mapper.ProxyAlertMapper;
import com.web3.aiproxy.mapper.ProxyRequestLogMapper;
import com.web3.aiproxy.service.DashboardService;
import com.web3.common.core.ApiResponse;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 类名：AdminStatsController
 * 所属模块：ai-proxy-service（中转站）
 * 职责：用量统计与日志接口（需管理员 JWT）——
 *       总览 / 按用户 / 按模型 / 按日趋势 / 时段热度 / 错误分布 / 智能看板 / 智能洞察 /
 *       日志明细分页（用户·模型·状态·时间范围筛选）。
 */
@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AdminStatsController {

    private final ProxyRequestLogMapper logMapper;
    private final ProxyAlertMapper alertMapper;
    private final DashboardService dashboardService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    /** 总览：今日请求数/消耗/token/活跃令牌 + 环比 */
    @GetMapping("/stats/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(dashboardService.overview());
    }

    /** 按用户聚合（近 30 天用量排行） */
    @GetMapping("/stats/by-user")
    public ApiResponse<List<Map<String, Object>>> byUser(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(logMapper.byUser(LocalDateTime.now().minusDays(30)));
    }

    /** 按模型聚合（近 30 天） */
    @GetMapping("/stats/by-model")
    public ApiResponse<List<Map<String, Object>>> byModel(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(logMapper.byModel(LocalDateTime.now().minusDays(30)));
    }

    /** 按日趋势（默认 14 天） */
    @GetMapping("/stats/trend")
    public ApiResponse<List<Map<String, Object>>> trend(
            @RequestParam(defaultValue = "14") int days,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        int safeDays = Math.min(Math.max(days, 1), 90);
        return ApiResponse.ok(logMapper.trend(LocalDate.now().minusDays(safeDays - 1L).atStartOfDay()));
    }

    /** 时段热度（近 24h） */
    @GetMapping("/stats/hourly")
    public ApiResponse<List<Map<String, Object>>> hourly(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(logMapper.hourly(LocalDateTime.now().minusHours(24)));
    }

    /** 错误分布（近 24h） */
    @GetMapping("/stats/errors")
    public ApiResponse<List<Map<String, Object>>> errors(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(logMapper.errorDist(LocalDateTime.now().minusHours(24)));
    }

    /** 智能看板：总览 + 趋势 + 分布 + 排行 + 时段，一次拉全 */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(dashboardService.dashboard());
    }

    /** 智能洞察：规则引擎输出的运营建议与风险告警 */
    @GetMapping("/insights")
    public ApiResponse<List<Map<String, Object>>> insights(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(dashboardService.insights());
    }

    /** 最近告警记录（低余额/耗尽/渠道故障/限流，dedup 幂等去重） */
    @GetMapping("/alerts")
    public ApiResponse<List<Map<String, Object>>> alerts(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(alertMapper.recent(Math.min(Math.max(limit, 1), 200)));
    }

    /** 日志明细：分页 + 令牌/模型/状态/时间范围筛选 */
    @GetMapping("/logs")
    public ApiResponse<PageResult<ProxyRequestLog>> logs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long tokenId,
            @RequestParam(required = false) String tokenName,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        LocalDateTime start = StringUtils.hasText(startDate)
                ? LocalDate.parse(startDate).atStartOfDay() : null;
        LocalDateTime end = StringUtils.hasText(endDate)
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : null;

        LambdaQueryWrapper<ProxyRequestLog> wrapper = Wrappers.<ProxyRequestLog>lambdaQuery()
                .eq(tokenId != null, ProxyRequestLog::getTokenId, tokenId)
                .eq(StringUtils.hasText(tokenName), ProxyRequestLog::getTokenName, tokenName)
                .like(StringUtils.hasText(model), ProxyRequestLog::getModelName, model)
                .eq(statusCode != null, ProxyRequestLog::getStatusCode, statusCode)
                .ge(start != null, ProxyRequestLog::getCreatedAt, start)
                .le(end != null, ProxyRequestLog::getCreatedAt, end)
                .orderByDesc(ProxyRequestLog::getId);

        var result = logMapper.selectPage(new Page<>(page, Math.min(size, 200)), wrapper);
        return ApiResponse.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }
}

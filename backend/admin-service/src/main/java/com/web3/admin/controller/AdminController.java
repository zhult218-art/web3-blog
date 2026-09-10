/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.web3.common.core.ApiResponse
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.web3.admin.controller;

import com.web3.admin.service.AdminService;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类名：AdminController
 * 所属模块：admin-service（管理后台服务）
 * 职责：管理后台控制器，管理控制台概览统计、操作日志查询、访问记录上报/分页查询/汇总统计等功能（除访问上报外均需管理员权限）。
 * 关键注解：@RestController 声明为 REST 控制器；@RequestMapping("/admin") 对外接口前缀为 /admin。
 */
@RestController
@RequestMapping(value={"/admin"})
public class AdminController {
    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 前端调用路由：GET /admin/dashboard（需管理员权限）。
     * 职责：返回控制台概览数据（用户数、文章数、帖子数、订单数、营收、活跃用户、今日访问、系统健康状态、各服务状态）。
     *
     * @param auth Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 时 data 为概览统计 Map
     */
    @GetMapping(value={"/dashboard"})
    public ApiResponse<Map<String, Object>> getDashboard(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.adminService.getDashboardOverview());
    }

    /**
     * 前端调用路由：GET /admin/logs（需管理员权限）。
     * 职责：查询最近的管理操作日志（最多 100 条）。
     *
     * @param limit 日志条数，默认 50
     * @param auth  Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 时 data 为 OperationLog 列表
     */
    @GetMapping(value={"/logs"})
    public ApiResponse<List<?>> getLogs(@RequestParam(defaultValue="50") Integer limit,
                                        @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.adminService.getRecentLogs(limit));
    }

    /**
     * 前端调用路由：POST /admin/visit（无需登录，供前端埋点上报）。
     * 职责：上报一次页面访问记录（IP、地域、经纬度、UA、页面路径等），用于访客分析。
     *
     * @param body 访问数据（ip/country/region/city/isp/latitude/longitude/userAgent/pagePath）
     * @return code=200 时 data 包含新记录 ID
     */
    @PostMapping(value={"/visit"})
    public ApiResponse<Map<String, Object>> recordVisit(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(this.adminService.recordVisit(body));
    }

    /**
     * 前端调用路由：GET /admin/visit/online（无需登录，供前台展示在线人数）
     * 职责：统计最近 10 分钟内有访问记录的去重 IP 数，作为真实在线人数。
     * @return code=200 且 data 包含 onlineCount（Long）
     */
    @GetMapping(value={"/visit/online"})
    public ApiResponse<Map<String, Object>> onlineUsers() {
        return ApiResponse.ok(this.adminService.countOnlineUsers());
    }

    /**
     * 前端调用路由：GET /admin/visit/list（需管理员权限）。
     * 职责：分页查询访问记录（按时间倒序，每页上限 100 条）。
     *
     * @param page 页码，默认 1
     * @param size 每页条数，默认 20
     * @param auth Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 时 data 包含 total（总条数）与 records（记录列表）
     */
    @GetMapping(value={"/visit/list"})
    public ApiResponse<Map<String, Object>> visitList(@RequestParam(defaultValue="1") Integer page,
                                                      @RequestParam(defaultValue="20") Integer size,
                                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.adminService.listVisits(page, size));
    }

    /**
     * 前端调用路由：GET /admin/visit/summary（需管理员权限）。
     * 职责：返回访问汇总（总访问量、今日访问量、近 7 天独立 IP 数、近 7 天已地理定位条数）。
     *
     * @param auth Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 时 data 为汇总统计 Map
     */
    @GetMapping(value={"/visit/summary"})
    public ApiResponse<Map<String, Object>> visitSummary(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.adminService.visitSummary());
    }
}

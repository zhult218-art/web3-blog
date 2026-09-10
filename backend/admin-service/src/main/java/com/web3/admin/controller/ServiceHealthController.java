package com.web3.admin.controller;

import com.web3.admin.service.AdminService;
import com.web3.admin.service.ServiceHealthService;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/services")
public class ServiceHealthController {
    private final ServiceHealthService healthService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public ServiceHealthController(ServiceHealthService healthService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.healthService = healthService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /** GET /admin/services/health — 所有服务健康状态 */
    @GetMapping("/health")
    public ApiResponse<List<Map<String, Object>>> health(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(healthService.getAllStatus());
    }

    /** GET /admin/services/status — 服务状态 Map（给导航栏用，无需管理员） */
    @GetMapping("/status")
    public ApiResponse<Map<String, String>> statusMap() {
        return ApiResponse.ok(healthService.getStatusMap());
    }

    /** POST /admin/services/{name}/start — 启动服务 */
    @PostMapping("/{name}/start")
    public ApiResponse<Map<String, Object>> start(
            @PathVariable String name,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(healthService.startService(name));
    }

    /** POST /admin/services/{name}/stop — 停止服务 */
    @PostMapping("/{name}/stop")
    public ApiResponse<Map<String, Object>> stop(
            @PathVariable String name,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(healthService.stopService(name));
    }
}

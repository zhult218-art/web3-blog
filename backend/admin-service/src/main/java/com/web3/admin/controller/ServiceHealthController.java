package com.web3.admin.controller;

import com.web3.admin.service.AdminService;
import com.web3.admin.service.ServiceHealthService;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ServiceHealthController —— 后端服务健康/启停管理。
 *
 * <p>2026-09-17 修订：</p>
 * <ol>
 *   <li>所有写操作（start/stop）已加 {@link AuthUtils#requireAdmin} 鉴权；</li>
 *   <li>新增审计日志：每次 start/stop 调用 {@link AdminService#logOperation} 记录
 *       操作人 ID/名称、动作、目标服务名、IP、UA，写入 admin_operation_log 表；</li>
 *   <li>停止服务这种"高危能力"特别增加 result.detail 字段，便于事后追溯。</li>
 * </ol>
 */
@RestController
@RequestMapping("/admin/services")
public class ServiceHealthController {
    private static final Logger log = LoggerFactory.getLogger(ServiceHealthController.class);
    private final ServiceHealthService healthService;
    private final AdminService adminService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public ServiceHealthController(ServiceHealthService healthService,
                                  AdminService adminService,
                                  JwtUtil jwtUtil,
                                  JwtProperties jwtProperties) {
        this.healthService = healthService;
        this.adminService = adminService;
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

    /** POST /admin/services/{name}/start — 启动服务（仅管理员，写审计日志） */
    @PostMapping("/{name}/start")
    public ApiResponse<Map<String, Object>> start(
            @PathVariable String name,
            @RequestHeader(value = "Authorization", required = false) String auth,
            HttpServletRequest request) {
        String operatorName = AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        Long operatorId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        Map<String, Object> result = healthService.startService(name);
        audit(operatorId, operatorName, "service", "start", name, result, request);
        return ApiResponse.ok(result);
    }

    /** POST /admin/services/{name}/stop — 停止服务（仅管理员，写审计日志） */
    @PostMapping("/{name}/stop")
    public ApiResponse<Map<String, Object>> stop(
            @PathVariable String name,
            @RequestHeader(value = "Authorization", required = false) String auth,
            HttpServletRequest request) {
        String operatorName = AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        Long operatorId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        Map<String, Object> result = healthService.stopService(name);
        audit(operatorId, operatorName, "service", "stop", name, result, request);
        return ApiResponse.ok(result);
    }

    /** 写审计日志：requireAdmin 返回用户名、requireUserId 返回用户 ID，一并落库便于事后追溯。 */
    private void audit(Long operatorId, String operatorName, String module, String action,
                       String serviceName, Map<String, Object> result, HttpServletRequest req) {
        try {
            String ip = req.getRemoteAddr();
            String ua = req.getHeader("User-Agent");
            String detail = "result=" + result.get("message");
            adminService.logOperation(operatorId, operatorName, module, action,
                    serviceName, serviceName, detail, ip, ua);
        } catch (Exception e) {
            // 审计写失败不影响业务操作
            log.debug("[health] audit log write failed: {}", e.getMessage());
        }
    }
}

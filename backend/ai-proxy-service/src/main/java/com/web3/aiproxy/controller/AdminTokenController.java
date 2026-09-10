package com.web3.aiproxy.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.config.AiProxyProperties;
import com.web3.aiproxy.entity.ProxyToken;
import com.web3.aiproxy.mapper.ProxyTokenMapper;
import com.web3.aiproxy.service.TokenAuthService;
import com.web3.aiproxy.util.ProxyTokenUtils;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 类名：AdminTokenController
 * 所属模块：ai-proxy-service（中转站）
 * 职责：令牌管理接口（需管理员 JWT）—— 生成/启停/删除/充值/列表；
 *       创建时返回完整 sk- 令牌（仅此一次），库中只存 SHA-256 哈希 + 前缀；
 *       额度为整数单位（500000 = 1 美元）；任何变更即时失效 Redis 缓存。
 */
@RestController
@RequestMapping("/admin/ai/tokens")
@RequiredArgsConstructor
public class AdminTokenController {

    private final ProxyTokenMapper tokenMapper;
    private final AiProxyProperties properties;
    private final TokenAuthService tokenAuthService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    /** 分页列表：脱敏展示 */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        var wrapper = Wrappers.<ProxyToken>lambdaQuery()
                .eq(userId != null, ProxyToken::getUserId, userId)
                .orderByDesc(ProxyToken::getId);
        var result = tokenMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("records", result.getRecords().stream().map(this::view).toList());
        return ApiResponse.ok(data);
    }

    /**
     * 创建令牌：{name, userId?, groupName?, quota?, unlimitedQuota?, rateLimit?,
     * expiredAt?, modelLimit?:[..], allowIps?:[..]}；fullToken 仅此一次返回。
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        String fullToken = ProxyTokenUtils.generate();
        ProxyToken token = new ProxyToken();
        token.setUserId(body.get("userId") == null ? 0L
                : Long.parseLong(String.valueOf(body.get("userId"))));
        token.setGroupName(str(body.get("groupName"), "default"));
        token.setTokenHash(ProxyTokenUtils.hash(properties.getTokenSecret(), fullToken));
        token.setTokenPrefix(ProxyTokenUtils.prefixOf(fullToken));
        token.setName(str(body.get("name"), "默认令牌"));
        token.setStatus(1);
        token.setUnlimitedQuota(parseInt(body.get("unlimitedQuota"), 0));
        boolean unlimited = token.getUnlimitedQuota() != null && token.getUnlimitedQuota() == 1;
        token.setQuota(unlimited ? null : parseLong(body.get("quota"), null));
        if (token.getQuota() != null && token.getQuota() < 0) return ApiResponse.fail(400, "quota must be >= 0");
        token.setModelLimit(toJsonArrayString(body.get("modelLimit")));
        token.setAllowIps(toJsonArrayString(body.get("allowIps")));
        token.setRateLimit(parseIntegerBox(body.get("rateLimit")));
        if (StringUtils.hasText((String) body.get("expiredAt"))) {
            token.setExpiredAt(LocalDateTime.parse(((String) body.get("expiredAt")).replace(" ", "T")));
        }
        token.setRequestCount(0L);
        token.setUsedQuota(0L);
        token.setCreatedBy(parseLong(body.get("createdBy"), 0L));
        token.setCreatedAt(LocalDateTime.now());
        tokenMapper.insert(token);

        Map<String, Object> data = new HashMap<>();
        data.put("id", token.getId());
        data.put("fullToken", fullToken); // 仅创建时展示一次
        data.put("prefix", "sk-" + token.getTokenPrefix() + "***");
        return ApiResponse.ok(data);
    }

    /** 更新：启停/改名/分组/限速/过期/白名单 */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        ProxyToken exist = tokenMapper.selectById(id);
        if (exist == null) return ApiResponse.fail(404, "token not found");
        if (body.containsKey("status")) exist.setStatus(parseInt(body.get("status"), exist.getStatus()));
        if (body.containsKey("name")) exist.setName(String.valueOf(body.get("name")));
        if (body.containsKey("groupName") && StringUtils.hasText((String) body.get("groupName"))) {
            exist.setGroupName((String) body.get("groupName"));
        }
        if (body.containsKey("rateLimit")) exist.setRateLimit(parseIntegerBox(body.get("rateLimit")));
        if (body.containsKey("unlimitedQuota")) {
            int unlim = parseInt(body.get("unlimitedQuota"), exist.getUnlimitedQuota() == null ? 0 : exist.getUnlimitedQuota());
            exist.setUnlimitedQuota(unlim);
            if (unlim == 1) exist.setQuota(null);
        }
        if (body.containsKey("quota")) exist.setQuota(parseLong(body.get("quota"), exist.getQuota()));
        if (body.containsKey("expiredAt")) {
            String v = (String) body.get("expiredAt");
            exist.setExpiredAt(StringUtils.hasText(v) ? LocalDateTime.parse(v.replace(" ", "T")) : null);
        }
        if (body.containsKey("modelLimit")) exist.setModelLimit(toJsonArrayString(body.get("modelLimit")));
        if (body.containsKey("allowIps")) exist.setAllowIps(toJsonArrayString(body.get("allowIps")));
        tokenMapper.updateById(exist);
        tokenAuthService.evictByHash(exist.getTokenHash());
        return ApiResponse.ok();
    }

    /** 充值：quota 增加 amount（整数单位） */
    @PostMapping("/{id}/topup")
    public ApiResponse<Void> topup(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        long amount = parseLong(body.get("amount"), 0L);
        if (amount <= 0) return ApiResponse.fail(400, "amount must be positive");
        ProxyToken exist = tokenMapper.selectById(id);
        if (exist == null) return ApiResponse.fail(404, "token not found");
        tokenMapper.addQuota(id, amount);
        tokenAuthService.evictByHash(exist.getTokenHash());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        ProxyToken exist = tokenMapper.selectById(id);
        if (exist != null) {
            tokenMapper.deleteById(id);
            tokenAuthService.evictByHash(exist.getTokenHash());
        }
        return ApiResponse.ok();
    }

    // ---------------- 工具 ----------------

    private Map<String, Object> view(ProxyToken t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("userId", t.getUserId());
        m.put("name", t.getName());
        m.put("groupName", t.getGroupName());
        m.put("keyMasked", "sk-" + t.getTokenPrefix() + "****");
        m.put("status", t.getStatus());
        m.put("unlimitedQuota", t.getUnlimitedQuota());
        m.put("rateLimit", t.getRateLimit());
        m.put("requestCount", t.getRequestCount());
        m.put("lastUsedAt", t.getLastUsedAt());
        m.put("expiredAt", t.getExpiredAt());
        m.put("quota", t.getQuota());
        m.put("usedQuota", t.getUsedQuota());
        boolean unlimited = t.getUnlimitedQuota() != null && t.getUnlimitedQuota() == 1;
        m.put("remaining", unlimited ? null :
                (t.getQuota() == null ? null : t.getQuota() - (t.getUsedQuota() == null ? 0 : t.getUsedQuota())));
        m.put("modelLimit", t.getModelLimit());
        m.put("allowIps", t.getAllowIps());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    private String str(Object v, String def) {
        return v == null || String.valueOf(v).isBlank() ? def : String.valueOf(v);
    }

    private int parseInt(Object v, int def) {
        try {
            return v == null ? def : Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private Long parseLong(Object v, Long def) {
        try {
            return v == null ? def : Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private Integer parseIntegerBox(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** List 入参 → JSON 数组字符串；空 → null（=不限） */
    private String toJsonArrayString(Object v) {
        if (!(v instanceof java.util.List<?> list) || list.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(String.valueOf(list.get(i)).replace("\"", "")).append('"');
        }
        return sb.append(']').toString();
    }
}

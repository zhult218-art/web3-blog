package com.web3.aiproxy.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.config.AiProxyProperties;
import com.web3.aiproxy.entity.ProxyRedeemCode;
import com.web3.aiproxy.entity.ProxyToken;
import com.web3.aiproxy.mapper.ProxyRedeemCodeMapper;
import com.web3.aiproxy.mapper.ProxyTokenMapper;
import com.web3.aiproxy.service.TokenAuthService;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类名：AdminRedeemController
 * 所属模块：ai-proxy-service（中转站）
 * 职责：兑换码管理接口（需管理员 JWT，P2-T11）—— 批量生成/批次查询/作废/核销到令牌；
 *       码只存 SHA-256 哈希 + 8 位前缀（防拖库撞码）；核销走条件原子 UPDATE 防并发抢码。
 */
@RestController
@RequestMapping("/admin/ai/redeems")
@RequiredArgsConstructor
public class AdminRedeemController {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final ProxyRedeemCodeMapper redeemMapper;
    private final ProxyTokenMapper tokenMapper;
    private final AiProxyProperties properties;
    private final TokenAuthService tokenAuthService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    /** 批量生成：{count(<=500), quota(面值), expiredAt?} → 返回明文码列表（仅此一次） */
    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody Map<String, Object> body,
                                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        int count = parse(body.get("count"), 0);
        long quota = parse(body.get("quota"), 0);
        if (count <= 0 || count > 500) return ApiResponse.fail(400, "count must be in [1,500]");
        if (quota <= 0) return ApiResponse.fail(400, "quota must be positive");
        String batchNo = "B" + System.currentTimeMillis();
        LocalDateTime expiredAt = StringUtils.hasText((String) body.get("expiredAt"))
                ? LocalDateTime.parse(((String) body.get("expiredAt")).replace(" ", "T")) : null;

        List<String> plainCodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String code = randomCode();
            ProxyRedeemCode entity = new ProxyRedeemCode();
            entity.setBatchNo(batchNo);
            entity.setCodePrefix(code.substring(0, 8));
            entity.setCodeHash(hash(code));
            entity.setQuota(quota);
            entity.setStatus(0);
            entity.setExpiredAt(expiredAt);
            entity.setCreatedAt(LocalDateTime.now());
            redeemMapper.insert(entity);
            plainCodes.add(code);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("batchNo", batchNo);
        data.put("codes", plainCodes); // 仅此一次返回
        return ApiResponse.ok(data);
    }

    /** 批次明细列表（脱敏：前缀 + 状态） */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String batchNo,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long size,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        var wrapper = Wrappers.<ProxyRedeemCode>lambdaQuery()
                .eq(StringUtils.hasText(batchNo), ProxyRedeemCode::getBatchNo, batchNo)
                .orderByDesc(ProxyRedeemCode::getId);
        var result = redeemMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, Math.min(size, 200)), wrapper);
        List<Map<String, Object>> records = result.getRecords().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("batchNo", c.getBatchNo());
            m.put("codeMasked", c.getCodePrefix() + "****");
            m.put("quota", c.getQuota());
            m.put("status", c.getStatus());
            m.put("usedByTokenId", c.getUsedByTokenId());
            m.put("usedAt", c.getUsedAt());
            m.put("expiredAt", c.getExpiredAt());
            m.put("createdAt", c.getCreatedAt());
            return m;
        }).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("records", records);
        return ApiResponse.ok(data);
    }

    /** 作废整批未使用码 */
    @PostMapping("/{batchNo}/void")
    public ApiResponse<Void> voidBatch(@PathVariable String batchNo,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        redeemMapper.voidBatch(batchNo);
        return ApiResponse.ok();
    }

    /** 核销到指定令牌：{code} —— 原子核销 + 充值，失败给出明确原因 */
    @PostMapping("/redeem/{tokenId}")
    public ApiResponse<Void> redeem(@PathVariable Long tokenId, @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        String code = body.get("code") == null ? null : String.valueOf(body.get("code")).trim();
        if (!StringUtils.hasText(code)) return ApiResponse.fail(400, "code is required");
        ProxyToken token = tokenMapper.selectById(tokenId);
        if (token == null) return ApiResponse.fail(404, "token not found");
        int rows = redeemMapper.redeem(hash(code), tokenId);
        if (rows == 0) {
            ProxyRedeemCode exist = redeemMapper.selectOne(Wrappers.<ProxyRedeemCode>lambdaQuery()
                    .eq(ProxyRedeemCode::getCodeHash, hash(code)));
            if (exist == null) return ApiResponse.fail(404, "code not found");
            if (exist.getStatus() == 1) return ApiResponse.fail(409, "code already used");
            if (exist.getStatus() == 2) return ApiResponse.fail(409, "code voided");
            return ApiResponse.fail(409, "code expired");
        }
        ProxyRedeemCode used = redeemMapper.selectOne(Wrappers.<ProxyRedeemCode>lambdaQuery()
                .eq(ProxyRedeemCode::getCodeHash, hash(code)));
        if (used != null) tokenMapper.addQuota(tokenId, used.getQuota());
        tokenAuthService.evictByHash(token.getTokenHash());
        return ApiResponse.ok();
    }

    // ---------------- 工具 ----------------

    private static String randomCode() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        return sb.toString();
    }

    /** SHA-256(salt + code)，与令牌同 salt 派生体系 */
    private static String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(("rdm:" + code).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private int parse(Object v, int def) {
        try {
            return v == null ? def : Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private long parse(Object v, long def) {
        try {
            return v == null ? def : Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

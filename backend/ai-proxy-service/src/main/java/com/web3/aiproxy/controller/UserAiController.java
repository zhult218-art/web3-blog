package com.web3.aiproxy.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.config.AiProxyProperties;
import com.web3.aiproxy.entity.ProxyModel;
import com.web3.aiproxy.entity.ProxyRedeemCode;
import com.web3.aiproxy.entity.ProxyRequestLog;
import com.web3.aiproxy.entity.ProxyToken;
import com.web3.aiproxy.mapper.ProxyModelMapper;
import com.web3.aiproxy.mapper.ProxyRedeemCodeMapper;
import com.web3.aiproxy.mapper.ProxyRequestLogMapper;
import com.web3.aiproxy.mapper.ProxyTokenMapper;
import com.web3.aiproxy.service.TokenAuthService;
import com.web3.aiproxy.util.ProxyTokenUtils;
import com.web3.common.core.ApiResponse;
import com.web3.common.core.BusinessException;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserAiController
 * (zh) 用户侧中转站门户接口（需登录 JWT，AuthUtils.requireUserId）——
 *      公开模型与倍率列表、我的令牌（创建仅限有限额度/启停/删除）、
 *      兑换码自助核销到本人令牌、本人用量汇总。
 *      与 /admin/ai/** 的区别：所有数据严格限定在当前登录用户名下，
 *      不允许设置无限额度，初始额度设上限防滥用；fullToken 仅创建时返回一次。
 */
@RestController
@RequestMapping("/user/ai")
@RequiredArgsConstructor
public class UserAiController {

    /** self quota cap: 500000 units = 1 USD */
    private static final long SELF_QUOTA_LIMIT = 5_000_000L;

    private final ProxyTokenMapper tokenMapper;
    private final ProxyModelMapper modelMapper;
    private final ProxyRedeemCodeMapper redeemMapper;
    private final ProxyRequestLogMapper logMapper;
    private final AiProxyProperties properties;
    private final TokenAuthService tokenAuthService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    /** public enabled models + rates for portal pricing table */
    @GetMapping("/models")
    public ApiResponse<List<Map<String, Object>>> models() {
        List<ProxyModel> list = modelMapper.selectList(Wrappers.<ProxyModel>lambdaQuery()
                .eq(ProxyModel::getStatus, 1)
                .orderByAsc(ProxyModel::getSortOrder)
                .orderByAsc(ProxyModel::getId));
        return ApiResponse.ok(list.stream().map(m -> {
            Map<String, Object> v = new HashMap<>();
            v.put("modelName", m.getModelName());
            v.put("modelRate", m.getModelRate());
            v.put("completionRate", m.getCompletionRate());
            v.put("maxTokens", m.getMaxTokens());
            v.put("tags", m.getTags());
            v.put("remark", m.getRemark());
            return v;
        }).toList());
    }

    /** my tokens, paged, masked */
    @GetMapping("/tokens")
    public ApiResponse<Map<String, Object>> tokens(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        var result = tokenMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, Math.min(size, 100)),
                Wrappers.<ProxyToken>lambdaQuery()
                        .eq(ProxyToken::getUserId, userId)
                        .orderByDesc(ProxyToken::getId));
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("records", result.getRecords().stream().map(UserAiController::view).toList());
        return ApiResponse.ok(data);
    }

    /**
     * create my token: {name, quota?, expiredAt?, modelLimit?:[..]}
     * limited quota only (cap SELF_QUOTA_LIMIT); fullToken returned once
     */
    @PostMapping("/tokens")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        long count = tokenMapper.selectCount(Wrappers.<ProxyToken>lambdaQuery().eq(ProxyToken::getUserId, userId));
        if (count >= 10) {
            throw new BusinessException("\u6bcf\u4e2a\u7528\u6237\u6700\u591a\u521b\u5efa 10 \u4e2a\u4ee4\u724c");
        }
        long quota = parseLong(body.get("quota"), 500_000L);
        if (quota <= 0 || quota > SELF_QUOTA_LIMIT) {
            throw new BusinessException("quota \u5fc5\u987b\u5728 (0, " + SELF_QUOTA_LIMIT + "] \u8303\u56f4\u5185");
        }

        String fullToken = ProxyTokenUtils.generate();
        ProxyToken token = new ProxyToken();
        token.setUserId(userId);
        token.setGroupName("default");
        token.setTokenHash(ProxyTokenUtils.hash(properties.getTokenSecret(), fullToken));
        token.setTokenPrefix(ProxyTokenUtils.prefixOf(fullToken));
        String name = body.get("name") == null ? "" : String.valueOf(body.get("name")).trim();
        token.setName(StringUtils.hasText(name) ? name : "\u6211\u7684\u4ee4\u724c");
        token.setStatus(1);
        token.setUnlimitedQuota(0);
        token.setQuota(quota);
        token.setModelLimit(toJsonArrayString(body.get("modelLimit")));
        token.setAllowIps(null);
        if (StringUtils.hasText((String) body.get("expiredAt"))) {
            try {
                token.setExpiredAt(LocalDateTime.parse(((String) body.get("expiredAt")).replace(" ", "T")));
            } catch (Exception e) {
                throw new BusinessException("expiredAt \u683c\u5f0f\u5e94\u4e3a yyyy-MM-ddTHH:mm:ss");
            }
        }
        token.setRequestCount(0L);
        token.setUsedQuota(0L);
        token.setCreatedBy(userId);
        token.setCreatedAt(LocalDateTime.now());
        tokenMapper.insert(token);

        Map<String, Object> data = new HashMap<>();
        data.put("id", token.getId());
        data.put("fullToken", fullToken);
        data.put("prefix", "sk-" + token.getTokenPrefix() + "***");
        return ApiResponse.ok(data);
    }

    /** update my token: rename / enable-disable only (ownership enforced) */
    @PutMapping("/tokens/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        ProxyToken token = requireOwned(id, auth);
        if (body.containsKey("name")) {
            String name = String.valueOf(body.get("name")).trim();
            if (!StringUtils.hasText(name)) throw new BusinessException("\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
            token.setName(name);
        }
        if (body.containsKey("status")) {
            int status = parseInt(body.get("status"), token.getStatus());
            if (status != 0 && status != 1) throw new BusinessException("status \u53ea\u80fd\u4e3a 0 \u6216 1");
            token.setStatus(status);
        }
        tokenMapper.updateById(token);
        tokenAuthService.evictByHash(token.getTokenHash());
        return ApiResponse.ok();
    }

    /** delete my token (ownership enforced) */
    @DeleteMapping("/tokens/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        ProxyToken token = requireOwned(id, auth);
        tokenMapper.deleteById(id);
        tokenAuthService.evictByHash(token.getTokenHash());
        return ApiResponse.ok();
    }

    /**
     * redeem a gift code into MY token: {code}
     * atomic claim (concurrency-safe) then topup by face value
     */
    @PostMapping("/tokens/{id}/redeem")
    public ApiResponse<Void> redeem(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        ProxyToken token = requireOwned(id, auth);
        String code = body.get("code") == null ? "" : String.valueOf(body.get("code")).trim();
        if (!StringUtils.hasText(code)) throw new BusinessException("\u8bf7\u8f93\u5165\u5151\u6362\u7801");
        String codeHash = hash(code);
        int rows = redeemMapper.redeem(codeHash, id);
        if (rows == 0) {
            ProxyRedeemCode exist = redeemMapper.selectOne(Wrappers.<ProxyRedeemCode>lambdaQuery()
                    .eq(ProxyRedeemCode::getCodeHash, codeHash));
            if (exist == null) throw new BusinessException(404, "\u5151\u6362\u7801\u4e0d\u5b58\u5728");
            if (exist.getStatus() == 1) throw new BusinessException(409, "\u5151\u6362\u7801\u5df2\u88ab\u4f7f\u7528");
            if (exist.getStatus() == 2) throw new BusinessException(409, "\u5151\u6362\u7801\u5df2\u4f5c\u5e9f");
            throw new BusinessException(409, "\u5151\u6362\u7801\u5df2\u8fc7\u671f");
        }
        ProxyRedeemCode used = redeemMapper.selectOne(Wrappers.<ProxyRedeemCode>lambdaQuery()
                .eq(ProxyRedeemCode::getCodeHash, codeHash));
        if (used != null) tokenMapper.addQuota(id, used.getQuota());
        tokenAuthService.evictByHash(token.getTokenHash());
        return ApiResponse.ok();
    }

    /** my usage summary: last 30 days calls/tokens/quota + per-model breakdown */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        List<Long> tokenIds = tokenMapper.selectList(Wrappers.<ProxyToken>lambdaQuery()
                        .select(ProxyToken::getId).eq(ProxyToken::getUserId, userId))
                .stream().map(ProxyToken::getId).toList();
        Map<String, Object> data = new HashMap<>();
        if (tokenIds.isEmpty()) {
            data.put("requestCount", 0);
            data.put("promptTokens", 0);
            data.put("completionTokens", 0);
            data.put("quotaUsed", 0);
            data.put("byModel", List.of());
            return ApiResponse.ok(data);
        }
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<ProxyRequestLog> logs = logMapper.selectList(Wrappers.<ProxyRequestLog>lambdaQuery()
                .in(ProxyRequestLog::getTokenId, tokenIds)
                .ge(ProxyRequestLog::getCreatedAt, since));
        long requests = logs.size();
        long prompt = logs.stream().mapToLong(l -> l.getPromptTokens() == null ? 0 : l.getPromptTokens()).sum();
        long completion = logs.stream().mapToLong(l -> l.getCompletionTokens() == null ? 0 : l.getCompletionTokens()).sum();
        long quota = logs.stream().mapToLong(l -> l.getQuotaCost() == null ? 0 : l.getQuotaCost()).sum();
        Map<String, long[]> byModel = new HashMap<>();
        for (ProxyRequestLog l : logs) {
            long[] agg = byModel.computeIfAbsent(l.getModelName() == null ? "unknown" : l.getModelName(),
                    k -> new long[2]);
            agg[0]++;
            agg[1] += l.getQuotaCost() == null ? 0 : l.getQuotaCost();
        }
        data.put("requestCount", requests);
        data.put("promptTokens", prompt);
        data.put("completionTokens", completion);
        data.put("quotaUsed", quota);
        data.put("byModel", byModel.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("model", e.getKey());
            m.put("requests", e.getValue()[0]);
            m.put("quotaCost", e.getValue()[1]);
            return m;
        }).sorted((a, b) -> Long.compare((long) b.get("requests"), (long) a.get("requests"))).toList());
        return ApiResponse.ok(data);
    }

    // ---------------- helpers ----------------

    /** verify the token exists and belongs to the current user */
    private ProxyToken requireOwned(Long tokenId, String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        ProxyToken token = tokenMapper.selectById(tokenId);
        if (token == null) throw new BusinessException(404, "\u4ee4\u724c\u4e0d\u5b58\u5728");
        if (!userId.equals(token.getUserId())) throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u8be5\u4ee4\u724c");
        return token;
    }

    /** masked view */
    private static Map<String, Object> view(ProxyToken t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("keyMasked", "sk-" + t.getTokenPrefix() + "****");
        m.put("status", t.getStatus());
        m.put("unlimitedQuota", t.getUnlimitedQuota());
        m.put("requestCount", t.getRequestCount());
        m.put("lastUsedAt", t.getLastUsedAt());
        m.put("expiredAt", t.getExpiredAt());
        m.put("quota", t.getQuota());
        m.put("usedQuota", t.getUsedQuota());
        boolean unlimited = t.getUnlimitedQuota() != null && t.getUnlimitedQuota() == 1;
        m.put("remaining", unlimited ? null :
                (t.getQuota() == null ? null : t.getQuota() - (t.getUsedQuota() == null ? 0 : t.getUsedQuota())));
        m.put("modelLimit", t.getModelLimit());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    /** List param -> JSON array string; empty -> null (= no limit) */
    private String toJsonArrayString(Object v) {
        if (!(v instanceof List<?> list) || list.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(String.valueOf(list.get(i)).replace("\"", "")).append('"');
        }
        return sb.append(']').toString();
    }

    /** SHA-256(salt + code), same derivation as admin redeem codes */
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

    private int parseInt(Object v, int def) {
        try {
            return v == null ? def : Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private long parseLong(Object v, long def) {
        try {
            return v == null ? def : Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

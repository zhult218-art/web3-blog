package com.web3.aiproxy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.config.AiProxyProperties;
import com.web3.aiproxy.entity.ProxyToken;
import com.web3.aiproxy.mapper.ProxyTokenMapper;
import com.web3.aiproxy.util.ProxyTokenUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 类名：TokenAuthService
 * 所属模块：ai-proxy-service（中转站）
 * 职责：令牌鉴权 —— SHA-256 哈希查库，Redis 缓存 60s 前置（管理端变更即失效）；
 *       校验启停/过期/IP 白名单/模型白名单/额度预检。任何不通过抛 OpenAI 风格 ProxyException。
 */
@Service
@RequiredArgsConstructor
public class TokenAuthService {

    private final ProxyTokenMapper tokenMapper;
    private final AiProxyProperties properties;
    private final ObjectMapper objectMapper;
    private final RedisCacheService redisCacheService;

    /** 校验 Bearer 令牌并返回令牌实体；不存在/停用/过期分别给出明确错误 */
    public ProxyToken authenticate(String fullToken) {
        String hash = ProxyTokenUtils.hash(properties.getTokenSecret(), fullToken);
        // Redis 缓存优先（fail-open：异常时直查 DB）
        ProxyToken token = redisCacheService.getTokenCache(hash, ProxyToken.class);
        if (token == null) {
            token = tokenMapper.selectOne(
                    Wrappers.<ProxyToken>lambdaQuery().eq(ProxyToken::getTokenHash, hash));
            if (token != null && token.getStatus() != null && token.getStatus() == 1
                    && (token.getExpiredAt() == null || token.getExpiredAt().isAfter(LocalDateTime.now()))) {
                redisCacheService.putTokenCache(hash, token);
            }
        }
        if (token == null) throw ProxyException.unauthorized("Invalid token");
        if (token.getStatus() == null || token.getStatus() != 1) throw ProxyException.unauthorized("Token disabled");
        if (token.getExpiredAt() != null && token.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw ProxyException.unauthorized("Token expired");
        }
        return token;
    }

    /** 管理端变更后按哈希失效缓存 */
    public void evictByHash(String tokenHash) {
        redisCacheService.evictToken(tokenHash);
    }

    /** IP 白名单校验（空 = 不限） */
    public void checkIpWhitelist(ProxyToken token, String clientIp) {
        List<String> whitelist = parseArray(token.getAllowIps());
        if (!whitelist.isEmpty() && !whitelist.contains(clientIp)) {
            throw ProxyException.forbidden("IP " + clientIp + " not allowed");
        }
    }

    /** 模型白名单校验（空 = 不限） */
    public void checkModelWhitelist(ProxyToken token, String model) {
        List<String> whitelist = parseArray(token.getModelLimit());
        if (!whitelist.isEmpty() && !whitelist.contains(model)) {
            throw new ProxyException(403, "model_not_allowed", "model not allowed: " + model);
        }
    }

    /** 额度预检：限额令牌剩余 <= 0 时拒绝转发 */
    public void checkQuotaEnough(ProxyToken token) {
        if (token.getUnlimitedQuota() != null && token.getUnlimitedQuota() == 1) return;
        Long quota = token.getQuota();
        if (quota == null) return;
        long used = token.getUsedQuota() == null ? 0 : token.getUsedQuota();
        if (quota - used <= 0) {
            throw ProxyException.forbidden("Insufficient quota");
        }
    }

    /** RPM 滑窗限流（Redis 分钟桶；超限抛 429 风格错误） */
    public void checkRateLimit(ProxyToken token) {
        if (!redisCacheService.allowRequest(token.getId(), token.getRateLimit())) {
            throw new ProxyException(429, "rate_limit_exceeded",
                    "Rate limit exceeded (" + token.getRateLimit() + " req/min)");
        }
    }

    /** 解析 JSON 数组字段为字符串列表 */
    public List<String> parseArray(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> result = new ArrayList<>();
            node.forEach(n -> result.add(n.asText()));
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}

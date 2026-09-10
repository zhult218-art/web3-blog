package com.web3.aiproxy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

/**
 * 类名：RedisCacheService
 * 所属模块：ai-proxy-service（中转站）
 * 职责：Redis 门面 —— ① 令牌/渠道缓存（读多写少，管理端变更即失效）；
 *       ② round-robin 分布式计数；③ 令牌 RPM 滑窗限流（INCR+EXPIRE，分钟桶）。
 *       所有方法 fail-open：Redis 异常时降级为直查数据库 / 放行限流，绝不阻断主链路。
 *
 * 键契约：
 *   proxy:token:{sha256hex}       TokenAuth 缓存 JSON   TTL 60s
 *   proxy:channel:list            启用渠道列表 JSON     TTL 10s
 *   proxy:model:list              启用模型列表 JSON     TTL 30s
 *   proxy:rr:{model}              轮询计数器 INCR
 *   proxy:rl:{tokenId}:{yyyyMMddHHmm} 分钟滑窗计数      TTL 65s
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private static final Duration TOKEN_TTL = Duration.ofSeconds(60);
    private static final Duration CHANNEL_TTL = Duration.ofSeconds(10);
    private static final Duration MODEL_TTL = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "proxy:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    // ---------------- 令牌缓存 ----------------

    public <T> T getTokenCache(String tokenHash, Class<T> type) {
        try {
            String json = redis.opsForValue().get(KEY_PREFIX + "token:" + tokenHash);
            return StringUtils.hasText(json) ? objectMapper.readValue(json, type) : null;
        } catch (Exception e) {
            log.debug("redis token cache read failed: {}", e.getMessage());
            return null;
        }
    }

    public void putTokenCache(String tokenHash, Object token) {
        try {
            redis.opsForValue().set(KEY_PREFIX + "token:" + tokenHash,
                    objectMapper.writeValueAsString(token), TOKEN_TTL);
        } catch (Exception e) {
            log.debug("redis token cache write failed: {}", e.getMessage());
        }
    }

    /** 管理端任何令牌变更后调用 */
    public void evictToken(String tokenHash) {
        try {
            redis.delete(KEY_PREFIX + "token:" + tokenHash);
        } catch (Exception e) {
            log.debug("redis token evict failed: {}", e.getMessage());
        }
    }

    // ---------------- 渠道/模型缓存 ----------------

    @SuppressWarnings("unchecked")
    public List<Object> getChannelListCache() {
        try {
            String json = redis.opsForValue().get(KEY_PREFIX + "channel:list");
            if (!StringUtils.hasText(json)) return null;
            return (List<Object>) objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, java.util.Map.class));
        } catch (Exception e) {
            log.debug("redis channel cache read failed: {}", e.getMessage());
            return null;
        }
    }

    public void putChannelListCache(Object channels) {
        try {
            redis.opsForValue().set(KEY_PREFIX + "channel:list",
                    objectMapper.writeValueAsString(channels), CHANNEL_TTL);
        } catch (Exception e) {
            log.debug("redis channel cache write failed: {}", e.getMessage());
        }
    }

    public void evictChannels() {
        try {
            redis.delete(KEY_PREFIX + "channel:list");
        } catch (Exception e) {
            log.debug("redis channel evict failed: {}", e.getMessage());
        }
    }

    public String getModelListCache() {
        try {
            return redis.opsForValue().get(KEY_PREFIX + "model:list");
        } catch (Exception e) {
            return null;
        }
    }

    public void putModelListCache(String json) {
        try {
            redis.opsForValue().set(KEY_PREFIX + "model:list", json, MODEL_TTL);
        } catch (Exception e) {
            log.debug("redis model cache write failed: {}", e.getMessage());
        }
    }

    public void evictModels() {
        try {
            redis.delete(KEY_PREFIX + "model:list");
        } catch (Exception e) {
            log.debug("redis model evict failed: {}", e.getMessage());
        }
    }

    // ---------------- 轮询计数 ----------------

    /** 分布式轮询计数器；Redis 不可用时返回 -1（调用方退化为本地随机） */
    public long rrNext(String model) {
        try {
            Long v = redis.opsForValue().increment(KEY_PREFIX + "rr:" + model);
            return v == null ? -1 : v;
        } catch (Exception e) {
            return -1;
        }
    }

    // ---------------- RPM 滑窗限流 ----------------

    /**
     * 分钟桶滑窗计数。@return true=放行 false=超限
     */
    public boolean allowRequest(Long tokenId, Integer rpmLimit) {
        if (rpmLimit == null || rpmLimit <= 0) return true;
        try {
            String bucket = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            String key = KEY_PREFIX + "rl:" + tokenId + ":" + bucket;
            Long cnt = redis.opsForValue().increment(key);
            if (cnt != null && cnt == 1L) redis.expire(key, Duration.ofSeconds(65));
            return cnt != null && cnt <= rpmLimit;
        } catch (Exception e) {
            log.debug("redis rate limit check failed (fail-open): {}", e.getMessage());
            return true;
        }
    }
}

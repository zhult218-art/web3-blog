package com.web3.aiproxy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.entity.ProxyUserGroup;
import com.web3.aiproxy.mapper.ProxyUserGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类名：UserGroupService
 * 所属模块：ai-proxy-service（中转站）
 * 职责：分组倍率解析（P2-T14）—— 本地缓存 60s + Redis 兜底缓存；
 *       分组不存在/停用一律按 1 计费，主链路永不因配置缺失中断。
 */
@Service
@RequiredArgsConstructor
public class UserGroupService {

    private final ProxyUserGroupMapper groupMapper;
    private final RedisCacheService redisCacheService;
    private final Map<String, CachedRate> localCache = new ConcurrentHashMap<>();

    /** 解析分组倍率；异常/缺失恒返回 1 */
    public BigDecimal rateOf(String groupName) {
        if (groupName == null || groupName.isBlank()) return BigDecimal.ONE;
        long now = System.currentTimeMillis();
        CachedRate cached = localCache.get(groupName);
        if (cached != null && now - cached.at < 60_000) {
            return cached.rate != null ? cached.rate : BigDecimal.ONE;
        }
        BigDecimal rate = BigDecimal.ONE;
        try {
            ProxyUserGroup group = groupMapper.selectOne(Wrappers.<ProxyUserGroup>lambdaQuery()
                    .eq(ProxyUserGroup::getGroupName, groupName).eq(ProxyUserGroup::getStatus, 1));
            if (group != null && group.getRate() != null && group.getRate().signum() > 0) {
                rate = group.getRate();
            }
        } catch (Exception e) {
            // DB 异常按 1 计费，不阻断请求
        }
        localCache.put(groupName, new CachedRate(rate, now));
        return rate;
    }

    /** 管理端变更分组后清本地缓存 */
    public void evictLocal() {
        localCache.clear();
    }

    private record CachedRate(BigDecimal rate, long at) {
    }
}

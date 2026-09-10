package com.web3.aiproxy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.config.AiProxyProperties;
import com.web3.aiproxy.entity.ProxyChannel;
import com.web3.aiproxy.mapper.ProxyChannelMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 类名：ChannelRouterService
 * 所属模块：ai-proxy-service（中转站）
 * 职责：渠道路由 —— 启用渠道 Redis 缓存 10s 前置；按优先级排序后，按策略
 *       （渠道级覆盖 > 全局：weight 加权随机 / random 纯随机 / round_robin Redis 分布式计数）
 *       产出有序候选列表，供引擎失败时依次降级重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelRouterService {

    private final ProxyChannelMapper channelMapper;
    private final AiProxyProperties properties;
    private final TokenAuthService tokenAuthService;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    /** 全部启用渠道（带缓存） */
    public List<ProxyChannel> listEnabled() {
        List<Object> cached = redisCacheService.getChannelListCache();
        if (cached != null) {
            try {
                List<ProxyChannel> result = new ArrayList<>(cached.size());
                for (Object o : cached) {
                    result.add(objectMapper.convertValue(o, ProxyChannel.class));
                }
                return result;
            } catch (Exception e) {
                log.debug("channel cache deserialize failed: {}", e.getMessage());
            }
        }
        // ORDER BY priority DESC, id ASC —— 高优先级渠道优先进入候选
        List<ProxyChannel> channels = channelMapper.selectList(
                Wrappers.<ProxyChannel>lambdaQuery()
                        .eq(ProxyChannel::getStatus, 1)
                        .orderByDesc(ProxyChannel::getPriority)
                        .orderByAsc(ProxyChannel::getId));
        redisCacheService.putChannelListCache(channels);
        return channels;
    }

    /**
     * 产出支持该模型的有序渠道候选列表。
     *
     * @return 有序候选；空列表 = 无可用渠道（调用方走兜底直连）
     */
    public List<ProxyChannel> candidates(String model) {
        List<ProxyChannel> matched = new ArrayList<>();
        for (ProxyChannel ch : listEnabled()) {
            if (tokenAuthService.parseArray(ch.getModels()).contains(model)) matched.add(ch);
        }
        if (matched.size() <= 1) return matched;

        String strategy = properties.getStrategy() == null ? "weight" : properties.getStrategy();
        switch (strategy) {
            case "random" -> Collections.shuffle(matched);
            case "round_robin", "round-robin" -> {
                long n = redisCacheService.rrNext(model);
                int offset = n >= 0 ? (int) (n % matched.size())
                        : ThreadLocalRandom.current().nextInt(matched.size());
                Collections.rotate(matched, -offset);
            }
            default -> weightedShuffle(matched);
        }
        return matched;
    }

    /** 加权随机打乱：权重越高越靠前概率越大（其余保持随机次序用于降级） */
    private void weightedShuffle(List<ProxyChannel> list) {
        List<ProxyChannel> pool = new ArrayList<>();
        for (ProxyChannel ch : list) {
            int w = Math.max(1, ch.getWeight() == null ? 1 : ch.getWeight());
            for (int i = 0; i < w; i++) pool.add(ch);
        }
        List<ProxyChannel> result = new ArrayList<>(list.size());
        while (!pool.isEmpty() && result.size() < list.size()) {
            ProxyChannel picked = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            result.add(picked);
            pool.removeIf(c -> c.getId().equals(picked.getId()));
        }
        list.clear();
        list.addAll(result);
    }

    /** 供管理端变更后失效缓存 */
    public void evictCache() {
        redisCacheService.evictChannels();
    }
}

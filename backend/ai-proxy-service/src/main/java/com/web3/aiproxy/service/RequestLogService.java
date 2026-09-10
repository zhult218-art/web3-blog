package com.web3.aiproxy.service;

import com.web3.aiproxy.entity.ProxyRequestLog;
import com.web3.aiproxy.mapper.ProxyChannelMapper;
import com.web3.aiproxy.mapper.ProxyDailyStatMapper;
import com.web3.aiproxy.mapper.ProxyRequestLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 类名：RequestLogService
 * 所属模块：ai-proxy-service（中转站）
 * 职责：请求后置处理（proxyLogExecutor 异步，不阻塞转发链路）——
 *       ① 日志落库；② 渠道成功/失败原子计数（熔断数据源）；
 *       ③ 日汇总 upsert 原子累加（长历史留存）。任一环节失败仅记日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestLogService {

    private final ProxyRequestLogMapper logMapper;
    private final ProxyDailyStatMapper dailyStatMapper;
    private final ProxyChannelMapper channelMapper;

    @Async("proxyLogExecutor")
    public void saveAsync(ProxyRequestLog entry) {
        try {
            logMapper.insert(entry);
        } catch (Exception e) {
            log.error("proxy request log insert failed: {}", e.getMessage());
        }
        afterCare(entry);
    }

    /** 后置处理：渠道统计 + 日汇总（顺序固定：channel -> daily_stat，避免锁交叉） */
    private void afterCare(ProxyRequestLog entry) {
        try {
            if (entry.getChannelId() != null && entry.getChannelId() > 0) {
                int sc = entry.getStatusCode() == null ? 500 : entry.getStatusCode();
                int latency = entry.getLatencyMs() == null ? 0 : entry.getLatencyMs();
                if (sc >= 200 && sc < 300 && entry.getFallback() == null) {
                    channelMapper.markSuccess(entry.getChannelId(), latency);
                } else if (sc >= 500 || sc == 0 || (entry.getFallback() != null && entry.getFallback() != 1 && sc >= 400)) {
                    // 仅统计上游侧失败；客户端 4xx 不计入渠道失败
                    if (sc >= 500 || sc == 0) channelMapper.markFailure(entry.getChannelId());
                }
            }
        } catch (Exception e) {
            log.warn("channel stat update failed: {}", e.getMessage());
        }
        try {
            boolean billed = entry.getTokenId() != null
                    && (entry.getFallback() == null || entry.getFallback() == 0);
            if (!billed) return;
            String userKey = "t:" + entry.getTokenId();
            long requests = 1;
            long errors = entry.getStatusCode() != null && entry.getStatusCode() >= 400 ? 1 : 0;
            long pt = entry.getPromptTokens() == null ? 0 : entry.getPromptTokens();
            long ct = entry.getCompletionTokens() == null ? 0 : entry.getCompletionTokens();
            long cost = entry.getQuotaCost() == null ? 0 : entry.getQuotaCost();
            dailyStatMapper.upsertIncrement(LocalDate.now(), userKey,
                    entry.getModelName() == null ? "unknown" : entry.getModelName(),
                    requests, errors, pt, ct, cost);
        } catch (Exception e) {
            log.warn("daily stat upsert failed: {}", e.getMessage());
        }
    }
}

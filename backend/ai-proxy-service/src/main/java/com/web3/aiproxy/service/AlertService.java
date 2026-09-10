package com.web3.aiproxy.service;

import com.web3.aiproxy.mapper.ProxyAlertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 类名：AlertService
 * 所属模块：ai-proxy-service（中转站）
 * 职责：运营告警异步落库 —— dedup_key 幂等去重（同对象同类事件每日一条），
 *       INSERT IGNORE 无锁竞争；全部在 proxyLogExecutor 异步线程执行，不阻塞主链路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    public static final String LOW_BALANCE = "LOW_BALANCE";
    public static final String TOKEN_EXHAUSTED = "TOKEN_EXHAUSTED";
    public static final String CHANNEL_FAIL = "CHANNEL_FAIL";
    public static final String RATE_LIMIT = "RATE_LIMIT";

    private final ProxyAlertMapper alertMapper;

    /** 通用告警入口（幂等） */
    @Async("proxyLogExecutor")
    public void raise(String type, int level, String refId, String message, String dedupKey) {
        try {
            alertMapper.insertIgnore(type, level, refId, message, dedupKey);
        } catch (Exception e) {
            log.warn("alert insert failed: {}", e.getMessage());
        }
    }

    /** 低余额检测：剩余 <20% warn / 已耗尽 danger，每日每令牌至多各一条；usedQuota 传空则自查 */
    @Async("proxyLogExecutor")
    public void checkLowBalance(com.web3.aiproxy.mapper.ProxyTokenMapper tokenMapper,
                                Long tokenId, String tokenName, Long quota, Long usedQuota) {
        try {
            if (tokenId == null) return;
            if (quota == null || quota <= 0) {
                var t = tokenMapper.selectById(tokenId);
                if (t == null) return;
                quota = t.getQuota();
                usedQuota = t.getUsedQuota();
                tokenName = t.getName();
            }
            if (quota == null || quota <= 0) return;
            long used = usedQuota == null ? 0 : usedQuota;
            long remaining = quota - used;
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            if (remaining <= 0) {
                raise(TOKEN_EXHAUSTED, 3, String.valueOf(tokenId),
                        "令牌 #" + tokenId + "(" + tokenName + ") 额度已耗尽",
                        TOKEN_EXHAUSTED + ":" + tokenId + ":" + today);
            } else if (used * 5 >= quota) {
                raise(LOW_BALANCE, 2, String.valueOf(tokenId),
                        "令牌 #" + tokenId + "(" + tokenName + ") 剩余额度不足 20%（余 " + remaining + "）",
                        LOW_BALANCE + ":" + tokenId + ":" + today);
            }
        } catch (Exception e) {
            log.warn("low balance check failed: {}", e.getMessage());
        }
    }
}

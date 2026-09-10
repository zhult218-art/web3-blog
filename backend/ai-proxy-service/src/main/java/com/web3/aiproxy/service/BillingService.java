package com.web3.aiproxy.service;

import com.web3.aiproxy.entity.ProxyModel;
import com.web3.aiproxy.mapper.ProxyTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 类名：BillingService
 * 所属模块：ai-proxy-service（中转站）
 * 职责：计费引擎 —— 与 one-api 对齐：
 *       本次额度 = 分组倍率 × 模型倍率 × (提示 token + 补全 token × 补全倍率)，整数单位。
 *       扣减走 ProxyTokenMapper.deductQuota 单语句原子 UPDATE（扣费+计数+时间戳一次完成，
 *       行级锁一次持有即释放，杜绝两段式事务死锁）；余额被守卫拦截时异步落 TOKEN_EXHAUSTED 告警。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private static final BigDecimal ONE = BigDecimal.ONE;
    private final ProxyTokenMapper tokenMapper;
    private final AlertService alertService;
    /**
     * 计算本次消耗额度（整数单位，500000 = 1 美元）。
     *
     * @param modelRate       模型基础倍率
     * @param completionRate  补全倍率
     * @param groupRate       分组倍率
     */
    public long computeQuota(BigDecimal modelRate, BigDecimal completionRate,
                             BigDecimal groupRate, int promptTokens, int completionTokens) {
        BigDecimal mr = modelRate == null ? ONE : modelRate;
        BigDecimal cr = completionRate == null ? ONE : completionRate;
        BigDecimal gr = groupRate == null ? ONE : groupRate;
        BigDecimal units = BigDecimal.valueOf(promptTokens).add(
                BigDecimal.valueOf(completionTokens).multiply(cr));
        return mr.multiply(gr, MathContext.DECIMAL64)
                .multiply(units, MathContext.DECIMAL64)
                .setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /** 兼容入口：直接传模型实体 */
    public long computeQuota(ProxyModel model, BigDecimal groupRate, int promptTokens, int completionTokens) {
        BigDecimal rate = model != null && model.getModelRate() != null ? model.getModelRate() : ONE;
        BigDecimal cr = model != null && model.getCompletionRate() != null ? model.getCompletionRate() : ONE;
        return computeQuota(rate, cr, groupRate, promptTokens, completionTokens);
    }

    /**
     * 原子扣减 + 用量计数（单语句）。
     *
     * @return true 扣减成功；false 余额不足被守卫拦截（仅告警，不阻断响应）
     */
    public boolean deduct(Long tokenId, String tokenName, Long quota, long amount) {
        if (tokenId == null) return true;
        if (amount < 0) amount = 0;
        int rows = tokenMapper.deductQuota(tokenId, amount);
        if (rows == 0) {
            log.warn("quota deduct blocked for token {}: insufficient remaining", tokenId);
            alertService.raise(AlertService.TOKEN_EXHAUSTED, 3, String.valueOf(tokenId),
                    "令牌 #" + tokenId + "(" + tokenName + ") 计费时余额不足，已拒绝本次扣减",
                    AlertService.TOKEN_EXHAUSTED + ":bill:" + tokenId + ":"
                            + java.time.LocalDate.now());
            return false;
        }
        // 异步低余额巡检（幂等告警；usedQuota 传 null 由告警服务自查最新值）
        alertService.checkLowBalance(tokenMapper, tokenId, tokenName, quota, null);
        return true;
    }
}

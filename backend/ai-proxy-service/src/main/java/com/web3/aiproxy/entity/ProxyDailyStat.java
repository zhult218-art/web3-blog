package com.web3.aiproxy.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 类名：ProxyDailyStat
 * 所属模块：ai-proxy-service（中转站）
 * 职责：按日汇总实体 —— 复合主键 (stat_date, user_key, model_name)，
 *       写入走 INSERT..ON DUPLICATE KEY UPDATE 单语句原子累加（防死锁）；
 *       日志表归档清理后长历史统计仍完整。仅经 ProxyDailyStatMapper 自定义 SQL 读写。
 */
@Data
public class ProxyDailyStat implements Serializable {
    private LocalDate statDate;
    /** 维度键：t:{tokenPrefix} 或 u:{userId} */
    private String userKey;
    private String modelName;
    private Long requests;
    private Long errorCount;
    private Long promptTokens;
    private Long completionTokens;
    private Long quotaCost;
    private LocalDateTime updatedAt;
}

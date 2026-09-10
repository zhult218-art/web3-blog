package com.web3.aiproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：ProxyAlert
 * 所属模块：ai-proxy-service（中转站）
 * 职责：告警记录实体 —— 低余额 / 额度耗尽 / 渠道故障 / 触发限流等运营事件落库；
 *       dedup_key 唯一键 + INSERT IGNORE 实现幂等去重（天然防重复告警与锁竞争）。
 */
@Data
@TableName("proxy_alert")
public class ProxyAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** LOW_BALANCE/TOKEN_EXHAUSTED/CHANNEL_FAIL/RATE_LIMIT */
    private String alertType;
    /** 1=info 2=warn 3=danger */
    private Integer level;
    private String refId;
    private String message;
    private String dedupKey;
    private LocalDateTime createdAt;
}

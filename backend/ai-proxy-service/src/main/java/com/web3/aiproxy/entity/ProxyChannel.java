package com.web3.aiproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 类名：ProxyChannel
 * 所属模块：ai-proxy-service（中转站）
 * 职责：渠道实体 —— 上游 AI 平台接入配置。Key 仅存 AES-GCM 密文；
 *       含调度策略、优先级、熔断统计（fail/success/avg_latency）与 JSON 扩展配置，
 *       为多上游类型 / 自动熔断 / 余额探测预留字段。
 */
@Data
@TableName("proxy_channel")
public class ProxyChannel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 1=OpenAI兼容 2=Azure 3=Anthropic（预留） */
    private Integer type;
    private String baseUrl;
    /** AES-GCM 密文 Base64(iv||ct||tag)，明文永不落库/回传 */
    private String apiKeyCipher;
    private String models;
    /** 渠道级策略覆盖：weight/random/round_robin，空=用全局 ai-proxy.strategy */
    private String strategy;
    private Integer weight;
    private Integer priority;
    private Integer status;
    private Integer autoDisabled;
    private Integer failCount;
    private Long successCount;
    private Integer avgLatencyMs;
    private BigDecimal balance;
    private String balanceCurrency;
    private LocalDateTime balanceUpdatedAt;
    private LocalDateTime lastUsedAt;
    /** 扩展配置 JSON：{extraHeaders:{}, timeoutMs:0, maxRetry:0} */
    private String config;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 入参明文 Key 通道（仅创建/更新请求体使用，非持久化） */
    @TableField(exist = false)
    private transient String apiSecret;
    /** 管理端脱敏展示（非持久化） */
    @TableField(exist = false)
    public transient String maskedKey;
}

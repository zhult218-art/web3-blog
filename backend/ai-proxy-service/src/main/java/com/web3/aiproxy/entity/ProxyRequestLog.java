package com.web3.aiproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：ProxyRequestLog
 * 所属模块：ai-proxy-service（中转站）
 * 职责：请求日志实体 —— 追加写流水；token/channel 名称冗余存储免 JOIN，
 *       fallback 标记区分兜底直连流量（不计费），request_id 唯一支持追踪与幂等。
 */
@Data
@TableName("proxy_request_log")
public class ProxyRequestLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private Long tokenId;
    private String tokenName;
    private Long channelId;
    private String channelName;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
    /** 本次扣费额度（整数单位） */
    private Long quotaCost;
    private Integer latencyMs;
    private Integer isStream;
    private Integer statusCode;
    /** 1=兜底直连未计费 */
    private Integer fallback;
    private String clientIp;
    private String errorMsg;
    private LocalDateTime createdAt;
}

package com.web3.aiproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：ProxyToken
 * 所属模块：ai-proxy-service（中转站）
 * 职责：令牌实体 —— 只存 SHA-256 哈希与前缀；额度为整数单位（500000 = 1 美元）。
 *       含分组倍率、RPM 限速、模型/IP 白名单、用量统计冗余列（request_count/last_used_at）。
 */
@Data
@TableName("proxy_token")
public class ProxyToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String groupName;
    private String tokenPrefix;
    private String tokenHash;
    private String name;
    /** 剩余额度（整数单位，500000=$1）；unlimited_quota=1 时忽略 */
    private Long quota;
    private Long usedQuota;
    private Integer unlimitedQuota;
    /** JSON 数组字符串：["deepseek-chat",...]，空=不限 */
    private String modelLimit;
    /** JSON 数组字符串，空=不限 */
    private String allowIps;
    /** RPM 限速，null=不限（Redis 滑窗实现） */
    private Integer rateLimit;
    private LocalDateTime expiredAt;
    private Integer status;
    private Long requestCount;
    private LocalDateTime lastUsedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

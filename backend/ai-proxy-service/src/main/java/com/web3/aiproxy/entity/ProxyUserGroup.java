package com.web3.aiproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 类名：ProxyUserGroup
 * 所属模块：ai-proxy-service（中转站）
 * 职责：用户分组实体 —— 分组计费倍率（P2-T14）；令牌通过 group_name 关联，
 *       停用分组按 1 计费，保证主链路永不因配置缺失而中断。
 */
@Data
@TableName("proxy_user_group")
public class ProxyUserGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupName;
    private BigDecimal rate;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.web3.aiproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：ProxyRedeemCode
 * 所属模块：ai-proxy-service（中转站）
 * 职责：额度兑换码实体 —— 只存 SHA-256 哈希 + 8 位前缀（防拖库撞码）；
 *       批次管理、核销留痕（used_by_token_id/used_at）、过期控制。
 */
@Data
@TableName("proxy_redeem_code")
public class ProxyRedeemCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private String codePrefix;
    private String codeHash;
    /** 面值额度（整数单位） */
    private Long quota;
    /** 0=未使用 1=已使用 2=作废 */
    private Integer status;
    private Long usedByTokenId;
    private LocalDateTime usedAt;
    private LocalDateTime expiredAt;
    private Long createdBy;
    private LocalDateTime createdAt;

    /** 入参/展示明文通道（非持久化） */
    @TableField(exist = false)
    private transient String plainCode;
}

package com.web3.aiproxy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 类名：ProxyModel
 * 所属模块：ai-proxy-service（中转站）
 * 职责：模型倍率实体 —— 单倍率模式（model_rate × completion_rate）与
 *       输入输出分离模式（rate_type=1，input_rate/output_rate 预留）双轨支持；
 *       另含单次上限 / 全局 RPM / 标签等运营扩展位。
 */
@Data
@TableName("proxy_model")
public class ProxyModel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelName;
    /** 0=单倍率 1=输入输出分离（预留） */
    private Integer rateType;
    private BigDecimal modelRate;
    private BigDecimal completionRate;
    private BigDecimal inputRate;
    private BigDecimal outputRate;
    private Integer maxTokens;
    private Integer rpmLimit;
    private Integer status;
    private String tags;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

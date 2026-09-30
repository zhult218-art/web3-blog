package com.web3.quant.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略展示层 VO(quant-service 模块)。
 *
 * <p>策略接口返回给前端的模型,字段与 {@link com.web3.quant.entity.Strategy}
 * 一致但不包含逻辑删除标记 deleted。</p>
 */
@Data
public class StrategyVO {
    /** 策略 ID */
    private Long id;
    /** 策略名称 */
    private String name;
    /** 策略描述 */
    private String description;
    /** 策略状态 */
    private String status;
    /** 策略代码内容 */
    private String code;
    /** 策略收益率 */
    private BigDecimal returns;
    /** 风险等级 */
    private String riskLevel;
    /** 策略标签 */
    private String tags;
    /** 回测数据(JSON) */
    private String backtestData;
    /** 创建时间 */
    private LocalDateTime createdAt;
}

package com.web3.quant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 量化策略实体类,对应数据库表 {@code strategy}。
 *
 * <p>字段说明:id 主键由 MyBatis-Plus 自动分配(雪花 ID);createdAt 在
 * 插入时自动填充;deleted 标注 {@code @TableLogic} 逻辑删除,查询默认
 * 排除已删除记录。</p>
 */
@Data
@TableName(value="strategy")
public class Strategy {
    /**
     * 策略主键(雪花 ID,自动分配)
     */
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    /**
     * 策略归属用户 ID（NULL 表示公共策略）。
     * 2026-09-17 新增：用于 update/delete/run 接口做归属校验，
     * 防止任意登录用户改/删/跑他人策略。
     */
    private Long userId;
    /** 策略名称 */
    private String name;
    /** 策略描述 */
    private String description;
    /** 策略状态,如 DRAFT/RUNNING/COMPLETED 等 */
    private String status;
    /** 策略代码内容 */
    private String code;
    /** 策略收益率 */
    private BigDecimal returns;
    /** 风险等级 */
    private String riskLevel;
    /** 策略标签,多个以分隔符拼接 */
    private String tags;
    /** 回测数据(JSON 字符串) */
    private String backtestData;
    /** 创建时间(插入时自动填充) */
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 逻辑删除标记(0 正常,1 已删除) */
    @TableLogic
    private Integer deleted;
}

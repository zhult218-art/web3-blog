package com.web3.quant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 量化运行日志实体类,对应数据库表 {@code quant_log}。
 *
 * <p>记录策略运行的日志消息(如 "Strategy [xxx] run started")。
 * id 由 MyBatis-Plus 自动分配;createdAt 插入时自动填充。</p>
 */
@Data
@TableName(value="quant_log")
public class QuantLog {
    /**
     * 日志主键(雪花 ID,自动分配)
     */
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    /** 关联的策略 ID */
    private Long strategyId;
    /** 日志消息内容 */
    private String message;
    /** 日志级别,如 INFO/WARN/ERROR */
    private String level;
    /** 创建时间(插入时自动填充) */
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
}

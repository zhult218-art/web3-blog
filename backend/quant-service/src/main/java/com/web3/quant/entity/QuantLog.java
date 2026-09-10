/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.FieldFill
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableField
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableName
 */
package com.web3.quant.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 量化运行日志实体类,对应数据库表 {@code quant_log}。
 *
 * <p>记录策略运行的日志消息(如 "Strategy [xxx] run started")。
 * id 由 MyBatis-Plus 自动分配;createdAt 插入时自动填充。</p>
 */
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

    public Long getId() {
        return this.id;
    }

    public Long getStrategyId() {
        return this.strategyId;
    }

    public String getMessage() {
        return this.message;
    }

    public String getLevel() {
        return this.level;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStrategyId(Long strategyId) {
        this.strategyId = strategyId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof QuantLog)) {
            return false;
        }
        QuantLog other = (QuantLog)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$strategyId = this.getStrategyId();
        Long other$strategyId = other.getStrategyId();
        if (this$strategyId == null ? other$strategyId != null : !((Object)this$strategyId).equals(other$strategyId)) {
            return false;
        }
        String this$message = this.getMessage();
        String other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) {
            return false;
        }
        String this$level = this.getLevel();
        String other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof QuantLog;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $strategyId = this.getStrategyId();
        result = result * 59 + ($strategyId == null ? 43 : ((Object)$strategyId).hashCode());
        String $message = this.getMessage();
        result = result * 59 + ($message == null ? 43 : $message.hashCode());
        String $level = this.getLevel();
        result = result * 59 + ($level == null ? 43 : $level.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "QuantLog(id=" + this.getId() + ", strategyId=" + this.getStrategyId() + ", message=" + this.getMessage() + ", level=" + this.getLevel() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}

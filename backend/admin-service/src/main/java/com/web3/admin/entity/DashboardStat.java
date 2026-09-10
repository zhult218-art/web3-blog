/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableLogic
 *  com.baomidou.mybatisplus.annotation.TableName
 */
package com.web3.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 类名：DashboardStat
 * 所属模块：admin-service（管理后台服务）
 * 职责：控制台统计项实体类，对应数据库表 admin_dashboard_stat，存储各项统计指标（键值对形式）。
 * 关键注解：@TableName("admin_dashboard_stat") 指定表名；@TableId(type = AUTO) 自增主键；deleted=逻辑删除标记（@TableLogic）。
 * 字段说明：statKey=指标名（如 totalUsers，service:* 前缀为各服务状态），statValue=指标值（字符串），category=分类，statDate=统计日期。
 */
@TableName(value="admin_dashboard_stat")
public class DashboardStat {
    @TableId(type=IdType.AUTO)
    private Long id;
    private String statKey;
    private String statValue;
    private String category;
    private LocalDateTime statDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    public Long getId() {
        return this.id;
    }

    public String getStatKey() {
        return this.statKey;
    }

    public String getStatValue() {
        return this.statValue;
    }

    public String getCategory() {
        return this.category;
    }

    public LocalDateTime getStatDate() {
        return this.statDate;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStatKey(String statKey) {
        this.statKey = statKey;
    }

    public void setStatValue(String statValue) {
        this.statValue = statValue;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setStatDate(LocalDateTime statDate) {
        this.statDate = statDate;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DashboardStat)) {
            return false;
        }
        DashboardStat other = (DashboardStat)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$deleted = this.getDeleted();
        Integer other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !((Object)this$deleted).equals(other$deleted)) {
            return false;
        }
        String this$statKey = this.getStatKey();
        String other$statKey = other.getStatKey();
        if (this$statKey == null ? other$statKey != null : !this$statKey.equals(other$statKey)) {
            return false;
        }
        String this$statValue = this.getStatValue();
        String other$statValue = other.getStatValue();
        if (this$statValue == null ? other$statValue != null : !this$statValue.equals(other$statValue)) {
            return false;
        }
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
            return false;
        }
        LocalDateTime this$statDate = this.getStatDate();
        LocalDateTime other$statDate = other.getStatDate();
        if (this$statDate == null ? other$statDate != null : !((Object)this$statDate).equals(other$statDate)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DashboardStat;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $statKey = this.getStatKey();
        result = result * 59 + ($statKey == null ? 43 : $statKey.hashCode());
        String $statValue = this.getStatValue();
        result = result * 59 + ($statValue == null ? 43 : $statValue.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        LocalDateTime $statDate = this.getStatDate();
        result = result * 59 + ($statDate == null ? 43 : ((Object)$statDate).hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "DashboardStat(id=" + this.getId() + ", statKey=" + this.getStatKey() + ", statValue=" + this.getStatValue() + ", category=" + this.getCategory() + ", statDate=" + this.getStatDate() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}

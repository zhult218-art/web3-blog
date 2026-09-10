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
package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * LikeRecord —— 点赞记录实体类
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 对应数据库表：like_record（点赞记录表）。
 * <p>
 * 一条记录表示"某用户点赞了某目标"，targetId + targetType + userId 唯一；createdAt 插入时自动填充。
 */
@TableName(value="like_record")
public class LikeRecord {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private Long targetId;
    private String targetType;
    private Long userId;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return this.id;
    }

    public Long getTargetId() {
        return this.targetId;
    }

    public String getTargetType() {
        return this.targetType;
    }

    public Long getUserId() {
        return this.userId;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LikeRecord)) {
            return false;
        }
        LikeRecord other = (LikeRecord)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$targetId = this.getTargetId();
        Long other$targetId = other.getTargetId();
        if (this$targetId == null ? other$targetId != null : !((Object)this$targetId).equals(other$targetId)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        String this$targetType = this.getTargetType();
        String other$targetType = other.getTargetType();
        if (this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LikeRecord;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $targetId = this.getTargetId();
        result = result * 59 + ($targetId == null ? 43 : ((Object)$targetId).hashCode());
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "LikeRecord(id=" + this.getId() + ", targetId=" + this.getTargetId() + ", targetType=" + this.getTargetType() + ", userId=" + this.getUserId() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}

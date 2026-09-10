/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.FieldFill
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableField
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableLogic
 *  com.baomidou.mybatisplus.annotation.TableName
 */
package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Playlist —— 播放列表实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：playlist（播放列表表）。
 * <p>
 * userId 标识播放列表的归属用户（用于越权校验）；deleted 为逻辑删除标记（@TableLogic 自动过滤）；
 * createdAt 插入时自动填充。
 */
@TableName(value="playlist")
public class Playlist {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;
    private Long userId;
    private String cover;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getCover() {
        return this.cover;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Playlist)) {
            return false;
        }
        Playlist other = (Playlist)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        Integer this$deleted = this.getDeleted();
        Integer other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !((Object)this$deleted).equals(other$deleted)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$cover = this.getCover();
        String other$cover = other.getCover();
        if (this$cover == null ? other$cover != null : !this$cover.equals(other$cover)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Playlist;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $cover = this.getCover();
        result = result * 59 + ($cover == null ? 43 : $cover.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "Playlist(id=" + this.getId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", userId=" + this.getUserId() + ", cover=" + this.getCover() + ", createdAt=" + this.getCreatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}

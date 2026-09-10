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
package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Comment —— 评论实体类
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 对应数据库表：comment（评论表）。
 * <p>
 * targetId + targetType 标识被评论的目标对象（如帖子）；deleted 为逻辑删除标记（@TableLogic 自动过滤）；
 * createdAt 插入时自动填充。
 */
@TableName(value="comment")
public class Comment {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private Long targetId;
    private String targetType;
    private String content;
    private Long parentId;
    private String replyToName;
    private Long authorId;
    private String authorName;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;

    public Long getId() {
        return this.id;
    }

    public Long getTargetId() {
        return this.targetId;
    }

    public String getTargetType() {
        return this.targetType;
    }

    public String getContent() {
        return this.content;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public String getReplyToName() {
        return this.replyToName;
    }

    public Long getAuthorId() {
        return this.authorId;
    }

    public String getAuthorName() {
        return this.authorName;
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

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public void setReplyToName(String replyToName) {
        this.replyToName = replyToName;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
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
        if (!(o instanceof Comment)) {
            return false;
        }
        Comment other = (Comment)o;
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
        Long this$authorId = this.getAuthorId();
        Long other$authorId = other.getAuthorId();
        if (this$authorId == null ? other$authorId != null : !((Object)this$authorId).equals(other$authorId)) {
            return false;
        }
        Integer this$deleted = this.getDeleted();
        Integer other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !((Object)this$deleted).equals(other$deleted)) {
            return false;
        }
        String this$targetType = this.getTargetType();
        String other$targetType = other.getTargetType();
        if (this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType)) {
            return false;
        }
        String this$content = this.getContent();
        String other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Comment;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $targetId = this.getTargetId();
        result = result * 59 + ($targetId == null ? 43 : ((Object)$targetId).hashCode());
        Long $authorId = this.getAuthorId();
        result = result * 59 + ($authorId == null ? 43 : ((Object)$authorId).hashCode());
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "Comment(id=" + this.getId() + ", targetId=" + this.getTargetId() + ", targetType=" + this.getTargetType() + ", content=" + this.getContent() + ", authorId=" + this.getAuthorId() + ", createdAt=" + this.getCreatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}

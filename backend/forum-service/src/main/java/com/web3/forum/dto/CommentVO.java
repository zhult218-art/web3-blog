/*
 * Decompiled with CFR 0.152.
 */
package com.web3.forum.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CommentVO —— 评论视图对象（VO）
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：对前端输出的评论数据载体，由 Comment 实体转换而来；
 * id 使用 ToStringSerializer 序列化为字符串，避免雪花 ID 超出 JS Number 安全范围导致精度丢失。
 * parentId 标记父评论（楼中楼），replyToName 为被回复人昵称；children 存放回复该评论的子评论列表。
 */
public class CommentVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long targetId;
    private String targetType;
    private String content;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private String replyToName;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private List<CommentVO> children = new ArrayList<CommentVO>();

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

    public List<CommentVO> getChildren() {
        return this.children;
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

    public void setChildren(List<CommentVO> children) {
        this.children = children;
    }

    public void addChild(CommentVO child) {
        this.children.add(child);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CommentVO)) {
            return false;
        }
        CommentVO other = (CommentVO)o;
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
        return other instanceof CommentVO;
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
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "CommentVO(id=" + this.getId() + ", targetId=" + this.getTargetId() + ", targetType=" + this.getTargetType() + ", content=" + this.getContent() + ", authorId=" + this.getAuthorId() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}

/*
 * Decompiled with CFR 0.152.
 */
package com.web3.forum.dto;

/**
 * CommentCreateDTO —— 评论创建请求参数对象（DTO）
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：接收前端 POST /comment 请求体，承载目标对象 ID 与类型（targetId + targetType）、评论内容、作者名等参数。
 */
public class CommentCreateDTO {
    private Long targetId;
    private String targetType;
    private String content;
    private Long parentId;
    private String replyToName;
    private String authorName;

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

    public String getAuthorName() {
        return this.authorName;
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

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CommentCreateDTO)) {
            return false;
        }
        CommentCreateDTO other = (CommentCreateDTO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$targetId = this.getTargetId();
        Long other$targetId = other.getTargetId();
        if (this$targetId == null ? other$targetId != null : !((Object)this$targetId).equals(other$targetId)) {
            return false;
        }
        String this$targetType = this.getTargetType();
        String other$targetType = other.getTargetType();
        if (this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType)) {
            return false;
        }
        String this$content = this.getContent();
        String other$content = other.getContent();
        return !(this$content == null ? other$content != null : !this$content.equals(other$content));
    }

    protected boolean canEqual(Object other) {
        return other instanceof CommentCreateDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $targetId = this.getTargetId();
        result = result * 59 + ($targetId == null ? 43 : ((Object)$targetId).hashCode());
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        return result;
    }

    public String toString() {
        return "CommentCreateDTO(targetId=" + this.getTargetId() + ", targetType=" + this.getTargetType() + ", content=" + this.getContent() + ")";
    }
}

/*
 * Decompiled with CFR 0.152.
 */
package com.web3.forum.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

/**
 * PostVO —— 帖子视图对象（VO）
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：对前端输出的帖子数据载体，由 Post 实体转换而来；
 * id 使用 ToStringSerializer 序列化为字符串，避免雪花 ID 超出 JS Number 安全范围导致精度丢失。
 */
public class PostVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String content;
    private String category;
    private Long authorId;
    private String authorName;
    private Integer likeCount;
    private Integer replyCount;
    private Long viewCount;
    private Integer isPinned;
    private String mediaUrl;
    private String mediaType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

    public String getCategory() {
        return this.category;
    }

    public Long getAuthorId() {
        return this.authorId;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public Integer getLikeCount() {
        return this.likeCount;
    }

    public Integer getReplyCount() {
        return this.replyCount;
    }

    public Long getViewCount() {
        return this.viewCount;
    }

    public Integer getIsPinned() {
        return this.isPinned;
    }

    public String getMediaUrl() {
        return this.mediaUrl;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public void setIsPinned(Integer isPinned) {
        this.isPinned = isPinned;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PostVO)) {
            return false;
        }
        PostVO other = (PostVO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$authorId = this.getAuthorId();
        Long other$authorId = other.getAuthorId();
        if (this$authorId == null ? other$authorId != null : !((Object)this$authorId).equals(other$authorId)) {
            return false;
        }
        Integer this$likeCount = this.getLikeCount();
        Integer other$likeCount = other.getLikeCount();
        if (this$likeCount == null ? other$likeCount != null : !((Object)this$likeCount).equals(other$likeCount)) {
            return false;
        }
        Integer this$replyCount = this.getReplyCount();
        Integer other$replyCount = other.getReplyCount();
        if (this$replyCount == null ? other$replyCount != null : !((Object)this$replyCount).equals(other$replyCount)) {
            return false;
        }
        Integer this$isPinned = this.getIsPinned();
        Integer other$isPinned = other.getIsPinned();
        if (this$isPinned == null ? other$isPinned != null : !((Object)this$isPinned).equals(other$isPinned)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$content = this.getContent();
        String other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) {
            return false;
        }
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
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
        return other instanceof PostVO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $authorId = this.getAuthorId();
        result = result * 59 + ($authorId == null ? 43 : ((Object)$authorId).hashCode());
        Integer $likeCount = this.getLikeCount();
        result = result * 59 + ($likeCount == null ? 43 : ((Object)$likeCount).hashCode());
        Integer $replyCount = this.getReplyCount();
        result = result * 59 + ($replyCount == null ? 43 : ((Object)$replyCount).hashCode());
        Integer $isPinned = this.getIsPinned();
        result = result * 59 + ($isPinned == null ? 43 : ((Object)$isPinned).hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "PostVO(id=" + this.getId() + ", title=" + this.getTitle() + ", content=" + this.getContent() + ", category=" + this.getCategory() + ", authorId=" + this.getAuthorId() + ", authorName=" + this.getAuthorName() + ", likeCount=" + this.getLikeCount() + ", replyCount=" + this.getReplyCount() + ", viewCount=" + this.getViewCount() + ", isPinned=" + this.getIsPinned() + ", mediaUrl=" + this.getMediaUrl() + ", mediaType=" + this.getMediaType() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}

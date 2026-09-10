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
 * Post —— 论坛帖子实体类
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 对应数据库表：post（帖子表）。
 * <p>
 * 字段说明：id 为雪花算法主键；likeCount 点赞数；replyCount 回复数；viewCount 浏览量；
 * isPinned 是否置顶（1=置顶）；mediaUrl/mediaType 帖子携带的媒体资源与类型；
 * status 状态（如 published）；deleted 为逻辑删除标记（@TableLogic 自动过滤）。
 * createdAt 插入时自动填充，updatedAt 插入/更新时自动填充。
 */
@TableName(value="post")
public class Post {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private String content;
    private String category;
    private String status;
    private Long authorId;
    private String authorName;
    private Integer likeCount;
    private Integer replyCount;
    private Long viewCount;
    private Integer isPinned;
    private String mediaUrl;
    private String mediaType;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill=FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

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

    public String getStatus() {
        return this.status;
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

    public Integer getDeleted() {
        return this.deleted;
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

    public void setStatus(String status) {
        this.status = status;
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

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Post)) {
            return false;
        }
        Post other = (Post)o;
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
        Integer this$deleted = this.getDeleted();
        Integer other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !((Object)this$deleted).equals(other$deleted)) {
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
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
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
        return other instanceof Post;
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
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "Post(id=" + this.getId() + ", title=" + this.getTitle() + ", content=" + this.getContent() + ", category=" + this.getCategory() + ", status=" + this.getStatus() + ", authorId=" + this.getAuthorId() + ", authorName=" + this.getAuthorName() + ", likeCount=" + this.getLikeCount() + ", replyCount=" + this.getReplyCount() + ", viewCount=" + this.getViewCount() + ", isPinned=" + this.getIsPinned() + ", mediaUrl=" + this.getMediaUrl() + ", mediaType=" + this.getMediaType() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}

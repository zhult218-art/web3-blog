/*
 * Decompiled with CFR 0.152.
 */
package com.web3.blog.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

/**
 * ArticleVO —— 文章视图对象（VO）
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：对前端输出的文章数据载体，由 Article 实体转换而来，
 * 通过 Jackson 序列化为 JSON 返回给前端；id 使用 ToStringSerializer 序列化为字符串，
 * 避免 Long 精度丢失问题（雪花 ID 超过 JS Number 安全范围）。
 */
public class ArticleVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String category;
    private String tags;
    private String cover;
    private Long authorId;
    private String authorName;
    private Long viewCount;
    private Integer likeCount;
    private String status;
    private Integer isTop;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSummary() {
        return this.summary;
    }

    public String getContent() {
        return this.content;
    }

    public String getCategory() {
        return this.category;
    }

    public String getTags() {
        return this.tags;
    }

    public String getCover() {
        return this.cover;
    }

    public Long getAuthorId() {
        return this.authorId;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public Long getViewCount() {
        return this.viewCount;
    }

    public Integer getLikeCount() {
        return this.likeCount;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getIsTop() {
        return this.isTop;
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

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setIsTop(Integer isTop) {
        this.isTop = isTop;
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
        if (!(o instanceof ArticleVO)) {
            return false;
        }
        ArticleVO other = (ArticleVO)o;
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
        Long this$viewCount = this.getViewCount();
        Long other$viewCount = other.getViewCount();
        if (this$viewCount == null ? other$viewCount != null : !((Object)this$viewCount).equals(other$viewCount)) {
            return false;
        }
        Integer this$isTop = this.getIsTop();
        Integer other$isTop = other.getIsTop();
        if (this$isTop == null ? other$isTop != null : !((Object)this$isTop).equals(other$isTop)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$summary = this.getSummary();
        String other$summary = other.getSummary();
        if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) {
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
        String this$tags = this.getTags();
        String other$tags = other.getTags();
        if (this$tags == null ? other$tags != null : !this$tags.equals(other$tags)) {
            return false;
        }
        String this$cover = this.getCover();
        String other$cover = other.getCover();
        if (this$cover == null ? other$cover != null : !this$cover.equals(other$cover)) {
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
        return other instanceof ArticleVO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $authorId = this.getAuthorId();
        result = result * 59 + ($authorId == null ? 43 : ((Object)$authorId).hashCode());
        Long $viewCount = this.getViewCount();
        result = result * 59 + ($viewCount == null ? 43 : ((Object)$viewCount).hashCode());
        Integer $isTop = this.getIsTop();
        result = result * 59 + ($isTop == null ? 43 : ((Object)$isTop).hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $summary = this.getSummary();
        result = result * 59 + ($summary == null ? 43 : $summary.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        String $tags = this.getTags();
        result = result * 59 + ($tags == null ? 43 : $tags.hashCode());
        String $cover = this.getCover();
        result = result * 59 + ($cover == null ? 43 : $cover.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "ArticleVO(id=" + this.getId() + ", title=" + this.getTitle() + ", summary=" + this.getSummary() + ", content=" + this.getContent() + ", category=" + this.getCategory() + ", tags=" + this.getTags() + ", cover=" + this.getCover() + ", authorId=" + this.getAuthorId() + ", authorName=" + this.getAuthorName() + ", viewCount=" + this.getViewCount() + ", likeCount=" + this.getLikeCount() + ", status=" + this.getStatus() + ", isTop=" + this.getIsTop() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}

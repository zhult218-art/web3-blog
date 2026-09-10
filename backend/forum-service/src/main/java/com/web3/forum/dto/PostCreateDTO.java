/*
 * Decompiled with CFR 0.152.
 */
package com.web3.forum.dto;

/**
 * PostCreateDTO —— 帖子创建/更新请求参数对象（DTO）
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：接收前端 POST /post 与 PUT /post/{id} 请求体，
 * 承载标题、内容、分类、作者名、媒体资源地址与类型等提交参数。
 */
public class PostCreateDTO {
    private String title;
    private String content;
    private String category;
    private String authorName;
    private String mediaUrl;
    private String mediaType;

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

    public String getCategory() {
        return this.category;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public String getMediaUrl() {
        return this.mediaUrl;
    }

    public String getMediaType() {
        return this.mediaType;
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

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PostCreateDTO)) {
            return false;
        }
        PostCreateDTO other = (PostCreateDTO)o;
        if (!other.canEqual(this)) {
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
        return !(this$category == null ? other$category != null : !this$category.equals(other$category));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PostCreateDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        return result;
    }

    public String toString() {
        return "PostCreateDTO(title=" + this.getTitle() + ", content=" + this.getContent() + ", category=" + this.getCategory() + ", authorName=" + this.getAuthorName() + ", mediaUrl=" + this.getMediaUrl() + ", mediaType=" + this.getMediaType() + ")";
    }
}

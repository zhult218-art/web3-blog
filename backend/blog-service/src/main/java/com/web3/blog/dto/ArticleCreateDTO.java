/*
 * Decompiled with CFR 0.152.
 */
package com.web3.blog.dto;

/**
 * ArticleCreateDTO —— 文章创建/更新请求参数对象（DTO）
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：接收前端 POST /article 与 PUT /article/{id} 请求体，
 * 承载标题、摘要、正文、分类、标签、封面、状态、作者名等提交参数。
 */
public class ArticleCreateDTO {
    private String title;
    private String summary;
    private String content;
    private String category;
    private String tags;
    private String cover;
    private String status;
    private Integer isTop;
    private String authorName;

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

    public String getStatus() {
        return this.status;
    }

    public Integer getIsTop() {
        return this.isTop;
    }

    public String getAuthorName() {
        return this.authorName;
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

    public void setStatus(String status) {
        this.status = status;
    }

    public void setIsTop(Integer isTop) {
        this.isTop = isTop;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ArticleCreateDTO)) {
            return false;
        }
        ArticleCreateDTO other = (ArticleCreateDTO)o;
        if (!other.canEqual(this)) {
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
        return !(this$status == null ? other$status != null : !this$status.equals(other$status));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ArticleCreateDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
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
        return result;
    }

    public String toString() {
        return "ArticleCreateDTO(title=" + this.getTitle() + ", summary=" + this.getSummary() + ", content=" + this.getContent() + ", category=" + this.getCategory() + ", tags=" + this.getTags() + ", cover=" + this.getCover() + ", status=" + this.getStatus() + ")";
    }
}

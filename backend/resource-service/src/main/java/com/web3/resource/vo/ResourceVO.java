/*
 * Decompiled with CFR 0.152.
 */
package com.web3.resource.vo;

import java.time.LocalDateTime;

/**
 * 资源展示层 VO(resource-service 模块)。
 *
 * <p>资源接口返回给前端的模型,字段与 {@link com.web3.resource.entity.Resource}
 * 一致,另附加 downloadCount 下载计数字段,不包含逻辑删除标记 deleted。</p>
 */
public class ResourceVO {
    /** 资源 ID */
    private Long id;
    /** 资源标题 */
    private String title;
    /** 资源描述 */
    private String description;
    /** 资源分类 */
    private String category;
    /** 存储文件名 */
    private String filename;
    /** 原始文件名 */
    private String originalFilename;
    /** 文件 MIME 类型 */
    private String contentType;
    /** 文件大小(字节) */
    private Long size;
    /** 下载地址 */
    private String downloadUrl;
    /** 下载次数 */
    private Long downloadCount;
    /** 创建时间 */
    private LocalDateTime createdAt;

    public Long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getCategory() {
        return this.category;
    }

    public String getFilename() {
        return this.filename;
    }

    public String getOriginalFilename() {
        return this.originalFilename;
    }

    public String getContentType() {
        return this.contentType;
    }

    public Long getSize() {
        return this.size;
    }

    public String getDownloadUrl() {
        return this.downloadUrl;
    }

    public Long getDownloadCount() {
        return this.downloadCount;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ResourceVO)) {
            return false;
        }
        ResourceVO other = (ResourceVO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$size = this.getSize();
        Long other$size = other.getSize();
        if (this$size == null ? other$size != null : !((Object)this$size).equals(other$size)) {
            return false;
        }
        Long this$downloadCount = this.getDownloadCount();
        Long other$downloadCount = other.getDownloadCount();
        if (this$downloadCount == null ? other$downloadCount != null : !((Object)this$downloadCount).equals(other$downloadCount)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
            return false;
        }
        String this$filename = this.getFilename();
        String other$filename = other.getFilename();
        if (this$filename == null ? other$filename != null : !this$filename.equals(other$filename)) {
            return false;
        }
        String this$originalFilename = this.getOriginalFilename();
        String other$originalFilename = other.getOriginalFilename();
        if (this$originalFilename == null ? other$originalFilename != null : !this$originalFilename.equals(other$originalFilename)) {
            return false;
        }
        String this$contentType = this.getContentType();
        String other$contentType = other.getContentType();
        if (this$contentType == null ? other$contentType != null : !this$contentType.equals(other$contentType)) {
            return false;
        }
        String this$downloadUrl = this.getDownloadUrl();
        String other$downloadUrl = other.getDownloadUrl();
        if (this$downloadUrl == null ? other$downloadUrl != null : !this$downloadUrl.equals(other$downloadUrl)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ResourceVO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $size = this.getSize();
        result = result * 59 + ($size == null ? 43 : ((Object)$size).hashCode());
        Long $downloadCount = this.getDownloadCount();
        result = result * 59 + ($downloadCount == null ? 43 : ((Object)$downloadCount).hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        String $filename = this.getFilename();
        result = result * 59 + ($filename == null ? 43 : $filename.hashCode());
        String $originalFilename = this.getOriginalFilename();
        result = result * 59 + ($originalFilename == null ? 43 : $originalFilename.hashCode());
        String $contentType = this.getContentType();
        result = result * 59 + ($contentType == null ? 43 : $contentType.hashCode());
        String $downloadUrl = this.getDownloadUrl();
        result = result * 59 + ($downloadUrl == null ? 43 : $downloadUrl.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "ResourceVO(id=" + this.getId() + ", title=" + this.getTitle() + ", description=" + this.getDescription() + ", category=" + this.getCategory() + ", filename=" + this.getFilename() + ", originalFilename=" + this.getOriginalFilename() + ", contentType=" + this.getContentType() + ", size=" + this.getSize() + ", downloadUrl=" + this.getDownloadUrl() + ", downloadCount=" + this.getDownloadCount() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}

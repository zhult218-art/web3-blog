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
package com.web3.resource.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 资源文件实体类,对应数据库表 {@code resource}。
 *
 * <p>保存上传资源文件的元信息(标题、描述、分类、存储文件名、原始文件名、
 * 文件类型、大小、下载地址等)。id 主键由 MyBatis-Plus 自动分配;
 * createdAt 插入时自动填充;deleted 标注 {@code @TableLogic} 逻辑删除。</p>
 */
@TableName(value="resource")
public class Resource {
    /**
     * 资源主键(雪花 ID,自动分配)
     */
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    /** 资源标题 */
    private String title;
    /** 资源描述 */
    private String description;
    /** 资源分类 */
    private String category;
    /** 存储文件名(UUID 重命名,含扩展名) */
    private String filename;
    /** 用户上传时的原始文件名 */
    private String originalFilename;
    /** 文件 MIME 类型 */
    private String contentType;
    /** 文件大小(字节) */
    private Long size;
    /** 下载地址(/resource/download/{filename}) */
    private String downloadUrl;
    /** 创建时间(插入时自动填充) */
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 逻辑删除标记(0 正常,1 已删除) */
    @TableLogic
    private Integer deleted;

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

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
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
        if (!(o instanceof Resource)) {
            return false;
        }
        Resource other = (Resource)o;
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
        return other instanceof Resource;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $size = this.getSize();
        result = result * 59 + ($size == null ? 43 : ((Object)$size).hashCode());
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
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
        return "Resource(id=" + this.getId() + ", title=" + this.getTitle() + ", description=" + this.getDescription() + ", category=" + this.getCategory() + ", filename=" + this.getFilename() + ", originalFilename=" + this.getOriginalFilename() + ", contentType=" + this.getContentType() + ", size=" + this.getSize() + ", downloadUrl=" + this.getDownloadUrl() + ", createdAt=" + this.getCreatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}

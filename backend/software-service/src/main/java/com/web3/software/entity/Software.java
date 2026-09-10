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
package com.web3.software.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 软件下载实体类,对应数据库表 {@code software}。
 *
 * <p>保存网站"软件下载"页面的软件条目(名称、描述、下载地址、分类、版本、
 * 大小、适用系统、图标等)。id 主键由 MyBatis-Plus 自动分配;
 * createdAt 插入时自动填充;deleted 标注 {@code @TableLogic} 逻辑删除。</p>
 */
@TableName(value="software")
public class Software {
    /**
     * 软件主键(雪花 ID,自动分配)
     */
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    /** 软件名称 */
    private String name;
    /** 软件描述 */
    private String description;
    /** 下载地址 */
    private String downloadUrl;
    /** 软件分类 */
    private String category;
    /** 版本号 */
    private String version;
    /** 文件大小(字节) */
    private Long size;
    /** 适用操作系统,如 Windows/macOS/Linux */
    private String os;
    /** 图标地址 */
    private String icon;
    /** 下载次数 */
    private Long downloadCount;
    /** 创建时间(插入时自动填充) */
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 逻辑删除标记(0 正常,1 已删除) */
    @TableLogic
    private Integer deleted;

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getDownloadUrl() {
        return this.downloadUrl;
    }

    public String getCategory() {
        return this.category;
    }

    public String getVersion() {
        return this.version;
    }

    public Long getSize() {
        return this.size;
    }

    public String getOs() {
        return this.os;
    }

    public String getIcon() {
        return this.icon;
    }

    public Long getDownloadCount() {
        return this.downloadCount;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
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
        if (!(o instanceof Software)) {
            return false;
        }
        Software other = (Software)o;
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
        Integer this$deleted = this.getDeleted();
        Integer other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !((Object)this$deleted).equals(other$deleted)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$downloadUrl = this.getDownloadUrl();
        String other$downloadUrl = other.getDownloadUrl();
        if (this$downloadUrl == null ? other$downloadUrl != null : !this$downloadUrl.equals(other$downloadUrl)) {
            return false;
        }
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
            return false;
        }
        String this$version = this.getVersion();
        String other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) {
            return false;
        }
        String this$os = this.getOs();
        String other$os = other.getOs();
        if (this$os == null ? other$os != null : !this$os.equals(other$os)) {
            return false;
        }
        String this$icon = this.getIcon();
        String other$icon = other.getIcon();
        if (this$icon == null ? other$icon != null : !this$icon.equals(other$icon)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Software;
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
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $downloadUrl = this.getDownloadUrl();
        result = result * 59 + ($downloadUrl == null ? 43 : $downloadUrl.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        String $version = this.getVersion();
        result = result * 59 + ($version == null ? 43 : $version.hashCode());
        String $os = this.getOs();
        result = result * 59 + ($os == null ? 43 : $os.hashCode());
        String $icon = this.getIcon();
        result = result * 59 + ($icon == null ? 43 : $icon.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "Software(id=" + this.getId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", downloadUrl=" + this.getDownloadUrl() + ", category=" + this.getCategory() + ", version=" + this.getVersion() + ", size=" + this.getSize() + ", os=" + this.getOs() + ", icon=" + this.getIcon() + ", downloadCount=" + this.getDownloadCount() + ", createdAt=" + this.getCreatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}

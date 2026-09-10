/*
 * Decompiled with CFR 0.152.
 */
package com.web3.software.vo;

import java.time.LocalDateTime;

/**
 * 软件展示层 VO(software-service 模块)。
 *
 * <p>软件接口返回给前端的模型,字段与 {@link com.web3.software.entity.Software}
 * 一致但不包含逻辑删除标记 deleted。</p>
 */
public class SoftwareVO {
    /** 软件 ID */
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
    /** 适用操作系统 */
    private String os;
    /** 图标地址 */
    private String icon;
    /** 下载次数 */
    private Long downloadCount;
    /** 创建时间 */
    private LocalDateTime createdAt;

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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SoftwareVO)) {
            return false;
        }
        SoftwareVO other = (SoftwareVO)o;
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
        return other instanceof SoftwareVO;
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
        return "SoftwareVO(id=" + this.getId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", downloadUrl=" + this.getDownloadUrl() + ", category=" + this.getCategory() + ", version=" + this.getVersion() + ", size=" + this.getSize() + ", os=" + this.getOs() + ", icon=" + this.getIcon() + ", downloadCount=" + this.getDownloadCount() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}

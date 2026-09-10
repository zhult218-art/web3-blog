/*
 * Decompiled with CFR 0.152.
 */
package com.web3.tool.vo;

import java.time.LocalDateTime;

/**
 * 脚本展示层 VO(tool-service 模块)。
 *
 * <p>脚本接口返回给前端的模型,字段与 {@link com.web3.tool.entity.Script}
 * 一致但不包含逻辑删除标记 deleted。</p>
 */
public class ScriptVO {
    /** 脚本 ID */
    private Long id;
    /** 脚本名称 */
    private String name;
    /** 脚本描述 */
    private String description;
    /** 脚本源码内容 */
    private String content;
    /** 脚本分类 */
    private String category;
    /** 脚本语言 */
    private String language;
    /** 脚本版本号 */
    private String version;
    /** 下载次数 */
    private Long downloadCount;
    /** 脚本标签 */
    private String tags;
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

    public String getContent() {
        return this.content;
    }

    public String getCategory() {
        return this.category;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getVersion() {
        return this.version;
    }

    public Long getDownloadCount() {
        return this.downloadCount;
    }

    public String getTags() {
        return this.tags;
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

    public void setContent(String content) {
        this.content = content;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ScriptVO)) {
            return false;
        }
        ScriptVO other = (ScriptVO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
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
        String this$language = this.getLanguage();
        String other$language = other.getLanguage();
        if (this$language == null ? other$language != null : !this$language.equals(other$language)) {
            return false;
        }
        String this$version = this.getVersion();
        String other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) {
            return false;
        }
        String this$tags = this.getTags();
        String other$tags = other.getTags();
        if (this$tags == null ? other$tags != null : !this$tags.equals(other$tags)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ScriptVO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $downloadCount = this.getDownloadCount();
        result = result * 59 + ($downloadCount == null ? 43 : ((Object)$downloadCount).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        String $language = this.getLanguage();
        result = result * 59 + ($language == null ? 43 : $language.hashCode());
        String $version = this.getVersion();
        result = result * 59 + ($version == null ? 43 : $version.hashCode());
        String $tags = this.getTags();
        result = result * 59 + ($tags == null ? 43 : $tags.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "ScriptVO(id=" + this.getId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", content=" + this.getContent() + ", category=" + this.getCategory() + ", language=" + this.getLanguage() + ", version=" + this.getVersion() + ", downloadCount=" + this.getDownloadCount() + ", tags=" + this.getTags() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}

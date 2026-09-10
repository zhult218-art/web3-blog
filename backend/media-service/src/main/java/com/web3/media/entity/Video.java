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
package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Video —— 视频实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：video（视频表）。
 * <p>
 * 字段说明：id 为雪花算法主键；url 视频文件访问地址（如 /video/file/xxx.mp4）；duration 时长（秒）；
 * episode 集数；deleted 为逻辑删除标记（@TableLogic 自动过滤）；createdAt 插入时自动填充。
 */
@TableName(value="video")
public class Video {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private String description;
    private String url;
    private String cover;
    private Integer duration;
    private String tags;
    private Integer episode;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
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

    public String getUrl() {
        return this.url;
    }

    public String getCover() {
        return this.cover;
    }

    public Integer getDuration() {
        return this.duration;
    }

    public String getTags() {
        return this.tags;
    }

    public Integer getEpisode() {
        return this.episode;
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

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setEpisode(Integer episode) {
        this.episode = episode;
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
        if (!(o instanceof Video)) {
            return false;
        }
        Video other = (Video)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$duration = this.getDuration();
        Integer other$duration = other.getDuration();
        if (this$duration == null ? other$duration != null : !((Object)this$duration).equals(other$duration)) {
            return false;
        }
        Integer this$episode = this.getEpisode();
        Integer other$episode = other.getEpisode();
        if (this$episode == null ? other$episode != null : !((Object)this$episode).equals(other$episode)) {
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
        String this$url = this.getUrl();
        String other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) {
            return false;
        }
        String this$cover = this.getCover();
        String other$cover = other.getCover();
        if (this$cover == null ? other$cover != null : !this$cover.equals(other$cover)) {
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
        return other instanceof Video;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $duration = this.getDuration();
        result = result * 59 + ($duration == null ? 43 : ((Object)$duration).hashCode());
        Integer $episode = this.getEpisode();
        result = result * 59 + ($episode == null ? 43 : ((Object)$episode).hashCode());
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        String $cover = this.getCover();
        result = result * 59 + ($cover == null ? 43 : $cover.hashCode());
        String $tags = this.getTags();
        result = result * 59 + ($tags == null ? 43 : $tags.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "Video(id=" + this.getId() + ", title=" + this.getTitle() + ", description=" + this.getDescription() + ", url=" + this.getUrl() + ", cover=" + this.getCover() + ", duration=" + this.getDuration() + ", tags=" + this.getTags() + ", episode=" + this.getEpisode() + ", createdAt=" + this.getCreatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}

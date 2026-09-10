/*
 * Decompiled with CFR 0.152.
 */
package com.web3.media.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

/**
 * VideoVO —— 视频视图对象（VO）
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：对前端输出的视频数据载体，由 Video 实体转换而来；
 * id 使用 ToStringSerializer 序列化为字符串，避免雪花 ID 超出 JS Number 安全范围导致精度丢失。
 */
public class VideoVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String description;
    private String url;
    private String cover;
    private Integer duration;
    private String tags;
    private Integer episode;
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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof VideoVO)) {
            return false;
        }
        VideoVO other = (VideoVO)o;
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
        return other instanceof VideoVO;
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
        return "VideoVO(id=" + this.getId() + ", title=" + this.getTitle() + ", description=" + this.getDescription() + ", url=" + this.getUrl() + ", cover=" + this.getCover() + ", duration=" + this.getDuration() + ", tags=" + this.getTags() + ", episode=" + this.getEpisode() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}

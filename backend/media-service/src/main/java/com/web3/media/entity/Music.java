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
 * Music —— 音乐实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：music（音乐表）。
 * <p>
 * 字段说明：id 为雪花算法主键；artist 歌手；url 音频访问地址；duration 时长（秒）；
 * album 专辑名；deleted 为逻辑删除标记（@TableLogic 自动过滤）；createdAt 插入时自动填充。
 */
@TableName(value="music")
public class Music {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private String artist;
    private String url;
    private String cover;
    private Integer duration;
    private String album;
    private String tags;
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

    public String getArtist() {
        return this.artist;
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

    public String getAlbum() {
        return this.album;
    }

    public String getTags() {
        return this.tags;
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

    public void setArtist(String artist) {
        this.artist = artist;
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

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setTags(String tags) {
        this.tags = tags;
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
        if (!(o instanceof Music)) {
            return false;
        }
        Music other = (Music)o;
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
        String this$artist = this.getArtist();
        String other$artist = other.getArtist();
        if (this$artist == null ? other$artist != null : !this$artist.equals(other$artist)) {
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
        String this$album = this.getAlbum();
        String other$album = other.getAlbum();
        if (this$album == null ? other$album != null : !this$album.equals(other$album)) {
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
        return other instanceof Music;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $duration = this.getDuration();
        result = result * 59 + ($duration == null ? 43 : ((Object)$duration).hashCode());
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $artist = this.getArtist();
        result = result * 59 + ($artist == null ? 43 : $artist.hashCode());
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        String $cover = this.getCover();
        result = result * 59 + ($cover == null ? 43 : $cover.hashCode());
        String $album = this.getAlbum();
        result = result * 59 + ($album == null ? 43 : $album.hashCode());
        String $tags = this.getTags();
        result = result * 59 + ($tags == null ? 43 : $tags.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "Music(id=" + this.getId() + ", title=" + this.getTitle() + ", artist=" + this.getArtist() + ", url=" + this.getUrl() + ", cover=" + this.getCover() + ", duration=" + this.getDuration() + ", album=" + this.getAlbum() + ", tags=" + this.getTags() + ", createdAt=" + this.getCreatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}

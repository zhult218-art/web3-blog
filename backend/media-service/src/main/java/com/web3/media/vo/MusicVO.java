/*
 * Decompiled with CFR 0.152.
 */
package com.web3.media.vo;

import java.time.LocalDateTime;

/**
 * MusicVO —— 音乐视图对象（VO）
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：对前端输出的音乐数据载体，由 Music 实体转换而来。
 */
public class MusicVO {
    private Long id;
    private String title;
    private String artist;
    private String url;
    private String cover;
    private Integer duration;
    private String album;
    private String tags;
    private LocalDateTime createdAt;

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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MusicVO)) {
            return false;
        }
        MusicVO other = (MusicVO)o;
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
        return other instanceof MusicVO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $duration = this.getDuration();
        result = result * 59 + ($duration == null ? 43 : ((Object)$duration).hashCode());
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
        return "MusicVO(id=" + this.getId() + ", title=" + this.getTitle() + ", artist=" + this.getArtist() + ", url=" + this.getUrl() + ", cover=" + this.getCover() + ", duration=" + this.getDuration() + ", album=" + this.getAlbum() + ", tags=" + this.getTags() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}

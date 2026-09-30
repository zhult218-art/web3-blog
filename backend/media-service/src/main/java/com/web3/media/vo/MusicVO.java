package com.web3.media.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * MusicVO —— 音乐视图对象（VO）
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：对前端输出的音乐数据载体，由 Music 实体转换而来。
 */
@Data
public class MusicVO {
    private Long id;
    private String title;
    private String artist;
    private String url;
    private String cover;
    private Integer duration;
    private String album;
    private String tags;
    /** 播放次数 */
    private Long playCount;
    /** 当前用户是否已收藏（详情接口返回，列表接口为 null） */
    private Boolean liked;
    private LocalDateTime createdAt;
}

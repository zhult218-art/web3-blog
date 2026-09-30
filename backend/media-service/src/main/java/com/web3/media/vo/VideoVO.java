package com.web3.media.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VideoVO —— 视频视图对象（VO）
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：对前端输出的视频数据载体，由 Video 实体转换而来；
 * id 使用 ToStringSerializer 序列化为字符串，避免雪花 ID 超出 JS Number 安全范围导致精度丢失。
 */
@Data
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
}

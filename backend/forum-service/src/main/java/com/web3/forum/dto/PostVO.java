package com.web3.forum.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PostVO —— 帖子视图对象（VO）
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：对前端输出的帖子数据载体，由 Post 实体转换而来；
 * id 使用 ToStringSerializer 序列化为字符串，避免雪花 ID 超出 JS Number 安全范围导致精度丢失。
 *
 * <p>关键注解作用：
 * <ul>
 *   <li>{@code @Data} 自动生成 getter/setter/equals/hashCode/toString；</li>
 *   <li>{@code @JsonSerialize(using = ToStringSerializer.class)} Long 序列化为字符串防止 JS 精度丢失。</li>
 * </ul>
 */
@Data
public class PostVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String content;
    private String category;
    /** 审核状态：published=已通过，pending=待审核，rejected=已驳回 */
    private String status;
    private Long authorId;
    private String authorName;
    private Integer likeCount;
    private Integer replyCount;
    private Long viewCount;
    private Integer isPinned;
    private String mediaUrl;
    private String mediaType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

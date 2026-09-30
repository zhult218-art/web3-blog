package com.web3.blog.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ArticleVO —— 文章视图对象（VO）
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：对前端输出的文章数据载体，由 Article 实体转换而来，
 * 通过 Jackson 序列化为 JSON 返回给前端；id 使用 ToStringSerializer 序列化为字符串，
 * 避免 Long 精度丢失问题（雪花 ID 超过 JS Number 安全范围）。
 *
 * <p>关键注解作用：
 * <ul>
 *   <li>{@code @Data} 自动生成 getter/setter/equals/hashCode/toString；</li>
 *   <li>{@code @JsonSerialize(using = ToStringSerializer.class)} 将 Long 序列化为字符串防止 JS 精度丢失。</li>
 * </ul>
 */
@Data
public class ArticleVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String category;
    private String tags;
    private String cover;
    private Long authorId;
    private String authorName;
    private Long viewCount;
    private Integer likeCount;
    private String status;
    private Integer isTop;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

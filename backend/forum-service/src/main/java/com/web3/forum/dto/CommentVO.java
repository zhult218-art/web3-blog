package com.web3.forum.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CommentVO —— 评论视图对象（VO）
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：对前端输出的评论数据载体，由 Comment 实体转换而来；
 * id 使用 ToStringSerializer 序列化为字符串，避免雪花 ID 超出 JS Number 安全范围导致精度丢失。
 * parentId 标记父评论（楼中楼），replyToName 为被回复人昵称；children 存放回复该评论的子评论列表。
 *
 * <p>关键注解作用：
 * <ul>
 *   <li>{@code @Data} 自动生成 getter/setter/equals/hashCode/toString；addChild 为业务自定义便捷方法，保留；</li>
 *   <li>{@code @JsonSerialize(using = ToStringSerializer.class)} Long 序列化为字符串防止 JS 精度丢失。</li>
 * </ul>
 */
@Data
public class CommentVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long targetId;
    private String targetType;
    private String content;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private String replyToName;
    private Long authorId;
    private String authorName;
    private String status;
    private LocalDateTime createdAt;
    /** 所属帖子标题（仅管理后台列表填充） */
    private String postTitle;
    private List<CommentVO> children = new ArrayList<>();

    /** 向子评论列表追加一条子评论（楼中楼） */
    public void addChild(CommentVO child) {
        this.children.add(child);
    }
}

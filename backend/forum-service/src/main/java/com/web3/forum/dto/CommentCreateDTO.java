package com.web3.forum.dto;

import lombok.Data;

/**
 * CommentCreateDTO —— 评论创建请求参数对象（DTO）
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：接收前端 POST /comment 请求体，承载目标对象 ID 与类型（targetId + targetType）、评论内容、作者名等参数。
 *
 * <p>关键注解作用：{@code @Data} 自动生成 getter/setter/equals/hashCode/toString。</p>
 */
@Data
public class CommentCreateDTO {
    private Long targetId;
    private String targetType;
    private String content;
    private Long parentId;
    private String replyToName;
    private String authorName;
}

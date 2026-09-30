package com.web3.forum.dto;

import lombok.Data;

/**
 * PostCreateDTO —— 帖子创建/更新请求参数对象（DTO）
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：接收前端 POST /post 与 PUT /post/{id} 请求体，
 * 承载标题、内容、分类、作者名、媒体资源地址与类型等提交参数。
 *
 * <p>关键注解作用：{@code @Data} 自动生成 getter/setter/equals/hashCode/toString。</p>
 */
@Data
public class PostCreateDTO {
    private String title;
    private String content;
    private String category;
    private String authorName;
    private String mediaUrl;
    private String mediaType;
}

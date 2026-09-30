package com.web3.blog.dto;

import lombok.Data;

/**
 * ArticleCreateDTO —— 文章创建/更新请求参数对象（DTO）
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 职责：接收前端 POST /article 与 PUT /article/{id} 请求体，
 * 承载标题、摘要、正文、分类、标签、封面、状态、作者名等提交参数。
 *
 * <p>关键注解作用：{@code @Data} 自动生成 getter/setter/equals/hashCode/toString。</p>
 */
@Data
public class ArticleCreateDTO {
    private String title;
    private String summary;
    private String content;
    private String category;
    private String tags;
    private String cover;
    private String status;
    private Integer isTop;
    private String authorName;
}

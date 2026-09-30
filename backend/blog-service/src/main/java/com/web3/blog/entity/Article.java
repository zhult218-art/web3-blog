package com.web3.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Article —— 博客文章实体类
 * <p>
 * 所属模块：blog-service（博客模块）。
 * <p>
 * 对应数据库表：article（文章表）。
 * <p>
 * 字段说明：id 为雪花算法主键；viewCount 浏览量；likeCount 点赞数；status 发布状态（PUBLISHED/DRAFT）；
 * isTop 是否置顶（1=置顶）；deleted 为逻辑删除标记（@TableLogic 注解，查询时自动过滤）。
 * createdAt 在插入时自动填充，updatedAt 在插入/更新时自动填充。
 *
 * <p>关键注解作用：
 * <ul>
 *   <li>{@code @Data} 自动生成 getter/setter/equals/hashCode/toString；</li>
 *   <li>{@code @TableId(type = ASSIGN_ID)} 雪花算法主键；{@code @TableLogic} 逻辑删除；{@code @TableField(fill=...)} 自动填充。</li>
 * </ul>
 */
@Data
@TableName(value = "article")
public class Article {
    @TableId(type = IdType.ASSIGN_ID)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}

package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Post —— 论坛帖子实体类
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 对应数据库表：post（帖子表）。
 * <p>
 * 字段说明：id 为雪花算法主键；likeCount 点赞数；replyCount 回复数；viewCount 浏览量；
 * isPinned 是否置顶（1=置顶）；mediaUrl/mediaType 帖子携带的媒体资源与类型；
 * status 状态（如 published）；deleted 为逻辑删除标记（@TableLogic 自动过滤）。
 * createdAt 插入时自动填充，updatedAt 插入/更新时自动填充。
 *
 * <p>关键注解作用：
 * <ul>
 *   <li>{@code @Data} 自动生成 getter/setter/equals/hashCode/toString；</li>
 *   <li>{@code @TableId(type = ASSIGN_ID)} 雪花算法主键；{@code @TableLogic} 逻辑删除；{@code @TableField(fill=...)} 自动填充。</li>
 * </ul>
 */
@Data
@TableName(value = "post")
public class Post {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private String content;
    private String category;
    private String status;
    private Long authorId;
    private String authorName;
    private Integer likeCount;
    private Integer replyCount;
    private Long viewCount;
    private Integer isPinned;
    private String mediaUrl;
    private String mediaType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}

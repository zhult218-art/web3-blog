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
 * Comment —— 评论实体类
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 对应数据库表：comment（评论表）。
 * <p>
 * targetId + targetType 标识被评论的目标对象（如帖子）；deleted 为逻辑删除标记（@TableLogic 自动过滤）；
 * createdAt 插入时自动填充。
 *
 * <p>关键注解作用：
 * <ul>
 *   <li>{@code @Data} 自动生成 getter/setter/equals/hashCode/toString；</li>
 *   <li>{@code @TableId(type = ASSIGN_ID)} 雪花算法主键；{@code @TableLogic} 逻辑删除；{@code @TableField(fill=INSERT)} 自动填充。</li>
 * </ul>
 */
@Data
@TableName(value = "comment")
public class Comment {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long targetId;
    private String targetType;
    private String content;
    private Long parentId;
    private String replyToName;
    private Long authorId;
    private String authorName;
    /** 审核状态：published=已通过（公开可见），pending=待审核，rejected=已驳回 */
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}

package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LikeRecord —— 点赞记录实体类
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 对应数据库表：like_record（点赞记录表）。
 * <p>
 * 一条记录表示"某用户点赞了某目标"，targetId + targetType + userId 唯一；createdAt 插入时自动填充。
 *
 * <p>关键注解作用：
 * <ul>
 *   <li>{@code @Data} 自动生成 getter/setter/equals/hashCode/toString；</li>
 *   <li>{@code @TableId(type = ASSIGN_ID)} 雪花算法主键；{@code @TableField(fill=INSERT)} 创建时间自动填充。</li>
 * </ul>
 */
@Data
@TableName(value = "like_record")
public class LikeRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long targetId;
    private String targetType;
    private Long userId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

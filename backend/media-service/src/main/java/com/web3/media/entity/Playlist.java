package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Playlist —— 播放列表实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：playlist（播放列表表）。
 * <p>
 * userId 标识播放列表的归属用户（用于越权校验）；deleted 为逻辑删除标记（@TableLogic 自动过滤）；
 * createdAt 插入时自动填充。
 */
@Data
@TableName(value="playlist")
public class Playlist {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;
    private Long userId;
    private String cover;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}

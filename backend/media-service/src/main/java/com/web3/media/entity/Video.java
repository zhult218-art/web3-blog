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
 * Video —— 视频实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：video（视频表）。
 * <p>
 * 字段说明：id 为雪花算法主键；url 视频文件访问地址（如 /video/file/xxx.mp4）；duration 时长（秒）；
 * episode 集数；deleted 为逻辑删除标记（@TableLogic 自动过滤）；createdAt 插入时自动填充。
 */
@Data
@TableName(value="video")
public class Video {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private String description;
    private String url;
    private String cover;
    private Integer duration;
    private String tags;
    private Integer episode;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}

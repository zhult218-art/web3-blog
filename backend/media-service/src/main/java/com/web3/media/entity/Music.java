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
 * Music —— 音乐实体类
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 对应数据库表：music（音乐表）。
 * <p>
 * 字段说明：id 为雪花算法主键；artist 歌手；url 音频访问地址；duration 时长（秒）；
 * album 专辑名；deleted 为逻辑删除标记（@TableLogic 自动过滤）；createdAt 插入时自动填充。
 */
@Data
@TableName(value="music")
public class Music {
    @TableId(type=IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private String artist;
    private String url;
    private String cover;
    private Integer duration;
    private String album;
    private String tags;
    /** 播放次数，累加字段，默认 0 */
    private Long playCount;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}

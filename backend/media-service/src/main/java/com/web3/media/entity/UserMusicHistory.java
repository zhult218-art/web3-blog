package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UserMusicHistory —— 用户音乐播放历史实体
 * <p>
 * 对应数据库表：user_music_history（web3_media 库）。
 * 自增主键，user_id + played_at 联合索引便于按时间倒序分页。
 */
@Data
@TableName(value = "user_music_history")
public class UserMusicHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long musicId;
    private LocalDateTime playedAt;
}

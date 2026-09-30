package com.web3.media.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UserMusicLike —— 用户音乐收藏关系实体
 * <p>
 * 对应数据库表：user_music_like（web3_media 库）。
 * 联合主键 (user_id, music_id)，music_id 加索引便于按歌曲反查收藏用户。
 */
@Data
@TableName(value = "user_music_like")
public class UserMusicLike {
    private Long userId;
    private Long musicId;
    private LocalDateTime createdAt;
}

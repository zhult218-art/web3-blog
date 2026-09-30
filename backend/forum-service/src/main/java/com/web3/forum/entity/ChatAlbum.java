package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 群相册（文件夹）实体
 */
@Data
@TableName("chat_album")
public class ChatAlbum {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String name;
    private String coverUrl;
    private Long userId;
    private String nickname;
    private LocalDateTime createdAt;
}

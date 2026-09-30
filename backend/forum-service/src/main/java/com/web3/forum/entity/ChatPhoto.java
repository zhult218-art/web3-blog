package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 群相册照片实体（归属于某相册文件夹）
 */
@Data
@TableName("chat_photo")
public class ChatPhoto {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long albumId;
    private String url;
    private Long userId;
    private String nickname;
    private Integer width;
    private Integer height;
    private LocalDateTime createdAt;
}

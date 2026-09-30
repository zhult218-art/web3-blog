package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 群通知实体
 */
@Data
@TableName("chat_notification")
public class ChatNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String type;
    private String title;
    private String content;
    private Long userId;
    private String nickname;
    private LocalDateTime createdAt;
}

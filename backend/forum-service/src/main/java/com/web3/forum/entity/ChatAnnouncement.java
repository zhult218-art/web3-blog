package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 群公告实体
 */
@Data
@TableName("chat_announcement")
public class ChatAnnouncement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String title;
    private String content;
    private Long userId;
    private String nickname;
    private Integer pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 群聊消息实体
 * msgType: text=文本, image=图片, system=系统消息
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long userId;
    /** 发送者用户名（冗余） */
    private String username;
    /** 发送者昵称（冗余） */
    private String nickname;
    /** 发送者头像 URL（冗余） */
    private String avatar;
    /** 消息类型 */
    private String msgType;
    /** 消息内容 */
    private String content;
    /** 引用的目标消息ID（可空，用于引用回复） */
    private Long quoteId;
    /** 引用消息预览（冗余，避免前端再查一次） */
    private String quotePreview;
    private LocalDateTime createdAt;
}

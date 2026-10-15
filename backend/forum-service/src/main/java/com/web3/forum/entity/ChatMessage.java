package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    /**
     * 发送者用户ID（雪花 ID，19 位，超出 JS Number 安全范围）。
     * 必须序列化为字符串：Socket.IO 网关（node-service/chat-gateway）与
     * UserVO.id 均以字符串下发，若此处按数字下发会因精度丢失而与前端
     * 「自己发的消息」判定、成员头像目录匹配不上，蓝色右侧气泡会失效。
     */
    @JsonSerialize(using = ToStringSerializer.class)
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

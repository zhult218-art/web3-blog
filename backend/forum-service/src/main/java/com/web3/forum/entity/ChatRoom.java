package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 聊天室（群/私聊）实体
 * type: group=群聊（默认）, private=私聊（仅两名成员）
 */
@Data
@TableName("chat_room")
public class ChatRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 房间名称 */
    private String name;
    /** 描述 */
    private String description;
    /** 群主用户ID（私聊为发起者） */
    private Long ownerId;
    /** 是否需要审批加入：0-否（任何人可进），1-是（需管理员审批） */
    private Integer requireApproval;
    /** 房间类型：group=群聊, private=私聊 */
    private String type;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}

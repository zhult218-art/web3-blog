package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 聊天室（群）实体
 */
@Data
@TableName("chat_room")
public class ChatRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 群名称 */
    private String name;
    /** 群描述 */
    private String description;
    /** 群主用户ID */
    private Long ownerId;
    /** 是否需要审批加入：0-否（任何人可进），1-是（需管理员审批） */
    private Integer requireApproval;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}

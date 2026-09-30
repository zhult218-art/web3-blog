package com.web3.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 群成员/申请记录实体
 * status: pending=待审批, approved=已加入, rejected=已拒绝
 */
@Data
@TableName("chat_room_member")
public class ChatRoomMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long userId;
    /** 加入时的用户名（冗余，方便展示） */
    private String username;
    /** 昵称 */
    private String nickname;
    /** 头像 URL（冗余自用户表，随最新资料同步） */
    private String avatar;
    /** 角色：member=成员, admin=管理员 */
    private String role;
    /** 状态：pending / approved / rejected */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

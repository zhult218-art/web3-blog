package com.web3.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类名：User
 * 所属模块：user-service（用户服务）
 * 职责：用户实体类，对应数据库表 {@code user}，映射用户核心信息。
 * 关键注解：{@code @TableName("user")} 指定表名；{@code @TableId(type = ASSIGN_ID)} 主键雪花 ID。
 * 字段说明：password=BCrypt 加密后的密码；role=角色（USER/ADMIN）；permissions=逗号分隔的权限字符串；
 *           createdAt/updatedAt 由 CommonMetaObjectHandler 自动填充；deleted=逻辑删除标记（{@code @TableLogic}）。
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String nickname;
    private String email;
    /** 手机号（手机验证码登录） */
    private String phone;
    /** Google 账号唯一 ID（OAuth 登录绑定） */
    private String googleId;
    /** 异世界职业（WARRIOR/MAGE/HEALER/ASSASSIN/RANGER/PALADIN/NECROMANCER/BERSERKER） */
    private String userClass;
    /** 实名认证：真实姓名 */
    private String realName;
    /** 实名认证：身份证号（脱敏存储） */
    private String idCard;
    /** 实名状态：0=未认证, 1=审核中, 2=已认证, 3=已拒绝 */
    private Integer verified;
    private String password;
    private String avatar;
    private String role;
    private String permissions;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}

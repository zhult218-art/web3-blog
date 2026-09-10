/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.FieldFill
 *  com.baomidou.mybatisplus.annotation.IdType
 *  com.baomidou.mybatisplus.annotation.TableField
 *  com.baomidou.mybatisplus.annotation.TableId
 *  com.baomidou.mybatisplus.annotation.TableLogic
 *  com.baomidou.mybatisplus.annotation.TableName
 */
package com.web3.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 类名：User
 * 所属模块：user-service（用户服务）
 * 职责：用户实体类，对应数据库表 user，映射用户核心信息。
 * 关键注解：@TableName("user") 指定表名；@TableId(type = ASSIGN_ID) 主键雪花 ID。
 * 字段说明：password=BCrypt 加密后的密码；role=角色（USER/ADMIN）；permissions=逗号分隔的权限字符串；
 *        createdAt/updatedAt 由 CommonMetaObjectHandler 自动填充；deleted=逻辑删除标记（@TableLogic）。
 */
@TableName(value="user")
public class User {
    @TableId(type=IdType.ASSIGN_ID)
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
    private String password;
    private String avatar;
    private String role;
    private String permissions;
    private Integer status;
    @TableField(fill=FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill=FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    public Long getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getNickname() {
        return this.nickname;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getGoogleId() {
        return this.googleId;
    }

    public String getPassword() {
        return this.password;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public String getRole() {
        return this.role;
    }

    public Integer getStatus() {
        return this.status;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getUserClass() {
        return this.userClass;
    }

    public void setUserClass(String userClass) {
        this.userClass = userClass;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPermissions() {
        return this.permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        User other = (User)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Integer this$deleted = this.getDeleted();
        Integer other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !((Object)this$deleted).equals(other$deleted)) {
            return false;
        }
        String this$username = this.getUsername();
        String other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) {
            return false;
        }
        String this$nickname = this.getNickname();
        String other$nickname = other.getNickname();
        if (this$nickname == null ? other$nickname != null : !this$nickname.equals(other$nickname)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        String this$password = this.getPassword();
        String other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) {
            return false;
        }
        String this$avatar = this.getAvatar();
        String other$avatar = other.getAvatar();
        if (this$avatar == null ? other$avatar != null : !this$avatar.equals(other$avatar)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof User;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Integer $deleted = this.getDeleted();
        result = result * 59 + ($deleted == null ? 43 : ((Object)$deleted).hashCode());
        String $username = this.getUsername();
        result = result * 59 + ($username == null ? 43 : $username.hashCode());
        String $nickname = this.getNickname();
        result = result * 59 + ($nickname == null ? 43 : $nickname.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $password = this.getPassword();
        result = result * 59 + ($password == null ? 43 : $password.hashCode());
        String $avatar = this.getAvatar();
        result = result * 59 + ($avatar == null ? 43 : $avatar.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "User(id=" + this.getId() + ", username=" + this.getUsername() + ", nickname=" + this.getNickname() + ", email=" + this.getEmail() + ", password=" + this.getPassword() + ", avatar=" + this.getAvatar() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}

/*
 * Decompiled with CFR 0.152.
 */
package com.web3.user.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 类名：UserVO
 * 所属模块：user-service（用户服务）
 * 职责：用户信息视图对象，向前端返回用户资料（不含密码等敏感字段），权限以字符串列表形式呈现。
 */
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String userClass;
    private String bio;
    private String website;
    private String role;
    private List<String> permissions;
    private Integer status;
    private LocalDateTime createdAt;

    public List<String> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getUserClass() {
        return this.userClass;
    }

    public void setUserClass(String userClass) {
        this.userClass = userClass;
    }

    public String getBio() {
        return this.bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getWebsite() {
        return this.website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}

/*
 * Decompiled with CFR 0.152.
 */
package com.web3.user.dto;

/**
 * 类名：UserRegisterDTO
 * 所属模块：user-service（用户服务）
 * 职责：用户注册/资料更新请求参数对象，承载注册（POST /user/register）、管理员建户（POST /user）、资料修改（PUT /user/profile）等接口的请求体。
 * 字段说明：username=用户名，password=密码，email=邮箱，nickname=昵称。
 */
public class UserRegisterDTO {
    private String username;
    private String password;
    private String email;
    private String nickname;

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

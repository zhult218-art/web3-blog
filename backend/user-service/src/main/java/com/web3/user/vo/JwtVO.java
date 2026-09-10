/*
 * Decompiled with CFR 0.152.
 */
package com.web3.user.vo;

import com.web3.user.vo.UserVO;

/**
 * 类名：JwtVO
 * 所属模块：user-service（用户服务）
 * 职责：登录/注册成功后的返回视图对象，封装 JWT 令牌、刷新令牌与用户信息，供前端保存会话凭证。
 */
public class JwtVO {
    private String token;
    private String refreshToken;
    private UserVO user;

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserVO getUser() {
        return this.user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }
}

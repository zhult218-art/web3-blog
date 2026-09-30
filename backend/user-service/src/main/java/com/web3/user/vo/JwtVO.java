package com.web3.user.vo;

import lombok.Data;

/**
 * 类名：JwtVO
 * 所属模块：user-service（用户服务）
 * 职责：登录/注册成功后的返回视图对象，封装 JWT 令牌、刷新令牌与用户信息，供前端保存会话凭证。
 */
@Data
public class JwtVO {
    private String token;
    private String refreshToken;
    private UserVO user;
}

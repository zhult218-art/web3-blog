/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.security.authentication.AbstractAuthenticationToken
 *  org.springframework.security.core.GrantedAuthority
 */
package com.web3.common.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * 类名：JwtAuthenticationToken
 * 所属模块：common-security（通用安全模块）
 * 职责：自定义的 JWT 认证令牌对象，封装令牌字符串、主体（用户名）与角色权限，供响应式安全框架传递认证信息。
 * 实现：继承 AbstractAuthenticationToken，构造时即标记为已认证（authenticated）。
 */
public class JwtAuthenticationToken
extends AbstractAuthenticationToken {
    private final String token;
    private final Object principal;

    /**
     * 构造已认证的 JWT 令牌对象。
     *
     * @param token       原始 JWT 字符串（作为凭证）
     * @param principal   主体信息（一般为用户名）
     * @param authorities 角色/权限集合
     */
    public JwtAuthenticationToken(String token, Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.token = token;
        this.principal = principal;
        this.setAuthenticated(true);
    }

    /**
     * 获取凭证（原始 JWT 字符串）。
     *
     * @return JWT 令牌字符串
     */
    public Object getCredentials() {
        return this.token;
    }

    /**
     * 获取主体信息（用户名）。
     *
     * @return 认证主体，一般为用户名
     */
    public Object getPrincipal() {
        return this.principal;
    }
}

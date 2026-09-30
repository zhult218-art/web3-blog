package com.web3.common.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * 类名：JwtAuthenticationToken
 * 所属模块：common-security（通用安全模块）
 * 职责：自定义的 JWT 认证令牌对象，封装令牌字符串、主体（用户名）与角色权限，供响应式安全框架传递认证信息。
 * 实现：继承 AbstractAuthenticationToken，构造时即标记为已认证（authenticated）。
 *
 * <p>2026-09-17 重写（CFR 反编译产物 → 语法清洁化）：
 * 去除 CFR 头部注释；保留手写构造器（含 {@code super(authorities)} 与 {@code setAuthenticated(true)} 逻辑）
 * 以及 getCredentials / getPrincipal 实现（方法名与字段名不一致，无法用 Lombok 自动生成）。</p>
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {
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
    @Override
    public Object getCredentials() {
        return this.token;
    }

    /**
     * 获取主体信息（用户名）。
     *
     * @return 认证主体，一般为用户名
     */
    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}

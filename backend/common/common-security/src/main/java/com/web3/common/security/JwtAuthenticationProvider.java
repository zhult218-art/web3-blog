/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.jsonwebtoken.Claims
 *  org.springframework.security.authentication.ReactiveAuthenticationManager
 *  org.springframework.security.authentication.UsernamePasswordAuthenticationToken
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.core.authority.SimpleGrantedAuthority
 *  org.springframework.stereotype.Component
 *  reactor.core.publisher.Mono
 */
package com.web3.common.security;

import com.web3.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
import java.util.Collections;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 类名：JwtAuthenticationProvider
 * 所属模块：common-security（通用安全模块）
 * 职责：响应式 JWT 认证管理器，解析令牌中的用户名与角色，构造 Spring Security 的 Authentication 对象。
 * 关键注解：@Component 注册为 Spring Bean，供网关及需要认证的 WebFlux 服务使用。
 * 实现：实现 ReactiveAuthenticationManager；令牌无效时返回 Mono.empty() 表示未认证。
 */
@Component
public class JwtAuthenticationProvider
implements ReactiveAuthenticationManager {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationProvider(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 认证流程：校验令牌 -> 读取 username 与 authorities 声明 -> 构造已认证的 Authentication 对象。
     *
     * @param authentication 携带原始令牌的认证请求对象
     * @return 认证成功返回含用户名、令牌与角色权限的 Authentication；令牌无效返回 Mono.empty()
     */
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();
        if (!this.jwtUtil.validate(token)) {
            return Mono.empty();
        }
        Claims claims = this.jwtUtil.parseClaims(token);
        String username = claims.getSubject();
        String authorities = (String)claims.get("authorities", String.class);
        String role = (authorities == null || authorities.isBlank()) ? "ROLE_USER" : authorities;
        return Mono.just(new UsernamePasswordAuthenticationToken((Object)username, (Object)token, Collections.singletonList(new SimpleGrantedAuthority(role))));
    }
}

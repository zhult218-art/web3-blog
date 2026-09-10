package com.web3.common.security;

import com.web3.common.security.JwtServerHttpConverter;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * 类名：JwtManager
 * 所属模块：common-security（通用安全模块）
 * 职责：响应式网关/WebFlux 服务的 JWT 认证 WebFilter，从请求中转换出令牌、完成认证并写入安全上下文（SecurityContext），供后续鉴权使用。
 * 实现：实现 WebFilter，通过 JwtServerHttpConverter 提取令牌，再交由 ReactiveAuthenticationManager 认证；未携带或无效令牌时放行并保留匿名上下文。
 */
public class JwtManager
implements WebFilter {
    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtServerHttpConverter converter;

    public JwtManager(ReactiveAuthenticationManager authenticationManager, JwtServerHttpConverter converter) {
        this.authenticationManager = authenticationManager;
        this.converter = converter;
    }

    /**
     * 过滤器入口：转换令牌 -> 认证 -> 注入安全上下文 -> 放行请求。
     *
     * @param exchange 当前 WebFlux 请求/响应交换对象
     * @param chain    过滤器链
     * @return 请求处理完成的信号（Mono<Void>）
     */
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Mono<Authentication> authMono = this.converter.convert(exchange);
        return authMono
            .flatMap(authentication -> this.authenticationManager.authenticate(authentication))
            .flatMap(auth -> chain.filter(exchange).contextWrite((ContextView) ReactiveSecurityContextHolder.withAuthentication((Authentication) auth)))
            .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)))
            .onErrorResume(e -> Mono.defer(() -> chain.filter(exchange)));
    }
}
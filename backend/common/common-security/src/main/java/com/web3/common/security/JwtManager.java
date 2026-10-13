package com.web3.common.security;

import com.web3.common.security.JwtServerHttpConverter;
import java.util.Optional;
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
     * <p>2026-10-13 修复：旧实现 {@code flatMap(chain.filter).switchIfEmpty(chain.filter)}
     * 在有效 token 下会把整条过滤器链订阅两次——{@code chain.filter} 返回的 Mono<Void>
     * 永远是空完成信号，switchIfEmpty 必然触发二次执行，导致响应提交后再次注入安全响应头
     * 抛出 UnsupportedOperationException、连接被强杀（chunked 终止帧丢失），
     * 经代理的客户端表现为无限挂起。现改为 Optional 包装认证结果，任何路径下
     * {@code chain.filter(exchange)} 恰好订阅一次；认证异常按匿名放行。</p>
     *
     * @param exchange 当前 WebFlux 请求/响应交换对象
     * @param chain    过滤器链
     * @return 请求处理完成的信号（Mono<Void>）
     */
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return this.converter.convert(exchange)
            .flatMap(authentication -> this.authenticationManager.authenticate(authentication))
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .onErrorResume(e -> Mono.just(Optional.empty()))
            .flatMap(opt -> {
                if (opt.isPresent()) {
                    return chain.filter(exchange)
                        .contextWrite((ContextView) ReactiveSecurityContextHolder.withAuthentication(opt.get()));
                }
                return chain.filter(exchange);
            });
    }
}
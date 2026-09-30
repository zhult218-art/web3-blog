package com.web3.common.security;

import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 类名：JwtAuthenticationEntryPoint
 * 所属模块：common-security（通用安全模块）
 * 职责：响应式（WebFlux）安全框架的未认证入口点，当请求未通过 JWT 认证时返回 HTTP 401 及统一的 JSON 错误体。
 * 说明：网关与各 WebFlux 服务均复用本类，保证未登录响应格式一致。
 * 实现：实现 ServerAuthenticationEntryPoint，直接向响应流写入 {"code":401,"message":"未登录或登录已过期"}。
 *
 * <p>2026-09-17 重写（CFR 反编译产物 → 语法清洁化）：
 * 去除 CFR 残留的冗余强转（{@code (HttpStatusCode)}、{@code (Publisher)}、{@code (Object)}）
 * 及随之失效的 import（HttpStatusCode、Publisher）；将 unicode 转义还原为中文字面量。</p>
 */
public class JwtAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    /**
     * 认证失败时的处理入口：向客户端写入 401 状态码与统一 JSON 错误信息。
     *
     * @param exchange 当前 WebFlux 请求/响应交换对象
     * @param ex       触发认证失败的异常
     * @return 响应完成信号（Mono&lt;Void&gt;）
     */
    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return Mono.defer(() -> {
            if (exchange.getResponse().isCommitted()) {
                return Mono.empty();
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] bytes = "{\"code\":401,\"message\":\"未登录或登录已过期\"}".getBytes();
            DefaultDataBuffer buffer = new DefaultDataBufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        });
    }
}

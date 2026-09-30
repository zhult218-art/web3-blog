package com.web3.common.security;

import io.jsonwebtoken.Claims;
import java.util.Collections;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 类名：JwtServerHttpConverter
 * 所属模块：common-security（通用安全模块）
 * 职责：响应式服务中从 HTTP 请求中提取并解析 JWT，将其转换为 JwtAuthenticationToken 的转换器。
 * 关键注解：@Component 注册为 Spring Bean。
 * 实现：实现 ServerAuthenticationConverter；无有效令牌时返回 Mono.empty() 表示匿名请求。
 *
 * <p>2026-09-17 重写（CFR 反编译产物 → 语法清洁化）：
 * 删除 CFR 残留的同包多余 import（JwtAuthenticationToken / JwtProperties / JwtUtil），
 * 去除冗余强转（{@code (String) auth}、{@code (String) claims.get(...)}）。</p>
 */
@Component
public class JwtServerHttpConverter implements ServerAuthenticationConverter {
    private final JwtUtil jwtUtil;
    private final JwtProperties properties;

    public JwtServerHttpConverter(JwtUtil jwtUtil, JwtProperties properties) {
        this.jwtUtil = jwtUtil;
        this.properties = properties;
    }

    /**
     * 转换入口：读取 Authorization 头 -> 校验令牌有效性 -> 组装认证对象。
     *
     * @param exchange 当前 WebFlux 请求/响应交换对象
     * @return 认证成功返回 JwtAuthenticationToken（含用户名、角色、Claims 详情）；无令牌或令牌无效返回 Mono.empty()
     */
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(auth) || !auth.startsWith(this.properties.getPrefix())) {
            return Mono.empty();
        }
        String token = auth.substring(this.properties.getPrefix().length());
        if (!this.jwtUtil.validate(token)) {
            return Mono.empty();
        }
        Claims claims = this.jwtUtil.parseClaims(token);
        String username = claims.getSubject();
        String authorities = claims.get("authorities", String.class);
        String role = (authorities == null || authorities.isBlank()) ? "ROLE_USER" : authorities;
        List<SimpleGrantedAuthority> roles = Collections.singletonList(new SimpleGrantedAuthority(role));
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(token, username, roles);
        authToken.setDetails(claims);
        return Mono.just(authToken);
    }
}

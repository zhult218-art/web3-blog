package com.web3.gateway.security;

import com.web3.common.security.JwtAuthenticationEntryPoint;
import com.web3.common.security.JwtAuthenticationProvider;
import com.web3.common.security.JwtManager;
import com.web3.common.security.JwtServerHttpConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 类名：GatewaySecurityConfig
 * 所属模块：gateway（API 网关服务）
 * 职责：网关安全配置类，装配 JWT 认证过滤器链、未认证入口点，并定义各路径的放行规则。
 * 关键注解：@Configuration 配置类；@EnableWebFluxSecurity 启用响应式（WebFlux）安全框架。
 *
 * <p>当前策略：所有请求放行（permitAll），OPTIONS 预检请求直接放行；
 * JWT 过滤器仍执行认证并将认证信息注入上下文，供下游服务各自校验。</p>
 *
 * <p>TODO（统一鉴权）：当前 39 条路由的鉴权分散在 13 个服务的 Controller 里（132 处手工
 * AuthUtils 调用）。未来若要把鉴权收敛到网关层，建议按以下分级配置 authorizeExchange：
 *
 * <pre>
 * 公开读（permitAll）：
 *   /user/login, /user/register, /user/oauth/**, /user/phone/code, /user/bind/**,
 *   /blog/**, /forum/**, /resource/**, /shop/**, /media/**, /photo/**, /uploads/**,
 *   /quant/**, /tool/**, /software/**, /pyquant/**, /whisper/**, /socket.io/**,
 *   /qzapi/**, /wifiapi/**, /osintapi/**, /voiceapi/**, /cockpit/**, /sixty/**,
 *   /itunes-audio/**, /audius-api/**
 *
 * 需登录（authenticated）：
 *   /user/**（除公开端点）, /jarvis/**, /ai-proxy/**, /chat/**, /vr/**,
 *   /admin/** 下的非公开统计接口
 *
 * 管理员专属（hasRole ADMIN）：
 *   /admin/** 下的管理操作（如服务启停、日志清理、配置下发）
 * </pre>
 *
 * <p>注意：/blog/**、/forum/** 等前缀下可能混有管理员端点（如 POST /blog/admin/**），
 * 网关层按前缀匹配会过于粗放；更彻底的方案是引入 `web3-auth-starter`，
 * 用方法级 @PreAuthorize 注解 + 网关全局过滤器，逐步替代 132 处手工调用。</p>
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

  /**
     * 构建安全过滤器链：禁用 CSRF/表单登录/Basic 认证，注册 JWT 过滤器与未认证入口点。
     *
     * @param http       WebFlux 的 ServerHttpSecurity 构建器
     * @param jwtManager JWT 认证过滤器
     * @param entryPoint 未认证（401）入口点
     * @return 配置完成的安全过滤器链
     */
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                       JwtManager jwtManager,
                                                       JwtAuthenticationEntryPoint entryPoint) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .addFilterAt(jwtManager, SecurityWebFiltersOrder.AUTHENTICATION)
        .exceptionHandling(spec -> spec.authenticationEntryPoint(entryPoint))
        .authorizeExchange(exchange -> exchange
            .pathMatchers(HttpMethod.OPTIONS).permitAll()
            .anyExchange().permitAll())
        .build();
  }

  /**
     * 注册 JWT 认证管理器 Bean。
     *
     * @param provider  JWT 认证管理器（令牌解析与认证）
     * @param converter 请求头 JWT 转换器
     * @return JwtManager 过滤器实例
     */
  @Bean
  public JwtManager jwtManager(JwtAuthenticationProvider provider, JwtServerHttpConverter converter) {
    return new JwtManager(provider, converter);
  }

  /**
     * 注册未认证（401）响应入口点 Bean。
     *
     * @return JwtAuthenticationEntryPoint 实例
     */
  @Bean
  public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
    return new JwtAuthenticationEntryPoint();
  }
}

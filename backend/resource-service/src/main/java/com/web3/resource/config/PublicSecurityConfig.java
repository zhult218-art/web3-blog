package com.web3.resource.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 类名：PublicSecurityConfig
 * 所属模块：resource-service（资源服务）
 * 职责：声明 Spring Security 安全过滤链，放行所有请求并关闭 CSRF；接口鉴权由业务代码通过
 *       com.web3.common.security.AuthUtils 手动校验 JWT 完成。
 * 关键注解：@Configuration 标记配置类；@EnableWebSecurity 启用 Web 安全。
 */
@Configuration
@EnableWebSecurity
public class PublicSecurityConfig {
    /**
     * 构建安全过滤链：所有请求放行、关闭 CSRF 防护。
     *
     * @param httpSecurity Spring Security 配置构建器
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(registry -> registry.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return httpSecurity.build();
    }
}

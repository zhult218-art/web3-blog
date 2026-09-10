/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.security.config.annotation.web.builders.HttpSecurity
 *  org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
 *  org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer$AuthorizedUrl
 *  org.springframework.security.web.SecurityFilterChain
 */
package com.web3.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 类名：PublicSecurityConfig
 * 所属模块：user-service（用户服务）
 * 职责：用户服务安全过滤链配置。
 * 关键注解：@Configuration 配置类；@EnableWebSecurity 启用 Spring Security（Servlet 体系）。
 * 说明：关闭 CSRF，并放行所有请求（permitAll）；实际的登录/权限校验由各 Controller 通过 AuthUtils 手动完成。
 */
@Configuration
@EnableWebSecurity
public class PublicSecurityConfig {
    /**
     * 构建安全过滤链：所有请求放行并禁用 CSRF。
     *
     * @param httpSecurity HttpSecurity 构建器
     * @return 配置完成的安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> ((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)authorizationManagerRequestMatcherRegistry.anyRequest()).permitAll()).csrf(csrfConfigurer -> csrfConfigurer.disable());
        return (SecurityFilterChain)httpSecurity.build();
    }
}

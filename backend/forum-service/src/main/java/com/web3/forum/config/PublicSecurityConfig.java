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
package com.web3.forum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PublicSecurityConfig —— 论坛服务安全配置类
 * <p>
 * 所属模块：forum-service（论坛模块）。
 * <p>
 * 职责：配置 Spring Security 过滤器链。本服务对外放行所有请求（permitAll），
 * 关闭 CSRF 防护，登录鉴权统一由网关侧完成，本服务不拦截任何请求。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Configuration：声明为 Spring 配置类</li>
 *   <li>@EnableWebSecurity：启用 Spring Security Web 安全配置</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class PublicSecurityConfig {
    /**
     * 构建安全过滤器链：所有请求放行并禁用 CSRF
     *
     * @param httpSecurity Spring Security 配置构建器
     * @return 配置完成的安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> ((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)authorizationManagerRequestMatcherRegistry.anyRequest()).permitAll()).csrf(csrfConfigurer -> csrfConfigurer.disable());
        return (SecurityFilterChain)httpSecurity.build();
    }
}

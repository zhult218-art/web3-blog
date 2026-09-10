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
package com.web3.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PublicSecurityConfig —— 商城服务安全配置类
 * <p>
 * 所属模块：shop-service（商城模块）。
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
     * @param http Spring Security 配置构建器
     * @return 配置完成的安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> ((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)auth.anyRequest()).permitAll());
        return (SecurityFilterChain)http.build();
    }
}

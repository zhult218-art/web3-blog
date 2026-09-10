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
package com.web3.software.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 软件下载服务安全配置(software-service 模块)。
 *
 * <p>声明 Spring Security 安全过滤链:{@code @Configuration} 标记为配置类,
 * {@code @EnableWebSecurity} 启用 Web 安全。当前策略为放行所有请求
 * (permitAll)并关闭 CSRF,本服务的接口鉴权由业务代码通过
 * {@link com.web3.common.security.AuthUtils} 手动校验 JWT 完成。</p>
 */
@Configuration
@EnableWebSecurity
public class PublicSecurityConfig {
    /**
     * 构建安全过滤链:所有请求放行、关闭 CSRF 防护。
     *
     * @param httpSecurity Spring Security 配置构建器
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> ((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)authorizationManagerRequestMatcherRegistry.anyRequest()).permitAll()).csrf(csrfConfigurer -> csrfConfigurer.disable());
        return (SecurityFilterChain)httpSecurity.build();
    }
}

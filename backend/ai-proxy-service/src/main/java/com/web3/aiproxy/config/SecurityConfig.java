package com.web3.aiproxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 类名：SecurityConfig
 * 所属模块：ai-proxy-service（中转站）
 * 职责：Servlet 安全过滤链 —— 放行所有请求并关闭 CSRF；
 *       /admin/ai/** 的管理员鉴权由 Controller 通过 AuthUtils.requireAdmin 完成，
 *       /v1/** 的令牌鉴权由中转引擎内部完成（OpenAI 兼容错误格式）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

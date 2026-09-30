package com.web3.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 类名：SecurityConfig
 * 所属模块：admin-service（管理后台服务）
 * 职责：管理后台服务的安全过滤链配置。
 * 关键注解：{@code @Configuration} 配置类；{@code @EnableWebSecurity} 启用 Spring Security（Servlet 体系）。
 * 说明：关闭 CSRF 并放行所有请求；管理接口的实际管理员鉴权由 Controller 通过 {@code AuthUtils.requireAdmin} 完成。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /**
     * 构建安全过滤链：所有请求放行并禁用 CSRF。
     *
     * @param http HttpSecurity 构建器
     * @return 配置完成的安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

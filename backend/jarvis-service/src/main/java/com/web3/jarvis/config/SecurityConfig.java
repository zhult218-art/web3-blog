package com.web3.jarvis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Jarvis 服务安全配置(jarvis-service 模块)。
 *
 * <p>声明 Spring Security 安全过滤链:{@code @Configuration} 标记为配置类,
 * {@code @EnableWebSecurity} 启用 Web 安全。当前策略为放行所有请求
 * (permitAll)并关闭 CSRF,接口的登录/管理员鉴权由业务代码通过
 * {@link com.web3.common.security.AuthUtils} 手动校验 JWT 完成。</p>
 *
 * <p>关键注解作用：
 * <ul>
 *   <li>@Configuration：声明为 Spring 配置类</li>
 *   <li>@EnableWebSecurity：启用 Spring Security Web 安全配置</li>
 *   <li>@Bean：注册安全过滤器链 Bean</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /**
     * 构建安全过滤链:关闭 CSRF、所有请求放行。
     *
     * @param http Spring Security 配置构建器
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> ((AuthorizeHttpRequestsConfigurer.AuthorizedUrl) auth.anyRequest()).permitAll());
        return http.build();
    }
}

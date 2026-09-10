/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 *  org.springframework.security.crypto.password.PasswordEncoder
 */
package com.web3.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 类名：SecurityConfig
 * 所属模块：user-service（用户服务）
 * 职责：用户服务安全相关 Bean 配置。
 * 关键注解：@Configuration 声明为 Spring 配置类。
 * 说明：提供 BCrypt 密码编码器，用于注册时密码加密与登录时密码校验。
 */
@Configuration
public class SecurityConfig {
    /**
     * 注册 BCrypt 密码编码器 Bean（密码存储/校验统一使用 BCrypt 哈希）。
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

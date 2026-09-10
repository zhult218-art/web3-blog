/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.context.properties.ConfigurationProperties
 *  org.springframework.stereotype.Component
 */
package com.web3.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 类名：JwtProperties
 * 所属模块：common-security（通用安全模块）
 * 职责：JWT 配置属性类，绑定 application.yml 中以 jwt 开头的配置项（密钥、有效期、请求头等）。
 * 关键注解：@Component 注册为 Bean；@ConfigurationProperties(prefix = "jwt") 自动映射配置前缀为 jwt 的属性。
 * 默认值：密钥默认取环境变量 JWT_SECRET；expire=120 分钟，refresh=43200 分钟；请求头 Authorization，令牌前缀 Bearer。
 */
@Component
@ConfigurationProperties(prefix="jwt")
public class JwtProperties {
    private String secret = System.getenv().getOrDefault("JWT_SECRET", "web3-portal-secret-key-change-me");
    private long expire = 120L;
    private long refresh = 43200L;
    private String header = "Authorization";
    private String prefix = "Bearer ";

    public String getSecret() {
        return this.secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpire() {
        return this.expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public long getRefresh() {
        return this.refresh;
    }

    public void setRefresh(long refresh) {
        this.refresh = refresh;
    }

    public String getHeader() {
        return this.header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}

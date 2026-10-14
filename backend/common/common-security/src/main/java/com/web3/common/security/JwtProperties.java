package com.web3.common.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 类名：JwtProperties
 * 所属模块：common-security（通用安全模块）
 * 职责：JWT 配置属性类，绑定 application.yml 中以 jwt 开头的配置项（密钥、有效期、请求头等）。
 * 关键注解：@Component 注册为 Bean；@ConfigurationProperties(prefix = "jwt") 自动映射配置前缀为 jwt 的属性。
 * 默认值：expire=120 分钟，refresh=43200 分钟；请求头 Authorization，令牌前缀 Bearer。
 * 安全：密钥必须通过 JWT_SECRET 环境变量或 jwt.secret 配置项注入；缺失时启动失败（fail-fast），
 *       不再提供任何兜底默认值，避免公开在 git 中的弱密钥导致 token 可被伪造。
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long expire = 120L;
    private long refresh = 43200L;
    private String header = "Authorization";
    private String prefix = "Bearer ";

    /**
     * 启动时校验密钥已注入：缺失则 fail-fast，避免使用空密钥签发 token。
     * 优先级：环境变量 JWT_SECRET > application.yml 中 jwt.secret 配置。
     */
    @PostConstruct
    void validateSecret() {
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && !envSecret.isBlank()) {
            this.secret = envSecret;
        }
        if (this.secret == null || this.secret.isBlank()) {
            throw new IllegalStateException(
                "JWT secret is not configured. Set the JWT_SECRET environment variable or jwt.secret in application.yml. "
                + "The default fallback was removed for security; a missing secret must fail-fast."
            );
        }
        String normalizedSecret = this.secret.toLowerCase(java.util.Locale.ROOT);
        if (normalizedSecret.contains("replace-with")
                || normalizedSecret.contains("change-me")
                || normalizedSecret.startsWith("your-")
                || normalizedSecret.startsWith("example-")) {
            throw new IllegalStateException(
                "JWT_SECRET must be a private, randomly generated value; placeholder values are not allowed."
            );
        }
        if (this.secret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least 32 characters long for HS256 security."
            );
        }
    }

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

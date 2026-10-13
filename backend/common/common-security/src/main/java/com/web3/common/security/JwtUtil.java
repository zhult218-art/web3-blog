package com.web3.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * 类名：JwtUtil
 * 所属模块：common-security（通用安全模块）
 * 职责：JWT 令牌的生成与校验工具，负责签发携带用户名、用户 ID、角色等声明的令牌，并解析/验证令牌有效性。
 * 关键注解：@Component 注册为 Spring Bean，供各微服务注入使用。
 */
@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expireMillis;

    /**
     * 构造函数：根据配置初始化 HMAC 签名密钥与令牌有效期（expire 分钟转毫秒）。
     *
     * @param properties JWT 配置（密钥、有效期）
     */
    public JwtUtil(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        // 令牌有效期取 jwt.refresh（默认 7 天），与 Redis 会话 TTL、Cookie maxAge 三者保持一致：
        // 这样刷新页面后浏览器仍持有有效 Cookie → /user/refresh 静默续期成功，登录态可持续整个周期。
        // jwt.expire（2h）保留为配置项备用；个人本机/内网站以可用性优先，access 与 refresh 复用同一令牌。
        this.expireMillis = properties.getRefresh() * 60 * 1000L;
    }

    /**
     * 生成普通用户（USER 角色）的 JWT 令牌。
     *
     * @param username 用户名（作为 subject）
     * @param userId   用户 ID
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(String username, Long userId) {
        return generateToken(username, userId, "USER");
    }

    /**
     * 生成指定角色的 JWT 令牌，声明中包含 username、userId 与 authorities（"ROLE_角色名"）。
     *
     * @param username 用户名（作为 subject）
     * @param userId   用户 ID
     * @param role     角色名，如 "USER" / "ADMIN"
     * @return 携带角色声明的签名 JWT 字符串
     */
    public String generateToken(String username, Long userId, String role) {
        return generateToken(username, userId, role, null, null);
    }

    /**
     * 生成带昵称的 JWT 令牌，声明中额外包含 nickname。
     */
    public String generateToken(String username, Long userId, String role, String nickname, String perms) {
        return generateToken(username, userId, role, nickname, perms, null);
    }

    /**
     * 生成带实名认证状态的 JWT 令牌（verified=2 表示已实名认证）。
     * forum-service 的聊天 WebSocket 通过该声明判断用户是否可发消息。
     */
    public String generateToken(String username, Long userId, String role, String nickname, String perms, Integer verified) {
        HashMap<String, Object> claims = new HashMap<String, Object>();
        claims.put("username", username);
        claims.put("userId", userId);
        claims.put("authorities", "ROLE_" + role);
        if (nickname != null) claims.put("nickname", nickname);
        if (perms != null && !perms.isBlank()) claims.put("perms", perms);
        if (verified != null) claims.put("verified", verified);
        return Jwts.builder().subject(username).claims(claims).issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expireMillis))
            .signWith(this.key).compact();
    }

    /**
     * 解析令牌，返回签名验证通过后的 Jws<Claims>（含头部、载荷、签名）。
     *
     * @param token JWT 字符串
     * @return 解析结果对象
     * @throws JwtException 签名不合法或格式错误时抛出
     */
    public Jws<Claims> parseToken(String token) {
        return Jwts.parser().verifyWith(this.key).build().parseSignedClaims(token);
    }

    /**
     * 提取令牌中的 Claims 载荷（subject、userId、authorities 等声明）。
     *
     * @param token JWT 字符串
     * @return 令牌载荷 Claims
     */
    public Claims parseClaims(String token) {
        return this.parseToken(token).getPayload();
    }

    /**
     * 校验令牌是否合法且未过期。
     *
     * @param token JWT 字符串
     * @return true=有效；false=无效或解析异常
     */
    public boolean validate(String token) {
        try {
            this.parseToken(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }
}

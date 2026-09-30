package com.web3.common.security;

/**
 * 类名：JwtConstants
 * 所属模块：common-security（通用安全模块）
 * 职责：JWT 相关的常量定义，统一令牌签发与校验过程中使用的键名与角色前缀，避免各处硬编码字符串。
 * 说明：ROLE_PREFIX=角色前缀；AUTHORITIES_KEY / USER_ID_KEY / USERNAME_KEY=JWT Claims 中的键名；DEFAULT_ROLE=默认角色。
 *
 * <p>2026-09-17 重写（CFR 反编译产物 → 语法清洁化）：
 * 接口字段修饰符 {@code public static final} 在接口中为隐式，去除冗余声明以保持简洁。</p>
 */
public interface JwtConstants {
    String ROLE_PREFIX = "ROLE_";
    String AUTHORITIES_KEY = "authorities";
    String USER_ID_KEY = "userId";
    String USERNAME_KEY = "username";
    String DEFAULT_ROLE = "USER";
}

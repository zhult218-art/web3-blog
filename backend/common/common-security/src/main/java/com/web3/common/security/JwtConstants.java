/*
 * Decompiled with CFR 0.152.
 */
package com.web3.common.security;

/**
 * 类名：JwtConstants
 * 所属模块：common-security（通用安全模块）
 * 职责：JWT 相关的常量定义，统一令牌签发与校验过程中使用的键名与角色前缀，避免各处硬编码字符串。
 * 说明：ROLE_PREFIX=角色前缀；AUTHORITIES_KEY / USER_ID_KEY / USERNAME_KEY=JWT Claims 中的键名；DEFAULT_ROLE=默认角色。
 */
public interface JwtConstants {
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String AUTHORITIES_KEY = "authorities";
    public static final String USER_ID_KEY = "userId";
    public static final String USERNAME_KEY = "username";
    public static final String DEFAULT_ROLE = "USER";
}

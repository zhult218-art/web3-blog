package com.web3.common.security;

import com.web3.common.core.UnauthorizedException;
import io.jsonwebtoken.Claims;

/**
 * 类名：AuthUtils
 * 所属模块：common-security（通用安全模块）
 * 职责：认证/鉴权工具类，提供从 Authorization 请求头解析 JWT、校验登录状态、校验管理员权限的静态方法，供各业务服务 Controller 直接调用。
 */
public final class AuthUtils {

  private AuthUtils() {
  }

  /**
   * 从 Authorization 请求头中解析出 JWT 令牌字符串（去除 "Bearer " 前缀）。
   *
   * @param authHeader Authorization 请求头原文，可为 null
   * @param properties JWT 配置（含令牌前缀）
   * @return 去除前缀后的 JWT 字符串
   * @throws UnauthorizedException 未携带或令牌为空时抛出 401
   */
  public static String resolveToken(String authHeader, JwtProperties properties) {
    if (authHeader == null || authHeader.isBlank()) {
      throw new UnauthorizedException(401, "未登录");
    }
    String prefix = properties.getPrefix();
    String token = prefix != null && authHeader.startsWith(prefix)
        ? authHeader.substring(prefix.length())
        : authHeader;
    if (token.isBlank()) {
      throw new UnauthorizedException(401, "未登录");
    }
    return token;
  }

  /**
   * 校验登录态并返回当前登录用户 ID（解析 JWT 中的 userId 声明）。
   *
   * @param authHeader Authorization 请求头原文
   * @param jwtUtil    JWT 工具类
   * @param properties JWT 配置
   * @return 当前登录用户 ID
   * @throws UnauthorizedException 未登录或令牌失效/过期时抛出 401
   */
  public static Long requireUserId(String authHeader, JwtUtil jwtUtil, JwtProperties properties) {
    String token = resolveToken(authHeader, properties);
    if (!jwtUtil.validate(token)) {
      throw new UnauthorizedException(401, "登录已过期");
    }
    Claims claims = jwtUtil.parseClaims(token);
    Long userId = claims.get("userId", Long.class);
    if (userId == null) {
      throw new UnauthorizedException(401, "登录已过期");
    }
    return userId;
  }

  /**
   * 校验令牌并检查当前用户是否拥有指定角色。
   *
   * @param authHeader Authorization 请求头原文
   * @param jwtUtil    JWT 工具类
   * @param properties JWT 配置
   * @param role       期望的角色名（如 "ADMIN"）
   * @return 令牌中的 Subject（用户名）
   * @throws UnauthorizedException 未登录抛 401，角色不匹配抛 403
   */
  public static String requireRole(String authHeader, JwtUtil jwtUtil, JwtProperties properties, String role) {
    String token = resolveToken(authHeader, properties);
    if (!jwtUtil.validate(token)) {
      throw new UnauthorizedException(401, "登录已过期");
    }
    Claims claims = jwtUtil.parseClaims(token);
    String authorities = claims.get("authorities", String.class);
    if (authorities == null || !authorities.equals("ROLE_" + role)) {
      throw new UnauthorizedException(403, "无权限");
    }
    return claims.getSubject();
  }

  /**
   * 校验当前用户是否为管理员（ADMIN 角色），用于各类管理接口的鉴权入口。
   *
   * @param authHeader Authorization 请求头原文
   * @param jwtUtil    JWT 工具类
   * @param properties JWT 配置
   * @return 管理员用户名
   * @throws UnauthorizedException 非管理员或无登录态时抛出
   */
  public static String requireAdmin(String authHeader, JwtUtil jwtUtil, JwtProperties properties) {
    return requireRole(authHeader, jwtUtil, properties, "ADMIN");
  }

  /**
   * 非抛异常地判断当前请求是否来自 ADMIN 角色用户。
   *
   * <p>与 {@link #requireAdmin} 不同，本方法不会抛出异常：未登录、令牌失效或非
   * 管理员时一律返回 {@code false}，供"先取 userId 再按角色决定分支"的场景使用
   * （例如公共策略仅管理员可操作，普通用户应得到明确的 403 而非被误判为"未登录"）。</p>
   *
   * @param authHeader Authorization 请求头原文，可为 null
   * @param jwtUtil    JWT 工具类
   * @param properties JWT 配置
   * @return 当前用户为 ADMIN 时返回 true，否则返回 false
   */
  public static boolean isAdmin(String authHeader, JwtUtil jwtUtil, JwtProperties properties) {
    if (authHeader == null || authHeader.isBlank()) {
      return false;
    }
    try {
      String token = resolveToken(authHeader, properties);
      if (!jwtUtil.validate(token)) {
        return false;
      }
      Claims claims = jwtUtil.parseClaims(token);
      return "ROLE_ADMIN".equals(claims.get("authorities", String.class));
    } catch (Exception ex) {
      return false;
    }
  }

  /**
   * 细粒度权限校验（与前端 utils/permissions.js 的负向权限语义保持一致）：
   * <ol>
   *   <li>ADMIN 角色恒通过；</li>
   *   <li>令牌 perms 声明包含 "-perm"（被管理员收回）→ 403；</li>
   *   <li>包含 "perm"（显式授权）→ 通过；</li>
   *   <li>两者都没有 → 按 defaultAllow 判定：互动类权限（评论/发帖/聊天）默认
   *       true（注册即拥有），上传类权限默认 false（需管理员显式授权）；
   *       旧令牌无 perms 声明时同样走此兜底，保证向后兼容。</li>
   * </ol>
   *
   * @param authHeader Authorization 请求头原文
   * @param jwtUtil    JWT 工具类
   * @param properties JWT 配置
   * @param perm       权限码（如 comment / post / chat / upload:photo）
   * @param defaultAllow 无显式授权/收回记录时的兜底策略
   * @return 当前用户名（subject），供业务记录操作人
   * @throws UnauthorizedException 未登录抛 401，权限不足抛 403
   */
  public static String requirePerm(String authHeader, JwtUtil jwtUtil, JwtProperties properties, String perm, boolean defaultAllow) {
    String token = resolveToken(authHeader, properties);
    if (!jwtUtil.validate(token)) {
      throw new UnauthorizedException(401, "登录已过期");
    }
    Claims claims = jwtUtil.parseClaims(token);
    String authorities = claims.get("authorities", String.class);
    if ("ROLE_ADMIN".equals(authorities)) {
      return claims.getSubject();
    }
    String perms = claims.get("perms", String.class);
    if (perms != null && !perms.isBlank()) {
      for (String p : perms.split(",")) {
        String code = p.trim();
        if (code.equals("-" + perm)) {
          throw new UnauthorizedException(403, "该操作已被管理员限制");
        }
        if (code.equals(perm)) {
          return claims.getSubject();
        }
      }
    }
    if (!defaultAllow) {
      throw new UnauthorizedException(403, "无权限：请联系管理员开通");
    }
    return claims.getSubject();
  }
}

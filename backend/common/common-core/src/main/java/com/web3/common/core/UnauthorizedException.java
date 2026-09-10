package com.web3.common.core;

/**
 * 类名：UnauthorizedException
 * 所属模块：common-core（通用核心模块）
 * 职责：自定义未授权/鉴权失败异常，在需要登录或权限校验的业务中抛出，由 GlobalExceptionHandler 统一转换为 HTTP 401/403 响应。
 */
public class UnauthorizedException extends RuntimeException {

  private final int code;

  /**
   * 构造异常，默认状态码 401（未登录/登录过期）。
   *
   * @param message 错误提示信息
   */
  public UnauthorizedException(String message) {
    this(401, message);
  }

  /**
   * 构造异常，指定业务状态码（如 401 未登录、403 无权限）。
   *
   * @param code    业务状态码
   * @param message 错误提示信息
   */
  public UnauthorizedException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}

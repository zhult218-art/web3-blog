package com.web3.common.core;

/**
 * 类名：BusinessException
 * 所属模块：common-core（通用核心模块）
 * 职责：业务规则异常（库存不足、状态冲突、参数不满足业务约束等），
 *       由 GlobalExceptionHandler 统一转换为 HTTP 400 + 真实错误消息返回前端，
 *       避免被兜底 RuntimeException 处理器吞成 500 "服务器内部错误"。
 */
public class BusinessException extends RuntimeException {

  private final int code;

  /**
   * 构造异常，默认业务码 400。
   *
   * @param message 面向用户的真实错误提示（如 "库存不足"）
   */
  public BusinessException(String message) {
    this(400, message);
  }

  /**
   * 构造异常，指定业务状态码。
   *
   * @param code    业务状态码（如 404 资源不存在、409 状态冲突）
   * @param message 错误提示信息
   */
  public BusinessException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}

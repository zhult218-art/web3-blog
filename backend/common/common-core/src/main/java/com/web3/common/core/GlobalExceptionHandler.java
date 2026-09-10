package com.web3.common.core;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类名：GlobalExceptionHandler
 * 所属模块：common-core（通用核心模块）
 * 职责：全局异常处理器，统一捕获各微服务 Controller 层抛出的异常并转换为 ApiResponse 返回。
 * 关键注解：@RestControllerAdvice 声明为全局 Controller 增强，自动拦截所有 @RestController 抛出的异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /* 处理未授权异常（未登录/登录过期），返回 HTTP 401 */
  @ExceptionHandler(value = {UnauthorizedException.class})
  @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
  public ApiResponse<Object> handleUnauthorized(UnauthorizedException ex) {
    return ApiResponse.fail(ex.getCode(), ex.getMessage());
  }

  /* 处理业务规则异常，返回真实错误消息（不打日志堆栈），HTTP 400 */
  @ExceptionHandler(value = {BusinessException.class})
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ApiResponse<Object> handleBusiness(BusinessException ex) {
    return ApiResponse.fail(ex.getCode(), ex.getMessage());
  }

  /* 处理 @Valid 参数校验失败异常，返回字段名 + 校验错误信息，HTTP 400 */
  @ExceptionHandler(value = {MethodArgumentNotValidException.class})
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ApiResponse<Object> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .findFirst()
        .orElse("参数验证失败");
    return ApiResponse.fail(400, msg);
  }

  /* 处理请求缺少必填参数异常，HTTP 400 */
  @ExceptionHandler(value = {MissingServletRequestParameterException.class})
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ApiResponse<Object> handleMissingParam(MissingServletRequestParameterException ex) {
    return ApiResponse.fail(400, "缺少参数: " + ex.getParameterName());
  }

  /* 处理请求缺少必填请求头异常：缺少鉴权头 Authorization 视为未登录（HTTP 401），其余视为 400 */
  @ExceptionHandler(value = {MissingRequestHeaderException.class})
  public ResponseEntity<ApiResponse<Object>> handleMissingHeader(MissingRequestHeaderException ex) {
    if ("Authorization".equalsIgnoreCase(ex.getHeaderName())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.fail(401, "未登录"));
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.fail(400, "缺少请求头: " + ex.getHeaderName()));
  }

  /* 处理参数类型转换失败异常，HTTP 400 */
  @ExceptionHandler(value = {MethodArgumentTypeMismatchException.class})
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ApiResponse<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return ApiResponse.fail(400, "参数类型错误: " + ex.getName());
  }

  /* 处理业务抛出的 IllegalArgumentException（参数不合法），HTTP 400 */
  @ExceptionHandler(value = {IllegalArgumentException.class})
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ApiResponse<Object> handleIllegalArg(IllegalArgumentException ex) {
    return ApiResponse.fail(400, "参数不合法");
  }

  /* 处理数据库唯一键冲突异常（如用户名重复），HTTP 409 */
  @ExceptionHandler(value = {DuplicateKeyException.class})
  @ResponseStatus(value = HttpStatus.CONFLICT)
  public ApiResponse<Object> handleDuplicateKey(DuplicateKeyException ex) {
    return ApiResponse.fail(409, "数据已存在");
  }

  /* 兜底处理运行时异常，记录日志并返回 HTTP 500 */
  @ExceptionHandler(value = {RuntimeException.class})
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Object> handleRuntime(RuntimeException ex) {
    log.error("RuntimeException caught", ex);
    return ApiResponse.fail(500, "服务器内部错误");
  }

  /* 最终兜底处理所有其他异常，记录日志并返回 HTTP 500 */
  @ExceptionHandler(value = {Exception.class})
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Object> handle(Exception ex) {
    log.error("Exception caught", ex);
    return ApiResponse.fail(500, "系统异常，请稍后重试");
  }
}

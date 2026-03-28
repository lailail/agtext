package com.agtext.common.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：拦截所有 Controller 抛出的异常，并将其转换为标准的 ApiErrorResponse 响应。
 * 核心目的：隐藏系统内部堆栈细节，提供一致的业务错误码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * 拦截 404 资源未找到异常：
   * 通常由业务逻辑中主动抛出的 NotFoundException 触发。
   */
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(
          NotFoundException e, HttpServletRequest req) {
    // 记录警告日志，包含请求方法、路径及业务错误码
    log.warn("404 {} {} code={}", req.getMethod(), req.getRequestURI(), e.code());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                    new ApiErrorResponse(
                            new ApiErrorResponse.ApiError(e.code(), e.getMessage(), Map.of())));
  }

  /**
   * 拦截 400 非法参数异常：
   * 处理业务代码中 Assert 或手动抛出的 IllegalArgumentException。
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleBadRequest(
          IllegalArgumentException e, HttpServletRequest req) {
    log.warn("400 {} {} error={}", req.getMethod(), req.getRequestURI(), e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                    new ApiErrorResponse(
                            new ApiErrorResponse.ApiError("BAD_REQUEST", safeMessage(e), Map.of())));
  }

  /**
   * 拦截 400 参数校验异常：
   * 处理 @Valid 或 @Validated 触发的字段校验失败，并将具体字段错误封装进 details。
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
          MethodArgumentNotValidException e, HttpServletRequest req) {
    Map<String, Object> details = new LinkedHashMap<>();
    // 遍历所有字段错误，构造键值对（字段名 -> 错误信息）
    for (FieldError fe : e.getBindingResult().getFieldErrors()) {
      details.put(fe.getField(), fe.getDefaultMessage());
    }
    log.warn(
            "400 {} {} validationErrors={}", req.getMethod(), req.getRequestURI(), details.keySet());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                    new ApiErrorResponse(
                            new ApiErrorResponse.ApiError("VALIDATION_ERROR", "Validation failed", details)));
  }

  /**
   * 拦截 400 数据读取异常：
   * 处理 JSON 格式错误或媒体类型不匹配导致无法解析请求体的情况。
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadable(
          HttpMessageNotReadableException e, HttpServletRequest req) {
    log.warn("400 {} {} unreadableBody", req.getMethod(), req.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                    new ApiErrorResponse(
                            new ApiErrorResponse.ApiError("BAD_REQUEST", "Malformed JSON", Map.of())));
  }

  /**
   * 拦截 409 数据冲突异常：
   * 处理数据库唯一索引冲突、外键约束等引起的 DataIntegrityViolationException。
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConflict(
          DataIntegrityViolationException e, HttpServletRequest req) {
    log.warn("409 {} {} conflict", req.getMethod(), req.getRequestURI());
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                    new ApiErrorResponse(
                            new ApiErrorResponse.ApiError("CONFLICT", "Data conflict", Map.of())));
  }

  /**
   * 兜底异常处理（500 内部错误）：
   * 捕获所有未被特定处理器涵盖的运行时异常，防止泄露底层实现细节。
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnknown(Exception e, HttpServletRequest req) {
    // 记录 ERROR 级别日志并打印完整堆栈，以便排查线上问题
    log.error("500 {} {} unexpected", req.getMethod(), req.getRequestURI(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                    new ApiErrorResponse(
                            new ApiErrorResponse.ApiError("INTERNAL_ERROR", "Internal error", Map.of())));
  }

  /**
   * 异常消息脱敏与截断：
   * 1. 确保消息不为空（为空则返回类名）。
   * 2. 限制消息长度（最大 500 字符），防止过大的异常详情影响网络传输或前端渲染。
   */
  private static String safeMessage(Exception e) {
    String msg = e.getMessage();
    if (msg == null || msg.isBlank()) {
      return e.getClass().getSimpleName();
    }
    if (msg.length() > 500) {
      return msg.substring(0, 500);
    }
    return msg;
  }
}
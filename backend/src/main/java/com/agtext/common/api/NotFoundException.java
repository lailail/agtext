package com.agtext.common.api;

/**
 * 自定义资源未找到异常：用于业务层（Service）明确标识某个实体（如会话、消息、文档）不存在。
 * 该异常会被 GlobalExceptionHandler 捕获并映射为 HTTP 404 状态码。
 */
public class NotFoundException extends RuntimeException {

  // 具体的业务错误码（如 "CONVERSATION_NOT_FOUND"），用于前端多语言处理或逻辑分支判断
  private final String code;

  /**
   * 构造函数
   * @param code 业务错误码
   * @param message 错误描述信息，通常用于日志记录
   */
  public NotFoundException(String code, String message) {
    super(message);
    this.code = code;
  }

  /**
   * 获取业务错误码
   */
  public String code() {
    return code;
  }
}
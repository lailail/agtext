package com.agtext.common.api;

import java.util.Map;

/**
 * 统一 API 错误响应包装类：用于向客户端返回标准化的错误信息。
 */
public record ApiErrorResponse(ApiError error) {

  /**
   * 错误详情模型：
   * @param code 业务错误码（如 "CONVERSATION_NOT_FOUND"），用于前端逻辑判断与国际化处理
   * @param message 错误描述信息，通常用于开发调试或直接展示给用户的提示文本
   * @param details 扩展信息映射表（如参数校验失败的字段列表、异常堆栈提示等），提供额外的上下文
   */
  public record ApiError(String code, String message, Map<String, Object> details) {}
}
package com.agtext.common.api;

import java.util.Map;

/**
 * 统一错误响应实体：用于在 API 层返回标准化的错误信息。
 * 采用嵌套 Record 结构以符合 RESTful API 的主流错误表示规范（如 { "error": { ... } }）。
 */
public record ErrorResponse(Error error) {

  /**
   * 错误详情模型：
   * @param code 业务错误码，用于前端逻辑判断与国际化处理。
   * @param message 错误描述信息，通常用于开发调试或直接展示给用户的提示文本。
   * @param details 扩展信息映射表，可包含字段校验失败详情、异常堆栈片段等元数据。
   */
  public record Error(String code, String message, Map<String, Object> details) {}

  /**
   * 静态工厂方法：快速创建一个不带扩展详情的标准错误响应。
   * @param code 业务错误码。
   * @param message 错误提示信息。
   * @return 初始化后的 ErrorResponse 实例。
   */
  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(new Error(code, message, Map.of()));
  }
}
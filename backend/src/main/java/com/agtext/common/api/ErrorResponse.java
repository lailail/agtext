package com.agtext.common.api;

import java.util.Map;

public record ErrorResponse(Error error) {
  public record Error(String code, String message, Map<String, Object> details) {}

  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(new Error(code, message, Map.of()));
  }
}

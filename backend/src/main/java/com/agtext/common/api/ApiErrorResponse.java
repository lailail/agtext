package com.agtext.common.api;

import java.util.Map;

public record ApiErrorResponse(ApiError error) {
  public record ApiError(String code, String message, Map<String, Object> details) {}
}

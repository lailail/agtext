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

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(
      NotFoundException e, HttpServletRequest req) {
    log.warn("404 {} {} code={}", req.getMethod(), req.getRequestURI(), e.code());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            new ApiErrorResponse(
                new ApiErrorResponse.ApiError(e.code(), e.getMessage(), Map.of())));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleBadRequest(
      IllegalArgumentException e, HttpServletRequest req) {
    log.warn("400 {} {} error={}", req.getMethod(), req.getRequestURI(), e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ApiErrorResponse(
                new ApiErrorResponse.ApiError("BAD_REQUEST", safeMessage(e), Map.of())));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException e, HttpServletRequest req) {
    Map<String, Object> details = new LinkedHashMap<>();
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

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadable(
      HttpMessageNotReadableException e, HttpServletRequest req) {
    log.warn("400 {} {} unreadableBody", req.getMethod(), req.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ApiErrorResponse(
                new ApiErrorResponse.ApiError("BAD_REQUEST", "Malformed JSON", Map.of())));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConflict(
      DataIntegrityViolationException e, HttpServletRequest req) {
    log.warn("409 {} {} conflict", req.getMethod(), req.getRequestURI());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            new ApiErrorResponse(
                new ApiErrorResponse.ApiError("CONFLICT", "Data conflict", Map.of())));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnknown(Exception e, HttpServletRequest req) {
    log.error("500 {} {} unexpected", req.getMethod(), req.getRequestURI(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ApiErrorResponse(
                new ApiErrorResponse.ApiError("INTERNAL_ERROR", "Internal error", Map.of())));
  }

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

package com.agtext.tool.config;

import com.agtext.common.ids.IdCodec;
import com.agtext.tool.domain.ConfirmationItem;
import com.agtext.tool.service.ConfirmationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 写入保护拦截器
 * 用于在 Agent（如 AI 智能体）执行写操作前，强制校验是否经过了人工或系统预设的二次确认
 */
public class WriteGuardInterceptor implements HandlerInterceptor {
  private static final String HEADER_ACTOR = "X-Actor";
  private static final String HEADER_CONFIRMATION_ID = "X-Confirmation-Id";
  private static final String ACTOR_AGENT = "agent";
  private static final String CONF_PREFIX = "cnf_";

  private final ConfirmationService confirmations;

  public WriteGuardInterceptor(ConfirmationService confirmations) {
    this.confirmations = confirmations;
  }

  /**
   * 前置处理逻辑：在 Controller 方法执行前拦截并进行权限与确认单校验
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
          throws Exception {

    // 1. 仅拦截写操作（POST, PUT, PATCH, DELETE），读操作（GET）直接放行
    String method = request.getMethod();
    if (!isWriteMethod(method)) {
      return true;
    }

    // 2. 排除确认单自身的接口路径，防止死循环（创建、审批确认单本身不需要 X-Confirmation-Id）
    String path = request.getRequestURI();
    if (path != null && path.startsWith("/api/confirmations")) {
      return true;
    }

    // 3. 校验调用者身份：只有当 Actor 为 "agent" 时才启用强制确认机制
    String actor = request.getHeader(HEADER_ACTOR);
    if (!ACTOR_AGENT.equalsIgnoreCase(actor)) {
      return true;
    }

    // 4. Agent 调用必须携带 X-Confirmation-Id 请求头
    String confirmationId = request.getHeader(HEADER_CONFIRMATION_ID);
    if (confirmationId == null || confirmationId.isBlank()) {
      throw new IllegalArgumentException("Missing header: " + HEADER_CONFIRMATION_ID);
    }

    // 5. 解码 ID 并校验确认单状态
    long rawId = IdCodec.decode(CONF_PREFIX, confirmationId);
    ConfirmationItem item = confirmations.get(rawId);

    // 6. 核心准入逻辑：确认单必须处于 "approved" 状态才允许执行后续 Controller 逻辑
    if (!"approved".equalsIgnoreCase(item.status())) {
      throw new IllegalArgumentException("Confirmation not approved: " + confirmationId);
    }

    return true;
  }

  /**
   * 判断是否为具有副作用的写入方法
   */
  private static boolean isWriteMethod(String method) {
    return "POST".equalsIgnoreCase(method)
            || "PUT".equalsIgnoreCase(method)
            || "PATCH".equalsIgnoreCase(method)
            || "DELETE".equalsIgnoreCase(method);
  }
}
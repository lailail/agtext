package com.agtext.tool.config;

import com.agtext.common.ids.IdCodec;
import com.agtext.tool.domain.ConfirmationItem;
import com.agtext.tool.service.ConfirmationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class WriteGuardInterceptor implements HandlerInterceptor {
  private static final String HEADER_ACTOR = "X-Actor";
  private static final String HEADER_CONFIRMATION_ID = "X-Confirmation-Id";
  private static final String ACTOR_AGENT = "agent";
  private static final String CONF_PREFIX = "cnf_";

  private final ConfirmationService confirmations;

  public WriteGuardInterceptor(ConfirmationService confirmations) {
    this.confirmations = confirmations;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String method = request.getMethod();
    if (!isWriteMethod(method)) {
      return true;
    }

    String path = request.getRequestURI();
    if (path != null && path.startsWith("/api/confirmations")) {
      return true;
    }

    String actor = request.getHeader(HEADER_ACTOR);
    if (!ACTOR_AGENT.equalsIgnoreCase(actor)) {
      return true;
    }

    String confirmationId = request.getHeader(HEADER_CONFIRMATION_ID);
    if (confirmationId == null || confirmationId.isBlank()) {
      throw new IllegalArgumentException("Missing header: " + HEADER_CONFIRMATION_ID);
    }

    long rawId = IdCodec.decode(CONF_PREFIX, confirmationId);
    ConfirmationItem item = confirmations.get(rawId);
    if (!"approved".equalsIgnoreCase(item.status())) {
      throw new IllegalArgumentException("Confirmation not approved: " + confirmationId);
    }
    return true;
  }

  private static boolean isWriteMethod(String method) {
    return "POST".equalsIgnoreCase(method)
        || "PUT".equalsIgnoreCase(method)
        || "PATCH".equalsIgnoreCase(method)
        || "DELETE".equalsIgnoreCase(method);
  }
}

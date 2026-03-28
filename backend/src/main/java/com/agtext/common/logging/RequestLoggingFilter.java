package com.agtext.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long start = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long ms = System.currentTimeMillis() - start;
      int status = response.getStatus();
      String path = request.getRequestURI();
      String method = request.getMethod();
      if (status >= 500) {
        log.error("request {} {} status={} durationMs={}", method, path, status, ms);
      } else if (status >= 400) {
        log.warn("request {} {} status={} durationMs={}", method, path, status, ms);
      } else {
        log.info("request {} {} status={} durationMs={}", method, path, status, ms);
      }
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path != null && path.startsWith("/actuator");
  }
}

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

/**
 * 请求日志过滤器：记录每个 HTTP 请求的执行细节，包括方法、路径、状态码及耗时。
 * 核心目的：提供生产环境的透明度，辅助排查性能瓶颈及接口异常。
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

    // 1. 记录请求进入的时间戳
    long start = System.currentTimeMillis();

    try {
      // 2. 执行后续过滤器链及 Controller 业务逻辑
      filterChain.doFilter(request, response);
    } finally {
      // 3. 计算请求总耗时（毫秒）
      long ms = System.currentTimeMillis() - start;
      int status = response.getStatus();
      String path = request.getRequestURI();
      String method = request.getMethod();

      // 4. 根据 HTTP 状态码采用不同的日志级别进行记录
      if (status >= 500) {
        // 服务端错误：记录为 ERROR，通常需要立即关注
        log.error("request {} {} status={} durationMs={}", method, path, status, ms);
      } else if (status >= 400) {
        // 客户端错误（参数错误、鉴权失败等）：记录为 WARN
        log.warn("request {} {} status={} durationMs={}", method, path, status, ms);
      } else {
        // 正常请求（2xx/3xx）：记录为 INFO
        log.info("request {} {} status={} durationMs={}", method, path, status, ms);
      }
    }
  }

  /**
   * 过滤规则：指定哪些路径不需要记录日志。
   * 逻辑：排除监控端点（/actuator/**），因为其高频的健康检查会产生大量冗余日志，掩盖真实的业务日志。
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path != null && path.startsWith("/actuator");
  }
}
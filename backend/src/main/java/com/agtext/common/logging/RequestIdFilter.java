package com.agtext.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求追踪过滤器：为每个进入系统的 HTTP 请求分配唯一的 Request ID。
 * 核心功能：
 * 1. 实现请求的唯一标识，方便在海量日志中检索特定请求的完整生命周期。
 * 2. 利用 SLF4J 的 MDC (Mapped Diagnostic Context) 将 ID 绑定到当前线程。
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {
  // 定义 HTTP 响应头和请求头中使用的追踪 ID 字段名
  public static final String HEADER = "X-Request-Id";
  // 定义在日志配置文件（如 logback.xml）中引用的变量名
  public static final String MDC_KEY = "requestId";

  @Override
  protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

    // 1. 尝试从请求头获取 ID（允许上游系统传递追踪 ID）
    String reqId = request.getHeader(HEADER);

    // 2. 如果上游未提供，则生成一个新的 UUID 作为当前请求的唯一标识
    if (reqId == null || reqId.isBlank()) {
      reqId = UUID.randomUUID().toString();
    }

    // 3. 将 ID 注入 MDC 容器。后续当前线程产生的所有日志都会自动携带此 ID
    MDC.put(MDC_KEY, reqId);

    // 4. 将 ID 回写至响应头，方便前端在遇到异常时提供该 ID 进行问题回溯
    response.setHeader(HEADER, reqId);

    try {
      // 执行后续的过滤器链路及业务逻辑
      filterChain.doFilter(request, response);
    } finally {
      // 5. 关键步骤：请求结束后必须清理 MDC。
      // 原因：Web 容器（如 Tomcat）使用线程池，若不清理，该线程被复用时会携带旧的 requestId，导致日志污染。
      MDC.remove(MDC_KEY);
    }
  }
}
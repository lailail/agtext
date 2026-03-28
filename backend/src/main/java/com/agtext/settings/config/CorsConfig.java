package com.agtext.settings.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域资源共享 (CORS) 配置类：
 * 解决浏览器“同源策略”限制，允许特定的前端域名（如开发环境的 Vite）访问后端接口。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  /**
   * 配置跨域映射规则：
   * 定义哪些路径允许跨域、来自哪些源、支持哪些 HTTP 方法。
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
            // 1. 拦截范围：仅对以 /api/ 开头的接口应用跨域规则，确保管理接口的安全隔离
            .addMapping("/api/**")

            // 2. 允许的源：明确指定前端开发服务器地址（通常是 Vite 的默认端口 5173）
            // 严肃性提示：生产环境应改为实际域名，严禁在生产中使用 "*" 允许所有源
            .allowedOrigins("http://localhost:5173")

            // 3. 允许的方法：涵盖了 RESTful 风格的所有常用动词
            // 包括用于预检请求的 OPTIONS 方法
            .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")

            // 4. 允许的请求头：允许前端发送任何自定义 Header（如 Authorization 令牌）
            .allowedHeaders("*");
  }
}
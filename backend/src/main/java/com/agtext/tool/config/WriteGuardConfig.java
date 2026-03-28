package com.agtext.tool.config;

import com.agtext.tool.service.ConfirmationService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 写入保护（Write Guard）配置类
 * 遵循 Spring MVC 的 WebMvcConfigurer 接口，用于扩展自定义拦截逻辑
 */
@Configuration
public class WriteGuardConfig implements WebMvcConfigurer {
  private final ConfirmationService confirmations;

  /**
   * 构造函数注入
   * 注入 ConfirmationService，供拦截器在执行过程中校验确认单状态
   */
  public WriteGuardConfig(ConfirmationService confirmations) {
    this.confirmations = confirmations;
  }

  /**
   * 注册自定义拦截器
   * 将 WriteGuardInterceptor 加入到 Spring MVC 的拦截器链中
   * 该拦截器通常用于在执行“敏感写入操作”前，拦截请求并检查是否已获得必要的二次确认
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // 实例化拦截器并传入业务服务，确保拦截逻辑具备数据库访问能力
    registry.addInterceptor(new WriteGuardInterceptor(confirmations));
  }
}
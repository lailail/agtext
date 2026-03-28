package com.agtext.tool.platform.config;

import com.agtext.tool.platform.service.ToolSecurityProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具平台基础配置
 * 负责定义工具执行所需的全局资源和安全配置属性
 */
@Configuration
// 启用对 ToolSecurityProperties 类的配置绑定，使其支持从 application.yml/properties 读取 tool.security.* 属性
@EnableConfigurationProperties(ToolSecurityProperties.class)
public class ToolPlatformConfig {

  /**
   * 初始化工具专用的 HttpClient
   * 用于工具执行过程中可能涉及的外部 API 调用（如请求第三方工具接口）
   */
  @Bean
  public HttpClient toolHttpClient() {
    return HttpClient.newBuilder()
            // 设置连接超时时间为 10 秒，防止因外部网络阻塞导致工具执行线程无限期挂起
            .connectTimeout(Duration.ofSeconds(10))
            // 使用默认线程池和 HTTP/2 协议（HttpClient 的默认行为）
            .build();
  }
}
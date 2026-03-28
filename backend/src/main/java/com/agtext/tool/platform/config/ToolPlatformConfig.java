package com.agtext.tool.platform.config;

import com.agtext.tool.platform.service.ToolSecurityProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ToolSecurityProperties.class)
public class ToolPlatformConfig {
  @Bean
  public HttpClient toolHttpClient() {
    return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }
}

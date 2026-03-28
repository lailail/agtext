package com.agtext.tool.config;

import com.agtext.tool.service.ConfirmationService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WriteGuardConfig implements WebMvcConfigurer {
  private final ConfirmationService confirmations;

  public WriteGuardConfig(ConfirmationService confirmations) {
    this.confirmations = confirmations;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new WriteGuardInterceptor(confirmations));
  }
}

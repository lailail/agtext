package com.agtext.tool.platform.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tool.security")
public record ToolSecurityProperties(List<String> domainAllowlist) {
  public ToolSecurityProperties {
    if (domainAllowlist == null) {
      domainAllowlist = List.of();
    }
  }
}

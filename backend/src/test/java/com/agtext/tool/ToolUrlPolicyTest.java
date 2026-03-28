package com.agtext.tool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agtext.tool.platform.service.ToolSecurityProperties;
import com.agtext.tool.platform.service.ToolUrlPolicy;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolUrlPolicyTest {
  @Test
  void shouldAllowAnyDomainWhenAllowlistEmpty() {
    ToolUrlPolicy.enforceDomainAllowlist(
        new ToolSecurityProperties(List.of()), URI.create("https://example.com"));
    ToolUrlPolicy.enforceDomainAllowlist(
        new ToolSecurityProperties(null), URI.create("https://sub.example.com"));
  }

  @Test
  void shouldAllowExactOrSubdomainMatch() {
    ToolSecurityProperties sec = new ToolSecurityProperties(List.of("example.com"));
    ToolUrlPolicy.enforceDomainAllowlist(sec, URI.create("https://example.com"));
    ToolUrlPolicy.enforceDomainAllowlist(sec, URI.create("https://a.example.com"));
  }

  @Test
  void shouldBlockWhenNotInAllowlist() {
    ToolSecurityProperties sec = new ToolSecurityProperties(List.of("example.com"));
    assertThatThrownBy(
            () -> ToolUrlPolicy.enforceDomainAllowlist(sec, URI.create("https://evil.com")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("domain not allowed");
  }
}

package com.agtext.tool.platform.service;

import java.net.URI;
import java.util.Locale;

public final class ToolUrlPolicy {
  private ToolUrlPolicy() {}

  public static void enforceDomainAllowlist(ToolSecurityProperties security, URI uri) {
    if (security == null || uri == null) {
      return;
    }
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      return;
    }
    var allow = security.domainAllowlist();
    if (allow == null || allow.isEmpty()) {
      return; // default open
    }
    String normalized = host.toLowerCase(Locale.ROOT);
    boolean ok =
        allow.stream()
            .filter(s -> s != null && !s.isBlank())
            .map(s -> s.toLowerCase(Locale.ROOT))
            .anyMatch(a -> normalized.equals(a) || normalized.endsWith("." + a));
    if (!ok) {
      throw new IllegalArgumentException("domain not allowed: " + host);
    }
  }
}

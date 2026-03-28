package com.agtext.knowledge.service;

import java.net.URI;

public final class UrlSafety {
  private UrlSafety() {}

  public static URI requireSafeHttpUrl(String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("url is required");
    }
    URI uri = URI.create(url.trim());
    String scheme = uri.getScheme();
    if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
      throw new IllegalArgumentException("Only http/https URLs are allowed");
    }
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("Invalid URL host");
    }
    if (isUnsafeHost(host)) {
      throw new IllegalArgumentException("Unsafe URL host");
    }
    return uri;
  }

  private static boolean isUnsafeHost(String host) {
    String h = host.toLowerCase();
    if (h.equals("localhost") || h.equals("127.0.0.1") || h.equals("0.0.0.0") || h.equals("::1")) {
      return true;
    }
    // block common private IPv4 ranges if host is an IP literal
    if (h.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
      if (h.startsWith("10.")) return true;
      if (h.startsWith("192.168.")) return true;
      if (h.startsWith("127.")) return true;
      if (h.startsWith("0.")) return true;
      if (h.startsWith("169.254.")) return true;
      if (h.startsWith("172.")) {
        String[] parts = h.split("\\.");
        if (parts.length >= 2) {
          int second = Integer.parseInt(parts[1]);
          if (second >= 16 && second <= 31) return true;
        }
      }
    }
    return false;
  }
}

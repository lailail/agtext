package com.agtext.knowledge.service;

import java.net.URI;

/**
 * URL 安全校验工具类：
 * 在抓取网页内容（Web Import）之前，强制校验 URL 的合法性。
 * 核心目的：防止攻击者诱导服务器访问内网敏感服务（SSRF 防御）。
 */
public final class UrlSafety {

  // 私有构造函数，防止工具类被实例化
  private UrlSafety() {}

  /**
   * 校验并返回一个安全的 HTTP/HTTPS URI 对象
   * @param url 输入的原始 URL 字符串
   * @throws IllegalArgumentException 如果 URL 格式错误、协议不支持或指向危险地址
   */
  public static URI requireSafeHttpUrl(String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("url is required");
    }

    // 1. 创建 URI 对象并规范化
    URI uri = URI.create(url.trim());
    String scheme = uri.getScheme();

    // 2. 协议白名单校验：仅允许 http 和 https
    // 防止攻击者使用 file://, ftp://, gopher:// 等协议探测服务器本地文件或执行非法操作
    if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
      throw new IllegalArgumentException("Only http/https URLs are allowed");
    }

    // 3. 域名/主机名提取
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("Invalid URL host");
    }

    // 4. 执行深度安全检查（IP/主机名黑名单）
    if (isUnsafeHost(host)) {
      throw new IllegalArgumentException("Unsafe URL host");
    }

    return uri;
  }

  /**
   * 判断主机名是否不安全：
   * 拦截所有回环地址和内网私有地址段。
   */
  private static boolean isUnsafeHost(String host) {
    String h = host.toLowerCase();

    // A. 拦截常见回环地址名称和 IPv6 本地地址
    if (h.equals("localhost") || h.equals("127.0.0.1") || h.equals("0.0.0.0") || h.equals("::1")) {
      return true;
    }

    // B. 正则匹配：如果 Host 是纯数字 IP 格式（IPv4 Literal）
    if (h.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
      // 拦截 10.0.0.0/8 (私有 A 类)
      if (h.startsWith("10.")) return true;
      // 拦截 192.168.0.0/16 (私有 C 类)
      if (h.startsWith("192.168.")) return true;
      // 拦截 127.0.0.0/8 (回环地址)
      if (h.startsWith("127.")) return true;
      // 拦截 0.0.0.0 (源地址)
      if (h.startsWith("0.")) return true;
      // 拦截 169.254.0.0/16 (链路本地地址/APIPA)
      if (h.startsWith("169.254.")) return true;

      // 拦截 172.16.0.0/12 (私有 B 类: 172.16.x.x 到 172.31.x.x)
      if (h.startsWith("172.")) {
        String[] parts = h.split("\\.");
        if (parts.length >= 2) {
          try {
            int second = Integer.parseInt(parts[1]);
            if (second >= 16 && second <= 31) return true;
          } catch (NumberFormatException e) {
            return true; // 无法解析则保守处理，视为不安全
          }
        }
      }
    }
    return false;
  }
}
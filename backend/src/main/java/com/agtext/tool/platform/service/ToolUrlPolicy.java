package com.agtext.tool.platform.service;

import java.net.URI;
import java.util.Locale;

/**
 * 工具 URL 安全策略
 * 提供静态工具方法，用于在工具执行远程请求前强制执行域名准入校验
 */
public final class ToolUrlPolicy {
  // 私有构造函数，防止工具类被实例化
  private ToolUrlPolicy() {}

  /**
   * 执行域名白名单强制检查
   * * @param security 安全配置属性，包含 domainAllowlist
   * @param uri 待访问的目标 URI
   * @throws IllegalArgumentException 如果目标域名不在允许列表中，抛出此异常阻止请求
   */
  public static void enforceDomainAllowlist(ToolSecurityProperties security, URI uri) {
    // 基础防御检查：如果配置或 URI 为空，视为无法校验（实事求是地说，此处可根据安全级别改为强制报错）
    if (security == null || uri == null) {
      return;
    }

    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      return;
    }

    var allow = security.domainAllowlist();
    // 策略回退逻辑：如果白名单配置为空，则默认放行所有请求（即“默认开放”策略）
    if (allow == null || allow.isEmpty()) {
      return;
    }

    // 标准化处理：统一转为小写，避免大小写绕过（Case-sensitivity bypass）
    String normalized = host.toLowerCase(Locale.ROOT);

    /**
     * 核心校验逻辑：
     * 1. 精确匹配：normalized.equals(a)
     * 2. 子域名匹配：normalized.endsWith("." + a)
     * 例如：配置为 "example.com"，则允许 "example.com" 和 "api.example.com"
     */
    boolean ok =
            allow.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .anyMatch(a -> normalized.equals(a) || normalized.endsWith("." + a));

    // 准入拦截：校验失败直接截断后续执行流
    if (!ok) {
      throw new IllegalArgumentException("domain not allowed: " + host);
    }
  }
}
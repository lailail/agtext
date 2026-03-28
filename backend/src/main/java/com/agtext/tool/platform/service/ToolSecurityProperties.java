package com.agtext.tool.platform.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 工具安全配置属性
 * 映射配置文件中以 'app.tool.security' 为前缀的配置项
 * 主要用于控制工具执行过程中的网络访问边界及敏感操作权限
 */
@ConfigurationProperties(prefix = "app.tool.security")
public record ToolSecurityProperties(
        /**
         * 域名白名单列表
         * 当工具涉及远程 HTTP 调用时，系统会校验目标域名是否在此列表中
         * 以防止 SSRF（服务端请求伪造）攻击或未经授权的数据外传
         */
        List<String> domainAllowlist
) {
  /**
   * 紧凑型构造函数（Compact Constructor）
   * 用于在配置绑定阶段执行参数校验和默认值填充
   */
  public ToolSecurityProperties {
    // 确保列表永不为 null，即使配置文件中未定义该项，也能返回空列表以防止 NullPointerException
    if (domainAllowlist == null) {
      domainAllowlist = List.of();
    }
  }
}
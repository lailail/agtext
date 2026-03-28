package com.agtext.common.time;

import java.time.Instant;

/**
 * UTC 时间工具类：提供统一的系统级时间获取入口。
 * 核心目的：
 * 1. 强制系统使用 UTC (协调世界时) 标准，避免因服务器时区配置不一致导致的数据错乱。
 * 2. 为后续实现“时间切片”或“测试桩（Mocking）”提供统一的收口。
 */
public final class UtcTime {

  /**
   * 私有构造函数：防止工具类被实例化。
   */
  private UtcTime() {}

  /**
   * 获取当前时间的 Instant 实例。
   * Java 的 Instant 类在底层默认采用 UTC 时区，代表时间轴上的一个绝对点。
   * * @return 当前 UTC 时间戳
   */
  public static Instant now() {
    return Instant.now();
  }
}
package com.agtext.common.ids;

/**
 * ID 编解码工具类：用于在内部长整型（Long）ID 与带前缀的外部字符串（String）标识之间进行转换。
 * 核心目的：提高外部标识的可读性，并初步区分不同业务实体的 ID 类型（如会话 cnv_123）。
 */
public final class IdCodec {

  /**
   * 私有构造函数：防止工具类被实例化。
   */
  private IdCodec() {}

  /**
   * 编码逻辑：将业务前缀与长整型 ID 拼接。
   * @param prefix 业务前缀（例如 "cnv_" 代表会话，"msg_" 代表消息）
   * @param id 数据库内部自增 ID
   * @return 拼接后的外部字符串标识
   */
  public static String encode(String prefix, long id) {
    return prefix + id;
  }

  /**
   * 解码逻辑：验证前缀并从字符串中还原长整型 ID。
   * @param prefix 预期的业务前缀
   * @param value 待解析的外部字符串标识
   * @return 还原后的内部长整型 ID
   * @throws IllegalArgumentException 当输入为空、前缀不匹配或后缀非数字时抛出
   */
  public static long decode(String prefix, String value) {
    // 1. 前缀合法性校验
    if (value == null || !value.startsWith(prefix)) {
      throw new IllegalArgumentException("Invalid id: expected prefix " + prefix);
    }
    try {
      // 2. 截取前缀后的部分并解析为 long
      return Long.parseLong(value.substring(prefix.length()));
    } catch (NumberFormatException e) {
      // 3. 格式校验：确保后缀必须为纯数字
      throw new IllegalArgumentException("Invalid id: expected numeric suffix", e);
    }
  }
}
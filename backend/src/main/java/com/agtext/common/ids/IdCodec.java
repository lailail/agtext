package com.agtext.common.ids;

public final class IdCodec {
  private IdCodec() {}

  public static String encode(String prefix, long id) {
    return prefix + id;
  }

  public static long decode(String prefix, String value) {
    if (value == null || !value.startsWith(prefix)) {
      throw new IllegalArgumentException("Invalid id: expected prefix " + prefix);
    }
    try {
      return Long.parseLong(value.substring(prefix.length()));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid id: expected numeric suffix", e);
    }
  }
}

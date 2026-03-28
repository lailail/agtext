package com.agtext.common.time;

import java.time.Instant;

public final class UtcTime {
  private UtcTime() {}

  public static Instant now() {
    return Instant.now();
  }
}

package com.famora.backup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.backup.quota")
public record BackupQuotaProperties(Long defaultBytes) {

  private static final long DEFAULT_QUOTA_BYTES = 10L * 1024 * 1024 * 1024;

  public long effectiveDefaultBytes() {
    return defaultBytes == null || defaultBytes < 1 ? DEFAULT_QUOTA_BYTES : defaultBytes;
  }
}

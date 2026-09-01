package com.famora.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.admin-bootstrap")
public record AdminBootstrapProperties(String token) {

  private static final int MINIMUM_TOKEN_LENGTH = 32;

  public AdminBootstrapProperties {
    token = token == null ? "" : token;
    if (!token.isBlank() && token.length() < MINIMUM_TOKEN_LENGTH) {
      throw new IllegalArgumentException(
          "Admin bootstrap token must contain at least 32 characters");
    }
  }
}

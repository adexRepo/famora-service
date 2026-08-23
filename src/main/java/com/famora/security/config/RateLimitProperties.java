package com.famora.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.rate-limit")
public record RateLimitProperties(
    int windowSeconds,
    int loginAttempts,
    int registrationAttempts,
    int refreshAttempts,
    int invitationJoinAttempts,
    int websocketTicketAttempts) {

  public RateLimitProperties {
    if (windowSeconds < 1 || loginAttempts < 1 || registrationAttempts < 1
        || refreshAttempts < 1 || invitationJoinAttempts < 1 || websocketTicketAttempts < 1) {
      throw new IllegalArgumentException("Rate-limit values must be positive");
    }
  }
}

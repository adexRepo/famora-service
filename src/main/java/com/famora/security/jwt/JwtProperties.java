package com.famora.security.jwt;

import com.famora.security.config.Base64KeyValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
    String issuer,
    String secret,
    long accessTokenExpirationMinutes,
    long refreshTokenExpirationDays
) {

  private static final int MINIMUM_KEY_BYTES = 32;

  public JwtProperties {
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalArgumentException("JWT issuer must be configured");
    }
    byte[] keyBytes = Base64KeyValidator.decode("JWT secret", secret);
    if (keyBytes.length < MINIMUM_KEY_BYTES) {
      throw new IllegalArgumentException("JWT secret must decode to at least 32 bytes");
    }
    if (accessTokenExpirationMinutes <= 0) {
      throw new IllegalArgumentException("JWT access token expiration must be positive");
    }
    if (refreshTokenExpirationDays <= 0) {
      throw new IllegalArgumentException("JWT refresh token expiration must be positive");
    }
  }

  public byte[] decodedSecret() {
    return Base64KeyValidator.decode("JWT secret", secret);
  }
}

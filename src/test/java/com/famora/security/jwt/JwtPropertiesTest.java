package com.famora.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

  @Test
  void acceptsBase64SecretWithAtLeastThirtyTwoDecodedBytes() {
    String testKey = Base64.getEncoder().encodeToString(new byte[32]);

    JwtProperties properties = new JwtProperties("test-issuer", testKey, 30, 30);

    assertThat(properties.decodedSecret()).hasSize(32);
  }

  @Test
  void rejectsMissingSecret() {
    assertThatThrownBy(() -> new JwtProperties("test-issuer", null, 30, 30))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be configured");
  }

  @Test
  void rejectsNonBase64Secret() {
    assertThatThrownBy(() -> new JwtProperties("test-issuer", "not a base64 secret", 30, 30))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Base64");
  }

  @Test
  void rejectsDecodedSecretShorterThanThirtyTwoBytes() {
    String testKey = Base64.getEncoder().encodeToString(new byte[16]);

    assertThatThrownBy(() -> new JwtProperties("test-issuer", testKey, 30, 30))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 32 bytes");
  }
}

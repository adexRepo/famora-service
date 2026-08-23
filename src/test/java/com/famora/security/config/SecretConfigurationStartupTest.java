package com.famora.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.famora.security.jwt.JwtProperties;
import com.famora.vault.config.VaultProperties;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class SecretConfigurationStartupTest {

  private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(SecretPropertiesConfiguration.class)
      .withPropertyValues(
          "app.security.jwt.issuer=test-issuer",
          "app.security.jwt.access-token-expiration-minutes=30",
          "app.security.jwt.refresh-token-expiration-days=30");

  @Test
  void startupFailsWithoutJwtSecret() {
    contextRunner
        .withPropertyValues("app.vault.encryption-key=" + TEST_KEY)
        .run(context -> assertThat(context.getStartupFailure())
            .isNotNull()
            .hasRootCauseMessage("JWT secret must be configured"));
  }

  @Test
  void startupFailsWithoutVaultEncryptionKey() {
    contextRunner
        .withPropertyValues("app.security.jwt.secret=" + TEST_KEY)
        .run(context -> assertThat(context.getStartupFailure())
            .isNotNull()
            .hasRootCauseMessage("Vault encryption key must be configured"));
  }

  @Test
  void startupAcceptsValidJwtAndVaultKeys() {
    contextRunner
        .withPropertyValues(
            "app.security.jwt.secret=" + TEST_KEY,
            "app.vault.encryption-key=" + TEST_KEY)
        .run(context -> assertThat(context.getStartupFailure()).isNull());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({JwtProperties.class, VaultProperties.class})
  static class SecretPropertiesConfiguration {
  }
}

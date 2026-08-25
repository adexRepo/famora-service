package com.famora.common.config;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

class EnvironmentIsolationValidatorTest {

  @Test
  void acceptsBucketOwnedByTheConfiguredEnvironment() {
    EnvironmentIsolationValidator validator = validator("vsit", "vsit-famora");

    assertThatNoException().isThrownBy(validator::validate);
  }

  @Test
  void rejectsCrossEnvironmentBucketConfiguration() {
    EnvironmentIsolationValidator validator = validator("prod", "vsit-famora");

    assertThatIllegalStateException().isThrownBy(validator::validate)
        .withMessageContaining("expected prod-famora");
  }

  private EnvironmentIsolationValidator validator(String environment, String bucket) {
    return new EnvironmentIsolationValidator(new EnvironmentNamespaceProperties(environment),
        bucket);
  }
}

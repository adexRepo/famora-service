package com.famora.vault.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class VaultPropertiesTest {

  @Test
  void acceptsSupportedAesKeyLengths() {
    assertThat(propertiesForBytes(16).decodedEncryptionKey()).hasSize(16);
    assertThat(propertiesForBytes(24).decodedEncryptionKey()).hasSize(24);
    assertThat(propertiesForBytes(32).decodedEncryptionKey()).hasSize(32);
  }

  @Test
  void rejectsMissingKey() {
    assertThatThrownBy(() -> new VaultProperties(null, java.util.List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be configured");
  }

  @Test
  void rejectsMalformedBase64() {
    assertThatThrownBy(() -> new VaultProperties("not-a-base64-key", java.util.List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Base64");
  }

  @Test
  void rejectsUnsupportedAesKeyLength() {
    assertThatThrownBy(() -> propertiesForBytes(20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("16, 24, or 32 bytes");
  }

  private VaultProperties propertiesForBytes(int length) {
    return new VaultProperties(Base64.getEncoder().encodeToString(new byte[length]),
        java.util.List.of());
  }
}

package com.famora.vault.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.famora.vault.config.VaultProperties;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class VaultCryptoServiceTest {

  private final VaultCryptoService service = new VaultCryptoService(
      new VaultProperties(Base64.getEncoder().encodeToString(new byte[32]), List.of()));

  @Test
  void decryptsValueEncryptedWithAesGcm() {
    String encrypted = service.encrypt("test vault value");

    assertThat(service.decrypt(encrypted)).isEqualTo("test vault value");
  }

  @Test
  void rejectsTamperedCiphertext() {
    String encrypted = service.encrypt("test vault value");
    int payloadSeparator = encrypted.lastIndexOf(':');
    String prefix = encrypted.substring(0, payloadSeparator + 1);
    String encoded = encrypted.substring(payloadSeparator + 1);
    byte[] tampered = Base64.getDecoder().decode(encoded);
    tampered[tampered.length - 1] ^= 1;

    assertThatThrownBy(() -> service.decrypt(prefix + Base64.getEncoder().encodeToString(tampered)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to decrypt vault secret");
  }

  @Test
  void decryptsExistingValuesWithPreviousRotationKey() {
    String oldKey = Base64.getEncoder().encodeToString(new byte[32]);
    byte[] newKeyBytes = new byte[32];
    java.util.Arrays.fill(newKeyBytes, (byte) 7);
    String newKey = Base64.getEncoder().encodeToString(newKeyBytes);
    String encryptedWithOldKey = new VaultCryptoService(new VaultProperties(oldKey, List.of()))
        .encrypt("rotated value");

    VaultCryptoService rotated = new VaultCryptoService(
        new VaultProperties(newKey, List.of(oldKey)));

    assertThat(rotated.decrypt(encryptedWithOldKey)).isEqualTo("rotated value");
    assertThat(rotated.requiresReencryption(encryptedWithOldKey)).isTrue();
    assertThat(rotated.encrypt("new value")).startsWith("v2:");
  }

  @Test
  void decryptsLegacyCiphertextWithoutKeyIdAndReencryptsWithActiveKeyId() {
    String current = service.encrypt("legacy value");
    String legacy = "v1:" + current.substring(current.lastIndexOf(':') + 1);

    assertThat(service.decrypt(legacy)).isEqualTo("legacy value");
    assertThat(service.requiresReencryption(legacy)).isTrue();
    assertThat(service.reencrypt(legacy)).startsWith(service.activeCiphertextPrefix());
  }
}

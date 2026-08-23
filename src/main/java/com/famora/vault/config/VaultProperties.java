package com.famora.vault.config;

import com.famora.security.config.Base64KeyValidator;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vault")
public record VaultProperties(String encryptionKey, List<String> previousEncryptionKeys) {

  public VaultProperties {
    validateKeyLength(Base64KeyValidator.decode("Vault encryption key", encryptionKey));
    previousEncryptionKeys = previousEncryptionKeys == null ? List.of()
        : previousEncryptionKeys.stream().map(String::trim).filter(key -> !key.isEmpty()).toList();
    for (String previousKey : previousEncryptionKeys) {
      validateKeyLength(Base64KeyValidator.decode("Previous vault encryption key", previousKey));
    }
  }

  public byte[] decodedEncryptionKey() {
    byte[] keyBytes = Base64KeyValidator.decode("Vault encryption key", encryptionKey);
    validateKeyLength(keyBytes);
    return keyBytes;
  }

  public List<byte[]> decodedPreviousEncryptionKeys() {
    return previousEncryptionKeys.stream()
        .map(key -> Base64KeyValidator.decode("Previous vault encryption key", key))
        .toList();
  }

  private static void validateKeyLength(byte[] keyBytes) {
    if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
      throw new IllegalArgumentException(
          "Vault encryption key must decode to 16, 24, or 32 bytes for AES");
    }
  }
}

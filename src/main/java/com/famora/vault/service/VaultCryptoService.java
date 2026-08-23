package com.famora.vault.service;

import com.famora.vault.config.VaultProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class VaultCryptoService {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;
  private static final int IV_LENGTH_BYTES = 12;

  private static final String LEGACY_FORMAT_PREFIX = "v1:";
  private static final String FORMAT_PREFIX = "v2:";
  private final SecretKey encryptionKey;
  private final String encryptionKeyId;
  private final List<SecretKey> decryptionKeys;
  private final Map<String, SecretKey> decryptionKeysById;
  private final SecureRandom secureRandom = new SecureRandom();

  public VaultCryptoService(VaultProperties properties) {
    byte[] activeKeyBytes = properties.decodedEncryptionKey();
    this.encryptionKey = new SecretKeySpec(activeKeyBytes, ALGORITHM);
    this.encryptionKeyId = keyId(activeKeyBytes);
    List<SecretKey> keys = new ArrayList<>();
    Map<String, SecretKey> keysById = new LinkedHashMap<>();
    keys.add(encryptionKey);
    keysById.put(encryptionKeyId, encryptionKey);
    properties.decodedPreviousEncryptionKeys().forEach(keyBytes -> {
      SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);
      keys.add(key);
      keysById.putIfAbsent(keyId(keyBytes), key);
    });
    this.decryptionKeys = List.copyOf(keys);
    this.decryptionKeysById = Map.copyOf(keysById);
  }

  public String encrypt(String plainText) {
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

      byte[] combined = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

      return activeCiphertextPrefix() + Base64.getEncoder().encodeToString(combined);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to encrypt vault secret", ex);
    }
  }

  public String decrypt(String encryptedText) {
    try {
      if (encryptedText.startsWith(FORMAT_PREFIX)) {
        int keySeparator = encryptedText.indexOf(':', FORMAT_PREFIX.length());
        if (keySeparator < 0) {
          throw new IllegalArgumentException("Invalid vault ciphertext version");
        }
        String keyId = encryptedText.substring(FORMAT_PREFIX.length(), keySeparator);
        SecretKey key = decryptionKeysById.get(keyId);
        if (key == null) {
          throw new IllegalStateException("Vault key is not configured for ciphertext key ID");
        }
        return decryptPayload(encryptedText.substring(keySeparator + 1), key);
      }

      String payload = encryptedText.startsWith(LEGACY_FORMAT_PREFIX)
          ? encryptedText.substring(LEGACY_FORMAT_PREFIX.length()) : encryptedText;
      for (SecretKey key : decryptionKeys) {
        try {
          return decryptPayload(payload, key);
        } catch (Exception ignored) {
          // Legacy values have no key ID, so each configured rotation key must be tried.
        }
      }
      throw new IllegalStateException("No configured vault key can decrypt the value");
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to decrypt vault secret", ex);
    }
  }

  public boolean requiresReencryption(String encryptedText) {
    return encryptedText == null || !encryptedText.startsWith(activeCiphertextPrefix());
  }

  public String reencrypt(String encryptedText) {
    return encrypt(decrypt(encryptedText));
  }

  public String activeCiphertextPrefix() {
    return FORMAT_PREFIX + encryptionKeyId + ":";
  }

  private String decryptPayload(String payload, SecretKey key) throws Exception {
    byte[] combined = Base64.getDecoder().decode(payload);
    if (combined.length <= IV_LENGTH_BYTES) {
      throw new IllegalArgumentException("Invalid vault ciphertext");
    }

    byte[] iv = new byte[IV_LENGTH_BYTES];
    byte[] encrypted = new byte[combined.length - IV_LENGTH_BYTES];

    System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
    System.arraycopy(combined, IV_LENGTH_BYTES, encrypted, 0, encrypted.length);

    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
    return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
  }

  private static String keyId(byte[] keyBytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(keyBytes);
      return HexFormat.of().formatHex(digest, 0, 8);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to identify vault encryption key", exception);
    }
  }
}

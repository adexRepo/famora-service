package com.famora.vault.service;

import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VaultCryptoService {
  
  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;
  private static final int IV_LENGTH_BYTES = 12;
  
  @Value("${app.vault.encryption-key}")
  private String encryptionKeyBase64;
  
  private SecretKey secretKey;
  private final SecureRandom secureRandom = new SecureRandom();
  
  @PostConstruct
  void init() {
    byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
    if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
      throw new IllegalStateException(
          "Invalid vault encryption key length: "
              + keyBytes.length + " bytes. AES key must be 16, 24, or 32 bytes. "
              + "Generate a valid key using: openssl rand -base64 32"
      );
    }
    this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
  }
  
  public String encrypt(String plainText) {
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);
      
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      
      byte[] encrypted = cipher.doFinal(plainText.getBytes());
      
      byte[] combined = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
      
      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to encrypt vault secret", ex);
    }
  }
  
  public String decrypt(String encryptedText) {
    try {
      byte[] combined = Base64.getDecoder().decode(encryptedText);
      
      byte[] iv = new byte[IV_LENGTH_BYTES];
      byte[] encrypted = new byte[combined.length - IV_LENGTH_BYTES];
      
      System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
      System.arraycopy(combined, IV_LENGTH_BYTES, encrypted, 0, encrypted.length);
      
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      
      return new String(cipher.doFinal(encrypted));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to decrypt vault secret", ex);
    }
  }
}

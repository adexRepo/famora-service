package com.famora.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class TokenHashService {
  
  public String sha256(String rawToken) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256")
          .digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to hash token", ex);
    }
  }
}

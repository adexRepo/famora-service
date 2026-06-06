package com.famora.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Authentication {
  
  // === Identifiers ===
  private UUID sessionId;
  private UUID userId;
  private String deviceId;
  
  // === User Info (minimal) ===
  private String keyNo;
  private String nickName;
  private List<String> roles;
  private List<UUID> orgIds;
  
  // === Token Info ===
  private String accessJti;
  private String refreshJti;
  private String oldRefreshJti;
  private Instant accessTokenExpiresAt;
  private Instant refreshTokenExpiresAt;
  private Instant sessionAbsExpiresAt;
  
  private String oldRefreshTokenHash;
  private String refreshTokenHash;
  
  // === Location (optional) ===
  private Map<String, List<String>> userAccess;
  
}

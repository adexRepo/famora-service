package com.famora.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthResponse(String accessToken, String refreshToken, Instant accessTokenExpiresAt,
                           Instant refreshTokenExpiresAt, UserSummary user,
                           List<FamilySummary> families) {
  
  public record UserSummary(UUID id, String fullName, String email) {
  
  }
  
  public record FamilySummary(UUID id, String name, String role, boolean isDefault) {
  
  }
}

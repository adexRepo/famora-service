package com.famora.user.repository;

import com.famora.user.entity.UserSession;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
  
  Optional<UserSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);
  
  @Modifying
  @Query("""
      update UserSession s
      set s.revokedAt = :revokedAt
      where s.user.id = :userId
        and s.revokedAt is null
      """)
  int revokeActiveSessionsByUserId(UUID userId, OffsetDateTime revokedAt);
}

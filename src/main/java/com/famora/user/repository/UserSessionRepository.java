package com.famora.user.repository;

import com.famora.user.entity.UserSession;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
  
  Optional<UserSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from UserSession s join fetch s.user where s.refreshTokenHash = :tokenHash")
  Optional<UserSession> findByRefreshTokenHashForUpdate(String tokenHash);
  
  @Modifying
  @Query("""
      update UserSession s
      set s.revokedAt = :revokedAt
      where s.user.id = :userId
        and s.revokedAt is null
      """)
  int revokeActiveSessionsByUserId(UUID userId, OffsetDateTime revokedAt);

  @Modifying
  @Query("delete from UserSession s where s.expiresAt < :cutoff")
  int deleteExpiredBefore(OffsetDateTime cutoff);
}

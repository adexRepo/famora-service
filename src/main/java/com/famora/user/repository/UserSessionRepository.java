package com.famora.user.repository;

import com.famora.user.entity.UserSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
  
  Optional<UserSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);
}

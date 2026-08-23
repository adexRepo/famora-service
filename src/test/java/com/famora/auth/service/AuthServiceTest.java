package com.famora.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.audit.service.AuditLogService;
import com.famora.auth.dto.RefreshTokenRequest;
import com.famora.auth.exception.RefreshTokenAuthenticationException;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.security.TokenHashService;
import com.famora.security.AbuseRateLimitService;
import com.famora.security.CurrentUserProvider;
import com.famora.security.jwt.JwtService;
import com.famora.user.entity.User;
import com.famora.user.entity.UserSession;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import com.famora.user.repository.UserSessionRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UserSessionRepository userSessionRepository;
  @Mock private FamilyMemberRepository familyMemberRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private AuditLogService auditLogService;
  @Mock private AbuseRateLimitService rateLimitService;
  @Mock private CurrentUserProvider currentUserProvider;

  private final TokenHashService tokenHashService = new TokenHashService();
  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(userRepository, userSessionRepository, familyMemberRepository,
        passwordEncoder, jwtService, tokenHashService, auditLogService, rateLimitService,
        currentUserProvider);
    ReflectionTestUtils.setField(authService, "refreshTokenExpirationDays", 30L);
  }

  @Test
  void refreshAtomicallyRevokesConsumedSessionAndIssuesReplacement() {
    String rawToken = "old-refresh-token";
    User user = activeUser();
    UserSession consumed = UserSession.builder()
        .user(user)
        .refreshTokenHash(tokenHashService.sha256(rawToken))
        .expiresAt(OffsetDateTime.now().plusDays(1))
        .build();
    when(userSessionRepository.findByRefreshTokenHashForUpdate(
        tokenHashService.sha256(rawToken))).thenReturn(Optional.of(consumed));
    when(jwtService.generateAccessTokenDetails(user.getId(), user.getEmail()))
        .thenReturn(new JwtService.GeneratedToken("access", Instant.now().plusSeconds(600)));
    when(familyMemberRepository.findActiveFamiliesByUserId(user.getId())).thenReturn(List.of());

    var response = authService.refresh(new RefreshTokenRequest(rawToken));

    assertThat(consumed.getRevokedAt()).isNotNull();
    assertThat(response.refreshToken()).isNotBlank().isNotEqualTo(rawToken);
    assertThat(response.accessToken()).isEqualTo("access");
    verify(userSessionRepository).save(consumed);
    verify(rateLimitService).checkRefreshAccount(user.getId());
  }

  @Test
  void logoutRevokesEverySessionForAuthenticatedUser() {
    User user = activeUser();
    when(currentUserProvider.getCurrentUser()).thenReturn(user);

    authService.logout();

    verify(userSessionRepository).revokeActiveSessionsByUserId(
        org.mockito.ArgumentMatchers.eq(user.getId()), any(OffsetDateTime.class));
  }

  @Test
  void refreshRejectsReplayOfRevokedSessionWithoutMintingToken() {
    String rawToken = "replayed-refresh-token";
    UserSession replayed = UserSession.builder()
        .user(activeUser())
        .refreshTokenHash(tokenHashService.sha256(rawToken))
        .expiresAt(OffsetDateTime.now().plusDays(1))
        .revokedAt(OffsetDateTime.now().minusSeconds(1))
        .build();
    when(userSessionRepository.findByRefreshTokenHashForUpdate(
        tokenHashService.sha256(rawToken))).thenReturn(Optional.of(replayed));

    assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(rawToken)))
        .isInstanceOf(RefreshTokenAuthenticationException.class)
        .hasMessageContaining("already been used");
    verify(jwtService, never()).generateAccessTokenDetails(any(), any());
  }

  private User activeUser() {
    User user = User.builder()
        .fullName("Test User")
        .email("test@example.com")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    return user;
  }
}

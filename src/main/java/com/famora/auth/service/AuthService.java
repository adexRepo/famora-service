package com.famora.auth.service;

import com.famora.auth.dto.AuthResponse;
import com.famora.auth.dto.LoginRequest;
import com.famora.auth.dto.RefreshTokenRequest;
import com.famora.auth.dto.RegisterRequest;
import com.famora.common.helper.Status;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.security.TokenHashService;
import com.famora.security.jwt.JwtService;
import com.famora.user.entity.User;
import com.famora.user.entity.UserSession;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import com.famora.user.repository.UserSessionRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
  
  private final UserRepository userRepository;
  private final UserSessionRepository userSessionRepository;
  private final FamilyMemberRepository familyMemberRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final TokenHashService tokenHashService;
  @Value("${app.security.jwt.refresh-token-expiration-days}")
  private long refreshTokenExpirationDays;
  private final SecureRandom secureRandom = new SecureRandom();
  
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase();
    
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new IllegalArgumentException("Email already registered");
    }
    
    User user = User.builder()
        .fullName(request.fullName().trim())
        .email(normalizedEmail)
        .passwordHash(passwordEncoder.encode(request.password()))
        .status(UserStatus.ACTIVE)
        .build();
    
    userRepository.save(user);
    
    return generateAuthResponse(user);
  }
  
  @Transactional
  public AuthResponse login(LoginRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase();
    User user = userRepository.findByEmailAndStatus(normalizedEmail, UserStatus.ACTIVE)
        .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid email or password");
    }
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new BadCredentialsException("User is not active");
    }
    user.setLastLoginAt(OffsetDateTime.now());
    userRepository.save(user);
    return generateAuthResponse(user);
  }
  
  @Transactional
  public AuthResponse refresh(RefreshTokenRequest request) {
    String refreshTokenHash = tokenHashService.sha256(request.refreshToken());
    UserSession session = userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(
        refreshTokenHash).orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
      throw new BadCredentialsException("Refresh token expired");
    }
    return generateAuthResponse(session.getUser());
  }
  
  @Transactional
  public void logout(String refreshToken) {
    String refreshTokenHash = tokenHashService.sha256(refreshToken);
    userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(refreshTokenHash)
        .ifPresent(session -> {
          session.setRevokedAt(OffsetDateTime.now());
          userSessionRepository.save(session);
        });
  }
  
  private AuthResponse generateAuthResponse(User user) {
    JwtService.GeneratedToken accessToken = jwtService.generateAccessTokenDetails(user.getId(),
        user.getEmail());
    String refreshToken = generateSecureToken();
    OffsetDateTime refreshTokenExpiresAt = OffsetDateTime.now().plusDays(refreshTokenExpirationDays);
    UserSession session = UserSession.builder().user(user)
        .refreshTokenHash(tokenHashService.sha256(refreshToken))
        .expiresAt(refreshTokenExpiresAt).build();
    log.debug("Save User Session");
    userSessionRepository.save(session);
    List<AuthResponse.FamilySummary> families = familyMemberRepository.findActiveFamiliesByUserId(
            user.getId()).stream()
        .map(member -> new AuthResponse.FamilySummary(member.getFamily().getId(),
            member.getFamily().getName(), member.getRole().name()))
        .toList();
    return new AuthResponse(accessToken.token(), refreshToken, accessToken.expiresAt(),
        refreshTokenExpiresAt.toInstant(),
        new AuthResponse.UserSummary(user.getId(), user.getFullName(), user.getEmail()), families);
  }
  
  private String generateSecureToken() {
    byte[] bytes = new byte[64];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}

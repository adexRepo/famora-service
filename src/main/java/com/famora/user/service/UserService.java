package com.famora.user.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.security.CurrentUserProvider;
import com.famora.user.dto.ChangePasswordRequest;
import com.famora.user.dto.UpdateUserProfileRequest;
import com.famora.user.dto.UserProfileResponse;
import com.famora.user.entity.User;
import com.famora.user.repository.UserRepository;
import com.famora.user.repository.UserSessionRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  
  private final CurrentUserProvider currentUserProvider;
  private final UserRepository userRepository;
  private final UserSessionRepository userSessionRepository;
  private final AuditLogService auditLogService;
  private final PasswordEncoder passwordEncoder;
  
  @Transactional(readOnly = true)
  public UserProfileResponse getMe() {
    User user = currentUserProvider.getCurrentUser();
    return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(),
        user.getDateOfBirth(),
        user.getStatus().name(), user.getCreatedAt());
  }
  
  @Transactional
  public UserProfileResponse updateMe(UpdateUserProfileRequest request) {
    User user = currentUserProvider.getCurrentUser();
    user.setFullName(request.fullName().trim());
    user.setDateOfBirth(request.dateOfBirth());
    userRepository.save(user);
    auditLogService.log(null, user, AuditAction.USER_PROFILE_UPDATED, "users", user.getId(),
        "{\"fullName\":\"" + user.getFullName() + "\"}");
    return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(),
        user.getDateOfBirth(),
        user.getStatus().name(), user.getCreatedAt());
  }
  
  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    User user = currentUserProvider.getCurrentUser();
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Current password is invalid");
    }
    OffsetDateTime changedAt = OffsetDateTime.now();
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setPasswordChangedAt(changedAt);
    userRepository.save(user);
    userSessionRepository.revokeActiveSessionsByUserId(user.getId(), changedAt);
    auditLogService.log(null, user, AuditAction.USER_PASSWORD_CHANGED, "users", user.getId(),
        "{\"passwordChanged\":true}");
  }
}

package com.famora.user.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.repository.AuditLogRepository;
import com.famora.audit.service.AuditLogService;
import com.famora.backup.service.BackupUploadService;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.business.repository.BusinessRepository;
import com.famora.common.exception.AppException;
import com.famora.common.helper.Status;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.family.repository.FamilyRepository;
import com.famora.notification.repository.NotificationPreferenceRepository;
import com.famora.notification.repository.ScheduledNotificationRepository;
import com.famora.security.CurrentUserProvider;
import com.famora.user.dto.ChangePasswordRequest;
import com.famora.user.dto.DeleteAccountRequest;
import com.famora.user.dto.UpdateUserProfileRequest;
import com.famora.user.dto.UserProfileResponse;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import com.famora.user.repository.UserSessionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
  private final FamilyRepository familyRepository;
  private final FamilyMemberRepository familyMemberRepository;
  private final BusinessRepository businessRepository;
  private final BusinessMemberRepository businessMemberRepository;
  private final AuditLogRepository auditLogRepository;
  private final NotificationPreferenceRepository notificationPreferenceRepository;
  private final ScheduledNotificationRepository scheduledNotificationRepository;
  private final BackupUploadService backupUploadService;
  
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

  @Transactional
  public void deleteAccount(DeleteAccountRequest request) {
    UUID currentUserId = currentUserProvider.getCurrentUserId();
    User user = userRepository.findAllByIdForUpdate(List.of(currentUserId)).stream()
        .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
        .findFirst()
        .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Current password is invalid");
    }
    if (!"DELETE".equals(request.confirmation().trim().toUpperCase(Locale.ROOT))) {
      throw new IllegalArgumentException("Type DELETE to confirm account deletion");
    }
    if (familyRepository.existsByOwnerUserAndStatus(user, Status.ACTIVE)
        || businessRepository.existsByOwnerUserIdAndStatus(user.getId(), Status.ACTIVE)) {
      throw new AppException(HttpStatus.CONFLICT,
          "Transfer ownership of every active family and business before deleting the account");
    }

    OffsetDateTime deletedAt = OffsetDateTime.now();
    familyMemberRepository.deactivateMembershipsForDeletedUser(user.getId(), deletedAt);
    businessMemberRepository.deactivateMembershipsForDeletedUser(user.getId(), deletedAt);
    userSessionRepository.revokeActiveSessionsByUserId(user.getId(), deletedAt);
    notificationPreferenceRepository.deleteByUserId(user.getId());
    scheduledNotificationRepository.deleteByReceiverUser_Id(user.getId());
    backupUploadService.cancelIncompleteSessionsForDeletedUser(user.getId(), deletedAt);
    auditLogRepository.anonymizePersonalDataByUserId(user.getId());

    UUID userId = user.getId();
    user.setFullName("Deleted user");
    user.setEmail("deleted+" + userId + "@deleted.invalid");
    user.setDateOfBirth(null);
    user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
    user.setPasswordChangedAt(deletedAt);
    user.setLastLoginAt(null);
    user.setStatus(UserStatus.DELETED);
    userRepository.save(user);
    auditLogService.log(null, user, AuditAction.USER_ACCOUNT_DELETED, "users", userId,
        "{\"userId\":\"" + userId + "\",\"identityAnonymized\":true}");
  }
}

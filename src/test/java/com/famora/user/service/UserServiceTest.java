package com.famora.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.audit.service.AuditLogService;
import com.famora.audit.repository.AuditLogRepository;
import com.famora.backup.service.BackupUploadService;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.business.repository.BusinessRepository;
import com.famora.common.exception.AppException;
import com.famora.common.helper.Status;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.family.repository.FamilyRepository;
import com.famora.security.CurrentUserProvider;
import com.famora.user.dto.DeleteAccountRequest;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import com.famora.user.repository.UserSessionRepository;
import com.famora.notification.repository.NotificationPreferenceRepository;
import com.famora.notification.repository.ScheduledNotificationRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private UserRepository userRepository;
  @Mock private UserSessionRepository userSessionRepository;
  @Mock private AuditLogService auditLogService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private FamilyRepository familyRepository;
  @Mock private FamilyMemberRepository familyMemberRepository;
  @Mock private BusinessRepository businessRepository;
  @Mock private BusinessMemberRepository businessMemberRepository;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private NotificationPreferenceRepository notificationPreferenceRepository;
  @Mock private ScheduledNotificationRepository scheduledNotificationRepository;
  @Mock private BackupUploadService backupUploadService;

  private UserService userService;
  private User user;

  @BeforeEach
  void setUp() {
    userService = new UserService(currentUserProvider, userRepository, userSessionRepository,
        auditLogService, passwordEncoder, familyRepository, familyMemberRepository,
        businessRepository, businessMemberRepository, auditLogRepository,
        notificationPreferenceRepository, scheduledNotificationRepository, backupUploadService);
    user = User.builder().fullName("Original Name").email("user@example.com")
        .passwordHash("existing-hash").status(UserStatus.ACTIVE).build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    when(currentUserProvider.getCurrentUserId()).thenReturn(user.getId());
    when(userRepository.findAllByIdForUpdate(List.of(user.getId()))).thenReturn(List.of(user));
    when(passwordEncoder.matches("correct-password", "existing-hash")).thenReturn(true);
  }

  @Test
  void deletionAnonymizesIdentityAndRevokesAllAccess() {
    when(passwordEncoder.encode(any())).thenReturn("unusable-random-hash");

    userService.deleteAccount(new DeleteAccountRequest("correct-password", "DELETE"));

    assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
    assertThat(user.getFullName()).isEqualTo("Deleted user");
    assertThat(user.getEmail()).startsWith("deleted+").endsWith("@deleted.invalid");
    assertThat(user.getDateOfBirth()).isNull();
    verify(familyMemberRepository).deactivateMembershipsForDeletedUser(
        org.mockito.ArgumentMatchers.eq(user.getId()), any(OffsetDateTime.class));
    verify(businessMemberRepository).deactivateMembershipsForDeletedUser(
        org.mockito.ArgumentMatchers.eq(user.getId()), any(OffsetDateTime.class));
    verify(userSessionRepository).revokeActiveSessionsByUserId(
        org.mockito.ArgumentMatchers.eq(user.getId()), any(OffsetDateTime.class));
    verify(auditLogRepository).anonymizePersonalDataByUserId(user.getId());
    verify(notificationPreferenceRepository).deleteByUserId(user.getId());
    verify(scheduledNotificationRepository).deleteByReceiverUser_Id(user.getId());
    verify(backupUploadService).cancelIncompleteSessionsForDeletedUser(
        org.mockito.ArgumentMatchers.eq(user.getId()), any(OffsetDateTime.class));
    verify(userRepository).save(user);
  }

  @Test
  void deletionIsBlockedUntilSharedResourceOwnershipIsTransferred() {
    when(familyRepository.existsByOwnerUserAndStatus(user, Status.ACTIVE)).thenReturn(true);

    assertThatThrownBy(() -> userService.deleteAccount(
        new DeleteAccountRequest("correct-password", "DELETE")))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("Transfer ownership");

    verify(userSessionRepository, never()).revokeActiveSessionsByUserId(any(), any());
    verify(userRepository, never()).save(any());
  }
}

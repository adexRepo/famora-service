package com.famora.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.admin.dto.UpdateBackupQuotaRequest;
import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.backup.service.BackupStorageQuotaService;
import com.famora.backup.service.BackupStorageQuotaService.BackupQuotaUsage;
import com.famora.user.entity.User;
import com.famora.user.entity.UserRole;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminBackupQuotaServiceTest {

  @Mock private AdminAuthorizationService authorizationService;
  @Mock private UserRepository userRepository;
  @Mock private BackupStorageQuotaService quotaService;
  @Mock private AuditLogService auditLogService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private AdminBackupQuotaService service;
  private User admin;

  @BeforeEach
  void setUp() {
    service = new AdminBackupQuotaService(authorizationService, userRepository, quotaService,
        auditLogService, objectMapper);
    admin = user("Admin", "admin@example.com", UserRole.ADMIN);
  }

  @Test
  void enablesQuotaOverrideAndAuditsReasonAndValues() throws Exception {
    User target = user("Target", "target@example.com", UserRole.USER);
    long fiftyGibibytes = 50L * 1024 * 1024 * 1024;
    when(authorizationService.requireAdmin()).thenReturn(admin);
    when(userRepository.findAllByIdForUpdate(List.of(target.getId())))
        .thenReturn(List.of(target));
    when(quotaService.usage(target)).thenReturn(
        new BackupQuotaUsage(10, fiftyGibibytes, 3, 2, fiftyGibibytes - 5, true));

    var response = service.updateQuota(target.getId(),
        new UpdateBackupQuotaRequest(true, fiftyGibibytes, " Premium storage "));

    assertThat(target.isBackupQuotaOverrideEnabled()).isTrue();
    assertThat(target.getBackupQuotaOverrideBytes()).isEqualTo(fiftyGibibytes);
    assertThat(response.effectiveQuotaBytes()).isEqualTo(fiftyGibibytes);
    ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
    verify(auditLogService).log(isNull(), same(admin),
        eq(AuditAction.BACKUP_QUOTA_OVERRIDE_UPDATED), eq("users"), eq(target.getId()),
        metadataCaptor.capture());
    JsonNode metadata = objectMapper.readTree(metadataCaptor.getValue());
    assertThat(metadata.get("previousOverrideEnabled").booleanValue()).isFalse();
    assertThat(metadata.get("overrideEnabled").booleanValue()).isTrue();
    assertThat(metadata.get("quotaBytes").longValue()).isEqualTo(fiftyGibibytes);
    assertThat(metadata.get("reason").textValue()).isEqualTo("Premium storage");
  }

  @Test
  void resetsQuotaToDefaultWithoutChangingCurrentUsage() {
    User target = user("Target", "target@example.com", UserRole.USER);
    target.setBackupQuotaOverrideEnabled(true);
    target.setBackupQuotaOverrideBytes(50L);
    target.setBackupStorageUsedBytes(25);
    when(authorizationService.requireAdmin()).thenReturn(admin);
    when(userRepository.findAllByIdForUpdate(List.of(target.getId())))
        .thenReturn(List.of(target));
    when(quotaService.usage(target)).thenReturn(new BackupQuotaUsage(10, 10, 25, 0, 0, false));

    var response = service.updateQuota(target.getId(),
        new UpdateBackupQuotaRequest(false, null, "Reset"));

    assertThat(target.isBackupQuotaOverrideEnabled()).isFalse();
    assertThat(target.getBackupQuotaOverrideBytes()).isNull();
    assertThat(target.getBackupStorageUsedBytes()).isEqualTo(25);
    assertThat(response.availableBytes()).isZero();
  }

  private User user(String fullName, String email, UserRole role) {
    User user = User.builder()
        .fullName(fullName)
        .email(email)
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .role(role)
        .build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    return user;
  }
}

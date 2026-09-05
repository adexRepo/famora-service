package com.famora.admin.service;

import com.famora.admin.dto.AdminBackupQuotaResponse;
import com.famora.admin.dto.UpdateBackupQuotaRequest;
import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.backup.service.BackupStorageQuotaService;
import com.famora.backup.service.BackupStorageQuotaService.BackupQuotaUsage;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBackupQuotaService {

  private final AdminAuthorizationService authorizationService;
  private final UserRepository userRepository;
  private final BackupStorageQuotaService quotaService;
  private final AuditLogService auditLogService;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public AdminBackupQuotaResponse getUsage(UUID userId) {
    authorizationService.requireAdmin();
    User target = requireTarget(userId, false);
    return response(target);
  }

  @Transactional
  public AdminBackupQuotaResponse updateQuota(UUID userId, UpdateBackupQuotaRequest request) {
    User admin = authorizationService.requireAdmin();
    User target = requireTarget(userId, true);
    boolean previousOverrideEnabled = target.isBackupQuotaOverrideEnabled();
    Long previousQuotaBytes = target.getBackupQuotaOverrideBytes();

    target.setBackupQuotaOverrideEnabled(request.overrideEnabled());
    target.setBackupQuotaOverrideBytes(request.overrideEnabled() ? request.quotaBytes() : null);
    userRepository.save(target);

    auditLogService.log(null, admin, AuditAction.BACKUP_QUOTA_OVERRIDE_UPDATED, "users",
        target.getId(), auditMetadata(target, previousOverrideEnabled, previousQuotaBytes,
            request.reason().trim()));
    return response(target);
  }

  private User requireTarget(UUID userId, boolean lock) {
    return (lock ? userRepository.findAllByIdForUpdate(List.of(userId)).stream()
        : userRepository.findById(userId).stream())
        .filter(user -> user.getStatus() != UserStatus.DELETED)
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  private AdminBackupQuotaResponse response(User target) {
    BackupQuotaUsage usage = quotaService.usage(target);
    return new AdminBackupQuotaResponse(
        target.getId(), usage.defaultQuotaBytes(), usage.effectiveQuotaBytes(),
        usage.usedBytes(), usage.reservedBytes(), usage.availableBytes(), usage.overrideEnabled());
  }

  private String auditMetadata(User target, boolean previousOverrideEnabled,
      Long previousQuotaBytes, String reason) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("targetUserId", target.getId());
    metadata.put("previousOverrideEnabled", previousOverrideEnabled);
    metadata.put("previousQuotaBytes", previousQuotaBytes);
    metadata.put("overrideEnabled", target.isBackupQuotaOverrideEnabled());
    metadata.put("quotaBytes", target.getBackupQuotaOverrideBytes());
    metadata.put("reason", reason);
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize backup quota audit metadata",
          exception);
    }
  }
}

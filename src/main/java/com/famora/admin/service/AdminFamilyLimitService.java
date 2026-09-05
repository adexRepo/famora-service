package com.famora.admin.service;

import com.famora.admin.dto.AdminFamilyLimitResponse;
import com.famora.admin.dto.UpdateFamilyLimitRequest;
import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.family.config.FamilyProperties;
import com.famora.family.dto.FamilyMembershipSummaryResponse;
import com.famora.family.service.FamilyMembershipPolicyService;
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
public class AdminFamilyLimitService {

  private final AdminAuthorizationService authorizationService;
  private final UserRepository userRepository;
  private final FamilyMembershipPolicyService familyMembershipPolicyService;
  private final FamilyProperties familyProperties;
  private final AuditLogService auditLogService;
  private final ObjectMapper objectMapper;

  @Transactional
  public AdminFamilyLimitResponse updateFamilyLimit(UUID userId,
      UpdateFamilyLimitRequest request) {
    User admin = authorizationService.requireAdmin();
    User target = userRepository.findAllByIdForUpdate(List.of(userId)).stream()
        .filter(user -> user.getStatus() != UserStatus.DELETED)
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    boolean previousOverrideEnabled = target.isFamilyLimitOverrideEnabled();
    Integer previousMaxFamilies = target.getMaxFamilyOverride();
    target.setFamilyLimitOverrideEnabled(request.overrideEnabled());
    target.setMaxFamilyOverride(request.overrideEnabled() ? request.maxFamilies() : null);
    userRepository.save(target);

    FamilyMembershipSummaryResponse summary = familyMembershipPolicyService.summary(target);
    auditLogService.log(null, admin, AuditAction.FAMILY_LIMIT_OVERRIDE_UPDATED, "users",
        target.getId(), auditMetadata(target, previousOverrideEnabled, previousMaxFamilies,
            request.reason().trim()));
    return new AdminFamilyLimitResponse(
        target.getId(),
        familyProperties.effectiveMaxActiveFamilyPerUser(),
        summary.maxFamilyCount(),
        summary.activeFamilyCount(),
        summary.canCreateOrJoinFamily(),
        summary.limitOverrideEnabled()
    );
  }

  private String auditMetadata(User target, boolean previousOverrideEnabled,
      Integer previousMaxFamilies, String reason) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("targetUserId", target.getId());
    metadata.put("previousOverrideEnabled", previousOverrideEnabled);
    metadata.put("previousMaxFamilies", previousMaxFamilies);
    metadata.put("overrideEnabled", target.isFamilyLimitOverrideEnabled());
    metadata.put("maxFamilies", target.getMaxFamilyOverride());
    metadata.put("reason", reason);
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize family limit audit metadata",
          exception);
    }
  }
}

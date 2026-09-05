package com.famora.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.admin.dto.UpdateFamilyLimitRequest;
import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.family.config.FamilyProperties;
import com.famora.family.dto.FamilyMembershipSummaryResponse;
import com.famora.family.service.FamilyMembershipPolicyService;
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
class AdminFamilyLimitServiceTest {

  @Mock private AdminAuthorizationService authorizationService;
  @Mock private UserRepository userRepository;
  @Mock private FamilyMembershipPolicyService familyMembershipPolicyService;
  @Mock private AuditLogService auditLogService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private AdminFamilyLimitService service;
  private User admin;

  @BeforeEach
  void setUp() {
    service = new AdminFamilyLimitService(authorizationService, userRepository,
        familyMembershipPolicyService, new FamilyProperties(3), auditLogService, objectMapper);
    admin = user("Admin", "admin@example.com", UserRole.ADMIN);
  }

  @Test
  void enablesFamilyLimitOverrideAndAuditsOldAndNewValues() throws Exception {
    User target = user("Target User", "target@example.com", UserRole.USER);
    when(authorizationService.requireAdmin()).thenReturn(admin);
    when(userRepository.findAllByIdForUpdate(List.of(target.getId())))
        .thenReturn(List.of(target));
    when(familyMembershipPolicyService.summary(target)).thenReturn(
        new FamilyMembershipSummaryResponse(4, 10, true, true));

    var response = service.updateFamilyLimit(target.getId(),
        new UpdateFamilyLimitRequest(true, 10, " Beta tester "));

    assertThat(target.isFamilyLimitOverrideEnabled()).isTrue();
    assertThat(target.getMaxFamilyOverride()).isEqualTo(10);
    assertThat(response.defaultMaxFamilies()).isEqualTo(3);
    assertThat(response.effectiveMaxFamilies()).isEqualTo(10);
    assertThat(response.activeFamilyCount()).isEqualTo(4);
    ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
    verify(auditLogService).log(isNull(), same(admin),
        eq(AuditAction.FAMILY_LIMIT_OVERRIDE_UPDATED), eq("users"), eq(target.getId()),
        metadataCaptor.capture());
    JsonNode metadata = objectMapper.readTree(metadataCaptor.getValue());
    assertThat(metadata.get("previousOverrideEnabled").booleanValue()).isFalse();
    assertThat(metadata.get("previousMaxFamilies").isNull()).isTrue();
    assertThat(metadata.get("overrideEnabled").booleanValue()).isTrue();
    assertThat(metadata.get("maxFamilies").intValue()).isEqualTo(10);
    assertThat(metadata.get("reason").textValue()).isEqualTo("Beta tester");
  }

  @Test
  void resetsFamilyLimitToSystemDefault() {
    User target = user("Target User", "target@example.com", UserRole.USER);
    target.setFamilyLimitOverrideEnabled(true);
    target.setMaxFamilyOverride(10);
    when(authorizationService.requireAdmin()).thenReturn(admin);
    when(userRepository.findAllByIdForUpdate(List.of(target.getId())))
        .thenReturn(List.of(target));
    when(familyMembershipPolicyService.summary(target)).thenReturn(
        new FamilyMembershipSummaryResponse(2, 3, true, false));

    var response = service.updateFamilyLimit(target.getId(),
        new UpdateFamilyLimitRequest(false, null, "Reset to default"));

    assertThat(target.isFamilyLimitOverrideEnabled()).isFalse();
    assertThat(target.getMaxFamilyOverride()).isNull();
    assertThat(response.effectiveMaxFamilies()).isEqualTo(3);
    verify(userRepository).save(target);
  }

  @Test
  void rejectsDeletedTargetUser() {
    User target = user("Deleted", "deleted@example.com", UserRole.USER);
    target.setStatus(UserStatus.DELETED);
    when(authorizationService.requireAdmin()).thenReturn(admin);
    when(userRepository.findAllByIdForUpdate(List.of(target.getId())))
        .thenReturn(List.of(target));

    assertThatThrownBy(() -> service.updateFamilyLimit(target.getId(),
        new UpdateFamilyLimitRequest(true, 5, "Not applicable")))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("User not found");

    verify(userRepository, never()).save(any());
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

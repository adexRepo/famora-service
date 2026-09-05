package com.famora.admin.service;

import com.famora.admin.dto.AdminUserSummaryResponse;
import com.famora.admin.spec.AdminUserSpecifications;
import com.famora.family.helper.FamilyMemberStatus;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.family.service.FamilyMembershipPolicyService;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserQueryService {

  private final AdminAuthorizationService authorizationService;
  private final UserRepository userRepository;
  private final FamilyMemberRepository familyMemberRepository;
  private final FamilyMembershipPolicyService familyMembershipPolicyService;

  @Transactional(readOnly = true)
  public Page<AdminUserSummaryResponse> searchUsers(String query, UserStatus status,
      Pageable pageable) {
    authorizationService.requireAdmin();
    Page<User> users = userRepository.findAll(
        AdminUserSpecifications.matchesQuery(query)
            .and(AdminUserSpecifications.hasStatus(status)),
        pageable);
    Map<UUID, Long> familyCounts = activeFamilyCounts(
        users.getContent().stream().map(User::getId).toList());
    return users.map(user -> new AdminUserSummaryResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getStatus().name(),
        user.getRole().name(),
        user.getCreatedAt(),
        user.getLastLoginAt(),
        familyCounts.getOrDefault(user.getId(), 0L),
        familyMembershipPolicyService.maxFamilyCount(user),
        user.isFamilyLimitOverrideEnabled()
    ));
  }

  private Map<UUID, Long> activeFamilyCounts(Collection<UUID> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return familyMemberRepository.countFamiliesByUserIdsAndStatus(userIds,
            FamilyMemberStatus.ACTIVE).stream()
        .collect(Collectors.toMap(
            FamilyMemberRepository.UserFamilyCount::getUserId,
            FamilyMemberRepository.UserFamilyCount::getFamilyCount,
            (first, second) -> first
        ));
  }
}

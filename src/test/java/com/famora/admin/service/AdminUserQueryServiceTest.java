package com.famora.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.family.helper.FamilyMemberStatus;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.family.service.FamilyMembershipPolicyService;
import com.famora.user.entity.User;
import com.famora.user.entity.UserRole;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserQueryServiceTest {

  @Mock private AdminAuthorizationService authorizationService;
  @Mock private UserRepository userRepository;
  @Mock private FamilyMemberRepository familyMemberRepository;
  @Mock private FamilyMembershipPolicyService familyMembershipPolicyService;

  private AdminUserQueryService service;

  @BeforeEach
  void setUp() {
    service = new AdminUserQueryService(authorizationService, userRepository,
        familyMemberRepository, familyMembershipPolicyService);
  }

  @Test
  void searchesUsersWithBatchedFamilyCounts() {
    User target = user("Target User", "target@example.com");
    Pageable pageable = PageRequest.of(0, 20);
    when(userRepository.findAll(anyUserSpecification(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(target), pageable, 1));
    FamilyMemberRepository.UserFamilyCount projection =
        mock(FamilyMemberRepository.UserFamilyCount.class);
    when(projection.getUserId()).thenReturn(target.getId());
    when(projection.getFamilyCount()).thenReturn(2L);
    when(familyMemberRepository.countFamiliesByUserIdsAndStatus(
        List.of(target.getId()), FamilyMemberStatus.ACTIVE)).thenReturn(List.of(projection));
    when(familyMembershipPolicyService.maxFamilyCount(target)).thenReturn(3);

    var page = service.searchUsers("target", UserStatus.ACTIVE, pageable);

    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent()).singleElement().satisfies(response -> {
      assertThat(response.userId()).isEqualTo(target.getId());
      assertThat(response.email()).isEqualTo("target@example.com");
      assertThat(response.activeFamilyCount()).isEqualTo(2);
      assertThat(response.maxFamilyCount()).isEqualTo(3);
    });
  }

  @Test
  void performsNoUserQueryWhenServiceAuthorizationFails() {
    when(authorizationService.requireAdmin())
        .thenThrow(new AuthorizationDeniedException("Administrator access required"));

    assertThatThrownBy(() -> service.searchUsers(null, null, PageRequest.of(0, 20)))
        .isInstanceOf(AuthorizationDeniedException.class);

    verify(userRepository, never()).findAll(
        org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class));
  }

  @SuppressWarnings("unchecked")
  private Specification<User> anyUserSpecification() {
    return any(Specification.class);
  }

  private User user(String fullName, String email) {
    User user = User.builder()
        .fullName(fullName)
        .email(email)
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .role(UserRole.USER)
        .build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(user, "createdAt", OffsetDateTime.now());
    return user;
  }
}

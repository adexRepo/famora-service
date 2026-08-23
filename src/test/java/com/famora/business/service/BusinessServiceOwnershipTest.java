package com.famora.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.business.dto.request.TransferBusinessOwnershipRequest;
import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessMember;
import com.famora.business.enums.BusinessRole;
import com.famora.business.publisher.BusinessAuditPublisher;
import com.famora.business.repository.BusinessInvitationRepository;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.business.repository.BusinessRepository;
import com.famora.common.helper.Status;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BusinessServiceOwnershipTest {

  @Mock private BusinessRepository businessRepository;
  @Mock private BusinessMemberRepository memberRepository;
  @Mock private BusinessInvitationRepository invitationRepository;
  @Mock private BusinessPermissionService permissionService;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private BusinessAuditPublisher auditPublisher;
  @Mock private UserRepository userRepository;

  private BusinessService service;

  @BeforeEach
  void setUp() {
    service = new BusinessService(businessRepository, memberRepository, invitationRepository,
        permissionService, currentUserProvider, auditPublisher, userRepository);
  }

  @Test
  void transferLocksBothUsersAndMovesTheOwnerRoleAtomically() {
    User oldOwner = user(UUID.randomUUID());
    User newOwner = user(UUID.randomUUID());
    Business business = new Business();
    ReflectionTestUtils.setField(business, "id", UUID.randomUUID());
    business.setOwnerUserId(oldOwner.getId());
    business.setStatus(Status.ACTIVE);
    BusinessMember oldOwnerMember = member(business, oldOwner.getId(), BusinessRole.OWNER);
    BusinessMember newOwnerMember = member(business, newOwner.getId(), BusinessRole.PARTNER);

    when(currentUserProvider.getCurrentUserId()).thenReturn(oldOwner.getId());
    when(userRepository.findAllByIdForUpdate(List.of(oldOwner.getId(), newOwner.getId())))
        .thenReturn(List.of(oldOwner, newOwner));
    when(permissionService.requireAnyRole(business.getId(), oldOwner.getId(), BusinessRole.OWNER))
        .thenReturn(oldOwnerMember);
    when(memberRepository.findByBusinessIdAndUserIdAndStatus(
        business.getId(), newOwner.getId(), Status.ACTIVE)).thenReturn(Optional.of(newOwnerMember));
    when(permissionService.requireActiveBusiness(business.getId())).thenReturn(business);
    when(memberRepository.findByUserIdAndDefaultBusinessTrueAndStatus(oldOwner.getId(),
        Status.ACTIVE)).thenReturn(Optional.empty());

    var response = service.transferOwnership(business.getId(),
        new TransferBusinessOwnershipRequest(newOwner.getId()));

    assertThat(business.getOwnerUserId()).isEqualTo(newOwner.getId());
    assertThat(oldOwnerMember.getRole()).isEqualTo(BusinessRole.PARTNER);
    assertThat(newOwnerMember.getRole()).isEqualTo(BusinessRole.OWNER);
    assertThat(response.ownerUserId()).isEqualTo(newOwner.getId());
    verify(userRepository).findAllByIdForUpdate(List.of(oldOwner.getId(), newOwner.getId()));
  }

  private User user(UUID id) {
    User user = User.builder().fullName("User").email(id + "@example.com")
        .passwordHash("hash").status(UserStatus.ACTIVE).build();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private BusinessMember member(Business business, UUID userId, BusinessRole role) {
    BusinessMember member = new BusinessMember();
    member.setBusiness(business);
    member.setUserId(userId);
    member.setRole(role);
    member.setStatus(Status.ACTIVE);
    return member;
  }
}

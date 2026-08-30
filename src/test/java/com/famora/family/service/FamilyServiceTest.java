package com.famora.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.audit.service.AuditLogService;
import com.famora.common.helper.Status;
import com.famora.family.dto.JoinFamilyRequest;
import com.famora.family.entity.Family;
import com.famora.family.entity.FamilyInvitation;
import com.famora.family.entity.FamilyMember;
import com.famora.family.helper.FamilyMemberRole;
import com.famora.family.helper.FamilyMemberStatus;
import com.famora.family.helper.InvitationStatus;
import com.famora.family.repository.FamilyInvitationRepository;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.family.repository.FamilyRepository;
import com.famora.security.CurrentUserProvider;
import com.famora.security.TokenHashService;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.time.OffsetDateTime;
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
class FamilyServiceTest {

  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private FamilyRepository familyRepository;
  @Mock private AuditLogService auditLogService;
  @Mock private FamilyMemberRepository familyMemberRepository;
  @Mock private FamilyInvitationRepository familyInvitationRepository;
  @Mock private FamilyMembershipPolicyService membershipPolicyService;
  @Mock private UserRepository userRepository;

  private final TokenHashService tokenHashService = new TokenHashService();
  private FamilyService service;
  private User user;

  @BeforeEach
  void setUp() {
    service = new FamilyService(currentUserProvider, familyRepository, auditLogService,
        familyMemberRepository, familyInvitationRepository, membershipPolicyService,
        tokenHashService, userRepository);
    user = User.builder().fullName("Member").email("member@example.com")
        .passwordHash("hash").status(UserStatus.ACTIVE).build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
  }

  @Test
  void joinConsumesActiveInvitationUsingMockedRepositories() {
    String rawCode = "FAM-ABC123";
    Family family = Family.builder().name("Family").ownerUser(user).status(Status.ACTIVE).build();
    ReflectionTestUtils.setField(family, "id", UUID.randomUUID());
    FamilyInvitation invitation = FamilyInvitation.builder()
        .family(family)
        .inviteCodeHash(tokenHashService.sha256(rawCode))
        .role(FamilyMemberRole.MEMBER)
        .status(InvitationStatus.ACTIVE)
        .expiresAt(OffsetDateTime.now().plusDays(1))
        .createdBy(user)
        .build();

    when(currentUserProvider.getCurrentUserId()).thenReturn(user.getId());
    when(userRepository.findAllByIdForUpdate(List.of(user.getId()))).thenReturn(List.of(user));
    when(familyInvitationRepository.findByInviteCodeHashAndStatus(
        tokenHashService.sha256(rawCode), InvitationStatus.ACTIVE))
        .thenReturn(Optional.of(invitation));
    when(familyMemberRepository.existsByFamilyIdAndUserIdAndStatus(
        family.getId(), user.getId(), FamilyMemberStatus.ACTIVE)).thenReturn(false);
    when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), user.getId()))
        .thenReturn(Optional.empty());
    when(familyMemberRepository.existsByUserIdAndDefaultFamilyTrueAndStatus(
        user.getId(), FamilyMemberStatus.ACTIVE)).thenReturn(false);
    when(familyMemberRepository.save(any())).thenAnswer(invocation -> {
      FamilyMember saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
      return saved;
    });

    var response = service.joinFamily(new JoinFamilyRequest(rawCode));

    assertThat(response.id()).isEqualTo(family.getId());
    assertThat(response.role()).isEqualTo(FamilyMemberRole.MEMBER.name());
    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.USED);
    assertThat(invitation.getUsedByUser()).isSameAs(user);
    verify(familyInvitationRepository).save(invitation);
  }
}

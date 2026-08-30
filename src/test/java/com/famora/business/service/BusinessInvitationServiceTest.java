package com.famora.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.business.dto.request.CreateInvitationRequest;
import com.famora.business.dto.request.JoinBusinessRequest;
import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessInvitation;
import com.famora.business.entity.BusinessMember;
import com.famora.business.enums.InvitationStatus;
import com.famora.business.enums.BusinessRole;
import com.famora.business.publisher.BusinessAuditPublisher;
import com.famora.business.repository.BusinessInvitationRepository;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.security.CurrentUserProvider;
import com.famora.security.TokenHashService;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BusinessInvitationServiceTest {

  @Mock private BusinessInvitationRepository invitationRepository;
  @Mock private BusinessMemberRepository memberRepository;
  @Mock private BusinessPermissionService permissionService;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private BusinessAuditPublisher auditPublisher;
  @Mock private UserRepository userRepository;

  private final TokenHashService tokenHashService = new TokenHashService();
  private BusinessInvitationService service;
  private User user;
  private Business business;

  @BeforeEach
  void setUp() {
    service = new BusinessInvitationService(invitationRepository, memberRepository,
        permissionService, currentUserProvider, auditPublisher, userRepository, tokenHashService);
    user = User.builder().fullName("Owner").email("owner@example.com")
        .passwordHash("hash").status(UserStatus.ACTIVE).build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    business = new Business();
    business.setName("Shop");
    ReflectionTestUtils.setField(business, "id", UUID.randomUUID());
    when(invitationRepository.save(any())).thenAnswer(invocation -> {
      BusinessInvitation saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
      return saved;
    });
  }

  @Test
  void createReturnsRawTokenOnceAndPersistsOnlyItsSha256Hash() {
    when(currentUserProvider.getCurrentUser()).thenReturn(user);
    when(permissionService.requireActiveBusiness(business.getId())).thenReturn(business);

    var response = service.create(business.getId(),
        new CreateInvitationRequest(null, null, BusinessRole.STAFF, null));

    assertThat(response.invitationToken()).hasSize(43);
    assertThat(response.invitation().id()).isNotNull();
    ArgumentCaptor<BusinessInvitation> captor = ArgumentCaptor.forClass(BusinessInvitation.class);
    verify(invitationRepository).save(captor.capture());
    assertThat(captor.getValue().getInvitationCodeHash())
        .isEqualTo(tokenHashService.sha256(response.invitationToken()))
        .doesNotContain(response.invitationToken());
  }

  @Test
  void joinConsumesPendingInvitationUsingMockedRepositories() {
    String rawToken = "join-token";
    BusinessInvitation invitation = new BusinessInvitation();
    invitation.setBusiness(business);
    invitation.setRole(BusinessRole.STAFF);
    invitation.setInvitationStatus(InvitationStatus.PENDING);
    invitation.setInvitationCodeHash(tokenHashService.sha256(rawToken));
    invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
    invitation.setInvitedByUserId(UUID.randomUUID());

    when(currentUserProvider.getCurrentUserId()).thenReturn(user.getId());
    when(userRepository.findAllByIdForUpdate(List.of(user.getId()))).thenReturn(List.of(user));
    when(invitationRepository.findByInvitationCodeHash(tokenHashService.sha256(rawToken)))
        .thenReturn(Optional.of(invitation));
    when(memberRepository.existsByBusinessIdAndUserIdAndStatus(any(), any(), any()))
        .thenReturn(false);
    when(memberRepository.findByBusinessIdAndUserId(business.getId(), user.getId()))
        .thenReturn(Optional.empty());
    when(memberRepository.existsByUserIdAndDefaultBusinessTrueAndStatus(any(), any()))
        .thenReturn(false);
    when(memberRepository.save(any())).thenAnswer(invocation -> {
      BusinessMember saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
      return saved;
    });

    var response = service.join(new JoinBusinessRequest(rawToken));

    assertThat(response.userId()).isEqualTo(user.getId());
    assertThat(response.role()).isEqualTo(BusinessRole.STAFF);
    assertThat(invitation.getInvitationStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    assertThat(invitation.getAcceptedByUserId()).isEqualTo(user.getId());
    verify(invitationRepository).save(invitation);
  }
}

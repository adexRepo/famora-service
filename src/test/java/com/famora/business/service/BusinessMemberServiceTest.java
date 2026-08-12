package com.famora.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.business.dto.response.BusinessMemberResponse;
import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessInvitation;
import com.famora.business.entity.BusinessMember;
import com.famora.business.enums.BusinessRole;
import com.famora.business.enums.InvitationStatus;
import com.famora.business.publisher.BusinessAuditPublisher;
import com.famora.business.repository.BusinessInvitationRepository;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.common.helper.Status;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import com.famora.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

class BusinessMemberServiceTest {

  private final BusinessMemberRepository memberRepository = mock(BusinessMemberRepository.class);
  private final BusinessPermissionService permissionService = mock(BusinessPermissionService.class);
  private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
  private final BusinessAuditPublisher auditPublisher = mock(BusinessAuditPublisher.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final BusinessInvitationRepository invitationRepository =
      mock(BusinessInvitationRepository.class);
  private final BusinessMemberService service = new BusinessMemberService(
      memberRepository,
      permissionService,
      currentUserProvider,
      auditPublisher,
      userRepository,
      invitationRepository
  );

  @Test
  @SuppressWarnings("unchecked")
  void listIncludesEachMembersFullName() {
    UUID businessId = UUID.randomUUID();
    UUID currentUserId = UUID.randomUUID();
    UUID memberUserId = UUID.randomUUID();
    PageRequest pageable = PageRequest.of(0, 20);

    Business business = new Business();
    business.setId(businessId);
    BusinessMember member = new BusinessMember();
    member.setId(UUID.randomUUID());
    member.setBusiness(business);
    member.setUserId(memberUserId);
    member.setRole(BusinessRole.STAFF);
    member.setStatus(Status.ACTIVE);

    User memberUser = new User();
    memberUser.setId(memberUserId);
    memberUser.setFullName("Ayu Lestari");

    BusinessInvitation invitation = new BusinessInvitation();
    invitation.setAcceptedByUserId(memberUserId);
    invitation.setInvitedPhone("+62 812-3456-7890");

    when(currentUserProvider.getCurrentUserId()).thenReturn(currentUserId);
    when(memberRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(member), pageable, 1));
    when(userRepository.findAllById(any())).thenReturn(List.of(memberUser));
    when(invitationRepository.findAcceptedByUsers(
        eq(businessId), any(), eq(InvitationStatus.ACCEPTED), eq(Status.ACTIVE)))
        .thenReturn(List.of(invitation));

    Page<BusinessMemberResponse> result = service.list(businessId, pageable);

    assertThat(result.getContent()).singleElement()
        .extracting(BusinessMemberResponse::name)
        .isEqualTo("Ayu Lestari");
    assertThat(result.getContent()).singleElement()
        .extracting(BusinessMemberResponse::phone)
        .isEqualTo("+62 812-3456-7890");
    verify(permissionService).requireCanView(businessId, currentUserId);
  }
}

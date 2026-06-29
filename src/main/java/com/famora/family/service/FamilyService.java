package com.famora.family.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.helper.Status;
import com.famora.family.dto.CreateFamilyRequest;
import com.famora.family.dto.CreateInvitationRequest;
import com.famora.family.dto.FamilyResponse;
import com.famora.family.dto.InvitationResponse;
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
import com.famora.security.CurrentUserService;
import com.famora.user.entity.User;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FamilyService {
  
  private final CurrentUserService currentUserService;
  private final FamilyRepository familyRepository;
  private final AuditLogService auditLogService;
  private final FamilyMemberRepository familyMemberRepository;
  private final FamilyInvitationRepository familyInvitationRepository;
  private final SecureRandom random = new SecureRandom();
  
  @Transactional(readOnly = true)
  public List<FamilyResponse> getMyFamilies() {
    User user = currentUserService.getCurrentUser();
    return familyMemberRepository.findActiveFamiliesByUserId(user.getId());
  }
  
  @Transactional
  public FamilyResponse createFamily(CreateFamilyRequest request) {
    User user = currentUserService.getCurrentUser();
    
    Family family = Family.builder()
        .name(request.name().trim())
        .ownerUser(user)
        .status(Status.ACTIVE)
        .build();
    
    familyRepository.save(family);
    
    FamilyMember member = FamilyMember.builder()
        .family(family)
        .user(user)
        .role(FamilyMemberRole.OWNER)
        .status(FamilyMemberStatus.ACTIVE)
        .joinedAt(OffsetDateTime.now())
        .build();
    
    familyMemberRepository.save(member);
    
    auditLogService.log(family, user, AuditAction.FAMILY_CREATED,
        "families", family.getId(),
        "{\"family\":\"" + family.getId() + "\",\"familyMemberId\":\"" + member.getId() + "\"}");
    
    return new FamilyResponse(
        family.getId(),
        family.getName(),
        FamilyMemberRole.OWNER.name()
    );
  }
  
  @Transactional
  public InvitationResponse createInvitation(UUID familyId, CreateInvitationRequest request) {
    User user = currentUserService.getCurrentUser();
    FamilyMember requester = familyMemberRepository.findByFamilyIdAndUserIdAndStatus(familyId,
            user.getId(), FamilyMemberStatus.ACTIVE)
        .orElseThrow(() -> new SecurityException("Access denied"));
    if (requester.getRole() != FamilyMemberRole.OWNER
        && requester.getRole() != FamilyMemberRole.ADMIN) {
      throw new SecurityException("Only OWNER or ADMIN can invite member");
    }
    String inviteCode = generateInviteCode();
    FamilyInvitation invitation = FamilyInvitation.builder()
        .family(requester.getFamily())
        .inviteCode(inviteCode)
        .role(request.role())
        .status(InvitationStatus.ACTIVE)
        .expiresAt(OffsetDateTime.now()
            .plusDays(2)).createdBy(user).build();
    familyInvitationRepository.save(invitation);
    
    auditLogService.log(requester.getFamily(), user, AuditAction.FAMILY_MEMBER_INVITED,
        "family_invitations", invitation.getId(),
        """
            {
              "family" : "%s",
              "userId" : "%s",
              "invitationCode" : "%s",
            }
            """.formatted(requester.getFamily().getId(), user.getId(), invitation.getInviteCode()));
    
    return new InvitationResponse(inviteCode, invitation.getExpiresAt(),
        invitation.getRole().name());
  }
  
  @Transactional
  public FamilyResponse joinFamily(JoinFamilyRequest request) {
    User user = currentUserService.getCurrentUser();
    FamilyInvitation invitation = familyInvitationRepository.findByInviteCodeAndStatus(
            request.inviteCode(), InvitationStatus.ACTIVE)
        .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));
    if (invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
      invitation.setStatus(InvitationStatus.EXPIRED);
      familyInvitationRepository.save(invitation);
      throw new IllegalArgumentException("Invite code expired");
    }
    boolean alreadyMember = familyMemberRepository.existsByFamilyIdAndUserIdAndStatus(
        invitation.getFamily().getId(), user.getId(), FamilyMemberStatus.ACTIVE);
    if (alreadyMember) {
      throw new IllegalArgumentException("User already joined this family");
    }
    FamilyMember member = FamilyMember.builder().family(invitation.getFamily()).user(user)
        .role(invitation.getRole()).status(FamilyMemberStatus.ACTIVE).joinedAt(OffsetDateTime.now())
        .build();
    familyMemberRepository.save(member);
    invitation.setStatus(InvitationStatus.USED);
    invitation.setUsedByUser(user);
    invitation.setUsedAt(OffsetDateTime.now());
    familyInvitationRepository.save(invitation);
    
    auditLogService.log(member.getFamily(), user, AuditAction.FAMILY_MEMBER_JOINED,
        "family_members", invitation.getId(),
        """
            {
              "family" : "%s",
              "userId" : "%s",
              "invitationCode" : "%s",
            }
            """.formatted(member.getFamily().getId(), user.getId(), invitation.getInviteCode()));
    
    return new FamilyResponse(invitation.getFamily().getId(), invitation.getFamily().getName(),
        member.getRole().name());
  }
  
  private String generateInviteCode() {
    String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    StringBuilder sb = new StringBuilder("FAM-");
    for (int i = 0; i < 6; i++) {
      sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
    }
    return sb.toString();
  }
}

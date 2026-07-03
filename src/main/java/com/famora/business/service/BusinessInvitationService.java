package com.famora.business.service;

import static com.famora.business.constant.BusinessAuditConstants.INVITATION;
import static com.famora.business.constant.BusinessAuditConstants.INVITATION_STATUS;
import static com.famora.business.constant.BusinessAuditConstants.ROLE;

import com.famora.audit.entity.AuditAction;
import com.famora.business.dto.request.CreateInvitationRequest;
import com.famora.business.dto.request.JoinBusinessRequest;
import com.famora.business.dto.response.BusinessInvitationResponse;
import com.famora.business.dto.response.BusinessMemberResponse;
import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessInvitation;
import com.famora.business.entity.BusinessMember;
import com.famora.business.enums.BusinessRole;
import com.famora.business.enums.InvitationStatus;
import com.famora.business.publisher.BusinessAuditPublisher;
import com.famora.business.repository.BusinessInvitationRepository;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.business.spec.BusinessInvitationSpecifications;
import com.famora.common.exception.BusinessException;
import com.famora.common.helper.Status;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessInvitationService {
  
  private static final SecureRandom RANDOM = new SecureRandom();
  private final BusinessInvitationRepository invitationRepository;
  private final BusinessMemberRepository memberRepository;
  private final BusinessPermissionService permissionService;
  private final CurrentUserProvider currentUserProvider;
  private final BusinessAuditPublisher auditPublisher;
  
  @Transactional
  public BusinessInvitationResponse create(UUID businessId, CreateInvitationRequest req) {
    User user = currentUserProvider.getCurrentUser();
    UUID userId = user.getId();
    permissionService.requireCanInviteMember(businessId, userId);
    if (req.role() == BusinessRole.OWNER) {
      throw BusinessException.validation("Cannot invite OWNER role");
    }
    
    Business business = permissionService.requireActiveBusiness(businessId);
    
    BusinessInvitation i = new BusinessInvitation();
    i.setBusiness(business);
    i.setInvitedEmail(req.invitedEmail());
    i.setInvitedPhone(req.invitedPhone());
    i.setRole(req.role());
    i.setInvitationCode(uniqueCode());
    i.setInvitationStatus(InvitationStatus.PENDING);
    i.setExpiresAt(req.expiresAt());
    i.setInvitedByUserId(userId);
    i.setCreatedBy(user);
    i = invitationRepository.save(i);
    publishInvitationAudit(user, businessId, AuditAction.BUSINESS_INVITATION_CREATED, i);
    return BusinessMapper.invitation(i);
  }
  
  @Transactional(readOnly = true)
  public Page<BusinessInvitationResponse> list(UUID businessId, Pageable pageable) {
    permissionService.requireCanInviteMember(businessId, currentUserProvider.getCurrentUserId());
    return invitationRepository.findAll(
        BusinessInvitationSpecifications.belongsToBusiness(businessId)
            .and(BusinessInvitationSpecifications.status(Status.ACTIVE))
            .and(BusinessInvitationSpecifications.invitationStatus(InvitationStatus.PENDING)),
        pageable).map(BusinessMapper::invitation);
  }
  
  @Transactional
  public void cancel(UUID businessId, UUID invitationId) {
    User user = currentUserProvider.getCurrentUser();
    permissionService.requireCanInviteMember(businessId, user.getId());
    
    BusinessInvitation i = invitationRepository.findById(invitationId)
        .filter(x -> x.getBusiness().getId().equals(businessId))
        .orElseThrow(() -> BusinessException.notFound("Invitation not found"));
    i.setInvitationStatus(InvitationStatus.CANCELLED);
    i.setUpdatedBy(user);
    invitationRepository.save(i);
    publishInvitationAudit(user, businessId, AuditAction.BUSINESS_INVITATION_CANCELLED, i);
  }
  
  @Transactional
  public BusinessMemberResponse join(JoinBusinessRequest req) {
    User user = currentUserProvider.getCurrentUser();
    UUID userId = user.getId();
    BusinessInvitation i = invitationRepository.findByInvitationCode(req.invitationCode().trim())
        .orElseThrow(() -> BusinessException.notFound("Invitation code not found"));
    if (i.getInvitationStatus() != InvitationStatus.PENDING) {
      throw BusinessException.validation("Invitation is not pending");
    }
    if (i.getExpiresAt() != null && i.getExpiresAt().isBefore(LocalDateTime.now())) {
      i.setInvitationStatus(InvitationStatus.EXPIRED);
      i.setUpdatedBy(user);
      invitationRepository.save(i);
      throw BusinessException.validation("Invitation is expired");
    }
    if (memberRepository.existsByBusinessIdAndUserIdAndStatus(i.getBusiness().getId(), userId,
        Status.ACTIVE)) {
      throw BusinessException.conflict("User already active member");
    }
    
    BusinessMember m = memberRepository.findByBusinessIdAndUserId(i.getBusiness().getId(), userId)
        .orElseGet(BusinessMember::new);
    boolean newMember = m.getId() == null;
    m.setBusiness(i.getBusiness());
    m.setUserId(userId);
    m.setRole(i.getRole());
    m.setStatus(Status.ACTIVE);
    m.setInvitedByUserId(i.getInvitedByUserId());
    m.setJoinedAt(LocalDateTime.now());
    if (newMember) {
      m.setCreatedBy(user);
    } else {
      m.setUpdatedBy(user);
    }
    m = memberRepository.save(m);
    
    i.setInvitationStatus(InvitationStatus.ACCEPTED);
    i.setAcceptedByUserId(userId);
    i.setUpdatedBy(user);
    invitationRepository.save(i);
    publishInvitationAudit(user, i.getBusiness().getId(),
        AuditAction.BUSINESS_INVITATION_ACCEPTED, i);
    return BusinessMapper.member(m);
  }
  
  private void publishInvitationAudit(User user, UUID businessId, AuditAction action,
      BusinessInvitation invitation) {
    auditPublisher.publishBusinessEvent(
        user.getId(),
        businessId,
        action,
        INVITATION,
        invitation.getId(),
        Map.of(
            ROLE, invitation.getRole(),
            INVITATION_STATUS, invitation.getInvitationStatus()
        )
    );
  }
  
  private String uniqueCode() {
    for (int i = 0; i < 20; i++) {
      String code = "DKR-" + (100000 + RANDOM.nextInt(900000));
      if (!invitationRepository.existsByInvitationCode(code)) {
        return code;
      }
    }
    throw BusinessException.conflict("Cannot generate invitation code");
  }
}

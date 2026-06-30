package com.famora.business.service;

import static com.famora.business.constant.BusinessAuditConstants.MEMBER;
import static com.famora.business.constant.BusinessAuditConstants.MEMBER_USER_ID;
import static com.famora.business.constant.BusinessAuditConstants.ROLE;
import static com.famora.business.constant.BusinessAuditConstants.STATUS;

import com.famora.audit.entity.AuditAction;
import com.famora.business.dto.request.UpdateMemberRoleRequest;
import com.famora.business.dto.response.BusinessMemberResponse;
import com.famora.business.entity.BusinessMember;
import com.famora.business.enums.BusinessRole;
import com.famora.business.publisher.BusinessAuditPublisher;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.business.spec.BusinessMemberSpecifications;
import com.famora.common.exception.BusinessException;
import com.famora.common.helper.Status;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessMemberService {
  
  private final BusinessMemberRepository memberRepository;
  private final BusinessPermissionService permissionService;
  private final CurrentUserProvider currentUserProvider;
  private final BusinessAuditPublisher auditPublisher;
  
  @Transactional(readOnly = true)
  public Page<BusinessMemberResponse> list(UUID businessId, Pageable pageable) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    return memberRepository.findAll(
        BusinessMemberSpecifications.belongsToBusiness(businessId)
            .and(BusinessMemberSpecifications.status(Status.ACTIVE)),
        pageable).map(BusinessMapper::member);
  }
  
  @Transactional
  public BusinessMemberResponse updateRole(UUID businessId, UUID memberId,
      UpdateMemberRoleRequest req) {
    User user = currentUserProvider.getCurrentUser();
    permissionService.requireCanManageMemberRole(businessId, user.getId());
    if (req.role() == BusinessRole.OWNER) {
      throw BusinessException.validation("Use transfer ownership flow for OWNER role");
    }
    BusinessMember m = memberRepository.findById(memberId)
        .filter(x -> x.getBusiness().getId().equals(businessId))
        .orElseThrow(() -> BusinessException.notFound("Business member not found"));
    if (m.getRole() == BusinessRole.OWNER) {
      throw BusinessException.validation("Cannot change OWNER role");
    }
    m.setRole(req.role());
    m.setUpdatedBy(user);
    m = memberRepository.save(m);
    publishMemberAudit(user, businessId, AuditAction.BUSINESS_MEMBER_ROLE_UPDATED, m);
    return BusinessMapper.member(m);
  }
  
  @Transactional
  public void remove(UUID businessId, UUID memberId) {
    User user = currentUserProvider.getCurrentUser();
    permissionService.requireCanManageMemberRole(businessId, user.getId());
    BusinessMember m = memberRepository.findById(memberId)
        .filter(x -> x.getBusiness().getId().equals(businessId))
        .orElseThrow(() -> BusinessException.notFound("Business member not found"));
    if (m.getRole() == BusinessRole.OWNER) {
      throw BusinessException.validation("Cannot remove OWNER");
    }
    m.setStatus(Status.DELETED);
    m.setUpdatedBy(user);
    memberRepository.save(m);
    publishMemberAudit(user, businessId, AuditAction.BUSINESS_MEMBER_REMOVED, m);
  }
  
  private void publishMemberAudit(User user, UUID businessId, AuditAction action,
      BusinessMember m) {
    auditPublisher.publishBusinessEvent(
        user.getId(),
        businessId,
        action,
        MEMBER,
        m.getId(),
        Map.of(MEMBER_USER_ID, m.getUserId(), ROLE, m.getRole(), STATUS, m.getStatus())
    );
  }
}

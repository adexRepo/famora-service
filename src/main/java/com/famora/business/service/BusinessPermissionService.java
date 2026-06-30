package com.famora.business.service;

import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessMember;
import com.famora.business.enums.BusinessRole;
import com.famora.business.helper.BusinessReportAccessContext;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.business.repository.BusinessRepository;
import com.famora.common.exception.BusinessException;
import com.famora.common.helper.Status;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessPermissionService {
  
  private final BusinessRepository businessRepository;
  private final BusinessMemberRepository memberRepository;
  
  
  public Business requireActiveBusiness(UUID businessId) {
    return businessRepository.findByIdAndStatusNot(businessId, Status.DELETED)
        .orElseThrow(() -> BusinessException.notFound("Business not found"));
  }
  
  public BusinessMember requireActiveMember(UUID businessId, UUID userId) {
    requireActiveBusiness(businessId);
    return memberRepository.findByBusinessIdAndUserIdAndStatus(businessId, userId,
            Status.ACTIVE)
        .orElseThrow(
            () -> BusinessException.forbidden("You are not an active member of this business"));
  }
  
  public BusinessMember requireAnyRole(UUID businessId, UUID userId, BusinessRole... roles) {
    BusinessMember member = requireActiveMember(businessId, userId);
    if (!Arrays.asList(roles).contains(member.getRole())) {
      throw BusinessException.forbidden("Your business role is not allowed for this action");
    }
    return member;
  }
  
  public BusinessMember requireCanView(UUID businessId, UUID userId) {
    return requireActiveMember(businessId, userId);
  }
  
  public BusinessMember requireCanManageProduct(UUID businessId, UUID userId) {
    BusinessMember m = requireActiveMember(businessId, userId);
    if (!m.getRole().canManageProduct()) {
      throw BusinessException.forbidden("Only OWNER or PARTNER can manage products");
    }
    return m;
  }
  
  public BusinessMember requireCanSubmitDailyReport(UUID businessId, UUID userId) {
    BusinessMember m = requireActiveMember(businessId, userId);
    if (!m.getRole().canSubmitDailyReport()) {
      throw BusinessException.forbidden("Your role cannot submit daily report");
    }
    return m;
  }
  
  public BusinessMember requireCanManageExpense(UUID businessId, UUID userId) {
    BusinessMember m = requireActiveMember(businessId, userId);
    if (!m.getRole().canManageExpense()) {
      throw BusinessException.forbidden("Your role cannot manage manual expenses");
    }
    return m;
  }
  
  public BusinessMember requireCanInviteMember(UUID businessId, UUID userId) {
    BusinessMember m = requireActiveMember(businessId, userId);
    if (!m.getRole().canInviteMember()) {
      throw BusinessException.forbidden("Your role cannot invite members");
    }
    return m;
  }
  
  public BusinessMember requireCanManageMemberRole(UUID businessId, UUID userId) {
    BusinessMember m = requireActiveMember(businessId, userId);
    if (!m.getRole().canManageMemberRole()) {
      throw BusinessException.forbidden("Only OWNER can manage member role");
    }
    return m;
  }
}

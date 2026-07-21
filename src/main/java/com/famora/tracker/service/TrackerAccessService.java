package com.famora.tracker.service;

import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessMember;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.business.repository.BusinessRepository;
import com.famora.common.exception.BusinessException;
import com.famora.common.helper.Status;
import com.famora.family.entity.Family;
import com.famora.family.entity.FamilyMember;
import com.famora.family.helper.FamilyMemberStatus;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.family.repository.FamilyRepository;
import com.famora.tracker.entity.Tracker;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackerAccessService {
  
  private final FamilyRepository familyRepository;
  private final FamilyMemberRepository familyMemberRepository;
  private final BusinessRepository businessRepository;
  private final BusinessMemberRepository businessMemberRepository;
  private final UserRepository userRepository;
  
  public TrackerScopeContext requireScopeAccess(TrackerScopeType scopeType, UUID scopeId,
      User user) {
    if (scopeType == null) {
      throw BusinessException.validation("scopeType is required");
    }
    return switch (scopeType) {
      case FAMILY -> requireFamilyScope(scopeId, user);
      case BUSINESS -> requireBusinessScope(scopeId, user);
      case PERSONAL -> new TrackerScopeContext(scopeType, null, null, null, null, user);
    };
  }
  
  public void requireCanAccess(Tracker tracker, User user) {
    switch (tracker.getScopeType()) {
      case FAMILY -> requireFamilyScope(tracker.getFamily().getId(), user);
      case BUSINESS -> requireBusinessScope(tracker.getBusiness().getId(), user);
      default -> {
        // PERSONAL
        if (!tracker.getOwnerUser().getId().equals(user.getId())) {
          throw BusinessException.forbidden("You cannot access this personal tracker");
        }
      }
    }
  }
  
  public User resolveAssignedUser(TrackerScopeContext scope, UUID assignedUserId) {
    if (assignedUserId == null) {
      return null;
    }
    User assignedUser = userRepository.findByIdAndStatus(assignedUserId, UserStatus.ACTIVE)
        .orElseThrow(() -> BusinessException.validation("Assigned user is not active"));
    switch (scope.scopeType()) {
      case FAMILY -> familyMemberRepository.findByFamilyIdAndUserIdAndStatus(
              scope.family().getId(), assignedUserId, FamilyMemberStatus.ACTIVE)
          .orElseThrow(() -> BusinessException.validation(
              "Assigned user is not active member of selected family"));
      case BUSINESS -> businessMemberRepository.findByBusinessIdAndUserIdAndStatus(
              scope.business().getId(), assignedUserId, Status.ACTIVE)
          .orElseThrow(() -> BusinessException.validation(
              "Assigned user is not active member of selected business"));
      default -> {
        // PERSONAL
        if (!assignedUserId.equals(scope.user().getId())) {
          throw BusinessException.validation("Personal tracker can only be assigned to owner");
        }
      }
    }
    return assignedUser;
  }
  
  public FamilyMember resolveAssignedFamilyMember(TrackerScopeContext scope, UUID memberId) {
    if (memberId == null) {
      return null;
    }
    if (scope.scopeType() != TrackerScopeType.FAMILY) {
      throw BusinessException.validation("assignedMemberId is only valid for FAMILY scope");
    }
    FamilyMember member = familyMemberRepository.findById(memberId)
        .filter(item -> item.getFamily().getId().equals(scope.family().getId()))
        .filter(item -> item.getStatus() == FamilyMemberStatus.ACTIVE)
        .orElseThrow(() -> BusinessException.validation("Assigned family member must belong to selected family"));
    return member;
  }
  
  public BusinessMember resolveAssignedBusinessMember(TrackerScopeContext scope, UUID memberId) {
    if (memberId == null) {
      return null;
    }
    if (scope.scopeType() != TrackerScopeType.BUSINESS) {
      throw BusinessException.validation("assignedMemberId is only valid for BUSINESS scope");
    }
    return businessMemberRepository.findById(memberId)
        .filter(item -> item.getBusiness().getId().equals(scope.business().getId()))
        .filter(item -> item.getStatus() == Status.ACTIVE)
        .orElseThrow(() -> BusinessException.validation(
            "Assigned business member must belong to selected business"));
  }
  
  private TrackerScopeContext requireFamilyScope(UUID scopeId, User user) {
    if (scopeId == null) {
      throw BusinessException.validation("scopeId is required for FAMILY tracker");
    }
    Family family = familyRepository.findById(scopeId)
        .orElseThrow(() -> BusinessException.notFound("Family not found"));
    FamilyMember member = familyMemberRepository.findByFamilyIdAndUserIdAndStatus(scopeId,
            user.getId(), FamilyMemberStatus.ACTIVE)
        .orElseThrow(() -> BusinessException.forbidden("You are not active member of this family"));
    return new TrackerScopeContext(TrackerScopeType.FAMILY, family, null, member, null, user);
  }
  
  private TrackerScopeContext requireBusinessScope(UUID scopeId, User user) {
    if (scopeId == null) {
      throw BusinessException.validation("scopeId is required for BUSINESS tracker");
    }
    Business business = businessRepository.findByIdAndStatusNot(scopeId, Status.DELETED)
        .orElseThrow(() -> BusinessException.notFound("Business not found"));
    BusinessMember member = businessMemberRepository.findByBusinessIdAndUserIdAndStatus(scopeId,
            user.getId(), Status.ACTIVE)
        .orElseThrow(
            () -> BusinessException.forbidden("You are not active member of this business"));
    return new TrackerScopeContext(TrackerScopeType.BUSINESS, null, business, null, member, user);
  }
}

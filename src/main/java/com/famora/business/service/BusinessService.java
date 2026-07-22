package com.famora.business.service;

import static com.famora.business.constant.BusinessAuditConstants.BUSINESS;
import static com.famora.business.constant.BusinessAuditConstants.BUSINESS_NAME;
import static com.famora.business.constant.BusinessAuditConstants.DEFAULT_CURRENCY;
import static com.famora.business.constant.BusinessAuditConstants.IS_DEFAULT;
import static com.famora.business.constant.BusinessAuditConstants.STATUS;

import com.famora.audit.entity.AuditAction;
import com.famora.business.constant.BusinessDefaults;
import com.famora.business.dto.request.CreateBusinessRequest;
import com.famora.business.dto.request.UpdateBusinessRequest;
import com.famora.business.dto.response.BusinessResponse;
import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessMember;
import com.famora.business.enums.BusinessRole;
import com.famora.business.enums.InvitationStatus;
import com.famora.business.publisher.BusinessAuditPublisher;
import com.famora.business.repository.BusinessInvitationRepository;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.business.repository.BusinessRepository;
import com.famora.business.spec.BusinessSpecifications;
import com.famora.common.exception.BusinessException;
import com.famora.common.helper.Status;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessService {
  
  private final BusinessRepository businessRepository;
  private final BusinessMemberRepository memberRepository;
  private final BusinessInvitationRepository invitationRepository;
  private final BusinessPermissionService permissionService;
  private final CurrentUserProvider currentUserProvider;
  private final BusinessAuditPublisher auditPublisher;
  
  @Transactional
  public BusinessResponse create(CreateBusinessRequest request) {
    User userId = currentUserProvider.getCurrentUser();
    boolean shouldBeDefault = !hasDefaultBusiness(userId.getId());
    Business b = new Business();
    b.setName(request.name().trim());
    b.setBusinessType(
        blank(request.businessType()) ? BusinessDefaults.BUSINESS_TYPE
            : request.businessType().trim());
    b.setDefaultCurrency(
        blank(request.defaultCurrency()) ? BusinessDefaults.CURRENCY
            : request.defaultCurrency().trim().toUpperCase());
    b.setOwnerUserId(userId.getId());
    b.setPrimaryFamilyId(request.primaryFamilyId());
    b.setDescription(request.description());
    b.setAddress(request.address());
    b.setContact(trimToNull(request.contact()));
    b.setCreatedBy(userId);
    b = businessRepository.save(b);
    
    BusinessMember owner = new BusinessMember();
    owner.setBusiness(b);
    owner.setUserId(userId.getId());
    owner.setRole(BusinessRole.OWNER);
    owner.setStatus(Status.ACTIVE);
    owner.setDefaultBusiness(shouldBeDefault);
    owner.setJoinedAt(LocalDateTime.now());
    owner.setCreatedBy(userId);
    memberRepository.save(owner);
    publishBusinessAudit(userId, b.getId(), AuditAction.BUSINESS_CREATED, b,
        Map.of(BUSINESS_NAME, b.getName(), DEFAULT_CURRENCY, b.getDefaultCurrency(), IS_DEFAULT,
            owner.isDefaultBusiness()));
    
    return BusinessMapper.business(b, owner.isDefaultBusiness(), owner.getRole());
  }
  
  @Transactional(readOnly = true)
  public Page<BusinessResponse> list(Pageable pageable) {
    UUID userId = currentUserProvider.getCurrentUserId();
    UUID defaultBusinessId = memberRepository.findByUserIdAndDefaultBusinessTrueAndStatus(userId,
            Status.ACTIVE)
        .map(member -> member.getBusiness().getId())
        .orElse(null);
    return businessRepository.findAll(
            BusinessSpecifications.accessibleByUser(userId),
            defaultSort(pageable))
        .map(business -> BusinessMapper.business(business, business.getId().equals(defaultBusinessId),
            memberRepository.findByBusinessIdAndUserIdAndStatus(business.getId(), userId,
                    Status.ACTIVE)
                .map(BusinessMember::getRole)
                .orElse(null)));
  }
  
  @Transactional
  public BusinessResponse getDefaultBusiness() {
    User user = currentUserProvider.getCurrentUser();
    UUID userId = user.getId();
    BusinessMember member = memberRepository.findByUserIdAndDefaultBusinessTrueAndStatus(userId,
            Status.ACTIVE)
        .filter(candidate -> {
          if (candidate.getBusiness().getStatus() == Status.ACTIVE) {
            return true;
          }
          candidate.setDefaultBusiness(false);
          candidate.setUpdatedBy(user);
          memberRepository.save(candidate);
          assignReplacementDefault(userId, candidate.getBusiness().getId(), user);
          return false;
        })
        .or(() -> memberRepository.findByUserIdAndDefaultBusinessTrueAndStatus(userId,
            Status.ACTIVE)
            .filter(candidate -> candidate.getBusiness().getStatus() == Status.ACTIVE))
        .orElseThrow(() -> BusinessException.notFound("Default business not found"));
    return BusinessMapper.business(member.getBusiness(), true, member.getRole());
  }
  
  @Transactional(readOnly = true)
  public BusinessResponse get(UUID businessId) {
    UUID userId = currentUserProvider.getCurrentUserId();
    BusinessMember member = permissionService.requireCanView(businessId, userId);
    Business b = permissionService.requireActiveBusiness(businessId);
    return BusinessMapper.business(b, isDefaultBusiness(userId, businessId), member.getRole());
  }
  
  @Transactional
  public BusinessResponse update(UUID businessId, UpdateBusinessRequest request) {
    UUID userId = currentUserProvider.getCurrentUserId();
    BusinessMember member = permissionService.requireAnyRole(businessId, userId,
        BusinessRole.OWNER, BusinessRole.PARTNER);
    Business b = permissionService.requireActiveBusiness(businessId);
    b.setName(request.name().trim());
    b.setBusinessType(
        blank(request.businessType()) ? b.getBusinessType() : request.businessType().trim());
    b.setDefaultCurrency(blank(request.defaultCurrency()) ? b.getDefaultCurrency()
        : request.defaultCurrency().trim().toUpperCase());
    b.setPrimaryFamilyId(request.primaryFamilyId());
    b.setDescription(request.description());
    b.setAddress(request.address());
    b.setContact(trimToNull(request.contact()));
    User user = currentUserProvider.getCurrentUser();
    b.setUpdatedBy(user);
    Business saved = businessRepository.save(b);
    publishBusinessAudit(user, businessId, AuditAction.BUSINESS_UPDATED, saved,
        Map.of(BUSINESS_NAME, saved.getName(), DEFAULT_CURRENCY, saved.getDefaultCurrency()));
    return BusinessMapper.business(saved, isDefaultBusiness(userId, businessId), member.getRole());
  }
  
  @Transactional
  public BusinessResponse setDefaultBusiness(UUID businessId) {
    User user = currentUserProvider.getCurrentUser();
    BusinessMember member = memberRepository.findByBusinessIdAndUserIdAndStatus(businessId,
            user.getId(), Status.ACTIVE)
        .orElseThrow(() -> BusinessException.notFound("Active business member not found"));
    if (member.getBusiness().getStatus() != Status.ACTIVE) {
      throw BusinessException.notFound("Active business not found");
    }
    memberRepository.clearDefaultByUserId(user.getId());
    member.setDefaultBusiness(true);
    member.setUpdatedBy(user);
    memberRepository.save(member);
    publishBusinessAudit(user, businessId, AuditAction.BUSINESS_DEFAULT_SET, member.getBusiness(),
        Map.of(IS_DEFAULT, true));
    return BusinessMapper.business(member.getBusiness(), true, member.getRole());
  }
  
  @Transactional
  public void delete(UUID businessId) {
    User user = currentUserProvider.getCurrentUser();
    permissionService.requireAnyRole(businessId, user.getId(),
        BusinessRole.OWNER);
    Business b = permissionService.requireActiveBusiness(businessId);
    b.setStatus(Status.DELETED);
    b.setUpdatedBy(user);
    Business saved = businessRepository.save(b);
    revokePendingInvitations(saved.getId(), user);
    clearDeletedBusinessDefaults(saved.getId(), user);
    publishBusinessAudit(user, businessId, AuditAction.BUSINESS_DELETED, saved,
        Map.of(STATUS, saved.getStatus()));
  }
  
  private void revokePendingInvitations(UUID businessId, User user) {
    var invitations = invitationRepository.findByBusinessIdAndInvitationStatusAndStatus(
        businessId, InvitationStatus.PENDING, Status.ACTIVE);
    invitations.forEach(invitation -> {
      invitation.setInvitationStatus(InvitationStatus.CANCELLED);
      invitation.setUpdatedBy(user);
    });
    invitationRepository.saveAll(invitations);
  }
  
  private void clearDeletedBusinessDefaults(UUID deletedBusinessId, User user) {
    var members = memberRepository.findByBusinessIdAndStatus(deletedBusinessId, Status.ACTIVE);
    for (BusinessMember member : members) {
      if (!member.isDefaultBusiness()) {
        continue;
      }
      member.setDefaultBusiness(false);
      member.setUpdatedBy(user);
      memberRepository.save(member);
      assignReplacementDefault(member.getUserId(), deletedBusinessId, user);
    }
  }
  
  private void assignReplacementDefault(UUID userId, UUID deletedBusinessId, User actor) {
    memberRepository.findByUserIdAndStatus(userId, Status.ACTIVE).stream()
        .filter(member -> deletedBusinessId == null
            || !member.getBusiness().getId().equals(deletedBusinessId))
        .filter(member -> member.getBusiness().getStatus() == Status.ACTIVE)
        .sorted(Comparator.comparing(BusinessMember::getJoinedAt,
            Comparator.nullsLast(Comparator.reverseOrder())))
        .findFirst()
        .ifPresent(member -> {
          member.setDefaultBusiness(true);
          member.setUpdatedBy(actor);
          memberRepository.save(member);
        });
  }
  
  private Pageable defaultSort(Pageable pageable) {
    if (pageable.getSort().isSorted()) {
      return pageable;
    }
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        Sort.by(Sort.Direction.DESC, "createdAt"));
  }
  
  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }
  
  private static String trimToNull(String s) {
    return blank(s) ? null : s.trim();
  }
  
  private boolean hasDefaultBusiness(UUID userId) {
    return memberRepository.existsByUserIdAndDefaultBusinessTrueAndStatus(userId, Status.ACTIVE);
  }
  
  private boolean isDefaultBusiness(UUID userId, UUID businessId) {
    return memberRepository.findByUserIdAndDefaultBusinessTrueAndStatus(userId, Status.ACTIVE)
        .map(member -> member.getBusiness().getId().equals(businessId))
        .orElse(false);
  }
  
  private void publishBusinessAudit(User user, UUID businessId, AuditAction action,
      Business business,
      Map<String, Object> metadata) {
    auditPublisher.publishBusinessEvent(user.getId(), businessId, action, BUSINESS,
        business.getId(), metadata);
  }
}

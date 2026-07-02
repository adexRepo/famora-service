package com.famora.business.service;

import com.famora.business.constant.BusinessDefaults;
import com.famora.business.dto.request.CreateBusinessRequest;
import com.famora.business.dto.request.UpdateBusinessRequest;
import com.famora.business.dto.response.BusinessResponse;
import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessMember;
import com.famora.business.enums.BusinessRole;
import com.famora.business.repository.BusinessMemberRepository;
import com.famora.business.repository.BusinessRepository;
import com.famora.business.spec.BusinessSpecifications;
import com.famora.common.helper.Status;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import java.time.LocalDateTime;
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
  private final BusinessPermissionService permissionService;
  private final CurrentUserProvider currentUserProvider;
  
  @Transactional
  public BusinessResponse create(CreateBusinessRequest request) {
    User userId = currentUserProvider.getCurrentUser();
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
    b.setCreatedBy(userId);
    b = businessRepository.save(b);
    
    BusinessMember owner = new BusinessMember();
    owner.setBusiness(b);
    owner.setUserId(userId.getId());
    owner.setRole(BusinessRole.OWNER);
    owner.setStatus(Status.ACTIVE);
    owner.setJoinedAt(LocalDateTime.now());
    owner.setCreatedBy(userId);
    memberRepository.save(owner);
    
    return BusinessMapper.business(b);
  }
  
  @Transactional(readOnly = true)
  public Page<BusinessResponse> list(Pageable pageable) {
    return businessRepository.findAll(
        BusinessSpecifications.accessibleByUser(currentUserProvider.getCurrentUserId()),
        defaultSort(pageable)).map(BusinessMapper::business);
  }
  
  @Transactional(readOnly = true)
  public BusinessResponse get(UUID businessId) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    return BusinessMapper.business(permissionService.requireActiveBusiness(businessId));
  }
  
  @Transactional
  public BusinessResponse update(UUID businessId, UpdateBusinessRequest request) {
    UUID userId = currentUserProvider.getCurrentUserId();
    permissionService.requireAnyRole(businessId, userId, BusinessRole.OWNER, BusinessRole.PARTNER);
    Business b = permissionService.requireActiveBusiness(businessId);
    b.setName(request.name().trim());
    b.setBusinessType(
        blank(request.businessType()) ? b.getBusinessType() : request.businessType().trim());
    b.setDefaultCurrency(blank(request.defaultCurrency()) ? b.getDefaultCurrency()
        : request.defaultCurrency().trim().toUpperCase());
    b.setPrimaryFamilyId(request.primaryFamilyId());
    b.setDescription(request.description());
    b.setUpdatedBy(currentUserProvider.getCurrentUser());
    return BusinessMapper.business(businessRepository.save(b));
  }
  
  @Transactional
  public void delete(UUID businessId) {
    permissionService.requireAnyRole(businessId, currentUserProvider.getCurrentUserId(),
        BusinessRole.OWNER);
    Business b = permissionService.requireActiveBusiness(businessId);
    b.setStatus(Status.DELETED);
    b.setUpdatedBy(currentUserProvider.getCurrentUser());
    businessRepository.save(b);
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
}

package com.famora.business.service;

import static com.famora.business.constant.BusinessAuditConstants.PRODUCT;
import static com.famora.business.constant.BusinessAuditConstants.PRODUCT_NAME;
import static com.famora.business.constant.BusinessAuditConstants.STATUS;

import com.famora.audit.entity.AuditAction;
import com.famora.business.constant.BusinessDefaults;
import com.famora.business.dto.request.CreateProductRequest;
import com.famora.business.dto.request.UpdateProductRequest;
import com.famora.business.dto.response.BusinessProductResponse;
import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessMember;
import com.famora.business.entity.BusinessProduct;
import com.famora.business.publisher.BusinessAuditPublisher;
import com.famora.business.repository.BusinessProductRepository;
import com.famora.business.spec.BusinessProductSpecifications;
import com.famora.common.exception.BusinessException;
import com.famora.common.helper.MoneyUtil;
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
public class BusinessProductService {
  
  private final BusinessProductRepository productRepository;
  private final BusinessPermissionService permissionService;
  private final CurrentUserProvider currentUserProvider;
  private final BusinessAuditPublisher auditPublisher;
  
  @Transactional
  public BusinessProductResponse create(UUID businessId, CreateProductRequest req) {
    User user = currentUserProvider.getCurrentUser();
    BusinessMember member = permissionService.requireCanManageProduct(businessId, user.getId());
    MoneyUtil.requireNonNegative(req.defaultSellingPrice(), "defaultSellingPrice");
    if (req.costPrice() != null) {
      MoneyUtil.requireNonNegative(req.costPrice(), "costPrice");
    }
    Business business = permissionService.requireActiveBusiness(businessId);
    
    BusinessProduct p = new BusinessProduct();
    p.setBusiness(business);
    p.setProductName(req.productName().trim());
    p.setCategory(req.category());
    p.setUnit(blank(req.unit()) ? BusinessDefaults.UNIT : req.unit().trim());
    p.setDefaultSellingPrice(MoneyUtil.nvl(req.defaultSellingPrice()));
    p.setCostPrice(req.costPrice() == null ? null : MoneyUtil.nvl(req.costPrice()));
    p.setCreatedBy(user);
    p = productRepository.save(p);
    publishProductAudit(user, businessId, AuditAction.BUSINESS_PRODUCT_CREATED, p);
    return BusinessMapper.product(p, member.getRole());
  }
  
  @Transactional(readOnly = true)
  public Page<BusinessProductResponse> list(UUID businessId, Pageable pageable) {
    UUID userId = currentUserProvider.getCurrentUserId();
    BusinessMember member = permissionService.requireCanView(businessId, userId);
    return productRepository.findAll(
            BusinessProductSpecifications.belongsToBusiness(businessId)
                .and(BusinessProductSpecifications.statusNot(Status.DELETED)),
            pageable)
        .map(p -> BusinessMapper.product(p, member.getRole()));
  }
  
  @Transactional(readOnly = true)
  public BusinessProductResponse get(UUID businessId, UUID productId) {
    UUID userId = currentUserProvider.getCurrentUserId();
    BusinessMember member = permissionService.requireCanView(businessId, userId);
    BusinessProduct p = productRepository.findByIdAndBusinessIdAndStatusNot(productId, businessId,
            Status.DELETED)
        .orElseThrow(() -> BusinessException.notFound("Product not found"));
    return BusinessMapper.product(p, member.getRole());
  }
  
  @Transactional
  public BusinessProductResponse update(UUID businessId, UUID productId, UpdateProductRequest req) {
    User user = currentUserProvider.getCurrentUser();
    BusinessMember member = permissionService.requireCanManageProduct(businessId, user.getId());
    BusinessProduct p = productRepository.findByIdAndBusinessIdAndStatusNot(productId, businessId,
            Status.DELETED)
        .orElseThrow(() -> BusinessException.notFound("Product not found"));
    MoneyUtil.requireNonNegative(req.defaultSellingPrice(), "defaultSellingPrice");
    if (req.costPrice() != null) {
      MoneyUtil.requireNonNegative(req.costPrice(), "costPrice");
    }
    p.setProductName(req.productName().trim());
    p.setCategory(req.category());
    p.setUnit(blank(req.unit()) ? BusinessDefaults.UNIT : req.unit().trim());
    p.setDefaultSellingPrice(MoneyUtil.nvl(req.defaultSellingPrice()));
    p.setCostPrice(req.costPrice() == null ? null : MoneyUtil.nvl(req.costPrice()));
    if (req.status() != null) {
      if (req.status() == Status.DELETED) {
        throw BusinessException.validation("Use delete product API to delete product");
      }
      p.setStatus(req.status());
    }
    p.setUpdatedBy(user);
    p = productRepository.save(p);
    publishProductAudit(user, businessId, AuditAction.BUSINESS_PRODUCT_UPDATED, p);
    return BusinessMapper.product(p, member.getRole());
  }
  
  @Transactional
  public void delete(UUID businessId, UUID productId) {
    User user = currentUserProvider.getCurrentUser();
    permissionService.requireCanManageProduct(businessId, user.getId());
    BusinessProduct p = productRepository.findByIdAndBusinessIdAndStatusNot(productId, businessId,
            Status.DELETED)
        .orElseThrow(() -> BusinessException.notFound("Product not found"));
    p.setStatus(Status.DELETED);
    p.setUpdatedBy(user);
    productRepository.save(p);
    publishProductAudit(user, businessId, AuditAction.BUSINESS_PRODUCT_DELETED, p);
  }
  
  private void publishProductAudit(User user, UUID businessId, AuditAction action,
      BusinessProduct p) {
    auditPublisher.publishBusinessEvent(
        user.getId(),
        businessId,
        action,
        PRODUCT,
        p.getId(),
        Map.of(PRODUCT_NAME, p.getProductName(), STATUS, p.getStatus())
    );
  }
  
  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }
}

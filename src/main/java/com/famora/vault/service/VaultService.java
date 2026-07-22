package com.famora.vault.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.AppException;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.common.helper.PermissionHelper;
import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import com.famora.common.spec.VisibleFamilyScopedSpecifications;
import com.famora.family.dto.FamilyContext;
import com.famora.user.entity.User;
import com.famora.vault.dto.CreateVaultItemRequest;
import com.famora.vault.dto.UpdateVaultItemRequest;
import com.famora.vault.dto.VaultItemDetailResponse;
import com.famora.vault.dto.VaultItemResponse;
import com.famora.vault.dto.VaultRevealPurpose;
import com.famora.vault.dto.VaultRevealSecretRequest;
import com.famora.vault.dto.VaultSecretResponse;
import com.famora.vault.entity.VaultItem;
import com.famora.vault.repository.VaultItemRepository;
import com.famora.vault.spec.VaultItemSpecifications;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VaultService {
  
  private final VaultItemRepository vaultItemRepository;
  private final VaultCryptoService vaultCryptoService;
  private final AuditLogService auditLogService;
  
  @Transactional
  public VaultItemDetailResponse create(FamilyContext ctx, CreateVaultItemRequest request) {
    User user = ctx.user();
    
    VaultItem item = VaultItem.builder()
        .family(ctx.family())
        .title(request.title().trim())
        .username(clean(request.username()))
        .encryptedSecret(vaultCryptoService.encrypt(request.secret()))
        .url(clean(request.url()))
        .notes(clean(request.notes()))
        .visibility(request.visibility() == null ? Visibility.PRIVATE : request.visibility())
        .createdBy(user)
        .build();
    
    vaultItemRepository.save(item);
    
    auditLogService.log(
        ctx.family(),
        user,
        AuditAction.VAULT_ITEM_CREATED,
        "vault_items",
        item.getId(),
        null
    );
    
    return toDetailResponse(ctx, item);
  }
  
  @Transactional(readOnly = true)
  public Page<VaultItemResponse> list(
      FamilyContext ctx,
      String keyword,
      Visibility visibility,
      Pageable pageable
  ) {
    UUID familyId = ctx.family().getId();
    UUID userId = ctx.user().getId();
    boolean isOwner = ctx.owner();
    
    Specification<VaultItem> spec = Specification
        .where(VisibleFamilyScopedSpecifications.<VaultItem>visibleToUser(
            familyId,
            userId,
            isOwner,
            Status.ACTIVE,
            visibility
        ))
        .and(VaultItemSpecifications.keyword(keyword));
    
    Page<VaultItem> page = vaultItemRepository.findAll(spec, pageable);
    
    return page.map(item -> toVaultItemResponse(ctx, item));
  }
  
  @Transactional(readOnly = true)
  public VaultItemDetailResponse getDetail(FamilyContext ctx, UUID id) {
    VaultItem item = getAccessibleVaultActive(ctx, id);
    
    auditLogService.log(
        ctx.family(),
        ctx.user(),
        AuditAction.VAULT_ITEM_VIEWED,
        "vault_items",
        item.getId(),
        null
    );
    
    return toDetailResponse(ctx, item);
  }
  
  @Transactional(readOnly = true)
  public VaultSecretResponse revealSecret(FamilyContext ctx, UUID id,
      VaultRevealSecretRequest request) {
    VaultItem item = getAccessibleVaultActive(ctx, id);
    if (!canViewSecret(ctx, item)) {
      throw new AppException(HttpStatus.FORBIDDEN,
          "You do not have permission to reveal this vault secret");
    }
    VaultRevealPurpose purpose = request == null || request.purpose() == null
        ? VaultRevealPurpose.VIEW
        : request.purpose();
    
    auditLogService.log(
        ctx.family(),
        ctx.user(),
        purpose == VaultRevealPurpose.COPY
            ? AuditAction.VAULT_ITEM_SECRET_COPIED
            : AuditAction.VAULT_ITEM_SECRET_REVEALED,
        "vault_items",
        item.getId(),
        null
    );
    
    return new VaultSecretResponse(
        item.getId(),
        vaultCryptoService.decrypt(item.getEncryptedSecret()),
        purpose,
        OffsetDateTime.now()
    );
  }
  
  private VaultItem getAccessibleVaultActive(FamilyContext ctx, UUID id) {
    VaultItem item = getVaultActive(ctx, id);
    PermissionHelper.assertCanAccess(item.getVisibility(), item.getStatus(),
        item.getCreatedBy().getId(), ctx);
    return item;
  }
  
  private VaultItem getVaultActive(FamilyContext ctx, UUID id) {
    return vaultItemRepository
        .findByIdAndFamilyIdAndStatus(id, ctx.family().getId(), Status.ACTIVE)
        .orElseThrow(() -> new ResourceNotFoundException("Vault item not found"));
  }
  
  @Transactional
  public VaultItemDetailResponse update(FamilyContext ctx, UUID id, UpdateVaultItemRequest request) {
    User user = ctx.user();
    VaultItem item = getVaultActive(ctx, id);
    assertCanMutate(ctx, item);
    
    item.setTitle(request.title().trim());
    item.setUsername(clean(request.username()));
    item.setUrl(clean(request.url()));
    item.setNotes(clean(request.notes()));
    item.setUpdatedBy(user);
    item.setVisibility(request.visibility());
    
    if (request.secret() != null && !request.secret().isBlank()) {
      item.setEncryptedSecret(vaultCryptoService.encrypt(request.secret()));
    }
    
    vaultItemRepository.save(item);
    
    auditLogService.log(
        ctx.family(),
        user,
        AuditAction.VAULT_ITEM_UPDATED,
        "vault_items",
        item.getId(),
        null
    );
    
    return toDetailResponse(ctx, item);
  }
  
  @Transactional
  public void delete(FamilyContext ctx, UUID id) {
    User user = ctx.user();
    VaultItem item = getVaultActive(ctx, id);
    assertCanMutate(ctx, item);
    
    item.setUpdatedBy(user);
    item.setStatus(Status.DELETED);
    
    vaultItemRepository.save(item);
    
    auditLogService.log(
        ctx.family(),
        user,
        AuditAction.VAULT_ITEM_DELETED,
        "vault_items",
        item.getId(),
        null
    );
  }
  
  private VaultItemResponse toVaultItemResponse(FamilyContext ctx, VaultItem item) {
    return new VaultItemResponse(
        item.getId(),
        item.getTitle(),
        item.getUsername(),
        item.getUrl(),
        item.getVisibility(),
        item.getCreatedBy().getId(),
        item.getCreatedBy().getFullName(),
        canViewSecret(ctx, item),
        canMutate(ctx, item),
        canMutate(ctx, item),
        item.getCreatedAt(),
        item.getUpdatedAt()
    );
  }
  
  private VaultItemDetailResponse toDetailResponse(FamilyContext ctx, VaultItem item) {
    return new VaultItemDetailResponse(
        item.getId(),
        item.getTitle(),
        item.getUsername(),
        null,
        item.getUrl(),
        item.getNotes(),
        item.getVisibility(),
        item.getCreatedBy().getId(),
        item.getCreatedBy().getFullName(),
        canViewSecret(ctx, item),
        canMutate(ctx, item),
        canMutate(ctx, item),
        item.getCreatedAt(),
        item.getUpdatedAt()
    );
  }
  
  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
  
  private void assertCanMutate(FamilyContext ctx, VaultItem item) {
    if (canMutate(ctx, item)) {
      return;
    }
    throw new AppException(HttpStatus.FORBIDDEN,
        "Only vault item owner or family owner can modify this vault item");
  }
  
  private boolean canViewSecret(FamilyContext ctx, VaultItem item) {
    return switch (item.getVisibility()) {
      case FAMILY -> true;
      case PRIVATE -> item.getCreatedBy().getId().equals(ctx.user().getId());
      case OWNER_ONLY -> ctx.owner();
    };
  }
  
  private boolean canMutate(FamilyContext ctx, VaultItem item) {
    return ctx.owner() || item.getCreatedBy().getId().equals(ctx.user().getId());
  }
}

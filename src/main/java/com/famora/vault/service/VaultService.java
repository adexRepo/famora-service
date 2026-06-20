package com.famora.vault.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.ResourceNotFoundException;
import com.famora.common.helper.Visibility;
import com.famora.family.dto.FamilyContext;
import com.famora.family.entity.Family;
import com.famora.security.CurrentUserService;
import com.famora.security.FamilyContextService;
import com.famora.user.entity.User;
import com.famora.vault.dto.CreateVaultItemRequest;
import com.famora.vault.dto.UpdateVaultItemRequest;
import com.famora.vault.dto.VaultItemDetailResponse;
import com.famora.vault.dto.VaultItemResponse;
import com.famora.vault.entity.VaultItem;
import com.famora.vault.repository.VaultItemRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VaultService {
  
  private final VaultItemRepository vaultItemRepository;
  private final CurrentUserService currentUserService;
  private final FamilyContextService familyContextService;
  private final VaultCryptoService vaultCryptoService;
  private final AuditLogService auditLogService;
  
  @Transactional
  public VaultItemDetailResponse create(CreateVaultItemRequest request) {
    User user = currentUserService.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    VaultItem item = VaultItem.builder()
        .family(family)
        .title(request.title().trim())
        .username(clean(request.username()))
        .encryptedSecret(vaultCryptoService.encrypt(request.secret()))
        .url(clean(request.url()))
        .notes(clean(request.notes()))
        .visibility(request.visibility())
        .createdBy(user)
        .build();
    
    vaultItemRepository.save(item);
    
    auditLogService.log(
        family,
        user,
        AuditAction.VAULT_ITEM_CREATED,
        "vault_items",
        item.getId(),
        null
    );
    
    return toDetailResponse(item);
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
    
    Visibility selectedVisibility = visibility == null
        ? Visibility.PRIVATE
        : visibility;
    
    String cleanKeyword = clean(keyword);
    
    Page<VaultItem> page;
    
    if (cleanKeyword == null) {
      page = vaultItemRepository.findVisibleByFamilyAndVisibility(
          familyId,
          userId,
          isOwner,
          selectedVisibility,
          pageable
      );
    } else {
      page = vaultItemRepository.searchVisibleByFamilyAndKeywordAndVisibility(
          familyId,
          userId,
          isOwner,
          cleanKeyword,
          selectedVisibility,
          pageable
      );
    }
    
    return page.map(this::toVaultItemResponse);
  }
  
  @Transactional(readOnly = true)
  public VaultItemDetailResponse getDetail(UUID id) {
    User user = currentUserService.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    VaultItem item = vaultItemRepository
        .findByIdAndFamilyIdAndDeletedAtIsNull(id, family.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault item not found"));
    
    auditLogService.log(
        family,
        user,
        AuditAction.VAULT_ITEM_VIEWED,
        "vault_items",
        item.getId(),
        null
    );
    
    return toDetailResponse(item);
  }
  
  @Transactional
  public VaultItemDetailResponse update(UUID id, UpdateVaultItemRequest request) {
    User user = currentUserService.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    VaultItem item = vaultItemRepository
        .findByIdAndFamilyIdAndDeletedAtIsNull(id, family.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault item not found"));
    
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
        family,
        user,
        AuditAction.VAULT_ITEM_UPDATED,
        "vault_items",
        item.getId(),
        null
    );
    
    return toDetailResponse(item);
  }
  
  @Transactional
  public void delete(UUID id) {
    User user = currentUserService.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    VaultItem item = vaultItemRepository
        .findByIdAndFamilyIdAndDeletedAtIsNull(id, family.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault item not found"));
    
    item.setDeletedAt(OffsetDateTime.now());
    item.setUpdatedBy(user);
    
    vaultItemRepository.save(item);
    
    auditLogService.log(
        family,
        user,
        AuditAction.VAULT_ITEM_DELETED,
        "vault_items",
        item.getId(),
        null
    );
  }
  
  private VaultItemResponse toVaultItemResponse(VaultItem item) {
    return new VaultItemResponse(
        item.getId(),
        item.getTitle(),
        item.getUsername(),
        item.getUrl(),
        item.getCreatedAt(),
        item.getUpdatedAt()
    );
  }
  
  private VaultItemDetailResponse toDetailResponse(VaultItem item) {
    return new VaultItemDetailResponse(
        item.getId(),
        item.getTitle(),
        item.getUsername(),
        vaultCryptoService.decrypt(item.getEncryptedSecret()),
        item.getUrl(),
        item.getNotes(),
        item.getCreatedAt(),
        item.getUpdatedAt()
    );
  }
  
  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}

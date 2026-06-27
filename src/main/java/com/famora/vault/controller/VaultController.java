package com.famora.vault.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.common.helper.Visibility;
import com.famora.family.dto.FamilyContext;
import com.famora.security.FamilyContextService;
import com.famora.vault.dto.CreateVaultItemRequest;
import com.famora.vault.dto.UpdateVaultItemRequest;
import com.famora.vault.dto.VaultItemDetailResponse;
import com.famora.vault.dto.VaultItemResponse;
import com.famora.vault.service.VaultService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vault/items")
@RequiredArgsConstructor
public class VaultController {
  
  private final FamilyContextService families;
  private final VaultService vaultService;
  
  @PostMapping
  public ApiResponse<VaultItemDetailResponse> create(
      @Valid @RequestBody CreateVaultItemRequest request) {
    return ApiResponse.ok(vaultService.create(request));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<VaultItemResponse>> list(
      @RequestHeader("X-Family-Id") String familyId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Visibility visibility,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    
    FamilyContext ctx = families.require(familyId);
    PageRequest request = PageRequest.of(page, size,
        Sort.by("createdAt", "updatedAt").descending());
    
    return ApiResponse.ok(PageResponse.from(vaultService.list(ctx, keyword, visibility, request)));
  }
  
  @GetMapping("/{id}")
  public ApiResponse<VaultItemDetailResponse> getDetail(@PathVariable UUID id) {
    return ApiResponse.ok(vaultService.getDetail(id));
  }
  
  @PutMapping("/{id}")
  public ApiResponse<VaultItemDetailResponse> update(@PathVariable UUID id,
      @Valid @RequestBody UpdateVaultItemRequest request) {
    return ApiResponse.ok(vaultService.update(id, request));
  }
  
  @DeleteMapping("/{id}")
  public ApiResponse<Boolean> delete(@PathVariable UUID id) {
    vaultService.delete(id);
    return ApiResponse.ok("Success", true);
  }
}

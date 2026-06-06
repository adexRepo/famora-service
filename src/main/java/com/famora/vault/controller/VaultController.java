package com.famora.vault.controller;

import com.famora.vault.dto.CreateVaultItemRequest;
import com.famora.vault.dto.UpdateVaultItemRequest;
import com.famora.vault.dto.VaultItemDetailResponse;
import com.famora.vault.dto.VaultItemListResponse;
import com.famora.vault.service.VaultService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vault/items")
@RequiredArgsConstructor
public class VaultController {
  
  private final VaultService vaultService;
  
  @PostMapping
  public VaultItemDetailResponse create(@Valid @RequestBody CreateVaultItemRequest request) {
    return vaultService.create(request);
  }
  
  @GetMapping
  public List<VaultItemListResponse> list(
      @RequestParam(required = false) String keyword
  ) {
    return vaultService.list(keyword);
  }
  
  @GetMapping("/{id}")
  public VaultItemDetailResponse getDetail(@PathVariable UUID id) {
    return vaultService.getDetail(id);
  }
  
  @PutMapping("/{id}")
  public VaultItemDetailResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateVaultItemRequest request
  ) {
    return vaultService.update(id, request);
  }
  
  @DeleteMapping("/{id}")
  public Map<String, Object> delete(@PathVariable UUID id) {
    vaultService.delete(id);
    return Map.of("success", true);
  }
}

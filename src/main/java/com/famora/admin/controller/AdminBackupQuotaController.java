package com.famora.admin.controller;

import com.famora.admin.dto.AdminBackupQuotaResponse;
import com.famora.admin.dto.UpdateBackupQuotaRequest;
import com.famora.admin.service.AdminBackupQuotaService;
import com.famora.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminBackupQuotaController {

  private final AdminBackupQuotaService adminBackupQuotaService;

  @GetMapping("/{userId}/backup-usage")
  public ApiResponse<AdminBackupQuotaResponse> getUsage(@PathVariable UUID userId) {
    return ApiResponse.ok(adminBackupQuotaService.getUsage(userId));
  }

  @PutMapping("/{userId}/backup-quota")
  public ApiResponse<AdminBackupQuotaResponse> updateQuota(
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateBackupQuotaRequest request) {
    return ApiResponse.ok(adminBackupQuotaService.updateQuota(userId, request));
  }
}

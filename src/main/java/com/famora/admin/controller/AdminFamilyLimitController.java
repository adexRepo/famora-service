package com.famora.admin.controller;

import com.famora.admin.dto.AdminFamilyLimitResponse;
import com.famora.admin.dto.UpdateFamilyLimitRequest;
import com.famora.admin.service.AdminFamilyLimitService;
import com.famora.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminFamilyLimitController {

  private final AdminFamilyLimitService adminFamilyLimitService;

  @PutMapping("/{userId}/family-limit")
  public ApiResponse<AdminFamilyLimitResponse> updateFamilyLimit(
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateFamilyLimitRequest request) {
    return ApiResponse.ok(adminFamilyLimitService.updateFamilyLimit(userId, request));
  }
}

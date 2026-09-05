package com.famora.admin.controller;

import com.famora.admin.dto.AdminMeResponse;
import com.famora.admin.service.AdminIdentityService;
import com.famora.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminIdentityController {

  private final AdminIdentityService adminIdentityService;

  @GetMapping("/me")
  public ApiResponse<AdminMeResponse> getMe() {
    return ApiResponse.ok(adminIdentityService.getMe());
  }
}

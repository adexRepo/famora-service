package com.famora.admin.controller;

import com.famora.admin.dto.AdminBootstrapResponse;
import com.famora.admin.service.AdminBootstrapService;
import com.famora.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/bootstrap")
@RequiredArgsConstructor
public class AdminBootstrapController {

  private final AdminBootstrapService adminBootstrapService;

  @PostMapping
  public ApiResponse<AdminBootstrapResponse> bootstrap(
      @RequestHeader(value = "X-Admin-Bootstrap-Token", required = false) String token) {
    return ApiResponse.ok("Administrator created",
        adminBootstrapService.bootstrapCurrentUser(token));
  }
}

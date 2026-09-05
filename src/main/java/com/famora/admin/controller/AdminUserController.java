package com.famora.admin.controller;

import com.famora.admin.dto.AdminUserSummaryResponse;
import com.famora.admin.service.AdminUserQueryService;
import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.user.entity.UserStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

  private final AdminUserQueryService adminUserQueryService;

  @GetMapping
  public ApiResponse<PageResponse<AdminUserSummaryResponse>> searchUsers(
      @RequestParam(required = false) @Size(max = 180) String query,
      @RequestParam(required = false) UserStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    PageRequest pageable = PageRequest.of(page, size,
        Sort.by(Sort.Direction.DESC, "createdAt"));
    return ApiResponse.ok(PageResponse.from(adminUserQueryService.searchUsers(query, status,
        pageable)));
  }
}

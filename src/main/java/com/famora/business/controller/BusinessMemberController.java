package com.famora.business.controller;

import com.famora.business.dto.request.UpdateMemberRoleRequest;
import com.famora.business.dto.response.BusinessMemberResponse;
import com.famora.business.service.BusinessMemberService;
import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/members")
@RequiredArgsConstructor
public class BusinessMemberController {
  
  private final BusinessMemberService memberService;
  
  @GetMapping
  public ApiResponse<PageResponse<BusinessMemberResponse>> list(@PathVariable UUID businessId,
      Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(memberService.list(businessId, pageable)));
  }
  
  @PutMapping("/{memberId}/role")
  public ApiResponse<BusinessMemberResponse> updateRole(@PathVariable UUID businessId,
      @PathVariable UUID memberId,
      @Valid @RequestBody UpdateMemberRoleRequest request) {
    return ApiResponse.ok(memberService.updateRole(businessId, memberId, request));
  }
  
  @DeleteMapping("/{memberId}")
  public ApiResponse<Void> remove(@PathVariable UUID businessId, @PathVariable UUID memberId) {
    memberService.remove(businessId, memberId);
    return ApiResponse.ok(null);
  }
}

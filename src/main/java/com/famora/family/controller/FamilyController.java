package com.famora.family.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.family.dto.CreateFamilyRequest;
import com.famora.family.dto.CreateInvitationRequest;
import com.famora.family.dto.FamilyContext;
import com.famora.family.dto.FamilyLeaveRequestResponse;
import com.famora.family.dto.FamilyMemberResponse;
import com.famora.family.dto.FamilyMembershipSummaryResponse;
import com.famora.family.dto.FamilyResponse;
import com.famora.family.dto.InvitationResponse;
import com.famora.family.dto.JoinFamilyRequest;
import com.famora.family.dto.LeaveFamilyRequest;
import com.famora.family.dto.LeaveFamilyResultResponse;
import com.famora.family.dto.OwnershipTransferRequest;
import com.famora.family.dto.OwnershipTransferResponse;
import com.famora.family.dto.ReviewLeaveRequest;
import com.famora.family.helper.FamilyMemberRole;
import com.famora.family.helper.FamilyMemberStatus;
import com.famora.family.service.FamilyLifecycleService;
import com.famora.family.service.FamilyMemberService;
import com.famora.family.service.FamilyService;
import com.famora.security.FamilyContextService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
public class FamilyController {
  
  private final FamilyService familyService;
  private final FamilyMemberService familyMemberService;
  private final FamilyLifecycleService familyLifecycleService;
  private final FamilyContextService familyContextService;
  
  @GetMapping
  public ApiResponse<List<FamilyResponse>> getMyFamilies() {
    return ApiResponse.ok(familyService.getMyFamilies());
  }
  
  @GetMapping("/default")
  public ApiResponse<FamilyResponse> getDefaultFamily() {
    return ApiResponse.ok(familyService.getDefaultFamily());
  }

  @GetMapping("/membership-summary")
  public ApiResponse<FamilyMembershipSummaryResponse> membershipSummary() {
    return ApiResponse.ok(familyService.membershipSummary());
  }
  
  @PostMapping
  public ApiResponse<FamilyResponse> createFamily(@Valid @RequestBody CreateFamilyRequest request) {
    return ApiResponse.ok(familyService.createFamily(request));
  }
  
  @PostMapping("/{familyId}/invitations")
  public ApiResponse<InvitationResponse> createInvitation(@PathVariable UUID familyId,
      @Valid @RequestBody CreateInvitationRequest request) {
    return ApiResponse.ok(familyService.createInvitation(familyId, request));
  }
  
  @PostMapping("/join")
  public ApiResponse<FamilyResponse> joinFamily(@Valid @RequestBody JoinFamilyRequest request) {
    return ApiResponse.ok(familyService.joinFamily(request));
  }
  
  @PutMapping("/{familyId}/default")
  public ApiResponse<FamilyResponse> setDefaultFamily(@PathVariable UUID familyId) {
    return ApiResponse.ok(familyService.setDefaultFamily(familyId));
  }

  @PostMapping("/{familyId}/leave-requests")
  public ApiResponse<FamilyLeaveRequestResponse> requestLeave(
      @PathVariable UUID familyId,
      @Valid @RequestBody LeaveFamilyRequest request) {
    return ApiResponse.ok(familyLifecycleService.requestLeave(familyId, request));
  }

  @GetMapping("/{familyId}/leave-requests")
  public ApiResponse<PageResponse<FamilyLeaveRequestResponse>> leaveRequests(
      @PathVariable UUID familyId,
      @PageableDefault(
          size = 20,
          sort = "createdAt",
          direction = Sort.Direction.DESC
      ) Pageable pageable) {
    return ApiResponse.ok(
        PageResponse.from(familyLifecycleService.listLeaveRequests(familyId, pageable)));
  }

  @PostMapping("/{familyId}/leave-requests/{requestId}/approve")
  public ApiResponse<LeaveFamilyResultResponse> approveLeave(
      @PathVariable UUID familyId,
      @PathVariable UUID requestId) {
    return ApiResponse.ok(familyLifecycleService.approve(familyId, requestId));
  }

  @PostMapping("/{familyId}/leave-requests/{requestId}/reject")
  public ApiResponse<FamilyLeaveRequestResponse> rejectLeave(
      @PathVariable UUID familyId,
      @PathVariable UUID requestId,
      @Valid @RequestBody ReviewLeaveRequest request) {
    return ApiResponse.ok(familyLifecycleService.reject(familyId, requestId, request));
  }

  @PostMapping("/{familyId}/leave-requests/{requestId}/cancel")
  public ApiResponse<FamilyLeaveRequestResponse> cancelLeave(
      @PathVariable UUID familyId,
      @PathVariable UUID requestId) {
    return ApiResponse.ok(familyLifecycleService.cancel(familyId, requestId));
  }

  @PostMapping("/{familyId}/ownership-transfer")
  public ApiResponse<OwnershipTransferResponse> transferOwnership(
      @PathVariable UUID familyId,
      @Valid @RequestBody OwnershipTransferRequest request) {
    return ApiResponse.ok(familyLifecycleService.transferOwnership(familyId, request));
  }
  
  
  @GetMapping("/{familyId}/members")
  public ApiResponse<PageResponse<FamilyMemberResponse>> list(
      @PathVariable String familyId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) FamilyMemberRole role,
      @RequestParam(required = false) FamilyMemberStatus status,
      @PageableDefault(
          size = 20,
          sort = "createdAt",
          direction = Sort.Direction.ASC
      ) Pageable pageable
  ) {
    
    FamilyContext ctx = familyContextService.require(familyId);
    Page<FamilyMemberResponse> page = familyMemberService.list(
        ctx,
        keyword,
        role,
        status,
        pageable
    );
    
    return ApiResponse.ok(PageResponse.from(page));
  }
}

package com.famora.business.controller;

import com.famora.business.constant.BusinessApiMessages;
import com.famora.business.dto.request.CreateInvitationRequest;
import com.famora.business.dto.request.JoinBusinessRequest;
import com.famora.business.dto.response.BusinessInvitationResponse;
import com.famora.business.dto.response.BusinessMemberResponse;
import com.famora.business.service.BusinessInvitationService;
import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
public class BusinessInvitationController {
  
  private final BusinessInvitationService invitationService;
  
  @PostMapping("/{businessId}/invitations")
  public ResponseEntity<ApiResponse<BusinessInvitationResponse>> create(
      @PathVariable UUID businessId,
      @Valid @RequestBody CreateInvitationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(BusinessApiMessages.CREATED,
            invitationService.create(businessId, request)));
  }
  
  @GetMapping("/{businessId}/invitations")
  public ApiResponse<PageResponse<BusinessInvitationResponse>> list(@PathVariable UUID businessId,
      Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(invitationService.list(businessId, pageable)));
  }
  
  @DeleteMapping("/{businessId}/invitations/{invitationId}")
  public ApiResponse<Void> cancel(@PathVariable UUID businessId, @PathVariable UUID invitationId) {
    invitationService.cancel(businessId, invitationId);
    return ApiResponse.ok(null);
  }
  
  @PostMapping("/join")
  public ApiResponse<BusinessMemberResponse> join(@Valid @RequestBody JoinBusinessRequest request) {
    return ApiResponse.ok(invitationService.join(request));
  }
}

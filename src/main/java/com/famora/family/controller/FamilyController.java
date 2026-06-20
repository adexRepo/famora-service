package com.famora.family.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.family.dto.CreateFamilyRequest;
import com.famora.family.dto.CreateInvitationRequest;
import com.famora.family.dto.FamilyResponse;
import com.famora.family.dto.InvitationResponse;
import com.famora.family.dto.JoinFamilyRequest;
import com.famora.family.service.FamilyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
public class FamilyController {
  
  private final FamilyService familyService;
  
  @GetMapping
  public ApiResponse<List<FamilyResponse>> getMyFamilies() {
    return ApiResponse.ok(familyService.getMyFamilies());
  }
  
  @PostMapping
  public ApiResponse<FamilyResponse> createFamily(@Valid @RequestBody CreateFamilyRequest request) {
    return ApiResponse.ok(familyService.createFamily(request));
  }
  
  @PostMapping("/{family}/invitations")
  public ApiResponse<InvitationResponse> createInvitation(@PathVariable UUID familyId,
      @Valid @RequestBody CreateInvitationRequest request) {
    return ApiResponse.ok(familyService.createInvitation(familyId, request));
  }
  
  @PostMapping("/join")
  public ApiResponse<FamilyResponse> joinFamily(@Valid @RequestBody JoinFamilyRequest request) {
    return ApiResponse.ok(familyService.joinFamily(request));
  }
}

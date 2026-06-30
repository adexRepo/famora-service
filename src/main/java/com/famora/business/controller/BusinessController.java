package com.famora.business.controller;

import com.famora.business.constant.BusinessApiMessages;
import com.famora.business.dto.request.CreateBusinessRequest;
import com.famora.business.dto.request.UpdateBusinessRequest;
import com.famora.business.dto.response.BusinessResponse;
import com.famora.business.service.BusinessService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses")
@RequiredArgsConstructor
public class BusinessController {
  
  private final BusinessService businessService;
  
  @PostMapping
  public ResponseEntity<ApiResponse<BusinessResponse>> create(
      @Valid @RequestBody CreateBusinessRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(BusinessApiMessages.CREATED, businessService.create(request)));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<BusinessResponse>> list(Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(businessService.list(pageable)));
  }
  
  @GetMapping("/{businessId}")
  public ApiResponse<BusinessResponse> get(@PathVariable UUID businessId) {
    return ApiResponse.ok(businessService.get(businessId));
  }
  
  @PutMapping("/{businessId}")
  public ApiResponse<BusinessResponse> update(@PathVariable UUID businessId,
      @Valid @RequestBody UpdateBusinessRequest request) {
    return ApiResponse.ok(businessService.update(businessId, request));
  }
  
  @DeleteMapping("/{businessId}")
  public ApiResponse<Void> delete(@PathVariable UUID businessId) {
    businessService.delete(businessId);
    return ApiResponse.ok(null);
  }
}

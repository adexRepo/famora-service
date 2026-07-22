package com.famora.business.controller;

import com.famora.business.dto.response.LookupItemResponse;
import com.famora.business.service.BusinessLookupService;
import com.famora.common.dto.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business-lookups")
@RequiredArgsConstructor
public class BusinessLookupController {
  
  private final BusinessLookupService lookupService;
  
  @GetMapping("/roles")
  public ApiResponse<List<LookupItemResponse>> roles() {
    return ApiResponse.ok(lookupService.roles());
  }
  
  @GetMapping("/payment-methods")
  public ApiResponse<List<LookupItemResponse>> paymentMethods() {
    return ApiResponse.ok(lookupService.paymentMethods());
  }
  
  @GetMapping("/expense-categories")
  public ApiResponse<List<LookupItemResponse>> expenseCategories() {
    return ApiResponse.ok(lookupService.expenseCategories());
  }
  
  @GetMapping("/loss-reasons")
  public ApiResponse<List<LookupItemResponse>> lossReasons() {
    return ApiResponse.ok(lookupService.lossReasons());
  }
  
  @GetMapping("/units")
  public ApiResponse<List<LookupItemResponse>> units() {
    return ApiResponse.ok(lookupService.units());
  }
  
  @GetMapping("/product-categories")
  public ApiResponse<List<LookupItemResponse>> productCategories() {
    return ApiResponse.ok(lookupService.productCategories());
  }
}

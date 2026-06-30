package com.famora.business.controller;

import com.famora.business.constant.BusinessApiMessages;
import com.famora.business.dto.request.CreateExpenseRequest;
import com.famora.business.dto.request.UpdateExpenseRequest;
import com.famora.business.dto.response.ExpenseResponse;
import com.famora.business.service.BusinessExpenseService;
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
@RequestMapping("/api/v1/businesses/{businessId}/expenses")
@RequiredArgsConstructor
public class BusinessExpenseController {
  
  private final BusinessExpenseService expenseService;
  
  @PostMapping
  public ResponseEntity<ApiResponse<ExpenseResponse>> create(@PathVariable UUID businessId,
      @Valid @RequestBody CreateExpenseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(BusinessApiMessages.CREATED,
            expenseService.create(businessId, request)));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<ExpenseResponse>> list(@PathVariable UUID businessId,
      Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(expenseService.list(businessId, pageable)));
  }
  
  @GetMapping("/{expenseId}")
  public ApiResponse<ExpenseResponse> get(@PathVariable UUID businessId,
      @PathVariable UUID expenseId) {
    return ApiResponse.ok(expenseService.get(businessId, expenseId));
  }
  
  @PutMapping("/{expenseId}")
  public ApiResponse<ExpenseResponse> update(@PathVariable UUID businessId,
      @PathVariable UUID expenseId,
      @Valid @RequestBody UpdateExpenseRequest request) {
    return ApiResponse.ok(expenseService.update(businessId, expenseId, request));
  }
  
  @DeleteMapping("/{expenseId}")
  public ApiResponse<Void> delete(@PathVariable UUID businessId, @PathVariable UUID expenseId) {
    expenseService.delete(businessId, expenseId);
    return ApiResponse.ok(null);
  }
}

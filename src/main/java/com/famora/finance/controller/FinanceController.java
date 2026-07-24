package com.famora.finance.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.common.helper.PagingHelper;
import com.famora.family.dto.FamilyContext;
import com.famora.finance.dto.CreateFinanceTransactionRequest;
import com.famora.finance.dto.FinanceSummaryResponse;
import com.famora.finance.dto.FinanceTransactionResponse;
import com.famora.finance.dto.UpdateFinanceTransactionRequest;
import com.famora.finance.entity.FinanceTransactionType;
import com.famora.finance.service.FinanceService;
import com.famora.security.FamilyContextService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {
  
  private final FinanceService financeService;
  private final FamilyContextService families;
  
  @PostMapping("/transactions")
  public ApiResponse<FinanceTransactionResponse> create(
      @RequestHeader("X-Family-Id") String familyId,
      @Valid @RequestBody CreateFinanceTransactionRequest request
  ) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(financeService.create(ctx, request));
  }
  
  @GetMapping("/transactions")
  public ApiResponse<PageResponse<FinanceTransactionResponse>> list(
      @RequestHeader("X-Family-Id") String familyId,
      @RequestParam(required = false) String month,
      @RequestParam(required = false) FinanceTransactionType type,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    
    FamilyContext ctx = families.require(familyId);
    
    PageRequest pageRequest = PagingHelper.buildPageRequest(page, size, "transactionDate", "createdAt", "category");
    
    return ApiResponse.ok(PageResponse.from(
        financeService.list(ctx, month, type, category, pageRequest)));
  }
  
  @GetMapping("/transactions/{id}")
  public ApiResponse<FinanceTransactionResponse> getDetail(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(financeService.getDetail(ctx, id));
  }
  
  @PutMapping("/transactions/{id}")
  public ApiResponse<FinanceTransactionResponse> update(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateFinanceTransactionRequest request
  ) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(financeService.update(ctx, id, request));
  }
  
  @DeleteMapping("/transactions/{id}")
  public ApiResponse<Boolean> delete(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    FamilyContext ctx = families.require(familyId);
    financeService.delete(ctx, id);
    return ApiResponse.ok("Success", true);
  }
  
  @GetMapping("/summary")
  public ApiResponse<FinanceSummaryResponse> getSummary(
      @RequestHeader("X-Family-Id") String familyId,
      @RequestParam(required = false) String month,
      @RequestParam(required = false) String currency
  ) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(financeService.getSummary(ctx, month, currency));
  }
}

package com.famora.finance.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.family.dto.FamilyContext;
import com.famora.finance.dto.FinanceDashboardDtos.DashboardResponse;
import com.famora.finance.dto.FinanceDashboardDtos.EquityTransactionsResponse;
import com.famora.finance.helper.FinanceEquityPeriodMode;
import com.famora.finance.helper.FinanceEquityRange;
import com.famora.finance.service.FinanceDashboardService;
import com.famora.security.FamilyContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceDashboardController {
  
  private final FamilyContextService families;
  private final FinanceDashboardService service;
  
  @GetMapping("/dashboard")
  public ApiResponse<DashboardResponse> dashboard(
      @RequestHeader("X-Family-Id") String familyId,
      @RequestParam(required = false) String currency) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(service.dashboard(ctx, currency));
  }
  
  @GetMapping("/equity-transactions")
  public ApiResponse<EquityTransactionsResponse> equityTransactions(
      @RequestHeader("X-Family-Id") String familyId,
      @RequestParam(required = false) FinanceEquityRange range,
      @RequestParam(required = false) FinanceEquityPeriodMode periodMode,
      @RequestParam(required = false) String currency) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(service.equityTransactions(ctx, range, periodMode, currency));
  }
}

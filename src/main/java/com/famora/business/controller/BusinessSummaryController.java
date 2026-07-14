package com.famora.business.controller;

import com.famora.business.constant.BusinessDefaults;
import com.famora.business.dto.response.BusinessDashboardSummaryResponse;
import com.famora.business.dto.response.BusinessSummaryResponse;
import com.famora.business.dto.response.CashFlowResponse;
import com.famora.business.dto.response.ExpenseCategorySummaryResponse;
import com.famora.business.dto.response.LossSummaryResponse;
import com.famora.business.dto.response.TopSalesItemResponse;
import com.famora.business.service.BusinessSummaryService;
import com.famora.common.dto.ApiResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}")
@RequiredArgsConstructor
public class BusinessSummaryController {
  
  private final BusinessSummaryService summaryService;
  
  @GetMapping("/summary")
  public ApiResponse<BusinessDashboardSummaryResponse> summary(@PathVariable UUID businessId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
    if (fromDate == null && toDate == null) {
      return ApiResponse.ok(summaryService.dashboardPresets(businessId));
    }
    if (fromDate == null || toDate == null) {
      throw new IllegalArgumentException("fromDate and toDate must be provided together");
    }
    if (fromDate.isAfter(toDate)) {
      throw new IllegalArgumentException("fromDate cannot be after toDate");
    }
    return ApiResponse.ok(summaryService.dashboardCustom(businessId, fromDate, toDate));
  }
  
  @GetMapping("/summary/custom")
  public ApiResponse<BusinessSummaryResponse> custom(@PathVariable UUID businessId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
    return ApiResponse.ok(summaryService.summarize(businessId, fromDate, toDate));
  }
  
  @GetMapping("/summary/monthly")
  public ApiResponse<BusinessSummaryResponse> monthly(@PathVariable UUID businessId,
      @RequestParam String month) {
    return ApiResponse.ok(summaryService.monthly(businessId, YearMonth.parse(month)));
  }
  
  @GetMapping("/cash-flow")
  public ApiResponse<CashFlowResponse> cashFlow(@PathVariable UUID businessId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
    return ApiResponse.ok(summaryService.cashFlow(businessId, fromDate, toDate));
  }
  
  @GetMapping("/reports/top-sales-items")
  public ApiResponse<List<TopSalesItemResponse>> topSalesItems(@PathVariable UUID businessId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
      @RequestParam(defaultValue = "" + BusinessDefaults.TOP_SALES_ITEM_LIMIT) int limit) {
    return ApiResponse.ok(summaryService.topSalesItems(businessId, fromDate, toDate, limit));
  }
  
  @GetMapping("/reports/expense-by-category")
  public ApiResponse<List<ExpenseCategorySummaryResponse>> expenseByCategory(
      @PathVariable UUID businessId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
    return ApiResponse.ok(summaryService.expenseByCategory(businessId, fromDate, toDate));
  }
  
  @GetMapping("/reports/loss-summary")
  public ApiResponse<List<LossSummaryResponse>> lossSummary(@PathVariable UUID businessId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
    return ApiResponse.ok(summaryService.lossSummary(businessId, fromDate, toDate));
  }
}

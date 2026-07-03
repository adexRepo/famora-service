package com.famora.business.controller;

import com.famora.business.dto.response.BusinessTransactionResponse;
import com.famora.business.enums.BusinessTransactionType;
import com.famora.business.service.BusinessTransactionService;
import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/transactions")
@RequiredArgsConstructor
public class BusinessTransactionController {
  
  private final BusinessTransactionService transactionService;
  
  @GetMapping
  public ApiResponse<PageResponse<BusinessTransactionResponse>> list(
      @PathVariable UUID businessId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
      @RequestParam(required = false) BusinessTransactionType type,
      Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(
        transactionService.list(businessId, fromDate, toDate, type, pageable)));
  }
}

package com.famora.finance.controller;

import com.famora.finance.dto.CreateFinanceTransactionRequest;
import com.famora.finance.dto.FinanceSummaryResponse;
import com.famora.finance.dto.FinanceTransactionResponse;
import com.famora.finance.dto.UpdateFinanceTransactionRequest;
import com.famora.finance.entity.FinanceTransactionType;
import com.famora.finance.service.FinanceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {
  
  private final FinanceService financeService;
  
  @PostMapping("/transactions")
  public FinanceTransactionResponse create(
      @Valid @RequestBody CreateFinanceTransactionRequest request
  ) {
    return financeService.create(request);
  }
  
  @GetMapping("/transactions")
  public List<FinanceTransactionResponse> list(
      @RequestParam(required = false) String month,
      @RequestParam(required = false) FinanceTransactionType type,
      @RequestParam(required = false) String category
  ) {
    return financeService.list(month, type, category);
  }
  
  @GetMapping("/transactions/{id}")
  public FinanceTransactionResponse getDetail(@PathVariable UUID id) {
    return financeService.getDetail(id);
  }
  
  @PutMapping("/transactions/{id}")
  public FinanceTransactionResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateFinanceTransactionRequest request
  ) {
    return financeService.update(id, request);
  }
  
  @DeleteMapping("/transactions/{id}")
  public Map<String, Object> delete(@PathVariable UUID id) {
    financeService.delete(id);
    return Map.of("success", true);
  }
  
  @GetMapping("/summary")
  public FinanceSummaryResponse getSummary(
      @RequestParam(required = false) String month,
      @RequestParam(required = false) String currency
  ) {
    return financeService.getSummary(month, currency);
  }
}

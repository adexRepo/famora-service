package com.famora.finance.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.family.dto.FamilyContext;
import com.famora.finance.dto.FinanceDebtDtos.CreateDebtPaymentRequest;
import com.famora.finance.dto.FinanceDebtDtos.CreateDebtRequest;
import com.famora.finance.dto.FinanceDebtDtos.DebtDetailResponse;
import com.famora.finance.dto.FinanceDebtDtos.DebtListResponse;
import com.famora.finance.dto.FinanceDebtDtos.DebtPaymentResponse;
import com.famora.finance.dto.FinanceDebtDtos.UpdateDebtRequest;
import com.famora.finance.helper.FinanceDebtStatus;
import com.famora.finance.helper.FinanceDebtType;
import com.famora.finance.service.FinanceDebtService;
import com.famora.security.FamilyContextService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/finance/debts")
@RequiredArgsConstructor
public class FinanceDebtController {
  
  private final FamilyContextService families;
  private final FinanceDebtService service;
  
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<DebtDetailResponse> create(
      @RequestHeader("X-Family-Id") String familyId,
      @Valid @RequestBody CreateDebtRequest request) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(service.create(ctx, request, null));
  }
  
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<DebtDetailResponse> createMultipart(
      @RequestHeader("X-Family-Id") String familyId,
      @Valid @RequestPart("request") CreateDebtRequest request,
      @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(service.create(ctx, request, attachment));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<DebtListResponse>> list(
      @RequestHeader("X-Family-Id") String familyId,
      @RequestParam(required = false) FinanceDebtType type,
      @RequestParam(required = false) FinanceDebtStatus status,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    FamilyContext ctx = families.require(familyId);
    PageRequest pageRequest = PageRequest.of(page, size,
        Sort.by("updatedAt", "createdAt").descending());
    return ApiResponse.ok(PageResponse.from(service.list(ctx, type, status, keyword, pageRequest)));
  }
  
  @GetMapping("/{id}")
  public ApiResponse<DebtDetailResponse> getDetail(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(service.getDetail(ctx, id));
  }
  
  @PutMapping("/{id}")
  public ApiResponse<DebtDetailResponse> update(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateDebtRequest request) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(service.update(ctx, id, request));
  }
  
  @DeleteMapping("/{id}")
  public ApiResponse<Boolean> cancel(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID id) {
    FamilyContext ctx = families.require(familyId);
    service.cancel(ctx, id);
    return ApiResponse.ok("Success", true);
  }
  
  @PostMapping(value = "/{debtId}/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<DebtPaymentResponse> addPayment(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID debtId,
      @Valid @RequestBody CreateDebtPaymentRequest request) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(service.addPayment(ctx, debtId, request, null));
  }
  
  @PostMapping(value = "/{debtId}/payments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<DebtPaymentResponse> addPaymentMultipart(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID debtId,
      @Valid @RequestPart("request") CreateDebtPaymentRequest request,
      @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
    FamilyContext ctx = families.require(familyId);
    return ApiResponse.ok(service.addPayment(ctx, debtId, request, attachment));
  }
  
  @DeleteMapping("/{debtId}/payments/{paymentId}")
  public ApiResponse<Boolean> deletePayment(
      @RequestHeader("X-Family-Id") String familyId,
      @PathVariable UUID debtId,
      @PathVariable UUID paymentId) {
    FamilyContext ctx = families.require(familyId);
    service.deletePayment(ctx, debtId, paymentId);
    return ApiResponse.ok("Success", true);
  }
}

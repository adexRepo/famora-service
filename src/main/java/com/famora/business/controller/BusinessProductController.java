package com.famora.business.controller;

import com.famora.business.constant.BusinessApiMessages;
import com.famora.business.dto.request.CreateProductRequest;
import com.famora.business.dto.request.UpdateProductRequest;
import com.famora.business.dto.response.BusinessProductResponse;
import com.famora.business.service.BusinessProductService;
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
@RequestMapping("/api/v1/businesses/{businessId}/products")
@RequiredArgsConstructor
public class BusinessProductController {
  
  private final BusinessProductService productService;
  
  @PostMapping
  public ResponseEntity<ApiResponse<BusinessProductResponse>> create(@PathVariable UUID businessId,
      @Valid @RequestBody CreateProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(BusinessApiMessages.CREATED,
            productService.create(businessId, request)));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<BusinessProductResponse>> list(@PathVariable UUID businessId,
      Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(productService.list(businessId, pageable)));
  }
  
  @GetMapping("/{productId}")
  public ApiResponse<BusinessProductResponse> get(@PathVariable UUID businessId,
      @PathVariable UUID productId) {
    return ApiResponse.ok(productService.get(businessId, productId));
  }
  
  @PutMapping("/{productId}")
  public ApiResponse<BusinessProductResponse> update(@PathVariable UUID businessId,
      @PathVariable UUID productId,
      @Valid @RequestBody UpdateProductRequest request) {
    return ApiResponse.ok(productService.update(businessId, productId, request));
  }
  
  @DeleteMapping("/{productId}")
  public ApiResponse<Void> delete(@PathVariable UUID businessId, @PathVariable UUID productId) {
    productService.delete(businessId, productId);
    return ApiResponse.ok(null);
  }
}

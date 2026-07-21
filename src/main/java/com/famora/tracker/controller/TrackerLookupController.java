package com.famora.tracker.controller;

import com.famora.business.dto.response.LookupItemResponse;
import com.famora.common.dto.ApiResponse;
import com.famora.tracker.service.TrackerLookupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracker-lookups")
@RequiredArgsConstructor
public class TrackerLookupController {
  
  private final TrackerLookupService lookupService;
  
  @GetMapping("/scope-types")
  public ApiResponse<List<LookupItemResponse>> scopeTypes() {
    return ApiResponse.ok(lookupService.scopeTypes());
  }
  
  @GetMapping("/source-modules")
  public ApiResponse<List<LookupItemResponse>> sourceModules() {
    return ApiResponse.ok(lookupService.sourceModules());
  }
  
  @GetMapping("/tracker-types")
  public ApiResponse<List<LookupItemResponse>> trackerTypes() {
    return ApiResponse.ok(lookupService.trackerTypes());
  }
  
  @GetMapping("/categories")
  public ApiResponse<List<LookupItemResponse>> categories() {
    return ApiResponse.ok(lookupService.categories());
  }
  
  @GetMapping("/frequencies")
  public ApiResponse<List<LookupItemResponse>> frequencies() {
    return ApiResponse.ok(lookupService.frequencies());
  }
  
  @GetMapping("/log-statuses")
  public ApiResponse<List<LookupItemResponse>> logStatuses() {
    return ApiResponse.ok(lookupService.logStatuses());
  }
  
  @GetMapping("/notification-statuses")
  public ApiResponse<List<LookupItemResponse>> notificationStatuses() {
    return ApiResponse.ok(lookupService.notificationStatuses());
  }
}

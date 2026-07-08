package com.famora.business.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.dashboard.DashboardActivityService;
import com.famora.dashboard.RecentActivityResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/recent-activities")
@RequiredArgsConstructor
public class BusinessActivityController {
  
  private final DashboardActivityService activities;
  
  @GetMapping
  public ApiResponse<List<RecentActivityResponse>> recentActivities(@PathVariable UUID businessId,
      @RequestParam(required = false) Integer limit) {
    return ApiResponse.ok(activities.businessActivities(businessId, limit));
  }
}

package com.famora.tracker.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.tracker.dto.CreateTrackerRequest;
import com.famora.tracker.dto.TrackerResponse;
import com.famora.tracker.dto.TrackerTodayResponse;
import com.famora.tracker.dto.UpdateTrackerRequest;
import com.famora.tracker.enums.TrackerCategory;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import com.famora.tracker.enums.TrackerStatus;
import com.famora.tracker.enums.TrackerType;
import com.famora.tracker.service.TrackerService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/trackers")
@RequiredArgsConstructor
public class TrackerController {
  
  private final TrackerService trackerService;
  
  @PostMapping
  public ResponseEntity<ApiResponse<TrackerResponse>> create(
      @Valid @RequestBody CreateTrackerRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("CREATED",
        trackerService.create(request)));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<TrackerResponse>> list(
      @RequestParam(required = false) TrackerScopeType scopeType,
      @RequestParam(required = false) UUID scopeId,
      @RequestParam(required = false) TrackerType trackerType,
      @RequestParam(required = false) TrackerCategory category,
      @RequestParam(required = false) TrackerSourceModule sourceModule,
      @RequestParam(required = false) TrackerStatus status,
      Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(trackerService.list(scopeType, scopeId, trackerType,
        category, sourceModule, status, pageable)));
  }
  
  @GetMapping("/today")
  public ApiResponse<List<TrackerTodayResponse>> today(
      @RequestParam TrackerScopeType scopeType,
      @RequestParam(required = false) UUID scopeId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ApiResponse.ok(trackerService.today(scopeType, scopeId, date));
  }
  
  @GetMapping("/{trackerId}")
  public ApiResponse<TrackerResponse> get(@PathVariable UUID trackerId) {
    return ApiResponse.ok(trackerService.get(trackerId));
  }
  
  @PutMapping("/{trackerId}")
  public ApiResponse<TrackerResponse> update(@PathVariable UUID trackerId,
      @Valid @RequestBody UpdateTrackerRequest request) {
    return ApiResponse.ok(trackerService.update(trackerId, request));
  }
  
  @DeleteMapping("/{trackerId}")
  public ApiResponse<Void> delete(@PathVariable UUID trackerId) {
    trackerService.delete(trackerId);
    return ApiResponse.ok(null);
  }
  
  @PostMapping("/{trackerId}/pause")
  public ApiResponse<TrackerResponse> pause(@PathVariable UUID trackerId) {
    return ApiResponse.ok(trackerService.pause(trackerId));
  }
  
  @PostMapping("/{trackerId}/resume")
  public ApiResponse<TrackerResponse> resume(@PathVariable UUID trackerId) {
    return ApiResponse.ok(trackerService.resume(trackerId));
  }
  
  @PostMapping("/{trackerId}/complete")
  public ApiResponse<TrackerResponse> complete(@PathVariable UUID trackerId) {
    return ApiResponse.ok(trackerService.complete(trackerId));
  }
}

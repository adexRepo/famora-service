package com.famora.tracker.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.tracker.dto.TrackerLogRequest;
import com.famora.tracker.dto.TrackerLogResponse;
import com.famora.tracker.service.TrackerLogService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/trackers/{trackerId}/logs")
@RequiredArgsConstructor
public class TrackerLogController {
  
  private final TrackerLogService logService;
  
  @PostMapping
  public ApiResponse<TrackerLogResponse> create(@PathVariable UUID trackerId,
      @Valid @RequestBody TrackerLogRequest request) {
    return ApiResponse.ok(logService.create(trackerId, request));
  }
  
  @GetMapping
  public ApiResponse<PageResponse<TrackerLogResponse>> list(@PathVariable UUID trackerId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
      Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(logService.list(trackerId, fromDate, toDate,
        pageable)));
  }
  
  @PutMapping("/{logId}")
  public ApiResponse<TrackerLogResponse> update(@PathVariable UUID trackerId,
      @PathVariable UUID logId,
      @Valid @RequestBody TrackerLogRequest request) {
    return ApiResponse.ok(logService.update(trackerId, logId, request));
  }
  
  @DeleteMapping("/{logId}")
  public ApiResponse<Void> delete(@PathVariable UUID trackerId, @PathVariable UUID logId) {
    logService.delete(trackerId, logId);
    return ApiResponse.ok(null);
  }
}

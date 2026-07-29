package com.famora.notification.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.notification.dto.NotificationPreferenceDtos;
import com.famora.notification.enums.NotificationType;
import com.famora.notification.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {
  
  private final NotificationPreferenceService preferenceService;
  
  @GetMapping
  public ApiResponse<NotificationPreferenceDtos.NotificationPreferenceListResponse> list() {
    return ApiResponse.ok(preferenceService.list());
  }
  
  @PutMapping("/{notificationType}")
  public ApiResponse<NotificationPreferenceDtos.NotificationPreferenceResponse> update(
      @PathVariable NotificationType notificationType,
      @Valid @RequestBody NotificationPreferenceDtos.UpdateNotificationPreferenceRequest request) {
    return ApiResponse.ok(preferenceService.update(notificationType, request));
  }
  
  @PutMapping
  public ApiResponse<NotificationPreferenceDtos.NotificationPreferenceListResponse> bulkUpdate(
      @Valid @RequestBody
      NotificationPreferenceDtos.BulkUpdateNotificationPreferenceRequest request) {
    return ApiResponse.ok(preferenceService.bulkUpdate(request));
  }
  
  @PostMapping("/reset")
  public ApiResponse<NotificationPreferenceDtos.NotificationPreferenceListResponse> reset() {
    return ApiResponse.ok(preferenceService.reset());
  }
}

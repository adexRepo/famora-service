package com.famora.notification.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.notification.dto.NotificationUnreadCountResponse;
import com.famora.notification.dto.ScheduledNotificationResponse;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.enums.NotificationReadStatus;
import com.famora.notification.service.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
  
  private final NotificationService notificationService;
  
  @GetMapping
  public ApiResponse<PageResponse<ScheduledNotificationResponse>> list(
      @RequestParam(required = false) NotificationDeliveryStatus deliveryStatus,
      @RequestParam(required = false) NotificationReadStatus readStatus,
      Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(notificationService.list(deliveryStatus, readStatus,
        pageable)));
  }
  
  @GetMapping("/unread-count")
  public ApiResponse<NotificationUnreadCountResponse> unreadCount() {
    return ApiResponse.ok(notificationService.unreadCount());
  }
  
  @PostMapping("/{notificationId}/mark-read")
  public ApiResponse<ScheduledNotificationResponse> markRead(@PathVariable UUID notificationId) {
    return ApiResponse.ok(notificationService.markRead(notificationId));
  }
  
  @PostMapping("/mark-all-read")
  public ApiResponse<NotificationUnreadCountResponse> markAllRead() {
    return ApiResponse.ok(notificationService.markAllRead());
  }
  
  @DeleteMapping("/{notificationId}")
  public ApiResponse<Void> delete(@PathVariable UUID notificationId) {
    notificationService.delete(notificationId);
    return ApiResponse.ok(null);
  }
}

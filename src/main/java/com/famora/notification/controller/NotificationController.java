package com.famora.notification.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.common.dto.PageResponse;
import com.famora.notification.dto.NotificationUnreadCountResponse;
import com.famora.notification.dto.ScheduledNotificationResponse;
import com.famora.notification.dto.WebSocketTicketResponse;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.enums.NotificationReadStatus;
import com.famora.notification.service.NotificationService;
import com.famora.notification.service.WebSocketTicketService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
  
  private final NotificationService notificationService;
  private final WebSocketTicketService webSocketTicketService;

  @PostMapping("/websocket-ticket")
  public ApiResponse<WebSocketTicketResponse> issueWebSocketTicket() {
    return ApiResponse.ok(webSocketTicketService.issue());
  }
  
  @GetMapping
  public ApiResponse<PageResponse<ScheduledNotificationResponse>> list(
      @RequestParam(required = false) NotificationDeliveryStatus deliveryStatus,
      @RequestParam(required = false) NotificationReadStatus readStatus,
      @RequestParam(required = false) Boolean read,
      @RequestParam(required = false) String scope,
      Pageable pageable) {
    return ApiResponse.ok(PageResponse.from(notificationService.list(deliveryStatus, readStatus,
        read, scope, pageable)));
  }
  
  @GetMapping("/unread-count")
  public ApiResponse<NotificationUnreadCountResponse> unreadCount() {
    return ApiResponse.ok(notificationService.unreadCount());
  }
  
  @PutMapping("/{notificationId}/read")
  public ApiResponse<ScheduledNotificationResponse> markRead(@PathVariable UUID notificationId) {
    return ApiResponse.ok(notificationService.markRead(notificationId));
  }
  
  @PutMapping("/read-all")
  public ApiResponse<NotificationUnreadCountResponse> markAllRead() {
    return ApiResponse.ok(notificationService.markAllRead());
  }
  
  @PostMapping("/{notificationId}/mark-read")
  public ApiResponse<ScheduledNotificationResponse> markReadLegacy(
      @PathVariable UUID notificationId) {
    return ApiResponse.ok(notificationService.markRead(notificationId));
  }
  
  @PostMapping("/mark-all-read")
  public ApiResponse<NotificationUnreadCountResponse> markAllReadLegacy() {
    return ApiResponse.ok(notificationService.markAllRead());
  }
  
  @DeleteMapping("/{notificationId}")
  public ApiResponse<Void> delete(@PathVariable UUID notificationId) {
    notificationService.delete(notificationId);
    return ApiResponse.ok(null);
  }
}

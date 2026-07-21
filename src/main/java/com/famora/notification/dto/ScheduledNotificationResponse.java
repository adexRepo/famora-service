package com.famora.notification.dto;

import com.famora.notification.entity.ScheduledNotification;
import com.famora.notification.enums.NotificationChannel;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.enums.NotificationReadStatus;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduledNotificationResponse(
    UUID id,
    UUID trackerId,
    TrackerScopeType scopeType,
    UUID familyId,
    UUID businessId,
    UUID receiverUserId,
    String title,
    String body,
    OffsetDateTime scheduledAt,
    NotificationChannel channel,
    NotificationDeliveryStatus deliveryStatus,
    NotificationReadStatus readStatus,
    TrackerSourceModule sourceModule,
    String sourceEntityType,
    UUID sourceEntityId,
    OffsetDateTime sentAt,
    OffsetDateTime readAt,
    OffsetDateTime createdAt
) {
  
  public static ScheduledNotificationResponse from(ScheduledNotification notification) {
    return new ScheduledNotificationResponse(
        notification.getId(),
        notification.getTracker() == null ? null : notification.getTracker().getId(),
        notification.getScopeType(),
        notification.getFamily() == null ? null : notification.getFamily().getId(),
        notification.getBusiness() == null ? null : notification.getBusiness().getId(),
        notification.getReceiverUser().getId(),
        notification.getTitle(),
        notification.getBody(),
        notification.getScheduledAt(),
        notification.getChannel(),
        notification.getDeliveryStatus(),
        notification.getReadStatus(),
        notification.getSourceModule(),
        notification.getSourceEntityType(),
        notification.getSourceEntityId(),
        notification.getSentAt(),
        notification.getReadAt(),
        notification.getCreatedAt()
    );
  }
}

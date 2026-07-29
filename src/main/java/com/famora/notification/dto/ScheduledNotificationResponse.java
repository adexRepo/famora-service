package com.famora.notification.dto;

import com.famora.notification.entity.ScheduledNotification;
import com.famora.notification.enums.NotificationChannel;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.enums.NotificationEntityType;
import com.famora.notification.enums.NotificationReadStatus;
import com.famora.notification.enums.NotificationType;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduledNotificationResponse(
    UUID id,
    NotificationType type,
    String title,
    String message,
    String scope,
    UUID scopeId,
    NotificationEntityType entityType,
    UUID entityId,
    JsonNode payload,
    boolean read,
    OffsetDateTime readAt,
    OffsetDateTime createdAt,
    UUID trackerId,
    TrackerScopeType scopeType,
    UUID familyId,
    UUID businessId,
    UUID receiverUserId,
    String body,
    OffsetDateTime scheduledAt,
    NotificationChannel channel,
    NotificationDeliveryStatus deliveryStatus,
    NotificationReadStatus readStatus,
    TrackerSourceModule sourceModule,
    String sourceEntityType,
    UUID sourceEntityId,
    OffsetDateTime sentAt
) {
  
  public static ScheduledNotificationResponse from(ScheduledNotification notification) {
    TrackerScopeType scopeType = notification.getScopeType();
    UUID familyId = notification.getFamily() == null ? null : notification.getFamily().getId();
    UUID businessId = notification.getBusiness() == null ? null
        : notification.getBusiness().getId();
    UUID scopeId = switch (scopeType) {
      case FAMILY -> familyId;
      case BUSINESS -> businessId;
      case PERSONAL -> notification.getReceiverUser().getId();
    };
    NotificationEntityType entityType = notification.getEntityType();
    if (entityType == null && notification.getSourceEntityType() != null) {
      entityType = mapEntityType(notification.getSourceEntityType());
    }
    return new ScheduledNotificationResponse(
        notification.getId(),
        notification.getNotificationType(),
        notification.getTitle(),
        notification.getBody(),
        scopeType.name(),
        scopeId,
        entityType,
        notification.getSourceEntityId(),
        notification.getPayloadJson(),
        notification.getReadStatus() == NotificationReadStatus.READ,
        notification.getReadAt(),
        notification.getCreatedAt(),
        notification.getTracker() == null ? null : notification.getTracker().getId(),
        scopeType,
        familyId,
        businessId,
        notification.getReceiverUser().getId(),
        notification.getBody(),
        notification.getScheduledAt(),
        notification.getChannel(),
        notification.getDeliveryStatus(),
        notification.getReadStatus(),
        notification.getSourceModule(),
        notification.getSourceEntityType(),
        notification.getSourceEntityId(),
        notification.getSentAt()
    );
  }
  
  private static NotificationEntityType mapEntityType(String sourceEntityType) {
    for (NotificationEntityType type : NotificationEntityType.values()) {
      if (type.name().equalsIgnoreCase(sourceEntityType)) {
        return type;
      }
    }
    return null;
  }
}

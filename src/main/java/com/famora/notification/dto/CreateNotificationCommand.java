package com.famora.notification.dto;

import com.famora.business.entity.Business;
import com.famora.family.entity.Family;
import com.famora.notification.enums.NotificationEntityType;
import com.famora.notification.enums.NotificationType;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record CreateNotificationCommand(
    NotificationType type,
    String title,
    String message,
    TrackerScopeType scope,
    Family family,
    Business business,
    TrackerSourceModule sourceModule,
    NotificationEntityType entityType,
    UUID entityId,
    OffsetDateTime scheduledAt,
    Map<String, Object> payload
) {
  
}

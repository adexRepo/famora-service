package com.famora.notification.dto;

import com.famora.notification.constant.NotificationPreferenceDefaults;
import com.famora.notification.entity.NotificationPreference;
import com.famora.notification.enums.NotificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

public final class NotificationPreferenceDtos {
  
  private NotificationPreferenceDtos() {
  }
  
  public record NotificationPreferenceListResponse(
      List<NotificationPreferenceResponse> items
  ) {
  
  }
  
  public record NotificationPreferenceResponse(
      NotificationType notificationType,
      boolean inAppEnabled,
      boolean pushEnabled,
      boolean emailEnabled,
      boolean quietHoursEnabled,
      LocalTime quietHoursStart,
      LocalTime quietHoursEnd,
      String timezone
  ) {
    
    public static NotificationPreferenceResponse defaults(NotificationType notificationType) {
      return new NotificationPreferenceResponse(
          notificationType,
          true,
          false,
          false,
          false,
          null,
          null,
          NotificationPreferenceDefaults.TIMEZONE
      );
    }
    
    public static NotificationPreferenceResponse from(NotificationPreference preference) {
      return new NotificationPreferenceResponse(
          preference.getNotificationType(),
          Boolean.TRUE.equals(preference.getInAppEnabled()),
          Boolean.TRUE.equals(preference.getPushEnabled()),
          Boolean.TRUE.equals(preference.getEmailEnabled()),
          Boolean.TRUE.equals(preference.getQuietHoursEnabled()),
          preference.getQuietHoursStart(),
          preference.getQuietHoursEnd(),
          preference.getTimezone()
      );
    }
  }
  
  public record UpdateNotificationPreferenceRequest(
      Boolean inAppEnabled,
      Boolean pushEnabled,
      Boolean emailEnabled,
      Boolean quietHoursEnabled,
      LocalTime quietHoursStart,
      LocalTime quietHoursEnd,
      @Size(max = 80) String timezone
  ) {
  
  }
  
  public record BulkUpdateNotificationPreferenceRequest(
      @Valid List<BulkNotificationPreferenceItemRequest> items
  ) {
  
  }
  
  public record BulkNotificationPreferenceItemRequest(
      NotificationType notificationType,
      Boolean inAppEnabled,
      Boolean pushEnabled,
      Boolean emailEnabled,
      Boolean quietHoursEnabled,
      LocalTime quietHoursStart,
      LocalTime quietHoursEnd,
      @Size(max = 80) String timezone
  ) {
  
  }
}

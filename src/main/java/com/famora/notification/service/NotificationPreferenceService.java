package com.famora.notification.service;

import com.famora.common.exception.BusinessException;
import com.famora.notification.constant.NotificationPreferenceDefaults;
import com.famora.notification.dto.NotificationPreferenceDtos;
import com.famora.notification.entity.NotificationPreference;
import com.famora.notification.enums.NotificationType;
import com.famora.notification.repository.NotificationPreferenceRepository;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {
  
  private final NotificationPreferenceRepository preferenceRepository;
  private final CurrentUserProvider currentUserProvider;
  
  @Transactional(readOnly = true)
  public NotificationPreferenceDtos.NotificationPreferenceListResponse list() {
    User user = currentUserProvider.getCurrentUser();
    Map<NotificationType, NotificationPreference> existing = new EnumMap<>(
        NotificationType.class);
    preferenceRepository.findByUserId(user.getId())
        .forEach(preference -> existing.put(preference.getNotificationType(), preference));
    
    List<NotificationPreferenceDtos.NotificationPreferenceResponse> items = Arrays.stream(
            NotificationType.values())
        .map(type -> existing.containsKey(type)
            ? NotificationPreferenceDtos.NotificationPreferenceResponse.from(existing.get(type))
            : NotificationPreferenceDtos.NotificationPreferenceResponse.defaults(type))
        .toList();
    return new NotificationPreferenceDtos.NotificationPreferenceListResponse(items);
  }
  
  @Transactional
  public NotificationPreferenceDtos.NotificationPreferenceResponse update(
      NotificationType notificationType,
      NotificationPreferenceDtos.UpdateNotificationPreferenceRequest request) {
    User user = currentUserProvider.getCurrentUser();
    NotificationPreference preference = preferenceRepository
        .findByUserIdAndNotificationType(user.getId(), notificationType)
        .orElseGet(() -> createDefaultPreference(user, notificationType));
    apply(preference, request);
    return NotificationPreferenceDtos.NotificationPreferenceResponse.from(
        preferenceRepository.save(preference));
  }
  
  @Transactional
  public NotificationPreferenceDtos.NotificationPreferenceListResponse bulkUpdate(
      NotificationPreferenceDtos.BulkUpdateNotificationPreferenceRequest request) {
    if (request.items() == null || request.items().isEmpty()) {
      return list();
    }
    
    for (NotificationPreferenceDtos.BulkNotificationPreferenceItemRequest item
        : request.items()) {
      if (item.notificationType() == null) {
        throw BusinessException.validation("notificationType is required");
      }
      update(item.notificationType(), toUpdateRequest(item));
    }
    return list();
  }
  
  @Transactional
  public NotificationPreferenceDtos.NotificationPreferenceListResponse reset() {
    preferenceRepository.deleteByUserId(currentUserProvider.getCurrentUserId());
    return list();
  }
  
  @Transactional(readOnly = true)
  public boolean isInAppEnabled(User user, NotificationType notificationType) {
    return preferenceRepository.findByUserIdAndNotificationType(user.getId(), notificationType)
        .map(NotificationPreference::getInAppEnabled)
        .orElse(true);
  }
  
  private NotificationPreference createDefaultPreference(User user, NotificationType type) {
    NotificationPreference preference = new NotificationPreference();
    preference.setUser(user);
    preference.setNotificationType(type);
    preference.setInAppEnabled(true);
    preference.setPushEnabled(false);
    preference.setEmailEnabled(false);
    preference.setQuietHoursEnabled(false);
    preference.setTimezone(NotificationPreferenceDefaults.TIMEZONE);
    return preference;
  }
  
  private void apply(NotificationPreference preference,
      NotificationPreferenceDtos.UpdateNotificationPreferenceRequest request) {
    if (request.inAppEnabled() != null) {
      preference.setInAppEnabled(request.inAppEnabled());
    }
    if (request.pushEnabled() != null) {
      preference.setPushEnabled(request.pushEnabled());
    }
    if (request.emailEnabled() != null) {
      preference.setEmailEnabled(request.emailEnabled());
    }
    if (request.quietHoursEnabled() != null) {
      preference.setQuietHoursEnabled(request.quietHoursEnabled());
    }
    if (request.quietHoursStart() != null) {
      preference.setQuietHoursStart(request.quietHoursStart());
    }
    if (request.quietHoursEnd() != null) {
      preference.setQuietHoursEnd(request.quietHoursEnd());
    }
    if (request.timezone() != null) {
      String timezone = request.timezone().isBlank()
          ? NotificationPreferenceDefaults.TIMEZONE
          : request.timezone().trim();
      preference.setTimezone(timezone);
    }
  }
  
  private NotificationPreferenceDtos.UpdateNotificationPreferenceRequest toUpdateRequest(
      NotificationPreferenceDtos.BulkNotificationPreferenceItemRequest item) {
    return new NotificationPreferenceDtos.UpdateNotificationPreferenceRequest(
        item.inAppEnabled(),
        item.pushEnabled(),
        item.emailEnabled(),
        item.quietHoursEnabled(),
        item.quietHoursStart(),
        item.quietHoursEnd(),
        item.timezone()
    );
  }
}

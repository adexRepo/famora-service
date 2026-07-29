package com.famora.notification.repository;

import com.famora.notification.entity.NotificationPreference;
import com.famora.notification.enums.NotificationType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface NotificationPreferenceRepository
    extends JpaRepository<NotificationPreference, UUID> {
  
  List<NotificationPreference> findByUserId(UUID userId);
  
  List<NotificationPreference> findByUserIdAndNotificationTypeIn(UUID userId,
      Collection<NotificationType> notificationTypes);
  
  Optional<NotificationPreference> findByUserIdAndNotificationType(UUID userId,
      NotificationType notificationType);
  
  @Modifying
  void deleteByUserId(UUID userId);
}

package com.famora.notification.spec;

import com.famora.notification.entity.ScheduledNotification;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.enums.NotificationReadStatus;
import com.famora.tracker.enums.TrackerScopeType;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ScheduledNotificationSpecifications {
  
  private ScheduledNotificationSpecifications() {
  }
  
  public static Specification<ScheduledNotification> receiver(UUID userId) {
    return (root, query, cb) -> cb.equal(root.get("receiverUser").get("id"), userId);
  }
  
  public static Specification<ScheduledNotification> deliveryStatus(
      NotificationDeliveryStatus status) {
    return (root, query, cb) -> status == null ? cb.conjunction()
        : cb.equal(root.get("deliveryStatus"), status);
  }
  
  public static Specification<ScheduledNotification> deliveryStatusNot(
      NotificationDeliveryStatus status) {
    return (root, query, cb) -> status == null ? cb.conjunction()
        : cb.notEqual(root.get("deliveryStatus"), status);
  }
  
  public static Specification<ScheduledNotification> readStatus(NotificationReadStatus status) {
    return (root, query, cb) -> status == null ? cb.conjunction()
        : cb.equal(root.get("readStatus"), status);
  }
  
  public static Specification<ScheduledNotification> read(Boolean read) {
    return (root, query, cb) -> {
      if (read == null) {
        return cb.conjunction();
      }
      NotificationReadStatus status = read ? NotificationReadStatus.READ
          : NotificationReadStatus.UNREAD;
      return cb.equal(root.get("readStatus"), status);
    };
  }
  
  public static Specification<ScheduledNotification> scope(TrackerScopeType scope) {
    return (root, query, cb) -> scope == null ? cb.conjunction()
        : cb.equal(root.get("scopeType"), scope);
  }
}

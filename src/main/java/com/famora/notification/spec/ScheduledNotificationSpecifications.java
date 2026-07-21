package com.famora.notification.spec;

import com.famora.notification.entity.ScheduledNotification;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.enums.NotificationReadStatus;
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
  
  public static Specification<ScheduledNotification> readStatus(NotificationReadStatus status) {
    return (root, query, cb) -> status == null ? cb.conjunction()
        : cb.equal(root.get("readStatus"), status);
  }
}

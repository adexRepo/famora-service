package com.famora.notification.service;

import com.famora.audit.entity.AuditAction;
import com.famora.common.exception.BusinessException;
import com.famora.notification.dto.NotificationUnreadCountResponse;
import com.famora.notification.dto.ScheduledNotificationResponse;
import com.famora.notification.entity.ScheduledNotification;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.enums.NotificationReadStatus;
import com.famora.notification.repository.ScheduledNotificationRepository;
import com.famora.notification.spec.ScheduledNotificationSpecifications;
import com.famora.security.CurrentUserProvider;
import com.famora.tracker.service.TrackerAuditService;
import com.famora.user.entity.User;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
  
  private final ScheduledNotificationRepository notificationRepository;
  private final CurrentUserProvider currentUserProvider;
  private final TrackerAuditService auditService;
  
  @Transactional(readOnly = true)
  public Page<ScheduledNotificationResponse> list(NotificationDeliveryStatus deliveryStatus,
      NotificationReadStatus readStatus, Pageable pageable) {
    UUID userId = currentUserProvider.getCurrentUserId();
    return notificationRepository.findAll(
        ScheduledNotificationSpecifications.receiver(userId)
            .and(ScheduledNotificationSpecifications.deliveryStatus(deliveryStatus))
            .and(ScheduledNotificationSpecifications.readStatus(readStatus)),
        pageable).map(ScheduledNotificationResponse::from);
  }
  
  @Transactional(readOnly = true)
  public NotificationUnreadCountResponse unreadCount() {
    UUID userId = currentUserProvider.getCurrentUserId();
    return new NotificationUnreadCountResponse(
        notificationRepository.countByReceiverUser_IdAndReadStatusAndDeliveryStatusNot(userId,
            NotificationReadStatus.UNREAD, NotificationDeliveryStatus.CANCELLED));
  }
  
  @Transactional
  public ScheduledNotificationResponse markRead(UUID notificationId) {
    User user = currentUserProvider.getCurrentUser();
    ScheduledNotification notification = requireOwnNotification(notificationId, user.getId());
    notification.setReadStatus(NotificationReadStatus.READ);
    notification.setReadAt(OffsetDateTime.now());
    notification = notificationRepository.save(notification);
    if (notification.getTracker() != null) {
      auditService.log(notification.getTracker(), AuditAction.NOTIFICATION_MARKED_READ,
          "scheduled_notifications", notification.getId(), null);
    }
    return ScheduledNotificationResponse.from(notification);
  }
  
  @Transactional
  public NotificationUnreadCountResponse markAllRead() {
    UUID userId = currentUserProvider.getCurrentUserId();
    notificationRepository.markAllReadByReceiverUserId(userId, NotificationReadStatus.UNREAD,
        NotificationReadStatus.READ, OffsetDateTime.now());
    return unreadCount();
  }
  
  @Transactional
  public void delete(UUID notificationId) {
    UUID userId = currentUserProvider.getCurrentUserId();
    ScheduledNotification notification = requireOwnNotification(notificationId, userId);
    notification.setDeliveryStatus(NotificationDeliveryStatus.CANCELLED);
    notification.setReadStatus(NotificationReadStatus.READ);
    notification.setReadAt(notification.getReadAt() == null ? OffsetDateTime.now()
        : notification.getReadAt());
    notificationRepository.save(notification);
  }
  
  private ScheduledNotification requireOwnNotification(UUID notificationId, UUID userId) {
    return notificationRepository.findById(notificationId)
        .filter(notification -> notification.getReceiverUser().getId().equals(userId))
        .orElseThrow(() -> BusinessException.notFound("Notification not found"));
  }
}

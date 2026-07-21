package com.famora.notification.repository;

import com.famora.notification.entity.ScheduledNotification;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.enums.NotificationReadStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduledNotificationRepository
    extends JpaRepository<ScheduledNotification, UUID>,
    JpaSpecificationExecutor<ScheduledNotification> {
  
  long countByReceiverUser_IdAndReadStatus(UUID receiverUserId, NotificationReadStatus readStatus);
  
  long countByReceiverUser_IdAndReadStatusAndDeliveryStatusNot(UUID receiverUserId,
      NotificationReadStatus readStatus, NotificationDeliveryStatus deliveryStatus);
  
  @Modifying
  @Query("""
      update ScheduledNotification n
      set n.deliveryStatus = :cancelled
      where n.tracker.id = :trackerId
        and n.deliveryStatus = :pending
        and n.scheduledAt >= :now
      """)
  int cancelFuturePendingByTrackerId(@Param("trackerId") UUID trackerId,
      @Param("pending") NotificationDeliveryStatus pending,
      @Param("cancelled") NotificationDeliveryStatus cancelled,
      @Param("now") OffsetDateTime now);
  
  @Modifying
  @Query("""
      update ScheduledNotification n
      set n.readStatus = :readStatus,
          n.readAt = :readAt
      where n.receiverUser.id = :receiverUserId
        and n.readStatus = :unreadStatus
      """)
  int markAllReadByReceiverUserId(@Param("receiverUserId") UUID receiverUserId,
      @Param("unreadStatus") NotificationReadStatus unreadStatus,
      @Param("readStatus") NotificationReadStatus readStatus,
      @Param("readAt") OffsetDateTime readAt);
}

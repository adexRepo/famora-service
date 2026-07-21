package com.famora.notification.entity;

import com.famora.business.entity.Business;
import com.famora.common.entity.BaseEntity;
import com.famora.family.entity.Family;
import com.famora.notification.enums.NotificationChannel;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.enums.NotificationReadStatus;
import com.famora.tracker.entity.Tracker;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import com.famora.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "scheduled_notifications")
public class ScheduledNotification extends BaseEntity {
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tracker_id")
  private Tracker tracker;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "scope_type", nullable = false, length = 30)
  private TrackerScopeType scopeType;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "family_id")
  private Family family;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "business_id")
  private Business business;
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "receiver_user_id", nullable = false)
  private User receiverUser;
  
  @Column(nullable = false, length = 180)
  private String title;
  
  @Column(columnDefinition = "text")
  private String body;
  
  @Column(name = "scheduled_at", nullable = false)
  private OffsetDateTime scheduledAt;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NotificationChannel channel;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_status", nullable = false, length = 30)
  private NotificationDeliveryStatus deliveryStatus;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "read_status", nullable = false, length = 30)
  private NotificationReadStatus readStatus;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "source_module", nullable = false, length = 50)
  private TrackerSourceModule sourceModule;
  
  @Column(name = "source_entity_type", length = 80)
  private String sourceEntityType;
  
  @Column(name = "source_entity_id", columnDefinition = "uuid")
  private UUID sourceEntityId;
  
  @Column(name = "sent_at")
  private OffsetDateTime sentAt;
  
  @Column(name = "read_at")
  private OffsetDateTime readAt;
  
  @PrePersist
  public void applyDefaults() {
    if (channel == null) {
      channel = NotificationChannel.PUSH;
    }
    if (deliveryStatus == null) {
      deliveryStatus = NotificationDeliveryStatus.PENDING;
    }
    if (readStatus == null) {
      readStatus = NotificationReadStatus.UNREAD;
    }
  }
}

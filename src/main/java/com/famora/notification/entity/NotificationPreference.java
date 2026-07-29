package com.famora.notification.entity;

import com.famora.common.entity.BaseEntity;
import com.famora.notification.constant.NotificationPreferenceDefaults;
import com.famora.notification.enums.NotificationType;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_preferences",
    uniqueConstraints = @UniqueConstraint(name = "uk_notification_preferences_user_type",
        columnNames = {"user_id", "notification_type"}))
public class NotificationPreference extends BaseEntity {
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false, length = 80)
  private NotificationType notificationType;
  
  @Column(name = "in_app_enabled", nullable = false)
  private Boolean inAppEnabled;
  
  @Column(name = "push_enabled", nullable = false)
  private Boolean pushEnabled;
  
  @Column(name = "email_enabled", nullable = false)
  private Boolean emailEnabled;
  
  @Column(name = "quiet_hours_enabled", nullable = false)
  private Boolean quietHoursEnabled;
  
  @Column(name = "quiet_hours_start")
  private LocalTime quietHoursStart;
  
  @Column(name = "quiet_hours_end")
  private LocalTime quietHoursEnd;
  
  @Column(nullable = false, length = 80)
  private String timezone;
  
  @PrePersist
  public void applyDefaults() {
    if (inAppEnabled == null) {
      inAppEnabled = true;
    }
    if (pushEnabled == null) {
      pushEnabled = false;
    }
    if (emailEnabled == null) {
      emailEnabled = false;
    }
    if (quietHoursEnabled == null) {
      quietHoursEnabled = false;
    }
    if (timezone == null || timezone.isBlank()) {
      timezone = NotificationPreferenceDefaults.TIMEZONE;
    }
  }
}

package com.famora.tracker.entity;

import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessMember;
import com.famora.common.entity.BaseEntity;
import com.famora.family.entity.Family;
import com.famora.family.entity.FamilyMember;
import com.famora.tracker.enums.TrackerCategory;
import com.famora.tracker.enums.TrackerFrequency;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import com.famora.tracker.enums.TrackerStatus;
import com.famora.tracker.enums.TrackerType;
import com.famora.tracker.enums.TrackerVisibility;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "trackers")
public class Tracker extends BaseEntity {
  
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
  @JoinColumn(name = "owner_user_id", nullable = false)
  private User ownerUser;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "source_module", nullable = false, length = 50)
  private TrackerSourceModule sourceModule;
  
  @Column(name = "source_entity_type", length = 80)
  private String sourceEntityType;
  
  @Column(name = "source_entity_id", columnDefinition = "uuid")
  private UUID sourceEntityId;
  
  @Column(nullable = false, length = 180)
  private String title;
  
  @Column(columnDefinition = "text")
  private String description;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "tracker_type", nullable = false, length = 80)
  private TrackerType trackerType;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 80)
  private TrackerCategory category;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_user_id")
  private User assignedUser;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_family_member_id")
  private FamilyMember assignedFamilyMember;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_business_member_id")
  private BusinessMember assignedBusinessMember;
  
  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;
  
  @Column(name = "due_date")
  private LocalDate dueDate;
  
  @Column(name = "reminder_time")
  private LocalTime reminderTime;
  
  @Column(nullable = false, length = 80)
  private String timezone;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TrackerFrequency frequency;
  
  @Column(name = "interval_value", nullable = false)
  private Integer intervalValue;
  
  @Column(name = "days_of_week", length = 120)
  private String daysOfWeek;
  
  @Column(name = "day_of_month")
  private Integer dayOfMonth;
  
  @Column(name = "notify_delay_minutes", nullable = false)
  private Integer notifyDelayMinutes;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TrackerVisibility visibility;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TrackerStatus status;
  
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdByUser;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by_user_id")
  private User updatedByUser;
  
  @PrePersist
  public void applyDefaults() {
    if (timezone == null || timezone.isBlank()) {
      timezone = "Asia/Kuala_Lumpur";
    }
    if (intervalValue == null) {
      intervalValue = 1;
    }
    if (notifyDelayMinutes == null) {
      notifyDelayMinutes = 0;
    }
    if (visibility == null) {
      visibility = TrackerVisibility.FAMILY;
    }
    if (status == null) {
      status = TrackerStatus.ACTIVE;
    }
  }
}

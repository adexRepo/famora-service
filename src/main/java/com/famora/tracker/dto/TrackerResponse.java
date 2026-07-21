package com.famora.tracker.dto;

import com.famora.tracker.entity.Tracker;
import com.famora.tracker.enums.TrackerCategory;
import com.famora.tracker.enums.TrackerFrequency;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import com.famora.tracker.enums.TrackerStatus;
import com.famora.tracker.enums.TrackerType;
import com.famora.tracker.enums.TrackerVisibility;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TrackerResponse(
    UUID id,
    TrackerScopeType scopeType,
    UUID scopeId,
    UUID ownerUserId,
    TrackerSourceModule sourceModule,
    String sourceEntityType,
    UUID sourceEntityId,
    String title,
    String description,
    TrackerType trackerType,
    TrackerCategory category,
    UUID assignedUserId,
    UUID assignedFamilyMemberId,
    UUID assignedBusinessMemberId,
    LocalDate startDate,
    LocalDate dueDate,
    LocalTime reminderTime,
    String timezone,
    TrackerFrequency frequency,
    Integer intervalValue,
    List<String> daysOfWeek,
    Integer dayOfMonth,
    Integer notifyDelayMinutes,
    TrackerVisibility visibility,
    TrackerStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
  
  public static TrackerResponse from(Tracker tracker) {
    UUID scopeId = switch (tracker.getScopeType()) {
      case FAMILY -> tracker.getFamily() == null ? null : tracker.getFamily().getId();
      case BUSINESS -> tracker.getBusiness() == null ? null : tracker.getBusiness().getId();
      case PERSONAL -> null;
    };
    return new TrackerResponse(
        tracker.getId(),
        tracker.getScopeType(),
        scopeId,
        tracker.getOwnerUser().getId(),
        tracker.getSourceModule(),
        tracker.getSourceEntityType(),
        tracker.getSourceEntityId(),
        tracker.getTitle(),
        tracker.getDescription(),
        tracker.getTrackerType(),
        tracker.getCategory(),
        tracker.getAssignedUser() == null ? null : tracker.getAssignedUser().getId(),
        tracker.getAssignedFamilyMember() == null ? null : tracker.getAssignedFamilyMember().getId(),
        tracker.getAssignedBusinessMember() == null ? null : tracker.getAssignedBusinessMember().getId(),
        tracker.getStartDate(),
        tracker.getDueDate(),
        tracker.getReminderTime(),
        tracker.getTimezone(),
        tracker.getFrequency(),
        tracker.getIntervalValue(),
        tracker.getDaysOfWeek() == null || tracker.getDaysOfWeek().isBlank()
            ? List.of()
            : List.of(tracker.getDaysOfWeek().split(",")),
        tracker.getDayOfMonth(),
        tracker.getNotifyDelayMinutes(),
        tracker.getVisibility(),
        tracker.getStatus(),
        tracker.getCreatedAt(),
        tracker.getUpdatedAt()
    );
  }
}

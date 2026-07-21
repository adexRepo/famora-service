package com.famora.tracker.dto;

import com.famora.tracker.enums.TrackerCategory;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import com.famora.tracker.enums.TrackerStatus;
import com.famora.tracker.enums.TrackerType;
import com.famora.tracker.enums.TrackerVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateTrackerRequest(
    @NotNull TrackerScopeType scopeType,
    UUID scopeId,
    @NotNull TrackerSourceModule sourceModule,
    @Size(max = 80) String sourceEntityType,
    UUID sourceEntityId,
    @NotBlank @Size(max = 180) String title,
    String description,
    @NotNull TrackerType trackerType,
    @NotNull TrackerCategory category,
    UUID assignedMemberId,
    UUID assignedUserId,
    @NotNull LocalDate startDate,
    LocalDate dueDate,
    LocalTime reminderTime,
    @Size(max = 80) String timezone,
    @NotNull @Valid RecurrenceRequest recurrence,
    @Min(0) Integer notifyDelayMinutes,
    TrackerVisibility visibility,
    TrackerStatus status
) {
}

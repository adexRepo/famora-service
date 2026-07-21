package com.famora.tracker.dto;

import com.famora.tracker.entity.TrackerLog;
import com.famora.tracker.enums.TrackerLogStatus;
import com.famora.tracker.enums.TrackerScopeType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TrackerLogResponse(
    UUID id,
    UUID trackerId,
    TrackerScopeType scopeType,
    UUID familyId,
    UUID businessId,
    UUID loggedByUserId,
    LocalDate logDate,
    TrackerLogStatus status,
    String value,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
  
  public static TrackerLogResponse from(TrackerLog log) {
    return new TrackerLogResponse(
        log.getId(),
        log.getTracker().getId(),
        log.getScopeType(),
        log.getFamily() == null ? null : log.getFamily().getId(),
        log.getBusiness() == null ? null : log.getBusiness().getId(),
        log.getLoggedByUser().getId(),
        log.getLogDate(),
        log.getStatus(),
        log.getValue(),
        log.getNotes(),
        log.getCreatedAt(),
        log.getUpdatedAt()
    );
  }
}

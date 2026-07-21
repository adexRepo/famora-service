package com.famora.tracker.dto;

import com.famora.tracker.enums.TrackerLogStatus;
import java.time.LocalDate;
import java.util.UUID;

public record TrackerTodayResponse(
    TrackerResponse tracker,
    LocalDate date,
    boolean dueToday,
    boolean overdue,
    UUID logId,
    TrackerLogStatus logStatus
) {
}

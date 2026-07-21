package com.famora.tracker.dto;

import com.famora.tracker.enums.TrackerLogStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TrackerLogRequest(
    @NotNull LocalDate logDate,
    @NotNull TrackerLogStatus status,
    @Size(max = 100) String value,
    String notes
) {
}

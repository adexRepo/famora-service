package com.famora.tracker.dto;

import com.famora.tracker.enums.TrackerFrequency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.util.List;

public record RecurrenceRequest(
    @NotNull TrackerFrequency frequency,
    @Min(1) Integer interval,
    List<DayOfWeek> daysOfWeek,
    Integer dayOfMonth
) {
}

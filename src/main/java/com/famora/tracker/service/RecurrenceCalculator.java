package com.famora.tracker.service;

import com.famora.common.exception.BusinessException;
import com.famora.tracker.entity.Tracker;
import com.famora.tracker.enums.TrackerFrequency;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecurrenceCalculator {
  
  public List<LocalDate> occurrencesBetween(Tracker tracker, LocalDate fromDate,
      LocalDate toDate) {
    validateRange(fromDate, toDate);
    return fromDate.datesUntil(toDate.plusDays(1))
        .filter(date -> isDueOn(tracker, date))
        .toList();
  }
  
  public boolean isDueOn(Tracker tracker, LocalDate date) {
    if (date == null || date.isBefore(tracker.getStartDate())) {
      return false;
    }
    if (tracker.getDueDate() != null && date.isAfter(tracker.getDueDate())
        && tracker.getFrequency() == TrackerFrequency.ONCE) {
      return false;
    }
    int interval = tracker.getIntervalValue() == null ? 1 : tracker.getIntervalValue();
    return switch (tracker.getFrequency()) {
      case ONCE -> tracker.getDueDate() != null && tracker.getDueDate().equals(date);
      case DAILY -> daysBetween(tracker.getStartDate(), date) % interval == 0;
      case WEEKLY -> isWeeklyDue(tracker, date, interval);
      case MONTHLY -> isMonthlyDue(tracker, date, interval);
      case YEARLY -> isYearlyDue(tracker, date, interval);
      case CUSTOM -> false;
    };
  }
  
  public boolean hasDueBefore(Tracker tracker, LocalDate date) {
    if (date == null || !date.isAfter(tracker.getStartDate())) {
      return false;
    }
    return occurrencesBetween(tracker, tracker.getStartDate(), date.minusDays(1))
        .stream()
        .anyMatch(dueDate -> tracker.getDueDate() == null || !dueDate.isAfter(tracker.getDueDate()));
  }
  
  private boolean isWeeklyDue(Tracker tracker, LocalDate date, int interval) {
    Set<DayOfWeek> days = daysOfWeek(tracker.getDaysOfWeek());
    if (!days.contains(date.getDayOfWeek())) {
      return false;
    }
    long weeks = daysBetween(tracker.getStartDate(), date) / 7;
    return weeks % interval == 0;
  }
  
  private boolean isMonthlyDue(Tracker tracker, LocalDate date, int interval) {
    Integer dayOfMonth = tracker.getDayOfMonth();
    if (dayOfMonth == null || date.getDayOfMonth() != dayOfMonth) {
      return false;
    }
    int months = (date.getYear() - tracker.getStartDate().getYear()) * 12
        + date.getMonthValue() - tracker.getStartDate().getMonthValue();
    return months >= 0 && months % interval == 0;
  }
  
  private boolean isYearlyDue(Tracker tracker, LocalDate date, int interval) {
    LocalDate source = tracker.getDueDate() == null ? tracker.getStartDate() : tracker.getDueDate();
    if (date.getMonth() != source.getMonth() || date.getDayOfMonth() != source.getDayOfMonth()) {
      return false;
    }
    int years = date.getYear() - tracker.getStartDate().getYear();
    return years >= 0 && years % interval == 0;
  }
  
  private Set<DayOfWeek> daysOfWeek(String raw) {
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .map(DayOfWeek::valueOf)
        .collect(Collectors.toSet());
  }
  
  private long daysBetween(LocalDate startDate, LocalDate date) {
    return java.time.temporal.ChronoUnit.DAYS.between(startDate, date);
  }
  
  private void validateRange(LocalDate fromDate, LocalDate toDate) {
    if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
      throw BusinessException.validation("Invalid recurrence date range");
    }
  }
}

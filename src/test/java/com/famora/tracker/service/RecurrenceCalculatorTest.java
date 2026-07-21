package com.famora.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.famora.tracker.entity.Tracker;
import com.famora.tracker.enums.TrackerFrequency;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecurrenceCalculatorTest {
  
  private final RecurrenceCalculator calculator = new RecurrenceCalculator();
  
  @Test
  void dailyTrackerIsDueEveryIntervalDays() {
    Tracker tracker = tracker(TrackerFrequency.DAILY);
    tracker.setIntervalValue(2);
    
    assertThat(calculator.isDueOn(tracker, LocalDate.of(2026, 7, 17))).isTrue();
    assertThat(calculator.isDueOn(tracker, LocalDate.of(2026, 7, 18))).isFalse();
    assertThat(calculator.isDueOn(tracker, LocalDate.of(2026, 7, 19))).isTrue();
  }
  
  @Test
  void weeklyTrackerIsDueOnlyOnConfiguredWeekdays() {
    Tracker tracker = tracker(TrackerFrequency.WEEKLY);
    tracker.setDaysOfWeek("MONDAY,WEDNESDAY,FRIDAY");
    
    assertThat(calculator.isDueOn(tracker, LocalDate.of(2026, 7, 17))).isTrue();
    assertThat(calculator.isDueOn(tracker, LocalDate.of(2026, 7, 18))).isFalse();
  }
  
  @Test
  void monthlyTrackerIsDueOnConfiguredDayOfMonth() {
    Tracker tracker = tracker(TrackerFrequency.MONTHLY);
    tracker.setDayOfMonth(25);
    
    assertThat(calculator.isDueOn(tracker, LocalDate.of(2026, 7, 25))).isTrue();
    assertThat(calculator.isDueOn(tracker, LocalDate.of(2026, 7, 24))).isFalse();
  }
  
  @Test
  void onceTrackerUsesDueDate() {
    Tracker tracker = tracker(TrackerFrequency.ONCE);
    tracker.setDueDate(LocalDate.of(2026, 10, 15));
    
    assertThat(calculator.isDueOn(tracker, LocalDate.of(2026, 10, 15))).isTrue();
    assertThat(calculator.isDueOn(tracker, LocalDate.of(2026, 10, 14))).isFalse();
  }
  
  private Tracker tracker(TrackerFrequency frequency) {
    Tracker tracker = new Tracker();
    tracker.setStartDate(LocalDate.of(2026, 7, 17));
    tracker.setFrequency(frequency);
    tracker.setIntervalValue(1);
    return tracker;
  }
}

package com.famora.prayer.dto;

import java.time.LocalDate;

public record PrayerTimesResponse(
    LocalDate date,
    String timezone,
    PrayerLocationResponse location,
    PrayerTimes times,
    HijriDateResponse hijriDate,
    PrayerCalculationMethodResponse calculationMethod,
    String source,
    boolean cached
) {
  
  public record PrayerLocationResponse(
      double latitude,
      double longitude
  ) {
  
  }
  
  public record PrayerTimes(
      String fajr,
      String sunrise,
      String dhuhr,
      String asr,
      String maghrib,
      String isha,
      String imsak,
      String sunset,
      String midnight
  ) {
  
  }
  
  public record HijriDateResponse(
      String date,
      String month,
      String year
  ) {
  
  }
  
  public record PrayerCalculationMethodResponse(
      int id,
      String name,
      int school
  ) {
  
  }
}

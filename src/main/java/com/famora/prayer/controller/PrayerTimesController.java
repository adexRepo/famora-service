package com.famora.prayer.controller;

import com.famora.common.dto.ApiResponse;
import com.famora.prayer.dto.PrayerTimesResponse;
import com.famora.prayer.service.PrayerTimesService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prayer-times")
@RequiredArgsConstructor
public class PrayerTimesController {
  
  private final PrayerTimesService prayerTimesService;
  
  @GetMapping
  public ApiResponse<PrayerTimesResponse> getPrayerTimes(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate date,
      @RequestParam double latitude,
      @RequestParam double longitude,
      @RequestParam(required = false) Integer method,
      @RequestParam(required = false) Integer school
  ) {
    return ApiResponse.ok(prayerTimesService.getPrayerTimes(
        date,
        latitude,
        longitude,
        method,
        school
    ));
  }
}

package com.famora.prayer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.prayer-times")
public record PrayerTimesProperties(
    String aladhanBaseUrl,
    int defaultMethod,
    int defaultSchool,
    int cacheHours
) {
  
}

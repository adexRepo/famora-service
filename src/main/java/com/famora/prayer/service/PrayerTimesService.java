package com.famora.prayer.service;

import com.famora.common.exception.AppException;
import com.famora.prayer.config.PrayerTimesProperties;
import com.famora.prayer.dto.AladhanTimingsResponse;
import com.famora.prayer.dto.PrayerTimesResponse;
import com.famora.prayer.dto.PrayerTimesResponse.HijriDateResponse;
import com.famora.prayer.dto.PrayerTimesResponse.PrayerCalculationMethodResponse;
import com.famora.prayer.dto.PrayerTimesResponse.PrayerLocationResponse;
import com.famora.prayer.dto.PrayerTimesResponse.PrayerTimes;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PrayerTimesService {
  
  private static final DateTimeFormatter ALADHAN_DATE_FORMAT = DateTimeFormatter.ofPattern(
      "dd-MM-yyyy");
  private static final String SOURCE = "ALADHAN";
  
  private final RestClient restClient;
  private final PrayerTimesProperties properties;
  
  private Cache<PrayerTimesCacheKey, PrayerTimesResponse> cache;
  
  @PostConstruct
  void initCache() {
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(properties.cacheHours()))
        .maximumSize(10_000)
        .build();
  }
  
  public PrayerTimesResponse getPrayerTimes(LocalDate date, double latitude, double longitude,
      Integer method, Integer school) {
    LocalDate targetDate = date == null ? LocalDate.now() : date;
    int calculationMethod = method == null ? properties.defaultMethod() : method;
    int asrSchool = school == null ? properties.defaultSchool() : school;
    validateCoordinates(latitude, longitude);
    
    PrayerTimesCacheKey cacheKey = new PrayerTimesCacheKey(
        targetDate,
        normalizeCoordinate(latitude),
        normalizeCoordinate(longitude),
        calculationMethod,
        asrSchool
    );
    
    PrayerTimesResponse cached = cache.getIfPresent(cacheKey);
    if (cached != null) {
      return copyWithCached(cached, true);
    }
    
    PrayerTimesResponse response = fetchPrayerTimes(cacheKey);
    cache.put(cacheKey, response);
    return response;
  }
  
  private PrayerTimesResponse fetchPrayerTimes(PrayerTimesCacheKey cacheKey) {
    AladhanTimingsResponse response = restClient.get()
        .uri(properties.aladhanBaseUrl()
                + "/timings/{date}?latitude={latitude}&longitude={longitude}&method={method}"
                + "&school={school}",
            cacheKey.date().format(ALADHAN_DATE_FORMAT),
            cacheKey.latitude(),
            cacheKey.longitude(),
            cacheKey.method(),
            cacheKey.school())
        .retrieve()
        .body(AladhanTimingsResponse.class);
    
    if (response == null || response.code() == null || response.code() != 200
        || response.data() == null || response.data().timings() == null) {
      throw new AppException(HttpStatus.BAD_GATEWAY, "Failed to fetch prayer times");
    }
    
    return normalize(cacheKey, response);
  }
  
  private PrayerTimesResponse normalize(PrayerTimesCacheKey cacheKey,
      AladhanTimingsResponse response) {
    AladhanTimingsResponse.Data data = response.data();
    AladhanTimingsResponse.Timings timings = data.timings();
    AladhanTimingsResponse.Meta meta = data.meta();
    AladhanTimingsResponse.Hijri hijri = data.date() == null ? null : data.date().hijri();
    AladhanTimingsResponse.Method method = meta == null ? null : meta.method();
    
    return new PrayerTimesResponse(
        cacheKey.date(),
        meta == null ? null : meta.timezone(),
        new PrayerLocationResponse(cacheKey.latitude(), cacheKey.longitude()),
        new PrayerTimes(
            cleanTime(timings.fajr()),
            cleanTime(timings.sunrise()),
            cleanTime(timings.dhuhr()),
            cleanTime(timings.asr()),
            cleanTime(timings.maghrib()),
            cleanTime(timings.isha()),
            cleanTime(timings.imsak()),
            cleanTime(timings.sunset()),
            cleanTime(timings.midnight())
        ),
        hijriDate(hijri),
        new PrayerCalculationMethodResponse(
            method == null || method.id() == null ? cacheKey.method() : method.id(),
            method == null ? null : method.name(),
            cacheKey.school()
        ),
        SOURCE,
        false
    );
  }
  
  private HijriDateResponse hijriDate(AladhanTimingsResponse.Hijri hijri) {
    if (hijri == null) {
      return null;
    }
    return new HijriDateResponse(
        hijri.date(),
        hijri.month() == null ? null : hijri.month().en(),
        hijri.year()
    );
  }
  
  private PrayerTimesResponse copyWithCached(PrayerTimesResponse response, boolean cached) {
    return new PrayerTimesResponse(
        response.date(),
        response.timezone(),
        response.location(),
        response.times(),
        response.hijriDate(),
        response.calculationMethod(),
        response.source(),
        cached
    );
  }
  
  private void validateCoordinates(double latitude, double longitude) {
    if (latitude < -90 || latitude > 90) {
      throw new AppException(HttpStatus.BAD_REQUEST, "latitude must be between -90 and 90");
    }
    if (longitude < -180 || longitude > 180) {
      throw new AppException(HttpStatus.BAD_REQUEST, "longitude must be between -180 and 180");
    }
  }
  
  private double normalizeCoordinate(double value) {
    return BigDecimal.valueOf(value)
        .setScale(4, RoundingMode.HALF_UP)
        .doubleValue();
  }
  
  private String cleanTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String clean = value.trim();
    return clean.length() >= 5 ? clean.substring(0, 5) : clean;
  }
  
  private record PrayerTimesCacheKey(
      LocalDate date,
      double latitude,
      double longitude,
      int method,
      int school
  ) {
  
  }
}

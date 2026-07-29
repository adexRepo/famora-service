package com.famora.prayer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AladhanTimingsResponse(
    Integer code,
    String status,
    Data data
) {
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(
      Timings timings,
      DateInfo date,
      Meta meta
  ) {
  
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Timings(
      @JsonProperty("Fajr") String fajr,
      @JsonProperty("Sunrise") String sunrise,
      @JsonProperty("Dhuhr") String dhuhr,
      @JsonProperty("Asr") String asr,
      @JsonProperty("Sunset") String sunset,
      @JsonProperty("Maghrib") String maghrib,
      @JsonProperty("Isha") String isha,
      @JsonProperty("Imsak") String imsak,
      @JsonProperty("Midnight") String midnight
  ) {
  
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record DateInfo(
      Gregorian gregorian,
      Hijri hijri
  ) {
  
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Gregorian(
      String date
  ) {
  
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Hijri(
      String date,
      Month month,
      String year
  ) {
  
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Month(
      String en
  ) {
  
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Meta(
      Double latitude,
      Double longitude,
      String timezone,
      Method method,
      Map<String, Object> offset
  ) {
  
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Method(
      Integer id,
      String name
  ) {
  
  }
}

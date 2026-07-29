package com.famora;

import com.famora.currency.config.CurrencyProperties;
import com.famora.prayer.config.PrayerTimesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({CurrencyProperties.class, PrayerTimesProperties.class})
public class FamoraApplication {
  
  public static void main(String[] args) {
    SpringApplication.run(FamoraApplication.class, args);
  }
  
}

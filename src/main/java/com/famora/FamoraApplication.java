package com.famora;

import com.famora.common.cache.RedisCacheProperties;
import com.famora.common.config.EnvironmentNamespaceProperties;
import com.famora.currency.config.CurrencyProperties;
import com.famora.family.config.FamilyProperties;
import com.famora.prayer.config.PrayerTimesProperties;
import com.famora.security.config.CorsProperties;
import com.famora.security.config.RateLimitProperties;
import com.famora.security.jwt.JwtProperties;
import com.famora.vault.config.VaultProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
    CurrencyProperties.class,
    EnvironmentNamespaceProperties.class,
    RedisCacheProperties.class,
    PrayerTimesProperties.class,
    FamilyProperties.class,
    CorsProperties.class,
    RateLimitProperties.class,
    JwtProperties.class,
    VaultProperties.class
})
public class FamoraApplication {

  public static void main(String[] args) {
    SpringApplication.run(FamoraApplication.class, args);
  }

}

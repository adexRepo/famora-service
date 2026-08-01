package com.famora.family.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.family")
public record FamilyProperties(
    Integer maxActiveFamilyPerUser
) {

  public int effectiveMaxActiveFamilyPerUser() {
    return maxActiveFamilyPerUser == null || maxActiveFamilyPerUser < 1
        ? 3
        : maxActiveFamilyPerUser;
  }
}

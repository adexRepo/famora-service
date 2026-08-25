package com.famora.common.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.namespace")
public record EnvironmentNamespaceProperties(
    @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]{0,30}") String environment
) {

  public String redisPrefix() {
    return environment + ":famora:";
  }

  public String expectedMinioBucket() {
    return environment + "-famora";
  }
}

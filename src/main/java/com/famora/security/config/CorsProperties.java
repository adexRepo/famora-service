package com.famora.security.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(List<String> allowedOrigins) {

  public CorsProperties {
    allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toList();
    if (allowedOrigins.contains("*")) {
      throw new IllegalArgumentException(
          "CORS allowed origins must be explicit when credentials are enabled");
    }
  }
}

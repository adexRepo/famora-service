package com.famora.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentIsolationValidator {

  private final EnvironmentNamespaceProperties namespaceProperties;
  private final String minioBucket;

  public EnvironmentIsolationValidator(EnvironmentNamespaceProperties namespaceProperties,
      @Value("${app.minio.bucket}") String minioBucket) {
    this.namespaceProperties = namespaceProperties;
    this.minioBucket = minioBucket;
  }

  @PostConstruct
  void validate() {
    String expectedBucket = namespaceProperties.expectedMinioBucket();
    if (!expectedBucket.equals(minioBucket)) {
      throw new IllegalStateException(
          "MinIO bucket must match the application environment: expected " + expectedBucket);
    }
  }
}

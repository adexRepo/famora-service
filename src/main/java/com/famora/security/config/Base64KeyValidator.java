package com.famora.security.config;

import java.util.Base64;

public final class Base64KeyValidator {

  private Base64KeyValidator() {
  }

  public static byte[] decode(String propertyName, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(propertyName + " must be configured");
    }
    if (!value.equals(value.trim()) || value.length() % 4 != 0
        || !value.matches("[A-Za-z0-9+/]+={0,2}")) {
      throw new IllegalArgumentException(
          propertyName + " must be standard Base64 without whitespace");
    }
    try {
      return Base64.getDecoder().decode(value);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(propertyName + " must be valid standard Base64", ex);
    }
  }
}

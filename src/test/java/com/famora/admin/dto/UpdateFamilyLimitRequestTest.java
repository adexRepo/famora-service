package com.famora.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class UpdateFamilyLimitRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsEnabledOverrideWithValueInsideAllowedRange() {
    assertThat(validator.validate(
        new UpdateFamilyLimitRequest(true, 10, "Beta tester"))).isEmpty();
  }

  @Test
  void rejectsEnabledOverrideWithoutValue() {
    assertThat(validator.validate(
        new UpdateFamilyLimitRequest(true, null, "Beta tester")))
        .extracting(violation -> violation.getMessage())
        .contains("maxFamilies is required only when overrideEnabled is true");
  }

  @Test
  void rejectsDisabledOverrideWithValue() {
    assertThat(validator.validate(
        new UpdateFamilyLimitRequest(false, 10, "Reset")))
        .extracting(violation -> violation.getMessage())
        .contains("maxFamilies is required only when overrideEnabled is true");
  }

  @Test
  void rejectsOverrideAboveMaximum() {
    assertThat(validator.validate(
        new UpdateFamilyLimitRequest(true, 101, "Too high")))
        .extracting(violation -> violation.getMessage())
        .contains("must be less than or equal to 100");
  }
}

package com.famora.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class UpdateBackupQuotaRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsEnabledOverrideWithFiftyGibibytes() {
    assertThat(validator.validate(new UpdateBackupQuotaRequest(
        true, 50L * 1024 * 1024 * 1024, "Premium storage"))).isEmpty();
  }

  @Test
  void rejectsEnabledOverrideWithoutQuota() {
    assertThat(validator.validate(new UpdateBackupQuotaRequest(true, null, "Missing")))
        .extracting(violation -> violation.getMessage())
        .contains("quotaBytes is required only when overrideEnabled is true");
  }

  @Test
  void rejectsDisabledOverrideWithQuota() {
    assertThat(validator.validate(new UpdateBackupQuotaRequest(false, 100L, "Reset")))
        .extracting(violation -> violation.getMessage())
        .contains("quotaBytes is required only when overrideEnabled is true");
  }

  @Test
  void rejectsQuotaAboveTenTebibytes() {
    assertThat(validator.validate(new UpdateBackupQuotaRequest(
        true, 10995116277761L, "Too high")))
        .extracting(violation -> violation.getMessage())
        .contains("must be less than or equal to 10995116277760");
  }
}

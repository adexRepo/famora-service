package com.famora.admin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateFamilyLimitRequest(
    @NotNull Boolean overrideEnabled,
    @Min(1) @Max(100) Integer maxFamilies,
    @NotBlank @Size(max = 500) String reason
) {

  @AssertTrue(message = "maxFamilies is required only when overrideEnabled is true")
  public boolean isFamilyLimitConfigurationValid() {
    if (overrideEnabled == null) {
      return true;
    }
    return overrideEnabled ? maxFamilies != null : maxFamilies == null;
  }
}

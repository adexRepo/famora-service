package com.famora.admin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBackupQuotaRequest(
    @NotNull Boolean overrideEnabled,
    @Min(1) @Max(10995116277760L) Long quotaBytes,
    @NotBlank @Size(max = 500) String reason
) {

  @AssertTrue(message = "quotaBytes is required only when overrideEnabled is true")
  public boolean isBackupQuotaConfigurationValid() {
    if (overrideEnabled == null) {
      return true;
    }
    return overrideEnabled ? quotaBytes != null : quotaBytes == null;
  }
}

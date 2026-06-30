package com.famora.business.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VoidReportRequest(
    @NotBlank(message = "Void reason is required")
    String reason
) {
}
